package com.example.carteiradepagamentos.data.remote

import com.example.carteiradepagamentos.domain.model.Session
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeAuthRemoteDataSourceTest {

    private val dataSource = FakeAuthRemoteDataSource()

    @Test
    fun `login succeeds for known user with correct password`() = runTest {
        val result = dataSource.login("alice@example.com", "alice123")

        assertTrue(result.isSuccess)
        val session: Session? = result.getOrNull()
        assertEquals("2", session?.user?.id)
        assertEquals("Alice", session?.user?.name)
        assertEquals("fake-token-2", session?.token)
    }

    @Test
    fun `login fails for unknown email`() = runTest {
        val result = dataSource.login("unknown@example.com", "whatever")

        assertTrue(result.isFailure)
    }

    @Test
    fun `login fails for wrong password`() = runTest {
        val result = dataSource.login("bob@example.com", "wrong")

        assertTrue(result.isFailure)
    }
}
