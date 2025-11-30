package com.example.carteiradepagamentos.ui.transfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.carteiradepagamentos.domain.model.Contact
import com.example.carteiradepagamentos.domain.repository.WalletRepository
import com.example.carteiradepagamentos.domain.service.Notifier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransferViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
    private val notifier: Notifier
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransferUiState(isLoading = true))
    val uiState: StateFlow<TransferUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val summary = walletRepository.getAccountSummary()
            val contacts = walletRepository.getContacts()

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                balanceText = formatBalance(summary.balanceInCents),
                contacts = contacts,
                selectedContact = contacts.firstOrNull()
            )
        }
    }

    fun onAmountChanged(value: String) {
        _uiState.value = _uiState.value.copy(
            amountInput = value,
            errorMessage = null,
            successMessage = null
        )
    }

    fun onContactSelected(contact: Contact) {
        _uiState.value = _uiState.value.copy(
            selectedContact = contact,
            errorMessage = null,
            successMessage = null
        )
    }

    fun onConfirmTransfer() {
        val state = _uiState.value
        val contact = state.selectedContact

        if (contact == null) {
            _uiState.value = state.copy(errorMessage = "Selecione um contato")
            return
        }

        val amountInCents = state.amountInput.toLongOrNull()
        if (amountInCents == null || amountInCents <= 0) {
            _uiState.value = state.copy(errorMessage = "Valor inválido")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            val result = walletRepository.transfer(contact.id, amountInCents)

            _uiState.value = result.fold(
                onSuccess = { summary ->
                    notifier.notifyTransferSuccess(contact, amountInCents)
                    _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Transferência realizada com sucesso",
                        balanceText = formatBalance(summary.balanceInCents),
                        amountInput = "",
                    )
                },
                onFailure = { error ->
                    val message = when {
                        error.message?.contains("operation not allowed", ignoreCase = true) == true ->
                            "Transferência bloqueada por política de segurança (valor R$ 403,00)"
                        error.message?.contains("Saldo insuficiente", ignoreCase = true) == true ->
                            "Saldo insuficiente"
                        else ->
                            error.message ?: "Erro na transferência"
                    }

                    _uiState.value.copy(
                        isLoading = false,
                        errorMessage = message
                    )
                }
            )
        }
    }

    private fun formatBalance(balanceInCents: Long): String {
        val reais = balanceInCents / 100
        val cents = balanceInCents % 100
        return "R$ %d,%02d".format(reais, cents)
    }
}
