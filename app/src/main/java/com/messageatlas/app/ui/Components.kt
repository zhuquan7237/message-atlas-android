package com.messageatlas.app.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.messageatlas.app.MainViewModel

/** System settings are external state: refresh whenever this activity resumes. */
@Composable
internal fun rememberListenerAccess(): Boolean {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    fun read() = context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)
    var enabled by remember { mutableStateOf(read()) }
    DisposableEffect(lifecycle, context) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) enabled = read() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    return enabled
}

@Composable
internal fun Page(
    title: String, subtitle: String,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 22.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 22.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(6.dp))
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
            actions()
        }
        content()
    }
}

@Composable
internal fun SectionLabel(text: String) {
    Text(text, Modifier.padding(top = 14.dp, bottom = 8.dp), style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun EmptyState(title: String, text: String, icon: ImageVector = Icons.Outlined.Inbox, action: (@Composable () -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(vertical = 36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(76.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(26.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(20.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(text, Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        action?.let { Spacer(Modifier.height(16.dp)); it() }
    }
}

@Composable
internal fun ConfirmDelete(title: String, description: String, dismiss: () -> Unit, confirm: () -> Unit) {
    AlertDialog(onDismissRequest = dismiss, icon = { Icon(Icons.Outlined.DeleteOutline, null) },
        title = { Text(title) }, text = { Text(description) },
        confirmButton = { TextButton(onClick = { confirm(); dismiss() }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("确认删除") } },
        dismissButton = { TextButton(onClick = dismiss) { Text("保留") } })
}

@Composable
internal fun AppAvatar(name: String, pkg: String, modifier: Modifier = Modifier) {
    val color = remember(name, pkg) { DesignTokens.appColor(name, pkg) }
    Box(modifier.size(42.dp).background(color.copy(alpha = 0.12f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
        Text(name.take(1).ifBlank { "讯" }, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun PermissionIntro(vm: MainViewModel) {
    val context = LocalContext.current
    AlertDialog(onDismissRequest = vm::dismissOnboarding,
        icon = { Icon(Icons.Outlined.AllInbox, null, Modifier.size(32.dp)) },
        title = { Text("消息，一处收好。") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("开启通知使用权后，新收到的通知会自动归档在这台设备上。随时搜索、标记重点，不必来回切换应用。")
                Text("AI 为可选功能：只有你主动整理或开启自动巡检，消息才会发送到你配置的模型接口。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { Button(onClick = { vm.dismissOnboarding(); context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }) { Text("开启通知收录") } },
        dismissButton = { TextButton(onClick = vm::dismissOnboarding) { Text("先看看") } })
}
