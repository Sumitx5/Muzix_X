package com.sumit.muzixx.data.repository

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sumit.muzixx.data.manager.SettingsManager // Ensure this import is here!
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsRepository(
    context: Context,
    private val externalScope: CoroutineScope
) {
    private val settingsManager = SettingsManager(context.applicationContext)

    // CORE APPLICATION STATES
    var streamWifiOnly by mutableStateOf(false)
        private set
    var downloadWifiOnly by mutableStateOf(true)
        private set
    var showLyrics by mutableStateOf(true)
        private set
    var normalizeAudio by mutableStateOf(true)
        private set
    var skipSilence by mutableStateOf(false)
        private set
    var checkUpdatesOnStart by mutableStateOf(true)
        private set
    var audioQuality by mutableStateOf("320kbps")
        private set
    var userName by mutableStateOf("User")
        private set
    var userGender by mutableStateOf("Prefer Not to Say")
    var appTheme by mutableStateOf("Neon Red")
        private set

    // EXTRA EQUALIZER
    var eqEnabled by mutableStateOf(false)
        private set
    var eqPresetIndex by mutableIntStateOf(0)
        private set
    var bassEnabled by mutableStateOf(false)
        private set
    var bassStrength by mutableFloatStateOf(0.0f)
        private set
    val eqBands = mutableStateListOf<Float>()

    init {
        externalScope.launch(Dispatchers.IO) {
            launch {
                settingsManager.streamWifiOnlyFlow.collectLatest { value ->
                    withContext(Dispatchers.Main) { streamWifiOnly = value }
                }
            }
            launch {
                settingsManager.downloadWifiOnlyFlow.collectLatest { value ->
                    withContext(Dispatchers.Main) { downloadWifiOnly = value }
                }
            }
            launch {
                settingsManager.showLyricsFlow.collectLatest { value ->
                    withContext(Dispatchers.Main) { showLyrics = value }
                }
            }
            launch {
                settingsManager.normalizeAudioFlow.collectLatest { value ->
                    withContext(Dispatchers.Main) { normalizeAudio = value }
                }
            }
            launch {
                settingsManager.skipSilenceFlow.collectLatest { value ->
                    withContext(Dispatchers.Main) { skipSilence = value }
                }
            }
            launch {
                settingsManager.checkUpdatesOnStartFlow.collectLatest { value ->
                    withContext(Dispatchers.Main) { checkUpdatesOnStart = value }
                }
            }
            launch {
                settingsManager.audioQualityFlow.collectLatest { value ->
                    withContext(Dispatchers.Main) { audioQuality = value }
                }
            }
            launch {
                settingsManager.userNameFlow.collectLatest { value ->
                    withContext(Dispatchers.Main) { userName = value }
                }
            }
            launch {
                settingsManager.userGenderFlow.collectLatest { value ->
                    withContext(Dispatchers.Main) { userGender = value }
                }
            }
            launch {
                settingsManager.appThemeFlow.collectLatest { value ->
                    withContext(Dispatchers.Main) { appTheme = value }
                }
            }

            //Collect Equalizer States
            launch {
                settingsManager.eqEnabledFlow.collectLatest { value ->
                    withContext(Dispatchers.Main) { eqEnabled = value }
                }
            }
            launch {
                settingsManager.eqPresetIndexFlow.collectLatest { value ->
                    withContext(Dispatchers.Main) { eqPresetIndex = value }
                }
            }
            launch {
                settingsManager.bassEnabledFlow.collectLatest { value ->
                    withContext(Dispatchers.Main) { bassEnabled = value }
                }
            }
            launch {
                settingsManager.bassStrengthFlow.collectLatest { value ->
                    withContext(Dispatchers.Main) { bassStrength = value }
                }
            }
            launch {
                settingsManager.eqBandsFlow.collectLatest { values ->
                    withContext(Dispatchers.Main) {
                        eqBands.clear()
                        eqBands.addAll(values)
                    }
                }
            }
        }
    }

    // THREAD-SAFE UPDATER FUNCTIONS
    fun updateStreamWifiOnly(value: Boolean) {
        externalScope.launch(Dispatchers.IO) { settingsManager.saveBooleanSetting(SettingsManager.STREAM_WIFI_ONLY, value) }
    }

    fun updateDownloadWifiOnly(value: Boolean) {
        externalScope.launch(Dispatchers.IO) { settingsManager.saveBooleanSetting(SettingsManager.DOWNLOAD_WIFI_ONLY, value) }
    }

    fun updateShowLyrics(value: Boolean) {
        externalScope.launch(Dispatchers.IO) { settingsManager.saveBooleanSetting(SettingsManager.SHOW_LYRICS, value) }
    }

    fun updateNormalizeAudio(value: Boolean) {
        externalScope.launch(Dispatchers.IO) { settingsManager.saveBooleanSetting(SettingsManager.NORMALIZE_AUDIO, value) }
    }

    fun updateSkipSilence(value: Boolean) {
        externalScope.launch(Dispatchers.IO) { settingsManager.saveBooleanSetting(SettingsManager.SKIP_SILENCE, value) }
    }

    fun updateCheckUpdatesOnStart(value: Boolean) {
        externalScope.launch(Dispatchers.IO) { settingsManager.saveBooleanSetting(SettingsManager.CHECK_UPDATES_ON_START, value) }
    }

    fun updateAudioQuality(value: String) {
        externalScope.launch(Dispatchers.IO) { settingsManager.saveStringSetting(SettingsManager.AUDIO_QUALITY, value) }
    }

    fun updateUserName(value: String) {
        externalScope.launch(Dispatchers.IO) { settingsManager.saveStringSetting(SettingsManager.USER_NAME, value) }
    }
    fun updateUserGender(value: String) {
        externalScope.launch(Dispatchers.IO) { settingsManager.saveStringSetting(SettingsManager.USER_GENDER, value) }
    }

    fun updateAppTheme(value: String) {
        externalScope.launch(Dispatchers.IO) { settingsManager.saveStringSetting(SettingsManager.APP_THEME, value) }
    }
    fun updateEqEnabled(value: Boolean, onHardwareUpdate: (Boolean) -> Unit) {
        externalScope.launch(Dispatchers.IO) {
            settingsManager.saveBooleanSetting(SettingsManager.EQ_ENABLED, value)
            withContext(Dispatchers.Main) { onHardwareUpdate(value) }
        }
    }

    fun updateEqPresetIndex(value: Int, onHardwareUpdate: (Short) -> Unit) {
        externalScope.launch(Dispatchers.IO) {
            settingsManager.saveIntSetting(SettingsManager.EQ_PRESET_INDEX, value)
            withContext(Dispatchers.Main) { onHardwareUpdate(value.toShort()) }
        }
    }

    fun updateBassEnabled(value: Boolean, onHardwareUpdate: (Boolean) -> Unit) {
        externalScope.launch(Dispatchers.IO) {
            settingsManager.saveBooleanSetting(SettingsManager.BASS_ENABLED, value)
            withContext(Dispatchers.Main) { onHardwareUpdate(value) }
        }
    }

    fun updateBassStrength(value: Float, onHardwareUpdate: (Float) -> Unit) {
        externalScope.launch(Dispatchers.IO) {
            settingsManager.saveFloatSetting(SettingsManager.BASS_STRENGTH, value)
            withContext(Dispatchers.Main) { onHardwareUpdate(value) }
        }
    }

    fun updateSingleBand(index: Int, dbValue: Float, onHardwareUpdate: (Int, Float) -> Unit) {
        if (index in 0 until eqBands.size) {
            eqBands[index] = dbValue
        }
        val bandsSnapshot = eqBands.toList()
        externalScope.launch(Dispatchers.IO) {
            try {
                settingsManager.saveEqBands(bandsSnapshot)

                withContext(Dispatchers.Main) {
                    onHardwareUpdate(index, dbValue)
                }
            } catch (e: Exception) {
                Log.e("SETTINGS_REPO", "Failed to save band array parameters to disk storage", e)
            }
        }
    }
}