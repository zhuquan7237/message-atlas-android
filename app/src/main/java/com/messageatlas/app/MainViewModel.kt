package com.messageatlas.app

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
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
    val busy: Boolean = false, val notice: String? = null, val availableModels: List<String> = emptyList()
)

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
    private val updateClient = UpdateClient()
    private val _update = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val update: StateFlow<UpdateState> = _update

    init {
        maybeAutoCheckUpdate()
    }

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
        val visibleMessages = content.messages.filter { message ->
            (controls.query.isBlank() ||
                message.appName.contains(controls.query, ignoreCase = true) ||
                message.title.contains(controls.query, ignoreCase = true) ||
                message.content.contains(controls.query, ignoreCase = true)) &&
                (controls.source == null || message.packageName == controls.source)
        }
        UiState(
            day = day, messages = content.messages, visibleMessages = visibleMessages,
            rules = content.rules, reports = content.reports,
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

    fun setInspectionEnabled(enabled: Boolean) = viewModelScope.launch {
        app.settings.setInspectionEnabled(enabled)
        val context: Context = getApplication()
        if (enabled) SmartInspector.scheduleNext(context)
        else SmartInspector.cancelInspectionAlarm(context)
    }
    fun setInspectionInterval(minutes: Int) = viewModelScope.launch {
        app.settings.setInspectionInterval(minutes)
        val context: Context = getApplication()
        SmartInspector.scheduleInspectionAlarm(context, minutes)
    }
    fun setUrgentVibrate(enabled: Boolean) = viewModelScope.launch { app.settings.setUrgentVibrate(enabled) }
    fun setUrgentSound(enabled: Boolean) = viewModelScope.launch { app.settings.setUrgentSound(enabled) }

    fun inspectNow() = viewModelScope.launch {
        busy.value = true
        notice.value = runCatching { SmartInspector.inspectNow(getApplication(), isManual = true) }
            .getOrElse { it.message ?: "巡检失败" }
        busy.value = false
    }

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
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

    private fun launchBusy(success: String, block: suspend () -> Any) = viewModelScope.launch {
        busy.value = true
        notice.value = runCatching { block(); success }.getOrElse { it.message ?: "操作失败" }
        busy.value = false
    }
}
