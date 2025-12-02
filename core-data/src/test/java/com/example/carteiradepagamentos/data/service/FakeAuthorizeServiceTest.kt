package com.example.carteiradepagamentos.data.service

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeAuthorizeServiceTest {

    @Test
    fun `amount 40300 is denied by fake authorize service`() = runTest {
        val service = FakeAuthorizeService()

        val result = service.authorizeTransfer(40_300)

        assertTrue(result.isSuccess)
        assertFalse(result.getOrThrow())
    }

    @Test
    fun `amount different from 40300 is allowed by fake authorize service`() = runTest {
        val service = FakeAuthorizeService()

        val result = service.authorizeTransfer(1_000)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow())
    }
}
