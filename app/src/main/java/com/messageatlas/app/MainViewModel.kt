package com.messageatlas.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.messageatlas.app.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class UiState(
    val day: LocalDate = LocalDate.now(), val messages: List<CapturedMessage> = emptyList(),
    val rules: List<AppRule> = emptyList(), val reports: List<DailyReport> = emptyList(),
    val settings: UserSettings = UserSettings(), val query: String = "", val source: String? = null,
    val busy: Boolean = false, val notice: String? = null, val availableModels: List<String> = emptyList()
) {
    val visibleMessages get() = messages.filter { m ->
        (query.isBlank() || listOf(m.appName, m.title, m.content).any { it.contains(query, true) }) &&
            (source == null || m.packageName == source)
    }
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as MessageAtlasApp
    private val repo = app.repository
    private val selectedDay = MutableStateFlow(LocalDate.now())
    private val query = MutableStateFlow("")
    private val source = MutableStateFlow<String?>(null)
    private val busy = MutableStateFlow(false)
    private val notice = MutableStateFlow<String?>(null)
    private val availableModels = MutableStateFlow<List<String>>(emptyList())

    private data class ContentState(
        val messages: List<CapturedMessage>, val rules: List<AppRule>,
        val reports: List<DailyReport>, val settings: UserSettings
    )
    private data class ControlState(
        val query: String, val source: String?, val busy: Boolean, val notice: String?,
        val availableModels: List<String>
    )

    private val content = combine(
        selectedDay.flatMapLatest(repo::messagesForDay), repo.rules, repo.reports, app.settings.flow
    ) { messages, rules, reports, settings -> ContentState(messages, rules, reports, settings) }
    private val controls = combine(query, source, busy, notice, availableModels) { q, s, b, n, models ->
        ControlState(q, s, b, n, models)
    }

    val state = combine(selectedDay, content, controls) { day, content, controls ->
        UiState(
            day = day, messages = content.messages, rules = content.rules, reports = content.reports,
            settings = content.settings, query = controls.query, source = controls.source,
            busy = controls.busy, notice = controls.notice, availableModels = controls.availableModels
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    fun selectDay(day: LocalDate) { selectedDay.value = day }
    fun search(value: String) { query.value = value }
    fun filterSource(value: String?) { source.value = value }
    fun toggleImportant(id: Long) = viewModelScope.launch { repo.toggleImportant(id) }
    fun deleteMessage(id: Long) = viewModelScope.launch { repo.deleteMessage(id) }
    fun clearDay() = viewModelScope.launch { repo.clearDay(selectedDay.value) }
    fun clearAll() = viewModelScope.launch { repo.clearAll() }
    fun setRuleMode(mode: RuleMode) = viewModelScope.launch { app.settings.setRuleMode(mode) }
    fun setRule(packageName: String, appName: String, action: AppRuleAction?) = viewModelScope.launch {
        if (action == null) repo.removeRule(packageName) else repo.upsertRule(AppRule(packageName, appName, action))
    }
    fun setAnimation(enabled: Boolean) = viewModelScope.launch { app.settings.setAnimation(enabled) }
    fun dismissOnboarding() = viewModelScope.launch { app.settings.setOnboardingSeen() }
    fun saveAi(url: String, key: String?, model: String, prompt: String) = viewModelScope.launch {
        app.settings.updateAi(url, key, model, prompt); notice.value = "AI 配置已安全保存"
    }
    fun testAi(url: String, key: String, model: String) = launchBusy("连接成功") {
        val effectiveKey = key.ifBlank { app.settings.decryptApiKey(app.settings.flow.first().encryptedApiKey) }
        repo.testConnection(url, effectiveKey, model)
    }
    fun fetchModels(url: String, key: String) = launchBusy("已获取模型列表") {
        val effectiveKey = key.ifBlank { app.settings.decryptApiKey(app.settings.flow.first().encryptedApiKey) }
        availableModels.value = repo.fetchModels(url, effectiveKey)
    }
    fun generateReport() = launchBusy("日报已生成并保存") { repo.generateReport(selectedDay.value) }
    fun toggleFavorite(date: String) = viewModelScope.launch { repo.toggleReportFavorite(date) }
    fun deleteReport(date: String) = viewModelScope.launch { repo.deleteReport(date) }
    fun consumeNotice() { notice.value = null }

    private fun launchBusy(success: String, block: suspend () -> Any) = viewModelScope.launch {
        busy.value = true
        notice.value = runCatching { block(); success }.getOrElse { it.message ?: "操作失败" }
        busy.value = false
    }
}
