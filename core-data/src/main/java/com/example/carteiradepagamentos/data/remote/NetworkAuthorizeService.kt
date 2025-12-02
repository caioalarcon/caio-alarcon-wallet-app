package com.example.carteiradepagamentos.data.remote

import com.example.carteiradepagamentos.domain.service.AuthorizeService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkAuthorizeService @Inject constructor(
    private val api: AuthorizeApi
) : AuthorizeService {

    override suspend fun authorizeTransfer(amountInCents: Long): Result<Boolean> {
        return Result.failure(UnsupportedOperationException("Authorize transfer not implemented"))
    }
}
