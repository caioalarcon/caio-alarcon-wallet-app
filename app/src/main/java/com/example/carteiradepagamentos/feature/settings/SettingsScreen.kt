package com.example.carteiradepagamentos.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.carteiradepagamentos.ThemeViewModel
import com.example.carteiradepagamentos.domain.model.ThemeMode

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val themeMode by themeViewModel.themeMode.collectAsState()

    BackHandler(onBack = onBack)

    SettingsContent(
        themeMode = themeMode,
        uiState = uiState,
        onBack = onBack,
        onThemeSelected = themeViewModel::onThemeSelected,
        onLogoutClick = { viewModel.onLogoutClicked(onLoggedOut) }
    )
}

@Composable
fun SettingsContent(
    themeMode: ThemeMode,
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onThemeSelected: (ThemeMode) -> Unit,
    onLogoutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Configurações",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Ajustes rápidos e novas opções no futuro",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onBack) {
                Text(text = "Voltar")
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "Tema", style = MaterialTheme.typography.titleMedium)
                        ThemeOption(
                            title = "Claro",
                            description = "Sempre usar o tema claro",
                            selected = themeMode == ThemeMode.LIGHT,
                            onClick = { onThemeSelected(ThemeMode.LIGHT) }
                        )
                        ThemeOption(
                            title = "Escuro",
                            description = "Sempre usar o tema escuro",
                            selected = themeMode == ThemeMode.DARK,
                            onClick = { onThemeSelected(ThemeMode.DARK) }
                        )
                        ThemeOption(
                            title = "Acompanhar sistema",
                            description = "Respeita a configuração do dispositivo",
                            selected = themeMode == ThemeMode.SYSTEM,
                            onClick = { onThemeSelected(ThemeMode.SYSTEM) }
                        )
                    }
                }

                uiState.errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onLogoutClick,
            enabled = !uiState.isLoggingOut,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (uiState.isLoggingOut) "Desconectando..." else "Desconectar")
        }
    }
}

@Composable
private fun ThemeOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
