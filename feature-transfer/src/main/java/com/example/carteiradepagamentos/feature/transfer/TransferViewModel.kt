package com.example.carteiradepagamentos.feature.transfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.carteiradepagamentos.data.toUserFriendlyMessage
import com.example.carteiradepagamentos.domain.model.Contact
import com.example.carteiradepagamentos.domain.format.toBRCurrency
import com.example.carteiradepagamentos.domain.repository.AuthRepository
import com.example.carteiradepagamentos.domain.repository.WalletRepository
import com.example.carteiradepagamentos.domain.service.Notifier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class TransferViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
    private val notifier: Notifier,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransferUiState(isLoading = true))
    val uiState: StateFlow<TransferUiState> = _uiState.asStateFlow()
    private var currentUserId: String? = null

    init {
        reload()
    }

    fun reload() {
        currentUserId = null
        _uiState.value = TransferUiState(isLoading = true)
        viewModelScope.launch {
            try {
                val session = authRepository.getCurrentSession()
                    ?: error("Sessão expirada")
                currentUserId = session.user.id

                val summary = walletRepository.getAccountSummary()
                val contacts = walletRepository.getContacts()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    balanceInCents = summary.balanceInCents,
                    balanceText = summary.balanceInCents.toBRCurrency(),
                    contacts = contacts,
                    selectedContact = contacts.firstOrNull(),
                    amountInput = 0L.toBRCurrency(),
                    amountInCents = 0,
                    successDialogData = null,
                    errorDialogData = null
                )
            } catch (e: Exception) {
                _uiState.value = TransferUiState(
                    isLoading = false,
                    errorDialogData = TransferErrorData(e.resolveFriendlyMessage())
                )
            }
        }
    }

    fun onAmountChanged(value: String) {
        val amountDigits = value.filter(Char::isDigit)
        val amountInCents = amountDigits.toLongOrNull() ?: 0L
        _uiState.value = _uiState.value.copy(
            amountInput = amountInCents.toBRCurrency(),
            amountInCents = amountInCents,
            successDialogData = null
        )
    }

    fun onContactSelected(contact: Contact) {
        _uiState.value = _uiState.value.copy(
            selectedContact = contact,
            successDialogData = null
        )
    }

    fun onConfirmTransfer() {
        val state = _uiState.value
        val contact = state.selectedContact

        // valida: contato selecionado
        if (contact == null) {
            _uiState.value = state.copy(
                errorDialogData = TransferErrorData(
                    message = "Selecione um contato",
                    amountText = state.amountInCents.toBRCurrency()
                )
            )
            return
        }

        // valida: valor > 0
        val amountInCents = state.amountInCents.takeIf { it > 0 }
        if (amountInCents == null) {
            _uiState.value = state.copy(
                errorDialogData = TransferErrorData(
                    message = "Valor inválido",
                    contactName = contact.name,
                    contactAccount = contact.accountNumber
                )
            )
            return
        }

        // valida: payer ≠ payee
        val currentUserId = currentUserId
        if (currentUserId == null) {
            _uiState.value = state.copy(
                errorDialogData = TransferErrorData(
                    message = "Sessão expirada, faça login novamente"
                )
            )
            return
        }
        if (contact.ownerUserId == currentUserId) {
            _uiState.value = state.copy(
                errorDialogData = TransferErrorData(
                    message = ERROR_SELF_TRANSFER,
                    contactName = contact.name,
                    contactAccount = contact.accountNumber
                )
            )
            return
        }

        // valida: saldo suficiente (local)
        if (amountInCents > state.balanceInCents) {
            _uiState.value = state.copy(
                errorDialogData = TransferErrorData(
                    message = ERROR_INSUFFICIENT_BALANCE,
                    amountText = amountInCents.toBRCurrency(),
                    contactName = contact.name,
                    contactAccount = contact.accountNumber
                )
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                successDialogData = null
            )

            val result = walletRepository.transfer(contact.id, amountInCents)

            _uiState.value = result.fold(
                onSuccess = { summary ->
                    notifier.notifyTransferSuccess(contact, amountInCents)
                    _uiState.value.copy(
                        isLoading = false,
                        successDialogData = TransferSuccessData(
                            contactName = contact.name,
                            contactAccount = contact.accountNumber,
                            amountText = amountInCents.toBRCurrency(),
                        ),
                        balanceInCents = summary.balanceInCents,
                        balanceText = summary.balanceInCents.toBRCurrency(),
                        amountInput = 0L.toBRCurrency(),
                        amountInCents = 0,
                    )
                },
                onFailure = { error ->
                    val message = error.resolveFriendlyMessage()

                    _uiState.value.copy(
                        isLoading = false,
                        errorDialogData = TransferErrorData(
                            message = message,
                            amountText = amountInCents.toBRCurrency(),
                            contactName = contact.name,
                            contactAccount = contact.accountNumber,
                        ),
                        successDialogData = null
                    )
                }
            )
        }
    }

    fun clearSuccessDialog() {
        _uiState.value = _uiState.value.copy(successDialogData = null)
    }

    fun clearErrorDialog() {
        _uiState.value = _uiState.value.copy(errorDialogData = null)
    }

    private fun Throwable.resolveFriendlyMessage(): String {
        val serverMessage = (this as? HttpException)
            ?.response()
            ?.errorBody()
            ?.string()
            ?.substringAfter("\"message\":\"", missingDelimiterValue = "")
            ?.substringBefore("\"", missingDelimiterValue = "")
            ?.takeIf { it.isNotBlank() }

        return when {
            serverMessage?.contains(ERROR_INSUFFICIENT_BALANCE, ignoreCase = true) == true ->
                ERROR_INSUFFICIENT_BALANCE

            serverMessage?.contains("payer e payee", ignoreCase = true) == true ||
                serverMessage?.contains("payer equals payee", ignoreCase = true) == true ->
                ERROR_SELF_TRANSFER

            serverMessage?.contains("operation not allowed", ignoreCase = true) == true ->
                "Transferência bloqueada por política de segurança (valor R$ 403,00)"

            message?.contains("operation not allowed", ignoreCase = true) == true ->
                "Transferência bloqueada por política de segurança (valor R$ 403,00)"

            message?.contains("Sessão expirada", ignoreCase = true) == true ->
                "Sessão expirada, faça login novamente"

            message?.contains("payer e payee", ignoreCase = true) == true ||
                message?.contains("payer equals payee", ignoreCase = true) == true ->
                ERROR_SELF_TRANSFER

            message?.contains(ERROR_INSUFFICIENT_BALANCE, ignoreCase = true) == true ->
                ERROR_INSUFFICIENT_BALANCE

            serverMessage != null -> serverMessage

            else -> toUserFriendlyMessage()
        }
    }

    private companion object {
        const val ERROR_SELF_TRANSFER = "Você não pode transferir para você mesmo"
        const val ERROR_INSUFFICIENT_BALANCE = "Saldo insuficiente"
    }
}
