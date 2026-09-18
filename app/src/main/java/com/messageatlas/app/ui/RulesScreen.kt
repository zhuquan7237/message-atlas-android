package com.messageatlas.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.messageatlas.app.MainViewModel
import com.messageatlas.app.UiState
import com.messageatlas.app.data.*

@Composable
internal fun RulesScreen(state: UiState, vm: MainViewModel, back: () -> Unit) {
    val apps = remember(state.knownApps, state.rules) { (state.knownApps + state.rules.map { SourceApp(it.packageName, it.appName) }).distinctBy { it.packageName }.sortedBy { it.appName } }
    val rules = remember(state.rules) { state.rules.associateBy { it.packageName } }
    Page("收录规则", "只让你关心的消息留下来", actions = { IconButton(onClick = back) { Icon(Icons.Outlined.Close, "返回设置") } }) {
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                SettingsGroup {
                    RuleMode.entries.forEach { mode ->
                        val (title, description) = when (mode) {
                            RuleMode.ALL -> "全部收录" to "接收所有应用的新通知"
                            RuleMode.WHITELIST -> "仅收录选中应用" to "白名单 · 默认不收录"
                            RuleMode.BLACKLIST -> "排除选中应用" to "黑名单 · 默认收录"
                        }
                        SettingsRow(Icons.Outlined.FilterList, title, description, onClick = { vm.setRuleMode(mode) }, trailing = { RadioButton(state.settings.ruleMode == mode, { vm.setRuleMode(mode) }, Modifier.semantics { contentDescription = title }) })
                    }
                }
            }
            item { SectionLabel("已收录过的应用 · "+apps.size) }
            if (apps.isEmpty()) item { EmptyState("还没有应用记录", "先用「全部收录」接收一条通知，再回来配置。应用列表不受当前日期影响。", Icons.Outlined.Apps) }
            items(apps, key = { it.packageName }, contentType = { "rule" }) { app ->
                val rule = rules[app.packageName]
                SettingsGroup {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        AppAvatar(app.appName, app.packageName)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(app.appName, style = MaterialTheme.typography.titleMedium)
                            Text(if (state.settings.ruleMode == RuleMode.ALL) "自动收录" else if (state.settings.ruleMode == RuleMode.WHITELIST) "开启后允许收录" else "开启后排除此应用", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (state.settings.ruleMode != RuleMode.ALL) {
                            val action = if (state.settings.ruleMode == RuleMode.WHITELIST) AppRuleAction.ALLOW else AppRuleAction.BLOCK
                            Switch(rule?.action == action, { vm.setRule(app.packageName, app.appName, if (it) action else null) }, Modifier.semantics { contentDescription = app.appName+if (action == AppRuleAction.ALLOW) "允许收录" else "排除收录" })
                        }
                    }
                }
            }
        }
    }
}
