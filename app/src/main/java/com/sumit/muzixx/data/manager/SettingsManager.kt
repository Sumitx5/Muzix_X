package com.sumit.muzixx.data.manager

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "muzixx_settings")

class SettingsManager(private val context: Context) {

    companion object {
        val STREAM_WIFI_ONLY = booleanPreferencesKey("stream_wifi_only")
        val DOWNLOAD_WIFI_ONLY = booleanPreferencesKey("download_wifi_only")
        val SHOW_LYRICS = booleanPreferencesKey("show_lyrics")
        val NORMALIZE_AUDIO = booleanPreferencesKey("normalize_audio")
        val SKIP_SILENCE = booleanPreferencesKey("skip_silence")
        val CHECK_UPDATES_ON_START = booleanPreferencesKey("check_updates_on_start")
        val AUDIO_QUALITY = stringPreferencesKey("audio_quality")
        val USER_NAME = stringPreferencesKey("user_name")
        val APP_THEME = stringPreferencesKey("app_theme")
    }

    val streamWifiOnlyFlow: Flow<Boolean> = context.dataStore.data.map { it[STREAM_WIFI_ONLY] ?: false }
    val downloadWifiOnlyFlow: Flow<Boolean> = context.dataStore.data.map { it[DOWNLOAD_WIFI_ONLY] ?: true }
    val showLyricsFlow: Flow<Boolean> = context.dataStore.data.map { it[SHOW_LYRICS] ?: true }
    val normalizeAudioFlow: Flow<Boolean> = context.dataStore.data.map { it[NORMALIZE_AUDIO] ?: true }
    val skipSilenceFlow: Flow<Boolean> = context.dataStore.data.map { it[SKIP_SILENCE] ?: false }
    val checkUpdatesOnStartFlow: Flow<Boolean> = context.dataStore.data.map { it[CHECK_UPDATES_ON_START] ?: false }
    val audioQualityFlow: Flow<String> = context.dataStore.data.map { it[AUDIO_QUALITY] ?: "320kbps" }
    val userNameFlow: Flow<String> = context.dataStore.data.map { it[USER_NAME] ?: "User" }
    val appThemeFlow: Flow<String> = context.dataStore.data.map {it[APP_THEME] ?: "Default"}

    suspend fun saveBooleanSetting(key: Preferences.Key<Boolean>, value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    suspend fun saveStringSetting(key: Preferences.Key<String>, value: String) {
        context.dataStore.edit { preferences ->
            preferences[key] = value
        }
    }
}