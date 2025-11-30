package com.example.carteiradepagamentos.data.local

import android.content.Context
import androidx.core.content.edit
import com.example.carteiradepagamentos.domain.model.ThemeMode
import com.example.carteiradepagamentos.domain.repository.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPrefsUserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : UserPreferencesRepository {

    private val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    override suspend fun getThemeForUser(userId: String?): ThemeMode {
        val key = themeKey(userId)
        val stored = prefs.getString(key, null) ?: return ThemeMode.SYSTEM
        return runCatching { ThemeMode.valueOf(stored) }.getOrDefault(ThemeMode.SYSTEM)
    }

    override suspend fun setThemeForUser(userId: String?, mode: ThemeMode) {
        val key = themeKey(userId)
        prefs.edit(commit = true) {
            putString(key, mode.name)
        }
    }

    override suspend fun getLastLoggedEmail(): String? =
        prefs.getString("last_logged_email", null)

    override suspend fun setLastLoggedEmail(email: String?) {
        prefs.edit(commit = true) {
            if (email == null) {
                remove("last_logged_email")
            } else {
                putString("last_logged_email", email)
            }
        }
    }

    private fun themeKey(userId: String?): String =
        if (userId != null) "theme_user_${userId}" else "theme_global"
}
