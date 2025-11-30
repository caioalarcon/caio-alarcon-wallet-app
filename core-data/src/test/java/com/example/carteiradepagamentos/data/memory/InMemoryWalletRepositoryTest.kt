package com.example.carteiradepagamentos.data.memory

import com.example.carteiradepagamentos.domain.service.AuthorizeService
import com.example.carteiradepagamentos.domain.storage.BalanceStorage
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryWalletRepositoryTest {

    class FakeAuthorizeService(private val allowed: Boolean) : AuthorizeService {
        override suspend fun authorizeTransfer(amountInCents: Long) = Result.success(allowed)
    }

    class FakeBalanceStorage(initial: Long) : BalanceStorage {
        var value = initial
        override fun loadBalance() = value
        override fun saveBalance(newBalance: Long) { value = newBalance }
    }

    @Test
    fun `transfer with valid contact amount and authorization succeeds and decreases balance`() = runTest {
        val balanceStorage = FakeBalanceStorage(initial = 10_000)
        val repository = InMemoryWalletRepository(FakeAuthorizeService(true), balanceStorage)

        val result = repository.transfer("c1", 2_500)

        assertTrue(result.isSuccess)
        assertEquals(7_500, balanceStorage.loadBalance())
    }

    @Test
    fun `transfer fails when contact does not exist`() = runTest {
        val repository = InMemoryWalletRepository(FakeAuthorizeService(true), FakeBalanceStorage(1_000))

        val result = repository.transfer("unknown", 100)

        assertTrue(result.isFailure)
        assertEquals("Contato inválido", result.exceptionOrNull()?.message)
    }

    @Test
    fun `transfer fails when amount is not positive`() = runTest {
        val repository = InMemoryWalletRepository(FakeAuthorizeService(true), FakeBalanceStorage(1_000))

        val result = repository.transfer("c1", 0)

        assertTrue(result.isFailure)
        assertEquals("Valor inválido", result.exceptionOrNull()?.message)
    }

    @Test
    fun `transfer fails when authorization denies`() = runTest {
        val repository = InMemoryWalletRepository(FakeAuthorizeService(false), FakeBalanceStorage(1_000))

        val result = repository.transfer("c1", 100)

        assertTrue(result.isFailure)
        assertEquals("operation not allowed", result.exceptionOrNull()?.message)
    }

    @Test
    fun `transfer fails when amount exceeds balance`() = runTest {
        val repository = InMemoryWalletRepository(FakeAuthorizeService(true), FakeBalanceStorage(500))

        val result = repository.transfer("c1", 1_000)

        assertTrue(result.isFailure)
        assertEquals("Saldo insuficiente", result.exceptionOrNull()?.message)
    }
}
