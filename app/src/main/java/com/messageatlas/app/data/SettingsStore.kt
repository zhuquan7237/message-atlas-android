package com.messageatlas.app.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.dataStore by preferencesDataStore("settings")

data class UserSettings(
    val ruleMode: RuleMode = RuleMode.ALL,
    val apiUrl: String = "https://api.openai.com/v1",
    val model: String = "gpt-4.1-mini",
    val encryptedApiKey: String = "",
    val prompt: String = DEFAULT_PROMPT,
    val animationEnabled: Boolean = true,
    val onboardingSeen: Boolean = false,
    val inspectionEnabled: Boolean = false,
    val inspectionIntervalMinutes: Int = 10,
    val urgentVibrateEnabled: Boolean = true,
    val urgentSoundEnabled: Boolean = true,
    val lastInspectionTime: Long = 0L,
    val lastInspectionResult: String = "尚未开始检查",
    val updateCheckEnabled: Boolean = true,
    val lastUpdateCheckTime: Long = 0L
)

class SettingsStore(private val context: Context) {
    private object Keys {
        val ruleMode = stringPreferencesKey("rule_mode")
        val apiUrl = stringPreferencesKey("api_url")
        val model = stringPreferencesKey("model")
        val apiKey = stringPreferencesKey("api_key_encrypted")
        val prompt = stringPreferencesKey("prompt")
        val animation = booleanPreferencesKey("animation")
        val onboarding = booleanPreferencesKey("onboarding")
        val inspectionEnabled = booleanPreferencesKey("inspection_enabled")
        val inspectionInterval = intPreferencesKey("inspection_interval")
        val urgentVibrate = booleanPreferencesKey("urgent_vibrate")
        val urgentSound = booleanPreferencesKey("urgent_sound")
        val lastInspectionTime = longPreferencesKey("last_inspection_time")
        val lastInspectionResult = stringPreferencesKey("last_inspection_result")
        val updateCheckEnabled = booleanPreferencesKey("update_check_enabled")
        val lastUpdateCheckTime = longPreferencesKey("last_update_check_time")
    }

    val flow: Flow<UserSettings> = context.dataStore.data.map { p ->
        UserSettings(
            ruleMode = runCatching { RuleMode.valueOf(p[Keys.ruleMode] ?: RuleMode.ALL.name) }.getOrDefault(RuleMode.ALL),
            apiUrl = p[Keys.apiUrl] ?: "https://api.openai.com/v1",
            model = p[Keys.model] ?: "gpt-4.1-mini",
            encryptedApiKey = p[Keys.apiKey] ?: "",
            prompt = p[Keys.prompt]?.takeUnless { it == LEGACY_DEFAULT_PROMPT } ?: DEFAULT_PROMPT,
            animationEnabled = p[Keys.animation] ?: true,
            onboardingSeen = p[Keys.onboarding] ?: false,
            inspectionEnabled = p[Keys.inspectionEnabled] ?: false,
            inspectionIntervalMinutes = p[Keys.inspectionInterval] ?: 10,
            urgentVibrateEnabled = p[Keys.urgentVibrate] ?: true,
            urgentSoundEnabled = p[Keys.urgentSound] ?: true,
            lastInspectionTime = p[Keys.lastInspectionTime] ?: 0L,
            lastInspectionResult = p[Keys.lastInspectionResult] ?: "尚未开始检查",
            updateCheckEnabled = p[Keys.updateCheckEnabled] ?: true,
            lastUpdateCheckTime = p[Keys.lastUpdateCheckTime] ?: 0L
        )
    }

