package com.sumit.muzixx.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.audiofx.BassBoost
import android.media.audiofx.LoudnessEnhancer
import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.sumit.muzixx.data.manager.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PlaybackService : MediaSessionService() {

    companion object {
        private const val TAG = "PlaybackService"
        private const val CHANNEL_ID = "muzixx_playback_channel"
        private const val NOTIFICATION_ID = 1001
    }

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var settingsManager: SettingsManager
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var equalizer: android.media.audiofx.Equalizer? = null
    private var bassBoost: BassBoost? = null

    private var normalizationEnabled = false

    private val callback = @UnstableApi
    object : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
            val customActions = listOf(
                "ACTION_SET_SKIP_SILENCE",
                "ACTION_SET_NORMALIZATION",
                "ACTION_SET_EQ_ENABLED",
                "ACTION_SET_EQ_BAND",
                "ACTION_SET_EQUALIZER_PRESET",
                "ACTION_SET_BASS_ENABLED",
                "ACTION_SET_BASS_STRENGTH"
            )

            customActions.forEach { action ->
                sessionCommands.add(androidx.media3.session.SessionCommand(action, android.os.Bundle.EMPTY))
            }

            return MediaSession.ConnectionResult.accept(
                sessionCommands.build(),
                MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS
            )
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: androidx.media3.session.SessionCommand,
            args: android.os.Bundle
        ): ListenableFuture<androidx.media3.session.SessionResult> {

            when (customCommand.customAction) {
                "ACTION_SET_SKIP_SILENCE" -> {
                    val enabled = args.getBoolean("enabled", false)
                    player.skipSilenceEnabled = enabled
                    Log.d(TAG, "Live update: Skip Silence set to $enabled")
                }
                "ACTION_SET_NORMALIZATION" -> {
                    val enabled = args.getBoolean("enabled", false)
                    normalizationEnabled = enabled
                    applyNormalizationState(enabled)
                    Log.d(TAG, "Live update: Audio Normalization set to $enabled")
                }
                "ACTION_SET_EQ_ENABLED" -> {
                    val enabled = args.getBoolean("enabled", true)
                    equalizer?.enabled = enabled
                    Log.d(TAG, "Live hardware patch: Equalizer toggle set to $enabled")
                    serviceScope.launch {
                        settingsManager.saveBooleanSetting(SettingsManager.EQ_ENABLED, enabled)
                    }
                }
                "ACTION_SET_EQ_BAND" -> {
                    val bandIndex = args.getInt("band_index", -1)
                    val dbValue = args.getFloat("db_value", 0f)

                    serviceScope.launch {
                        val currentBands = settingsManager.eqBandsFlow.first().toMutableList()
                        if (bandIndex in 0 until currentBands.size) {
                            currentBands[bandIndex] = dbValue
                            settingsManager.saveEqBands(currentBands)
                        }
                    }

                    equalizer?.let { eq ->
                        val availableBands = eq.numberOfBands.toInt()
                        if (bandIndex in 0 until availableBands) {
                            try {
                                val milliBels = (dbValue * 100).toInt().coerceIn(-1500, 1500).toShort()
                                eq.setBandLevel(bandIndex.toShort(), milliBels)
                                Log.d(TAG, "Hardware Band Update -> Index: $bandIndex, mB: $milliBels")
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed updating band hardware parameters configuration", e)
                            }
                        } else {
                            Log.w(TAG, "Ignored out-of-bounds band: Index $bandIndex requested, but hardware only supports $availableBands bands.")
                        }
                    }
                }
                "ACTION_SET_EQUALIZER_PRESET" -> {
                    val presetIndex = args.getShort("preset_index", -1)
                    if (presetIndex != (-1).toShort() && equalizer != null) {
                        try {
                            if (presetIndex < (equalizer?.numberOfPresets ?: 0)) {
                                equalizer?.usePreset(presetIndex)
                                Log.d(TAG, "Hardware preset initialized successfully: Index $presetIndex")

                                serviceScope.launch {
                                    settingsManager.saveIntSetting(SettingsManager.EQ_PRESET_INDEX, presetIndex.toInt())
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Driver rejected specific native structural EQ preset allocation", e)
                        }
                    }
                }
                "ACTION_SET_BASS_ENABLED" -> {
                    val enabled = args.getBoolean("enabled", false)
                    bassBoost?.enabled = enabled
                    Log.d(TAG, "Live hardware patch: Bass Boost set to $enabled")

                    serviceScope.launch {
                        settingsManager.saveBooleanSetting(SettingsManager.BASS_ENABLED, enabled)
                    }
                }
                "ACTION_SET_BASS_STRENGTH" -> {
                    val strengthPercent = args.getFloat("strength_percent", 0.0f)
                    if (bassBoost != null) {
                        try {
                            val hardwareStrength = (strengthPercent * 1000).toInt().toShort()
                            bassBoost?.setStrength(hardwareStrength)
                            Log.d(TAG, "Hardware Bass Boost Strength adjusted: $hardwareStrength/1000")

                            serviceScope.launch {
                                settingsManager.saveFloatSetting(SettingsManager.BASS_STRENGTH, strengthPercent)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed pushing live Bass Boost parameter update", e)
                        }
                    }
                }
            }

            return Futures.immediateFuture(
                androidx.media3.session.SessionResult(androidx.media3.session.SessionResult.RESULT_SUCCESS)
            )
        }

        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val items = (0 until player.mediaItemCount).map { player.getMediaItemAt(it) }
            return Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(items, player.currentMediaItemIndex, player.currentPosition)
            )
        }
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        createChannel()
        val cachingDataSourceFactory = com.sumit.muzixx.data.manager.MuzixCacheManager.createCacheDataSourceFactory(this)
        val mediaSourceFactory = DefaultMediaSourceFactory(cachingDataSourceFactory)
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(5000, 15000, 1500, 3000)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .setLoadControl(loadControl)
            .build()

        player.repeatMode = ExoPlayer.REPEAT_MODE_OFF
        player.shuffleModeEnabled = false
        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                super.onAudioSessionIdChanged(audioSessionId)
                initializeEffectsPipeline(audioSessionId)
            }
        })

        settingsManager = SettingsManager(this)
        serviceScope.launch {
            try {
                val skipSilenceEnabled = settingsManager.skipSilenceFlow.first()
                normalizationEnabled = settingsManager.normalizeAudioFlow.first()

                player.skipSilenceEnabled = skipSilenceEnabled
                initializeEffectsPipeline(player.audioSessionId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed applying initial playback settings variables", e)
            }
        }

        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(CHANNEL_ID)
            .setNotificationId(NOTIFICATION_ID)
            .build()

        setMediaNotificationProvider(notificationProvider)

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(callback)
            .build()
    }

    @OptIn(UnstableApi::class)
    private fun initializeEffectsPipeline(audioSessionId: Int) {
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET) return

        loudnessEnhancer?.release()
        loudnessEnhancer = null
        equalizer?.release()
        equalizer = null
        bassBoost?.release()
        bassBoost = null

        try {
            equalizer = android.media.audiofx.Equalizer(0, audioSessionId).apply {
                enabled = true
            }
            Log.d(TAG, "Hardware structural Equalizer tied safely to session: $audioSessionId")
        } catch (e: Exception) {
            Log.e(TAG, "System Audio framework driver failed to bind Equalizer reference block", e)
        }

        try {
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = false
            }
            Log.d(TAG, "Hardware Bass Boost module initialized on session: $audioSessionId")
        } catch (e: Exception) {
            Log.e(TAG, "System Audio framework driver failed to bind BassBoost reference block", e)
        }

        applyNormalizationState(normalizationEnabled)
    }

    @UnstableApi
    private fun applyNormalizationState(enabled: Boolean) {
        val sessionId = player.audioSessionId
        if (sessionId == C.AUDIO_SESSION_ID_UNSET) return

        loudnessEnhancer?.release()
        loudnessEnhancer = null

        if (enabled) {
            try {
                loudnessEnhancer = LoudnessEnhancer(sessionId).apply {
                    setTargetGain(200)
                    this.enabled = true
                }
                Log.d(TAG, "LoudnessEnhancer module dynamically engaged.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed creating runtime LoudnessEnhancer configuration processing envelope", e)
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        serviceScope.cancel()
        loudnessEnhancer?.release()
        equalizer?.release()
        bassBoost?.release()
        mediaSession?.release()
        player.release()
        mediaSession = null
        super.onDestroy()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "MuzixX Playback", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }
}