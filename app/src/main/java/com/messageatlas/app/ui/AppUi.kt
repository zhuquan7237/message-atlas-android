@file:OptIn(ExperimentalMaterial3Api::class)

package com.messageatlas.app.ui

import android.content.*
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.messageatlas.app.MainViewModel
import com.messageatlas.app.UiState
import com.messageatlas.app.data.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private data class Destination(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
private val destinations = listOf(
    Destination("home", "今日", Icons.Outlined.Home), Destination("rules", "规则", Icons.Outlined.Tune),
    Destination("history", "历史", Icons.Outlined.History), Destination("settings", "设置", Icons.Outlined.Settings)
)

@Composable fun MessageAtlasRoot(vm: MainViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val current = nav.currentBackStackEntryAsState().value?.destination?.route
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.notice) { state.notice?.let { snackbar.showSnackbar(it); vm.consumeNotice() } }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar { destinations.forEach { d ->
                NavigationBarItem(current == d.route, {
                    nav.navigate(d.route) { popUpTo(nav.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true }
                }, { Icon(d.icon, d.label) }, label = { Text(d.label) })
            } }
        }
    ) { padding ->
        val routeOrder = destinations.map { it.route } + "ai"
        val motionSpec: FiniteAnimationSpec<IntOffset> = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
        NavHost(
            nav, "home", Modifier.padding(padding),
            enterTransition = {
                if (!state.settings.animationEnabled) EnterTransition.None else {
                    val forward = routeOrder.indexOf(targetState.destination.route) >= routeOrder.indexOf(initialState.destination.route)
                    slideIntoContainer(if (forward) AnimatedContentTransitionScope.SlideDirection.Start else AnimatedContentTransitionScope.SlideDirection.End, motionSpec) +
                        fadeIn(tween(150, easing = LinearOutSlowInEasing))
                }
            },
            exitTransition = {
                if (!state.settings.animationEnabled) ExitTransition.None else {
                    val forward = routeOrder.indexOf(targetState.destination.route) >= routeOrder.indexOf(initialState.destination.route)
                    slideOutOfContainer(if (forward) AnimatedContentTransitionScope.SlideDirection.Start else AnimatedContentTransitionScope.SlideDirection.End, motionSpec) +
                        fadeOut(tween(110, easing = FastOutLinearInEasing))
                }
            },
            popEnterTransition = {
                if (!state.settings.animationEnabled) EnterTransition.None else
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, motionSpec) + fadeIn(tween(150))
            },
            popExitTransition = {
                if (!state.settings.animationEnabled) ExitTransition.None else
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, motionSpec) + fadeOut(tween(110))
            }
        ) {
            composable("home") { HomeScreen(state, vm, { nav.navigate("ai") }) }
            composable("rules") { RulesScreen(state, vm) }
            composable("history") { HistoryScreen(state, vm) }
            composable("settings") { SettingsScreen(state, vm, { nav.navigate("ai") }) }
            composable("ai") { AiScreen(state, vm, nav::popBackStack) }
        }
    }
    if (!state.settings.onboardingSeen) PermissionIntro(vm)
}

@Composable private fun Page(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(26.dp)); Text(title, style = MaterialTheme.typography.headlineLarge)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(18.dp)); content()
    }
}

@Composable private fun HomeScreen(state: UiState, vm: MainViewModel, openAi: () -> Unit) {
    var searchOpen by remember { mutableStateOf(false) }
    var sourceMenu by remember { mutableStateOf(false) }
    val sources = state.messages.distinctBy { it.packageName }
    Page("消息图谱", state.day.format(DateTimeFormatter.ofPattern("M月d日 EEEE"))) {
        Button(vm::generateReport, Modifier.fillMaxWidth().height(54.dp), enabled = !state.busy && state.messages.isNotEmpty(), shape = RoundedCornerShape(18.dp)) {
            if (state.busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp) else Icon(Icons.Outlined.AutoAwesome, null)
            Spacer(Modifier.width(10.dp)); Text(if (state.busy) "正在梳理今日消息…" else "一键 AI 智能整理", fontWeight = FontWeight.SemiBold)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton({ vm.selectDay(state.day.minusDays(1)) }) { Icon(Icons.Outlined.ChevronLeft, null); Text("前一天") }
            Spacer(Modifier.weight(1f))
            IconButton({ searchOpen = !searchOpen }) { Icon(Icons.Outlined.Search, "搜索") }
            Box {
                IconButton({ sourceMenu = true }) { Icon(Icons.Outlined.FilterList, "来源筛选") }
                DropdownMenu(sourceMenu, { sourceMenu = false }) {
                    DropdownMenuItem({ Text("全部来源") }, { vm.filterSource(null); sourceMenu = false })
                    sources.forEach { m -> DropdownMenuItem({ Text(m.appName) }, { vm.filterSource(m.packageName); sourceMenu = false }) }
                }
            }
            if (state.day < LocalDate.now()) TextButton({ vm.selectDay(state.day.plusDays(1)) }) { Text("后一天"); Icon(Icons.Outlined.ChevronRight, null) }
        }
        AnimatedVisibility(searchOpen) { OutlinedTextField(state.query, vm::search, Modifier.fillMaxWidth(), placeholder = { Text("搜索来源、标题或内容") }, singleLine = true) }
        Text("${state.visibleMessages.size} 条消息", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        if (state.visibleMessages.isEmpty()) EmptyState("暂时没有消息", "开启通知读取权限后，新通知会自动出现在这里。")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
            items(state.visibleMessages, key = { it.id }) { message -> MessageCard(message, vm) }
        }
    }
}

