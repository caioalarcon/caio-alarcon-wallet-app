package com.example.carteiradepagamentos.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.carteiradepagamentos.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

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
}

data class SettingsUiState(
    val isLoggingOut: Boolean = false,
    val errorMessage: String? = null
)
