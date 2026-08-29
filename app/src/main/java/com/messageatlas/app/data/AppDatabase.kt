package com.messageatlas.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [CapturedMessage::class, AppRule::class, DailyReport::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun ruleDao(): RuleDao
    abstract fun reportDao(): ReportDao

    companion object {
        fun create(context: Context): AppDatabase = Room.databaseBuilder(
            context.applicationContext, AppDatabase::class.java, "message_atlas.db"
        ).build()
    }
}

