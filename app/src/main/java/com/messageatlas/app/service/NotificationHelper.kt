package com.messageatlas.app.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.messageatlas.app.MainActivity
import com.messageatlas.app.R

object NotificationHelper {
    private const val CHANNEL_ALERT = "channel_ai_urgent_alert"
    private const val CHANNEL_VIBRATE = "channel_ai_urgent_vibrate"
    private const val CHANNEL_SILENT = "channel_ai_urgent_silent"
    private const val CHANNEL_NAME = "AI 紧急强提醒"
    private const val NOTIFICATION_ID_BASE = 888000

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
            .build()
        val description = "当 AI 检测到重要紧急消息时触发的高优先级强提醒（支持横幅、强震动与提示音）"

        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERT, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                this.description = description
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                enableLights(true)
                setSound(soundUri, audioAttributes)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_VIBRATE, "$CHANNEL_NAME（仅震动）", NotificationManager.IMPORTANCE_HIGH).apply {
                this.description = description
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                enableLights(true)
                setSound(null, null)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_SILENT, "$CHANNEL_NAME（静默）", NotificationManager.IMPORTANCE_HIGH).apply {
                this.description = description
                enableVibration(false)
                enableLights(true)
                setSound(null, null)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
        )
    }

    fun showUrgentAlert(
        context: Context,
        title: String,
        reason: String,
        action: String,
        appName: String,
        messageId: Long? = null,
        vibrate: Boolean = true,
        sound: Boolean = true
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        createNotificationChannel(context)
        val channelId = when {
            sound -> CHANNEL_ALERT
            vibrate -> CHANNEL_VIBRATE
            else -> CHANNEL_SILENT
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            messageId?.let { putExtra("TARGET_MESSAGE_ID", it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            (messageId ?: System.currentTimeMillis()).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_funnel)
            .setContentTitle("🚨 [AI 强提醒] $title")
            .setContentText(reason.ifBlank { action })
            .setSubText(appName.ifBlank { "消息图谱" })
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("【紧急原因】$reason\n【建议行动】${if (action.isNotBlank()) action else "请立即查看"}\n【来源应用】$appName")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(
                NOTIFICATION_ID_BASE + (messageId?.toInt() ?: (System.currentTimeMillis() % 1000).toInt()),
                notification
            )
        }
    }

    fun showTestAlert(context: Context) {
        showUrgentAlert(
            context = context,
            title = "测试：检测到重要紧急事项",
            reason = "这是一条 AI 强提醒测试，已成功触发强震动与高优先级横幅通知！",
            action = "点击横幅即可直达消息图谱",
            appName = "消息图谱",
            vibrate = true,
            sound = true
        )
    }
}
