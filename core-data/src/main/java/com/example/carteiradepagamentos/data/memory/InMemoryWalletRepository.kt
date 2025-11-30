package com.example.carteiradepagamentos.data.memory

import com.example.carteiradepagamentos.domain.model.AccountSummary
import com.example.carteiradepagamentos.domain.model.Contact
import com.example.carteiradepagamentos.domain.repository.WalletRepository
import com.example.carteiradepagamentos.domain.service.AuthorizeService
import com.example.carteiradepagamentos.domain.storage.BalanceStorage
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryWalletRepository @Inject constructor(
    private val authorizeService: AuthorizeService,
    private val balanceStorage: BalanceStorage
) : WalletRepository {

    private val contacts = listOf(
        Contact(id = "c1", name = "Alice", accountNumber = "0001-1"),
        Contact(id = "c2", name = "Bob", accountNumber = "0001-2"),
        Contact(id = "c3", name = "Carol", accountNumber = "0001-3"),
    )

    override suspend fun getAccountSummary(): AccountSummary {
        delay(300)
        return AccountSummary(balanceStorage.loadBalance())
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

        val authorization = authorizeService.authorizeTransfer(amountInCents)
        val isAllowed = authorization.getOrElse { return Result.failure(it) }
        if (!isAllowed) {
            return Result.failure(IllegalStateException("operation not allowed"))
        }

        val currentBalance = balanceStorage.loadBalance()
        if (amountInCents > currentBalance) {
            return Result.failure(IllegalStateException("Saldo insuficiente"))
        }

        val newBalance = currentBalance - amountInCents
        balanceStorage.saveBalance(newBalance)
        return Result.success(AccountSummary(newBalance))
    }
}
