package com.sumit.muzixx.data.network

import android.util.Log
import com.sumit.muzixx.data.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

@Suppress("DEPRECATION")
class YouTubeAudioExtractor {
    companion object {
        private const val TAG = "YTExtractor"
    }
    private val searchBridge = YouTubeMusicScraper()
    private val preloadedStreamCache = ConcurrentHashMap<String, PreloadedData>()
    private val preloadScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    data class PreloadedData(
        val streamUrl: String,
        val artworkUrl: String,
        val durationMs: Long
    )

    fun preloadStream(videoId: String) {
        val sanitizedId = videoId.replace("yt_", "").trim()
        if (sanitizedId.isBlank()) return

        if (preloadedStreamCache.containsKey(sanitizedId)) {
            Log.d(TAG, "ID $sanitizedId already cached. Skipping background fetch.")
            return
        }

        preloadScope.launch {
            try {
                Log.d(TAG, "[PRELOAD START] Requesting network scrape for: $sanitizedId")
                val url = "https://www.youtube.com/watch?v=$sanitizedId"
                val info = StreamInfo.getInfo(ServiceList.YouTube, url)

                var targetStreamUrl = info.audioStreams
                    ?.filter { !it.url.isNullOrBlank() }
                    ?.maxByOrNull { it.bitrate }
                    ?.url

                if (targetStreamUrl.isNullOrBlank()) {
                    targetStreamUrl = info.videoStreams
                        ?.filter { !it.url.isNullOrBlank() }
                        ?.minByOrNull { it.bitrate }
                        ?.url
                }

                if (!targetStreamUrl.isNullOrBlank()) {
                    val artworkUrl = if (info.thumbnails.isNotEmpty()) {
                        checkThumbnailUrl(sanitizedId)
                    } else {
                        "https://img.youtube.com/vi/$sanitizedId/hqdefault.jpg"
                    }

                    preloadedStreamCache[sanitizedId] = PreloadedData(
                        streamUrl = targetStreamUrl,
                        artworkUrl = artworkUrl,
                        durationMs = info.duration * 1000L
                    )
                    Log.d(TAG, "[PRELOAD SUCCESS] Cache filled for ID: $sanitizedId (Total Cached: ${preloadedStreamCache.size})")
                } else {
                    Log.e(TAG, "[PRELOAD FAILED] Scrape completed but found no playable streams for: $sanitizedId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "[PRELOAD ERROR] Failed background fetch for video $sanitizedId: ${e.message}")
            }
        }
    }

    private fun isNonMusicContent(title: String, channel: String): Boolean {
        val lowerTitle = title.lowercase()
        val lowerChannel = channel.lowercase()
        val isMusicMix = lowerTitle.contains("mix") || lowerTitle.contains("lofi") ||
                lowerTitle.contains("remix") || lowerTitle.contains("playlist") ||
                lowerTitle.contains("bgm")
        if (isMusicMix) return false

        val nonMusicKeywords = listOf("podcast", "full episode", "gameplay", "walkthrough", "vlog", "tutorial", "news", "reaction")
        val nonMusicChannels = listOf("gaming", "news", "podcast", "vlogs", "tech")
        return nonMusicKeywords.any { lowerTitle.contains(it) } || nonMusicChannels.any { lowerChannel.contains(it) }
    }

    private fun checkThumbnailUrl(videoId: String): String {
        return try {
            val maxResUrl = "https://img.youtube.com/vi/$videoId/maxresdefault.jpg"
            val connection = URL(maxResUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 800
            connection.readTimeout = 800
            val responseCode = connection.responseCode
            connection.disconnect()
            if (responseCode == HttpURLConnection.HTTP_OK) maxResUrl else "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
        } catch (_: Exception) {
            "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
        }
    }

    suspend fun getSongFromVideoId(videoIdOrQuery: String): Song? = withContext(Dispatchers.IO) {
        try {
            val sanitizedInput = videoIdOrQuery.replace("yt_", "").trim()
            Log.d(TAG, "[PLAYER REQUEST] Fetch incoming for ID/Query: $sanitizedInput")

            val finalVideoId = if (sanitizedInput.length == 11 && !sanitizedInput.contains(" ")) {
                sanitizedInput
            } else {
                val matches = searchBridge.searchSongs(videoIdOrQuery)
                val topMatch = matches.firstOrNull()?.id?.replace("yt_", "") ?: ""
                if (topMatch.isBlank()) return@withContext null
                topMatch
            }

            val cachedData = preloadedStreamCache[finalVideoId] ?: preloadedStreamCache["yt_$finalVideoId"]

            if (cachedData != null) {
                Log.d(TAG, "[CACHE HIT] Perfect! Playing instantly from background cache for: $finalVideoId")
                return@withContext Song(
                    id = "yt_$finalVideoId",
                    title = "Loading...",
                    artist = "YouTube Stream",
                    uri = cachedData.streamUrl,
                    artUri = cachedData.artworkUrl,
                    duration = cachedData.durationMs,
                    isStreaming = true,
                    folderName = "YouTube Stream",
                    type = "yt"
                )
            }

            Log.w(TAG, "[CACHE MISS] Falling back to slow live extraction for ID: $finalVideoId")
            val url = "https://www.youtube.com/watch?v=$finalVideoId"
            val info = StreamInfo.getInfo(ServiceList.YouTube, url)

            var targetStreamUrl = info.audioStreams
                ?.filter { !it.url.isNullOrBlank() }
                ?.maxByOrNull { it.bitrate }
                ?.url

            if (targetStreamUrl.isNullOrBlank()) {
                targetStreamUrl = info.videoStreams
                    ?.filter { !it.url.isNullOrBlank() }
                    ?.minByOrNull { it.bitrate }
                    ?.url
            }

            if (targetStreamUrl.isNullOrBlank()) return@withContext null

            val artworkUrl = if (info.thumbnails.isNotEmpty()) checkThumbnailUrl(finalVideoId) else ""

            return@withContext Song(
                id = "yt_$finalVideoId",
                title = info.name ?: "Unknown",
                artist = info.uploaderName ?: "Unknown",
                uri = targetStreamUrl,
                artUri = artworkUrl,
                duration = info.duration * 1000L,
                isStreaming = true,
                folderName = "YouTube Stream",
                type = "yt"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Extractor pipeline crash", e)
            null
        }
    }

    fun clearPreloadCacheIfFull() {
        if (preloadedStreamCache.size > 30) {
            Log.d(TAG, "Preload buffer threshold hit. Flashing cache clean.")
            preloadedStreamCache.clear()
        }
    }

    suspend fun getRelatedSongsFromVideoId(videoId: String): List<Song> = withContext(Dispatchers.IO) {
        try {
            val sanitizedId = videoId.replace("yt_", "").trim()
            val url = "https://www.youtube.com/watch?v=$sanitizedId"
            val info = StreamInfo.getInfo(ServiceList.YouTube, url)
            val relatedItems = info.relatedItems ?: return@withContext emptyList()

            return@withContext relatedItems
                .filter { it.url != null && it is org.schabi.newpipe.extractor.stream.StreamInfoItem }
                .map { it as org.schabi.newpipe.extractor.stream.StreamInfoItem }
                .filter { !isNonMusicContent(it.name ?: "", it.uploaderName ?: "") }
                .map { item ->
                    val extractedId = item.url?.substringAfter("v=")?.substringBefore("&") ?: ""
                    val finalId = if (extractedId.isNotBlank()) "yt_$extractedId" else item.name ?: ""
                    val artworkUrl = if (extractedId.isNotBlank()) "https://img.youtube.com/vi/$extractedId/hqdefault.jpg" else ""

                    Song(
                        id = finalId, title = item.name ?: "Unknown Track", artist = item.uploaderName ?: "Unknown Artist",
                        uri = "", artUri = artworkUrl, duration = item.duration * 1000L, isStreaming = true,
                        folderName = "YouTube Recommendation", type = "yt"
                    )
                }
        } catch (_: Exception) {
            emptyList()
        }
    }
}