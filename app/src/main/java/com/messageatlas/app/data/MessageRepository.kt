package com.messageatlas.app.data

import android.content.Context
import com.messageatlas.app.network.AiClient
import kotlinx.coroutines.flow.first
import java.time.*
import java.time.format.DateTimeFormatter

class MessageRepository(
    @Suppress("UNUSED_PARAMETER") context: Context, private val db: AppDatabase, private val settings: SettingsStore
) {
    val rules = db.ruleDao().observeAll()
    val reports = db.reportDao().observeAll()

    fun messagesForDay(day: LocalDate) = db.messageDao().observeBetween(day.startMillis(), day.endMillis())
    suspend fun messagesForDayNow(day: LocalDate) = db.messageDao().getBetween(day.startMillis(), day.endMillis())
    suspend fun getMessagesAfter(since: Long) = db.messageDao().getAfter(since)
    suspend fun insert(message: CapturedMessage) = db.messageDao().insert(message)
    suspend fun toggleImportant(id: Long) = db.messageDao().toggleImportant(id)
    suspend fun setImportant(id: Long, important: Boolean) = db.messageDao().setImportant(id, important)
    suspend fun deleteMessage(id: Long) = db.messageDao().delete(id)
    suspend fun clearDay(day: LocalDate) = db.messageDao().deleteBetween(day.startMillis(), day.endMillis())
    suspend fun clearAll() = db.messageDao().deleteAll()
    suspend fun upsertRule(rule: AppRule) = db.ruleDao().upsert(rule)
    suspend fun removeRule(packageName: String) = db.ruleDao().delete(packageName)
    suspend fun rulesNow() = db.ruleDao().getAll()
    suspend fun toggleReportFavorite(date: String) = db.reportDao().toggleFavorite(date)
    suspend fun deleteReport(date: String) = db.reportDao().delete(date)

    suspend fun isAllowed(packageName: String): Boolean {
        val config = settings.flow.first()
        val rule = db.ruleDao().getAll().firstOrNull { it.packageName == packageName }
        return when (config.ruleMode) {
            RuleMode.ALL -> true
            RuleMode.WHITELIST -> rule?.action == AppRuleAction.ALLOW
            RuleMode.BLACKLIST -> rule?.action != AppRuleAction.BLOCK
        }
    }

    suspend fun generateReport(day: LocalDate): DailyReport {
        val messages = messagesForDayNow(day)
        require(messages.isNotEmpty()) { "当天没有可整理的消息" }
        val config = settings.flow.first()
        val key = settings.decryptApiKey(config.encryptedApiKey)
        require(config.apiUrl.isNotBlank() && config.model.isNotBlank()) { "请先完整配置 AI 接口" }
        val markdown = AiClient().summarize(config.apiUrl, key, config.model, config.prompt, messages)
        val report = DailyReport(day.toString(), markdown, config.model, System.currentTimeMillis())
        db.reportDao().upsert(report)
        return report
    }

    suspend fun testConnection(apiUrl: String, key: String, model: String): String =
        AiClient().test(apiUrl, key, model)
    suspend fun fetchModels(apiUrl: String, key: String): List<String> = AiClient().listModels(apiUrl, key)

}

fun LocalDate.startMillis(): Long = atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
fun LocalDate.endMillis(): Long = plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
fun Long.timeText(): String = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
