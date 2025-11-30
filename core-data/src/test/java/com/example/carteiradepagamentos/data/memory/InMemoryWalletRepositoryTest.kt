package com.example.carteiradepagamentos.data.memory

import com.example.carteiradepagamentos.domain.model.Session
import com.example.carteiradepagamentos.domain.model.User
import com.example.carteiradepagamentos.domain.repository.AuthRepository
import com.example.carteiradepagamentos.domain.service.AuthorizeService
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryWalletRepositoryTest {

    class FakeAuthorizeService(private val allowed: Boolean) : AuthorizeService {
        override suspend fun authorizeTransfer(amountInCents: Long) = Result.success(allowed)
    }

    class FakeAuthRepository(private val session: Session?) : AuthRepository {
        override suspend fun login(email: String, password: String) = Result.failure<Session>(UnsupportedOperationException())
        override suspend fun logout() = Unit
        override suspend fun getCurrentSession(): Session? = session
    }

    @Test
    fun `transfer with valid contact amount and authorization succeeds and decreases balance`() = runTest {
        val repository = InMemoryWalletRepository(
            FakeAuthorizeService(true),
            FakeAuthRepository(Session(token = "t", user = User("1", "User", "u@example.com")))
        )

        val result = repository.transfer("c1", 2_500)

        assertTrue(result.isSuccess)
        assertEquals(97_500, result.getOrNull()?.balanceInCents)
    }

    @Test
    fun `transfer fails when contact does not exist`() = runTest {
        val repository = InMemoryWalletRepository(
            FakeAuthorizeService(true),
            FakeAuthRepository(Session(token = "t", user = User("1", "User", "u@example.com")))
        )

        val result = repository.transfer("unknown", 100)

        assertTrue(result.isFailure)
        assertEquals("Contato inválido", result.exceptionOrNull()?.message)
    }

    @Test
    fun `transfer fails when amount is not positive`() = runTest {
        val repository = InMemoryWalletRepository(
            FakeAuthorizeService(true),
            FakeAuthRepository(Session(token = "t", user = User("1", "User", "u@example.com")))
        )

        val result = repository.transfer("c1", 0)

        assertTrue(result.isFailure)
        assertEquals("Valor inválido", result.exceptionOrNull()?.message)
    }

    @Test
    fun `transfer fails when authorization denies`() = runTest {
        val repository = InMemoryWalletRepository(
            FakeAuthorizeService(false),
            FakeAuthRepository(Session(token = "t", user = User("1", "User", "u@example.com")))
        )

        val result = repository.transfer("c1", 100)

        assertTrue(result.isFailure)
        assertEquals("operation not allowed", result.exceptionOrNull()?.message)
    }

    @Test
    fun `transfer fails when amount exceeds balance`() = runTest {
        val repository = InMemoryWalletRepository(
            FakeAuthorizeService(true),
            FakeAuthRepository(Session(token = "t", user = User("1", "User", "u@example.com")))
        )

        val result = repository.transfer("c1", 200_000)

        assertTrue(result.isFailure)
        assertEquals("Saldo insuficiente", result.exceptionOrNull()?.message)
    }
}
