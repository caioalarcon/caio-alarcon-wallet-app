package com.example.carteiradepagamentos.feature.transfer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import com.example.carteiradepagamentos.domain.model.Contact

@Composable
fun TransferScreen(
    contactId: String?,
    onBackToHome: () -> Unit,
    viewModel: TransferViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val dialogVisible = uiState.successDialogData != null || uiState.errorDialogData != null
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val notificationsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* ignored: apenas habilita push local */ }

    BackHandler(enabled = !dialogVisible, onBack = onBackToHome)

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

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                notificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
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
        TransferOutcomeDialog(
            title = "Transferência enviada",
            message = null,
            amountText = successData.amountText,
            contactName = successData.contactName,
            contactAccount = successData.contactAccount,
            confirmLabel = "OK",
            onConfirm = {
                viewModel.clearSuccessDialog()
                onBackToHome()
            }
        )
    }

    uiState.errorDialogData?.let { errorData ->
        TransferOutcomeDialog(
            title = "Erro na transferência",
            message = errorData.message,
            amountText = errorData.amountText,
            contactName = errorData.contactName,
            contactAccount = errorData.contactAccount,
            confirmLabel = "Tentar novamente",
            onConfirm = {
                viewModel.clearErrorDialog()
                viewModel.reload()
            },
            dismissLabel = "Voltar",
            onDismiss = {
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
private fun TransferOutcomeDialog(
    title: String,
    message: String?,
    amountText: String?,
    contactName: String?,
    contactAccount: String?,
    confirmLabel: String,
    onConfirm: () -> Unit,
    dismissLabel: String? = null,
    onDismiss: (() -> Unit)? = null,
) {
    val handleDismiss = onDismiss ?: onConfirm
    Dialog(onDismissRequest = handleDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                TransferDialogContent(
                    message = message,
                    amountText = amountText,
                    contactName = contactName,
                    contactAccount = contactAccount
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    dismissLabel?.let {
                        TextButton(onClick = handleDismiss) { Text(it) }
                        Spacer(Modifier.width(8.dp))
                    }
                    Button(onClick = onConfirm) { Text(confirmLabel) }
                }
            }
        }
    }
}

@Composable
private fun TransferDialogContent(
    message: String?,
    amountText: String?,
    contactName: String?,
    contactAccount: String?,
) {
    Column {
        message?.let {
            Text(it)
            if (amountText != null || !contactName.isNullOrBlank() || !contactAccount.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
            }
        }
        amountText?.let {
            Text("Valor: $it")
        }
        if (!contactName.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text("Destinatário: $contactName")
        }
        if (!contactAccount.isNullOrBlank()) {
            Text("Conta: $contactAccount")
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
