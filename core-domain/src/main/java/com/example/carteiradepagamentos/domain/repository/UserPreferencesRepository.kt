package com.example.carteiradepagamentos.domain.repository

import com.example.carteiradepagamentos.domain.model.ThemeMode

interface UserPreferencesRepository {
    suspend fun getThemeForUser(userId: String?): ThemeMode
    suspend fun setThemeForUser(userId: String?, mode: ThemeMode)

    suspend fun getLastLoggedEmail(): String?
    suspend fun setLastLoggedEmail(email: String?)
}
