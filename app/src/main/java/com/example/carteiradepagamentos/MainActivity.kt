package com.example.carteiradepagamentos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import com.example.carteiradepagamentos.domain.model.ThemeMode
import com.example.carteiradepagamentos.feature.home.HomeScreen
import com.example.carteiradepagamentos.feature.login.LoginScreen
import com.example.carteiradepagamentos.feature.settings.SettingsScreen
import com.example.carteiradepagamentos.feature.transfer.TransferScreen
import com.example.carteiradepagamentos.ui.theme.CarteiraDePagamentosTheme
import dagger.hilt.android.AndroidEntryPoint

sealed class Screen {
    data object Login : Screen()
    data object Home : Screen()
    data class Transfer(val contactId: String?) : Screen()
    data object Settings : Screen()
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WalletAppRoot()
        }
    }
}

@Composable
fun WalletAppRoot() {
    val appViewModel: AppViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val themeViewModel: ThemeViewModel = androidx.hilt.navigation.compose.hiltViewModel()

    val appUiState by appViewModel.uiState.collectAsState()
    val themeMode by themeViewModel.themeMode.collectAsState()

    val isDarkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    CarteiraDePagamentosTheme(darkTheme = isDarkTheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            if (!appUiState.isReady) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center)
                ) {
                    CircularProgressIndicator()
                }
            } else {
                when (val screen = appUiState.currentScreen) {
                    is Screen.Login -> LoginScreen(
                        onLoginSuccess = { session ->
                            themeViewModel.onSessionChanged(session)
                            appViewModel.onLoginSuccess()
                        }
                    )

                    is Screen.Home -> HomeScreen(
                        onLogout = { appViewModel.onLoggedOut() },
                        onOpenSettings = { appViewModel.navigateTo(Screen.Settings) },
                        onContactSelected = { contactId ->
                            appViewModel.navigateTo(Screen.Transfer(contactId))
                        }
                    )

                    is Screen.Transfer -> TransferScreen(
                        contactId = screen.contactId,
                        onBackToHome = { appViewModel.navigateTo(Screen.Home) }
                    )

                    is Screen.Settings -> SettingsScreen(
                        onBack = { appViewModel.navigateTo(Screen.Home) },
                        onLoggedOut = {
                            themeViewModel.onSessionChanged(null)
                            appViewModel.onLoggedOut()
                        }
                    )
                }
            }
        }
    }
}
