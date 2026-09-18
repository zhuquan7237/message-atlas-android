@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.messageatlas.app.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.messageatlas.app.MainViewModel
import com.messageatlas.app.UiState
import com.messageatlas.app.UpdateState
import com.messageatlas.app.data.*
import java.time.*
import java.time.format.DateTimeFormatter

@Composable
internal fun HomeScreen(state: UiState, vm: MainViewModel, openAi: () -> Unit, openReports: () -> Unit) {
    val listenerOn = rememberListenerAccess()
    val context = LocalContext.current
    var datePicker by rememberSaveable { mutableStateOf(false) }
    var aiConsent by rememberSaveable { mutableStateOf(false) }
    val update by vm.update.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    LaunchedEffect(state.day, state.source, state.importantOnly, state.query) { listState.scrollToItem(0) }
    Page("收件箱", "消息图谱 · 给每条消息一个去处", actions = {
        IconButton(onClick = { datePicker = true }) { Icon(Icons.Outlined.CalendarToday, "选择日期") }
    }) {
        LazyColumn(state = listState, modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            if (!listenerOn) item(key = "permission") {
                Surface(onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }, color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(18.dp)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.NotificationsNone, null)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text("开启收录，让消息在这里相遇", style = MaterialTheme.typography.titleMedium)
                            Text("需要授予通知使用权", style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(20.dp))
                    }
                }
            }
            item(key = "overview") { InboxOverview(state, onGenerate = { aiConsent = true }, onConfigure = openAi) }
            item(key = "date") {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (state.day == LocalDate.now()) "今天" else state.day.format(DateTimeFormatter.ofPattern("M 月 d 日")), style = MaterialTheme.typography.titleLarge)
                        Text(state.day.format(DateTimeFormatter.ofPattern("yyyy.MM.dd")), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { vm.selectDay(state.day.minusDays(1)) }) { Icon(Icons.Outlined.ChevronLeft, "前一天") }
                    if (state.day != LocalDate.now()) TextButton(onClick = { vm.selectDay(LocalDate.now()) }) { Text("回今天") }
                    IconButton(onClick = { vm.selectDay(state.day.plusDays(1)) }, enabled = state.day < LocalDate.now()) { Icon(Icons.Outlined.ChevronRight, "后一天") }
                }
            }
            item(key = "search") {
                OutlinedTextField(state.query, vm::search, Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text("搜索消息、联系人或应用") }, leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    trailingIcon = { if (state.query.isNotEmpty()) IconButton(onClick = { vm.search("") }) { Icon(Icons.Outlined.Close, "清空搜索") } },
                    shape = RoundedCornerShape(18.dp), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant))
            }
            item(key = "filters") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selected = !state.importantOnly && state.source == null, onClick = { vm.filterImportant(false); vm.filterSource(null) }, label = { Text("全部 "+state.messages.size) }) }
                    item { FilterChip(selected = state.importantOnly, onClick = { vm.filterImportant(!state.importantOnly) }, label = { Text("重点 "+state.importantCount) }, leadingIcon = { Icon(Icons.Outlined.StarBorder, null, Modifier.size(16.dp)) }) }
                    items(state.sources, key = { it.packageName }) { source ->
                        FilterChip(selected = state.source == source.packageName, onClick = { vm.filterSource(if (state.source == source.packageName) null else source.packageName) }, label = { Text(source.appName+" "+source.count) })
                    }
                }
            }
            if (state.busy) item(key = "busy") {
                Column {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("正在处理，请稍候…", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall)
                }
            }
            if (state.visibleMessages.isEmpty()) item(key = "empty") {
                if (state.query.isNotBlank() || state.source != null || state.importantOnly) {
                    EmptyState("没有找到匹配的消息", "试试其他关键词，或移除筛选条件。", Icons.Outlined.SearchOff) {
                        TextButton(onClick = { vm.search(""); vm.filterSource(null); vm.filterImportant(false) }) { Text("清除筛选") }
                    }
                } else EmptyState("留一点空白，也很好。", if (listenerOn) "这一天还没有消息。新通知到来后，会安静地收录在这里。" else "完成授权后收到的新通知会出现在这里；不会读取过往通知。")
            }
            items(state.visibleMessages, key = { it.id }, contentType = { "message" }) { message ->
                MessageCard(message, state.settings.animationEnabled, { vm.toggleImportant(message.id) }, { vm.deleteMessage(message.id) })
            }
            if (state.reports.any { it.dateKey == state.day.toString() }) item(key = "report") {
                TextButton(onClick = openReports, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("这一天的日报已整理，去看看") }
            }
            when (val u = update) {
                is UpdateState.Available -> item(key = "update") { UpdateBanner("新版本 "+u.info.version, "下载", null, vm::downloadUpdate) }
                is UpdateState.Downloading -> item(key = "update") { UpdateBanner("正在下载更新", null, u.progress) {} }
                is UpdateState.Downloaded -> item(key = "update") { UpdateBanner("更新已下载", "安装", null, vm::installDownloaded) }
                else -> Unit
            }
        }
    }
    if (datePicker) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = state.day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates { override fun isSelectableDate(utcTimeMillis: Long) = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate() <= LocalDate.now() })
        DatePickerDialog(onDismissRequest = { datePicker = false }, confirmButton = {
            TextButton(onClick = { picker.selectedDateMillis?.let { vm.selectDay(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }; datePicker = false }) { Text("查看这一天") }
        }, dismissButton = { TextButton(onClick = { datePicker = false }) { Text("取消") } }) { DatePicker(picker) }
    }
    if (aiConsent) AlertDialog(onDismissRequest = { aiConsent = false }, icon = { Icon(Icons.Outlined.AutoAwesome, null) },
        title = { Text("把这一天整理成重点") },
        text = { Text("将向你配置的模型接口发送所选日期的全部 "+state.messages.size+" 条消息（不受当前筛选影响）。日报保存在本机；同日已有日报将被替换。") },
        confirmButton = { Button(onClick = { aiConsent = false; vm.generateReport() }) { Text("开始整理") } },
        dismissButton = { TextButton(onClick = { aiConsent = false }) { Text("暂不发送") } })
}

