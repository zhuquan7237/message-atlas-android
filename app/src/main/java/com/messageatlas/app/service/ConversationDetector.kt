package com.messageatlas.app.service

import android.app.Notification
import android.os.Build
import android.os.Bundle

/** Cheap, local-only signal for real person-to-person notifications. */
object ConversationDetector {
    private val nonConversationCategories = setOf(
        Notification.CATEGORY_CALL,
        Notification.CATEGORY_ALARM,
        Notification.CATEGORY_EVENT,
        Notification.CATEGORY_PROGRESS,
        Notification.CATEGORY_SERVICE,
        Notification.CATEGORY_TRANSPORT,
        Notification.CATEGORY_SYSTEM,
        Notification.CATEGORY_ERROR
    )

    fun isPersonConversation(notification: Notification, extras: Bundle, title: String, content: String): Boolean {
        val isGroupSummary = notification.flags and Notification.FLAG_GROUP_SUMMARY != 0
        return isPersonConversation(
            category = notification.category,
            hasMessages = extras.containsKey(Notification.EXTRA_MESSAGES),
            hasPeople = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                extras.containsKey(Notification.EXTRA_PEOPLE_LIST),
            template = extras.getString(Notification.EXTRA_TEMPLATE),
            title = title,
            content = content,
            isGroupSummary = isGroupSummary
        )
    }

    internal fun isPersonConversation(
        category: String?,
        hasMessages: Boolean,
        hasPeople: Boolean,
        template: String?,
        title: String,
        content: String,
        isGroupSummary: Boolean = false
    ): Boolean {
        if (isGroupSummary || (title.isBlank() && content.isBlank())) return false
        if (category in nonConversationCategories) return false
        return category == Notification.CATEGORY_MESSAGE ||
            hasMessages ||
            hasPeople ||
            template?.contains("MessagingStyle", ignoreCase = true) == true
    }
}
