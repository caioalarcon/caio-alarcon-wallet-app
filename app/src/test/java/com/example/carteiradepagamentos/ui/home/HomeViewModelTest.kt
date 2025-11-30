package com.example.carteiradepagamentos.ui.home

import com.example.carteiradepagamentos.MainDispatcherRule
import com.example.carteiradepagamentos.domain.model.AccountSummary
import com.example.carteiradepagamentos.domain.model.Contact
import com.example.carteiradepagamentos.domain.model.Session
import com.example.carteiradepagamentos.domain.model.User
import com.example.carteiradepagamentos.domain.repository.AuthRepository
import com.example.carteiradepagamentos.domain.repository.WalletRepository
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    class FakeAuthRepository(var session: Session? = null) : AuthRepository {
        override suspend fun login(email: String, password: String): Result<Session> =
            Result.success(session ?: Session("token", User("1", "User", email)))

        override suspend fun logout() {
            session = null
        }

        override suspend fun getCurrentSession(): Session? = session
    }

    class FakeWalletRepository(
        private val accountSummary: AccountSummary,
        private val contacts: List<Contact>
    ) : WalletRepository {
        override suspend fun getAccountSummary(): AccountSummary = accountSummary
        override suspend fun getContacts(): List<Contact> = contacts
        override suspend fun transfer(toContactId: String, amountInCents: Long) =
            Result.success(accountSummary)
    }

    @Test
    fun `when session is null user is logged out`() = runTest {
        val authRepository = FakeAuthRepository(session = null)
        val walletRepository = FakeWalletRepository(AccountSummary(0), emptyList())
        val viewModel = HomeViewModel(authRepository, walletRepository)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isLoggedOut)
        assertEquals("Sessão expirada", state.errorMessage)
    }

    @Test
    fun `when session exists home data is populated`() = runTest {
        val session = Session(token = "token", user = User("1", "Usuário", "user@example.com"))
        val contacts = listOf(Contact("1", "Alice", "0001-1"), Contact("2", "Bob", "0001-2"))
        val authRepository = FakeAuthRepository(session)
        val walletRepository = FakeWalletRepository(AccountSummary(12_345), contacts)
        val viewModel = HomeViewModel(authRepository, walletRepository)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Usuário", state.userName)
        assertEquals("user@example.com", state.userEmail)
        assertEquals("R$ 123,45", state.balanceText)
        assertEquals(contacts, state.contacts)
    }
}
