package com.sumit.muzixx.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sumit.muzixx.data.Playlist
import com.sumit.muzixx.data.Song
import com.sumit.muzixx.data.manager.PlayerController
import com.sumit.muzixx.data.manager.PlaylistController
import com.sumit.muzixx.data.manager.MediaStateHolder
import com.sumit.muzixx.data.repository.SettingsRepository
import com.sumit.muzixx.data.repository.PlaybackStatsRepository
import com.sumit.muzixx.data.network.JioSaavnApiService
import com.sumit.muzixx.data.network.UpdateChecker
import com.sumit.muzixx.data.model.toSong
import com.sumit.muzixx.data.network.YouTubeAudioExtractor
import com.sumit.muzixx.data.network.YouTubeMusicScraper
import com.sumit.muzixx.utils.NetworkUtils.isWifiConnected
import kotlinx.coroutines.*

class MusicViewModel : ViewModel() {

    val mediaStateHolder by lazy { MediaStateHolder(viewModelScope) }
    private val ytScraper = YouTubeMusicScraper()
    private val ytExtractor = YouTubeAudioExtractor()

    lateinit var settings: SettingsRepository private set
    lateinit var stats: PlaybackStatsRepository private set

    private val playlistController by lazy { PlaylistController(onSavePlaylists = { savePlaylistsToStorage() }) }
    private val jioSaavnApiService by lazy { JioSaavnApiService.create() }
    private var sharedPreferences: SharedPreferences? = null
    private var applicationContext: Context? = null

    //EXPOSED PLAYER STATES
    val isPlaying get() = playerController.isPlaying
    val selectedSong get() = playerController.selectedSong
    val currentPosition get() = mediaStateHolder.currentPosition
    val totalDuration get() = mediaStateHolder.totalDuration
    val currentRepeatMode get() = playerController.currentRepeatMode

    val activePlaylistIndex get() = playerController.activePlaylistIndex
    val activePlaybackQueue get() = playerController.activePlaybackQueue

