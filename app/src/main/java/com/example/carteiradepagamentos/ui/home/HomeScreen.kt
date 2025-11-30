package com.example.carteiradepagamentos.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onContactSelected: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isLoggedOut) {
        if (uiState.isLoggedOut) {
            onLogout()
        }
    }

    HomeContent(
        uiState = uiState,
        onContactClick = onContactSelected
    )
}

@Composable
fun HomeContent(
    uiState: HomeUiState,
    onContactClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Olá, ${uiState.userName}",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = uiState.userEmail,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Saldo atual",
            style = MaterialTheme.typography.labelLarge
        )
        Text(
            text = uiState.balanceText,
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(16.dp))

        Text(text = "Contatos", style = MaterialTheme.typography.titleMedium)

        when {
            uiState.isLoading -> {
                Spacer(Modifier.height(16.dp))
                CircularProgressIndicator()
            }

            uiState.contacts.isEmpty() -> {
                Spacer(Modifier.height(16.dp))
                Text("Nenhum contato disponível.")
            }

            else -> {
                Spacer(Modifier.height(8.dp))
                LazyColumn {
                    items(uiState.contacts) { contact ->
                        ContactItem(
                            name = contact.name,
                            accountNumber = contact.accountNumber,
                            onClick = { onContactClick(contact.id) }
                        )
                    }
                }
            }
        }

        uiState.errorMessage?.let { msg ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = msg,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun ContactItem(
    name: String,
    accountNumber: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(text = name, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "Conta: $accountNumber",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
