package com.example.carteiradepagamentos.data.remote

import com.example.carteiradepagamentos.domain.repository.AppPreferencesRepository
import com.example.carteiradepagamentos.domain.service.AuthRemoteDataSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigurableAuthRemoteDataSource @Inject constructor(
    private val network: NetworkAuthRemoteDataSource,
    private val local: FakeAuthRemoteDataSource,
    private val appPreferencesRepository: AppPreferencesRepository
) : AuthRemoteDataSource {

    override suspend fun login(email: String, password: String) =
        if (appPreferencesRepository.getNetworkConfig().useRemoteServer) {
            network.login(email, password)
        } else {
            local.login(email, password)
        }
}
