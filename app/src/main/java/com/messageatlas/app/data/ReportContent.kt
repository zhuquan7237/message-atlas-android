package com.messageatlas.app.data

import kotlinx.serialization.json.*

data class ReportItem(
    val source: String = "",
    val time: String = "",
    val title: String = "",
    val detail: String = "",
    val action: String = ""
)

data class ReportContent(
    val summary: String,
    val urgent: List<ReportItem> = emptyList(),
    val todos: List<ReportItem> = emptyList(),
    val important: List<ReportItem> = emptyList(),
    val normal: List<ReportItem> = emptyList(),
    val ignored: List<ReportItem> = emptyList(),
    val rawText: String? = null
)

fun parseReportContent(raw: String): ReportContent {
    val cleaned = raw.trim()
        .removePrefix("```json").removePrefix("```JSON").removePrefix("```")
        .removeSuffix("```").trim()
    return runCatching {
        val root = Json.parseToJsonElement(cleaned).jsonObject
        ReportContent(
            summary = root.string("summary").ifBlank { "今日消息已整理" },
            urgent = root.items("urgent"),
            todos = root.items("todos"),
            important = root.items("important"),
            normal = root.items("normal"),
            ignored = root.items("ignored")
        )
    }.getOrElse { ReportContent(summary = "历史报告", rawText = raw) }
}

private fun JsonObject.string(name: String): String = this[name]?.jsonPrimitive?.contentOrNull.orEmpty()

private fun JsonObject.items(name: String): List<ReportItem> =
    (this[name] as? JsonArray).orEmpty().mapNotNull { element ->
        (element as? JsonObject)?.let { item ->
            ReportItem(
                source = item.string("source"), time = item.string("time"),
                title = item.string("title"), detail = item.string("detail"),
                action = item.string("action")
            )
        }
    }
