package com.example.carteiradepagamentos.feature.transfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.carteiradepagamentos.domain.model.Contact
import com.example.carteiradepagamentos.domain.repository.WalletRepository
import com.example.carteiradepagamentos.domain.service.Notifier
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.NumberFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class TransferViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
    private val notifier: Notifier
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransferUiState(isLoading = true))
    val uiState: StateFlow<TransferUiState> = _uiState.asStateFlow()

    init {
        reload()
    }

    fun reload() {
        _uiState.value = TransferUiState(isLoading = true)
        viewModelScope.launch {
            val summary = walletRepository.getAccountSummary()
            val contacts = walletRepository.getContacts()

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                balanceText = formatCurrency(summary.balanceInCents),
                contacts = contacts,
                selectedContact = contacts.firstOrNull(),
                amountInput = formatCurrency(0),
                amountInCents = 0,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun onAmountChanged(value: String) {
        val amountDigits = value.filter(Char::isDigit)
        val amountInCents = amountDigits.toLongOrNull() ?: 0
        _uiState.value = _uiState.value.copy(
            amountInput = formatCurrency(amountInCents),
            amountInCents = amountInCents,
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

        val amountInCents = state.amountInCents.takeIf { it > 0 }
        if (amountInCents == null) {
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
                        balanceText = formatCurrency(summary.balanceInCents),
                        amountInput = formatCurrency(0),
                        amountInCents = 0,
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

    private fun formatCurrency(amountInCents: Long): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        return formatter.format(amountInCents / 100.0).replace('\u00A0', ' ')
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
}
