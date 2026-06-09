package com.sumit.muzixx.data.network

import android.util.Log
import com.sumit.muzixx.data.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.search.SearchExtractor
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfoItem

class YouTubeMusicScraper {

    private val videoIdRegex = "(?:v=|\\/v\\/|embed\\/|youtu\\.be\\/|\\/shorts\\/)([^\"&?\\/\\s]{11})".toRegex()

    suspend fun searchSongs(query: String): List<Song> = withContext(Dispatchers.IO) {
        try {
            val list = mutableListOf<Song>()

            val extractor: SearchExtractor = ServiceList.YouTube.getSearchExtractor(query)
            extractor.fetchPage()

            for (item in extractor.initialPage.items) {
                if (item is StreamInfoItem) {
                    val videoUrl = item.url ?: continue

                    val matchResult = videoIdRegex.find(videoUrl)
                    val videoId = matchResult?.groupValues?.get(1) ?: continue

                    val artworkUrl = item.thumbnails?.firstOrNull()?.url
                        ?: "https://img.youtube.com/vi/$videoId/hqdefault.jpg"

                    list.add(
                        Song(
                            id = "yt_$videoId",
                            title = item.name ?: "Unknown Title",
                            artist = item.uploaderName ?: "Unknown Artist",
                            uri = "",
                            artUri = artworkUrl,
                            duration = item.duration * 1000L,
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