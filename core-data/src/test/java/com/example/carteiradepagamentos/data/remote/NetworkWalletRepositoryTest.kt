package com.example.carteiradepagamentos.data.remote

import com.example.carteiradepagamentos.domain.model.AccountSummary
import com.example.carteiradepagamentos.domain.model.Contact
import com.example.carteiradepagamentos.domain.model.NetworkConfig
import com.example.carteiradepagamentos.domain.model.Session
import com.example.carteiradepagamentos.domain.model.User
import com.example.carteiradepagamentos.domain.repository.AppPreferencesRepository
import com.example.carteiradepagamentos.domain.repository.AuthRepository
import com.example.carteiradepagamentos.domain.service.AuthorizeService
import com.google.gson.JsonParser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkWalletRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var appPrefs: FakeAppPreferencesRepository
    private val retrofitFactory = RetrofitFactory()

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
        appPrefs = FakeAppPreferencesRepository(server.url("/").toString())
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
            retrofitFactory = retrofitFactory,
            appPreferencesRepository = appPrefs,
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
        assertEquals("/wallet/summary", url.encodedPath)
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
                      {"id":"acc2","ownerUserId":"2","name":"Alice","accountNumber":"0001-2"},
                      {"id":"acc3","ownerUserId":"3","name":"Bob","accountNumber":"0001-3"}
                    ]
                    """.trimIndent()
                )
        )

        val contacts: List<Contact> = repository.getContacts()

        assertEquals(2, contacts.size)
        assertEquals("acc2", contacts[0].id)
        assertEquals("2", contacts[0].ownerUserId)
        assertEquals("Alice", contacts[0].name)
        assertEquals("0001-2", contacts[0].accountNumber)

        val request = server.takeRequest()
        val url = request.requestUrl!!
        assertEquals("GET", request.method)
        assertEquals("/wallet/contacts", url.encodedPath)
        assertEquals("1", url.queryParameter("userId"))
    }

    @Test
    fun `transfer sends POST with correct body and returns updated balance when authorized`() = runTest {
        val authorizeService = RecordingAuthorizeService(Result.success(true))
        val repository = buildRepository(authorizeService = authorizeService)

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"balanceInCents": 100_000}""".replace("_", ""))
        )
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

        val summaryRequest = server.takeRequest()
        val summaryUrl = summaryRequest.requestUrl!!
        assertEquals("GET", summaryRequest.method)
        assertEquals("/wallet/summary", summaryUrl.encodedPath)
        assertEquals("1", summaryUrl.queryParameter("userId"))

        val transferRequest = server.takeRequest()
        val transferUrl = transferRequest.requestUrl!!
        assertEquals("POST", transferRequest.method)
        assertEquals("/wallet/transfer", transferUrl.encodedPath)

        val bodyJson = JsonParser().parse(transferRequest.body.readUtf8()).asJsonObject
        assertEquals("1", bodyJson.get("userId").asString)
        assertEquals("acc2", bodyJson.get("toContactId").asString)
        assertEquals(2_500L, bodyJson.get("amountInCents").asLong)
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
                .setResponseCode(200)
                .setBody("""{"balanceInCents": 300_000}""".replace("_", ""))
        )
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

        val summaryRequest = server.takeRequest()
        val summaryUrl = summaryRequest.requestUrl!!
        assertEquals("/wallet/summary", summaryUrl.encodedPath)
        assertEquals("GET", summaryRequest.method)

        val transferRequest = server.takeRequest()
        val transferUrl = transferRequest.requestUrl!!
        assertEquals("/wallet/transfer", transferUrl.encodedPath)
        assertEquals("POST", transferRequest.method)
    }

    private class FakeAppPreferencesRepository(
        baseUrl: String
    ) : AppPreferencesRepository {
        private val config = NetworkConfig(
            useRemoteServer = true,
            baseUrl = baseUrl
        )

        override suspend fun getNetworkConfig(): NetworkConfig = config
        override fun observeNetworkConfig() = kotlinx.coroutines.flow.flowOf(config)
        override suspend fun setNetworkConfig(config: NetworkConfig) = Unit
    }
}
