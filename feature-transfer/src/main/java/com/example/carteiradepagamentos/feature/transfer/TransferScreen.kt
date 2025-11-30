package com.example.carteiradepagamentos.feature.transfer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.carteiradepagamentos.domain.model.Contact
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun TransferScreen(
    contactId: String?,
    onBackToHome: () -> Unit,
    viewModel: TransferViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(contactId, uiState.contacts) {
        if (contactId != null && uiState.contacts.isNotEmpty()) {
            val target = uiState.contacts.find { it.id == contactId }
            if (target != null && uiState.selectedContact?.id != contactId) {
                viewModel.onContactSelected(target)
            }
        }
    }

    // Só navega, sem trocar layout antes
    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) {
            viewModel.clearSuccessMessage()
            onBackToHome()
        }
    }

    // Mantém sempre o mesmo layout na tela
    TransferContent(
        uiState = uiState,
        onAmountChange = viewModel::onAmountChanged,
        onConfirmTransfer = viewModel::onConfirmTransfer,
        onContactSelected = viewModel::onContactSelected
    )
}

@Composable
fun TransferContent(
    uiState: TransferUiState,
    onAmountChange: (String) -> Unit,
    onConfirmTransfer: () -> Unit,
    onContactSelected: (Contact) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Transferência", style = MaterialTheme.typography.headlineSmall)

        Spacer(Modifier.height(8.dp))

        Text("Saldo: ${uiState.balanceText}")

        Spacer(Modifier.height(16.dp))

        if (uiState.isLoading) {
            CircularProgressIndicator()
            return
        }

        ContactSelection(
            contacts = uiState.contacts,
            selectedContact = uiState.selectedContact,
            onContactSelected = onContactSelected
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.amountInput,
            onValueChange = onAmountChange,
            label = { Text("Valor (em centavos)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onConfirmTransfer,
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Enviar")
            }
        }

        uiState.errorMessage?.let { msg ->
            Spacer(Modifier.height(8.dp))
            Text(msg, color = MaterialTheme.colorScheme.error)
        }

        uiState.successMessage?.let { msg ->
            Spacer(Modifier.height(8.dp))
            Text(msg, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ContactSelection(
    contacts: List<Contact>,
    selectedContact: Contact?,
    onContactSelected: (Contact) -> Unit
) {
    if (contacts.isEmpty()) {
        Text("Nenhum contato disponível.")
        return
    }

    Text(
        text = "Destinatário: ${selectedContact?.name ?: "Selecione"}",
        style = MaterialTheme.typography.titleMedium
    )

    Spacer(Modifier.height(8.dp))

    LazyColumn {
        items(contacts) { contact ->
            ContactRow(
                contact = contact,
                isSelected = contact.id == selectedContact?.id,
                onClick = { onContactSelected(contact) }
            )
            Divider()
        }
    }
}

@Composable
private fun ContactRow(
    contact: Contact,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(text = contact.name, style = MaterialTheme.typography.bodyLarge)
            Text(text = "Conta: ${contact.accountNumber}")
            if (isSelected) {
                Text(
                    text = "Selecionado",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}
