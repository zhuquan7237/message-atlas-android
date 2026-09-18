package com.messageatlas.app

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.messageatlas.app.data.*
import com.messageatlas.app.network.AppUpdate
import com.messageatlas.app.network.UpdateClient
import com.messageatlas.app.service.NotificationHelper
import com.messageatlas.app.service.SmartInspector
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.time.LocalDate

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class Available(val info: AppUpdate) : UpdateState
    data object UpToDate : UpdateState
    data class Downloading(val progress: Int) : UpdateState
    data class Downloaded(val info: AppUpdate, val file: File) : UpdateState
    data class Failed(val message: String) : UpdateState
}

data class UiState(
    val day: LocalDate = LocalDate.now(), val messages: List<CapturedMessage> = emptyList(),
    val visibleMessages: List<CapturedMessage> = emptyList(),
    val rules: List<AppRule> = emptyList(), val reports: List<DailyReport> = emptyList(),
    val settings: UserSettings = UserSettings(), val query: String = "", val source: String? = null,
    val busy: Boolean = false, val notice: String? = null, val availableModels: List<String> = emptyList(),
    val loaded: Boolean = false, val importantOnly: Boolean = false,
    val importantCount: Int = 0, val sources: List<MessageSource> = emptyList(),
    val knownApps: List<SourceApp> = emptyList()
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as MessageAtlasApp
    private val repo = app.repository
    private val selectedDay = MutableStateFlow(LocalDate.now())
    private val query = MutableStateFlow("")
    private val source = MutableStateFlow<String?>(null)
    private val importantOnly = MutableStateFlow(false)
    private val busy = MutableStateFlow(false)
    private val notice = MutableStateFlow<String?>(null)
    private val availableModels = MutableStateFlow<List<String>>(emptyList())
    private val updateClient = UpdateClient()
    private val _update = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val update: StateFlow<UpdateState> = _update

    init {
        maybeAutoCheckUpdate()
    }

    private data class ContentState(
        val day: LocalDate, val messages: List<CapturedMessage>, val rules: List<AppRule>,
        val reports: List<DailyReport>, val settings: UserSettings, val knownApps: List<SourceApp>
    )
    private data class ControlState(
        val query: String, val source: String?, val busy: Boolean, val notice: String?,
        val availableModels: List<String>
    )

    // Carry date and its rows together: a new date can never be paired with yesterday's rows.
    private val dayMessages = selectedDay.flatMapLatest { day -> repo.messagesForDay(day).map { day to it } }
    private val content = combine(dayMessages, repo.rules, repo.reports, app.settings.flow, repo.knownApps) { dated, rules, reports, settings, apps ->
        ContentState(dated.first, dated.second, rules, reports, settings, apps)
    }
    private val controls = combine(query, source, busy, notice, availableModels) { q, s, b, n, models ->
        ControlState(q, s, b, n, models)
    }

    val state = combine(content, controls, importantOnly) { content, controls, starred ->
        val visibleMessages = filterMessages(content.messages, MessageFilter(controls.query, controls.source, starred))
        UiState(
            day = content.day, messages = content.messages, visibleMessages = visibleMessages,
            rules = content.rules, reports = content.reports,
            settings = content.settings, query = controls.query, source = controls.source,
            busy = controls.busy, notice = controls.notice, availableModels = controls.availableModels,
            loaded = true, importantOnly = starred, importantCount = content.messages.count { it.isImportant },
            sources = messageSources(content.messages), knownApps = content.knownApps
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    fun selectDay(day: LocalDate) { if (day <= LocalDate.now()) { source.value = null; selectedDay.value = day } }
    fun filterImportant(value: Boolean) { importantOnly.value = value }
    fun search(value: String) { query.value = value }
    fun filterSource(value: String?) { source.value = value }
    fun toggleImportant(id: Long) = viewModelScope.launch { repo.toggleImportant(id) }
    fun deleteMessage(id: Long) = viewModelScope.launch { repo.deleteMessage(id) }
    fun clearDay() { val day = state.value.day; launchBusy("所选日期消息已清理") { repo.clearDay(day) } }
    fun clearAll() = viewModelScope.launch { repo.clearAll() }
    fun setRuleMode(mode: RuleMode) = viewModelScope.launch { app.settings.setRuleMode(mode) }
    fun setRule(packageName: String, appName: String, action: AppRuleAction?) = viewModelScope.launch {
        if (action == null) repo.removeRule(packageName) else repo.upsertRule(AppRule(packageName, appName, action))
    }
    fun setAnimation(enabled: Boolean) = viewModelScope.launch { app.settings.setAnimation(enabled) }
    fun dismissOnboarding() = viewModelScope.launch { app.settings.setOnboardingSeen() }
    fun saveAi(url: String, key: String?, model: String, prompt: String, onSaved: () -> Unit = {}) = launchBusy("AI 配置已安全保存") {
        require((url.trim().startsWith("https://") || url.trim().startsWith("http://")) && model.isNotBlank()) { "请填写有效的接口地址和模型 ID" }
        app.settings.updateAi(url, key, model, prompt)
        onSaved()
    }
    fun testAi(url: String, key: String, model: String) = launchBusy("连接成功") {
        val effectiveKey = key.ifBlank { app.settings.decryptAiConfig(app.settings.flow.first().protectedAiConfig) }
        repo.testConnection(url, effectiveKey, model)
    }
    fun fetchModels(url: String, key: String) = launchBusy("已获取模型列表") {
        val effectiveKey = key.ifBlank { app.settings.decryptAiConfig(app.settings.flow.first().protectedAiConfig) }
        availableModels.value = repo.fetchModels(url, effectiveKey)
    }
    fun generateReport() { val day = state.value.day; launchBusy("日报已生成，可在「日报」中查看") { repo.generateReport(day) } }
    fun toggleFavorite(date: String) = viewModelScope.launch { repo.toggleReportFavorite(date) }
    fun deleteReport(date: String) = viewModelScope.launch { repo.deleteReport(date) }

    fun setInspectionEnabled(enabled: Boolean) = viewModelScope.launch {
        app.settings.setInspectionEnabled(enabled)
        val context: Context = getApplication()
        if (enabled) SmartInspector.scheduleNext(context)
        else SmartInspector.cancelInspectionAlarm(context)
    }
    fun setInspectionInterval(minutes: Int) = viewModelScope.launch {
        app.settings.setInspectionInterval(minutes)
        val context: Context = getApplication()
        if (app.settings.flow.first().inspectionEnabled) SmartInspector.scheduleInspectionAlarm(context, minutes)
    }
    fun setUrgentVibrate(enabled: Boolean) = viewModelScope.launch { app.settings.setUrgentVibrate(enabled) }
    fun setUrgentSound(enabled: Boolean) = viewModelScope.launch { app.settings.setUrgentSound(enabled) }

    fun inspectNow() = launchBusy(null) { notice.value = SmartInspector.inspectNow(getApplication(), isManual = true) }

    fun testUrgentAlert() {
        NotificationHelper.showTestAlert(getApplication())
        notice.value = "已发出测试强提醒，请查看手机通知栏与横幅"
    }

    fun setUpdateCheckEnabled(enabled: Boolean) = viewModelScope.launch {
        app.settings.setUpdateCheckEnabled(enabled)
    }

    private fun currentVersion(): String = runCatching {
        getApplication<Application>().packageManager
            .getPackageInfo(getApplication<Application>().packageName, 0).versionName
    }.getOrNull()?.takeIf { it.isNotBlank() } ?: "0.0.0"

    /** 启动时静默检查更新，24 小时内不重复 */
    private fun maybeAutoCheckUpdate() = viewModelScope.launch {
        val settings = app.settings.flow.first()
        if (!settings.updateCheckEnabled) return@launch
        val now = System.currentTimeMillis()
        if (now - settings.lastUpdateCheckTime < 24 * 60 * 60 * 1000L) return@launch
        app.settings.recordUpdateCheck(now)
        runCatching { updateClient.latestRelease() }.getOrNull()?.let { release ->
            if (UpdateClient.isRemoteNewer(release.version, currentVersion())) {
                _update.value = UpdateState.Available(release)
            }
        }
    }

    fun checkUpdateNow() = viewModelScope.launch {
        if (_update.value is UpdateState.Downloading || _update.value is UpdateState.Checking) return@launch
        _update.value = UpdateState.Checking
        val release = runCatching { updateClient.latestRelease() }
            .getOrElse { _update.value = UpdateState.Failed(it.message ?: "网络错误"); return@launch }
        if (release == null) {
            _update.value = UpdateState.Failed("无法获取发布信息，请稍后重试")
            return@launch
        }
        if (UpdateClient.isRemoteNewer(release.version, currentVersion())) {
            _update.value = UpdateState.Available(release)
        } else {
            _update.value = UpdateState.UpToDate
        }
    }

    fun downloadUpdate() {
        val info = (_update.value as? UpdateState.Available)?.info ?: return
        _update.value = UpdateState.Downloading(0)
        viewModelScope.launch {
            runCatching {
                val context = getApplication<Application>()
                val dir = File(context.filesDir, "updates").apply { mkdirs() }
                val target = File(dir, "message-atlas-update.apk")
                updateClient.download(info, target) { progress ->
                    _update.value = UpdateState.Downloading(progress)
                }
                _update.value = UpdateState.Downloaded(info, target)
            }.onSuccess {
                installDownloaded()
            }.onFailure {
                _update.value = UpdateState.Failed("下载失败：${it.message ?: "网络错误"}")
            }
        }
    }

    fun installDownloaded() {
        val context = getApplication<Application>()
        val state = _update.value
        val file = (state as? UpdateState.Downloaded)?.file ?: return
        if (!context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            notice.value = "请先允许「安装未知应用」权限，返回后再点安装"
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
            .onFailure { notice.value = "无法启动安装器：${it.message ?: "未知错误"}" }
    }

    fun dismissUpdateStatus() {
        if (_update.value !is UpdateState.Downloading) _update.value = UpdateState.Idle
    }

    fun consumeNotice() { notice.value = null }

    private fun launchBusy(success: String?, block: suspend () -> Any) = viewModelScope.launch {
        if (busy.value) return@launch
        busy.value = true
        try {
            block()
            if (success != null) notice.value = success
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            notice.value = failure.message ?: "操作失败，请重试"
        } finally {
            busy.value = false
        }
    }
}
