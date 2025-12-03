package com.example.carteiradepagamentos.di

import com.example.carteiradepagamentos.data.remote.AuthApi
import com.example.carteiradepagamentos.data.remote.AuthorizeApi
import com.example.carteiradepagamentos.data.remote.WalletApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit =
        Retrofit.Builder()
            // No emulador Android, acesse o Node em 10.0.2.2
            .baseUrl("http://10.0.2.2:3000/")
            .addConverterFactory(MoshiConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi =
        retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideWalletApi(retrofit: Retrofit): WalletApi =
        retrofit.create(WalletApi::class.java)

    @Provides
    @Singleton
    fun provideAuthorizeApi(retrofit: Retrofit): AuthorizeApi =
        retrofit.create(AuthorizeApi::class.java)
}
