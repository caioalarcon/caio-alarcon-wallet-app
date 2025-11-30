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
        buildUser(
            id = "1",
            name = "Usuário Exemplo",
            email = "user@example.com",
            password = "123456"
        ),
        buildUser(
            id = "2",
            name = "Alice",
            email = "alice@example.com",
            password = "alice123"
        ),
        buildUser(
            id = "3",
            name = "Bob",
            email = "bob@example.com",
            password = "bob123"
        ),
        buildUser(
            id = "4",
            name = "Carol",
            email = "carol@example.com",
            password = "carol123"
        )
    )

    private fun buildUser(
        id: String,
        name: String,
        email: String,
        password: String
    ): FakeUserRecord = FakeUserRecord(
        id = id,
        name = name,
        email = email,
        passwordHash = hash(password)
    )

    private fun hash(input: String): String = input.hashCode().toUInt().toString(16)

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
}
