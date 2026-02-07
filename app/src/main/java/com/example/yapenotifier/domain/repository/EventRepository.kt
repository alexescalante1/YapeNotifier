package com.example.yapenotifier.domain.repository

import com.example.yapenotifier.domain.model.CapturedEvent
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    fun recentEventsFlow(limit: Int = 10): Flow<List<CapturedEvent>>
    fun eventsByDateFlow(startMs: Long, endMs: Long): Flow<List<CapturedEvent>>
    suspend fun appendEvent(event: CapturedEvent)
}
