package com.example.carteiradepagamentos.domain.service

interface AuthorizeService {
    suspend fun authorizeTransfer(amountInCents: Long): Result<Boolean>
}
