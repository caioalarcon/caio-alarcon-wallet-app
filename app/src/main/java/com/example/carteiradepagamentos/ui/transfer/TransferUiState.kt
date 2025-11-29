package com.example.carteiradepagamentos.ui.transfer

import com.example.carteiradepagamentos.domain.model.Contact

data class TransferUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val selectedContact: Contact? = null,
    val amountInput: String = "",
    val balanceText: String = "",
    val contacts: List<Contact> = emptyList()
)
