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
    private const val CHANNEL_CONVERSATION = "channel_person_conversation"
    private const val CHANNEL_CONVERSATION_SOUND = "channel_person_conversation_sound"
    private const val CHANNEL_CONVERSATION_VIBRATE = "channel_person_conversation_vibrate"
    private const val CHANNEL_CONVERSATION_SILENT = "channel_person_conversation_silent"
    private const val CHANNEL_NAME = "AI 紧急强提醒"
    private const val NOTIFICATION_ID_BASE = 888000
    private const val NOTIFICATION_ID_CONVERSATION = 887001

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
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_CONVERSATION, "人物对话即时提醒", NotificationManager.IMPORTANCE_HIGH).apply {
                this.description = "收到疑似人物对话时立即提醒，并保留在通知栏直到打开消息图谱"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 350, 150, 350)
                setSound(soundUri, audioAttributes)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setShowBadge(true)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_CONVERSATION_VIBRATE, "人物对话即时提醒（仅震动）", NotificationManager.IMPORTANCE_HIGH).apply {
                this.description = "收到疑似人物对话时立即震动，并保留在通知栏直到打开消息图谱"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 350, 150, 350)
                setSound(null, null)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setShowBadge(true)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_CONVERSATION_SOUND, "人物对话即时提醒（仅响铃）", NotificationManager.IMPORTANCE_HIGH).apply {
                this.description = "收到疑似人物对话时播放提示音，并保留在通知栏直到打开消息图谱"
                enableVibration(false)
                setSound(soundUri, audioAttributes)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setShowBadge(true)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_CONVERSATION_SILENT, "人物对话即时提醒（静默）", NotificationManager.IMPORTANCE_HIGH).apply {
                this.description = "收到疑似人物对话时保留高优先级通知，不播放声音或震动"
                enableVibration(false)
                setSound(null, null)
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
        messageId: Long,
        vibrate: Boolean,
        sound: Boolean
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
            context, 887001, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val channelId = when {
            sound && vibrate -> CHANNEL_CONVERSATION
            sound -> CHANNEL_CONVERSATION_SOUND
            vibrate -> CHANNEL_CONVERSATION_VIBRATE
            else -> CHANNEL_CONVERSATION_SILENT
        }
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_funnel)
            .setContentTitle("新的人物消息 · $appName")
            .setContentText(content.ifBlank { title })
            .setStyle(NotificationCompat.MessagingStyle("消息图谱").addMessage(
                content.ifBlank { title }, System.currentTimeMillis(), title
            ))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(false)
            .setContentIntent(pendingIntent)
        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_CONVERSATION, builder.build()) }
    }

    fun clearConversationAlert(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_CONVERSATION)
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
