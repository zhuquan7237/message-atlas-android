package com.messageatlas.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ReportContentTest {
    @Test
    fun `parses all report categories and fields`() {
        val raw = """
            {
              "summary": "今天有两条重点",
              "urgent": [{
                "source": "日历",
                "time": "09:30",
                "title": "会议开始",
                "detail": "项目评审",
                "action": "立即加入"
              }],
              "todos": [{"title": "回复邮件"}],
              "important": [],
              "normal": [],
              "ignored": [{"title": "促销"}]
            }
        """.trimIndent()

        val result = parseReportContent(raw)

        assertEquals("今天有两条重点", result.summary)
        assertEquals(1, result.urgent.size)
        assertEquals("日历", result.urgent.single().source)
        assertEquals("立即加入", result.urgent.single().action)
        assertEquals("回复邮件", result.todos.single().title)
        assertEquals("促销", result.ignored.single().title)
        assertNull(result.rawText)
    }

    @Test
    fun `accepts fenced json response`() {
        val result = parseReportContent("""```json
            {"summary":"完成","urgent":[]}
            ```""".trimIndent())

        assertEquals("完成", result.summary)
        assertEquals(emptyList<ReportItem>(), result.urgent)
    }

    @Test
    fun `uses readable default when summary is blank`() {
        val result = parseReportContent("""{"summary":""}""")

        assertEquals("今日消息已整理", result.summary)
        assertNull(result.rawText)
    }

    @Test
    fun `preserves original text when response is not json`() {
        val raw = "这是一份旧版纯文本报告"

        val result = parseReportContent(raw)

        assertEquals("历史报告", result.summary)
        assertEquals(raw, result.rawText)
        assertNotNull(result.rawText)
    }
}

