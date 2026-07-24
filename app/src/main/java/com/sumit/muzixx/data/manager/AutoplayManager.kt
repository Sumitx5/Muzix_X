package com.sumit.muzixx.data.manager

import android.util.Log
import com.sumit.muzixx.data.Song
import com.sumit.muzixx.data.model.toSong
import com.sumit.muzixx.data.network.JioSaavnApiService
import com.sumit.muzixx.data.network.YouTubeAudioExtractor
import com.sumit.muzixx.data.network.YouTubeMusicScraper

class AutoplayManager(
    private val ytScraper: YouTubeMusicScraper,
    private val ytExtractor: YouTubeAudioExtractor,
    private val jioSaavnApiService: JioSaavnApiService
) {

    suspend fun buildBootQueue(currentSong: Song): List<Song> {
        if (currentSong.type.lowercase().trim() == "local") return listOf(currentSong)

        val combinedQueue = mutableListOf(currentSong)
        try {
            val isJioSaavn = currentSong.id.all { it.isDigit() } ||
                    currentSong.artist.contains("JioSaavn") ||
                    currentSong.title.contains("JioSaavn")

            if (isJioSaavn) {
                val queryBuilder = StringBuilder(currentSong.title)
                if (currentSong.artist.isNotBlank() && currentSong.artist.lowercase() != "unknown") {
                    queryBuilder.append(" ").append(currentSong.artist)
                }
                val searchQuery = queryBuilder.toString().trim()
                val ytSearchResults = ytScraper.searchSongs(searchQuery)
                if (ytSearchResults.isNotEmpty()) {
                    val firstYtMatch = ytSearchResults.first()
                    val targetVideoId = firstYtMatch.id.replace("yt_", "").trim()
                    val recommendations = ytExtractor.getRelatedSongsFromVideoId(targetVideoId)
                    val lowerTitle = currentSong.title.lowercase().trim()

                    val uniqueRecs = recommendations.filter { rec ->
                        rec.title.lowercase().trim() != lowerTitle
                    }
                    combinedQueue.addAll(uniqueRecs)
                }
            } else {
                val targetVideoId = currentSong.id.replace("yt_", "").trim()
                val recommendations = ytExtractor.getRelatedSongsFromVideoId(targetVideoId)
                if (recommendations.isNotEmpty()) {
                    val uniqueRecs = recommendations.filter { rec ->
                        rec.id.replace("yt_", "").trim() != targetVideoId &&
                                rec.title.lowercase().trim() != currentSong.title.lowercase().trim()
                    }
                    combinedQueue.addAll(uniqueRecs)
                }
            }
        } catch (e: Exception) {
            Log.e("AutoplayManager", "Failed building boot queue recommendations", e)
        }
        return combinedQueue
    }

    suspend fun fetchJioSaavnWithYouTubeRecommendations(saavnSong: Song): List<Song> {
        val queryBuilder = StringBuilder(saavnSong.title)
        if (saavnSong.artist.isNotBlank() && saavnSong.artist.lowercase() != "unknown") {
            queryBuilder.append(" ").append(saavnSong.artist)
        }
        val searchQuery = queryBuilder.toString().trim()
        val ytSearchResults = ytScraper.searchSongs(searchQuery)

        if (ytSearchResults.isNotEmpty()) {
            val firstYtMatch = ytSearchResults.first()
            val targetVideoId = firstYtMatch.id.replace("yt_", "").trim()
            val recommendations = ytExtractor.getRelatedSongsFromVideoId(targetVideoId)
            val lowerTitle = saavnSong.title.lowercase().trim()

            return recommendations.filter { rec ->
                rec.title.lowercase().trim() != lowerTitle
            }
        }
        return emptyList()
    }

    suspend fun fetchYouTubeAutoplayQueue(youtubeSong: Song): List<Song> {
        val finalQueue = mutableListOf(youtubeSong)
        try {
            val targetVideoId = youtubeSong.id.replace("yt_", "").trim()
            val recommendations = ytExtractor.getRelatedSongsFromVideoId(targetVideoId)
            if (recommendations.isNotEmpty()) {
                val targetLowerTitle = youtubeSong.title.lowercase().trim()
                val uniqueRecs = recommendations.filter { rec ->
                    val recNormalizedId = rec.id.replace("yt_", "").trim()
                    val recLowerTitle = rec.title.lowercase().trim()
                    recNormalizedId != targetVideoId && recLowerTitle != targetLowerTitle
                }
                finalQueue.addAll(uniqueRecs)
            }
        } catch (e: Exception) {
            Log.e("AutoplayManager", "YouTube autoplay fetch failed", e)
        }
        return finalQueue
    }

    suspend fun fetchNextLookaheadTracks(currentSong: Song): List<Song> {
        val isSaavn = currentSong.id.all { it.isDigit() } ||
                currentSong.artist.contains("JioSaavn") ||
                currentSong.title.contains("JioSaavn")

        if (isSaavn) {
            val response = jioSaavnApiService.getSongSuggestions(currentSong.id)
            return response.data?.songs?.map { it.toSong("Autoplay Suggestion") } ?: emptyList()
        } else {
            val targetVideoId = currentSong.id.replace("yt_", "").trim()
            val relatedSongs = ytExtractor.getRelatedSongsFromVideoId(targetVideoId)
            return relatedSongs.filter { rec ->
                rec.id.replace("yt_", "").trim() != targetVideoId
            }
        }
    }
}