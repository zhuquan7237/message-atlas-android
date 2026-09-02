package com.messageatlas.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(message: CapturedMessage)
    @Query("SELECT * FROM messages WHERE postedAt BETWEEN :start AND :end ORDER BY postedAt DESC") fun observeBetween(start: Long, end: Long): Flow<List<CapturedMessage>>
    @Query("SELECT * FROM messages WHERE postedAt BETWEEN :start AND :end ORDER BY postedAt ASC") suspend fun getBetween(start: Long, end: Long): List<CapturedMessage>
    @Query("SELECT * FROM messages WHERE postedAt > :since ORDER BY postedAt ASC") suspend fun getAfter(since: Long): List<CapturedMessage>
    @Query("UPDATE messages SET isImportant = NOT isImportant WHERE id = :id") suspend fun toggleImportant(id: Long)
    @Query("UPDATE messages SET isImportant = :important WHERE id = :id") suspend fun setImportant(id: Long, important: Boolean)
    @Query("DELETE FROM messages WHERE id = :id") suspend fun delete(id: Long)
    @Query("DELETE FROM messages WHERE postedAt BETWEEN :start AND :end") suspend fun deleteBetween(start: Long, end: Long)
    @Query("DELETE FROM messages") suspend fun deleteAll()
}

@Dao
interface RuleDao {
    @Query("SELECT * FROM app_rules ORDER BY appName COLLATE NOCASE") fun observeAll(): Flow<List<AppRule>>
    @Query("SELECT * FROM app_rules") suspend fun getAll(): List<AppRule>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(rule: AppRule)
    @Query("DELETE FROM app_rules WHERE packageName = :packageName") suspend fun delete(packageName: String)
}

@Dao
interface ReportDao {
    @Query("SELECT * FROM daily_reports ORDER BY dateKey DESC") fun observeAll(): Flow<List<DailyReport>>
    @Query("SELECT * FROM daily_reports WHERE dateKey = :dateKey") suspend fun get(dateKey: String): DailyReport?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(report: DailyReport)
    @Query("UPDATE daily_reports SET isFavorite = NOT isFavorite WHERE dateKey = :dateKey") suspend fun toggleFavorite(dateKey: String)
    @Query("DELETE FROM daily_reports WHERE dateKey = :dateKey") suspend fun delete(dateKey: String)
}
