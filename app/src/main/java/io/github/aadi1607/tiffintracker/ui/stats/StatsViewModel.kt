package io.github.aadi1607.tiffintracker.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.aadi1607.tiffintracker.TiffinApplication
import io.github.aadi1607.tiffintracker.data.SettingsRepository
import io.github.aadi1607.tiffintracker.data.TiffinRepository
import io.github.aadi1607.tiffintracker.data.db.EntryStatus
import io.github.aadi1607.tiffintracker.data.db.User
import io.github.aadi1607.tiffintracker.domain.BillCalculator
import io.github.aadi1607.tiffintracker.domain.BillingCycle
import io.github.aadi1607.tiffintracker.domain.Streaks
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth

data class MonthBars(
    val month: YearMonth,
    /** One value per user, in user order. */
    val counts: List<Int>,
)

data class StatsUiState(
    val currencySymbol: String = "₹",
    val users: List<User> = emptyList(),
    val cycleTiffins: Int = 0,
    val cycleDuePaise: Long = 0,
    val cyclePaidPaise: Long = 0,
    val cycleSkippedDays: Int = 0,
    val monthly: List<MonthBars> = emptyList(),
    /** userId -> current streak in days. */
    val streaks: Map<Long, Int> = emptyMap(),
)

class StatsViewModel(
    repository: TiffinRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<StatsUiState> = combine(
        settingsRepository.settings,
        repository.users,
        repository.allEntries(),
        repository.payments,
    ) { settings, users, entries, payments ->
        val today = LocalDate.now()
        val cycle = BillingCycle.cycleFor(today, settings.cycleStartDay)
        val inCycle = entries.filter { it.epochDay in cycle.startEpochDay..cycle.endEpochDay }

        val bills = users.map { user ->
            BillCalculator.userBill(user.id, user.pricePaise, cycle, entries, payments)
        }

        val skippedDays = inCycle
            .filter { it.status == EntryStatus.SKIPPED }
            .map { it.userId to it.epochDay }
            .distinct()
            .count { (userId, day) ->
                // A user-day counts as skipped only if nothing was taken that day.
                inCycle.none {
                    it.userId == userId && it.epochDay == day && it.status == EntryStatus.TAKEN
                }
            }

        val months = (5 downTo 0).map { YearMonth.from(today).minusMonths(it.toLong()) }
        val monthly = months.map { month ->
            val startDay = month.atDay(1).toEpochDay()
            val endDay = month.atEndOfMonth().toEpochDay()
            MonthBars(
                month = month,
                counts = users.map { user ->
                    BillCalculator.tiffinCount(
                        entries.filter { it.userId == user.id && it.epochDay in startDay..endDay }
                    )
                },
            )
        }

        val streaks = users.associate { user ->
            val takenDays = entries
                .filter { it.userId == user.id && it.status == EntryStatus.TAKEN }
                .map { it.epochDay }
                .toSet()
            user.id to Streaks.currentStreak(takenDays, today)
        }

        StatsUiState(
            currencySymbol = settings.currencySymbol,
            users = users,
            cycleTiffins = bills.sumOf { it.tiffinCount },
            cycleDuePaise = bills.sumOf { it.totalDuePaise },
            cyclePaidPaise = bills.sumOf { it.paidPaise },
            cycleSkippedDays = skippedDays,
            monthly = monthly,
            streaks = streaks,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = TiffinApplication.from(checkNotNull(this[APPLICATION_KEY]))
                StatsViewModel(app.repository, app.settingsRepository)
            }
        }
    }
}
