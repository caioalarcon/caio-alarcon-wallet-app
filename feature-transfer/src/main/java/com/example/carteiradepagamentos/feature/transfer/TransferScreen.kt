package com.example.carteiradepagamentos.feature.transfer

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.carteiradepagamentos.domain.model.Contact

@Composable
fun TransferScreen(
    contactId: String?,
    onBackToHome: () -> Unit,
    viewModel: TransferViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val successDialogVisible = uiState.successDialogData != null
    val lifecycleOwner = LocalLifecycleOwner.current

    BackHandler(enabled = !successDialogVisible, onBack = onBackToHome)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                viewModel.reload()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(contactId, uiState.contacts) {
        if (contactId != null && uiState.contacts.isNotEmpty()) {
            val target = uiState.contacts.find { it.id == contactId }
            if (target != null && uiState.selectedContact?.id != contactId) {
                viewModel.onContactSelected(target)
            }
        }
    }

    // Só navega, sem trocar layout antes
    // Mantém sempre o mesmo layout na tela
    TransferContent(
        uiState = uiState,
        onAmountChange = viewModel::onAmountChanged,
        onConfirmTransfer = viewModel::onConfirmTransfer,
        onContactSelected = viewModel::onContactSelected,
        onBack = onBackToHome
    )

    uiState.successDialogData?.let { successData ->
        TransferSuccessDialog(
            successData = successData,
            onConfirm = {
                viewModel.clearSuccessDialog()
                onBackToHome()
            }
        )
    }

    uiState.errorDialogData?.let { errorData ->
        TransferErrorDialog(
            message = errorData.message,
            onRetry = {
                viewModel.clearErrorDialog()
                viewModel.reload()
            },
            onCancel = {
                viewModel.clearErrorDialog()
                onBackToHome()
            }
        )
    }
}

@Composable
fun TransferContent(
    uiState: TransferUiState,
    onAmountChange: (String) -> Unit,
    onConfirmTransfer: () -> Unit,
    onContactSelected: (Contact) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Transferência", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onBack) {
                Text("Voltar")
            }
        }

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
            label = { Text("Valor") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onConfirmTransfer,
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enviar")
        }
    }
}

@Composable
private fun TransferSuccessDialog(
    successData: TransferSuccessData,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onConfirm,
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("OK")
            }
        },
        title = { Text("Transferência enviada") },
        text = {
            Column {
                Text("Valor: ${successData.amountText}")
                Spacer(Modifier.height(4.dp))
                Text("Destinatário: ${successData.contactName}")
                Text("Conta: ${successData.contactAccount}")
            }
        }
    )
}

@Composable
private fun TransferErrorDialog(
    message: String,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onRetry,
        confirmButton = {
            Button(onClick = onRetry) { Text("Tentar novamente") }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancelar") }
        },
        title = { Text("Erro na transferência") },
        text = { Text(message) }
    )
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
