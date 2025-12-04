package com.example.carteiradepagamentos.feature.login

import com.example.carteiradepagamentos.MainDispatcherRule
import com.example.carteiradepagamentos.domain.model.Session
import com.example.carteiradepagamentos.domain.model.User
import com.example.carteiradepagamentos.domain.repository.AuthRepository
import com.example.carteiradepagamentos.domain.repository.UserPreferencesRepository
import com.example.carteiradepagamentos.domain.model.ThemeMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    class FakeAuthRepository(
        private val shouldSucceed: Boolean = true
    ) : AuthRepository {

        var lastEmail: String? = null
        var lastPassword: String? = null
        var loginCalls: Int = 0

        override suspend fun login(email: String, password: String): Result<Session> {
            loginCalls++
            lastEmail = email
            lastPassword = password

            return if (shouldSucceed) {
                Result.success(
                    Session(
                        token = "token",
                        user = User("1", "User", email)
                    )
                )
            } else {
                Result.failure(IllegalArgumentException("Credenciais inválidas"))
            }
        }

        override suspend fun logout() {
            // não usado aqui
        }

        override suspend fun getCurrentSession(): Session? = null
    }

    class FakeUserPreferencesRepository : UserPreferencesRepository {
        var lastSavedEmail: String? = null

        override suspend fun getThemeForUser(userId: String?) = ThemeMode.SYSTEM

        override suspend fun setThemeForUser(userId: String?, mode: ThemeMode) {
            // não usado neste teste
        }

        override suspend fun getLastLoggedEmail(): String? = null

        override suspend fun setLastLoggedEmail(email: String?) {
            lastSavedEmail = email
        }
    }

    @Test
    fun `when email or password is blank shows validation error and does not call repository`() = runTest {
        val fakeRepo = FakeAuthRepository()
        val fakeUserPrefs = FakeUserPreferencesRepository()
        val viewModel = LoginViewModel(fakeRepo, fakeUserPrefs)

        // estado inicial: ambos vazios
        viewModel.onLoginClicked()

        val state = viewModel.uiState.value
        assertEquals("Preencha email e senha", state.errorMessage)
        assertEquals(0, fakeRepo.loginCalls)
    }

    @Test
    fun `successful login updates state with success and stops loading`() = runTest {
        val fakeRepo = FakeAuthRepository(shouldSucceed = true)
        val fakeUserPrefs = FakeUserPreferencesRepository()
        val viewModel = LoginViewModel(fakeRepo, fakeUserPrefs)

        viewModel.onEmailChanged("user@example.com")
        viewModel.onPasswordChanged("123456")

        viewModel.onLoginClicked()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.loginSucceeded)
        assertEquals(null, state.errorMessage)
        assertEquals("user@example.com", fakeRepo.lastEmail)
        assertEquals("123456", fakeRepo.lastPassword)
        assertEquals("user@example.com", fakeUserPrefs.lastSavedEmail)
    }

    @Test
    fun `failed login shows error and does not mark success`() = runTest {
        val fakeRepo = FakeAuthRepository(shouldSucceed = false)
        val fakeUserPrefs = FakeUserPreferencesRepository()
        val viewModel = LoginViewModel(fakeRepo, fakeUserPrefs)

        viewModel.onEmailChanged("wrong@example.com")
        viewModel.onPasswordChanged("bad")

        viewModel.onLoginClicked()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.loginSucceeded)
        assertEquals("Credenciais inválidas", state.errorMessage)
    }
}
