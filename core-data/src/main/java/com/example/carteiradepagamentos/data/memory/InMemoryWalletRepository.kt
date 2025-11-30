package com.example.carteiradepagamentos.data.memory

import com.example.carteiradepagamentos.domain.model.AccountSummary
import com.example.carteiradepagamentos.domain.model.Contact
import com.example.carteiradepagamentos.domain.repository.AuthRepository
import com.example.carteiradepagamentos.domain.repository.WalletRepository
import com.example.carteiradepagamentos.domain.service.AuthorizeService
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryWalletRepository @Inject constructor(
    private val authorizeService: AuthorizeService,
    private val authRepository: AuthRepository
) : WalletRepository {

    private data class AccountState(
        val ownerUserId: String,
        val contact: Contact,
        var balanceInCents: Long
    )

    private val accounts = mutableListOf(
        AccountState(
            ownerUserId = "1",
            contact = Contact(id = "self", name = "Você", accountNumber = "0000-0"),
            balanceInCents = 100_000L
        ),
        AccountState(
            ownerUserId = "2",
            contact = Contact(id = "c1", name = "Alice", accountNumber = "0001-1"),
            balanceInCents = 50_000L
        ),
        AccountState(
            ownerUserId = "3",
            contact = Contact(id = "c2", name = "Bob", accountNumber = "0001-2"),
            balanceInCents = 75_000L
        ),
        AccountState(
            ownerUserId = "4",
            contact = Contact(id = "c3", name = "Carol", accountNumber = "0001-3"),
            balanceInCents = 200_000L
        ),
    )

    override suspend fun getAccountSummary(): AccountSummary {
        delay(300)
        return AccountSummary(currentPayerAccount().balanceInCents)
    }

    override suspend fun getContacts(): List<Contact> {
        delay(300)
        val payerUserId = currentUserId()
        return accounts.filter { it.ownerUserId != payerUserId }.map { it.contact }
    }

    override suspend fun transfer(
        toContactId: String,
        amountInCents: Long
    ): Result<AccountSummary> {
        delay(500)

        val payerAccount = currentPayerAccount()
        val payeeAccount = accounts.firstOrNull { it.contact.id == toContactId }
            ?: return Result.failure(IllegalArgumentException("Contato inválido"))

        if (payeeAccount.ownerUserId == payerAccount.ownerUserId) {
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

        if (amountInCents > payerAccount.balanceInCents) {
            return Result.failure(IllegalStateException("Saldo insuficiente"))
        }

        payerAccount.balanceInCents -= amountInCents
        payeeAccount.balanceInCents += amountInCents

        return Result.success(AccountSummary(payerAccount.balanceInCents))
    }

    private fun currentUserId(): String {
        return authRepository.getCurrentSession()?.user?.id
            ?: error("Sessão inexistente ao consultar carteira")
    }

    private fun currentPayerAccount(): AccountState {
        val payerId = currentUserId()
        return accounts.firstOrNull { it.ownerUserId == payerId }
            ?: error("Conta inexistente para usuário atual")
    }
}
