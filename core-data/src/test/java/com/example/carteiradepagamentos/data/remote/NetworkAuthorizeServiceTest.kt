package com.example.carteiradepagamentos.data.remote

import com.example.carteiradepagamentos.domain.model.NetworkConfig
import com.example.carteiradepagamentos.domain.repository.AppPreferencesRepository
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NetworkAuthorizeServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var service: NetworkAuthorizeService

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val baseUrl = server.url("/").toString()
        val prefs = FakeAppPrefs(baseUrl)
        service = NetworkAuthorizeService(RetrofitFactory(), prefs)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `amount 40300 is denied by authorize service`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"authorized": false, "reason":"operation not allowed"}""")
        )

        val result = service.authorizeTransfer(40_300)

        assertTrue(result.isSuccess)
        assertFalse(result.getOrThrow())
    }

    private class FakeAppPrefs(
        private val baseUrl: String
    ) : AppPreferencesRepository {
        private val config = NetworkConfig(useRemoteServer = true, baseUrl = baseUrl)
        override suspend fun getNetworkConfig(): NetworkConfig = config
        override fun observeNetworkConfig() = kotlinx.coroutines.flow.flowOf(config)
        override suspend fun setNetworkConfig(config: NetworkConfig) = Unit
    }
}
