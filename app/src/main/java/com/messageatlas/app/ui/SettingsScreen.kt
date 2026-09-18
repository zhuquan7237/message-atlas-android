package com.messageatlas.app.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.messageatlas.app.MainViewModel
import com.messageatlas.app.UiState
import com.messageatlas.app.UpdateState

@Composable
internal fun SettingsScreen(state: UiState, vm: MainViewModel, openAi: () -> Unit, openRules: () -> Unit) {
    val context = LocalContext.current
    val listenerOn = rememberListenerAccess()
    val update by vm.update.collectAsStateWithLifecycle()
    var intervalDialog by rememberSaveable { mutableStateOf(false) }
    var clearTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var inspectionConsent by rememberSaveable { mutableStateOf(false) }
    var manualConsent by rememberSaveable { mutableStateOf(false) }
    val version = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty() }
    Page("设置", "按你的节奏，收好每一条消息") {
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 28.dp)) {
            item {
                SettingsGroup {
                    SettingsRow(Icons.Outlined.NotificationsNone, "通知使用权", if (listenerOn) "已授权 · 可收录新通知" else "未授权 · 点击开启",
                        onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) })
                    SettingsDivider()
                    SettingsRow(Icons.Outlined.FilterList, "收录规则", "全部、白名单或黑名单", onClick = openRules)
                }
            }
            item { SectionLabel("智能整理") }
            item {
                SettingsGroup {
                    SettingsRow(Icons.Outlined.AutoAwesome, "AI 接口与模型", state.settings.model.ifBlank { "配置你自己的模型服务" }, onClick = openAi)
                    SettingsDivider()
                    SettingsRow(Icons.Outlined.Shield, "自动巡检", "将新增消息发送给配置的 AI，判断紧急事项", trailing = {
                        Switch(state.settings.inspectionEnabled, { if (it) inspectionConsent = true else vm.setInspectionEnabled(false) }, Modifier.semantics { contentDescription = "自动巡检" })
                    })
                    if (state.settings.inspectionEnabled) {
                        SettingsDivider()
                        SettingsRow(Icons.Outlined.Timer, "检查间隔", "约每 "+state.settings.inspectionIntervalMinutes+" 分钟；系统可能延迟", onClick = { intervalDialog = true })
                        SettingsDivider()
                        SettingsRow(Icons.AutoMirrored.Outlined.VolumeUp, "AI 提醒声音", "人物消息仍由独立渠道提醒", trailing = { Switch(state.settings.urgentSoundEnabled, vm::setUrgentSound, Modifier.semantics { contentDescription = "AI 提醒声音" }) })
                        SettingsDivider()
                        SettingsRow(Icons.Outlined.Vibration, "AI 提醒震动", "遵循系统通知与勿扰设置", trailing = { Switch(state.settings.urgentVibrateEnabled, vm::setUrgentVibrate, Modifier.semantics { contentDescription = "AI 提醒震动" }) })
                    }
                }
            }
            item {
                Text(state.settings.lastInspectionResult, Modifier.padding(horizontal = 4.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { manualConsent = true }, enabled = !state.busy) { Icon(Icons.Outlined.Bolt, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("巡检一次") }
                    TextButton(onClick = vm::testUrgentAlert) { Icon(Icons.Outlined.NotificationsActive, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("测试人物提醒") }
                }
            }
            item { SectionLabel("体验与系统") }
            item {
                SettingsGroup {
                    SettingsRow(Icons.Outlined.Animation, "界面动效", "轻量页面过渡；关闭后即时切换", trailing = { Switch(state.settings.animationEnabled, vm::setAnimation, Modifier.semantics { contentDescription = "界面动效" }) })
                    SettingsDivider()
                    SettingsRow(Icons.Outlined.BatterySaver, "后台电池设置", "查看系统省电限制，降低漏录风险", onClick = { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) })
                    SettingsDivider()
                    SettingsRow(Icons.Outlined.SettingsSuggest, "系统应用设置", "通知渠道、权限与厂商自启动设置", onClick = { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:"+context.packageName))) })
                }
            }
            item { SectionLabel("版本与更新") }
            item {
                SettingsGroup {
                    SettingsRow(Icons.Outlined.Update, "自动检查更新", "消息图谱 v"+version+" · 每 24 小时检查", trailing = { Switch(state.settings.updateCheckEnabled, vm::setUpdateCheckEnabled, Modifier.semantics { contentDescription = "自动检查更新" }) })
                    SettingsDivider()
                    Column(Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
                        when (val u = update) {
                            UpdateState.Idle -> TextButton(onClick = vm::checkUpdateNow) { Text("检查新版本") }
                            UpdateState.Checking -> { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("正在检查…", Modifier.padding(vertical = 12.dp)) }
                            UpdateState.UpToDate -> TextButton(onClick = vm::checkUpdateNow) { Text("已是最新版本 · 再次检查") }
                            is UpdateState.Available -> UpdateBanner("新版本 "+u.info.version, "下载", null, vm::downloadUpdate)
                            is UpdateState.Downloading -> UpdateBanner("下载中 "+u.progress+"%", null, u.progress) {}
                            is UpdateState.Downloaded -> UpdateBanner("更新已下载", "安装", null, vm::installDownloaded)
                            is UpdateState.Failed -> { Text(u.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error); TextButton(onClick = vm::checkUpdateNow) { Text("重试检查") } }
                        }
                    }
                }
            }
            item { SectionLabel("本机存储") }
            item {
                SettingsGroup {
                    SettingsRow(Icons.Outlined.DeleteOutline, "清理所选日期", state.day.toString()+" · "+state.messages.size+" 条消息", onClick = { clearTarget = "day" })
                    SettingsDivider()
                    SettingsRow(Icons.Outlined.DeleteSweep, "清理全部消息", "不删除日报、规则或 AI 配置", onClick = { clearTarget = "all" })
                }
            }
            item {
                Row(Modifier.padding(8.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.Lock, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("本地优先。通知保存在设备上；仅主动整理或开启巡检时发往你配置的 AI 接口。更新检查连接 GitHub。",
                        Modifier.padding(start = 10.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
    if (intervalDialog) IntervalPickerDialog(state.settings.inspectionIntervalMinutes, { intervalDialog = false }, vm::setInspectionInterval)
    clearTarget?.let { target -> ConfirmDelete(if (target == "all") "清空所有收录消息？" else "清空 "+state.day+" 的消息？", "此操作无法撤销。日报、规则和 AI 配置将保留。", { clearTarget = null }, { if (target == "all") vm.clearAll() else vm.clearDay() }) }
    if (inspectionConsent || manualConsent) AlertDialog(onDismissRequest = { inspectionConsent = false; manualConsent = false },
        title = { Text(if (inspectionConsent) "开启自动巡检？" else "检查新增消息？") },
        text = { Text("新增通知内容将发送到你在 AI 配置中填写的接口，可能包含私密信息并产生接口费用。自动巡检可随时关闭。") },
        confirmButton = { Button(onClick = { if (inspectionConsent) vm.setInspectionEnabled(true) else vm.inspectNow(); inspectionConsent = false; manualConsent = false }) { Text("同意并继续") } },
        dismissButton = { TextButton(onClick = { inspectionConsent = false; manualConsent = false }) { Text("取消") } })
}

@Composable
internal fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) { Column(content = content) }
}

@Composable
private fun SettingsDivider() { HorizontalDivider(Modifier.padding(start = 54.dp, end = 18.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)) }

@Composable
internal fun SettingsRow(icon: ImageVector, title: String, subtitle: String, onClick: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f).padding(start = 14.dp, end = 8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing?.invoke() ?: if (onClick != null) Icon(Icons.Outlined.ChevronRight, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.outline) else Unit
    }
}
