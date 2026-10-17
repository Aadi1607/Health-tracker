package com.tiffintracker.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiffintracker.data.db.MealEntry
import com.tiffintracker.data.db.MealStatus
import com.tiffintracker.data.db.Provider
import com.tiffintracker.data.repository.TiffinRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

enum class DayStatus {
    HAS_RECEIVED, ALL_SKIPPED, MIXED, NONE
}

data class CalendarUiState(
    val yearMonth: YearMonth = YearMonth.now(),
    val dayStatusMap: Map<LocalDate, DayStatus> = emptyMap(),
    val selectedDate: LocalDate? = null,
    val selectedDateEntries: List<MealEntry> = emptyList(),
    val providers: List<Provider> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: TiffinRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    init {
        loadMonth(YearMonth.now())
        viewModelScope.launch {
            repository.getActiveProviders().collect { providers ->
                _uiState.update { it.copy(providers = providers) }
            }
        }
    }

    fun loadMonth(yearMonth: YearMonth) {
        _uiState.update { it.copy(yearMonth = yearMonth, isLoading = true) }

        val startDate = yearMonth.atDay(1).format(dateFormatter)
        val endDate = yearMonth.atEndOfMonth().format(dateFormatter)

        viewModelScope.launch {
            repository.getEntriesInRange(startDate, endDate).collect { entries ->
                val dayMap = entries.groupBy { LocalDate.parse(it.date, dateFormatter) }
                    .mapValues { (_, dayEntries) -> classifyDay(dayEntries) }

                _uiState.update {
                    it.copy(
                        dayStatusMap = dayMap,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun selectDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
        val dateStr = date.format(dateFormatter)

        viewModelScope.launch {
            repository.getEntriesForDate(dateStr).collect { entries ->
                _uiState.update { it.copy(selectedDateEntries = entries) }
            }
        }
    }

    fun navigateMonth(forward: Boolean) {
        val current = _uiState.value.yearMonth
        val next = if (forward) current.plusMonths(1) else current.minusMonths(1)
        loadMonth(next)
    }

    fun updateEntry(entry: MealEntry) {
        viewModelScope.launch {
            repository.updateEntry(entry)
        }
    }

    private fun classifyDay(entries: List<MealEntry>): DayStatus {
        if (entries.isEmpty()) return DayStatus.NONE
        val hasReceived = entries.any { it.status == MealStatus.RECEIVED.name }
        val hasSkipped = entries.any { it.status == MealStatus.SKIPPED.name }
        return when {
            hasReceived && hasSkipped -> DayStatus.MIXED
            hasReceived -> DayStatus.HAS_RECEIVED
            hasSkipped -> DayStatus.ALL_SKIPPED
            else -> DayStatus.NONE
        }
    }
}