@Composable private fun MessageCard(message: CapturedMessage, vm: MainViewModel) {
    var expanded by remember { mutableStateOf(false) }
    AnimatedVisibility(true, enter = fadeIn()) {
        ElevatedCard(
            Modifier.fillMaxWidth().animateContentSize(
                spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
            ).clickable { expanded = !expanded },
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(9.dp)) { Text(message.appName.take(2), Modifier.padding(8.dp), fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(message.appName, fontWeight = FontWeight.SemiBold); Text(message.postedAt.timeText(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    IconButton({ vm.toggleImportant(message.id) }) { Icon(if (message.isImportant) Icons.Outlined.Star else Icons.Outlined.StarBorder, "重点") }
                }
                if (message.title.isNotBlank()) { Spacer(Modifier.height(8.dp)); Text(message.title, fontWeight = FontWeight.SemiBold) }
                Text(message.content, maxLines = if (expanded) Int.MAX_VALUE else 3, overflow = TextOverflow.Ellipsis)
                if (expanded) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton({ vm.deleteMessage(message.id) }) { Icon(Icons.Outlined.Delete, null); Text("删除") } }
            }
        }
    }
}

@Composable private fun RulesScreen(state: UiState, vm: MainViewModel) {
    val apps = (state.messages.map { it.packageName to it.appName } + state.rules.map { it.packageName to it.appName }).distinctBy { it.first }.sortedBy { it.second }
    Page("收录规则", "仅展示已捕获过的来源，不申请安装应用列表权限") {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) { RuleMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = state.settings.ruleMode == mode, onClick = { vm.setRuleMode(mode) },
                shape = SegmentedButtonDefaults.itemShape(index, 3)
            ) { Text(when(mode){RuleMode.ALL->"全部";RuleMode.WHITELIST->"白名单";RuleMode.BLACKLIST->"黑名单"}) }
        } }
        Spacer(Modifier.height(16.dp))
        if (apps.isEmpty()) EmptyState("还没有来源", "收到通知后即可在这里设置允许或屏蔽。") else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(apps, key = { it.first }) { (pkg, name) ->
                val rule = state.rules.firstOrNull { it.packageName == pkg }
                ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        when (state.settings.ruleMode) {
                            RuleMode.ALL -> Text("默认收录", style = MaterialTheme.typography.labelSmall)
                            RuleMode.WHITELIST -> Switch(rule?.action == AppRuleAction.ALLOW, { vm.setRule(pkg, name, if (it) AppRuleAction.ALLOW else null) })
                            RuleMode.BLACKLIST -> Switch(rule?.action != AppRuleAction.BLOCK, { vm.setRule(pkg, name, if (it) null else AppRuleAction.BLOCK) })
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun AiScreen(state: UiState, vm: MainViewModel, back: () -> Unit) {
    var url by remember(state.settings.apiUrl) { mutableStateOf(state.settings.apiUrl) }
    var model by remember(state.settings.model) { mutableStateOf(state.settings.model) }
    var key by remember { mutableStateOf("") }
    var prompt by remember(state.settings.prompt) { mutableStateOf(state.settings.prompt) }
    var modelMenu by remember { mutableStateOf(false) }
    Page("AI 自定义配置", "兼容 OpenAI Chat Completions 协议；公网接口建议使用 HTTPS") {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
            item { OutlinedTextField(url, { url = it }, Modifier.fillMaxWidth(), label = { Text("API Base URL 或完整端点") }, singleLine = true, supportingText = { Text("例如：https://api.openai.com/v1") }) }
            item { OutlinedTextField(key, { key = it }, Modifier.fillMaxWidth(), label = { Text(if (state.settings.encryptedApiKey.isBlank()) "API 密钥" else "API 密钥（留空则保持原密钥）") }, visualTransformation = PasswordVisualTransformation(), singleLine = true) }
            item {
                OutlinedButton(
                    { vm.fetchModels(url, key) }, Modifier.fillMaxWidth(), enabled = !state.busy && url.isNotBlank()
                ) {
                    if (state.busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Outlined.CloudDownload, null)
                    Spacer(Modifier.width(8.dp)); Text(if (state.busy) "正在连接提供方…" else "从提供方获取模型列表")
                }
            }
            item {
                ExposedDropdownMenuBox(expanded = modelMenu, onExpandedChange = { modelMenu = it }) {
                    OutlinedTextField(
                        model, { model = it }, Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable),
                        label = { Text("模型") }, placeholder = { Text("先获取列表，或直接输入模型 ID") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(modelMenu) }, singleLine = true
                    )
                    ExposedDropdownMenu(expanded = modelMenu && state.availableModels.isNotEmpty(), onDismissRequest = { modelMenu = false }) {
                        state.availableModels.forEach { id ->
                            DropdownMenuItem(
                                text = { Column { Text(id, fontWeight = if (id == model) FontWeight.Bold else FontWeight.Normal); if (id == model) Text("当前选择", style = MaterialTheme.typography.labelSmall) } },
                                onClick = { model = id; modelMenu = false },
                                leadingIcon = { Icon(Icons.Outlined.SmartToy, null) }
                            )
                        }
                    }
                }
            }
            item { Text("密钥由 Android Keystore 加密并仅保存在本机。生成报告时，消息会发送至你配置的接口；HTTP 仅建议用于可信局域网私有服务。", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            item { OutlinedTextField(prompt, { prompt = it }, Modifier.fillMaxWidth().heightIn(min = 220.dp), label = { Text("结构化整理模板 Prompt") }, supportingText = { Text("默认要求 AI 返回 JSON，由应用渲染为重点卡片，不展示 Markdown。") }) }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton({ vm.testAi(url, key, model) }, Modifier.weight(1f), enabled = !state.busy) { Text("测试连接") }
                    Button({ vm.saveAi(url, key.takeIf { it.isNotBlank() }, model, prompt); back() }, Modifier.weight(1f)) { Text("保存配置") }
                }
            }
        }
    }
}

