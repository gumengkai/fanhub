package com.fanhub.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fanhub.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "fanhub_settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val SERVER_URL      = stringPreferencesKey("server_url")
        val AUTO_PLAY       = booleanPreferencesKey("auto_play")
        val LOOP_VIDEO      = booleanPreferencesKey("loop_video")
        val REMEMBER_PROGRESS = booleanPreferencesKey("remember_progress")
    }

    val serverUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.SERVER_URL] ?: BuildConfig.DEFAULT_SERVER_URL
    }

    val autoPlay: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.AUTO_PLAY] ?: true
    }

    val loopVideo: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.LOOP_VIDEO] ?: true
    }

    val rememberProgress: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.REMEMBER_PROGRESS] ?: true
    }

    suspend fun saveServerUrl(url: String) {
        context.dataStore.edit { it[Keys.SERVER_URL] = url.trimEnd('/') }
    }

    suspend fun saveAutoPlay(value: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_PLAY] = value }
    }

    suspend fun saveLoopVideo(value: Boolean) {
        context.dataStore.edit { it[Keys.LOOP_VIDEO] = value }
    }

    suspend fun saveRememberProgress(value: Boolean) {
        context.dataStore.edit { it[Keys.REMEMBER_PROGRESS] = value }
    }
}
