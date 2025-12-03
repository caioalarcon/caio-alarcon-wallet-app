package com.example.carteiradepagamentos.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.carteiradepagamentos.data.toUserFriendlyMessage
import com.example.carteiradepagamentos.domain.repository.AuthRepository
import com.example.carteiradepagamentos.domain.repository.WalletRepository
import com.example.carteiradepagamentos.domain.format.toBRCurrency
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val walletRepository: WalletRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    fun load() {
        viewModelScope.launch {
            val session = authRepository.getCurrentSession()
            if (session == null) {
                _uiState.value = HomeUiState(
                    isLoggedOut = true,
                    errorMessage = "Sessão expirada"
                )
                return@launch
            }

            val hasExistingData = _uiState.value.contacts.isNotEmpty()

            _uiState.value = _uiState.value.copy(
                isLoading = !hasExistingData,
                errorMessage = null
            )

            try {
                val summary = walletRepository.getAccountSummary()
                val contacts = walletRepository.getContacts()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    userName = session.user.name,
                    userEmail = session.user.email,
                    balanceText = summary.balanceInCents.toBRCurrency(),
                    contacts = contacts
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.toUserFriendlyMessage()
                )
            }
        }
    }
}
