package com.example.carteiradepagamentos.data.remote

import com.example.carteiradepagamentos.domain.model.Session
import com.example.carteiradepagamentos.domain.model.User
import com.example.carteiradepagamentos.domain.service.AuthRemoteDataSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeAuthRemoteDataSource @Inject constructor() : AuthRemoteDataSource {

    private data class FakeUserRecord(
        val id: String,
        val name: String,
        val email: String,
        val passwordHash: String
    )

    private val users = listOf(
        FakeUserRecord(
            id = "1",
            name = "Usuário Exemplo",
            email = "user@example.com",
            passwordHash = hash("123456")
        ),
        FakeUserRecord(
            id = "2",
            name = "Alice",
            email = "alice@example.com",
            passwordHash = hash("alice123")
        ),
        FakeUserRecord(
            id = "3",
            name = "Bob",
            email = "bob@example.com",
            passwordHash = hash("bob123")
        ),
        FakeUserRecord(
            id = "4",
            name = "Carol",
            email = "carol@example.com",
            passwordHash = hash("carol123")
        ),
    )

    override suspend fun login(email: String, password: String): Result<Session> {
        val user = users.firstOrNull { it.email == email }
            ?: return Result.failure(IllegalArgumentException("Credenciais inválidas"))

        if (user.passwordHash != hash(password)) {
            return Result.failure(IllegalArgumentException("Credenciais inválidas"))
        }

        return Result.success(
            Session(
                token = "fake-token-${user.id}",
                user = User(
                    id = user.id,
                    name = user.name,
                    email = user.email
                )
            )
        )
    }

    private fun hash(input: String): String {
        return input.hashCode().toUInt().toString(16)
    }
}
