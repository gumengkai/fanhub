package com.fantok.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fantok.app.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "fantok_settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        private val SERVER_URL_KEY = stringPreferencesKey("server_url")
    }

    val serverUrl: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[SERVER_URL_KEY] ?: BuildConfig.DEFAULT_SERVER_URL }

    suspend fun saveServerUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[SERVER_URL_KEY] = url
        }
    }

    fun getServerUrl(): String {
        return try {
            val prefs = context.getSharedPreferences("fantok_settings", Context.MODE_PRIVATE)
            prefs.getString("server_url", null) ?: BuildConfig.DEFAULT_SERVER_URL
        } catch (e: Exception) {
            BuildConfig.DEFAULT_SERVER_URL
        }
    }

    suspend fun clearSettings() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}