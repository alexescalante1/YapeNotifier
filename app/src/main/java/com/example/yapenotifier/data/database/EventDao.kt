package com.example.yapenotifier.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    @Query("SELECT * FROM events ORDER BY timestamp DESC LIMIT :limit")
    fun recentEvents(limit: Int): Flow<List<CapturedEventEntity>>

    @Query("SELECT * FROM events WHERE timestamp BETWEEN :startMs AND :endMs ORDER BY timestamp DESC")
    fun eventsByDateRange(startMs: Long, endMs: Long): Flow<List<CapturedEventEntity>>

    @Insert
    suspend fun insert(event: CapturedEventEntity): Long

    @Query("SELECT COUNT(*) FROM events")
    suspend fun count(): Int

    @Query("DELETE FROM events WHERE id IN (SELECT id FROM events ORDER BY timestamp ASC LIMIT :count)")
    suspend fun deleteOldest(count: Int)
}
