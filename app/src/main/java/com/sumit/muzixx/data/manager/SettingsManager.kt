package com.sumit.muzixx.data.manager

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
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
        val USER_GENDER = stringPreferencesKey("user_gender")
        val APP_THEME = stringPreferencesKey("app_theme")

        //Equalizer PERSISTENCE Keys
        val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        val EQ_PRESET_INDEX = intPreferencesKey("eq_preset_index")
        val BASS_ENABLED = booleanPreferencesKey("bass_enabled")
        val BASS_STRENGTH = floatPreferencesKey("bass_strength")
        val EQ_BANDS_STRING = stringPreferencesKey("eq_bands_string")
    }

    val streamWifiOnlyFlow: Flow<Boolean> = context.dataStore.data.map { it[STREAM_WIFI_ONLY] ?: false }
    val downloadWifiOnlyFlow: Flow<Boolean> = context.dataStore.data.map { it[DOWNLOAD_WIFI_ONLY] ?: true }
    val showLyricsFlow: Flow<Boolean> = context.dataStore.data.map { it[SHOW_LYRICS] ?: true }
    val normalizeAudioFlow: Flow<Boolean> = context.dataStore.data.map { it[NORMALIZE_AUDIO] ?: true }
    val skipSilenceFlow: Flow<Boolean> = context.dataStore.data.map { it[SKIP_SILENCE] ?: false }
    val checkUpdatesOnStartFlow: Flow<Boolean> = context.dataStore.data.map { it[CHECK_UPDATES_ON_START] ?: false }
    val audioQualityFlow: Flow<String> = context.dataStore.data.map { it[AUDIO_QUALITY] ?: "320kbps" }
    val userNameFlow: Flow<String> = context.dataStore.data.map { it[USER_NAME] ?: "User" }
    val appThemeFlow: Flow<String> = context.dataStore.data.map { it[APP_THEME] ?: "Neon Red" }
    val userGenderFlow: Flow<String> = context.dataStore.data.map { it[USER_GENDER] ?: "Prefer Not to Say" }
    val eqEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[EQ_ENABLED] ?: false }
    val eqPresetIndexFlow: Flow<Int> = context.dataStore.data.map { it[EQ_PRESET_INDEX] ?: 0 }
    val bassEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[BASS_ENABLED] ?: false }
    val bassStrengthFlow: Flow<Float> = context.dataStore.data.map { it[BASS_STRENGTH] ?: 0.0f }
    val eqBandsFlow: Flow<List<Float>> = context.dataStore.data.map { prefs ->
        val rawStr = prefs[EQ_BANDS_STRING] ?: "0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0"
        rawStr.split(",").map { it.toFloatOrNull() ?: 0.0f }
    }

    suspend fun saveBooleanSetting(key: Preferences.Key<Boolean>, value: Boolean) {
        context.dataStore.edit { preferences -> preferences[key] = value }
    }

    suspend fun saveStringSetting(key: Preferences.Key<String>, value: String) {
        context.dataStore.edit { preferences -> preferences[key] = value }
    }

    suspend fun saveIntSetting(key: Preferences.Key<Int>, value: Int) {
        context.dataStore.edit { preferences -> preferences[key] = value }
    }

    suspend fun saveFloatSetting(key: Preferences.Key<Float>, value: Float) {
        context.dataStore.edit { preferences -> preferences[key] = value }
    }

    suspend fun saveEqBands(bands: List<Float>) {
        val serialized = bands.joinToString(",") { it.toString() }
        context.dataStore.edit { preferences -> preferences[EQ_BANDS_STRING] = serialized }
    }
}