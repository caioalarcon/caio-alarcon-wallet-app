package com.example.carteiradepagamentos.data.remote

import com.example.carteiradepagamentos.domain.model.AccountSummary
import com.example.carteiradepagamentos.domain.model.Contact
import com.example.carteiradepagamentos.domain.model.Session
import com.example.carteiradepagamentos.domain.model.User
import com.example.carteiradepagamentos.domain.repository.AuthRepository
import com.example.carteiradepagamentos.domain.service.AuthorizeService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkWalletRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var walletApi: WalletApi

    private class FakeAuthRepository(
        private val session: Session?
    ) : AuthRepository {
        override suspend fun login(email: String, password: String): Result<Session> =
            Result.failure(UnsupportedOperationException("Not used in these tests"))

        override suspend fun logout() = Unit

        override suspend fun getCurrentSession(): Session? = session
    }

    private class RecordingAuthorizeService(
        private val result: Result<Boolean>
    ) : AuthorizeService {
        var lastRequestedAmount: Long? = null

        override suspend fun authorizeTransfer(amountInCents: Long): Result<Boolean> {
            lastRequestedAmount = amountInCents
            return result
        }
    }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val client = OkHttpClient.Builder().build()

        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        walletApi = retrofit.create(WalletApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun buildRepository(
        authRepository: AuthRepository = FakeAuthRepository(
            Session(
                token = "token",
                user = User(id = "1", name = "User", email = "user@example.com")
            )
        ),
        authorizeService: AuthorizeService = RecordingAuthorizeService(Result.success(true))
    ): NetworkWalletRepository {
        return NetworkWalletRepository(
            walletApi = walletApi,
            authRepository = authRepository,
            authorizeService = authorizeService
        )
    }

    @Test
    fun `getAccountSummary calls correct endpoint and parses balance`() = runTest {
        val repository = buildRepository()

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"balanceInCents": 100000}""")
        )

        val summary: AccountSummary = repository.getAccountSummary()

        assertEquals(100_000L, summary.balanceInCents)

        val request = server.takeRequest()
        val url = request.requestUrl!!
        assertEquals("GET", request.method)
        assertEquals("/wallet/summary", url.encodedPath())
        assertEquals("1", url.queryParameter("userId"))
    }

    @Test
    fun `getContacts calls correct endpoint and parses contacts`() = runTest {
        val repository = buildRepository()

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    [
                      {"id":"acc2","name":"Alice","accountNumber":"0001-2"},
                      {"id":"acc3","name":"Bob","accountNumber":"0001-3"}
                    ]
                    """.trimIndent()
                )
        )

        val contacts: List<Contact> = repository.getContacts()

        assertEquals(2, contacts.size)
        assertEquals("acc2", contacts[0].id)
        assertEquals("Alice", contacts[0].name)
        assertEquals("0001-2", contacts[0].accountNumber)

        val request = server.takeRequest()
        val url = request.requestUrl!!
        assertEquals("GET", request.method)
        assertEquals("/wallet/contacts", url.encodedPath())
        assertEquals("1", url.queryParameter("userId"))
    }

    @Test
    fun `transfer sends POST with correct body and returns updated balance when authorized`() = runTest {
        val authorizeService = RecordingAuthorizeService(Result.success(true))
        val repository = buildRepository(authorizeService = authorizeService)

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"balanceInCents": 97_500}""".replace("_", ""))
        )

        val result = repository.transfer(toContactId = "acc2", amountInCents = 2_500L)
        val summary = result.getOrThrow()

        // saldo retornado pelo servidor
        assertEquals(97_500L, summary.balanceInCents)
        // valor enviado para autorização
        assertEquals(2_500L, authorizeService.lastRequestedAmount)

        // pode haver múltiplas requisições (ex. futuras mudanças),
        // garantimos que ao menos uma delas é o POST /wallet/transfer correto
        var transferRequestFound: Boolean = false
        repeat(server.requestCount) {
            val request = server.takeRequest()
            val url = request.requestUrl!!

            if (url.encodedPath() == "/wallet/transfer") {
                transferRequestFound = true
                assertEquals("POST", request.method)

                val bodyJson = JSONObject(request.body.readUtf8())
                assertEquals("1", bodyJson.getString("userId"))
                assertEquals("acc2", bodyJson.getString("toContactId"))
                assertEquals(2_500L, bodyJson.getLong("amountInCents"))
            }
        }

        assertTrue("Expected at least one POST /wallet/transfer call", transferRequestFound)
    }

    @Test
    fun `transfer returns failure and does not hit server when session is null`() = runTest {
        val authRepository = FakeAuthRepository(session = null)
        val authorizeService = RecordingAuthorizeService(Result.success(true))
        val repository = buildRepository(
            authRepository = authRepository,
            authorizeService = authorizeService
        )

        val result = repository.transfer(toContactId = "acc2", amountInCents = 1_000L)

        assertTrue(result.isFailure)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `transfer propagates http error when server responds with 400`() = runTest {
        val authorizeService = RecordingAuthorizeService(Result.success(true))
        val repository = buildRepository(authorizeService = authorizeService)

        server.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setBody("""{"message":"Saldo insuficiente"}""")
        )

        val result = repository.transfer(toContactId = "acc2", amountInCents = 200_000L)

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertNotNull(error)
        assertTrue(error is HttpException)
        assertEquals(400, (error as HttpException).code())

        val request = server.takeRequest()
        val url = request.requestUrl!!
        assertEquals("/wallet/transfer", url.encodedPath())
        assertEquals("POST", request.method)
    }
}
