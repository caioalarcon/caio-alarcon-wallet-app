package com.example.carteiradepagamentos.di

import com.example.carteiradepagamentos.data.local.SharedPrefsAuthRepository
import com.example.carteiradepagamentos.data.remote.NetworkAuthRemoteDataSource
import com.example.carteiradepagamentos.data.remote.NetworkAuthorizeService
import com.example.carteiradepagamentos.data.remote.NetworkWalletRepository
import com.example.carteiradepagamentos.domain.repository.AuthRepository
import com.example.carteiradepagamentos.domain.repository.WalletRepository
import com.example.carteiradepagamentos.domain.service.AuthRemoteDataSource
import com.example.carteiradepagamentos.domain.service.AuthorizeService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindAuthRemoteDataSource(
        impl: NetworkAuthRemoteDataSource
    ): AuthRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: SharedPrefsAuthRepository
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindWalletRepository(
        impl: NetworkWalletRepository
    ): WalletRepository

    @Binds
    @Singleton
    abstract fun bindAuthorizeService(
        impl: NetworkAuthorizeService
    ): AuthorizeService
}
