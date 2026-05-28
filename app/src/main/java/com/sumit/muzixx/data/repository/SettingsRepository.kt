package com.sumit.muzixx.data.repository

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sumit.muzixx.data.manager.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SettingsRepository(
    context: Context,
    private val externalScope: CoroutineScope
) {
    private val settingsManager = SettingsManager(context.applicationContext)

    //STATES
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

    init {
        // Collect individual preferences asynchronously on initialization
        externalScope.launch(Dispatchers.IO) {
            launch { settingsManager.streamWifiOnlyFlow.collectLatest { streamWifiOnly = it } }
            launch { settingsManager.downloadWifiOnlyFlow.collectLatest { downloadWifiOnly = it } }
            launch { settingsManager.showLyricsFlow.collectLatest { showLyrics = it } }
            launch { settingsManager.normalizeAudioFlow.collectLatest { normalizeAudio = it } }
            launch { settingsManager.skipSilenceFlow.collectLatest { skipSilence = it } }
            launch { settingsManager.checkUpdatesOnStartFlow.collectLatest { checkUpdatesOnStart = it } }
            launch { settingsManager.audioQualityFlow.collectLatest { audioQuality = it } }
            launch { settingsManager.userNameFlow.collectLatest { userName = it } }
        }
    }

    //THREAD-SAFE UPDATER
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
        externalScope.launch(Dispatchers.IO) {
            settingsManager.saveStringSetting(SettingsManager.AUDIO_QUALITY, value)
        }
    }

    fun updateUserName(value: String) {
        externalScope.launch(Dispatchers.IO) {
            settingsManager.saveStringSetting(SettingsManager.USER_NAME, value)
        }
    }
}