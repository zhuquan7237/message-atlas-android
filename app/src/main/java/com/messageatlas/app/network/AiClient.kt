package com.messageatlas.app.network

import com.messageatlas.app.data.CapturedMessage
import com.messageatlas.app.data.timeText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class UrgentEvaluationResult(
    val isUrgent: Boolean,
    val urgentLevel: String = "NORMAL",
    val title: String = "",
    val reason: String = "",
    val action: String = "",
    val keyMessageId: Long? = null,
    val appName: String = ""
)

class AiClient {
    companion object {
        private val client = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).readTimeout(120, TimeUnit.SECONDS).callTimeout(150, TimeUnit.SECONDS).build()
        private const val MAX_INPUT_CHARS = 120_000
    }
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    suspend fun summarize(baseUrl: String, key: String, model: String, prompt: String, messages: List<CapturedMessage>): String {
        val input = messages.joinToString("\n") { "[${it.postedAt.timeText()}] [${it.appName}] ${it.title}: ${it.content}" }
        return call(baseUrl, key, model, prompt, "以下是当天收录消息：\n$input")
    }

    suspend fun evaluateUrgent(baseUrl: String, key: String, model: String, messages: List<CapturedMessage>): UrgentEvaluationResult {
        if (messages.isEmpty()) return UrgentEvaluationResult(isUrgent = false)
        val indexed = messages.mapIndexed { index, m ->
            "[消息${index + 1}] [${m.postedAt.timeText()}] [${m.appName}] 标题: ${m.title} | 内容: ${m.content.take(150)}"
        }.joinToString("\n")
        val systemPrompt = """你是一名专业严谨的手机消息紧急程度判断与强提醒决策专家。
你的任务是：从用户最新收到的消息中，识别出【极高重要性、紧急且需立即采取行动】的事项。

紧急重要判定范围（is_urgent 必须为 true）：
1. 涉及资金与安全：银行/支付宝/微信扣款异常、重要转账、账号安全报警、登录验证码。
2. 紧急工作协同：领导/核心同事单独@我或直接指派的紧急任务、线上系统故障、即将超期的待办。
3. 紧急人际联络：家人紧急联络、紧急未接电话。
4. 紧急行程：航班/车次变更或即将发车。

非紧急普通消息（is_urgent 必须为 false）：
1. 营销推广、电商大促打折、签到、新闻资讯、自媒体推送、短视频推荐。
2. 普通群聊闲聊、表情包、例行打卡、系统常规通知。

必须且仅返回合法 JSON，严禁任何额外文本或 Markdown：
{
  "is_urgent": true,
  "urgent_level": "CRITICAL",
  "title": "一句话紧迫标题",
  "reason": "为什么重要且必须立即处理",
  "action": "建议立即采取的行动",
  "key_index": 1
}"""

        val raw = call(baseUrl, key, model, systemPrompt, "以下是最新收到的消息：\n$indexed")
        return runCatching {
            val root = Json.parseToJsonElement(raw).jsonObject
            val isUrgent = root["is_urgent"]?.jsonPrimitive?.booleanOrNull ?: false
            val level = root["urgent_level"]?.jsonPrimitive?.contentOrNull ?: "NORMAL"
            val title = root["title"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val reason = root["reason"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val action = root["action"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val keyIndex = root["key_index"]?.jsonPrimitive?.intOrNull ?: 0
            val targetMsg = if (keyIndex in 1..messages.size) messages[keyIndex - 1] else messages.firstOrNull()
            UrgentEvaluationResult(
                isUrgent = isUrgent && (level.uppercase() in listOf("CRITICAL", "HIGH", "URGENT") || isUrgent),
                urgentLevel = level,
                title = title.ifBlank { targetMsg?.title ?: "重要通知" },
                reason = reason.ifBlank { targetMsg?.content?.take(40) ?: "收到重要新消息" },
                action = action.ifBlank { "请尽快查看处理" },
                keyMessageId = targetMsg?.id,
                appName = targetMsg?.appName ?: ""
            )
        }.getOrElse { error("AI 重要性判断格式无效，请检查模型是否支持 JSON 输出") }
    }

    suspend fun test(baseUrl: String, key: String, model: String): String =
        call(baseUrl, key, model, "你是连接测试助手。", "只回复：连接成功")

    suspend fun listModels(baseUrl: String, key: String): List<String> = withContext(Dispatchers.IO) {
        val builder = Request.Builder()
            .url(normalizeModelsEndpoint(baseUrl))
            .get()
            .header("Accept", "application/json")
        if (key.isNotBlank()) builder.header("Authorization", "Bearer $key")
        client.newCall(builder.build()).awaitResponse().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("模型接口返回 HTTP ${response.code}，请检查地址、密钥及服务额度")
            val root = Json.parseToJsonElement(raw)
            val items = when (root) {
                is JsonArray -> root
                is JsonObject -> root["data"] as? JsonArray
                    ?: root["models"] as? JsonArray
                    ?: root["items"] as? JsonArray
                    ?: JsonArray(emptyList())
                else -> JsonArray(emptyList())
            }
            items.mapNotNull { item ->
                when (item) {
                    is JsonPrimitive -> item.contentOrNull
                    is JsonObject -> listOf("id", "name", "model")
                        .firstNotNullOfOrNull { field -> item[field]?.jsonPrimitive?.contentOrNull }
                    else -> null
                }
            }.filter { it.isNotBlank() }.distinct().sorted()
                .ifEmpty { error("接口连接成功，但响应中没有可用模型") }
        }
    }

    private suspend fun call(baseUrl: String, key: String, model: String, system: String, user: String): String = withContext(Dispatchers.IO) {
        require(user.length <= MAX_INPUT_CHARS) { "消息内容过多，请清理不需要的通知后重试（单次最多 12 万字符）" }
        val endpoint = normalizeEndpoint(baseUrl)
        val body = buildJsonObject {
            put("model", model)
            putJsonArray("messages") {
                addJsonObject { put("role", "system"); put("content", system) }
                addJsonObject { put("role", "user"); put("content", user) }
            }
            put("temperature", 0.2)
        }.toString()
        val builder = Request.Builder().url(endpoint).post(body.toRequestBody(jsonType)).header("Accept", "application/json")
        if (key.isNotBlank()) builder.header("Authorization", "Bearer $key")
        client.newCall(builder.build()).awaitResponse().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("接口返回 HTTP ${response.code}，请检查地址、密钥及服务额度")
            val root = Json.parseToJsonElement(raw).jsonObject
            val content = root["choices"]?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.content
                ?: root["output_text"]?.jsonPrimitive?.content
                ?: error("接口响应中未找到文本内容")
            content.trim().removePrefix("```json").removePrefix("```JSON").removeSuffix("```").trim()
        }
    }

    private fun normalizeEndpoint(value: String): String {
        val url = value.trim().trimEnd('/')
        require(url.startsWith("https://") || url.startsWith("http://")) { "接口地址必须以 http:// 或 https:// 开头" }
        return if (url.endsWith("/chat/completions")) url else "$url/chat/completions"
    }

    private fun normalizeModelsEndpoint(value: String): String {
        var url = value.trim().trimEnd('/')
        require(url.startsWith("https://") || url.startsWith("http://")) { "接口地址必须以 http:// 或 https:// 开头" }
        listOf("/chat/completions", "/responses", "/completions", "/models").firstOrNull { url.endsWith(it) }
            ?.let { url = url.removeSuffix(it) }
        return "$url/models"
    }
}
