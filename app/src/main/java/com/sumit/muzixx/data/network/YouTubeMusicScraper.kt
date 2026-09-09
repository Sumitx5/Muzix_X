package com.sumit.muzixx.data.network

import android.util.Log
import com.sumit.muzixx.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.search.SearchExtractor
import org.schabi.newpipe.extractor.stream.StreamInfoItem

class YouTubeMusicScraper {
    private val videoIdRegex = "(?:v=|/v/|embed/|youtu\\.be/|/shorts/)([^\"&?/\\s]{11})".toRegex()

    private val titleCleanerRegex = Regex(
        "(?i)\\(official video\\)|\\[official video]|\\(official audio\\)|\\[official audio]|\\(lyric video\\)|\\(audio\\)"
    )

    private val musicKeywords = listOf(
        "music", "official video", "official audio", "lyric", "lyrics",
        "song", "full song", "remix", "soundtrack", "ost", "prod", "ft.",
        "feat", "album", "single", "vevo", "records", "label", "topic",
        "phonk", "brazilian phonk", "drift phonk", "mix", "playlist",
        "compilation", "bass boosted", "slowed", "reverb", "mashup",
        "1 hour", "2 hour", "hours", "lofi", "chillhop", "beat", "instrumental"
    )

    suspend fun searchSongs(query: String): List<Song> = withContext(Dispatchers.IO) {
        try {
            val list = mutableListOf<Song>()
            val trimmedQuery = query.trim()

            val hasExplicitIntent = listOf(
                "music", "song", "phonk", "mix", "playlist", "remix",
                "mashup", "lofi", "audio", "track", "album", "soundtrack"
            ).any { trimmedQuery.contains(it, ignoreCase = true) }

            val searchQuery = if (hasExplicitIntent) {
                trimmedQuery
            } else {
                "$trimmedQuery song"
            }

            val extractor: SearchExtractor = ServiceList.YouTube.getSearchExtractor(searchQuery)
            extractor.fetchPage()

            for (item in extractor.initialPage.items) {
                if (item is StreamInfoItem) {
                    val videoUrl = item.url ?: continue

                    if (videoUrl.contains("/shorts/", ignoreCase = true)) continue

                    val durationSeconds = item.duration

                    if (durationSeconds < 30) continue

                    val title = item.name ?: ""
                    val artist = item.uploaderName ?: ""
                    val combinedText = "$title $artist".lowercase()

                    val isMusicChannel = artist.contains("- Topic", ignoreCase = true) ||
                            artist.contains("VEVO", ignoreCase = true) ||
                            artist.contains("Records", ignoreCase = true) ||
                            artist.contains("Music", ignoreCase = true)

                    val containsMusicKeywords = musicKeywords.any { keyword ->
                        combinedText.contains(keyword)
                    }

                    if (!isMusicChannel && !containsMusicKeywords) {
                        continue
                    }

                    val matchResult = videoIdRegex.find(videoUrl)
                    val videoId = matchResult?.groupValues?.get(1) ?: continue

                    val artworkUrl = item.thumbnails.firstOrNull()?.url
                        ?: "https://img.youtube.com/vi/$videoId/hqdefault.jpg"

                    val cleanedTitle = title.replace(titleCleanerRegex, "").trim()

                    list.add(
                        Song(
                            id = "yt_$videoId",
                            title = cleanedTitle.ifBlank { title },
                            artist = artist.removeSuffix(" - Topic").trim(),
                            uri = "",
                            artUri = artworkUrl,
                            duration = durationSeconds * 1000L,
                            isStreaming = true,
                            folderName = "YouTube Music",
                            type = "yt"
                        )
                    )
                }
            }
            return@withContext list
        } catch (e: Exception) {
            Log.e("YT_SCRAPER_ERROR", "Failed scraping YouTube matches", e)
            return@withContext emptyList()
        }
    }
}