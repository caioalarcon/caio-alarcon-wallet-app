package com.example.carteiradepagamentos.feature.transfer

import com.example.carteiradepagamentos.MainDispatcherRule
import com.example.carteiradepagamentos.domain.model.AccountSummary
import com.example.carteiradepagamentos.domain.model.Contact
import com.example.carteiradepagamentos.domain.repository.WalletRepository
import com.example.carteiradepagamentos.domain.service.Notifier
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class TransferViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    class FakeNotifier : Notifier {
        var lastNotification: Pair<Contact, Long>? = null
        override fun notifyTransferSuccess(contact: Contact, amountInCents: Long) {
            lastNotification = contact to amountInCents
        }
    }

    class FakeWalletRepository(
        private var accountSummary: AccountSummary,
        private val contacts: List<Contact>,
        private var transferResult: Result<AccountSummary>? = null
    ) : WalletRepository {
        override suspend fun getAccountSummary(): AccountSummary = accountSummary

        override suspend fun getContacts(): List<Contact> = contacts

        override suspend fun transfer(toContactId: String, amountInCents: Long): Result<AccountSummary> {
            val result = transferResult
            return if (result != null) {
                result
            } else {
                val updatedSummary = AccountSummary(accountSummary.balanceInCents - amountInCents)
                accountSummary = updatedSummary
                Result.success(updatedSummary)
            }
        }

        fun setTransferResult(result: Result<AccountSummary>) {
            transferResult = result
        }
    }

    private val contacts = listOf(Contact(id = "1", name = "Alice", accountNumber = "0001-1"))

    @Test
    fun `onConfirmTransfer without selected contact shows error`() = runTest {
        val notifier = FakeNotifier()
        val repository = FakeWalletRepository(AccountSummary(1_000), emptyList())
        val viewModel = TransferViewModel(repository, notifier)

        advanceUntilIdle()

        viewModel.onConfirmTransfer()

        val state = viewModel.uiState.value
        assertEquals("Selecione um contato", state.errorDialogData?.message)
    }

    @Test
    fun `onConfirmTransfer with invalid amount shows error`() = runTest {
        val notifier = FakeNotifier()
        val repository = FakeWalletRepository(AccountSummary(1_000), contacts)
        val viewModel = TransferViewModel(repository, notifier)

        advanceUntilIdle()

        viewModel.onAmountChanged("")
        viewModel.onConfirmTransfer()

        val state = viewModel.uiState.value
        assertEquals("Valor inválido", state.errorDialogData?.message)
    }

    @Test
    fun `onConfirmTransfer success updates state and notifies`() = runTest {
        val notifier = FakeNotifier()
        val repository = FakeWalletRepository(AccountSummary(10_000), contacts)
        val viewModel = TransferViewModel(repository, notifier)

        advanceUntilIdle()

        viewModel.onAmountChanged("1500")
        viewModel.onConfirmTransfer()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val successData = state.successDialogData
        assertEquals("Alice", successData?.contactName)
        assertEquals("0001-1", successData?.contactAccount)
        assertEquals("R$ 15,00", successData?.amountText)
        assertEquals("R$ 0,00", state.amountInput)
        assertEquals(contacts.first() to 1_500L, notifier.lastNotification)
    }

    @Test
    fun `onConfirmTransfer with authorization denied shows policy message`() = runTest {
        val notifier = FakeNotifier()
        val repository = FakeWalletRepository(AccountSummary(50_000), contacts)
        repository.setTransferResult(Result.failure(IllegalStateException("operation not allowed")))
        val viewModel = TransferViewModel(repository, notifier)

        advanceUntilIdle()

        viewModel.onAmountChanged("40300")
        viewModel.onConfirmTransfer()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Transferência bloqueada por política de segurança (valor R$ 403,00)", state.errorDialogData?.message)
        assertNull(notifier.lastNotification)
    }

    @Test
    fun `onConfirmTransfer with insufficient balance shows error`() = runTest {
        val notifier = FakeNotifier()
        val repository = FakeWalletRepository(AccountSummary(1_000), contacts)
        repository.setTransferResult(Result.failure(IllegalStateException("Saldo insuficiente")))
        val viewModel = TransferViewModel(repository, notifier)

        advanceUntilIdle()

        viewModel.onAmountChanged("2000")
        viewModel.onConfirmTransfer()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Saldo insuficiente", state.errorDialogData?.message)
        assertNull(notifier.lastNotification)
    }
}
