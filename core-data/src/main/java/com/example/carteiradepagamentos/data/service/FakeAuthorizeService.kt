package com.example.carteiradepagamentos.data.service

import com.example.carteiradepagamentos.domain.service.AuthorizeService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeAuthorizeService @Inject constructor() : AuthorizeService {
    override suspend fun authorizeTransfer(amountInCents: Long): Result<Boolean> {
        return if (amountInCents == 40_300L) {
            Result.success(false)
        } else {
            Result.success(true)
        }
    }
}
