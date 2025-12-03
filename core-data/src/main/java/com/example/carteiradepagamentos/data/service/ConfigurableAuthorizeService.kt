package com.example.carteiradepagamentos.data.service

import com.example.carteiradepagamentos.data.remote.NetworkAuthorizeService
import com.example.carteiradepagamentos.domain.repository.AppPreferencesRepository
import com.example.carteiradepagamentos.domain.service.AuthorizeService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigurableAuthorizeService @Inject constructor(
    private val network: NetworkAuthorizeService,
    private val local: LocalAuthorizeService,
    private val appPreferencesRepository: AppPreferencesRepository
) : AuthorizeService {

    override suspend fun authorizeTransfer(amountInCents: Long): Result<Boolean> {
        val config = appPreferencesRepository.getNetworkConfig()
        val delegate = if (config.useRemoteServer) network else local
        return delegate.authorizeTransfer(amountInCents)
    }
}
