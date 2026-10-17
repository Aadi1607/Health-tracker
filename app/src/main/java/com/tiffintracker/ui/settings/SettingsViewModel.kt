package com.tiffintracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiffintracker.data.repository.BackupRepository
import com.tiffintracker.data.repository.SettingsRepository
import com.tiffintracker.data.repository.TiffinRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val reminderEnabled: Boolean = true,
    val reminderHour: Int = 21,
    val reminderMinute: Int = 0,
    val themeMode: String = "system",
    val currency: String = "₹",
    val exportedJson: String? = null,
    val importSuccess: Boolean? = null,
    val isClearing: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val backupRepository: BackupRepository,
    private val tiffinRepository: TiffinRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.settings.collect { appSettings ->
                _uiState.update {
                    it.copy(
                        reminderEnabled = appSettings.reminderEnabled,
                        reminderHour = appSettings.reminderHour,
                        reminderMinute = appSettings.reminderMinute,
                        themeMode = appSettings.themeMode,
                        currency = appSettings.currency
                    )
                }
            }
        }
    }

    fun updateReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setReminderEnabled(enabled)
        }
    }

    fun updateReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsRepository.setReminderTime(hour, minute)
        }
    }

    fun updateThemeMode(mode: String) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun exportBackup() {
        viewModelScope.launch {
            val json = backupRepository.exportToJson()
            _uiState.update { it.copy(exportedJson = json) }
        }
    }

    fun clearExportedJson() {
        _uiState.update { it.copy(exportedJson = null) }
    }

    fun importBackup(json: String) {
        viewModelScope.launch {
            val success = backupRepository.importFromJson(json)
            _uiState.update { it.copy(importSuccess = success) }
        }
    }

    fun clearImportResult() {
        _uiState.update { it.copy(importSuccess = null) }
    }

    fun clearAllData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isClearing = true) }
            tiffinRepository.clearAllData()
            _uiState.update { it.copy(isClearing = false) }
        }
    }
}
