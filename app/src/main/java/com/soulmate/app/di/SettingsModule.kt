package com.soulmate.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.soulmate.app.domain.repository.ISettingsRepository
import com.soulmate.app.domain.repository.SettingsRepositoryImpl
import com.soulmate.app.domain.repository.dataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SettingsModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(dataStore: DataStore<Preferences>): ISettingsRepository {
        return SettingsRepositoryImpl(dataStore)
    }
}