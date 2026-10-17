package com.tiffintracker.ui.provider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiffintracker.data.db.Provider
import com.tiffintracker.data.repository.TiffinRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProviderUiState(
    val providers: List<Provider> = emptyList(),
    val editingProvider: Provider? = null,
    val isLoading: Boolean = true,
    val isSaved: Boolean = false
)

@HiltViewModel
class ProviderViewModel @Inject constructor(
    private val repository: TiffinRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProviderUiState())
    val uiState: StateFlow<ProviderUiState> = _uiState.asStateFlow()

    val providers: StateFlow<List<Provider>> = repository.getAllProviders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadProviders()
    }

    fun loadProviders() {
        viewModelScope.launch {
            repository.getAllProviders().collect { list ->
                _uiState.update { it.copy(providers = list, isLoading = false) }
            }
        }
    }

    fun addProvider(provider: Provider) {
        viewModelScope.launch {
            repository.insertProvider(provider)
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    fun updateProvider(provider: Provider) {
        viewModelScope.launch {
            repository.updateProvider(provider)
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    fun deleteProvider(provider: Provider) {
        viewModelScope.launch {
            repository.deleteProvider(provider)
        }
    }

    fun getProvider(id: Long) {
        viewModelScope.launch {
            repository.getProviderById(id).collect { provider ->
                _uiState.update { it.copy(editingProvider = provider) }
            }
        }
    }

    fun clearEditingProvider() {
        _uiState.update { it.copy(editingProvider = null) }
    }

    fun resetSavedState() {
        _uiState.update { it.copy(isSaved = false) }
    }
}
