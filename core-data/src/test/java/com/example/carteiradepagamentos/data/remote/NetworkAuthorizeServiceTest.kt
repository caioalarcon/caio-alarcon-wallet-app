package com.example.carteiradepagamentos.data.remote

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class NetworkAuthorizeServiceTest {

    private fun buildService(): NetworkAuthorizeService {
        val client = OkHttpClient.Builder()
            .addInterceptor(FakeAuthorizeInterceptor())
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://example.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()

        val api = retrofit.create(AuthorizeApi::class.java)
        return NetworkAuthorizeService(api)
    }

    @Test
    fun `amount 40300 is denied by authorize service`() = runTest {
        val service = buildService()

        val result = service.authorizeTransfer(40_300)

        assertTrue(result.isSuccess)
        assertFalse(result.getOrThrow())
    }
}
