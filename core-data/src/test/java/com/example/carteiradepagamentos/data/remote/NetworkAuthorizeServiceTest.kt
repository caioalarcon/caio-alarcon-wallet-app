package com.example.carteiradepagamentos.data.remote

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkAuthorizeServiceTest {

    private fun buildService(): NetworkAuthorizeService =
        NetworkAuthorizeService(FakeAuthorizeApi())

    @Test
    fun `amount 40300 is denied by authorize service`() = runTest {
        val service = buildService()

        val result = service.authorizeTransfer(40_300)

        assertTrue(result.isSuccess)
        assertFalse(result.getOrThrow())
    }
}

private class FakeAuthorizeApi : AuthorizeApi {
    override suspend fun authorize(request: AuthorizeRequest): AuthorizeResponse {
        return AuthorizeResponse(authorized = request.value != 40_300L)
    }
}
