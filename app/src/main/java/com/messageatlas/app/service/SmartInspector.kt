package com.messageatlas.app.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import com.messageatlas.app.MessageAtlasApp
import com.messageatlas.app.data.timeText
import com.messageatlas.app.network.AiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

object SmartInspector {
    const val ACTION_INSPECT = "com.messageatlas.app.ACTION_INSPECT_NOTIFICATIONS"
    private const val ALARM_REQUEST_CODE = 9991
    private val inspectMutex = Mutex()

    suspend fun inspectNow(context: Context, isManual: Boolean = false): String = withContext(Dispatchers.IO) {
        if (!inspectMutex.tryLock()) return@withContext "上一轮巡检仍在进行中"
        try {
            doInspect(context, isManual)
        } finally {
            inspectMutex.unlock()
        }
    }

    private suspend fun doInspect(context: Context, isManual: Boolean): String {
        val app = context.applicationContext as MessageAtlasApp
        val settings = app.settings.flow.first()

        if (!isManual && !settings.inspectionEnabled) {
            return "已关闭自动巡检"
        }

        if (settings.apiUrl.isBlank() || settings.model.isBlank()) {
            val res = "AI 接口未配置，请先在设置中配置 API"
            app.settings.recordInspection(System.currentTimeMillis(), res)
            return res
        }

        val key = app.settings.decryptApiKey(settings.encryptedApiKey)

        // 查询上次巡检之后的新消息；从未巡检过则回看一个周期
        val lookbackTime = if (settings.lastInspectionTime > 0) {
            settings.lastInspectionTime
        } else {
            System.currentTimeMillis() - (settings.inspectionIntervalMinutes.coerceAtLeast(10) * 60 * 1000L)
        }

        val messages = app.repository.getMessagesAfter(lookbackTime)
        val now = System.currentTimeMillis()
        val timeLabel = now.timeText()

        if (messages.isEmpty()) {
            val result = "$timeLabel 巡检完成 · 无新增消息"
            app.settings.recordInspection(now, result)
            return result
        }

        // 调用 AI 研判紧急重要程度
        val eval = runCatching {
            AiClient().evaluateUrgent(
                baseUrl = settings.apiUrl,
                key = key,
                model = settings.model,
                messages = messages
            )
        }.getOrElse { e ->
            val errResult = "$timeLabel 巡检失败：${e.message ?: "连接错误"}"
            app.settings.recordInspection(now, errResult)
            return errResult
        }

        if (!eval.isUrgent) {
            val result = "$timeLabel 已检查 ${messages.size} 条新消息 · 无紧急事项"
            app.settings.recordInspection(now, result)
            return result
        }

        eval.keyMessageId?.let { id ->
            app.repository.setImportant(id, true)
        }

        NotificationHelper.showUrgentAlert(
            context = context,
            title = eval.title,
            reason = eval.reason,
            action = eval.action,
            appName = eval.appName.ifBlank { "通知" },
            messageId = eval.keyMessageId,
            vibrate = settings.urgentVibrateEnabled,
            sound = settings.urgentSoundEnabled
        )

        val result = "🚨 $timeLabel 触发强提醒：${eval.title}"
        app.settings.recordInspection(now, result)
        return result
    }

    suspend fun scheduleNext(context: Context) {
        val app = context.applicationContext as MessageAtlasApp
        val settings = app.settings.flow.first()
        if (settings.inspectionEnabled) {
            scheduleInspectionAlarm(context, settings.inspectionIntervalMinutes)
        }
    }

    fun scheduleNextBlocking(context: Context) {
        runBlocking { scheduleNext(context) }
    }

    fun scheduleInspectionAlarm(context: Context, intervalMinutes: Int = 10) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            Intent(context, SmartInspectionReceiver::class.java).apply { action = ACTION_INSPECT },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = SystemClock.elapsedRealtime() + (intervalMinutes.coerceAtLeast(1) * 60 * 1000L)
        // Android 12+ 精确闹钟需要用户授权，未授权时退回到非精确闹钟，仅延迟几分钟
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancelInspectionAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            Intent(context, SmartInspectionReceiver::class.java).apply { action = ACTION_INSPECT },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
