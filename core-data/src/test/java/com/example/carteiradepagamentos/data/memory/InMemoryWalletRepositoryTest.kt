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

    class FakeAuthRepository(
        private var session: Session?
    ) : AuthRepository {
        override suspend fun login(email: String, password: String) = Result.failure<Session>(UnsupportedOperationException())
        override suspend fun logout() { session = null }
        override suspend fun getCurrentSession(): Session? = session
    }

    private fun buildRepository(
        authorizeService: AuthorizeService = FakeAuthorizeService(true),
        session: Session? = Session(
            token = "token",
            user = User(id = "1", name = "Usuário Exemplo", email = "user@example.com")
        )
    ): InMemoryWalletRepository {
        val authRepository = FakeAuthRepository(session)
        return InMemoryWalletRepository(authorizeService, authRepository)
    }

    @Test
    fun `transfer with valid contact amount and authorization succeeds and decreases balance`() = runTest {
        val repository = buildRepository()

        val result = repository.transfer("acc2", 2_500)

        assertTrue(result.isSuccess)
        assertEquals(97_500L, result.getOrNull()?.balanceInCents)
    }

    @Test
    fun `transfer fails when contact does not exist`() = runTest {
        val repository = buildRepository()

        val result = repository.transfer("unknown", 100)

        assertTrue(result.isFailure)
        assertEquals("Contato inválido", result.exceptionOrNull()?.message)
    }

    @Test
    fun `transfer fails when amount is not positive`() = runTest {
        val repository = buildRepository()

        val result = repository.transfer("acc2", 0)

        assertTrue(result.isFailure)
        assertEquals("Valor inválido", result.exceptionOrNull()?.message)
    }

    @Test
    fun `transfer fails when authorization denies`() = runTest {
        val repository = buildRepository(FakeAuthorizeService(false))

        val result = repository.transfer("acc2", 100)

        assertTrue(result.isFailure)
        assertEquals("operation not allowed", result.exceptionOrNull()?.message)
    }

    @Test
    fun `transfer fails when payer and payee are the same`() = runTest {
        val repository = buildRepository()

        val result = repository.transfer("acc1", 1_000)

        assertTrue(result.isFailure)
        assertEquals("Payer e payee não podem ser iguais", result.exceptionOrNull()?.message)
    }

    @Test
    fun `transfer fails when amount exceeds balance`() = runTest {
        val repository = buildRepository()

        val result = repository.transfer("acc2", 150_000)

        assertTrue(result.isFailure)
        assertEquals("Saldo insuficiente", result.exceptionOrNull()?.message)
    }

    @Test
    fun `getContacts returns all accounts except the payer`() = runTest {
        val repository = buildRepository()

        val contacts = repository.getContacts()

        assertEquals(listOf("acc2", "acc3", "acc4"), contacts.map { it.id })
    }

    @Test
    fun `getAccountSummary uses current user account`() = runTest {
        val repository = buildRepository(
            session = Session(token = "token", user = User(id = "3", name = "Bob", email = "bob@example.com"))
        )

        val summary = repository.getAccountSummary()

        assertEquals(75_000L, summary.balanceInCents)
    }
}