@Composable private fun HistoryScreen(state: UiState, vm: MainViewModel) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf<DailyReport?>(null) }
    Page("历史记录", "每日消息与 AI 报告均保存在本机") {
        if (state.reports.isEmpty()) EmptyState("尚无日报", "在首页点击“一键 AI 智能整理”生成第一份日报。")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { items(state.reports, key = { it.dateKey }) { report ->
            ElevatedCard(Modifier.fillMaxWidth().clickable { selected = report }, shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(report.dateKey, fontWeight = FontWeight.SemiBold); Text(report.model, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    IconButton({ vm.toggleFavorite(report.dateKey) }) { Icon(if (report.isFavorite) Icons.Outlined.Star else Icons.Outlined.StarBorder, "收藏") }
                    IconButton({ context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_SUBJECT, "${report.dateKey} 消息总结"); putExtra(Intent.EXTRA_TEXT, reportShareText(report)) }, "导出日报")) }) { Icon(Icons.Outlined.Share, "导出") }
                    IconButton({ vm.deleteReport(report.dateKey) }) { Icon(Icons.Outlined.Delete, "删除") }
                }
            }
        } }
    }
    selected?.let { report -> ReportDialog(report) { selected = null } }
}

@Composable private fun ReportDialog(report: DailyReport, close: () -> Unit) {
    val content = remember(report.markdown) { parseReportContent(report.markdown) }
    AlertDialog(
        onDismissRequest = close,
        confirmButton = { TextButton(close) { Text("关闭") } },
        title = { Column { Text(report.dateKey + " 消息重点"); Text(report.model, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(14.dp)) { Text("今日摘要", fontWeight = FontWeight.Bold); Spacer(Modifier.height(4.dp)); Text(content.summary) }
                    }
                }
                content.rawText?.let { raw -> item { Text(raw) } }
                reportSection("立即关注", content.urgent, Color(0xFFD94A4A), Icons.Outlined.PriorityHigh)
                reportSection("待办事项", content.todos, Color(0xFFCC7A22), Icons.Outlined.CheckCircle)
                reportSection("重要信息", content.important, Color(0xFF496FB2), Icons.Outlined.Star)
                reportSection("一般信息", content.normal, Color(0xFF5D7A68), Icons.Outlined.Info)
                reportSection("已归为次要", content.ignored, Color(0xFF7A7A7A), Icons.Outlined.FilterAltOff)
            }
        }
    )
}

