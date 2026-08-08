package com.sumit.muzixx.data.manager

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sumit.muzixx.data.model.Playlist
import com.sumit.muzixx.data.model.SaavnCloudPlaylistObject
import com.sumit.muzixx.data.model.Song
import com.sumit.muzixx.data.model.toSong
import com.sumit.muzixx.data.network.JioSaavnApiService
import com.sumit.muzixx.data.network.SpotifyImporter
import com.sumit.muzixx.data.network.YouTubeAudioExtractor
import com.sumit.muzixx.data.network.YouTubeMusicScraper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ContentManager(
    private val scope: CoroutineScope,
    private val jioSaavnApiService: JioSaavnApiService,
    private val ytScraper: YouTubeMusicScraper,
    private val ytExtractor: YouTubeAudioExtractor,
    private val autoplayManager: AutoplayManager,
    private val spotifyImporter: SpotifyImporter,
    private val createPlaylistCallback: (String, List<Song>) -> Playlist?
) {
    val saavnTrendingSongs = mutableStateListOf<Song>()
    val saavnNewReleases = mutableStateListOf<Song>()
    val saavnHminiHits = mutableStateListOf<Song>()
    val recommendedSongs = mutableStateListOf<Song>()
    val youtubeTrendingSongs = mutableStateListOf<Song>()
    val currentCloudPlaylistSongs = mutableStateListOf<Song>()

    var currentCloudPlaylistName by mutableStateOf<String?>(null)
    var isYouTubeTrendingLoading by mutableStateOf(false)
        private set
    var isRecommendationsLoading by mutableStateOf(false)
        private set
    var isTrendingLoading by mutableStateOf(false)
        private set
    var isNewReleasesLoading by mutableStateOf(false)
        private set
    var isHindiHitLoading by mutableStateOf(false)
        private set
    var isCloudPlaylistLoading by mutableStateOf(false)
        private set

    private var hasFetchedRecommendations = false
    private var hasFetchedYouTubeTrending = false

    fun loadJioSaavnHomeContent() {
        if (isTrendingLoading || isNewReleasesLoading || isHindiHitLoading) return
        isTrendingLoading = true
        isNewReleasesLoading = true
        isHindiHitLoading = true

        scope.launch(Dispatchers.IO) {
            try {
                val trendingDef = async { runCatching { jioSaavnApiService.getPlaylistDetails("1774824") }.getOrNull() }
                val newRelDef = async { runCatching { jioSaavnApiService.getPlaylistDetails("153668826") }.getOrNull() }
                val hindiHitsDef = async { runCatching { jioSaavnApiService.getPlaylistDetails("1134543272") }.getOrNull() }

                val chuddyBuddies = trendingDef.await()?.data?.songs?.map { it.toSong("JioSaavn Chuddy Buddies") } ?: emptyList()
                val baarishOrDance = newRelDef.await()?.data?.songs?.map { it.toSong("JioSaavn Baarish Or Dance") } ?: emptyList()
                val hindiHits = hindiHitsDef.await()?.data?.songs?.map { it.toSong("Hindi Hits") } ?: emptyList()

                withContext(Dispatchers.Main) {
                    saavnTrendingSongs.apply { clear(); addAll(chuddyBuddies) }
                    saavnNewReleases.apply { clear(); addAll(baarishOrDance) }
                    saavnHminiHits.apply { clear(); addAll(hindiHits) }
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isTrendingLoading = false; isNewReleasesLoading = false; isHindiHitLoading = false
                }
            }
        }
    }

    fun fetchRecommendationsFromHistory(history: List<Song>) {
        if (hasFetchedRecommendations && recommendedSongs.isNotEmpty()) return
        val seedTrack = history.firstOrNull { it.id.isNotBlank() } ?: return

        scope.launch {
            isRecommendationsLoading = true
            try {
                val recs = withContext(Dispatchers.IO) {
                    if (seedTrack.id.startsWith("yt_")) {
                        autoplayManager.fetchYouTubeAutoplayQueue(seedTrack)
                    } else {
                        autoplayManager.fetchJioSaavnWithYouTubeRecommendations(seedTrack)
                    }
                }

                val historyIds = history.map { it.id }.toSet()
                val filteredRecs = recs.filter { it.id !in historyIds && it.id.isNotBlank() }.take(8)

                if (filteredRecs.isNotEmpty()) {
                    recommendedSongs.clear()
                    recommendedSongs.addAll(filteredRecs)
                    hasFetchedRecommendations = true
                    filteredRecs.firstOrNull()?.let { topSong ->
                        val cleanYtId = topSong.id.removePrefix("yt_")
                        ytExtractor.preloadStream(cleanYtId)
                    }
                }
            } catch (e: Exception) {
                Log.e("ContentManager", "Failed to fetch recommended songs", e)
            } finally {
                isRecommendationsLoading = false
            }
        }
    }

    fun loadYouTubeTrendingSongs() {
        if (hasFetchedYouTubeTrending && youtubeTrendingSongs.isNotEmpty()) return

        scope.launch {
            isYouTubeTrendingLoading = true
            try {
                val trendingResults = withContext(Dispatchers.IO) {
                    ytScraper.searchSongs("Trending Today Official Songs")
                }

                val topTrending = trendingResults.take(10)

                if (topTrending.isNotEmpty()) {
                    youtubeTrendingSongs.clear()
                    youtubeTrendingSongs.addAll(topTrending)
                    hasFetchedYouTubeTrending = true
                    topTrending.firstOrNull()?.let { topSong ->
                        val cleanYtId = topSong.id.removePrefix("yt_")
                        ytExtractor.preloadStream(cleanYtId)
                    }
                }
            } catch (e: Exception) {
                Log.e("ContentManager", "Failed fetching YouTube trending songs", e)
            } finally {
                isYouTubeTrendingLoading = false
            }
        }
    }

    suspend fun searchJioSaavnPlaylists(query: String): List<SaavnCloudPlaylistObject> {
        if (query.isBlank()) return emptyList()
        return withContext(Dispatchers.IO) {
            try {
                val response = jioSaavnApiService.searchPlaylists(query)
                if (response.success) response.data?.results ?: emptyList() else emptyList()
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    fun loadCloudPlaylistDetails(playlistId: String, playlistName: String) {
        if (playlistId.isBlank()) return
        scope.launch {
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

    fun importSpotifyPlaylist(
        url: String,
        onSuccess: (playlistName: String, count: Int) -> Unit,
        onError: (String) -> Unit
    ) {
        if (url.isBlank()) {
            onError("Please enter a valid Spotify playlist URL")
            return
        }

        scope.launch {
            try {
                val importResult = spotifyImporter.fetchPlaylistTracks(url)
                val spotifyTracks = importResult.tracks

                if (spotifyTracks.isEmpty()) {
                    onError("Failed to parse Spotify playlist. Ensure the link is public.")
                    return@launch
                }

                val resolvedSongs = withContext(Dispatchers.IO) {
                    spotifyTracks.map { spotifyTrack ->
                        async {
                            try {
                                val query = "${spotifyTrack.title} ${spotifyTrack.artist}"
                                val results = ytScraper.searchSongs(query)
                                results.firstOrNull()
                            } catch (_: Exception) {
                                null
                            }
                        }
                    }.awaitAll().filterNotNull()
                }

                if (resolvedSongs.isNotEmpty()) {
                    val playlistName = importResult.playlistName
                    createPlaylistCallback(playlistName, resolvedSongs)
                    onSuccess(playlistName, resolvedSongs.size)
                } else {
                    onError("Could not resolve playable audio streams for tracks in this playlist.")
                }
            } catch (e: Exception) {
                onError("Import failed: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }
}