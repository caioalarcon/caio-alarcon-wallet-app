package com.example.carteiradepagamentos.data.memory

import com.example.carteiradepagamentos.domain.storage.BalanceStorage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryBalanceStorage @Inject constructor() : BalanceStorage {

    private var balanceInCents: Long = 100_000L

    override fun loadBalance(): Long = balanceInCents

    override fun saveBalance(newBalance: Long) {
        balanceInCents = newBalance
    }
}
