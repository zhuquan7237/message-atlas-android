package com.messageatlas.app.data

data class SourceApp(val packageName: String, val appName: String)
data class MessageSource(val packageName: String, val appName: String, val count: Int)
data class MessageFilter(val query: String = "", val source: String? = null, val importantOnly: Boolean = false)

/** Pure projection, computed off the UI thread rather than inside lazy items. */
fun filterMessages(messages: List<CapturedMessage>, filter: MessageFilter): List<CapturedMessage> {
    val query = filter.query.trim()
    return messages.filter { message ->
        (!filter.importantOnly || message.isImportant) &&
            (filter.source == null || message.packageName == filter.source) &&
            (query.isBlank() || sequenceOf(message.appName, message.title, message.content).any { it.contains(query, ignoreCase = true) })
    }
}

fun messageSources(messages: List<CapturedMessage>): List<MessageSource> = messages.groupBy { it.packageName }
    .map { (pkg, values) -> MessageSource(pkg, values.first().appName, values.size) }
    .sortedWith(compareByDescending<MessageSource> { it.count }.thenBy { it.appName })
