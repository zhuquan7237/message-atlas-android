package com.messageatlas.app.ui

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.messageatlas.app.MainViewModel
import com.messageatlas.app.UiState
import com.messageatlas.app.data.*

@Composable
internal fun HistoryScreen(state: UiState, vm: MainViewModel, openInbox: () -> Unit) {
    val context = LocalContext.current
    var selectedDate by rememberSaveable { mutableStateOf<String?>(null) }
    var deletingDate by rememberSaveable { mutableStateOf<String?>(null) }
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }
    val reports = remember(state.reports, favoritesOnly) { state.reports.filter { !favoritesOnly || it.isFavorite } }
    Page("每一天，都有重点", "AI 日报 · 在信息里找回清晰") {
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(!favoritesOnly, { favoritesOnly = false }, label = { Text("全部日报 "+state.reports.size) })
                    FilterChip(favoritesOnly, { favoritesOnly = true }, label = { Text("已收藏") }, leadingIcon = { Icon(Icons.Outlined.BookmarkBorder, null, Modifier.size(18.dp)) })
                }
            }
            if (reports.isEmpty()) item {
                EmptyState(if (favoritesOnly) "把值得回看的日子留下" else "第一份清晰，从今天开始", if (favoritesOnly) "收藏的日报会出现在这里。" else "在收件箱点击「整理这一天」，将通知转为摘要、待办与重要事项。", Icons.Outlined.AutoAwesome) {
                    if (!favoritesOnly) Button(onClick = openInbox) { Text("去收件箱") }
                }
            }
            items(reports, key = { it.dateKey }, contentType = { "report" }) { report ->
                val content = remember(report.markdown) { parseReportContent(report.markdown) }
                Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth().clickable { selectedDate = report.dateKey }) {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(report.dateKey.replace("-", "."), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                            IconButton(onClick = { vm.toggleFavorite(report.dateKey) }) { Icon(if (report.isFavorite) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder, if (report.isFavorite) "取消收藏" else "收藏日报", tint = MaterialTheme.colorScheme.primary) }
                        }
                        Text(content.summary, style = MaterialTheme.typography.bodyLarge, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(12.dp))
                        Text("立即关注 "+content.urgent.size+"  ·  待办 "+content.todos.size+"  ·  重要 "+content.important.size, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(report.model, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            IconButton(onClick = {
                                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_SUBJECT, report.dateKey+" 消息日报"); putExtra(Intent.EXTRA_TEXT, reportShareText(report)) }, "分享日报"))
                            }) { Icon(Icons.Outlined.IosShare, "分享日报", Modifier.size(20.dp)) }
                            IconButton(onClick = { deletingDate = report.dateKey }) { Icon(Icons.Outlined.DeleteOutline, "删除日报", Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }
            }
        }
    }
    state.reports.firstOrNull { it.dateKey == selectedDate }?.let { report -> ReportDialog(report) { selectedDate = null } }
    deletingDate?.let { date -> ConfirmDelete("删除这份日报？", date+" 的日报将从本机移除，原始消息不受影响。", { deletingDate = null }, { vm.deleteReport(date) }) }
}
