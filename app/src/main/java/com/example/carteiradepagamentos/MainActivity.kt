package com.example.carteiradepagamentos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.carteiradepagamentos.ui.home.HomeScreen
import com.example.carteiradepagamentos.ui.login.LoginScreen
import com.example.carteiradepagamentos.ui.theme.CarteiraDePagamentosTheme
import com.example.carteiradepagamentos.ui.transfer.TransferScreen
import dagger.hilt.android.AndroidEntryPoint

sealed class Screen {
    data object Login : Screen()
    data object Home : Screen()
    data class Transfer(val contactId: String?) : Screen()
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
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Login) }
    CarteiraDePagamentosTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (val screen = currentScreen) {
                is Screen.Login -> LoginScreen(
                    onLoginSuccess = { currentScreen = Screen.Home }
                )

                is Screen.Home -> HomeScreen(
                    onLogout = { currentScreen = Screen.Login },
                    onContactSelected = { contactId ->
                        currentScreen = Screen.Transfer(contactId)
                    }
                )

                is Screen.Transfer -> TransferScreen(
                    contactId = screen.contactId,
                    onBackToHome = { currentScreen = Screen.Home }
                )
            }
        }
    }
}
