package com.linghualive.flamekit.feature.stats.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linghualive.flamekit.core.database.dao.ReadingStatDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class StatsUiState(
    val totalDurationSeconds: Long = 0,
    val totalReadingDays: Int = 0,
    val consecutiveDays: Int = 0,
    val weekDurationSeconds: Long = 0,
    val calendarData: Map<String, Long> = emptyMap(),
    val weeklyData: List<Pair<String, Long>> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val readingStatDao: ReadingStatDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            val totalDuration = readingStatDao.getTotalDuration() ?: 0
            val totalDays = readingStatDao.getTotalReadingDays()

            val today = LocalDate.now()
            val threeMonthsAgo = today.minusMonths(3)
            val weekAgo = today.minusDays(6)

            // Load 3-month calendar data
            val calendarStats = mutableMapOf<String, Long>()
            readingStatDao.getStatsBetween(threeMonthsAgo.toString(), today.toString())
                .collect { stats ->
                    calendarStats.clear()
                    for (stat in stats) {
                        calendarStats[stat.date] =
                            (calendarStats[stat.date] ?: 0) + stat.durationSeconds
                    }

                    // Weekly data
                    val weeklyData = (0..6).map { offset ->
                        val date = weekAgo.plusDays(offset.toLong())
                        val dateStr = date.toString()
                        val dayLabel = "${date.monthValue}/${date.dayOfMonth}"
                        dayLabel to (calendarStats[dateStr] ?: 0)
                    }

                    // Weekly total
                    val weekDuration = weeklyData.sumOf { it.second }

                    // Consecutive days
                    var consecutiveDays = 0
                    var checkDate = today
                    while (true) {
                        val dateStr = checkDate.toString()
                        val duration = calendarStats[dateStr] ?: 0
                        if (duration > 0) {
                            consecutiveDays++
                            checkDate = checkDate.minusDays(1)
                        } else {
                            break
                        }
                    }

                    _uiState.update {
                        it.copy(
                            totalDurationSeconds = totalDuration,
                            totalReadingDays = totalDays,
                            consecutiveDays = consecutiveDays,
                            weekDurationSeconds = weekDuration,
                            calendarData = calendarStats.toMap(),
                            weeklyData = weeklyData,
                            isLoading = false,
                        )
                    }
                }
        }
    }
}
