package io.github.aadi1607.habittracker.ui.home

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.aadi1607.habittracker.HabitApplication
import io.github.aadi1607.habittracker.R
import io.github.aadi1607.habittracker.data.HabitRepository
import io.github.aadi1607.habittracker.data.ReminderSettings
import io.github.aadi1607.habittracker.data.SettingsRepository
import io.github.aadi1607.habittracker.data.backup.BackupManager
import io.github.aadi1607.habittracker.data.db.Habit
import io.github.aadi1607.habittracker.domain.Streaks
import io.github.aadi1607.habittracker.reminder.ReminderScheduler
import io.github.aadi1607.habittracker.widget.HabitWidget
import androidx.glance.appwidget.updateAll
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A single day cell in the 7-day chain. */
data class DayCell(
    val date: LocalDate,
    /** The daily target was reached on this day. */
    val completed: Boolean,
    /** Some progress was logged but the target was not reached. */
    val partial: Boolean,
    val isToday: Boolean,
)

data class HabitCardUi(
    val habit: Habit,
    /** Logs in the current period: today for daily habits, this week for weekly ones. */
    val periodCount: Int,
    /** Streak in the habit's own unit — days for daily, weeks for weekly. */
    val streak: Int,
    val week: List<DayCell>,
) {
    val completedNow: Boolean get() = periodCount >= habit.dailyTarget
}

data class HomeUiState(
    val habits: List<HabitCardUi> = emptyList(),
    val doneToday: Int = 0,
    val today: LocalDate = LocalDate.now(),
    val loaded: Boolean = false,
) {
    val total: Int get() = habits.size
    val allDone: Boolean get() = total > 0 && doneToday == total

    /** Header bar progress with partial credit for multi-target habits. */
    val progress: Float
        get() = if (total == 0) {
            0f
        } else {
            habits.map {
                it.periodCount.coerceAtMost(it.habit.dailyTarget).toFloat() / it.habit.dailyTarget
            }.sum() / total
        }
}

