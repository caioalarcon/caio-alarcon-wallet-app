package com.example.carteiradepagamentos.data.remote

import com.example.carteiradepagamentos.domain.repository.AppPreferencesRepository
import com.example.carteiradepagamentos.domain.service.AuthorizeService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkAuthorizeService @Inject constructor(
    private val retrofitFactory: RetrofitFactory,
    private val appPreferencesRepository: AppPreferencesRepository
) : AuthorizeService {

    override suspend fun authorizeTransfer(amountInCents: Long): Result<Boolean> {
        return try {
            val config = appPreferencesRepository.getNetworkConfig()
            val api = retrofitFactory.create(config.baseUrl).create(AuthorizeApi::class.java)
            val response = api.authorize(AuthorizeRequest(value = amountInCents))

            Result.success(response.authorized)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
