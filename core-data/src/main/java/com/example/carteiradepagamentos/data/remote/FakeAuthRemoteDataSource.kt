package com.example.carteiradepagamentos.data.remote

import com.example.carteiradepagamentos.domain.model.Session
import com.example.carteiradepagamentos.domain.model.User
import com.example.carteiradepagamentos.domain.service.AuthRemoteDataSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeAuthRemoteDataSource @Inject constructor() : AuthRemoteDataSource {

    override suspend fun login(email: String, password: String): Result<Session> {
        return if (email == "user@example.com" && password == "123456") {
            val user = User(
                id = "1",
                name = "Usuário Exemplo",
                email = email
            )
            Result.success(
                Session(
                    token = "fake-token-123",
                    user = user
                )
            )
        } else {
            Result.failure(IllegalArgumentException("Credenciais inválidas"))
        }
    }
}
