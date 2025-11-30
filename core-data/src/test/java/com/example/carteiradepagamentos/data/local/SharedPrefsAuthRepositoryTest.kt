package com.example.carteiradepagamentos.data.local

import com.example.carteiradepagamentos.domain.model.Session
import com.example.carteiradepagamentos.domain.model.User
import com.example.carteiradepagamentos.domain.repository.AuthRepository
import com.example.carteiradepagamentos.domain.service.AuthRemoteDataSource
import com.example.carteiradepagamentos.domain.storage.AuthStorage
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedPrefsAuthRepositoryTest {

    class FakeAuthRemoteDataSource(private val shouldSucceed: Boolean) : AuthRemoteDataSource {
        override suspend fun login(email: String, password: String): Result<Session> {
            return if (shouldSucceed) {
                Result.success(Session(token = "token", user = User("1", "User", email)))
            } else {
                Result.failure(IllegalArgumentException("Credenciais inválidas"))
            }
        }
    }

    class FakeAuthStorage : AuthStorage {
        var saved: Session? = null
        override fun saveSession(session: Session) { saved = session }
        override fun loadSession(): Session? = saved
        override fun clearSession() { saved = null }
    }

    private fun buildRepository(
        storage: FakeAuthStorage,
        remote: AuthRemoteDataSource = FakeAuthRemoteDataSource(true)
    ): AuthRepository =
        SharedPrefsAuthRepository(storage, remote)

    @Test
    fun `login with valid credentials saves session`() = runTest {
        val storage = FakeAuthStorage()
        val repository = buildRepository(storage, FakeAuthRemoteDataSource(true))

        val result = repository.login("user@example.com", "123456")

        assertTrue(result.isSuccess)
        assertNotNull(storage.saved)
        assertEquals("user@example.com", storage.saved?.user?.email)
    }

    @Test
    fun `login with invalid credentials fails and does not persist session`() = runTest {
        val storage = FakeAuthStorage()
        val repository = buildRepository(storage, FakeAuthRemoteDataSource(false))

        val result = repository.login("wrong@example.com", "bad")

        assertTrue(result.isFailure)
        assertNull(storage.saved)
    }

    @Test
    fun `logout clears stored session`() = runTest {
        val storage = FakeAuthStorage()
        val repository = buildRepository(storage)
        storage.saved = Session(token = "token", user = User("1", "User", "u@example.com"))

        repository.logout()

        assertNull(storage.saved)
    }

    @Test
    fun `getCurrentSession returns what is stored`() = runTest {
        val storage = FakeAuthStorage()
        val repository = buildRepository(storage)
        val session = Session(token = "abc", user = User("1", "User", "u@example.com"))
        storage.saveSession(session)

        val result = repository.getCurrentSession()

        assertEquals(session, result)
    }
}
