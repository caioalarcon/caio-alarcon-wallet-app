package com.example.carteiradepagamentos.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

data class AccountSummaryResponse(
    val balanceInCents: Long
)

data class ContactResponse(
    val id: String,
    val ownerUserId: String,
    val name: String,
    val accountNumber: String
)

data class TransferRequest(
    val userId: String,
    val toContactId: String,
    val amountInCents: Long
)

data class TransferResponse(
    val balanceInCents: Long
)

interface WalletApi {

    @GET("wallet/summary")
    suspend fun getSummary(
        @Query("userId") userId: String
    ): AccountSummaryResponse

    @GET("wallet/contacts")
    suspend fun getContacts(
        @Query("userId") userId: String
    ): List<ContactResponse>

    @POST("wallet/transfer")
    suspend fun transfer(
        @Body request: TransferRequest
    ): TransferResponse
}
