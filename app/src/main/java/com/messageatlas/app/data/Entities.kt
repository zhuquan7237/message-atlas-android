package com.messageatlas.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "messages", indices = [Index("postedAt"), Index("packageName"), Index(value = ["notificationKey"], unique = true)])
data class CapturedMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val notificationKey: String, val packageName: String, val appName: String,
    val title: String, val content: String, val postedAt: Long,
    val capturedAt: Long = System.currentTimeMillis(),
    val originalOngoing: Boolean = false, val originalClearable: Boolean = true,
    val isImportant: Boolean = false
)

enum class RuleMode { ALL, WHITELIST, BLACKLIST }
enum class AppRuleAction { ALLOW, BLOCK }

@Entity(tableName = "app_rules")
data class AppRule(@PrimaryKey val packageName: String, val appName: String, val action: AppRuleAction)

@Entity(tableName = "daily_reports")
data class DailyReport(
    @PrimaryKey val dateKey: String, val markdown: String, val model: String,
    val generatedAt: Long, val isFavorite: Boolean = false
)

