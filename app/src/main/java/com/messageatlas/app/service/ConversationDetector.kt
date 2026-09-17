package com.messageatlas.app.service

import android.app.Notification
import android.os.Build
import android.os.Bundle

/** Cheap, local-only signal for real person-to-person notifications. */
object ConversationDetector {
    private val knownMessagingPackages = setOf(
        "com.tencent.mm",
        "com.tencent.mobileqq",
        "com.alibaba.android.rimet",
        "com.ss.android.lark",
        "com.larksuite.suite",
        "org.telegram.messenger",
        "com.whatsapp",
        "com.facebook.orca",
        "com.discord",
        "com.Slack",
        "com.microsoft.teams",
        "com.google.android.apps.messaging",
        "com.android.mms"
    )

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

    fun isPersonConversation(
        packageName: String,
        notification: Notification,
        extras: Bundle,
        title: String,
        content: String
    ): Boolean {
        val isGroupSummary = notification.flags and Notification.FLAG_GROUP_SUMMARY != 0
        return isPersonConversation(
            packageName = packageName,
            category = notification.category,
            hasMessages = extras.containsKey(Notification.EXTRA_MESSAGES),
            hasPeople = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                extras.containsKey(Notification.EXTRA_PEOPLE_LIST),
            template = extras.getString(Notification.EXTRA_TEMPLATE),
            title = title,
            content = content,
            isGroupSummary = isGroupSummary,
            isOngoing = notification.flags and Notification.FLAG_ONGOING_EVENT != 0
        )
    }

    internal fun isPersonConversation(
        category: String?,
        hasMessages: Boolean,
        hasPeople: Boolean,
        template: String?,
        title: String,
        content: String,
        isGroupSummary: Boolean = false,
        isOngoing: Boolean = false,
        packageName: String = ""
    ): Boolean {
        if (isGroupSummary || isOngoing || (title.isBlank() && content.isBlank())) return false
        if (category in nonConversationCategories) return false
        return category == Notification.CATEGORY_MESSAGE ||
            hasMessages ||
            hasPeople ||
            template?.contains("MessagingStyle", ignoreCase = true) == true ||
            packageName in knownMessagingPackages
    }
}
