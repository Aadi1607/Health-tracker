package com.tiffintracker.ui.bills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiffintracker.data.db.Payment
import com.tiffintracker.data.db.Provider
import com.tiffintracker.data.repository.BillSummary
import com.tiffintracker.data.repository.TiffinRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class ProviderBill(
    val provider: Provider,
    val billSummary: BillSummary
)

data class BillsUiState(
    val yearMonth: YearMonth = YearMonth.now(),
    val providerBills: List<ProviderBill> = emptyList(),
    val selectedProviderBill: ProviderBill? = null,
    val payments: List<Payment> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class BillsViewModel @Inject constructor(
    private val repository: TiffinRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BillsUiState())
    val uiState: StateFlow<BillsUiState> = _uiState.asStateFlow()

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    init {
        loadAllProviderBills(YearMonth.now())
    }

    fun loadAllProviderBills(yearMonth: YearMonth) {
        _uiState.update { it.copy(yearMonth = yearMonth, isLoading = true) }

        val startDate = yearMonth.atDay(1).format(dateFormatter)
        val endDate = yearMonth.atEndOfMonth().format(dateFormatter)

        viewModelScope.launch {
            val providers = repository.getActiveProviders().first()
            val bills = providers.map { provider ->
                val summary = repository.calculateBill(provider.id, startDate, endDate)
                ProviderBill(provider, summary)
            }
            _uiState.update {
                it.copy(
                    providerBills = bills,
                    isLoading = false
                )
            }
        }
    }

    fun loadBillForProvider(providerId: Long, yearMonth: YearMonth) {
        val startDate = yearMonth.atDay(1).format(dateFormatter)
        val endDate = yearMonth.atEndOfMonth().format(dateFormatter)

        viewModelScope.launch {
            val bill = _uiState.value.providerBills.find { it.provider.id == providerId }
            if (bill != null) {
                _uiState.update { it.copy(selectedProviderBill = bill) }
            }

            repository.getPaymentsForProviderInRange(providerId, startDate, endDate)
                .collect { payments ->
                    _uiState.update { it.copy(payments = payments) }
                }
        }
    }

    fun addPayment(payment: Payment) {
        viewModelScope.launch {
            repository.insertPayment(payment)
            loadAllProviderBills(_uiState.value.yearMonth)
        }
    }

    fun deletePayment(payment: Payment) {
        viewModelScope.launch {
            repository.deletePayment(payment)
            loadAllProviderBills(_uiState.value.yearMonth)
        }
    }

    fun generateBillText(providerId: Long, yearMonth: YearMonth): String {
        val bill = _uiState.value.providerBills.find { it.provider.id == providerId }
            ?: return ""
        val provider = bill.provider
        val summary = bill.billSummary
        val monthLabel = yearMonth.month.name.lowercase()
            .replaceFirstChar { it.uppercase() } + " " + yearMonth.year

        return buildString {
            appendLine("🍱 Tiffin Bill — $monthLabel")
            appendLine("Provider: ${provider.name}")
            appendLine("─────────────────────")
            if (provider.offersBreakfast && summary.breakfastCount > 0) {
                appendLine("Breakfast: ${summary.breakfastCount} × ₹${provider.breakfastPrice.toLong()} = ₹${summary.breakfastAmount.toLong()}")
            }
            if (provider.offersLunch && summary.lunchCount > 0) {
                appendLine("Lunch: ${summary.lunchCount} × ₹${provider.lunchPrice.toLong()} = ₹${summary.lunchAmount.toLong()}")
            }
            if (provider.offersDinner && summary.dinnerCount > 0) {
                appendLine("Dinner: ${summary.dinnerCount} × ₹${provider.dinnerPrice.toLong()} = ₹${summary.dinnerAmount.toLong()}")
            }
            appendLine("─────────────────────")
            appendLine("Total: ₹${summary.totalAmount.toLong()}")
            appendLine("Paid: ₹${summary.totalPaid.toLong()}")
            val balance = summary.balanceDue
            if (balance > 0) {
                appendLine("Due: ₹${balance.toLong()}")
            } else if (balance < 0) {
                appendLine("Advance: ₹${(-balance).toLong()}")
            } else {
                appendLine("Settled ✅")
            }
        }
    }

    fun navigateMonth(forward: Boolean) {
        val current = _uiState.value.yearMonth
        val next = if (forward) current.plusMonths(1) else current.minusMonths(1)
        loadAllProviderBills(next)
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedProviderBill = null, payments = emptyList()) }
    }
}
