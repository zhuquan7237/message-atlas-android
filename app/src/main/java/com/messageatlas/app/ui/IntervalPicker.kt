@file:OptIn(ExperimentalMaterial3Api::class)

package com.messageatlas.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun IntervalPickerDialog(current: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
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

