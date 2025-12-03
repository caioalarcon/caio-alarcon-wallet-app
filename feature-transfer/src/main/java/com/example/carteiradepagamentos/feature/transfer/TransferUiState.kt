package com.example.carteiradepagamentos.feature.transfer

import com.example.carteiradepagamentos.domain.model.Contact

data class TransferUiState(
    val isLoading: Boolean = false,
    val selectedContact: Contact? = null,
    val amountInput: String = "R$ 0,00",
    val amountInCents: Long = 0,
    val balanceText: String = "",
    val contacts: List<Contact> = emptyList(),
    val successDialogData: TransferSuccessData? = null,
    val errorDialogData: TransferErrorData? = null
)

data class TransferSuccessData(
    val contactName: String,
    val contactAccount: String,
    val amountText: String,
)

data class TransferErrorData(
    val message: String,
)
