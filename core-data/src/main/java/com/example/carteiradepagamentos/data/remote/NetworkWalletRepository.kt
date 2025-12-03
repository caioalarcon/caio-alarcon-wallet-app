package com.example.carteiradepagamentos.data.remote

import com.example.carteiradepagamentos.domain.model.AccountSummary
import com.example.carteiradepagamentos.domain.model.Contact
import com.example.carteiradepagamentos.domain.repository.AuthRepository
import com.example.carteiradepagamentos.domain.repository.WalletRepository
import com.example.carteiradepagamentos.domain.service.AuthorizeService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkWalletRepository @Inject constructor(
    private val walletApi: WalletApi,
    private val authRepository: AuthRepository,
    private val authorizeService: AuthorizeService
) : WalletRepository {

    override suspend fun getAccountSummary(): AccountSummary {
        val session = authRepository.getCurrentSession()
            ?: throw IllegalStateException("Sessão expirada")

        val response = walletApi.getSummary(session.user.id)
        return AccountSummary(balanceInCents = response.balanceInCents)
    }

    override suspend fun getContacts(): List<Contact> {
        val session = authRepository.getCurrentSession()
            ?: throw IllegalStateException("Sessão expirada")

        val response = walletApi.getContacts(session.user.id)
        return response.map {
            Contact(
                id = it.id,
                name = it.name,
                accountNumber = it.accountNumber
            )
        }
    }

    override suspend fun transfer(
        toContactId: String,
        amountInCents: Long
    ): Result<AccountSummary> {
        val session = authRepository.getCurrentSession()
            ?: return Result.failure(IllegalStateException("Sessão expirada"))

        val authResult = authorizeService.authorizeTransfer(amountInCents)
        if (authResult.isFailure) {
            return Result.failure(authResult.exceptionOrNull()!!)
        }
        if (!authResult.getOrThrow()) {
            return Result.failure(IllegalStateException("operation not allowed"))
        }

        return try {
            val response = walletApi.transfer(
                TransferRequest(
                    userId = session.user.id,
                    toContactId = toContactId,
                    amountInCents = amountInCents
                )
            )
            Result.success(AccountSummary(balanceInCents = response.balanceInCents))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
