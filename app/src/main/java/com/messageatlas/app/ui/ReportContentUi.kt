@file:OptIn(ExperimentalMaterial3Api::class)

package com.messageatlas.app.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.messageatlas.app.data.*

@Composable
internal fun ReportDialog(report: DailyReport, close: () -> Unit) {
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

internal fun reportShareText(report: DailyReport): String {
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

