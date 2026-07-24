package com.sumit.muzixx.utils

import android.util.Log
import com.sumit.muzixx.data.Song
import com.sumit.muzixx.data.network.YouTubeAudioExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object YouTubeResolver {

    private val ytExtractor = YouTubeAudioExtractor()

    suspend fun resolveStreamUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        val cleanId = videoId.replace("yt_", "").trim()
        if (cleanId.isBlank()) return@withContext null

        try {
            Log.d("YouTubeResolver", "Extracting audio stream for videoId: $cleanId")
            val extractedSong = ytExtractor.getSongFromVideoId(cleanId)
            extractedSong?.uri
        } catch (e: Exception) {
            Log.e("YouTubeResolver", "Failed to extract audio stream for $cleanId", e)
            null
        }
    }

    suspend fun resolveDeepLinkSong(
        songId: String,
        title: String,
        artist: String,
        rawArt: String
    ): Song = withContext(Dispatchers.IO) {
        val isYoutube = songId.startsWith("yt_")
        val cleanYtId = if (isYoutube) songId.removePrefix("yt_") else ""

        val reconstructedArtUri = when {
            isYoutube -> "https://img.youtube.com/vi/$cleanYtId/mqdefault.jpg"
            rawArt.isNotBlank() && !rawArt.startsWith("http") -> "https://c.saavncdn.com/$rawArt"
            else -> rawArt
        }

        val streamUrl = if (isYoutube) {
            resolveStreamUrl(cleanYtId) ?: ""
        } else {
            ""
        }

        Song(
            id = songId,
            title = title,
            artist = artist,
            uri = streamUrl,
            artUri = reconstructedArtUri,
            duration = 0,
            isStreaming = true,
            folderName = "",
            type = if (isYoutube) "yt" else "saavn"
        )
    }
}