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

class AiClient {
    private val client = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).readTimeout(120, TimeUnit.SECONDS).build()
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    suspend fun summarize(baseUrl: String, key: String, model: String, prompt: String, messages: List<CapturedMessage>): String {
        val input = messages.joinToString("\n") { "[${it.postedAt.timeText()}] [${it.appName}] ${it.title}: ${it.content}" }
        return call(baseUrl, key, model, prompt, "以下是当天收录消息：\n$input")
    }

    suspend fun test(baseUrl: String, key: String, model: String): String =
        call(baseUrl, key, model, "你是连接测试助手。", "只回复：连接成功")

    suspend fun listModels(baseUrl: String, key: String): List<String> = withContext(Dispatchers.IO) {
        val builder = Request.Builder()
            .url(normalizeModelsEndpoint(baseUrl))
            .get()
            .header("Accept", "application/json")
        if (key.isNotBlank()) builder.header("Authorization", "Bearer $key")
        client.newCall(builder.build()).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("模型接口返回 ${response.code}：${raw.take(300)}")
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
        client.newCall(builder.build()).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("接口返回 ${response.code}：${raw.take(300)}")
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
