package com.sumit.muzixx.data.network

import android.util.Log
import com.sumit.muzixx.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

@Suppress("DEPRECATION")
class YouTubeAudioExtractor {
    companion object {
        private const val TAG = "YTExtractor"
    }
    private val searchBridge = YouTubeMusicScraper()
    private val preloadedStreamCache = ConcurrentHashMap<String, PreloadedData>()
    private val preloadScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    data class PreloadedData(
        val title: String,
        val artist: String,
        val streamUrl: String,
        val artworkUrl: String,
        val durationMs: Long,
        val quality: String
    )

    private fun sanitizeId(id: String): String {
        return id.replace("yt_", "").trim()
    }

    private fun selectAudioStream(audioStreams: List<AudioStream>?, qualityPref: String): String? {
        if (audioStreams.isNullOrEmpty()) return null

        val validStreams = audioStreams.filter { !it.url.isNullOrBlank() }
        if (validStreams.isEmpty()) return null

        val normalizedPref = qualityPref.lowercase()

        return when {
            normalizedPref.contains("320") || normalizedPref.contains("high") -> {
                validStreams.maxByOrNull { it.bitrate }?.url
            }
            normalizedPref.contains("96") || normalizedPref.contains("low") -> {
                validStreams.filter { it.bitrate >= 32000 }.minByOrNull { it.bitrate }?.url
                    ?: validStreams.minByOrNull { it.bitrate }?.url
            }
            else -> {
                val targetBitrate = 128000
                validStreams.minByOrNull { abs(it.bitrate - targetBitrate) }?.url
            }
        }
    }

    fun preloadStream(videoId: String, qualityPref: String = "160kbps") {
        val sanitizedId = sanitizeId(videoId)
        if (sanitizedId.isBlank()) return

        val cached = preloadedStreamCache[sanitizedId]
        if (cached != null) {
            Log.d(TAG, "ID $sanitizedId already cached in memory. Skipping preload.")
            return
        }

        preloadScope.launch {
            try {
                Log.d(TAG, "[PRELOAD START] Requesting network scrape for: $sanitizedId (Quality: $qualityPref)")
                val url = "https://www.youtube.com/watch?v=$sanitizedId"
                val info = StreamInfo.getInfo(ServiceList.YouTube, url)

                var targetStreamUrl = selectAudioStream(info.audioStreams, qualityPref)

                if (targetStreamUrl.isNullOrBlank()) {
                    targetStreamUrl = info.videoStreams
                        ?.filter { !it.url.isNullOrBlank() }
                        ?.minByOrNull { it.bitrate }
                        ?.url
                }

                if (!targetStreamUrl.isNullOrBlank()) {
                    val artworkUrl = "https://img.youtube.com/vi/$sanitizedId/hqdefault.jpg"

                    preloadedStreamCache[sanitizedId] = PreloadedData(
                        title = info.name ?: "Unknown Track",
                        artist = info.uploaderName ?: "Unknown Artist",
                        streamUrl = targetStreamUrl,
                        artworkUrl = artworkUrl,
                        durationMs = info.duration * 1000L,
                        quality = qualityPref
                    )
                    Log.d(TAG, "[PRELOAD SUCCESS] Cache filled for ID: $sanitizedId")
                } else {
                    Log.e(TAG, "[PRELOAD FAILED] No stream found for: $sanitizedId")
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

    suspend fun getSongFromVideoId(
        videoIdOrQuery: String,
        qualityPref: String = "160kbps",
        originalTitle: String? = null,
        originalArtist: String? = null
    ): Song? = withContext(Dispatchers.IO) {
        try {
            val sanitizedInput = sanitizeId(videoIdOrQuery)
            Log.d(TAG, "[PLAYER REQUEST] Fetch incoming for ID/Query: $sanitizedInput (Quality: $qualityPref)")

            val finalVideoId = if (sanitizedInput.length == 11 && !sanitizedInput.contains(" ")) {
                sanitizedInput
            } else {
                val matches = searchBridge.searchSongs(videoIdOrQuery)
                val topMatch = matches.firstOrNull()?.id?.let { sanitizeId(it) } ?: ""
                if (topMatch.isBlank()) return@withContext null
                topMatch
            }

            val cachedData = preloadedStreamCache[finalVideoId]

            if (cachedData != null) {
                Log.d(TAG, "[CACHE HIT] Playing from cache for: $finalVideoId (Cached Quality: ${cachedData.quality}, Requested: $qualityPref)")
                return@withContext Song(
                    id = "yt_$finalVideoId",
                    title = if (cachedData.title != "Unknown Track") cachedData.title else (originalTitle ?: "YouTube Song"),
                    artist = if (cachedData.artist != "Unknown Artist") cachedData.artist else (originalArtist ?: "YouTube Artist"),
                    uri = cachedData.streamUrl,
                    artUri = cachedData.artworkUrl,
                    duration = cachedData.durationMs,
                    isStreaming = true,
                    folderName = "YouTube Stream",
                    type = "yt"
                )
            }

            Log.w(TAG, "[CACHE MISS] Fetching live extraction for ID: $finalVideoId")
            val url = "https://www.youtube.com/watch?v=$finalVideoId"
            val info = StreamInfo.getInfo(ServiceList.YouTube, url)

            var targetStreamUrl = selectAudioStream(info.audioStreams, qualityPref)

            if (targetStreamUrl.isNullOrBlank()) {
                targetStreamUrl = info.videoStreams
                    ?.filter { !it.url.isNullOrBlank() }
                    ?.minByOrNull { it.bitrate }
                    ?.url
            }

            if (targetStreamUrl.isNullOrBlank()) return@withContext null

            val artworkUrl = "https://img.youtube.com/vi/$finalVideoId/hqdefault.jpg"

            return@withContext Song(
                id = "yt_$finalVideoId",
                title = info.name ?: (originalTitle ?: "Unknown Track"),
                artist = info.uploaderName ?: (originalArtist ?: "Unknown Artist"),
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

    fun clearPreloadCache() {
        Log.d(TAG, "Flashing cache clean due to quality setting change or threshold.")
        preloadedStreamCache.clear()
    }

    fun clearPreloadCacheIfFull() {
        if (preloadedStreamCache.size > 30) {
            clearPreloadCache()
        }
    }

    suspend fun getRelatedSongsFromVideoId(videoId: String): List<Song> = withContext(Dispatchers.IO) {
        try {
            val sanitizedId = sanitizeId(videoId)
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
                        id = finalId,
                        title = item.name ?: "Unknown Track",
                        artist = item.uploaderName ?: "Unknown Artist",
                        uri = "",
                        artUri = artworkUrl,
                        duration = item.duration * 1000L,
                        isStreaming = true,
                        folderName = "YouTube Recommendation",
                        type = "yt"
                    )
                }
        } catch (_: Exception) {
            emptyList()
        }
    }
}