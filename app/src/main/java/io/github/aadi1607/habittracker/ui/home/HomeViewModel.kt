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
    val completed: Boolean,
    val isToday: Boolean,
)

data class HabitCardUi(
    val habit: Habit,
    val completedToday: Boolean,
    val streak: Int,
    val week: List<DayCell>,
)

data class HomeUiState(
    val habits: List<HabitCardUi> = emptyList(),
    val doneToday: Int = 0,
    val today: LocalDate = LocalDate.now(),
    val loaded: Boolean = false,
) {
    val total: Int get() = habits.size
    val progress: Float get() = if (total == 0) 0f else doneToday.toFloat() / total
}

class HomeViewModel(
    private val repository: HabitRepository,
    private val settings: SettingsRepository,
    private val reminderScheduler: ReminderScheduler,
    private val backupManager: BackupManager,
) : ViewModel() {

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
            val byHabit = completions.groupBy(
                keySelector = { it.habitId },
                valueTransform = { LocalDate.parse(it.date) },
            )
            val lastSeven = Streaks.lastDays(date, 7)
            val cards = habits.map { habit ->
                val done = byHabit[habit.id]?.toSet().orEmpty()
                HabitCardUi(
                    habit = habit,
                    completedToday = date in done,
                    streak = Streaks.currentStreak(done, date),
                    week = lastSeven.map { day ->
                        DayCell(date = day, completed = day in done, isToday = day == date)
                    },
                )
            }
            HomeUiState(
                habits = cards,
                doneToday = cards.count { it.completedToday },
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

    fun addHabit(name: String, emoji: String, color: Long) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { repository.addHabit(trimmed, emoji, color) }
    }

    fun deleteHabit(habitId: Long) {
        viewModelScope.launch { repository.deleteHabit(habitId) }
    }

    fun toggleCompletion(card: HabitCardUi) {
        viewModelScope.launch {
            repository.setCompleted(card.habit.id, uiState.value.today, !card.completedToday)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settings.setDynamicColor(enabled) }
    }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settings.setReminderEnabled(enabled)
            if (enabled) {
                val current = settings.reminder.first()
                reminderScheduler.schedule(current.hour, current.minute)
            } else {
                reminderScheduler.cancel()
            }
        }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            settings.setReminderTime(hour, minute)
            if (reminder.value?.enabled == true) {
                reminderScheduler.schedule(hour, minute)
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
                    repository = app.container.repository,
                    settings = app.container.settings,
                    reminderScheduler = app.container.reminderScheduler,
                    backupManager = app.container.backupManager,
                )
            }
        }
    }
}
