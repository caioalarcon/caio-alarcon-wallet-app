package com.example.carteiradepagamentos.domain.service

import com.example.carteiradepagamentos.domain.model.Session

fun interface AuthRemoteDataSource {
    suspend fun login(email: String, password: String): Result<Session>
}
