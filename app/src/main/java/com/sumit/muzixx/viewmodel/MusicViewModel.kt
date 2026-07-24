package com.sumit.muzixx.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sumit.muzixx.data.Playlist
import com.sumit.muzixx.data.Song
import com.sumit.muzixx.data.manager.AutoplayManager
import com.sumit.muzixx.data.manager.MediaStateHolder
import com.sumit.muzixx.data.manager.PlayerController
import com.sumit.muzixx.data.manager.PlaybackPersistenceManager
import com.sumit.muzixx.data.manager.PlaylistController
import com.sumit.muzixx.data.model.toSong
import com.sumit.muzixx.data.network.JioSaavnApiService
import com.sumit.muzixx.data.network.UpdateChecker
import com.sumit.muzixx.data.network.YouTubeAudioExtractor
import com.sumit.muzixx.data.network.YouTubeMusicScraper
import com.sumit.muzixx.data.repository.PlaybackStatsRepository
import com.sumit.muzixx.data.repository.SettingsRepository
import com.sumit.muzixx.utils.NetworkUtils.isWifiConnected
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicViewModel : ViewModel() {

    val mediaStateHolder by lazy { MediaStateHolder(viewModelScope) }
    private val ytScraper = YouTubeMusicScraper()
    private val ytExtractor = YouTubeAudioExtractor()
    private val jioSaavnApiService by lazy { JioSaavnApiService.create() }
    private val autoplayManager by lazy { AutoplayManager(ytScraper, ytExtractor, jioSaavnApiService) }
    private var persistenceManager: PlaybackPersistenceManager? = null

    lateinit var settings: SettingsRepository private set
    lateinit var stats: PlaybackStatsRepository private set

    private val playlistController: PlaylistController by lazy {
        PlaylistController(onSavePlaylists = {
            persistenceManager?.saveCustomPlaylistsJson(playlistController.getCustomPlaylistsJson())
        })
    }

    private var applicationContext: Context? = null

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
                if (::stats.isInitialized) {
                    stats.startPlaybackTimer(isPlayingProvider = { isPlayingNow })
                }
                mediaStateHolder.startTracking(mediaControllerProvider = { activeController })
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
            onQueueUpdated = { updatedList ->
                currentPlaybackQueue = updatedList
            },
            resolveYouTubeStream = { songItem ->
                try {
                    val cleanId = songItem.id.replace("yt_", "")
                    val extractedSong = ytExtractor.getSongFromVideoId(cleanId)
                    extractedSong?.uri
                } catch (e: Exception) {
                    Log.e("VM_YT_RESOLVE", "Failed extracting YouTube stream path", e)
                    null
                }
            },
            jioSaavnApiService = jioSaavnApiService,
            getAudioQualityPreference = {
                if (isSettingsInitialized()) settings.audioQuality else "320kbps"
            },
            getStreamWifiOnlyPreference = {
                if (isSettingsInitialized()) settings.streamWifiOnly else false
            }
        )
    }

    var currentPlaybackQueue by mutableStateOf<List<Song>>(emptyList())
        private set

    var cacheSizeText by mutableStateOf("Calculating...")
        private set

    // UI STATE LISTS
    val songs = mutableStateListOf<Song>()
    val searchResults = mutableStateListOf<Song>()
    val saavnTrendingSongs = mutableStateListOf<Song>()
    val saavnNewReleases = mutableStateListOf<Song>()
    val saavnHminiHits = mutableStateListOf<Song>()
    val saavnSearchResults = mutableStateListOf<Song>()
    val saavnPlaylistSearchResults = mutableStateListOf<com.sumit.muzixx.data.model.SaavnCloudPlaylistObject>()
    var currentCloudPlaylistName by mutableStateOf<String?>(null)
    val currentCloudPlaylistSongs = mutableStateListOf<Song>()
    val searchHistory = mutableStateListOf<String>()
    val recentlyPlayedSongs = mutableStateListOf<Song>()

    // Loading States
    var isLocalSongsLoading by mutableStateOf(false)
        private set
    var isTrendingLoading by mutableStateOf(false)
        private set
    var isNewReleasesLoading by mutableStateOf(false)
        private set
    var isHindiHitLoading by mutableStateOf(false)
        private set
    var isSearchLoading by mutableStateOf(false)
        private set
    var isSaavnLoading by mutableStateOf(false)
        private set
    var isCloudPlaylistLoading by mutableStateOf(false)
        private set

    val playlists get() = playlistController.playlists
    var selectedPlaylist: Playlist?
        get() = playlistController.selectedPlaylist
        set(value) { playlistController.selectedPlaylist = value }

    // Initialization
    fun initSettings(context: Context) {
        if (::settings.isInitialized) return
        this.applicationContext = context.applicationContext
        settings = SettingsRepository(context.applicationContext, viewModelScope)
    }

    fun isSettingsInitialized(): Boolean = ::settings.isInitialized

    fun initStatsManager(context: Context) {
        if (::stats.isInitialized) return
        this.applicationContext = context.applicationContext
        stats = PlaybackStatsRepository(context.applicationContext, viewModelScope)
    }

    fun initMediaController(context: Context, onControllerReady: () -> Unit = {}) {
        this.applicationContext = context.applicationContext
        playerController.initMediaController(context, onControllerReady)
    }

    fun initStorage(context: Context, skipSongRestoration: Boolean = false) {
        this.applicationContext = context.applicationContext
        persistenceManager = PlaybackPersistenceManager(context)

        loadSearchHistory()
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
                if (savedProgress > 0L) {
                    seekTo(savedProgress)
                }
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

    fun resetCloudSyncFlag() {
        persistenceManager?.resetCloudSyncFlag()
    }

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

    fun clearAudioCache(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cacheDir = context.applicationContext.cacheDir
                if (cacheDir.exists() && cacheDir.isDirectory) {
                    cacheDir.listFiles()?.forEach { file -> file.deleteRecursively() }
                }
                withContext(Dispatchers.Main) { cacheSizeText = "0.00 MB" }
            } catch (e: Exception) {
                Log.e("MuzixX_Cache", "Failed to clear audio cache", e)
            }
        }
    }

    @SuppressLint("DefaultLocale")
    fun calculateCurrentCacheSize(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cacheDir = context.applicationContext.cacheDir
                var totalBytes = 0L
                if (cacheDir.exists() && cacheDir.isDirectory) {
                    cacheDir.walkTopDown().forEach { if (it.isFile) totalBytes += it.length() }
                }
                val megaBytes = totalBytes.toDouble() / (1024 * 1024)
                withContext(Dispatchers.Main) { cacheSizeText = String.format("%.2f MB", megaBytes) }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { cacheSizeText = "0.00 MB" }
            }
        }
    }

    fun updateAppTheme(themeName: String) {
        if (isSettingsInitialized()) settings.updateAppTheme(themeName)
    }

    // Search & Content Fetching
    fun searchJioSaavn(query: String) {
        if (query.isBlank()) return
        saveSearchQuery(query)
        viewModelScope.launch {
            isSaavnLoading = true
            try {
                val response = withContext(Dispatchers.IO) { jioSaavnApiService.searchSongs(query) }
                if (response.success) {
                    val tracks = response.data?.songs?.map { it.toSong("JioSaavn Search Result") } ?: emptyList()
                    saavnSearchResults.clear()
                    saavnSearchResults.addAll(tracks)
                }
            } catch (_: Exception) {
                saavnSearchResults.clear()
            } finally {
                isSaavnLoading = false
            }
        }
    }

    fun searchOnlineSongs(query: String) {
        if (query.isBlank()) return
        saveSearchQuery(query)
        viewModelScope.launch {
            isSearchLoading = true
            try {
                val scrapedResults = ytScraper.searchSongs(query.trim())
                searchResults.clear()
                searchResults.addAll(scrapedResults)
            } catch (_: Exception) {
                searchResults.clear()
            } finally {
                isSearchLoading = false
            }
        }
    }

    fun loadJioSaavnHomeContent() {
        if (isTrendingLoading || isNewReleasesLoading || isHindiHitLoading) return
        isTrendingLoading = true
        isNewReleasesLoading = true
        isHindiHitLoading = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val trendingDef = async { runCatching { jioSaavnApiService.getPlaylistDetails("110858205") }.getOrNull() }
                val newRelDef = async { runCatching { jioSaavnApiService.getPlaylistDetails("47599074") }.getOrNull() }
                val hindiHitsDef = async { runCatching { jioSaavnApiService.getPlaylistDetails("1134543272") }.getOrNull() }

                val trending = trendingDef.await()?.data?.songs?.map { it.toSong("JioSaavn Trending") } ?: emptyList()
                val newReleases = newRelDef.await()?.data?.songs?.map { it.toSong("JioSaavn New Releases") } ?: emptyList()
                val hindiHits = hindiHitsDef.await()?.data?.songs?.map { it.toSong("Hindi Hits") } ?: emptyList()

                withContext(Dispatchers.Main) {
                    saavnTrendingSongs.clear(); saavnTrendingSongs.addAll(trending)
                    saavnNewReleases.clear(); saavnNewReleases.addAll(newReleases)
                    saavnHminiHits.clear(); saavnHminiHits.addAll(hindiHits)
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isTrendingLoading = false; isNewReleasesLoading = false; isHindiHitLoading = false
                }
            }
        }
    }

    fun searchJioSaavnPlaylists(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) { jioSaavnApiService.searchPlaylists(query) }
                if (response.success) {
                    saavnPlaylistSearchResults.clear()
                    saavnPlaylistSearchResults.addAll(response.data?.results ?: emptyList())
                }
            } catch (_: Exception) {
                saavnPlaylistSearchResults.clear()
            }
        }
    }

    fun loadCloudPlaylistDetails(playlistId: String, playlistName: String) {
        if (playlistId.isBlank()) return
        viewModelScope.launch {
            isCloudPlaylistLoading = true
            currentCloudPlaylistSongs.clear()
            currentCloudPlaylistName = playlistName
            try {
                val response = withContext(Dispatchers.IO) { jioSaavnApiService.getPlaylistDetails(playlistId) }
                if (response.success) {
                    val tracks = response.data?.songs?.map { it.toSong("Cloud Playlist: $playlistName") } ?: emptyList()
                    currentCloudPlaylistSongs.addAll(tracks)
                }
            } finally {
                isCloudPlaylistLoading = false
            }
        }
    }

    fun closeCloudPlaylistDetails() {
        currentCloudPlaylistName = null
        currentCloudPlaylistSongs.clear()
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

    // Playback Controls & Autoplay Bridges
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

    // Equalizer Controls
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

    // Search History
    fun loadSearchHistory() {
        persistenceManager?.let { pm ->
            searchHistory.clear()
            searchHistory.addAll(pm.loadSearchHistory())
        }
    }

    fun saveSearchQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        val currentList = searchHistory.toMutableList()
        currentList.remove(trimmed)
        currentList.add(0, trimmed)
        val cappedList = if (currentList.size > 10) currentList.take(10) else currentList

        searchHistory.clear()
        searchHistory.addAll(cappedList)
        persistenceManager?.saveSearchHistory(cappedList)
    }

    fun deleteSearchQuery(query: String) {
        searchHistory.remove(query)
        persistenceManager?.saveSearchHistory(searchHistory)
    }

    private fun handleQueueLookaheadAutoplay() {
        val currentSong = selectedSong ?: return
        val currentIndex = activePlaylistIndex
        val totalQueueLength = activePlaybackQueue.size

        if (currentIndex >= totalQueueLength - 2) {
            if (isSettingsInitialized() && settings.streamWifiOnly) {
                applicationContext?.let { ctx ->
                    if (!isWifiConnected(ctx)) return
                }
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


    fun createCustomPlaylist(name: String) = playlistController.createCustomPlaylist(name)
    fun addSongToPlaylist(playlistId: String, song: Song) = playlistController.addSongToPlaylist(playlistId, song)
    fun removeSongFromPlaylist(playlistId: String, song: Song) = playlistController.removeSongFromPlaylist(playlistId, song)
    fun renamePlaylist(playlistId: String, newName: String) = playlistController.renamePlaylist(playlistId, newName)
    fun deletePlaylist(playlistId: String) = playlistController.deletePlaylist(playlistId)

    fun triggerUpdateCheck(context: Context) {
        viewModelScope.launch { UpdateChecker.check(context, isManualCheck = true) }
    }

    fun preloadYouTubeStream(videoId: String) = ytExtractor.preloadStream(videoId)

    override fun onCleared() {
        super.onCleared()
        if (::stats.isInitialized) stats.stopPlaybackTimer()
        playerController.release()
    }
}