@Composable
internal fun InboxOverview(state: UiState, onGenerate: () -> Unit, onConfigure: () -> Unit) {
    Surface(color = DesignTokens.Ink, contentColor = Color.White, shape = RoundedCornerShape(28.dp)) {
        Column(Modifier.fillMaxWidth().padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("一处收好，轻松找回", style = MaterialTheme.typography.labelLarge, color = DesignTokens.Lavender)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(state.messages.size.toString(), fontSize = 52.sp, lineHeight = 60.sp, fontWeight = FontWeight.Light, letterSpacing = (-2).sp)
                        Text("条消息", Modifier.padding(start = 10.dp, bottom = 10.dp), style = MaterialTheme.typography.bodyMedium, color = DesignTokens.Lavender)
                    }
                    Text(state.sources.size.toString()+" 个来源  ·  "+state.importantCount+" 条重点", style = MaterialTheme.typography.bodyMedium, color = DesignTokens.Lavender)
                }
                // Static vector geometry: no bitmap decoding, blur, or perpetual animation.
                Canvas(Modifier.size(76.dp).clearAndSetSemantics {}) {
                    val center = Offset(size.width / 2, size.height / 2)
                    for (i in 1..3) drawCircle(DesignTokens.Lavender.copy(alpha = 0.2f + i * 0.08f), size.width * i / 6.5f, center, style = Stroke(1.5.dp.toPx()))
                    drawCircle(Color(0xFFD0E0BE), 7.dp.toPx(), Offset(size.width * 0.83f, size.height * 0.25f))
                    drawCircle(DesignTokens.Lavender, 5.dp.toPx(), center)
                    drawCircle(Color(0xFFE9C6AE), 4.dp.toPx(), Offset(size.width * 0.23f, size.height * 0.76f))
                }
            }
            Spacer(Modifier.height(20.dp))
            Button(onClick = onGenerate, enabled = state.messages.isNotEmpty() && !state.busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DesignTokens.Lavender, contentColor = DesignTokens.Ink, disabledContainerColor = Color.White.copy(alpha = 0.1f), disabledContentColor = Color.White.copy(alpha = 0.55f))) {
                Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("整理这一天")
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().clickable(onClick = onConfigure).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Shield, null, Modifier.size(14.dp), tint = DesignTokens.Lavender)
                Spacer(Modifier.width(6.dp))
                Text(if (state.settings.inspectionEnabled) "自动巡检已开启 · "+state.settings.inspectionIntervalMinutes+" 分钟" else "本地归档 · AI 巡检未开启", style = MaterialTheme.typography.labelSmall, color = DesignTokens.Lavender, modifier = Modifier.weight(1f))
                Icon(Icons.Outlined.ChevronRight, null, Modifier.size(16.dp), tint = DesignTokens.Lavender)
            }
        }
    }
}

@Composable
internal fun MessageCard(message: CapturedMessage, motion: Boolean, toggleImportant: () -> Unit, delete: () -> Unit) {
    var expanded by rememberSaveable(message.id) { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
            .animateContentSize(animationSpec = if (motion) spring(stiffness = Spring.StiffnessMediumLow) else snap())
            .clip(RoundedCornerShape(22.dp)).clickable(onClickLabel = if (expanded) "收起消息" else "展开消息") { expanded = !expanded }
            .semantics { stateDescription = if (expanded) "已展开" else "已收起" }) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppAvatar(message.appName, message.packageName)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(message.appName, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(message.postedAt.timeText(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconToggleButton(checked = message.isImportant, onCheckedChange = { toggleImportant() }) {
                    Icon(if (message.isImportant) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                        if (message.isImportant) "取消重点" else "标记重点", tint = if (message.isImportant) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                }
            }
            if (message.title.isNotBlank()) { Spacer(Modifier.height(12.dp)); Text(message.title, style = MaterialTheme.typography.titleMedium, maxLines = if (expanded) Int.MAX_VALUE else 2, overflow = TextOverflow.Ellipsis) }
            if (message.content.isNotBlank()) { Spacer(Modifier.height(5.dp)); Text(message.content, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = if (expanded) Int.MAX_VALUE else 3, overflow = TextOverflow.Ellipsis) }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { deleting = true }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Icon(Icons.Outlined.DeleteOutline, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("删除消息") }
                }
            }
        }
    }
    if (deleting) ConfirmDelete("删除这条消息？", "仅删除本机归档，无法撤销。", { deleting = false }, delete)
}

@Composable
internal fun UpdateBanner(title: String, actionText: String?, progress: Int?, onClick: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.SystemUpdate, null, Modifier.size(20.dp))
                Text(title, Modifier.weight(1f).padding(horizontal = 10.dp), style = MaterialTheme.typography.bodyMedium)
                actionText?.let { TextButton(onClick = onClick) { Text(it) } }
            }
            progress?.let { LinearProgressIndicator(progress = { it / 100f }, modifier = Modifier.fillMaxWidth()) }
        }
    }
}
