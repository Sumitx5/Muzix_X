package com.sumit.muzixx.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sumit.muzixx.data.manager.*
import com.sumit.muzixx.data.model.Playlist
import com.sumit.muzixx.data.model.Song
import com.sumit.muzixx.data.network.JioSaavnApiService
import com.sumit.muzixx.data.network.SpotifyImporter
import com.sumit.muzixx.data.network.UpdateChecker
import com.sumit.muzixx.data.network.YouTubeAudioExtractor
import com.sumit.muzixx.data.network.YouTubeMusicScraper
import com.sumit.muzixx.data.repository.PlaybackStatsRepository
import com.sumit.muzixx.data.repository.SettingsRepository
import com.sumit.muzixx.utils.LikeManager
import com.sumit.muzixx.utils.NetworkUtils.isWifiConnected
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val context get() = getApplication<Application>().applicationContext

    // MANAGERS
    val mediaStateHolder by lazy { MediaStateHolder(viewModelScope) }
    private val ytScraper = YouTubeMusicScraper()
    private val ytExtractor = YouTubeAudioExtractor()
    private val jioSaavnApiService by lazy { JioSaavnApiService.create() }
    private val autoplayManager by lazy { AutoplayManager(ytScraper, ytExtractor, jioSaavnApiService) }
    private var persistenceManager: PlaybackPersistenceManager? = null
    private val spotifyImporter = SpotifyImporter()

    val cacheManager by lazy { CacheManager(context, viewModelScope) }
    val searchManager by lazy {
        SearchManager(
            scope = viewModelScope,
            jioSaavnApiService = jioSaavnApiService,
            ytScraper = ytScraper,
            persistenceManagerProvider = { persistenceManager }
        )
    }

    val contentManager by lazy {
        ContentManager(
            scope = viewModelScope,
            jioSaavnApiService = jioSaavnApiService,
            ytScraper = ytScraper,
            ytExtractor = ytExtractor,
            autoplayManager = autoplayManager,
            spotifyImporter = spotifyImporter,
            createPlaylistCallback = { name, songs -> createPlaylist(name, songs) }
        )
    }

    lateinit var settings: SettingsRepository private set
    lateinit var stats: PlaybackStatsRepository private set

    private val playlistController: PlaylistController by lazy {
        PlaylistController(onSavePlaylists = {
            persistenceManager?.saveCustomPlaylistsJson(playlistController.getCustomPlaylistsJson())
        })
    }

    val likeManager by lazy {
        LikeManager(
            getPlaylists = { playlistController.playlists },
            createPlaylist = { name -> playlistController.createCustomPlaylist(name) },
            addSongToPlaylist = { playlistId, song -> playlistController.addSongToPlaylist(playlistId, song) },
            removeSongFromPlaylist = { playlistId, song -> playlistController.removeSongFromPlaylist(playlistId, song) }
        )
    }

    // EXPOSED PLAYER STATES
    val isPlaying get() = playerController.isPlaying
    val selectedSong get() = playerController.selectedSong
    val currentPosition get() = mediaStateHolder.currentPosition
    val totalDuration get() = mediaStateHolder.totalDuration
    val currentRepeatMode get() = playerController.currentRepeatMode
    val currentVolume get() = playerController.currentVolume
    val activePlaylistIndex get() = playerController.activePlaylistIndex
    val activePlaybackQueue get() = playerController.activePlaybackQueue

    // CENTRALIZED PLAYER CONTROLLER
    private val playerController by lazy {
        PlayerController(
            scope = viewModelScope,
            onPlaybackStarted = { isPlayingNow, activeController ->
                if (::stats.isInitialized) stats.startPlaybackTimer { isPlayingNow }
                mediaStateHolder.startTracking { activeController }
            },
            onPlaybackStopped = {
                saveCurrentPlaybackPosition()
                if (::stats.isInitialized) stats.stopPlaybackTimer()
                mediaStateHolder.stopTracking()
            },
            onTrackSwitched = { switchedSong ->
                persistenceManager?.resetLastPlaybackPosition()
                if (::stats.isInitialized) stats.incrementSongsHeardCount()
                persistenceManager?.saveLastPlayedSong(switchedSong)
                addToRecentlyPlayed(switchedSong)
                handleQueueLookaheadAutoplay()
            },
            onQueueUpdated = { updatedList -> currentPlaybackQueue = updatedList },
            resolveYouTubeStream = { songItem ->
                try {
                    val cleanId = songItem.id.replace("yt_", "")
                    val qualityPref = if (isSettingsInitialized()) settings.audioQuality else "320kbps"
                    ytExtractor.getSongFromVideoId(
                        videoIdOrQuery = cleanId,
                        qualityPref = qualityPref,
                        originalTitle = songItem.title,
                        originalArtist = songItem.artist
                    )?.uri
                } catch (e: Exception) {
                    Log.e("VM_YT_RESOLVE", "Failed extracting YouTube stream path", e)
                    null
                }
            },
            jioSaavnApiService = jioSaavnApiService,
            getAudioQualityPreference = { if (isSettingsInitialized()) settings.audioQuality else "320kbps" },
            getStreamWifiOnlyPreference = { if (isSettingsInitialized()) settings.streamWifiOnly else false }
        )
    }

    var currentPlaybackQueue by mutableStateOf<List<Song>>(emptyList())
        private set

    // LOCAL MUSIC & RECENTLY PLAYED STATE
    val songs = mutableStateListOf<Song>()
    val recentlyPlayedSongs = mutableStateListOf<Song>()
    var isLocalSongsLoading by mutableStateOf(false)
        private set

    // PLAYLIST DELEGATION
    val playlists get() = playlistController.playlists
    var selectedPlaylist: Playlist?
        get() = playlistController.selectedPlaylist
        set(value) { playlistController.selectedPlaylist = value }

    // INITIALIZATIONS
    fun initSettings() {
        if (!::settings.isInitialized) settings = SettingsRepository(context, viewModelScope)
    }

    fun isSettingsInitialized(): Boolean = ::settings.isInitialized

    fun initStatsManager() {
        if (!::stats.isInitialized) stats = PlaybackStatsRepository(context, viewModelScope)
    }

    fun initMediaController(onControllerReady: () -> Unit = {}) {
        playerController.initMediaController(context, onControllerReady)
    }

    fun initStorage(skipSongRestoration: Boolean = false) {
        persistenceManager = PlaybackPersistenceManager(context)

        searchManager.loadSearchHistory()
        loadRecentlyPlayedFromStorage()

        try {
            val json = persistenceManager?.loadCustomPlaylistsJson()
            playlistController.loadPlaylistsFromJson(json)
        } catch (_: Exception) {
            persistenceManager?.clearCustomPlaylistsStorage()
        }

        if (skipSongRestoration) return

        if (playerController.selectedSong == null) {
            val lastSong = persistenceManager?.loadLastPlayedSong()
            if (lastSong != null && lastSong.id.isNotBlank()) {
                playerController.selectedSong = lastSong
                playerController.submitQueueToPlayer(listOf(lastSong), 0, playWhenReady = false)
                playerController.preparePlayerEngine()

                viewModelScope.launch(Dispatchers.IO) {
                    val combinedQueue = autoplayManager.buildBootQueue(lastSong)
                    if (combinedQueue.size > 1) {
                        withContext(Dispatchers.Main) {
                            playerController.submitQueueToPlayer(combinedQueue, 0, playWhenReady = false)
                            currentPlaybackQueue = combinedQueue
                        }
                    }
                }

                val savedProgress = persistenceManager?.getLastPlaybackPosition() ?: 0L
                if (savedProgress > 0L) seekTo(savedProgress)
            }
        }
    }

    fun saveCurrentPlaybackPosition() {
        persistenceManager?.saveCurrentPlaybackPosition(currentPosition)
    }

    fun overwriteStatsFromCloud(
        totalHeard: Int, monthlyHeard: Int, yearlyHeard: Int,
        totalSec: Long, monthlySec: Long, yearlySec: Long
    ) {
        if (totalHeard == 0 && monthlyHeard == 0 && totalSec == 0L) return
        val pm = persistenceManager ?: return
        if (!pm.isCloudStatsSynced() && ::stats.isInitialized) {
            stats.overwriteLocalStatsWithCloud(totalHeard, monthlyHeard, yearlyHeard, totalSec, monthlySec, yearlySec)
            pm.markCloudStatsSynced()
        }
    }

    fun resetCloudSyncFlag() = persistenceManager?.resetCloudSyncFlag()

    private fun addToRecentlyPlayed(song: Song) {
        if (song.id.isBlank()) return
        val matchIndex = recentlyPlayedSongs.indexOfFirst { it.id == song.id }
        if (matchIndex != -1) recentlyPlayedSongs.removeAt(matchIndex)
        recentlyPlayedSongs.add(0, song)
        if (recentlyPlayedSongs.size > 20) recentlyPlayedSongs.removeAt(recentlyPlayedSongs.lastIndex)
        persistenceManager?.saveRecentlyPlayed(recentlyPlayedSongs)
    }

    private fun loadRecentlyPlayedFromStorage() {
        persistenceManager?.let { pm ->
            recentlyPlayedSongs.clear()
            recentlyPlayedSongs.addAll(pm.loadRecentlyPlayed())
        }
    }

    fun updateAppTheme(themeName: String) {
        if (isSettingsInitialized()) settings.updateAppTheme(themeName)
    }

    fun loadLocalSongsWithLoadingState(songList: List<Song>) {
        viewModelScope.launch {
            isLocalSongsLoading = true
            songs.clear()
            songs.addAll(songList)
            playlistController.initializeLocalSongsPlaylist(songs)
            isLocalSongsLoading = false
        }
    }

    // PLAYBACK CONTROLS
    fun playMusicCollection(songList: List<Song>, startIndex: Int) {
        if (songList.isEmpty() || startIndex !in songList.indices) return
        playerController.submitQueueToPlayer(songList, startIndex)
        currentPlaybackQueue = songList
    }

    fun playSaavnSong(songList: List<Song>, startIndex: Int) = playMusicCollection(songList, startIndex)
    fun playYouTubeSong(songList: List<Song>, startIndex: Int) = playMusicCollection(songList, startIndex)
    fun playLocalSong(songList: List<Song>, startIndex: Int) = playMusicCollection(songList, startIndex)

    fun playSaavnSongWithYouTubeAutoplay(saavnList: List<Song>, startIndex: Int) {
        if (saavnList.isEmpty() || startIndex !in saavnList.indices) return
        val clickedSong = saavnList[startIndex]
        playMusicCollection(listOf(clickedSong), 0)

        viewModelScope.launch(Dispatchers.IO) {
            val recs = autoplayManager.fetchJioSaavnWithYouTubeRecommendations(clickedSong)
            if (recs.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    playerController.injectTracksToQueue(recs)
                    currentPlaybackQueue = playerController.activePlaybackQueue
                }
            }
        }
    }

    fun playYouTubeSearchResultWithAutoplay(youtubeList: List<Song>, startIndex: Int) {
        if (youtubeList.isEmpty() || startIndex !in youtubeList.indices) return
        val clickedSong = youtubeList[startIndex]

        viewModelScope.launch(Dispatchers.IO) {
            val fullQueue = autoplayManager.fetchYouTubeAutoplayQueue(clickedSong)
            withContext(Dispatchers.Main) {
                playMusicCollection(fullQueue, 0)
            }
        }
    }

    private fun handleQueueLookaheadAutoplay() {
        val currentSong = selectedSong ?: return
        val currentIndex = activePlaylistIndex
        val totalQueueLength = activePlaybackQueue.size

        if (currentIndex >= totalQueueLength - 2) {
            if (isSettingsInitialized() && settings.streamWifiOnly) {
                if (!isWifiConnected(context)) return
            }

            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val nextTracks = autoplayManager.fetchNextLookaheadTracks(currentSong)
                    if (nextTracks.isNotEmpty()) {
                        withContext(Dispatchers.Main) { playerController.injectTracksToQueue(nextTracks) }
                    }
                } catch (e: Exception) {
                    Log.e("MusicViewModel", "Lookahead autoplay failed", e)
                }
            }
        }
    }

    fun addSongToQueue(song: Song) {
        if (playerController.selectedSong == null) {
            playMusicCollection(listOf(song), 0)
            return
        }
        playerController.addTrackImmediatelyNext(song)
        currentPlaybackQueue = playerController.activePlaybackQueue
    }

    fun seekTo(position: Long) {
        mediaStateHolder.updateManualSeekPosition(position)
        playerController.seekTo(position)
    }

    fun togglePlayPause() = playerController.togglePlayPause()
    fun playNext() = playerController.playNext()
    fun playPrevious() = playerController.playPrevious()
    fun toggleRepeatMode() = playerController.cycleRepeatMode()

    // EQUALIZER CONTROLS
    fun setEqualizerPresetLive(presetIndex: Short) {
        if (isSettingsInitialized()) {
            settings.updateEqPresetIndex(presetIndex.toInt()) { playerController.setEqualizerPreset(it) }
        } else {
            playerController.setEqualizerPreset(presetIndex)
        }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        if (isSettingsInitialized()) {
            settings.updateEqEnabled(enabled) { playerController.setEqualizerEnabled(it) }
        } else {
            playerController.setEqualizerEnabled(enabled)
        }
    }

    fun setBandLevel(bandIndex: Int, dbValue: Float) {
        if (isSettingsInitialized()) {
            settings.updateSingleBand(bandIndex, dbValue) { idx, db -> playerController.setBandLevel(idx, db) }
        } else {
            playerController.setBandLevel(bandIndex, dbValue)
        }
    }

    fun setBassBoostEnabled(enabled: Boolean) {
        if (isSettingsInitialized()) {
            settings.updateBassEnabled(enabled) { playerController.setBassBoostEnabled(it) }
        } else {
            playerController.setBassBoostEnabled(enabled)
        }
    }

    fun setBassBoostStrength(strengthPercent: Float) {
        if (isSettingsInitialized()) {
            settings.updateBassStrength(strengthPercent) { playerController.setBassBoostStrength(it) }
        } else {
            playerController.setBassBoostStrength(strengthPercent)
        }
    }

    fun setMasterVolume(volumePercent: Float) = playerController.setMasterVolume(volumePercent)

    fun updateSkipSilenceLive(enabled: Boolean) {
        if (isSettingsInitialized()) {
            settings.updateSkipSilence(enabled)
            playerController.setSkipSilenceOnPlayer(enabled)
        }
    }

    fun updateNormalizeAudioLive(enabled: Boolean) {
        if (isSettingsInitialized()) {
            settings.updateNormalizeAudio(enabled)
            playerController.setAudioNormalizationOnPlayer(enabled)
        }
    }

    // PLAYLIST MANAGEMENT
    fun createPlaylist(name: String, initialSongs: List<Song> = emptyList()): Playlist? {
        val createdPlaylist = playlistController.createCustomPlaylist(name) ?: return null
        initialSongs.forEach { song ->
            playlistController.addSongToPlaylist(createdPlaylist.id, song)
        }
        return createdPlaylist
    }

    fun createCustomPlaylist(name: String): Playlist? = playlistController.createCustomPlaylist(name)
    fun addSongToPlaylist(playlistId: String, song: Song) = playlistController.addSongToPlaylist(playlistId, song)
    fun removeSongFromPlaylist(playlistId: String, song: Song) = playlistController.removeSongFromPlaylist(playlistId, song)
    fun renamePlaylist(playlistId: String, newName: String) = playlistController.renamePlaylist(playlistId, newName)
    fun deletePlaylist(playlistId: String) = playlistController.deletePlaylist(playlistId)

    // LIKE HELPERS
    fun isSongLiked(songId: String?): Boolean = likeManager.isSongLiked(songId)
    fun toggleLike(song: Song?): Boolean = likeManager.toggleLike(song)

    fun triggerUpdateCheck() {
        viewModelScope.launch { UpdateChecker.check(context, isManualCheck = true) }
    }

    fun preloadYouTubeStream(videoId: String) = ytExtractor.preloadStream(videoId)

    override fun onCleared() {
        super.onCleared()
        if (::stats.isInitialized) stats.stopPlaybackTimer()
        playerController.release()
    }
}