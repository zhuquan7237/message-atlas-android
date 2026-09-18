package com.messageatlas.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.messageatlas.app.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StorageRegressionTest {
    @Test fun notificationUpdatesPreserveIdentityAndStarAndSuppressIdenticalAlerts() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).build()
        try {
            val dao = db.messageDao()
            val message = CapturedMessage(notificationKey = "same", packageName = "chat", appName = "聊天", title = "同事", content = "hello", postedAt = 100)
            val first = dao.capture(message)
            assertTrue(first.changed)
            dao.setImportant(first.id, true)
            val duplicate = dao.capture(message)
            assertFalse(duplicate.changed)
            assertEquals(first.id, duplicate.id)
            val changed = dao.capture(message.copy(content = "updated", postedAt = 200))
            assertTrue(changed.changed)
            assertEquals(first.id, changed.id)
            assertTrue(dao.findByKey("same")!!.isImportant)
            assertEquals(1, dao.getBetween(0, 300).size)
            assertEquals(listOf(SourceApp("chat", "聊天")), dao.observeSources().first())
        } finally { db.close() }
    }

    @Test fun failedInspectionDoesNotAdvanceCursorAndIntervalsAreBounded() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<MessageAtlasApp>()
        val previous = app.settings.flow.first()
        try {
            app.settings.recordInspection(1234, "success")
            app.settings.recordInspectionFailure("temporary network error")
            assertEquals(1234L, app.settings.flow.first().lastInspectionTime)
            assertEquals("temporary network error", app.settings.flow.first().lastInspectionResult)
            app.settings.setInspectionInterval(0)
            assertEquals(1, app.settings.flow.first().inspectionIntervalMinutes)
            app.settings.setInspectionInterval(999)
            assertEquals(720, app.settings.flow.first().inspectionIntervalMinutes)
        } finally {
            app.settings.recordInspection(previous.lastInspectionTime, previous.lastInspectionResult)
            app.settings.setInspectionInterval(previous.inspectionIntervalMinutes)
        }
    }
}
