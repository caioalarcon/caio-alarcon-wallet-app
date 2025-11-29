package com.example.carteiradepagamentos.domain.repository

import com.example.carteiradepagamentos.domain.model.AccountSummary
import com.example.carteiradepagamentos.domain.model.Contact

interface WalletRepository {
    suspend fun getAccountSummary(): AccountSummary
    suspend fun getContacts(): List<Contact>

    /**
     * amountInCents: valor em centavos (ex.: 1000 = R$ 10,00)
     * Retorna o novo saldo em caso de sucesso.
     */
    suspend fun transfer(
        toContactId: String,
        amountInCents: Long
    ): Result<AccountSummary>
}
