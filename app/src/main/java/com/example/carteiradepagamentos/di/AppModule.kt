package com.example.carteiradepagamentos.di

import com.example.carteiradepagamentos.data.local.SharedPrefsAuthRepository
import com.example.carteiradepagamentos.data.memory.InMemoryWalletRepository
import com.example.carteiradepagamentos.domain.repository.AuthRepository
import com.example.carteiradepagamentos.domain.repository.WalletRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: SharedPrefsAuthRepository
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindWalletRepository(
        impl: InMemoryWalletRepository
    ): WalletRepository
}
