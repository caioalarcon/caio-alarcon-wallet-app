package com.example.carteiradepagamentos.data.local

import android.content.Context
import androidx.core.content.edit
import com.example.carteiradepagamentos.domain.model.NetworkConfig
import com.example.carteiradepagamentos.domain.repository.AppPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val DEFAULT_BASE_URL = "http://192.168.1.110:3000/"

@Singleton
class SharedPrefsAppPreferencesRepository @Inject constructor(
    @ApplicationContext context: Context
) : AppPreferencesRepository {

    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(readConfig())

    override fun observeNetworkConfig(): Flow<NetworkConfig> = state.asStateFlow()

    override suspend fun getNetworkConfig(): NetworkConfig = state.value

    override suspend fun setNetworkConfig(config: NetworkConfig) {
        state.value = config
        prefs.edit(commit = true) {
            putBoolean("use_remote_server", config.useRemoteServer)
            putString("remote_base_url", config.baseUrl)
        }
    }

    private fun readConfig(): NetworkConfig {
        val useRemote = prefs.getBoolean("use_remote_server", false)
        val baseUrl = prefs.getString("remote_base_url", DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
        return NetworkConfig(
            useRemoteServer = useRemote,
            baseUrl = baseUrl
        )
    }
}
