package com.example.yapenotifier.data.repository

import com.example.yapenotifier.data.database.EventDao
import com.example.yapenotifier.data.database.toDomain
import com.example.yapenotifier.data.database.toEntity
import com.example.yapenotifier.domain.model.CapturedEvent
import com.example.yapenotifier.domain.repository.EventRepository
import com.example.yapenotifier.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepositoryImpl @Inject constructor(
    private val eventDao: EventDao
) : EventRepository {

    override fun recentEventsFlow(limit: Int): Flow<List<CapturedEvent>> {
        return eventDao.recentEvents(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun eventsByDateFlow(startMs: Long, endMs: Long): Flow<List<CapturedEvent>> {
        return eventDao.eventsByDateRange(startMs, endMs).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun appendEvent(event: CapturedEvent) {
        eventDao.insert(event.toEntity())
        val count = eventDao.count()
        if (count > Constants.MAX_EVENTS) {
            eventDao.deleteOldest(count - Constants.MAX_EVENTS)
        }
    }
}
