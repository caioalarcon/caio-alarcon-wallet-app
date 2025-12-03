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
        val id: String,
        val ownerUserId: String,
        val ownerName: String,
        val accountNumber: String,
        var balanceInCents: Long
    )

    private val accounts = mutableListOf(
        AccountState(
            id = "acc1",
            ownerUserId = "1",
            ownerName = "Usuário Exemplo",
            accountNumber = "0001-1",
            balanceInCents = 100_000
        ),
        AccountState(
            id = "acc2",
            ownerUserId = "2",
            ownerName = "Alice",
            accountNumber = "0001-2",
            balanceInCents = 50_000
        ),
        AccountState(
            id = "acc3",
            ownerUserId = "3",
            ownerName = "Bob",
            accountNumber = "0001-3",
            balanceInCents = 75_000
        ),
        AccountState(
            id = "acc4",
            ownerUserId = "4",
            ownerName = "Carol",
            accountNumber = "0001-4",
            balanceInCents = 25_000
        ),
    )

    private suspend fun currentUserAccount(): AccountState {
        val session = authRepository.getCurrentSession()
            ?: error("Sessão inexistente ao consultar carteira")
        return accounts.first { it.ownerUserId == session.user.id }
    }

    override suspend fun getAccountSummary(): AccountSummary {
        delay(300)
        val account = currentUserAccount()
        return AccountSummary(account.balanceInCents)
    }

    override suspend fun getContacts(): List<Contact> {
        delay(300)
        val currentUserAccount = currentUserAccount()
        return accounts
            .filter { it.ownerUserId != currentUserAccount.ownerUserId }
            .map { account ->
                Contact(
                    id = account.id,
                    name = account.ownerName,
                    accountNumber = account.accountNumber
                )
            }
    }

    override suspend fun transfer(
        toContactId: String,
        amountInCents: Long
    ): Result<AccountSummary> {
        delay(500)

        if (amountInCents <= 0) {
            return Result.failure(IllegalArgumentException("Valor inválido"))
        }

        val payerAccount = currentUserAccount()
        val payeeAccount = accounts.firstOrNull { it.id == toContactId }
            ?: return Result.failure(IllegalArgumentException("Contato inválido"))

        if (payeeAccount.ownerUserId == payerAccount.ownerUserId) {
            return Result.failure(IllegalStateException("Payer e payee não podem ser iguais"))
        }

        if (amountInCents > payerAccount.balanceInCents) {
            return Result.failure(IllegalStateException("Saldo insuficiente"))
        }

        val authorization = authorizeService.authorizeTransfer(amountInCents)
        val isAllowed = authorization.getOrElse { return Result.failure(it) }
        if (!isAllowed) {
            return Result.failure(IllegalStateException("operation not allowed"))
        }

        payerAccount.balanceInCents -= amountInCents
        payeeAccount.balanceInCents += amountInCents

        return Result.success(AccountSummary(payerAccount.balanceInCents))
    }
}
