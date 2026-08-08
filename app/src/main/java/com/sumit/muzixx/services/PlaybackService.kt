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
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.sumit.muzixx.data.manager.SettingsManager
import com.sumit.muzixx.data.model.Song
import com.sumit.muzixx.utils.LikeManager
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
        private const val ACTION_TOGGLE_LIKE = "ACTION_TOGGLE_LIKE"
    }

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var settingsManager: SettingsManager
    private lateinit var likeManager: LikeManager

    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var equalizer: android.media.audiofx.Equalizer? = null
    private var bassBoost: BassBoost? = null

    private var normalizationEnabled = false
    private var isCurrentTrackLiked = false

    private val callback = @UnstableApi
    object : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val sessionCommandsBuilder = SessionCommands.Builder()
                .addSessionCommands(MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.commands)

            val customActions = listOf(
                "ACTION_SET_SKIP_SILENCE",
                "ACTION_SET_NORMALIZATION",
                "ACTION_SET_EQ_ENABLED",
                "ACTION_SET_EQ_BAND",
                "ACTION_SET_EQUALIZER_PRESET",
                "ACTION_SET_BASS_ENABLED",
                "ACTION_SET_BASS_STRENGTH",
                ACTION_TOGGLE_LIKE
            )

            customActions.forEach { action ->
                sessionCommandsBuilder.add(SessionCommand(action, android.os.Bundle.EMPTY))
            }

            val customCommands = sessionCommandsBuilder.build()
            val customLayoutList = listOf(getLikeCommandButton(isCurrentTrackLiked))

            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(customCommands)
                .setAvailablePlayerCommands(MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS)
                .setCustomLayout(customLayoutList)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: android.os.Bundle
        ): ListenableFuture<SessionResult> {

            when (customCommand.customAction) {
                ACTION_TOGGLE_LIKE -> {
                    val currentMediaItem = player.currentMediaItem
                    if (currentMediaItem != null) {
                        val mediaMetadata = currentMediaItem.mediaMetadata
                        val song = Song(
                            id = currentMediaItem.mediaId,
                            title = mediaMetadata.title?.toString() ?: "Unknown",
                            artist = mediaMetadata.artist?.toString() ?: "Unknown",
                            artUri = mediaMetadata.artworkUri?.toString(),
                            uri = currentMediaItem.localConfiguration?.uri?.toString() ?: "",
                            duration = player.duration.coerceAtLeast(0L),
                            isStreaming = false,
                            type = "LOCAL"
                        )

                        serviceScope.launch {
                            val nowLiked = likeManager.toggleLike(song)
                            isCurrentTrackLiked = nowLiked
                            mediaSession?.setCustomLayout(listOf(getLikeCommandButton(isCurrentTrackLiked)))
                        }
                    }
                }
                "ACTION_SET_SKIP_SILENCE" -> {
                    val enabled = args.getBoolean("enabled", false)
                    player.skipSilenceEnabled = enabled
                }
                "ACTION_SET_NORMALIZATION" -> {
                    val enabled = args.getBoolean("enabled", false)
                    normalizationEnabled = enabled
                    applyNormalizationState(enabled)
                }
                "ACTION_SET_EQ_ENABLED" -> {
                    val enabled = args.getBoolean("enabled", true)
                    equalizer?.enabled = enabled
                    serviceScope.launch {
                        settingsManager.saveBooleanSetting(SettingsManager.EQ_ENABLED, enabled)
                    }
                }
                "ACTION_SET_EQ_BAND" -> {
                    val bandIndex = args.getInt("band_index", -1)
                    val dbValue = args.getFloat("db_value", 0f)

                    serviceScope.launch {
                        val currentBands = settingsManager.eqBandsFlow.first().toMutableList()
                        if (bandIndex in currentBands.indices) {
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
                            } catch (e: Exception) {
                                Log.e(TAG, "Error setting EQ band", e)
                            }
                        }
                    }
                }
                "ACTION_SET_EQUALIZER_PRESET" -> {
                    val presetIndex = args.getShort("preset_index", -1)
                    if (presetIndex != (-1).toShort() && equalizer != null) {
                        try {
                            if (presetIndex < (equalizer?.numberOfPresets ?: 0)) {
                                equalizer?.usePreset(presetIndex)
                                serviceScope.launch {
                                    settingsManager.saveIntSetting(SettingsManager.EQ_PRESET_INDEX, presetIndex.toInt())
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error applying preset", e)
                        }
                    }
                }
                "ACTION_SET_BASS_ENABLED" -> {
                    val enabled = args.getBoolean("enabled", false)
                    bassBoost?.enabled = enabled
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
                            serviceScope.launch {
                                settingsManager.saveFloatSetting(SettingsManager.BASS_STRENGTH, strengthPercent)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error updating Bass Boost", e)
                        }
                    }
                }
            }

            return Futures.immediateFuture(
                SessionResult(SessionResult.RESULT_SUCCESS)
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
        settingsManager = SettingsManager(this)

        likeManager = LikeManager(
            getPlaylists = { emptyList() },
            createPlaylist = { _ -> },
            addSongToPlaylist = { _, _ -> },
            removeSongFromPlaylist = { _, _ -> }
        )

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

            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                val songId = mediaItem?.mediaId
                if (songId != null) {
                    serviceScope.launch {
                        isCurrentTrackLiked = likeManager.isSongLiked(songId)
                        mediaSession?.setCustomLayout(listOf(getLikeCommandButton(isCurrentTrackLiked)))
                    }
                }
            }
        })

        serviceScope.launch {
            try {
                val skipSilenceEnabled = settingsManager.skipSilenceFlow.first()
                normalizationEnabled = settingsManager.normalizeAudioFlow.first()

                player.skipSilenceEnabled = skipSilenceEnabled
                initializeEffectsPipeline(player.audioSessionId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed applying initial playback settings", e)
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

    private fun getLikeCommandButton(isLiked: Boolean): CommandButton {
        val iconRes = if (isLiked) {
            android.R.drawable.btn_star_big_on
        } else {
            android.R.drawable.btn_star_big_off
        }

        return CommandButton.Builder()
            .setDisplayName(if (isLiked) "Unlike" else "Like")
            .setIconResId(iconRes)
            .setSessionCommand(SessionCommand(ACTION_TOGGLE_LIKE, android.os.Bundle.EMPTY))
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
        } catch (e: Exception) {
            Log.e(TAG, "Failed binding Equalizer", e)
        }

        try {
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed binding BassBoost", e)
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
            } catch (e: Exception) {
                Log.e(TAG, "Failed creating LoudnessEnhancer", e)
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