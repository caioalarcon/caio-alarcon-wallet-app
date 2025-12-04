package com.example.carteiradepagamentos.domain.service

fun interface AuthorizeService {
    suspend fun authorizeTransfer(amountInCents: Long): Result<Boolean>
}
