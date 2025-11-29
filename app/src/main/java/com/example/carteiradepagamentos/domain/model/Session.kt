package com.example.carteiradepagamentos.domain.model

data class Session(
    val token: String,
    val user: User
)
