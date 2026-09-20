package com.sumit.muzixx.data.network

import android.util.Log
import com.sumit.muzixx.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.util.regex.Pattern

data class YouTubeImportResult(
    val playlistName: String,
    val songs: List<Song>
)

class YouTubePlaylistImporter {

    companion object {
        private const val TAG = "YTPlaylistImporter"
        private const val MAX_IMPORT_LIMIT = 400
    }

    suspend fun fetchPlaylistTracks(url: String): YouTubeImportResult = withContext(Dispatchers.IO) {
        val emptyResult = YouTubeImportResult(playlistName = "Imported YT Playlist", songs = emptyList())
        val playlistId = extractPlaylistId(url) ?: return@withContext emptyResult

        val normalizedUrl = "https://www.youtube.com/playlist?list=$playlistId"

        try {
            Log.d(TAG, "Starting extraction for playlist ID: $playlistId")
            val playlistInfo = PlaylistInfo.getInfo(ServiceList.YouTube, normalizedUrl)

            val playlistTitle = playlistInfo.name?.ifBlank { null } ?: "YouTube Playlist"
            val importedSongs = mutableListOf<Song>()

            playlistInfo.relatedItems?.let { items ->
                for (item in items) {
                    if (item is StreamInfoItem) {
                        mapStreamItemToSong(item)?.let { importedSongs.add(it) }
                        if (importedSongs.size >= MAX_IMPORT_LIMIT) break
                    }
                }
            }

            var currentInfo = playlistInfo
            while (currentInfo.hasNextPage() && importedSongs.size < MAX_IMPORT_LIMIT) {
                try {
                    val nextPage = PlaylistInfo.getMoreItems(ServiceList.YouTube, normalizedUrl, currentInfo.nextPage)
                    val nextItems = nextPage.items ?: break

                    if (nextItems.isEmpty()) break

                    for (item in nextItems) {
                        if (item is StreamInfoItem) {
                            mapStreamItemToSong(item)?.let { importedSongs.add(it) }
                            if (importedSongs.size >= MAX_IMPORT_LIMIT) break
                        }
                    }

                    if (!nextPage.hasNextPage()) break
                    currentInfo = PlaylistInfo.getInfo(ServiceList.YouTube, normalizedUrl)
                } catch (pageError: Exception) {
                    Log.w(TAG, "Pagination terminated or failed: ${pageError.message}")
                    break
                }
            }

            Log.d(TAG, "Imported ${importedSongs.size} tracks from: $playlistTitle")
            return@withContext YouTubeImportResult(playlistTitle, importedSongs)

        } catch (e: Exception) {
            Log.e(TAG, "Failed extracting playlist from $url", e)
            return@withContext emptyResult
        }
    }

    private fun extractPlaylistId(url: String): String? {
        val pattern = Pattern.compile("list=([a-zA-Z0-9_-]+)")
        val matcher = pattern.matcher(url)
        return if (matcher.find()) matcher.group(1) else null
    }

    private fun mapStreamItemToSong(item: StreamInfoItem): Song? {
        val videoUrl = item.url ?: return null
        val videoId = videoUrl.substringAfter("v=").substringBefore("&").trim()
        if (videoId.isBlank()) return null

        val artworkUrl = item.thumbnails.lastOrNull()?.url
            ?: "https://img.youtube.com/vi/$videoId/hqdefault.jpg"

        return Song(
            id = "yt_$videoId",
            title = item.name ?: "Unknown Title",
            artist = item.uploaderName ?: "Unknown Artist",
            uri = "",
            artUri = artworkUrl,
            duration = (item.duration * 1000L).coerceAtLeast(0L),
            isStreaming = true,
            folderName = "YouTube Imported",
            type = "yt"
        )
    }
}