    //CORE CENTRALIZED PLAYER HANDLER
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
                if (::stats.isInitialized) stats.stopPlaybackTimer()
                mediaStateHolder.stopTracking()
            },

            onTrackSwitched = { switchedSong ->
                if (::stats.isInitialized) stats.incrementSongsHeardCount()
                saveLastPlayedSong(switchedSong)

                handleQueueLookaheadAutoplay()
            },
            onQueueUpdated = { updatedList ->
                currentPlaybackQueue = updatedList
            },

            resolveYouTubeStream = { songItem ->
                try {
                    val cleanId = songItem.id.replace("yt_", "")
                    Log.d("VM_YT_RESOLVE", "Extracting live NewPipe audio stream link for: $cleanId")
                    val extractedSong = ytExtractor.getSongFromVideoId(cleanId)
                    extractedSong?.uri
                } catch (e: Exception) {
                    Log.e("VM_YT_RESOLVE", "Failed extracting YouTube stream path via NewPipe", e)
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

    //UI STATE LISTS
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

    //Settings
    fun initSettings(context: Context) {
        if (::settings.isInitialized) return
        this.applicationContext = context.applicationContext
        settings = SettingsRepository(context.applicationContext, viewModelScope)
    }

    fun isSettingsInitialized(): Boolean {
        return ::settings.isInitialized
    }

    fun initStatsManager(context: Context) {
        if (::stats.isInitialized) return
        this.applicationContext = context.applicationContext
        stats = PlaybackStatsRepository(context.applicationContext, viewModelScope)
    }

    fun initMediaController(context: Context) {
        this.applicationContext = context.applicationContext
        playerController.initMediaController(context)
    }

    fun initStorage(context: Context) {
        this.applicationContext = context.applicationContext
        sharedPreferences = context.getSharedPreferences("muzix_prefs", Context.MODE_PRIVATE)
        loadSearchHistory()
        try {
            val json = sharedPreferences?.getString("custom_playlists", null)
            playlistController.loadPlaylistsFromJson(json)
        } catch (_: Exception) {
            Log.e("PLAYLIST_STORAGE", "Corrupted storage cleared")
            sharedPreferences?.edit { remove("custom_playlists") }
        }

        if (playerController.selectedSong == null) {
            val lastSong = loadLastPlayedSong()
            if (lastSong != null && lastSong.id.isNotBlank()) {
                playerController.selectedSong = lastSong
                playerController.submitQueueToPlayer(listOf(lastSong), 0, playWhenReady = false)
            } else {
                currentPlaybackQueue = emptyList()
                Log.d("INIT_STORAGE", "First time launch or clear cache sequence triggered.")
            }
        }
    }

    private fun saveLastPlayedSong(song: Song) {
        sharedPreferences?.edit {
            putString("last_song_id", song.id)
            putString("last_song_title", song.title)
            putString("last_song_artist", song.artist)
            putString("last_song_uri", song.uri)
            putString("last_song_art_uri", song.artUri)
            putLong("last_song_duration", song.duration)
            putBoolean("last_song_is_streaming", song.isStreaming)
            putString("last_song_folder", song.folderName)
            putString("last_song_type", song.type)
        }
    }

    private fun loadLastPlayedSong(): Song? {
        val id = sharedPreferences?.getString("last_song_id", null) ?: return null
        val title = sharedPreferences?.getString("last_song_title", "Unknown Track") ?: "Unknown Track"
        val artist = sharedPreferences?.getString("last_song_artist", "Unknown Artist") ?: "Unknown Artist"
        val uri = sharedPreferences?.getString("last_song_uri", "") ?: ""
        val artUri = sharedPreferences?.getString("last_song_art_uri", null)
        val duration = sharedPreferences?.getLong("last_song_duration", 0L) ?: 0L
        val isStreaming = sharedPreferences?.getBoolean("last_song_is_streaming", false) ?: false
        val folderName = sharedPreferences?.getString("last_song_folder", "Unknown") ?: "Unknown"
        val type = sharedPreferences?.getString("last_song_type", "local") ?: "local"
        return Song(
            id = id, title = title, artist = artist, uri = uri, artUri = artUri,
            duration = duration, isStreaming = isStreaming, folderName = folderName, type = type
        )
    }

    private fun savePlaylistsToStorage() {
        val json = playlistController.getCustomPlaylistsJson()
        sharedPreferences?.edit { putString("custom_playlists", json) }
    }

    fun clearAudioCache(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cacheDir = context.applicationContext.cacheDir
                if (cacheDir.exists() && cacheDir.isDirectory) {
                    cacheDir.listFiles()?.forEach { file ->
                        file.deleteRecursively()
                    }
                }

                withContext(Dispatchers.Main) {
                    cacheSizeText = "0.00 MB"
                    Log.d("MuzixX_Cache", "Internal application audio stream cache cleared successfully.")
                }
            } catch (e: Exception) {
                Log.e("MuzixX_Cache", "Failed to clear temporary audio cache layer buffers", e)
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
                    cacheDir.walkTopDown().forEach { file ->
                        if (file.isFile) totalBytes += file.length()
                    }
                }
                val megaBytes = totalBytes.toDouble() / (1024 * 1024)
                withContext(Dispatchers.Main) {
                    cacheSizeText = String.format("%.2f MB", megaBytes)
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { cacheSizeText = "0.00 MB" }
            }
        }
    }

    fun updateAppTheme(themeName: String) {
        if (isSettingsInitialized()) {
            settings.updateAppTheme(themeName)
        }
    }

    fun searchJioSaavn(query: String) {
        if (query.isBlank()) return
        saveSearchQuery(query)
        viewModelScope.launch {
            isSaavnLoading = true
            try {
                val response = withContext(Dispatchers.IO) { jioSaavnApiService.searchSongs(query) }
                if (response.success) {
                    val tracks = response.data?.songs ?: emptyList()
                    val mappedTracks = tracks.map { it.toSong("JioSaavn Search Result") }

                    withContext(Dispatchers.Main) {
                        saavnSearchResults.clear()
                        saavnSearchResults.addAll(mappedTracks)
                    }
                }
            } catch (e: Exception) {
                Log.e("SAAVN_SEARCH_ERROR", "Search parsing failed: ${e.message}")
                withContext(Dispatchers.Main) { saavnSearchResults.clear() }
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
                withContext(Dispatchers.Main) {
                    searchResults.clear()
                    searchResults.addAll(scrapedResults)
                }
            } catch (e: Exception) {
                Log.e("YT_SEARCH_ERROR", "Failed pulling YouTube matches: ${e.message}", e)
                withContext(Dispatchers.Main) { searchResults.clear() }
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
                val trendingDeferred = async(Dispatchers.IO) {
                    try { jioSaavnApiService.getPlaylistDetails("110858205") } catch(_: Exception) { null }
                }
                val newReleasesDeferred = async(Dispatchers.IO) {
                    try { jioSaavnApiService.getPlaylistDetails("47599074") } catch(_: Exception) { null }
                }
                val hindiHitsDeferred = async(Dispatchers.IO) {
                    try { jioSaavnApiService.getPlaylistDetails("1134543272") } catch(_: Exception) { null }
                }

                val trendingSongsList = trendingDeferred.await()?.data?.songs?.map { it.toSong("JioSaavn Trending") } ?: emptyList()
                val releasesSongsList = newReleasesDeferred.await()?.data?.songs?.map { it.toSong("JioSaavn New Releases") } ?: emptyList()
                val hmList = hindiHitsDeferred.await()?.data?.songs?.map { it.toSong("Hindi Hits") } ?: emptyList()

                withContext(Dispatchers.Main) {
                    saavnTrendingSongs.clear()
                    saavnTrendingSongs.addAll(trendingSongsList)
                    saavnNewReleases.clear()
                    saavnNewReleases.addAll(releasesSongsList)
                    saavnHminiHits.clear()
                    saavnHminiHits.addAll(hmList)
                }
            } catch (e: Exception) {
                Log.e("SAAVN_HOME_ERROR", "Failed to load curated home grids: ${e.message}")
            } finally {
                withContext(Dispatchers.Main) {
                    isTrendingLoading = false
                    isNewReleasesLoading = false
                    isHindiHitLoading = false
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
                    val playlistsFound = response.data?.results ?: emptyList()

                    withContext(Dispatchers.Main) {
                        saavnPlaylistSearchResults.clear()
                        saavnPlaylistSearchResults.addAll(playlistsFound)
                    }
                }
            } catch (e: Exception) {
                Log.e("SAAVN_PLAYLIST_ERROR", "Failed to lookup playlists: ${e.message}")
                withContext(Dispatchers.Main) { saavnPlaylistSearchResults.clear() }
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
                    val tracks = response.data?.songs ?: emptyList()
                    val mappedTracks = tracks.map { it.toSong("Cloud Playlist: $playlistName") }

                    withContext(Dispatchers.Main) {
                        currentCloudPlaylistSongs.addAll(mappedTracks)
                    }
                }
            } catch (e: Exception) {
                Log.e("SAAVN_PLAYL_ERROR", "Failed to fetch cloud playlist layout rows: ${e.message}")
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

    fun playMusicCollection(songList: List<Song>, startIndex: Int) {
        if (songList.isEmpty() || startIndex !in songList.indices) return

        playerController.submitQueueToPlayer(songList, startIndex)
        currentPlaybackQueue = songList
    }

    fun playSaavnSong(songList: List<Song>, startIndex: Int) = playMusicCollection(songList, startIndex)
    fun playYouTubeSong(songList: List<Song>, startIndex: Int) = playMusicCollection(songList, startIndex)
    fun playLocalSong(songList: List<Song>, startIndex: Int) = playMusicCollection(songList, startIndex)

    fun playSearchResultWithAutoplay(searchList: List<Song>, startIndex: Int) {
        if (searchList.isEmpty() || startIndex !in searchList.indices) return

        val clickedSong = searchList[startIndex]

        viewModelScope.launch {
            var finalQueue = searchList.toMutableList()

            if (clickedSong.id.all { it.isDigit() }) {
                try {
                    val response = withContext(Dispatchers.IO) {
                        jioSaavnApiService.getSongSuggestions(clickedSong.id, limit = 20)
                    }

                    if (response.success) {
                        val recommendations = response.data?.songs?.map {
                            it.toSong("Autoplay Suggestion")
                        } ?: emptyList()

                        finalQueue.addAll(recommendations)
                        Log.d("VM_SEARCH_AUTOPLAY", "Successfully unified queue with ${recommendations.size} recommendations.")
                    }
                } catch (e: Exception) {
                    Log.e("VM_SEARCH_AUTOPLAY", "Failed to fetch background suggestions, falling back to raw list: ${e.message}")
                }
            }

            withContext(Dispatchers.Main) {
                playMusicCollection(finalQueue, startIndex)
            }
        }
    }

    fun playYouTubeSearchResultWithAutoplay(youtubeList: List<Song>, startIndex: Int) {
        if (youtubeList.isEmpty() || startIndex !in youtubeList.indices) return

        val clickedSong = youtubeList[startIndex]

        viewModelScope.launch {
            val finalQueue = mutableListOf(clickedSong)
            val newStartIndex = 0

            try {
                val targetVideoId = clickedSong.id.replace("yt_", "").trim()
                Log.d("VM_YT_AUTOPLAY", "Fetching native NewPipe recommendations for video ID: $targetVideoId")

                val recommendations = ytExtractor.getRelatedSongsFromVideoId(targetVideoId)

                if (recommendations.isNotEmpty()) {
                    val targetNormalizedId = clickedSong.id.replace("yt_", "").trim()
                    val targetLowerTitle = clickedSong.title.lowercase().trim()

                    val uniqueRecs = recommendations.filter { rec ->
                        val recNormalizedId = rec.id.replace("yt_", "").trim()
                        val recLowerTitle = rec.title.lowercase().trim()

                        val isSameTrack = recNormalizedId == targetNormalizedId || recLowerTitle == targetLowerTitle

                        !isSameTrack
                    }

                    finalQueue.addAll(uniqueRecs)
                    Log.d("VM_YT_AUTOPLAY", "Successfully appended ${uniqueRecs.size} high-quality NewPipe related tracks.")
                }
            } catch (e: Exception) {
                Log.e("VM_YT_AUTOPLAY", "Native related item generation block failed cleanly", e)
            }

            withContext(Dispatchers.Main) {
                playMusicCollection(finalQueue, newStartIndex)
            }
        }
    }

    fun loadSearchHistory() {
        sharedPreferences?.getStringSet("search_history_set", emptySet())?.let { savedSet ->
            searchHistory.clear()
            searchHistory.addAll(savedSet.toList().reversed())
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

        sharedPreferences?.edit {
            putStringSet("search_history_set", cappedList.toSet())
        }
    }

    fun deleteSearchQuery(query: String) {
        searchHistory.remove(query)
        sharedPreferences?.edit {
            putStringSet("search_history_set", searchHistory.toSet())
        }
    }

    private fun handleQueueLookaheadAutoplay() {
        val currentSong = selectedSong ?: return
        val currentIndex = activePlaylistIndex
        val totalQueueLength = activePlaybackQueue.size

        if (currentIndex >= totalQueueLength - 2) {
            if (isSettingsInitialized() && settings.streamWifiOnly) {
                applicationContext?.let { ctx ->
                    if (!isWifiConnected(ctx)) {
                        Log.d("INFINITE_AUTOPLAY", "Autoplay cancelled: Device is on cellular data.")
                        return
                    }
                }
            }

            if (currentSong.id.all { it.isDigit() } || currentSong.artist.contains("JioSaavn") || currentSong.title.contains("JioSaavn")) {
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val response = jioSaavnApiService.getSongSuggestions(currentSong.id)
                        val suggestedSongs = response.data?.songs?.map { it.toSong("Autoplay Suggestion") } ?: emptyList()
                        withContext(Dispatchers.Main) { playerController.injectTracksToQueue(suggestedSongs) }
                    } catch (e: Exception) {
                        Log.e("INFINITE_AUTOPLAY", "Failed updating recommendations pipeline", e)
                    }
                }
            } else {
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val relatedSongs = emptyList<Song>()
                        withContext(Dispatchers.Main) { playerController.injectTracksToQueue(relatedSongs) }
                    } catch (e: Exception) {
                        Log.e("INFINITE_YT_AUTOPLAY", "Failed updating related video queue", e)
                    }
                }
            }
        }
    }

    fun seekTo(position: Long) {
        mediaStateHolder.updateManualSeekPosition(position)
        playerController.seekTo(position)
    }

    fun togglePlayPause() = playerController.togglePlayPause()
    fun playNext() = playerController.playNext()
    fun playPrevious() = playerController.playPrevious()

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

    //Playlist Controlling
    fun createCustomPlaylist(name: String) = playlistController.createCustomPlaylist(name)
    fun addSongToPlaylist(playlistId: String, song: Song) = playlistController.addSongToPlaylist(playlistId, song)
    fun removeSongFromPlaylist(playlistId: String, song: Song) = playlistController.removeSongFromPlaylist(playlistId, song)
    fun renamePlaylist(playlistId: String, newName: String) = playlistController.renamePlaylist(playlistId, newName)
    fun deletePlaylist(playlistId: String) = playlistController.deletePlaylist(playlistId)

    fun triggerUpdateCheck(context: Context) {
        viewModelScope.launch { UpdateChecker.check(context, isManualCheck = true) }
    }

    fun preloadYouTubeStream(videoId: String) {
        ytExtractor.preloadStream(videoId)
    }

    override fun onCleared() {
        super.onCleared()
        if (::stats.isInitialized) {
            stats.stopPlaybackTimer()
        }
        playerController.release()
    }

    fun toggleRepeatMode() {
        playerController.cycleRepeatMode()
    }
}