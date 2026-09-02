@file:OptIn(ExperimentalMaterial3Api::class)

package com.messageatlas.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.messageatlas.app.MainViewModel
import com.messageatlas.app.UpdateState
import com.messageatlas.app.UiState
import com.messageatlas.app.data.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private data class Destination(val route: String, val label: String, val icon: ImageVector)
private val destinations = listOf(
    Destination("home", "今日", Icons.Outlined.Home),
    Destination("rules", "规则", Icons.Outlined.Tune),
    Destination("history", "历史", Icons.Outlined.History),
    Destination("settings", "设置", Icons.Outlined.Settings)
)

@Composable
fun MessageAtlasRoot(vm: MainViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val current = nav.currentBackStackEntryAsState().value?.destination?.route
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.notice) {
        state.notice?.let {
            snackbar.showSnackbar(it)
            vm.consumeNotice()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.shadow(8.dp, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            ) {
                destinations.forEach { d ->
                    val selected = current == d.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            nav.navigate(d.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(d.icon, d.label) },
                        label = { Text(d.label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
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

@Composable
private fun Page(
    title: String,
    subtitle: String,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
            actions()
        }
        Spacer(Modifier.height(16.dp))
        content()
    }
}

@Composable
private fun HomeScreen(state: UiState, vm: MainViewModel, openAi: () -> Unit) {
    var searchOpen by remember { mutableStateOf(false) }
    val sources = remember(state.messages) { state.messages.distinctBy { it.packageName } }
    val urgentCount = remember(state.messages) { state.messages.count { it.isImportant } }
    val update by vm.update.collectAsStateWithLifecycle()

    Page(
        title = "消息图谱",
        subtitle = "AI 实时巡检 · 重点消息强提醒",
        actions = {
            Surface(
                color = if (state.settings.inspectionEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (state.settings.inspectionEnabled) DesignTokens.onlineGreen() else DesignTokens.idleGrey())
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (state.settings.inspectionEnabled) "${state.settings.inspectionIntervalMinutes}分巡检中" else "巡检已暂停",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (state.settings.inspectionEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    ) {
        // 发现新版本横幅
        when (val u = update) {
            is UpdateState.Available -> UpdateBanner(
                title = "发现新版本 ${u.info.version}",
                actionText = "更新",
                progress = null
            ) { vm.downloadUpdate() }
            is UpdateState.Downloading -> UpdateBanner(
                title = "正在下载更新包 ${u.progress}%",
                actionText = null,
                progress = u.progress
            ) {}
            is UpdateState.Downloaded -> UpdateBanner(
                title = "更新包已就绪",
                actionText = "安装",
                progress = null
            ) { vm.installDownloaded() }
            else -> {}
        }

        // AI 智能守护 Hero 卡片
        GuardianHeroCard(
            inspectionResult = state.settings.lastInspectionResult,
            busy = state.busy,
            inspectionEnabled = state.settings.inspectionEnabled,
            intervalMinutes = state.settings.inspectionIntervalMinutes,
            urgentCount = urgentCount,
            totalCount = state.messages.size,
            onInspectNow = vm::inspectNow,
            onGenerateReport = vm::generateReport,
            openAi = openAi
        )

        Spacer(Modifier.height(14.dp))

        // 现代化 7 日水平滑动日历选择条
        DateStripCarousel(
            selectedDay = state.day,
            onSelectDay = vm::selectDay
        )

        Spacer(Modifier.height(12.dp))

        // 搜索与来源筛选
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${state.visibleMessages.size} 条消息",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (urgentCount > 0) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = DesignTokens.urgentContainer(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "🔥 $urgentCount 条强提醒",
                            Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = DesignTokens.urgentColor(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            IconButton(
                onClick = { searchOpen = !searchOpen },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    if (searchOpen) Icons.Outlined.Close else Icons.Outlined.Search,
                    "搜索",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        AnimatedVisibility(searchOpen) {
            Column {
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = state.query,
                    onValueChange = vm::search,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("搜索来源、标题或内容…") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = { vm.search("") }) {
                                Icon(Icons.Outlined.Clear, null)
                            }
                        }
                    }
                )
                Spacer(Modifier.height(6.dp))
            }
        }

        // 来源水平滑动过滤器
        if (sources.isNotEmpty()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.source == null,
                    onClick = { vm.filterSource(null) },
                    label = { Text("全部 (${state.messages.size})") },
                    shape = RoundedCornerShape(12.dp)
                )
                sources.forEach { s ->
                    val count = state.messages.count { it.packageName == s.packageName }
                    FilterChip(
                        selected = state.source == s.packageName,
                        onClick = { vm.filterSource(if (state.source == s.packageName) null else s.packageName) },
                        label = { Text("${s.appName} ($count)") },
                        leadingIcon = {
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(DesignTokens.appColor(s.appName, s.packageName))
                            )
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // 消息列表
        if (state.visibleMessages.isEmpty()) {
            EmptyState("该日暂无收录消息", "开启通知读取权限后，手机收到的通知会自动在此呈现。")
        } else {
            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(state.visibleMessages, key = { it.id }, contentType = { "message" }) { message ->
                    ModernMessageCard(message = message, vm = vm)
                }
            }
        }
    }
}

@Composable
private fun GuardianHeroCard(
    inspectionResult: String,
    busy: Boolean,
    inspectionEnabled: Boolean,
    intervalMinutes: Int,
    urgentCount: Int,
    totalCount: Int,
    onInspectNow: () -> Unit,
    onGenerateReport: () -> Unit,
    openAi: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(DesignTokens.HeroGradient)
                .padding(18.dp)
        ) {
            Column {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "AI 智能巡检守护",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }

                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "每${intervalMinutes}分钟唤醒研判",
                            Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // 巡检状态指示
                Surface(
                    color = Color.Black.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (inspectionResult.isBlank()) "就绪 · 点击即刻检查最新消息" else inspectionResult,
                            color = Color.White.copy(alpha = 0.95f),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // 两个高质感毛玻璃操作按钮
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onInspectNow,
                        enabled = !busy,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.25f),
                            contentColor = Color.White
                        )
                    ) {
                        if (busy) {
                            CircularProgressIndicator(
                                Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(Icons.Outlined.Bolt, null, Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text("即刻巡检", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = onGenerateReport,
                        enabled = !busy && totalCount > 0,
                        modifier = Modifier
                            .weight(1.2f)
                            .height(44.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("AI 重点整理", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateBanner(title: String, actionText: String?, progress: Int?, onClick: () -> Unit) {
    Surface(
        color = DesignTokens.urgentContainer(),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.CloudDownload, null,
                    Modifier.size(18.dp), tint = DesignTokens.urgentColor()
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    title, Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    color = DesignTokens.urgentColor()
                )
                if (actionText != null) {
                    TextButton(onClick = onClick, enabled = progress == null, contentPadding = PaddingValues(horizontal = 10.dp)) {
                        Text(actionText, fontWeight = FontWeight.Bold)
                    }
                }
            }
            progress?.let {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { it / 100f },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                )
            }
        }
    }
}

@Composable
private fun IntervalPickerDialog(current: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var text by remember { mutableStateOf(current.toString()) }
    val parsed = text.trim().toIntOrNull()
    val valid = parsed != null && parsed in 1..720
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Alarm, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("自定义检查频率") },
        text = {
            Column {
                Text(
                    "每多少分钟自动唤醒一次 AI 巡检（1–720 分钟）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(5, 10, 15, 30, 60).forEach { preset ->
                        FilterChip(
                            selected = text.trim() == preset.toString(),
                            onClick = { text = preset.toString() },
                            label = { Text("${preset}分") }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { value -> text = value.filter { it.isDigit() }.take(3) },
                    Modifier.fillMaxWidth(),
                    label = { Text("自定义分钟数") },
                    singleLine = true,
                    isError = !valid,
                    supportingText = {
                        Text(
                            if (valid) "将在每 $parsed 分钟自动检查新消息" else "请输入 1–720 之间的整数",
                            color = if (valid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                )
            }
        },
        confirmButton = {
            Button(enabled = valid, onClick = { onConfirm(parsed!!); onDismiss() }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun DateStripCarousel(
    selectedDay: LocalDate,
    onSelectDay: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    // 展示过去 6 天至今日的滑动卡片
    val days = remember { (6 downTo 0).map { today.minusDays(it.toLong()) } }

    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        days.forEach { day ->
            val isSelected = day == selectedDay
            val isToday = day == today
            val weekdayText = when {
                isToday -> "今天"
                day == today.minusDays(1) -> "昨天"
                else -> when (day.dayOfWeek) {
                    DayOfWeek.MONDAY -> "周一"
                    DayOfWeek.TUESDAY -> "周二"
                    DayOfWeek.WEDNESDAY -> "周三"
                    DayOfWeek.THURSDAY -> "周四"
                    DayOfWeek.FRIDAY -> "周五"
                    DayOfWeek.SATURDAY -> "周六"
                    DayOfWeek.SUNDAY -> "周日"
                    else -> ""
                }
            }

            Surface(
                onClick = { onSelectDay(day) },
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                border = if (isSelected) null else BorderStroke(1.dp, DesignTokens.cardBorder()),
                modifier = Modifier
                    .width(54.dp)
                    .height(64.dp)
            ) {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = weekdayText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${day.dayOfMonth}",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernMessageCard(message: CapturedMessage, vm: MainViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val brandColor = remember(message.appName, message.packageName) {
        DesignTokens.appColor(message.appName, message.packageName)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (message.isImportant) DesignTokens.urgentColor().copy(alpha = 0.5f) else DesignTokens.cardBorder()
        )
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 应用专属彩底头像
                Surface(
                    color = brandColor,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = message.appName.take(1).ifBlank { "讯" },
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            message.appName,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            message.postedAt.timeText(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 若是强提醒/重点关注
                if (message.isImportant) {
                    Surface(
                        color = DesignTokens.urgentContainer(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "🔥 强提醒",
                            Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = DesignTokens.urgentColor(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                }

                IconButton(
                    onClick = { vm.toggleImportant(message.id) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        if (message.isImportant) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                        contentDescription = "标记重要",
                        tint = if (message.isImportant) DesignTokens.starColor() else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (message.title.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    message.title,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.height(4.dp))
            Text(
                message.content,
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (expanded) {
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { vm.deleteMessage(message.id) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Outlined.Delete, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("删除此条")
                    }
                }
            }
        }
    }
}

@Composable
private fun RulesScreen(state: UiState, vm: MainViewModel) {
    val apps = (state.messages.map { it.packageName to it.appName } + state.rules.map { it.packageName to it.appName })
        .distinctBy { it.first }
        .sortedBy { it.second }

    Page("收录规则", "配置哪些应用的通知允许进入消息图谱") {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            RuleMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = state.settings.ruleMode == mode,
                    onClick = { vm.setRuleMode(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index, 3)
                ) {
                    Text(
                        when (mode) {
                            RuleMode.ALL -> "全部收录"
                            RuleMode.WHITELIST -> "白名单模式"
                            RuleMode.BLACKLIST -> "黑名单模式"
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (apps.isEmpty()) {
            EmptyState("尚未捕获任何应用", "收到第一条手机通知后，该应用即可在此设置规则。")
        } else {
            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(apps, key = { it.first }, contentType = { "rule" }) { (pkg, name) ->
                    val rule = state.rules.firstOrNull { it.packageName == pkg }
                    val brandColor = remember(name, pkg) { DesignTokens.appColor(name, pkg) }

                    ElevatedCard(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = brandColor,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(name.take(1), color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)

                            when (state.settings.ruleMode) {
                                RuleMode.ALL -> Text("默认收录", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                RuleMode.WHITELIST -> Switch(
                                    rule?.action == AppRuleAction.ALLOW,
                                    { vm.setRule(pkg, name, if (it) AppRuleAction.ALLOW else null) }
                                )
                                RuleMode.BLACKLIST -> Switch(
                                    rule?.action != AppRuleAction.BLOCK,
                                    { vm.setRule(pkg, name, if (it) null else AppRuleAction.BLOCK) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiScreen(state: UiState, vm: MainViewModel, back: () -> Unit) {
    var url by remember(state.settings.apiUrl) { mutableStateOf(state.settings.apiUrl) }
    var model by remember(state.settings.model) { mutableStateOf(state.settings.model) }
    var key by remember { mutableStateOf("") }
    var prompt by remember(state.settings.prompt) { mutableStateOf(state.settings.prompt) }
    var modelMenu by remember { mutableStateOf(false) }

    Page("AI 模型接口", "兼容 OpenAI 标准接口，用于定时智能巡检与日报生成") {
        LazyColumn(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            item {
                OutlinedTextField(
                    url, { url = it },
                    Modifier.fillMaxWidth(),
                    label = { Text("API Base URL 或完整端点") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    supportingText = { Text("例如：https://cpa.zhuquan.xyz/v1 或 https://api.openai.com/v1") }
                )
            }

            item {
                OutlinedTextField(
                    key, { key = it },
                    Modifier.fillMaxWidth(),
                    label = { Text(if (state.settings.encryptedApiKey.isBlank()) "API 密钥" else "API 密钥（留空保持原密钥）") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
            }

            item {
                OutlinedButton(
                    onClick = { vm.fetchModels(url, key) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    enabled = !state.busy && url.isNotBlank(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (state.busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Outlined.CloudDownload, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (state.busy) "正在连接提供方…" else "从提供方获取模型列表")
                }
            }

            item {
                ExposedDropdownMenuBox(expanded = modelMenu, onExpandedChange = { modelMenu = it }) {
                    OutlinedTextField(
                        model, { model = it },
                        Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable),
                        label = { Text("模型 ID") },
                        placeholder = { Text("例如：gpt-4.1-mini / gemini-3.8-flash-high") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(modelMenu) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = modelMenu && state.availableModels.isNotEmpty(),
                        onDismissRequest = { modelMenu = false }
                    ) {
                        state.availableModels.forEach { id ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(id, fontWeight = if (id == model) FontWeight.Bold else FontWeight.Normal)
                                        if (id == model) Text("当前选中", style = MaterialTheme.typography.labelSmall)
                                    }
                                },
                                onClick = { model = id; modelMenu = false },
                                leadingIcon = { Icon(Icons.Outlined.SmartToy, null) }
                            )
                        }
                    }
                }
            }

            item {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Lock, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "API 密钥由 Android Keystore 硬件级 AES-GCM 加密，仅存储于本机，绝对不外传。",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    prompt, { prompt = it },
                    Modifier.fillMaxWidth().heightIn(min = 180.dp),
                    label = { Text("结构化重点整理 Prompt 模板") },
                    shape = RoundedCornerShape(14.dp),
                    supportingText = { Text("用于一键日报生成，输出高优先级重点卡片。") }
                )
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { vm.testAi(url, key, model) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        enabled = !state.busy,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("测试连接")
                    }

                    Button(
                        onClick = {
                            vm.saveAi(url, key.takeIf { it.isNotBlank() }, model, prompt)
                            back()
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("保存配置")
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryScreen(state: UiState, vm: MainViewModel) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf<DailyReport?>(null) }

    Page("历史日报", "每日 AI 智能整理重点本地归档") {
        if (state.reports.isEmpty()) {
            EmptyState("暂无历史日报", "在首页点击“AI 重点整理”生成第一份日报。")
        } else {
            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.reports, key = { it.dateKey }, contentType = { "report" }) { report ->
                    ElevatedCard(
                        Modifier
                            .fillMaxWidth()
                            .clickable { selected = report },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(report.dateKey, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(2.dp))
                                Text(report.model, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { vm.toggleFavorite(report.dateKey) }) {
                                Icon(
                                    if (report.isFavorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                                    "收藏",
                                    tint = if (report.isFavorite) DesignTokens.starColor() else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = {
                                context.startActivity(
                                    Intent.createChooser(
                                        Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, "${report.dateKey} 消息总结")
                                            putExtra(Intent.EXTRA_TEXT, reportShareText(report))
                                        },
                                        "导出日报"
                                    )
                                )
                            }) {
                                Icon(Icons.Outlined.Share, "导出")
                            }
                            IconButton(onClick = { vm.deleteReport(report.dateKey) }) {
                                Icon(Icons.Outlined.Delete, "删除")
                            }
                        }
                    }
                }
            }
        }
    }
    selected?.let { report -> ReportDialog(report) { selected = null } }
}

@Composable
private fun ReportDialog(report: DailyReport, close: () -> Unit) {
    val content = remember(report.markdown) { parseReportContent(report.markdown) }

    AlertDialog(
        onDismissRequest = close,
        confirmButton = {
            Button(onClick = close, shape = RoundedCornerShape(12.dp)) {
                Text("关闭")
            }
        },
        title = {
            Column {
                Text("${report.dateKey} 消息决策重点", fontWeight = FontWeight.Bold)
                Text(report.model, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            val urgentColor = DesignTokens.urgentColor()
            val todoColor = DesignTokens.todoColor()
            val importantColor = DesignTokens.importantColor()
            val normalColor = DesignTokens.normalColor()
            val mutedColor = DesignTokens.mutedColor()
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text("核心摘要", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(4.dp))
                            Text(content.summary, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                content.rawText?.let { raw -> item { Text(raw) } }
                reportSection("立即关注", content.urgent, urgentColor, Icons.Outlined.PriorityHigh)
                reportSection("待办事项", content.todos, todoColor, Icons.Outlined.CheckCircle)
                reportSection("重要信息", content.important, importantColor, Icons.Outlined.Star)
                reportSection("一般信息", content.normal, normalColor, Icons.Outlined.Info)
                reportSection("已归为次要", content.ignored, mutedColor, Icons.Outlined.FilterAltOff)
            }
        }
    )
}

private fun LazyListScope.reportSection(
    title: String,
    items: List<ReportItem>,
    color: Color,
    icon: ImageVector
) {
    if (items.isEmpty()) return
    item {
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("$title · ${items.size}", fontWeight = FontWeight.Bold, color = color, fontSize = 14.sp)
        }
    }
    items(items) { entry ->
        Surface(
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(entry.title.ifBlank { entry.detail }, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Text(
                        listOf(entry.source, entry.time).filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (entry.detail.isNotBlank() && entry.detail != entry.title) {
                    Spacer(Modifier.height(4.dp))
                    Text(entry.detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (entry.action.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        color = color.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "建议动作：${entry.action}",
                            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = color,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun reportShareText(report: DailyReport): String {
    val content = parseReportContent(report.markdown)
    if (content.rawText != null) return content.rawText
    fun section(title: String, values: List<ReportItem>) = if (values.isEmpty()) "" else buildString {
        appendLine()
        appendLine(title)
        values.forEach { item ->
            append("• ${item.title}")
            listOf(item.source, item.time).filter { it.isNotBlank() }.takeIf { it.isNotEmpty() }?.let { append("（${it.joinToString(" · ")}）") }
            if (item.detail.isNotBlank()) append("：${item.detail}")
            if (item.action.isNotBlank()) append("；下一步：${item.action}")
            appendLine()
        }
    }
    return buildString {
        appendLine("${report.dateKey} 消息重点")
        appendLine(content.summary)
        append(section("立即关注", content.urgent))
        append(section("待办事项", content.todos))
        append(section("重要信息", content.important))
        append(section("一般信息", content.normal))
        append(section("已归为次要", content.ignored))
    }.trim()
}

@Composable
private fun SettingsScreen(state: UiState, vm: MainViewModel, openAi: () -> Unit) {
    val context = LocalContext.current
    val listenerOn = remember {
        derivedStateOf {
            Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
                ?.contains(context.packageName) == true
        }
    }
    val update by vm.update.collectAsStateWithLifecycle()
    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrDefault("")
    }
    var intervalDialog by remember { mutableStateOf(false) }
    val interval = state.settings.inspectionIntervalMinutes

    Page("设置", "AI ${interval}分钟智能巡检、强提醒与系统权限") {
        LazyColumn(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            // 分组 1：AI 智能巡检与强提醒
            item {
                Text(
                    "AI ${interval}分钟智能巡检与强提醒",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                ElevatedCard(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Alarm, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("每 $interval 分钟自动检查新消息", fontWeight = FontWeight.Bold)
                                Text("后台唤醒 AI 研判紧急度，发现重要事项立即触发强提醒", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = state.settings.inspectionEnabled,
                                onCheckedChange = vm::setInspectionEnabled
                            )
                        }

                        Divider(Modifier.padding(vertical = 12.dp), color = DesignTokens.cardBorder())

                        Row(
                            Modifier.fillMaxWidth().clickable { intervalDialog = true },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Timer, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("检查频率", fontWeight = FontWeight.SemiBold)
                                Text("自定义自动巡检的唤醒间隔", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                "$interval 分钟",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Divider(Modifier.padding(vertical = 12.dp), color = DesignTokens.cardBorder())

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Vibration, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("强提醒震动", fontWeight = FontWeight.SemiBold)
                                Text("检测到重要事项时发出高感知强震动", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = state.settings.urgentVibrateEnabled,
                                onCheckedChange = vm::setUrgentVibrate
                            )
                        }

                        Divider(Modifier.padding(vertical = 12.dp), color = DesignTokens.cardBorder())

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.NotificationsActive, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("强提醒响铃", fontWeight = FontWeight.SemiBold)
                                Text("触发高优先级警报提示音", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = state.settings.urgentSoundEnabled,
                                onCheckedChange = vm::setUrgentSound
                            )
                        }

                        Divider(Modifier.padding(vertical = 12.dp), color = DesignTokens.cardBorder())

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = vm::testUrgentAlert,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Outlined.Notifications, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("测试强提醒通知")
                            }

                            Button(
                                onClick = vm::inspectNow,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Outlined.Bolt, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("即刻巡检一次")
                            }
                        }
                    }
                }
            }

            // 分组 2：AI 接口与模型
            item {
                Text(
                    "AI 大模型与提示词",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                SettingCard(
                    Icons.Outlined.AutoAwesome,
                    "AI 接口与模型配置",
                    "当前模型：${state.settings.model}",
                    openAi
                )
            }

            // 分组：版本与更新
            item {
                Text(
                    "版本与更新",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                ElevatedCard(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.SystemUpdateAlt, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("自动检查更新", fontWeight = FontWeight.SemiBold)
                                Text("当前版本 v$versionName · 每 24 小时静默检查一次", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = state.settings.updateCheckEnabled,
                                onCheckedChange = vm::setUpdateCheckEnabled
                            )
                        }

                        Divider(Modifier.padding(vertical = 12.dp), color = DesignTokens.cardBorder())

                        when (val u = update) {
                            is UpdateState.Checking -> Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("正在检查更新…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            is UpdateState.Available -> Column {
                                Text(
                                    "发现新版本 ${u.info.version}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (u.info.notes.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        u.info.notes.take(200),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 4,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(Modifier.height(10.dp))
                                Button(
                                    onClick = vm::downloadUpdate,
                                    modifier = Modifier.fillMaxWidth().height(44.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Outlined.CloudDownload, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("下载并安装")
                                }
                            }
                            UpdateState.UpToDate -> Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.CheckCircle, null, Modifier.size(16.dp), tint = DesignTokens.normalColor())
                                Spacer(Modifier.width(6.dp))
                                Text("已是最新版本", style = MaterialTheme.typography.bodyMedium, color = DesignTokens.normalColor())
                            }
                            is UpdateState.Downloading -> Column {
                                Text("正在下载更新包 ${u.progress}%", style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { u.progress / 100f },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                                )
                            }
                            is UpdateState.Downloaded -> Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.CheckCircle, null, Modifier.size(16.dp), tint = DesignTokens.normalColor())
                                Spacer(Modifier.width(6.dp))
                                Text("更新包已就绪", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                Button(onClick = vm::installDownloaded, shape = RoundedCornerShape(12.dp)) {
                                    Text("安装")
                                }
                            }
                            is UpdateState.Failed -> Column {
                                Text(u.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = vm::checkUpdateNow,
                                    modifier = Modifier.fillMaxWidth().height(44.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text("重试") }
                            }
                            UpdateState.Idle -> OutlinedButton(
                                onClick = vm::checkUpdateNow,
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("检查更新") }
                        }
                    }
                }
            }

            // 分组 3：系统权限与保活
            item {
                Text(
                    "权限与系统保活",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                SettingCard(
                    Icons.Outlined.Notifications,
                    "通知使用权",
                    if (listenerOn.value) "已开启 · 正在自动捕获系统通知" else "未开启：无法记录通知",
                    onClick = { context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")) }
                )
            }

            item {
                SettingCard(
                    Icons.Outlined.BatterySaver,
                    "后台忽略电池优化",
                    "允许无限制后台运行，确保 10 分钟定时巡检不被系统冻结",
                    onClick = { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
                )
            }

            item {
                SettingCard(
                    Icons.Outlined.PowerSettingsNew,
                    "自启动权限（按机型）",
                    "国内厂商 ROM 请前往应用详情开启自启动",
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                        )
                    }
                )
            }

            // 分组 4：外观与清理
            item {
                Text(
                    "外观与存储",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                ElevatedCard(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Animation, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("平滑界面过渡动画", fontWeight = FontWeight.SemiBold)
                            Text("页面切换与列表柔和弹簧动效", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(state.settings.animationEnabled, vm::setAnimation)
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = vm::clearDay,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("清空当前选中日期消息")
                }
            }

            item {
                OutlinedButton(
                    onClick = vm::clearAll,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("清空全部收录消息")
                }
            }

            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "隐私安全承诺：本应用完全本地优先，不申请通讯录、短信、相册、麦克风或定位权限。除 AI 巡检与日报生成发送至你配置的接口外，数据永不离开手机。",
                        Modifier.padding(12.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (intervalDialog) {
        IntervalPickerDialog(
            current = interval,
            onDismiss = { intervalDialog = false },
            onConfirm = vm::setInspectionInterval
        )
    }
}

@Composable
private fun SettingCard(
    icon: ImageVector,
    title: String,
    text: String,
    onClick: () -> Unit
) {
    ElevatedCard(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PermissionIntro(vm: MainViewModel) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = {},
        icon = { Icon(Icons.Outlined.Shield, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("开启通知守护服务", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("1. 通知读取权限：核心权限，用于实时记录手机通知，不开启无法工作。")
                Text("2. 强提醒横幅权限：当 AI 检测到极高重要度的紧急事件时弹出强提醒。")
                Text("3. 后台电池优化：建议允许忽略电池优化，确保每 10 分钟自动检查正常唤醒。")
                Text("安全承诺：不申请通讯录、相册、短信或定位权限，数据完全存储在本地。", fontWeight = FontWeight.SemiBold)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    vm.dismissOnboarding()
                    context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("去开启通知读取")
            }
        },
        dismissButton = {
            TextButton(onClick = vm::dismissOnboarding) {
                Text("稍后设置")
            }
        }
    )
}

@Composable
private fun EmptyState(title: String, text: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = CircleShape,
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Outlined.NotificationsNone,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}
