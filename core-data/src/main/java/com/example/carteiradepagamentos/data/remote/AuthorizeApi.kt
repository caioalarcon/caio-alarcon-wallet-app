package com.example.carteiradepagamentos.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

data class AuthorizeRequest(
    val value: Long
)

data class AuthorizeResponse(
    val authorized: Boolean,
    val reason: String? = null
)

fun interface AuthorizeApi {
    @POST("authorize")
    suspend fun authorize(
        @Body request: AuthorizeRequest
    ): AuthorizeResponse
}
