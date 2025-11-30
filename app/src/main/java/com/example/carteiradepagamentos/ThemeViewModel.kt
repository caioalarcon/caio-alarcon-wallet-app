package com.example.carteiradepagamentos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.carteiradepagamentos.domain.model.Session
import com.example.carteiradepagamentos.domain.model.ThemeMode
import com.example.carteiradepagamentos.domain.repository.AuthRepository
import com.example.carteiradepagamentos.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _currentUserId = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            val session = authRepository.getCurrentSession()
            _currentUserId.value = session?.user?.id
            _themeMode.value = userPreferencesRepository.getThemeForUser(session?.user?.id)
        }
    }

    fun onThemeSelected(mode: ThemeMode) {
        viewModelScope.launch {
            val userId = _currentUserId.value
            _themeMode.value = mode
            userPreferencesRepository.setThemeForUser(userId, mode)
        }
    }

    fun onSessionChanged(session: Session?) {
        viewModelScope.launch {
            _currentUserId.value = session?.user?.id
            _themeMode.value = userPreferencesRepository.getThemeForUser(session?.user?.id)
        }
    }
}
