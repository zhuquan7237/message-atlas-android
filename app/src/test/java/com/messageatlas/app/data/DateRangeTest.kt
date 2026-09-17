package com.messageatlas.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class DateRangeTest {
    @Test
    fun `day range ends exactly one millisecond before next day`() {
        val day = LocalDate.of(2026, 9, 17)

        assertEquals(day.plusDays(1).startMillis() - 1, day.endMillis())
    }

    @Test
    fun `day range uses the current system timezone`() {
        val day = LocalDate.of(2026, 9, 17)
        val expected = day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        assertEquals(expected, day.startMillis())
    }

    @Test
    fun `time text formats a valid local hour and minute`() {
        val instant = Instant.parse("2026-09-17T08:05:00Z").toEpochMilli()

        assertTrue(instant.timeText().matches(Regex("\\d{2}:\\d{2}")))
    }
}
