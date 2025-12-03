package com.example.carteiradepagamentos.data.remote

import com.example.carteiradepagamentos.domain.model.Session
import com.example.carteiradepagamentos.domain.model.User
import com.example.carteiradepagamentos.domain.repository.AuthRepository
import com.example.carteiradepagamentos.domain.service.AuthorizeService
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class NetworkWalletRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var walletApi: WalletApi
    private lateinit var repository: NetworkWalletRepository

    private class FakeAuthRepository : AuthRepository {
        override suspend fun login(email: String, password: String) =
            Result.failure<Session>(UnsupportedOperationException())

        override suspend fun logout() = Unit

        override suspend fun getCurrentSession(): Session? =
            Session(token = "token", user = User("1", "User", "user@example.com"))
    }

    private class AllowAllAuthorizeService : AuthorizeService {
        override suspend fun authorizeTransfer(amountInCents: Long) =
            Result.success(true)
    }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(MoshiConverterFactory.create())
            .build()

        walletApi = retrofit.create(WalletApi::class.java)

        repository = NetworkWalletRepository(
            walletApi = walletApi,
            authRepository = FakeAuthRepository(),
            authorizeService = AllowAllAuthorizeService()
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `transfer makes http call and parses response`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"balanceInCents": 97500}""")
        )

        val result = repository.transfer("acc2", 2500)

        val summary = result.getOrThrow()
        assertEquals(97_500L, summary.balanceInCents)
    }
}
