package com.example.carteiradepagamentos.domain.repository

import com.example.carteiradepagamentos.domain.model.NetworkConfig
import kotlinx.coroutines.flow.Flow

interface AppPreferencesRepository {
    suspend fun getNetworkConfig(): NetworkConfig
    fun observeNetworkConfig(): Flow<NetworkConfig>
    suspend fun setNetworkConfig(config: NetworkConfig)
}
