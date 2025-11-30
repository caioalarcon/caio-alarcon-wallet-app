package com.example.carteiradepagamentos.data.local

import com.example.carteiradepagamentos.domain.model.Session
import com.example.carteiradepagamentos.domain.repository.AuthRepository
import com.example.carteiradepagamentos.domain.service.AuthRemoteDataSource
import com.example.carteiradepagamentos.domain.storage.AuthStorage
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPrefsAuthRepository @Inject constructor(
    private val authStorage: AuthStorage,
    private val remote: AuthRemoteDataSource
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<Session> {
        delay(500)

        val result = remote.login(email, password)
        result.onSuccess { session ->
            authStorage.saveSession(session)
        }
        return result
    }

    override suspend fun logout() {
        delay(500)
        authStorage.clearSession()
    }

    override suspend fun getCurrentSession(): Session? {
        return authStorage.loadSession()
    }
}
