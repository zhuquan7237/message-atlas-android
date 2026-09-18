package com.messageatlas.app.data

import org.junit.Assert.*
import org.junit.Test

class MessageFiltersTest {
    private val messages = listOf(
        CapturedMessage(1, "a", "chat", "微信", "小林", "下午 3 点开会", 100, isImportant = true),
        CapturedMessage(2, "b", "mail", "Mail", "Project update", "Ready for review", 90),
        CapturedMessage(3, "c", "chat", "微信", "工作群", "文档已发送", 80)
    )
    @Test fun emptyFiltersKeepAllRowsInOriginalOrder() { assertEquals(messages, filterMessages(messages, MessageFilter())) }
    @Test fun queryMatchesContentTitleAndAppIgnoringCaseAndOuterWhitespace() {
        assertEquals(listOf(messages[1]), filterMessages(messages, MessageFilter(" REVIEW ")))
        assertEquals(listOf(messages[0]), filterMessages(messages, MessageFilter("小林")))
        assertEquals(2, filterMessages(messages, MessageFilter("微信")).size)
    }
    @Test fun filtersIntersect() {
        assertEquals(listOf(messages[0]), filterMessages(messages, MessageFilter(source = "chat", importantOnly = true)))
        assertTrue(filterMessages(messages, MessageFilter(source = "mail", importantOnly = true)).isEmpty())
    }
    @Test fun countsUsePackageNotDisplayNameAndSortByCount() {
        assertEquals(listOf(MessageSource("chat", "微信", 2), MessageSource("mail", "Mail", 1)), messageSources(messages))
        assertEquals(2, messageSources(messages.map { it.copy(appName = "相同名称") }).size)
    }
    @Test fun missingSourceProducesEmptyResult() { assertTrue(filterMessages(messages, MessageFilter(source = "missing")).isEmpty()) }
}
