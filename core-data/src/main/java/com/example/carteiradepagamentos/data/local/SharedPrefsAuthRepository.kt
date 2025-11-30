package com.example.carteiradepagamentos.data.local

import com.example.carteiradepagamentos.domain.model.Session
import com.example.carteiradepagamentos.domain.model.User
import com.example.carteiradepagamentos.domain.repository.AuthRepository
import com.example.carteiradepagamentos.domain.storage.AuthStorage
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPrefsAuthRepository @Inject constructor(
    private val authStorage: AuthStorage
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<Session> {
        delay(500)

        return if (email == "user@example.com" && password == "123456") {
            val user = User(
                id = "1",
                name = "Usuário Exemplo",
                email = email
            )
            val newSession = Session(
                token = "fake-token-123",
                user = user
            )
            authStorage.saveSession(newSession)
            Result.success(newSession)
        } else {
            Result.failure(IllegalArgumentException("Credenciais inválidas"))
        }
    }

    override suspend fun logout() {
        authStorage.clearSession()
    }

    override suspend fun getCurrentSession(): Session? {
        return authStorage.loadSession()
    }
}