private fun LazyListScope.reportSection(
    title: String, items: List<ReportItem>, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    if (items.isEmpty()) return
    item {
        Row(verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = color); Spacer(Modifier.width(7.dp)); Text(title + " · ${items.size}", fontWeight = FontWeight.Bold, color = color) }
    }
    items(items) { entry ->
        Surface(shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, color.copy(alpha = 0.28f))) {
            Column(Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(entry.title.ifBlank { entry.detail }, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Text(listOf(entry.source, entry.time).filter { it.isNotBlank() }.joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (entry.detail.isNotBlank() && entry.detail != entry.title) { Spacer(Modifier.height(4.dp)); Text(entry.detail, style = MaterialTheme.typography.bodyMedium) }
                if (entry.action.isNotBlank()) { Spacer(Modifier.height(7.dp)); Surface(color = color.copy(alpha = 0.11f), shape = RoundedCornerShape(8.dp)) { Text("下一步：${entry.action}", Modifier.padding(horizontal = 8.dp, vertical = 5.dp), color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold) } }
            }
        }
    }
}

private fun reportShareText(report: DailyReport): String {
    val content = parseReportContent(report.markdown)
    if (content.rawText != null) return content.rawText
    fun section(title: String, values: List<ReportItem>) = if (values.isEmpty()) "" else buildString {
        appendLine(); appendLine(title)
        values.forEach { item ->
            append("• ${item.title}")
            listOf(item.source, item.time).filter { it.isNotBlank() }.takeIf { it.isNotEmpty() }?.let { append("（${it.joinToString(" · ")}）") }
            if (item.detail.isNotBlank()) append("：${item.detail}")
            if (item.action.isNotBlank()) append("；下一步：${item.action}")
            appendLine()
        }
    }
    return buildString {
        appendLine("${report.dateKey} 消息重点"); appendLine(content.summary)
        append(section("立即关注", content.urgent)); append(section("待办事项", content.todos))
        append(section("重要信息", content.important)); append(section("一般信息", content.normal))
        append(section("已归为次要", content.ignored))
    }.trim()
}

@Composable private fun SettingsScreen(state: UiState, vm: MainViewModel, openAi: () -> Unit) {
    val context = LocalContext.current
    val listenerOn = remember { derivedStateOf { Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")?.contains(context.packageName) == true } }
    Page("设置", "权限、界面与本地存储") {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { SettingCard(Icons.Outlined.Notifications, "通知读取权限", if (listenerOn.value) "已开启" else "未开启：无法自动收录通知") { context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")) } }
            item { SettingCard(Icons.Outlined.BatterySaver, "后台运行设置", "允许忽略电池优化可降低漏录概率") { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) } }
            item { SettingCard(Icons.Outlined.PowerSettingsNew, "自启动（可选）", "不同厂商入口不同，请在系统应用管理中开启") { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:${context.packageName}"))) } }
            item { SettingCard(Icons.Outlined.AutoAwesome, "AI 接口与模板", "自定义地址、密钥、模型与 Prompt", openAi) }
            item { ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Animation, null); Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text("界面动画", fontWeight = FontWeight.SemiBold); Text("页面和列表细腻过渡", style = MaterialTheme.typography.labelSmall) }; Switch(state.settings.animationEnabled, vm::setAnimation) } } }
            item { OutlinedButton(vm::clearDay, Modifier.fillMaxWidth()) { Text("清空当前日期消息") } }
            item { Text("隐私承诺：应用不申请通讯录、短信、相册或定位权限。除你主动生成 AI 报告外，消息不会离开设备。", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable private fun SettingCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, text: String, onClick: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null); Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold); Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Icon(Icons.Outlined.ChevronRight, null) }
    }
}

@Composable private fun PermissionIntro(vm: MainViewModel) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = {}, icon = { Icon(Icons.Outlined.PrivacyTip, null) }, title = { Text("先完成必要设置") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("1. 通知读取：核心权限，用于读取通知标题、内容、时间与来源；不开启将无法工作。")
            Text("2. 后台运行：建议关闭本应用电池优化，减少系统休眠造成的漏录。")
            Text("3. 自启动：可选，请按手机品牌需要开启。")
            Text("不会申请通讯录、短信、相册、定位权限。", fontWeight = FontWeight.SemiBold)
        } },
        confirmButton = { Button({ vm.dismissOnboarding(); context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")) }) { Text("去开启通知读取") } },
        dismissButton = { TextButton(vm::dismissOnboarding) { Text("稍后设置") } }
    )
}

@Composable private fun EmptyState(title: String, text: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 54.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Outlined.Inbox, null, Modifier.size(46.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(12.dp)); Text(title, fontWeight = FontWeight.SemiBold); Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
