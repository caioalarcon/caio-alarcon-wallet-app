package com.example.carteiradepagamentos.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.carteiradepagamentos.domain.model.NetworkConfig
import com.example.carteiradepagamentos.domain.repository.AppPreferencesRepository
import com.example.carteiradepagamentos.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val appPreferencesRepository: AppPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            appPreferencesRepository.observeNetworkConfig().collectLatest { config ->
                _uiState.value = _uiState.value.copy(
                    networkEnabled = config.useRemoteServer,
                    networkBaseUrl = config.baseUrl,
                    isSavingNetworkConfig = false
                )
            }
        }
    }

    fun onLogoutClicked(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoggingOut = true, errorMessage = null)

            runCatching { authRepository.logout() }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isLoggingOut = false)
                    onLoggedOut()
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isLoggingOut = false,
                        errorMessage = "Não foi possível sair. Tente novamente."
                    )
                }
        }
    }

    fun onNetworkBaseUrlChanged(value: String) {
        _uiState.value = _uiState.value.copy(networkBaseUrl = value)
    }

    fun onToggleNetwork(enabled: Boolean) {
        saveNetworkConfig(enabled = enabled, baseUrl = _uiState.value.networkBaseUrl)
    }

    fun onSaveNetworkConfig() {
        saveNetworkConfig(
            enabled = _uiState.value.networkEnabled,
            baseUrl = _uiState.value.networkBaseUrl
        )
    }

    private fun saveNetworkConfig(enabled: Boolean, baseUrl: String) {
        viewModelScope.launch {
            val sanitizedUrl = if (baseUrl.isBlank()) DEFAULT_BASE_URL else baseUrl
            _uiState.value = _uiState.value.copy(
                isSavingNetworkConfig = true,
                errorMessage = null,
                networkBaseUrl = sanitizedUrl,
                networkEnabled = enabled
            )

            val config = NetworkConfig(
                useRemoteServer = enabled,
                baseUrl = sanitizedUrl
            )

            runCatching { appPreferencesRepository.setNetworkConfig(config) }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isSavingNetworkConfig = false,
                        errorMessage = "Não foi possível salvar as preferências de rede"
                    )
                }
        }
    }

    companion object {
        const val DEFAULT_BASE_URL = "http://192.168.1.110:3000/"
    }
}

data class SettingsUiState(
    val isLoggingOut: Boolean = false,
    val errorMessage: String? = null,
    val networkEnabled: Boolean = false,
    val networkBaseUrl: String = SettingsViewModel.DEFAULT_BASE_URL,
    val isSavingNetworkConfig: Boolean = false
)
