package com.example.carteiradepagamentos.feature.login

import com.example.carteiradepagamentos.domain.model.Session

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val loginSucceeded: Boolean = false,
    val session: Session? = null
)
