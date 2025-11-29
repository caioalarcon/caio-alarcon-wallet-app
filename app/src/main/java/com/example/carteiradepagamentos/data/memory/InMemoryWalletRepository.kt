package com.example.carteiradepagamentos.data.memory

import com.example.carteiradepagamentos.domain.model.AccountSummary
import com.example.carteiradepagamentos.domain.model.Contact
import com.example.carteiradepagamentos.domain.repository.WalletRepository
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryWalletRepository @Inject constructor() : WalletRepository {

    // Saldo inicial: R$ 1.000,00
    private var balanceInCents: Long = 100_000L

    private val contacts = listOf(
        Contact(id = "c1", name = "Alice", accountNumber = "0001-1"),
        Contact(id = "c2", name = "Bob", accountNumber = "0001-2"),
        Contact(id = "c3", name = "Carol", accountNumber = "0001-3"),
    )

    override suspend fun getAccountSummary(): AccountSummary {
        delay(300)
        return AccountSummary(balanceInCents)
    }

    override suspend fun getContacts(): List<Contact> {
        delay(300)
        return contacts
    }

    override suspend fun transfer(
        toContactId: String,
        amountInCents: Long
    ): Result<AccountSummary> {
        delay(500)

        if (contacts.none { it.id == toContactId }) {
            return Result.failure(IllegalArgumentException("Contato inválido"))
        }

        if (amountInCents <= 0) {
            return Result.failure(IllegalArgumentException("Valor inválido"))
        }

        // Regra especial do desafio: exatamente R$ 403,00 é bloqueado
        if (amountInCents == 40_300L) {
            return Result.failure(IllegalStateException("operation not allowed"))
        }

        if (amountInCents > balanceInCents) {
            return Result.failure(IllegalStateException("Saldo insuficiente"))
        }

        balanceInCents -= amountInCents
        return Result.success(AccountSummary(balanceInCents))
    }
}
