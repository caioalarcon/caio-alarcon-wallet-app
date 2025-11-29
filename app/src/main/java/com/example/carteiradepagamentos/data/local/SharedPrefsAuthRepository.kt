package com.example.carteiradepagamentos.data.local

import android.content.Context
import androidx.core.content.edit
import com.example.carteiradepagamentos.domain.model.Session
import com.example.carteiradepagamentos.domain.model.User
import com.example.carteiradepagamentos.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPrefsAuthRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : AuthRepository {

    private val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    @Volatile
    private var cachedSession: Session? = null

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
            saveSession(newSession)
            Result.success(newSession)
        } else {
            Result.failure(IllegalArgumentException("Credenciais inválidas"))
        }
    }

    override suspend fun logout() {
        cachedSession = null
        prefs.edit {
            clear()
        }
    }

    override suspend fun getCurrentSession(): Session? {
        cachedSession?.let { return it }

        val token = prefs.getString("token", null) ?: return null
        val userId = prefs.getString("user_id", null) ?: return null
        val name = prefs.getString("user_name", null) ?: ""
        val email = prefs.getString("user_email", null) ?: ""

        val user = User(
            id = userId,
            name = name,
            email = email
        )
        val session = Session(
            token = token,
            user = user
        )
        cachedSession = session
        return session
    }

    private fun saveSession(session: Session) {
        cachedSession = session
        prefs.edit(commit = true) {
            putString("token", session.token)
            putString("user_id", session.user.id)
            putString("user_name", session.user.name)
            putString("user_email", session.user.email)
        }
    }
}
