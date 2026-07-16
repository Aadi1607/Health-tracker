package io.github.aadi1607.tiffintracker.ui.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.aadi1607.tiffintracker.TiffinApplication
import io.github.aadi1607.tiffintracker.data.SettingsRepository
import io.github.aadi1607.tiffintracker.data.TiffinRepository
import io.github.aadi1607.tiffintracker.data.db.Payment
import io.github.aadi1607.tiffintracker.data.db.User
import io.github.aadi1607.tiffintracker.domain.BillCalculator
import io.github.aadi1607.tiffintracker.domain.BillingCycle
import io.github.aadi1607.tiffintracker.domain.UserBill
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class BillingRow(val user: User, val bill: UserBill)

data class BillingUiState(
    val cycle: BillingCycle? = null,
    val currencySymbol: String = "₹",
    val rows: List<BillingRow> = emptyList(),
    val cyclePayments: List<Payment> = emptyList(),
    val userNames: Map<Long, String> = emptyMap(),
)

class BillingViewModel(
    private val repository: TiffinRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    /** Any date inside the cycle being viewed; today = current cycle. */
    private val cycleAnchor = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<BillingUiState> = combine(
        cycleAnchor,
        settingsRepository.settings,
        repository.users,
        repository.allEntries(),
        repository.payments,
    ) { anchor, settings, users, entries, payments ->
        val cycle = BillingCycle.cycleFor(anchor, settings.cycleStartDay)
        BillingUiState(
            cycle = cycle,
            currencySymbol = settings.currencySymbol,
            rows = users.map { user ->
                BillingRow(
                    user = user,
                    bill = BillCalculator.userBill(
                        user.id, user.pricePaise, cycle, entries, payments,
                    ),
                )
            },
            cyclePayments = payments.filter { it.cycleStartEpochDay == cycle.startEpochDay },
            userNames = users.associate { it.id to it.name },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BillingUiState())

    fun previousCycle() {
        uiState.value.cycle?.let { cycleAnchor.value = it.start.minusDays(1) }
    }

    fun nextCycle() {
        uiState.value.cycle?.let { cycleAnchor.value = it.end.plusDays(1) }
    }

    fun addPayment(userId: Long, amountPaise: Long, date: LocalDate) {
        val cycle = uiState.value.cycle ?: return
        viewModelScope.launch {
            repository.addPayment(
                Payment(
                    userId = userId,
                    cycleStartEpochDay = cycle.startEpochDay,
                    amountPaise = amountPaise,
                    epochDay = date.toEpochDay(),
                )
            )
        }
    }

    fun deletePayment(payment: Payment) {
        viewModelScope.launch { repository.deletePayment(payment) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = TiffinApplication.from(checkNotNull(this[APPLICATION_KEY]))
                BillingViewModel(app.repository, app.settingsRepository)
            }
        }
    }
}
