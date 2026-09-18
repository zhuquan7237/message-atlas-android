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
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import com.messageatlas.app.MainActivity
import com.messageatlas.app.R

object NotificationHelper {
    private const val CHANNEL_ALERT = "channel_ai_urgent_alert"
    private const val CHANNEL_VIBRATE = "channel_ai_urgent_vibrate"
    private const val CHANNEL_SILENT = "channel_ai_urgent_silent"
    // Android persists channel importance, sound and vibration. A new ID prevents
    // stale v0.3.2 channel settings from silently weakening this alert.
    private const val CHANNEL_CONVERSATION_V2 = "channel_person_conversation_strong_v2"
    private const val CHANNEL_NAME = "AI 紧急强提醒"
    private const val NOTIFICATION_ID_BASE = 888000
    private const val NOTIFICATION_ID_CONVERSATION_BASE = 887000
    private const val NOTIFICATION_ID_CONVERSATION_SLOTS = 4

    fun createNotificationChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        @Suppress("DEPRECATION") // USAGE_NOTIFICATION_CONVERSATION 需要 API 36+，当前 compileSdk 35
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
            .build()
        val alarmSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val alarmAudioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_ALARM)
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
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_CONVERSATION_V2, "人物消息强提醒", NotificationManager.IMPORTANCE_HIGH).apply {
                this.description = "人物消息立即响铃、强震动并显示横幅，直到打开消息图谱"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 800, 200, 800, 200, 1200)
                enableLights(true)
                setSound(alarmSoundUri, alarmAudioAttributes)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setShowBadge(true)
            }
        )
    }

    fun showConversationAlert(
        context: Context,
        title: String,
        content: String,
        appName: String,
        messageId: Long
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        createNotificationChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("TARGET_MESSAGE_ID", messageId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, messageId.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notificationId = NOTIFICATION_ID_CONVERSATION_BASE +
            Math.floorMod(messageId, NOTIFICATION_ID_CONVERSATION_SLOTS.toLong()).toInt()
        val body = content.ifBlank { title }
        val style: NotificationCompat.Style = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val me = Person.Builder().setName("消息图谱").build()
            val author = Person.Builder().setName(title.ifBlank { "新消息" }).build()
            NotificationCompat.MessagingStyle(me).addMessage(body, System.currentTimeMillis(), author)
        } else {
            @Suppress("DEPRECATION")
            val legacy = NotificationCompat.MessagingStyle("消息图谱").addMessage(body, System.currentTimeMillis(), title)
            legacy
        }
        val builder = NotificationCompat.Builder(context, CHANNEL_CONVERSATION_V2)
            .setSmallIcon(R.drawable.ic_stat_funnel)
            .setContentTitle("新的人物消息 · $appName")
            .setContentText(body)
            .setStyle(style)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(false)
            .setDefaults(android.app.Notification.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
        runCatching {
            NotificationManagerCompat.from(context).apply {
                // Rotate through a few IDs so OEM builds cannot silently classify every
                // new message as a non-interruptive update, while keeping only one alert.
                repeat(NOTIFICATION_ID_CONVERSATION_SLOTS) {
                    cancel(NOTIFICATION_ID_CONVERSATION_BASE + it)
                }
                notify(notificationId, builder.build())
            }
        }
    }

    fun clearConversationAlert(context: Context) {
        NotificationManagerCompat.from(context).apply {
            repeat(NOTIFICATION_ID_CONVERSATION_SLOTS) {
                cancel(NOTIFICATION_ID_CONVERSATION_BASE + it)
            }
        }
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
        showConversationAlert(
            context = context,
            title = "人物消息测试",
            content = "这是一条人物消息强提醒测试，应触发铃声、强震动和横幅",
            appName = "消息图谱",
            messageId = System.currentTimeMillis()
        )
    }
}
