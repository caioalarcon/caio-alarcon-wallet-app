package com.example.carteiradepagamentos

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
class AppViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUiState(isReady = false))
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val session = authRepository.getCurrentSession()
            _uiState.value = AppUiState(
                currentScreen = if (session != null) Screen.Home else Screen.Login,
                isReady = true
            )
        }
    }

    fun navigateTo(screen: Screen) {
        _uiState.value = _uiState.value.copy(currentScreen = screen)
    }

    fun onLoginSuccess() {
        _uiState.value = _uiState.value.copy(currentScreen = Screen.Home)
    }

    fun onLoggedOut() {
        _uiState.value = _uiState.value.copy(currentScreen = Screen.Login)
    }
}

data class AppUiState(
    val currentScreen: Screen = Screen.Login,
    val isReady: Boolean = false
)
