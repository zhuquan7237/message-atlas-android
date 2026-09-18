@file:OptIn(ExperimentalMaterial3Api::class)

package com.messageatlas.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.messageatlas.app.MainViewModel
import com.messageatlas.app.UiState

@Composable
internal fun AiScreen(state: UiState, vm: MainViewModel, back: () -> Unit) {
    var url by rememberSaveable(state.settings.apiUrl) { mutableStateOf(state.settings.apiUrl) }
    var model by rememberSaveable(state.settings.model) { mutableStateOf(state.settings.model) }
    var key by remember { mutableStateOf("") }
    var prompt by rememberSaveable(state.settings.prompt) { mutableStateOf(state.settings.prompt) }
    var modelMenu by remember { mutableStateOf(false) }

    Page("你的 AI 助手", "连接自己的模型，数据只发往你选择的接口", actions = { IconButton(onClick = back) { Icon(Icons.Outlined.Close, "返回") } }) {
        LazyColumn(
            Modifier.weight(1f).imePadding(),
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
                    supportingText = { Text("例如：https://api.example.com/v1；公网建议使用 HTTPS") }
                )
            }

            item {
                OutlinedTextField(
                    key, { key = it },
                    Modifier.fillMaxWidth(),
                    label = { Text(if (state.settings.protectedAiConfig.isBlank()) "API 密钥" else "API 密钥（留空保持原密钥）") },
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
                        placeholder = { Text("填写提供方支持的模型 ID") },
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
                            "密钥使用 Android Keystore 加密保存。调用模型时会向你填写的接口发送认证信息及消息内容，请只使用可信服务。",
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
                        enabled = !state.busy && url.isNotBlank() && model.isNotBlank(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("测试连接")
                    }

                    Button(
                        onClick = {
                            vm.saveAi(url, key.takeIf { it.isNotBlank() }, model, prompt, back)
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        enabled = !state.busy && url.isNotBlank() && model.isNotBlank(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("保存配置")
                    }
                }
            }
        }
    }
}
