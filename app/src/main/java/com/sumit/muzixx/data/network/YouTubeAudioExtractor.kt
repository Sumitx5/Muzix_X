package com.sumit.muzixx.data.network

import android.util.Log
import com.sumit.muzixx.data.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.net.HttpURLConnection
import java.net.URL

@Suppress("DEPRECATION")
class YouTubeAudioExtractor {
    companion object {
        private const val TAG = "YTExtractor"
    }
    private val searchBridge = YouTubeMusicScraper()
    private fun isNonMusicContent(title: String, channel: String): Boolean {
        val lowerTitle = title.lowercase()
        val lowerChannel = channel.lowercase()

        val isMusicMix = lowerTitle.contains("mix") ||
                lowerTitle.contains("lofi") ||
                lowerTitle.contains("remix") ||
                lowerTitle.contains("playlist") ||
                lowerTitle.contains("bgm")

        if (isMusicMix) return false

        val nonMusicKeywords = listOf(
            "podcast", "full episode", "gameplay", "walkthrough", "review",
            "unboxing", "vlog", "tutorial", "news", "reaction", "interview"
        )
        val nonMusicChannels = listOf(
            "gaming", "news", "podcast", "vlogs", "tv", "series", "drama", "tech"
        )

        val hasBadKeyword = nonMusicKeywords.any { lowerTitle.contains(it) }
        val hasBadChannel = nonMusicChannels.any { lowerChannel.contains(it) }

        return hasBadKeyword || hasBadChannel
    }

    private fun checkThumbnailUrl(videoId: String): String {
        return try {
            val maxResUrl = "https://img.youtube.com/vi/$videoId/maxresdefault.jpg"
            val connection = URL(maxResUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 1000
            connection.readTimeout = 1000

            val responseCode = connection.responseCode
            connection.disconnect()

            if (responseCode == HttpURLConnection.HTTP_OK) {
                maxResUrl
            } else {
                "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
            }
        } catch (_: Exception) {
            "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
        }
    }

    suspend fun getSongFromVideoId(videoIdOrQuery: String): Song? = withContext(Dispatchers.IO) {
        try {
            val sanitizedInput = videoIdOrQuery.replace("yt_", "").trim()

            val finalVideoId = if (sanitizedInput.length == 11 && !sanitizedInput.contains(" ")) {
                sanitizedInput
            } else {
                Log.d(TAG, "Resolving text fallback query via Scraper: $videoIdOrQuery")
                val matches = searchBridge.searchSongs(videoIdOrQuery)
                val topMatch = matches.firstOrNull()?.id?.replace("yt_", "") ?: ""

                if (topMatch.isBlank()) {
                    Log.e(TAG, "No match found on YouTube for query: $videoIdOrQuery")
                    return@withContext null
                }
                topMatch
            }

            val url = "https://www.youtube.com/watch?v=$finalVideoId"
            val info = StreamInfo.getInfo(ServiceList.YouTube, url)
            if (isNonMusicContent(info.name ?: "", info.uploaderName ?: "")) {
                Log.w(TAG, "Blocked loading main search result as it looks like non-music video media.")
                return@withContext null
            }

            var targetStreamUrl = info.audioStreams
                ?.filter { !it.url.isNullOrBlank() }
                ?.maxByOrNull { it.bitrate }
                ?.url

            if (targetStreamUrl.isNullOrBlank()) {
                Log.d(TAG, "Pure audio streams empty for $finalVideoId. Attempting video stream fallback...")
                targetStreamUrl = info.videoStreams
                    ?.filter { !it.url.isNullOrBlank() }
                    ?.minByOrNull { it.bitrate }
                    ?.url
            }

            if (targetStreamUrl.isNullOrBlank()) {
                Log.e(TAG, "No valid audio or video stream link fetched for $finalVideoId")
                return@withContext null
            }

            val artworkUrl = if (finalVideoId.isNotBlank()) {
                if (info.thumbnails.isNotEmpty()) {
                    checkThumbnailUrl(finalVideoId)
                } else {
                    "https://img.youtube.com/vi/$finalVideoId/hqdefault.jpg"
                }
            } else {
                ""
            }

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
            Log.e(TAG, "Extractor process failed unexpectedly", e)
            null
        }
    }

    suspend fun getRelatedSongsFromVideoId(videoId: String): List<Song> = withContext(Dispatchers.IO) {
        try {
            val sanitizedId = videoId.replace("yt_", "").trim()
            val url = "https://www.youtube.com/watch?v=$sanitizedId"
            val info = StreamInfo.getInfo(ServiceList.YouTube, url)
            val relatedItems = info.relatedItems ?: return@withContext emptyList()

            Log.d(TAG, "NewPipe Extractor pulled ${relatedItems.size} structural recommendations for video: $sanitizedId")

            return@withContext relatedItems
                .filter { item ->
                    item.url != null && item is org.schabi.newpipe.extractor.stream.StreamInfoItem
                }
                .map { item -> item as org.schabi.newpipe.extractor.stream.StreamInfoItem }
                .filter { streamItem ->
                    val itemTitle = streamItem.name ?: ""
                    val itemChannel = streamItem.uploaderName ?: ""

                    val isTrashContent = isNonMusicContent(itemTitle, itemChannel)

                    if (isTrashContent) {
                        Log.d(TAG, "Filtered out non-music media recommendation: $itemTitle")
                    }

                    !isTrashContent
                }
                .map { item ->
                    val extractedId = item.url?.substringAfter("v=")?.substringBefore("&") ?: ""
                    val finalId = if (extractedId.isNotBlank()) "yt_$extractedId" else item.name ?: ""

                    val artworkUrl = if (extractedId.isNotBlank()) {
                        if (item.thumbnails.isNotEmpty()) {
                            checkThumbnailUrl(extractedId)
                        } else {
                            "https://img.youtube.com/vi/$extractedId/hqdefault.jpg"
                        }
                    } else {
                        ""
                    }

                    val resolvedArtist = item.uploaderName ?: "Unknown Artist"

                    Song(
                        id = finalId,
                        title = item.name ?: "Unknown Track",
                        artist = resolvedArtist,
                        uri = "",
                        artUri = artworkUrl,
                        duration = item.duration * 1000L,
                        isStreaming = true,
                        folderName = "YouTube Recommendation",
                        type = "yt"
                    )
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pull related tracks via NewPipe Extractor channel", e)
            emptyList()
        }
    }
}