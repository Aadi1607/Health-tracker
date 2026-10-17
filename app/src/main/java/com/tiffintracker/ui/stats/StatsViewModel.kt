package com.tiffintracker.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiffintracker.data.db.MealStatus
import com.tiffintracker.data.repository.TiffinRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class MonthlyStats(
    val label: String,
    val receivedCount: Int,
    val skippedCount: Int,
    val totalSpent: Double
)

data class StatsUiState(
    val monthlyData: List<MonthlyStats> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val repository: TiffinRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    init {
        loadStats()
    }

    fun loadStats() {
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            val currentMonth = YearMonth.now()
            val monthlyData = (5 downTo 0).map { offset ->
                val month = currentMonth.minusMonths(offset.toLong())
                computeMonthStats(month)
            }
            _uiState.update {
                it.copy(monthlyData = monthlyData, isLoading = false)
            }
        }
    }

    private suspend fun computeMonthStats(yearMonth: YearMonth): MonthlyStats {
        val startDate = yearMonth.atDay(1).format(dateFormatter)
        val endDate = yearMonth.atEndOfMonth().format(dateFormatter)

        val entries = repository.getEntriesInRange(startDate, endDate).first()
        val receivedCount = entries.count { it.status == MealStatus.RECEIVED.name }
        val skippedCount = entries.count { it.status == MealStatus.SKIPPED.name }

        val providers = repository.getActiveProviders().first()
        var totalSpent = 0.0
        for (provider in providers) {
            val bill = repository.calculateBill(provider.id, startDate, endDate)
            totalSpent += bill.totalAmount
        }

        val label = yearMonth.month.name.take(3).lowercase()
            .replaceFirstChar { it.uppercase() } + " " + yearMonth.year.toString().takeLast(2)

        return MonthlyStats(
            label = label,
            receivedCount = receivedCount,
            skippedCount = skippedCount,
            totalSpent = totalSpent
        )
    }
}
