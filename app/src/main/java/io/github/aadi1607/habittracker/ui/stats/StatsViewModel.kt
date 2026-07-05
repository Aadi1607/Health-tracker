package io.github.aadi1607.habittracker.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.aadi1607.habittracker.HabitApplication
import io.github.aadi1607.habittracker.data.HabitRepository
import io.github.aadi1607.habittracker.data.db.Habit
import io.github.aadi1607.habittracker.domain.Streaks
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HabitStats(
    val habit: Habit,
    val currentStreak: Int,
    val bestStreak: Int,
    /** Fraction of days completed since the habit was created, 0..1. */
    val completionRate: Float,
)

data class StatsUiState(
    val habitStats: List<HabitStats> = emptyList(),
    val month: YearMonth = YearMonth.now(),
    /** Fraction of habits completed for each day of [month], 0..1. */
    val heatmap: Map<LocalDate, Float> = emptyMap(),
    val totalCompletions: Int = 0,
)

class StatsViewModel(
    repository: HabitRepository,
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())

    val uiState: StateFlow<StatsUiState> =
        combine(
            repository.observeHabits(),
            repository.observeCompletions(),
            month,
        ) { habits, completions, selectedMonth ->
            val today = LocalDate.now()
            val zone = ZoneId.systemDefault()
            val byHabit = completions.groupBy(
                keySelector = { it.habitId },
                valueTransform = { LocalDate.parse(it.date) },
            )

            val stats = habits.map { habit ->
                val done = byHabit[habit.id]?.toSet().orEmpty()
                val createdOn = Instant.ofEpochMilli(habit.createdAt).atZone(zone).toLocalDate()
                HabitStats(
                    habit = habit,
                    currentStreak = Streaks.currentStreak(done, today),
                    bestStreak = Streaks.bestStreak(done),
                    completionRate = Streaks.completionRate(done, createdOn, today),
                )
            }

            val habitCount = habits.size
            val completionsPerDay = completions.groupingBy { it.date }.eachCount()
            val heatmap = buildMap {
                if (habitCount > 0) {
                    for (dayOfMonth in 1..selectedMonth.lengthOfMonth()) {
                        val date = selectedMonth.atDay(dayOfMonth)
                        val count = completionsPerDay[date.toString()] ?: 0
                        put(date, (count.toFloat() / habitCount).coerceIn(0f, 1f))
                    }
                }
            }

            StatsUiState(
                habitStats = stats,
                month = selectedMonth,
                heatmap = heatmap,
                totalCompletions = completions.size,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())

    fun previousMonth() {
        month.value = month.value.minusMonths(1)
    }

    fun nextMonth() {
        month.value = month.value.plusMonths(1)
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as HabitApplication
                StatsViewModel(repository = app.container.repository)
            }
        }
    }
}
