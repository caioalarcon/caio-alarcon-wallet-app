package com.example.carteiradepagamentos.data.local

import android.content.Context
import androidx.core.content.edit
import com.example.carteiradepagamentos.domain.model.Session
import com.example.carteiradepagamentos.domain.model.User
import com.example.carteiradepagamentos.domain.storage.AuthStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPrefsAuthStorage @Inject constructor(
    @ApplicationContext private val context: Context
) : AuthStorage {

    private val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    @Volatile
    private var cachedSession: Session? = null

    override fun saveSession(session: Session) {
        cachedSession = session
        prefs.edit(commit = true) {
            putString("token", session.token)
            putString("user_id", session.user.id)
            putString("user_name", session.user.name)
            putString("user_email", session.user.email)
        }
    }

    override fun loadSession(): Session? {
        cachedSession?.let { return it }

        val token = prefs.getString("token", null) ?: return null
        val userId = prefs.getString("user_id", null) ?: return null
        val name = prefs.getString("user_name", null) ?: ""
        val email = prefs.getString("user_email", null) ?: ""

        val user = User(
            id = userId,
            name = name,
            email = email
        )
        val session = Session(
            token = token,
            user = user
        )
        cachedSession = session
        return session
    }

    override fun clearSession() {
        cachedSession = null
        prefs.edit {
            clear()
        }
    }
}
