package com.example.yapenotifier.presentation.dashboard

import androidx.lifecycle.ViewModel
import com.example.yapenotifier.domain.model.CapturedEvent
import com.example.yapenotifier.domain.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import java.util.Calendar
import javax.inject.Inject

sealed class DateFilter {
    data object Recent : DateFilter()
    data object Today : DateFilter()
    data object Yesterday : DateFilter()
    data object ThisWeek : DateFilter()
    data class Custom(val startMs: Long, val endMs: Long) : DateFilter()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _dateFilter = MutableStateFlow<DateFilter>(DateFilter.Recent)
    val dateFilter: StateFlow<DateFilter> = _dateFilter.asStateFlow()

    val events: Flow<List<CapturedEvent>> = _dateFilter.flatMapLatest { filter ->
        when (filter) {
            is DateFilter.Recent -> eventRepository.recentEventsFlow(10)
            is DateFilter.Today -> {
                val (start, end) = dayRange(0)
                eventRepository.eventsByDateFlow(start, end)
            }
            is DateFilter.Yesterday -> {
                val (start, end) = dayRange(-1)
                eventRepository.eventsByDateFlow(start, end)
            }
            is DateFilter.ThisWeek -> {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                }
                val start = cal.timeInMillis
                val end = System.currentTimeMillis()
                eventRepository.eventsByDateFlow(start, end)
            }
            is DateFilter.Custom -> {
                eventRepository.eventsByDateFlow(filter.startMs, filter.endMs)
            }
        }
    }

    fun setFilter(filter: DateFilter) {
        _dateFilter.value = filter
    }

    private fun dayRange(offsetDays: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, offsetDays)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val end = cal.timeInMillis - 1
        return start to end
    }
}
