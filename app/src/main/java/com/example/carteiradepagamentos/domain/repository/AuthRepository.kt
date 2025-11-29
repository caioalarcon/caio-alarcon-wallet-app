package com.example.carteiradepagamentos.domain.repository

import com.example.carteiradepagamentos.domain.model.Session

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<Session>
    suspend fun logout()
    suspend fun getCurrentSession(): Session?
}
