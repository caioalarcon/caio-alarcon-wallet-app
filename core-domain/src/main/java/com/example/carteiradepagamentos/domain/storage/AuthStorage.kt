package com.example.carteiradepagamentos.domain.storage

import com.example.carteiradepagamentos.domain.model.Session

interface AuthStorage {
    fun saveSession(session: Session)
    fun loadSession(): Session?
    fun clearSession()
}
