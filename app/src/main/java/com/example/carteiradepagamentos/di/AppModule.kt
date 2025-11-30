package com.example.carteiradepagamentos.di

import com.example.carteiradepagamentos.data.local.SharedPrefsAuthRepository
import com.example.carteiradepagamentos.data.local.SharedPrefsAuthStorage
import com.example.carteiradepagamentos.data.memory.InMemoryBalanceStorage
import com.example.carteiradepagamentos.data.memory.InMemoryWalletRepository
import com.example.carteiradepagamentos.data.notification.AndroidNotifier
import com.example.carteiradepagamentos.data.service.FakeAuthorizeService
import com.example.carteiradepagamentos.domain.repository.AuthRepository
import com.example.carteiradepagamentos.domain.repository.WalletRepository
import com.example.carteiradepagamentos.domain.service.AuthorizeService
import com.example.carteiradepagamentos.domain.service.Notifier
import com.example.carteiradepagamentos.domain.storage.AuthStorage
import com.example.carteiradepagamentos.domain.storage.BalanceStorage
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

    @Binds
    @Singleton
    abstract fun bindNotifier(
        impl: AndroidNotifier
    ): Notifier

    @Binds
    @Singleton
    abstract fun bindAuthStorage(
        impl: SharedPrefsAuthStorage
    ): AuthStorage

    @Binds
    @Singleton
    abstract fun bindAuthorizeService(
        impl: FakeAuthorizeService
    ): AuthorizeService

    @Binds
    @Singleton
    abstract fun bindBalanceStorage(
        impl: InMemoryBalanceStorage
    ): BalanceStorage
}