    suspend fun updateAi(apiUrl: String, apiKey: String?, model: String, prompt: String) {
        context.dataStore.edit { p ->
            p[Keys.apiUrl] = apiUrl.trim().trimEnd('/')
            p[Keys.model] = model.trim()
            p[Keys.prompt] = prompt
            if (apiKey != null) p[Keys.apiKey] = if (apiKey.isBlank()) "" else SecretBox.encrypt(apiKey)
        }
    }
    suspend fun setRuleMode(mode: RuleMode) { context.dataStore.edit { it[Keys.ruleMode] = mode.name } }
    suspend fun setAnimation(enabled: Boolean) { context.dataStore.edit { it[Keys.animation] = enabled } }
    suspend fun setOnboardingSeen() { context.dataStore.edit { it[Keys.onboarding] = true } }
    suspend fun setInspectionEnabled(enabled: Boolean) { context.dataStore.edit { it[Keys.inspectionEnabled] = enabled } }
    suspend fun setInspectionInterval(minutes: Int) { context.dataStore.edit { it[Keys.inspectionInterval] = minutes } }
    suspend fun setUrgentVibrate(enabled: Boolean) { context.dataStore.edit { it[Keys.urgentVibrate] = enabled } }
    suspend fun setUrgentSound(enabled: Boolean) { context.dataStore.edit { it[Keys.urgentSound] = enabled } }
    suspend fun recordInspection(time: Long, result: String) {
        context.dataStore.edit {
            it[Keys.lastInspectionTime] = time
            it[Keys.lastInspectionResult] = result
        }
    }
    suspend fun setUpdateCheckEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.updateCheckEnabled] = enabled }
    }
    suspend fun recordUpdateCheck(time: Long) {
        context.dataStore.edit { it[Keys.lastUpdateCheckTime] = time }
    }
    fun decryptApiKey(value: String): String = if (value.isBlank()) "" else SecretBox.decrypt(value)
}

private object SecretBox {
    private const val ALIAS = "message_atlas_api_key"
    private val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private fun key(): SecretKey {
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        return generator.generateKey()
    }
    fun encrypt(text: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        return Base64.encodeToString(cipher.iv + cipher.doFinal(text.toByteArray()), Base64.NO_WRAP)
    }
    fun decrypt(encoded: String): String {
        val all = Base64.decode(encoded, Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, all.copyOfRange(0, 12)))
        return String(cipher.doFinal(all.copyOfRange(12, all.size)))
    }
}

private const val LEGACY_DEFAULT_PROMPT = """你是一名严谨的个人消息整理助手。请仅依据输入消息生成中文 Markdown 报告。
要求：
1. 每条关键信息标注来源 APP、时间和核心内容；
2. 分类为社交、工作、系统通知、广告、紧急事项、次要信息；
3. 按极高重要、重要、一般、无效垃圾四级排序；
4. 最前面给出今日摘要和待办事项；
5. 不推测输入中不存在的事实，不泄露或复述 API 密钥。"""

const val DEFAULT_PROMPT = """你是个人消息决策助手。只依据输入消息提取事实，帮助用户在几秒内抓住重点。

必须只返回一个合法 JSON 对象，禁止 Markdown、代码围栏、解释文字。严格使用以下结构：
{
  "summary": "不超过60字的今日核心摘要",
  "urgent": [{"source":"APP","time":"HH:mm","title":"一句话重点","detail":"必要细节","action":"立即行动建议"}],
  "todos": [{"source":"APP","time":"HH:mm","title":"待办事项","detail":"截止时间或上下文","action":"下一步"}],
  "important": [{"source":"APP","time":"HH:mm","title":"重要信息","detail":"必要细节","action":"建议动作或空字符串"}],
  "normal": [{"source":"APP","time":"HH:mm","title":"普通信息","detail":"简要内容","action":""}],
  "ignored": [{"source":"APP","time":"HH:mm","title":"广告或无效信息","detail":"忽略理由","action":""}]
}

规则：
1. 极高风险、截止时间临近、支付、安全、必须回复的内容放 urgent。
2. 需要用户执行的内容放 todos，不要把纯资讯写成待办。
3. 同类重复通知合并，标题先写结论，避免复述长原文。
4. 每项必须保留来源 APP 和发生时间；没有内容的数组返回 []。
5. 不推测不存在的事实，不泄露或复述 API 密钥。"""
