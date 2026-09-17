package com.messageatlas.app

import android.app.Application
import com.messageatlas.app.data.AppDatabase
import com.messageatlas.app.data.MessageRepository
import com.messageatlas.app.data.SettingsStore

class MessageAtlasApp : Application() {
    val database by lazy { AppDatabase.create(this) }
    val settings by lazy { SettingsStore(this) }
    val repository by lazy { MessageRepository(this, database, settings) }
}

