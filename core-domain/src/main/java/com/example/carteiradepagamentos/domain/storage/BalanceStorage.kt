package com.example.carteiradepagamentos.domain.storage

interface BalanceStorage {
    fun loadBalance(): Long
    fun saveBalance(newBalance: Long)
}
