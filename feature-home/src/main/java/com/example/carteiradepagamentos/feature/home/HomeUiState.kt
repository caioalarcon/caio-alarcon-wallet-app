package com.example.carteiradepagamentos.feature.home

import com.example.carteiradepagamentos.domain.model.Contact

data class HomeUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val userName: String = "",
    val userEmail: String = "",
    val balanceText: String = "",
    val contacts: List<Contact> = emptyList(),
    val isLoggedOut: Boolean = false
)
