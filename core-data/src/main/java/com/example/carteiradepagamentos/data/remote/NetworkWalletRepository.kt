package com.example.carteiradepagamentos.data.remote

import com.example.carteiradepagamentos.domain.model.AccountSummary
import com.example.carteiradepagamentos.domain.model.Contact
import com.example.carteiradepagamentos.domain.repository.AppPreferencesRepository
import com.example.carteiradepagamentos.domain.repository.AuthRepository
import com.example.carteiradepagamentos.domain.repository.WalletRepository
import com.example.carteiradepagamentos.domain.service.AuthorizeService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkWalletRepository @Inject constructor(
    private val retrofitFactory: RetrofitFactory,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val authRepository: AuthRepository,
    private val authorizeService: AuthorizeService
) : WalletRepository {

    override suspend fun getAccountSummary(): AccountSummary {
        val session = authRepository.getCurrentSession()
            ?: throw IllegalStateException("Sessão expirada")

        val api = walletApi()
        val response = api.getSummary(session.user.id)
        return AccountSummary(balanceInCents = response.balanceInCents)
    }

    override suspend fun getContacts(): List<Contact> {
        val session = authRepository.getCurrentSession()
            ?: throw IllegalStateException("Sessão expirada")

        val response = walletApi().getContacts(session.user.id)
        val contacts = response.map {
            Contact(
                id = it.id,
                ownerUserId = it.ownerUserId,
                name = it.name,
                accountNumber = it.accountNumber
            )
        }
        val selfContact = Contact(
            id = "self-${session.user.id}",
            ownerUserId = session.user.id,
            name = session.user.name,
            accountNumber = "0000-0"
        )
        return listOf(selfContact) + contacts
    }

    override suspend fun transfer(
        toContactId: String,
        amountInCents: Long
    ): Result<AccountSummary> {
        val session = authRepository.getCurrentSession()
            ?: return Result.failure(IllegalStateException("Sessão expirada"))

        return runCatching {
            val summary = walletApi().getSummary(session.user.id)
            if (amountInCents > summary.balanceInCents) {
                throw IllegalStateException("Saldo insuficiente")
            }

            val authResult = authorizeService.authorizeTransfer(amountInCents)
            if (authResult.isFailure) {
                throw authResult.exceptionOrNull()!!
            }
            if (!authResult.getOrThrow()) {
                throw IllegalStateException("operation not allowed")
            }

            val response = walletApi().transfer(
                TransferRequest(
                    userId = session.user.id,
                    toContactId = toContactId,
                    amountInCents = amountInCents
                )
            )
            AccountSummary(balanceInCents = response.balanceInCents)
        }
    }

    private suspend fun walletApi(): WalletApi {
        val config = appPreferencesRepository.getNetworkConfig()
        return retrofitFactory.create(config.baseUrl).create(WalletApi::class.java)
    }
}
