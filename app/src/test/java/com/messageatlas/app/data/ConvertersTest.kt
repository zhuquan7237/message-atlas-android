package com.messageatlas.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {
    private val converters = Converters()

    @Test
    fun `round trips every rule action`() {
        AppRuleAction.entries.forEach { action ->
            assertEquals(action, converters.toAction(converters.fromAction(action)))
        }
    }
}

