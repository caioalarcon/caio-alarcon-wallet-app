package com.example.carteiradepagamentos.ui.transfer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.carteiradepagamentos.domain.model.Contact
import com.example.carteiradepagamentos.domain.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TRANSFER_CHANNEL_ID = "transfers"

@HiltViewModel
class TransferViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val walletRepository: WalletRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransferUiState(isLoading = true))
    val uiState: StateFlow<TransferUiState> = _uiState.asStateFlow()

    init {
        createNotificationChannelIfNeeded()
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
                    sendSuccessNotification(contact, amountInCents)
                    _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Transferência realizada com sucesso",
                        balanceText = formatBalance(summary.balanceInCents),
                        amountInput = ""
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

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Transferências"
            val descriptionText = "Notificações de transferências realizadas"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(TRANSFER_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendSuccessNotification(contact: Contact, amountInCents: Long) {
        val reais = amountInCents / 100
        val cents = amountInCents % 100
        val amountText = "R$ %d,%02d".format(reais, cents)

        val builder = NotificationCompat.Builder(context, TRANSFER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Transferência realizada")
            .setContentText("Você enviou %s para %s".format(amountText, contact.name))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(context)) {
            notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), builder.build())
        }
    }
}
