package com.messageatlas.app.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationDetectorTest {
    @Test
    fun isMessageCategoryConversation() {
        assertTrue(ConversationDetector.isPersonConversation("msg", false, false, null, "Alice", "Hi"))
    }

    @Test
    fun alarmIsNotConversation() {
        assertFalse(ConversationDetector.isPersonConversation("alarm", true, true, "MessagingStyle", "Alarm", "Wake up"))
    }

    @Test
    fun groupSummaryIsNotConversation() {
        assertFalse(ConversationDetector.isPersonConversation("msg", true, true, "MessagingStyle", "3 messages", "", true))
    }
}
