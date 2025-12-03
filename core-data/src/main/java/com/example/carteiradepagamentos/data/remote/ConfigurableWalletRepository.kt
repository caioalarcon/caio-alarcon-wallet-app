package com.example.carteiradepagamentos.data.remote

import com.example.carteiradepagamentos.data.memory.InMemoryWalletRepository
import com.example.carteiradepagamentos.domain.repository.AppPreferencesRepository
import com.example.carteiradepagamentos.domain.repository.WalletRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigurableWalletRepository @Inject constructor(
    private val network: NetworkWalletRepository,
    private val local: InMemoryWalletRepository,
    private val appPreferencesRepository: AppPreferencesRepository
) : WalletRepository {

    override suspend fun getAccountSummary() =
        delegate().getAccountSummary()

    override suspend fun getContacts() =
        delegate().getContacts()

    override suspend fun transfer(toContactId: String, amountInCents: Long) =
        delegate().transfer(toContactId, amountInCents)

    private suspend fun delegate(): WalletRepository {
        val config = appPreferencesRepository.getNetworkConfig()
        return if (config.useRemoteServer) network else local
    }
}
