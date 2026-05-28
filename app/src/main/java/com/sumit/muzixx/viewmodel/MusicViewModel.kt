package com.sumit.muzixx.viewmodel

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
import org.schabi.newpipe.extractor.stream.StreamExtractor
import org.schabi.newpipe.extractor.ServiceList
import com.sumit.muzixx.data.network.YouTubeMusicScraper
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

    //EXPOSED PLAYER STATES
    val isPlaying get() = playerController.isPlaying
    val selectedSong get() = playerController.selectedSong
    val currentPosition get() = mediaStateHolder.currentPosition
    val totalDuration get() = mediaStateHolder.totalDuration

    val mediaController get() = playerController.mediaController
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
            onTrackSwitched = {
                if (::stats.isInitialized) stats.incrementSongsHeardCount()
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
            }
        )
    }

    var currentPlaybackQueue by mutableStateOf<List<Song>>(emptyList())
        private set

    //UI STATE LISTS
    val songs = mutableStateListOf<Song>()
    val searchResults = mutableStateListOf<Song>()
    val saavnTrendingSongs = mutableStateListOf<Song>()
    val saavnNewReleases = mutableStateListOf<Song>()
    val saavnHindiHits = mutableStateListOf<Song>()
    val saavnSearchResults = mutableStateListOf<Song>()

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

    val playlists get() = playlistController.playlists
    var selectedPlaylist: Playlist?
        get() = playlistController.selectedPlaylist
        set(value) { playlistController.selectedPlaylist = value }

    // ========================= SUBSYSTEM INITIALIZERS =========================
    fun initSettings(context: Context) {
        if (::settings.isInitialized) return
        settings = SettingsRepository(context.applicationContext, viewModelScope)
    }

    fun isSettingsInitialized(): Boolean {
        return ::settings.isInitialized
    }

    fun initStatsManager(context: Context) {
        if (::stats.isInitialized) return
        stats = PlaybackStatsRepository(context.applicationContext, viewModelScope)
    }

    fun initMediaController(context: Context) {
        playerController.initMediaController(context)
    }

    fun initStorage(context: Context) {
        sharedPreferences = context.getSharedPreferences("muzix_prefs", Context.MODE_PRIVATE)
        try {
            val json = sharedPreferences?.getString("custom_playlists", null)
            playlistController.loadPlaylistsFromJson(json)
        } catch (e: Exception) {
            Log.e("PLAYLIST_STORAGE", "Corrupted storage cleared")
            sharedPreferences?.edit { remove("custom_playlists") }
        }
    }

    private fun savePlaylistsToStorage() {
        val json = playlistController.getCustomPlaylistsJson()
        sharedPreferences?.edit { putString("custom_playlists", json) }
    }

    //Data Searcher
    fun searchJioSaavn(query: String) {
        if (query.isBlank()) return
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
        viewModelScope.launch(Dispatchers.IO) {
            isTrendingLoading = true
            isNewReleasesLoading = true
            isHindiHitLoading = true
            try {
                val trendingDeferred = async(Dispatchers.IO) {
                    try { jioSaavnApiService.getPlaylistDetails("932189657") } catch(e: Exception) { null }
                }
                val newReleasesDeferred = async(Dispatchers.IO) {
                    try { jioSaavnApiService.getPlaylistDetails("155321561") } catch(e: Exception) { null }
                }
                val hindiHitsDeferred = async(Dispatchers.IO) {
                    try { jioSaavnApiService.getPlaylistDetails("1080335349") } catch(e: Exception) { null }
                }

                val trendingSongsList = trendingDeferred.await()?.data?.songs?.map { it.toSong("JioSaavn Trending") } ?: emptyList()
                val releasesSongsList = newReleasesDeferred.await()?.data?.songs?.map { it.toSong("JioSaavn New Releases") } ?: emptyList()
                val hindiHitsSongsList = hindiHitsDeferred.await()?.data?.songs?.map { it.toSong("Hindi Hits") } ?: emptyList()

                withContext(Dispatchers.Main) {
                    saavnTrendingSongs.clear()
                    saavnTrendingSongs.addAll(trendingSongsList)
                    saavnNewReleases.clear()
                    saavnNewReleases.addAll(releasesSongsList)
                    saavnHindiHits.clear()
                    saavnHindiHits.addAll(hindiHitsSongsList)
                }
            } catch (e: Exception) {
                Log.e("SAAVN_HOME_ERROR", "Failed to load curated home grids: ${e.message}")
            } finally {
                isTrendingLoading = false
                isNewReleasesLoading = false
                isHindiHitLoading = false
            }
        }
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

    private fun handleQueueLookaheadAutoplay() {
        val currentSong = selectedSong ?: return
        val currentIndex = activePlaylistIndex
        val totalQueueLength = activePlaybackQueue.size

        if (currentIndex >= totalQueueLength - 2) {
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

    // ========================= PLAYLIST MANIPULATION =========================
    fun createCustomPlaylist(name: String) = playlistController.createCustomPlaylist(name)
    fun addSongToPlaylist(playlistId: String, song: Song) = playlistController.addSongToPlaylist(playlistId, song)
    fun removeSongFromPlaylist(playlistId: String, song: Song) = playlistController.removeSongFromPlaylist(playlistId, song)
    fun renamePlaylist(playlistId: String, newName: String) = playlistController.renamePlaylist(playlistId, newName)
    fun deletePlaylist(playlistId: String) = playlistController.deletePlaylist(playlistId)

    fun triggerUpdateCheck(context: Context) {
        viewModelScope.launch { UpdateChecker.check(context, isManualCheck = true) }
    }

    override fun onCleared() {
        super.onCleared()
        if (::stats.isInitialized) {
            stats.stopPlaybackTimer()
        }
        playerController.release()
    }
}