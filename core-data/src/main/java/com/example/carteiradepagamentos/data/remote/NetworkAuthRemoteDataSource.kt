package com.example.carteiradepagamentos.data.remote

import com.example.carteiradepagamentos.domain.model.Session
import com.example.carteiradepagamentos.domain.model.User
import com.example.carteiradepagamentos.domain.repository.AppPreferencesRepository
import com.example.carteiradepagamentos.domain.service.AuthRemoteDataSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkAuthRemoteDataSource @Inject constructor(
    private val retrofitFactory: RetrofitFactory,
    private val appPreferencesRepository: AppPreferencesRepository
) : AuthRemoteDataSource {

    override suspend fun login(email: String, password: String): Result<Session> {
        return try {
            val config = appPreferencesRepository.getNetworkConfig()
            val api = retrofitFactory.create(config.baseUrl).create(AuthApi::class.java)
            val response = api.login(LoginRequest(email, password))
            Result.success(
                Session(
                    token = response.token,
                    user = User(
                        id = response.user.id,
                        name = response.user.name,
                        email = response.user.email
                    )
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
