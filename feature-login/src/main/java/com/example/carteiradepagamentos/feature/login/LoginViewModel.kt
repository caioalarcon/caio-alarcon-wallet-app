package com.example.carteiradepagamentos.feature.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.carteiradepagamentos.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChanged(value: String) {
        _uiState.value = _uiState.value.copy(
            email = value,
            errorMessage = null,
            loginSucceeded = false
        )
    }

    fun onPasswordChanged(value: String) {
        _uiState.value = _uiState.value.copy(
            password = value,
            errorMessage = null,
            loginSucceeded = false
        )
    }

    fun onLoginClicked() {
        val current = _uiState.value
        if (current.email.isBlank() || current.password.isBlank()) {
            _uiState.value = current.copy(
                errorMessage = "Preencha email e senha"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                loginSucceeded = false
            )

            val result = authRepository.login(current.email, current.password)

            _uiState.value = result.fold(
                onSuccess = {
                    _uiState.value.copy(
                        isLoading = false,
                        loginSucceeded = true,
                        errorMessage = null
                    )
                },
                onFailure = { error ->
                    _uiState.value.copy(
                        isLoading = false,
                        loginSucceeded = false,
                        errorMessage = error.message ?: "Erro ao fazer login"
                    )
                }
            )
        }
    }
}