class HomeViewModel(
    private val application: HabitApplication,
    private val repository: HabitRepository,
    private val settings: SettingsRepository,
    private val reminderScheduler: ReminderScheduler,
    private val backupManager: BackupManager,
) : ViewModel() {

    private suspend fun refreshWidget() {
        runCatching { HabitWidget().updateAll(application) }
    }

    /** Re-emits the current date just after every midnight so streaks roll over. */
    private val today = flow {
        while (true) {
            emit(LocalDate.now())
            val now = LocalDateTime.now()
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
            delay(Duration.between(now, nextMidnight).toMillis() + 1_000)
        }
    }

    val uiState: StateFlow<HomeUiState> =
        combine(repository.observeHabits(), repository.observeCompletions(), today) { habits, completions, date ->
            val countsByHabit: Map<Long, Map<LocalDate, Int>> = completions
                .groupBy { it.habitId }
                .mapValues { (_, list) -> list.associate { LocalDate.parse(it.date) to it.count } }
            val lastSeven = Streaks.lastDays(date, 7)
            val cards = habits.map { habit ->
                val counts = countsByHabit[habit.id].orEmpty()
                if (habit.isWeekly) {
                    // Weekly goal: progress is this ISO week's total; every logged
                    // day lights up in the chain.
                    val weekSum = Streaks.weeklySums(counts)[Streaks.weekStart(date)] ?: 0
                    HabitCardUi(
                        habit = habit,
                        periodCount = weekSum,
                        streak = Streaks.currentWeeklyStreak(counts, habit.dailyTarget, date),
                        week = lastSeven.map { day ->
                            DayCell(
                                date = day,
                                completed = (counts[day] ?: 0) > 0,
                                partial = false,
                                isToday = day == date,
                            )
                        },
                    )
                } else {
                    val done = counts.filterValues { it >= habit.dailyTarget }.keys
                    HabitCardUi(
                        habit = habit,
                        periodCount = counts[date] ?: 0,
                        streak = Streaks.currentStreak(done, date),
                        week = lastSeven.map { day ->
                            val count = counts[day] ?: 0
                            DayCell(
                                date = day,
                                completed = day in done,
                                partial = count in 1 until habit.dailyTarget,
                                isToday = day == date,
                            )
                        },
                    )
                }
            }
            HomeUiState(
                habits = cards,
                doneToday = cards.count { it.completedNow },
                today = date,
                loaded = true,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    val reminder: StateFlow<ReminderSettings?> =
        settings.reminder.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val dynamicColor: StateFlow<Boolean> =
        settings.dynamicColor.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _userMessage = MutableStateFlow<Int?>(null)
    val userMessage: StateFlow<Int?> = _userMessage.asStateFlow()

    fun addHabit(name: String, emoji: String, color: Long, dailyTarget: Int, goalPeriod: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.addHabit(trimmed, emoji, color, dailyTarget, goalPeriod)
            refreshWidget()
        }
    }

    fun updateHabit(
        habitId: Long,
        name: String,
        emoji: String,
        color: Long,
        dailyTarget: Int,
        goalPeriod: String,
    ) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.updateHabit(habitId, trimmed, emoji, color, dailyTarget, goalPeriod)
            refreshWidget()
        }
    }

    /** Moves a habit one position up or down in the list. */
    fun moveHabit(card: HabitCardUi, up: Boolean) {
        val list = uiState.value.habits
        val index = list.indexOfFirst { it.habit.id == card.habit.id }
        if (index == -1) return
        val neighbor = list.getOrNull(index + if (up) -1 else 1) ?: return
        viewModelScope.launch {
            repository.swapSortOrders(card.habit, neighbor.habit)
            refreshWidget()
        }
    }

    fun deleteHabit(habitId: Long) {
        viewModelScope.launch {
            repository.deleteHabit(habitId)
            refreshWidget()
        }
    }

    /**
     * One tap on the check button: logs one more completion, or — once the
     * target is reached — undoes the last log so mistakes are reversible.
     */
    fun tap(card: HabitCardUi) {
        viewModelScope.launch {
            if (card.completedNow) {
                repository.decrement(card.habit.id, uiState.value.today)
            } else {
                repository.increment(card.habit.id, uiState.value.today)
            }
            refreshWidget()
        }
    }

    /** True when Android is currently blocking exact alarms for this app. */
    fun needsExactAlarmPermission(): Boolean = !reminderScheduler.canScheduleExact()

    fun logout() {
        viewModelScope.launch { settings.setLoggedIn(false) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settings.setDynamicColor(enabled) }
    }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settings.setReminderEnabled(enabled)
            if (enabled) {
                val current = settings.reminder.first()
                reminderScheduler.scheduleDaily(current.hour, current.minute)
            } else {
                reminderScheduler.cancelDaily()
            }
        }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            settings.setReminderTime(hour, minute)
            if (settings.reminder.first().enabled) {
                reminderScheduler.scheduleDaily(hour, minute)
            }
        }
    }

    fun setNudgesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settings.setNudgesEnabled(enabled)
            if (enabled) {
                reminderScheduler.scheduleNextNudge(settings.reminder.first().nudgeIntervalHours)
            } else {
                reminderScheduler.cancelNudges()
            }
        }
    }

    fun setNudgeInterval(hours: Int) {
        viewModelScope.launch {
            settings.setNudgeIntervalHours(hours)
            if (settings.reminder.first().nudgesEnabled) {
                reminderScheduler.scheduleNextNudge(hours)
            }
        }
    }

    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            val result = backupManager.exportTo(uri)
            showMessage(if (result.isSuccess) R.string.export_success else R.string.export_failure)
        }
    }

    fun importFrom(uri: Uri) {
        viewModelScope.launch {
            val result = backupManager.importFrom(uri)
            showMessage(if (result.isSuccess) R.string.import_success else R.string.import_failure)
            refreshWidget()
        }
    }

    private fun showMessage(@StringRes message: Int) {
        _userMessage.value = message
    }

    fun messageShown() {
        _userMessage.value = null
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as HabitApplication
                HomeViewModel(
                    application = app,
                    repository = app.container.repository,
                    settings = app.container.settings,
                    reminderScheduler = app.container.reminderScheduler,
                    backupManager = app.container.backupManager,
                )
            }
        }
    }
}
