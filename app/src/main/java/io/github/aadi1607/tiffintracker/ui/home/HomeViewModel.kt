package io.github.aadi1607.tiffintracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.aadi1607.tiffintracker.TiffinApplication
import io.github.aadi1607.tiffintracker.data.DayLog
import io.github.aadi1607.tiffintracker.data.TiffinRepository
import io.github.aadi1607.tiffintracker.data.db.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HomeUiState(
    val date: LocalDate = LocalDate.now(),
    val users: List<User> = emptyList(),
    val logs: Map<Long, DayLog> = emptyMap(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(private val repository: TiffinRepository) : ViewModel() {

    private val selectedDate = MutableStateFlow(LocalDate.now())
    val date: StateFlow<LocalDate> = selectedDate.asStateFlow()

    val uiState: StateFlow<HomeUiState> = selectedDate
        .flatMapLatest { date ->
            combine(repository.users, repository.entriesForDay(date)) { users, entries ->
                HomeUiState(
                    date = date,
                    users = users,
                    logs = users.associate { user ->
                        user.id to DayLog.fromEntries(entries.filter { it.userId == user.id })
                    },
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
    }

    fun updateLog(userId: Long, log: DayLog) {
        val date = selectedDate.value
        viewModelScope.launch { repository.saveDayLog(userId, date, log) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HomeViewModel(TiffinApplication.from(checkNotNull(this[APPLICATION_KEY])).repository)
            }
        }
    }
}
