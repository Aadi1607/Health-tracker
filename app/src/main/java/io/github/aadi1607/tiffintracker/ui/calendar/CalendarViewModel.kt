package io.github.aadi1607.tiffintracker.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.aadi1607.tiffintracker.TiffinApplication
import io.github.aadi1607.tiffintracker.data.DayLog
import io.github.aadi1607.tiffintracker.data.TiffinRepository
import io.github.aadi1607.tiffintracker.data.db.EntryStatus
import io.github.aadi1607.tiffintracker.data.db.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/** Status of one user on one calendar day. */
enum class DayStatus { TAKEN, SKIPPED, PENDING, FUTURE }

data class CalendarUiState(
    val month: YearMonth = YearMonth.now(),
    val users: List<User> = emptyList(),
    /** epochDay -> one status per user, in user order. */
    val statuses: Map<Long, List<DayStatus>> = emptyMap(),
)

data class DaySheetState(
    val date: LocalDate,
    val users: List<User> = emptyList(),
    val logs: Map<Long, DayLog> = emptyMap(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(private val repository: TiffinRepository) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())
    private val selectedDate = MutableStateFlow<LocalDate?>(null)

    val uiState: StateFlow<CalendarUiState> = month
        .flatMapLatest { m ->
            val start = m.atDay(1)
            val end = m.atEndOfMonth()
            combine(
                repository.users,
                repository.entriesForRange(start, end),
            ) { users, entries ->
                val today = LocalDate.now()
                val byDay = entries.groupBy { it.epochDay }
                val statuses = (1..m.lengthOfMonth()).associate { dayOfMonth ->
                    val date = m.atDay(dayOfMonth)
                    val day = date.toEpochDay()
                    val dayEntries = byDay[day].orEmpty()
                    day to users.map { user ->
                        val own = dayEntries.filter { it.userId == user.id }
                        when {
                            own.any { it.status == EntryStatus.TAKEN } -> DayStatus.TAKEN
                            own.isNotEmpty() -> DayStatus.SKIPPED
                            date.isAfter(today) -> DayStatus.FUTURE
                            else -> DayStatus.PENDING
                        }
                    }
                }
                CalendarUiState(month = m, users = users, statuses = statuses)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState())

    val daySheet: StateFlow<DaySheetState?> = selectedDate
        .flatMapLatest { date ->
            if (date == null) {
                flowOf(null)
            } else {
                combine(
                    repository.users,
                    repository.entriesForDay(date),
                ) { users, entries ->
                    DaySheetState(
                        date = date,
                        users = users,
                        logs = users.associate { user ->
                            user.id to DayLog.fromEntries(entries.filter { it.userId == user.id })
                        },
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun previousMonth() {
        month.value = month.value.minusMonths(1)
    }

    fun nextMonth() {
        month.value = month.value.plusMonths(1)
    }

    fun selectDay(date: LocalDate?) {
        selectedDate.value = date
    }

    fun updateLog(userId: Long, log: DayLog) {
        val date = selectedDate.value ?: return
        viewModelScope.launch { repository.saveDayLog(userId, date, log) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                CalendarViewModel(
                    TiffinApplication.from(checkNotNull(this[APPLICATION_KEY])).repository
                )
            }
        }
    }
}
