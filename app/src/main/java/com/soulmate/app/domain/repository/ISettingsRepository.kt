package com.soulmate.app.domain.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "soulmate_settings")

interface ISettingsRepository {
    val notificationEnabled: Flow<Boolean>
    suspend fun toggleNotification(isEnabled: Boolean)

    val isDarkMode: Flow<Boolean>
    suspend fun toggleDarkMode(isDark: Boolean)
}

class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ISettingsRepository {

    private object PreferencesKeys {
        val NOTIFICATION_ENABLED = booleanPreferencesKey("notification_enabled")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
    }

    override val notificationEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_ENABLED] ?: true
        }

    override val isDarkMode: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.DARK_MODE] ?: false
        }

    override suspend fun toggleNotification(isEnabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_ENABLED] = isEnabled
        }
    }

    override suspend fun toggleDarkMode(isDark: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DARK_MODE] = isDark
        }
    }
}