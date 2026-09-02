package com.messageatlas.app.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.messageatlas.app.MessageAtlasApp
import com.messageatlas.app.data.CapturedMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class MessageNotificationListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onListenerConnected() {
        super.onListenerConnected()
        NotificationHelper.createNotificationChannel(this)
        scope.launch {
            val app = application as MessageAtlasApp
            if (app.settings.flow.first().inspectionEnabled) {
                SmartInspector.scheduleNext(this@MessageNotificationListener)
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty().trim()
        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.joinToString("\n") { it.toString() }.orEmpty()
        val text = sequenceOf(
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString(), lines,
            extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
            extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
        ).mapNotNull { it?.trim() }.firstOrNull { it.isNotBlank() }.orEmpty()
        if (title.isBlank() && text.isBlank()) return

        scope.launch {
            val app = application as MessageAtlasApp
            if (!app.repository.isAllowed(sbn.packageName)) return@launch
            val appName = runCatching { packageManager.getApplicationLabel(packageManager.getApplicationInfo(sbn.packageName, 0)).toString() }
                .getOrDefault(sbn.packageName)
            app.repository.insert(CapturedMessage(
                notificationKey = sbn.key, packageName = sbn.packageName, appName = appName,
                title = title, content = text, postedAt = sbn.postTime,
                originalOngoing = sbn.isOngoing, originalClearable = sbn.isClearable
            ))
        }
    }

    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}
