package com.tiffintracker.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiffintracker.data.db.MealEntry
import com.tiffintracker.data.db.MealStatus
import com.tiffintracker.data.db.MealType
import com.tiffintracker.data.db.Provider
import com.tiffintracker.data.repository.TiffinRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class TodayUiState(
    val todayDate: LocalDate = LocalDate.now(),
    val providers: List<Provider> = emptyList(),
    val todayEntries: Map<Long, List<MealEntry>> = emptyMap(),
    val isLoading: Boolean = true
)

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val repository: TiffinRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    init {
        loadTodayData()
    }

    fun loadTodayData() {
        val today = LocalDate.now()
        val todayStr = today.format(dateFormatter)
        _uiState.update { it.copy(todayDate = today, isLoading = true) }

        viewModelScope.launch {
            combine(
                repository.getActiveProviders(),
                repository.getEntriesForDate(todayStr)
            ) { providers, entries ->
                val grouped = entries.groupBy { it.providerId }
                TodayUiState(
                    todayDate = today,
                    providers = providers,
                    todayEntries = grouped,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun toggleMealStatus(providerId: Long, mealType: MealType, newStatus: MealStatus) {
        viewModelScope.launch {
            val dateStr = _uiState.value.todayDate.format(dateFormatter)
            val existing = repository.getEntryForProviderDateMeal(
                providerId, dateStr, mealType.name
            )
            if (existing != null) {
                repository.updateEntry(existing.copy(status = newStatus.name))
            } else {
                repository.insertEntry(
                    MealEntry(
                        providerId = providerId,
                        date = dateStr,
                        mealType = mealType.name,
                        status = newStatus.name,
                        quantity = 1
                    )
                )
            }
        }
    }

    fun updateQuantity(providerId: Long, mealType: MealType, quantity: Int) {
        viewModelScope.launch {
            val dateStr = _uiState.value.todayDate.format(dateFormatter)
            val existing = repository.getEntryForProviderDateMeal(
                providerId, dateStr, mealType.name
            )
            if (existing != null) {
                repository.updateEntry(existing.copy(quantity = quantity))
            } else {
                repository.insertEntry(
                    MealEntry(
                        providerId = providerId,
                        date = dateStr,
                        mealType = mealType.name,
                        status = MealStatus.RECEIVED.name,
                        quantity = quantity
                    )
                )
            }
        }
    }

    fun updateNote(providerId: Long, mealType: MealType, note: String) {
        viewModelScope.launch {
            val dateStr = _uiState.value.todayDate.format(dateFormatter)
            val existing = repository.getEntryForProviderDateMeal(
                providerId, dateStr, mealType.name
            )
            if (existing != null) {
                repository.updateEntry(existing.copy(note = note))
            }
        }
    }

    fun markHoliday() {
        viewModelScope.launch {
            val dateStr = _uiState.value.todayDate.format(dateFormatter)
            val providerIds = _uiState.value.providers.map { it.id }
            repository.markHoliday(dateStr, providerIds)
        }
    }

    fun markAwayRange(startDate: LocalDate, endDate: LocalDate) {
        viewModelScope.launch {
            val startStr = startDate.format(dateFormatter)
            val endStr = endDate.format(dateFormatter)
            val providerIds = _uiState.value.providers.map { it.id }
            repository.markAwayRange(startStr, endStr, providerIds)
        }
    }
}
