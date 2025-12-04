package com.example.carteiradepagamentos.di

import com.example.carteiradepagamentos.data.local.SharedPrefsAppPreferencesRepository
import com.example.carteiradepagamentos.data.local.SharedPrefsAuthStorage
import com.example.carteiradepagamentos.data.local.SharedPrefsUserPreferencesRepository
import com.example.carteiradepagamentos.data.notification.AndroidNotifier
import com.example.carteiradepagamentos.domain.repository.AppPreferencesRepository
import com.example.carteiradepagamentos.domain.repository.UserPreferencesRepository
import com.example.carteiradepagamentos.domain.service.Notifier
import com.example.carteiradepagamentos.domain.storage.AuthStorage
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface AppModule {

    @Binds
    @Singleton
    fun bindNotifier(
        impl: AndroidNotifier
    ): Notifier

    @Binds
    @Singleton
    fun bindAuthStorage(
        impl: SharedPrefsAuthStorage
    ): AuthStorage
}

@Module
@InstallIn(SingletonComponent::class)
object AppProvidesModule {

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(
        impl: SharedPrefsUserPreferencesRepository
    ): UserPreferencesRepository = impl

    @Provides
    @Singleton
    fun provideAppPreferencesRepository(
        impl: SharedPrefsAppPreferencesRepository
    ): AppPreferencesRepository = impl
}
