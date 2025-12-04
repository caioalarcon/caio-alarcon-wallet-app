package com.example.carteiradepagamentos.di

import com.example.carteiradepagamentos.data.remote.AuthApi
import com.example.carteiradepagamentos.data.remote.AuthorizeApi
import com.example.carteiradepagamentos.data.remote.WalletApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
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
    fun provideRetrofit(): Retrofit {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        return Retrofit.Builder()
            // No emulador Android, acesse o Node em 192.168.1.110
            .baseUrl("http://192.168.1.110:3000/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

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
