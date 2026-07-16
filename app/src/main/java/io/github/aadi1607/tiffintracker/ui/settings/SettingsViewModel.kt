package io.github.aadi1607.tiffintracker.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.aadi1607.tiffintracker.R
import io.github.aadi1607.tiffintracker.TiffinApplication
import io.github.aadi1607.tiffintracker.data.AppSettings
import io.github.aadi1607.tiffintracker.data.ThemeMode
import io.github.aadi1607.tiffintracker.data.db.User
import io.github.aadi1607.tiffintracker.data.export.CsvExporter
import io.github.aadi1607.tiffintracker.data.export.PdfExporter
import io.github.aadi1607.tiffintracker.data.export.shareFile
import io.github.aadi1607.tiffintracker.domain.BillingCycle
import io.github.aadi1607.tiffintracker.notifications.ReminderScheduler
import io.github.aadi1607.tiffintracker.ui.theme.UserColors
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class SettingsUiState(
    val users: List<User> = emptyList(),
    val settings: AppSettings = AppSettings(),
)

class SettingsViewModel(private val app: Application) : ViewModel() {

    private val tiffinApp = app as TiffinApplication
    private val repository = tiffinApp.repository
    private val settingsRepository = tiffinApp.settingsRepository
    private val backupManager = tiffinApp.backupManager

    val uiState: StateFlow<SettingsUiState> = combine(
        repository.users,
        settingsRepository.settings,
    ) { users, settings ->
        SettingsUiState(users, settings)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    /** One-shot toast messages (string resource ids). */
    private val _messages = MutableSharedFlow<Int>(extraBufferCapacity = 4)
    val messages: SharedFlow<Int> = _messages

    // Users -------------------------------------------------------------

    fun saveUser(user: User) {
        viewModelScope.launch {
            if (user.id == 0L) {
                repository.addUser(user.copy(sortOrder = uiState.value.users.size))
            } else {
                repository.updateUser(user)
            }
        }
    }

    fun deleteUser(userId: Long) {
        viewModelScope.launch { repository.deleteUser(userId) }
    }

    // Preferences ---------------------------------------------------------

    fun setCycleStartDay(day: Int) {
        viewModelScope.launch { settingsRepository.setCycleStartDay(day) }
    }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setReminderEnabled(enabled)
            if (enabled) {
                val s = settingsRepository.current()
                ReminderScheduler.scheduleDailyReminder(app, s.reminderHour, s.reminderMinute, replace = true)
            }
        }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsRepository.setReminderTime(hour, minute)
            ReminderScheduler.scheduleDailyReminder(app, hour, minute, replace = true)
        }
    }

    fun setPaymentReminderDays(days: Int) {
        viewModelScope.launch { settingsRepository.setPaymentReminderDays(days) }
    }

    fun setCurrencySymbol(symbol: String) {
        viewModelScope.launch { settingsRepository.setCurrencySymbol(symbol) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    // Export & backup ------------------------------------------------------

    fun exportCsv() {
        viewModelScope.launch {
            val settings = settingsRepository.current()
            val cycle = BillingCycle.cycleFor(LocalDate.now(), settings.cycleStartDay)
            val file = CsvExporter.export(
                app,
                cycle,
                repository.getUsers(),
                repository.getEntriesForRange(cycle.start, cycle.end),
                settings.currencySymbol,
            )
            shareFile(app, file, "text/csv", app.getString(R.string.export_share_csv))
        }
    }

    fun exportPdf() {
        viewModelScope.launch {
            val settings = settingsRepository.current()
            val cycle = BillingCycle.cycleFor(LocalDate.now(), settings.cycleStartDay)
            val file = PdfExporter.export(
                app,
                cycle,
                repository.getUsers(),
                repository.getEntriesForRange(LocalDate.ofEpochDay(0), cycle.end),
                repository.getPayments(),
                settings.currencySymbol,
            )
            shareFile(app, file, "application/pdf", app.getString(R.string.export_share_pdf))
        }
    }

    fun backupTo(uri: Uri) {
        viewModelScope.launch {
            runCatching { backupManager.exportTo(uri) }
                .onSuccess { _messages.tryEmit(R.string.settings_backup_done) }
                .onFailure { _messages.tryEmit(R.string.settings_restore_failed) }
        }
    }

    fun restoreFrom(uri: Uri) {
        viewModelScope.launch {
            runCatching { backupManager.importFrom(uri) }
                .onSuccess { _messages.tryEmit(R.string.settings_restore_done) }
                .onFailure { _messages.tryEmit(R.string.settings_restore_failed) }
        }
    }

    // Danger zone ----------------------------------------------------------

    fun resetCurrentCycle() {
        viewModelScope.launch {
            val settings = settingsRepository.current()
            val cycle = BillingCycle.cycleFor(LocalDate.now(), settings.cycleStartDay)
            repository.deleteEntriesInRange(cycle.start, cycle.end)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            repository.seedDefaultUsersIfEmpty(
                names = listOf("Me", "Rahul"),
                colors = UserColors.palette,
                pricePaise = 6000,
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SettingsViewModel(checkNotNull(this[APPLICATION_KEY]))
            }
        }
    }
}
