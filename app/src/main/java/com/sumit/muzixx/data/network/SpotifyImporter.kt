package com.sumit.muzixx.data.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.regex.Pattern

data class SpotifyTrack(
    val title: String,
    val artist: String
)

data class SpotifyImportResult(
    val playlistName: String,
    val tracks: List<SpotifyTrack>
)

class SpotifyImporter(private val client: OkHttpClient = OkHttpClient()) {

    companion object {
        private const val TAG = "SpotifyImporter"
        private const val MAX_IMPORT_LIMIT = 400
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
    }

    suspend fun fetchPlaylistTracks(url: String): SpotifyImportResult = withContext(Dispatchers.IO) {
        val emptyResult = SpotifyImportResult(playlistName = "Imported Playlist", tracks = emptyList())
        val playlistId = extractPlaylistId(url) ?: return@withContext emptyResult
        val embedUrl = "https://open.spotify.com/embed/playlist/$playlistId"

        val request = Request.Builder()
            .url(embedUrl)
            .header("User-Agent", USER_AGENT)
            .header("Accept-Language", "en-US,en;q=0.9")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyResult
                val html = response.body?.string() ?: return@withContext emptyResult

                val jsonString = extractScriptContent(html, "__NEXT_DATA__")
                    ?: extractScriptContent(html, "session")
                    ?: extractGenericJson(html)
                    ?: return@withContext emptyResult

                val initialResult = parsePlaylistFromJson(jsonString)
                val totalDeclaredTracks = extractTotalCount(jsonString)
                val accessToken = extractAccessToken(jsonString)

                if (accessToken != null && totalDeclaredTracks > initialResult.tracks.size) {
                    val extendedTracks = fetchRemainingTracks(
                        playlistId = playlistId,
                        token = accessToken,
                        initialTracks = initialResult.tracks,
                        maxLimit = MAX_IMPORT_LIMIT
                    )
                    return@withContext SpotifyImportResult(initialResult.playlistName, extendedTracks)
                }

                return@withContext initialResult
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import Spotify playlist", e)
            emptyResult
        }
    }

    private fun extractPlaylistId(url: String): String? {
        val pattern = Pattern.compile("playlist[/:]([a-zA-Z0-9]+)")
        val matcher = pattern.matcher(url)
        return if (matcher.find()) matcher.group(1) else null
    }

    private fun extractScriptContent(html: String, scriptId: String): String? {
        val regex = Regex("""<script\s+id="$scriptId"\s+type="application/json">(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)
        return regex.find(html)?.groupValues?.get(1)
    }

    private fun extractGenericJson(html: String): String? {
        val regex = Regex("""<script\s+type="application/json">(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)
        return regex.find(html)?.groupValues?.get(1)
    }

    private fun extractAccessToken(jsonStr: String): String? {
        return try {
            val root = JSONObject(jsonStr)
            root.optJSONObject("props")
                ?.optJSONObject("pageProps")
                ?.optString("accessToken")
                ?.ifEmpty { null }
                ?: root.optString("accessToken").ifEmpty { null }
        } catch (_: Exception) {
            null
        }
    }

    private fun extractTotalCount(jsonStr: String): Int {
        return try {
            val root = JSONObject(jsonStr)
            val pageProps = root.optJSONObject("props")?.optJSONObject("pageProps")
            val entity = pageProps?.optJSONObject("state")?.optJSONObject("data")?.optJSONObject("entity")
                ?: pageProps?.optJSONObject("entity")

            entity?.optJSONObject("tracks")?.optInt("total", 0) ?: 0
        } catch (_: Exception) {
            0
        }
    }

    private fun fetchRemainingTracks(
        playlistId: String,
        token: String,
        initialTracks: List<SpotifyTrack>,
        maxLimit: Int
    ): List<SpotifyTrack> {
        val allTracks = initialTracks.toMutableList()
        var offset = initialTracks.size

        while (allTracks.size < maxLimit) {
            val limit = (maxLimit - allTracks.size).coerceAtMost(100)
            val apiUrl = "https://api.spotify.com/v1/playlists/$playlistId/tracks?offset=$offset&limit=$limit"

            val apiRequest = Request.Builder()
                .url(apiUrl)
                .header("Authorization", "Bearer $token")
                .header("User-Agent", USER_AGENT)
                .build()

            try {
                client.newCall(apiRequest).execute().use { response ->
                    if (!response.isSuccessful) return allTracks
                    val bodyStr = response.body?.string() ?: return allTracks
                    val root = JSONObject(bodyStr)
                    val items = root.optJSONArray("items") ?: return allTracks

                    if (items.length() == 0) return allTracks

                    for (i in 0 until items.length()) {
                        val itemObj = items.getJSONObject(i)
                        val trackObj = itemObj.optJSONObject("track") ?: continue
                        val title = trackObj.optString("name")
                        val artistsArray = trackObj.optJSONArray("artists")
                        val artist = if (artistsArray != null && artistsArray.length() > 0) {
                            artistsArray.getJSONObject(0).optString("name")
                        } else ""

                        if (title.isNotEmpty()) {
                            allTracks.add(SpotifyTrack(title, artist))
                            if (allTracks.size >= maxLimit) break
                        }
                    }

                    offset += items.length()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Pagination error at offset $offset: ${e.message}")
                break
            }
        }

        return allTracks
    }

    private fun parsePlaylistFromJson(jsonStr: String): SpotifyImportResult {
        val tracks = mutableListOf<SpotifyTrack>()
        var playlistName = "Imported Playlist"

        try {
            val root = JSONObject(jsonStr)

            val pageProps = root.optJSONObject("props")?.optJSONObject("pageProps")
            val entity = pageProps?.optJSONObject("state")?.optJSONObject("data")?.optJSONObject("entity")
                ?: pageProps?.optJSONObject("entity")

            if (entity != null) {
                val extractedTitle = entity.optString("title")
                    .ifEmpty { entity.optString("name") }
                if (extractedTitle.isNotEmpty()) {
                    playlistName = extractedTitle
                }

                val trackList = entity.optJSONArray("trackList")
                if (trackList != null) {
                    for (i in 0 until trackList.length()) {
                        val item = trackList.getJSONObject(i)
                        val title = item.optString("title").ifEmpty { item.optString("name") }
                        val artist = item.optString("subtitle").ifEmpty { item.optString("artists") }
                        if (title.isNotEmpty()) {
                            tracks.add(SpotifyTrack(title, artist))
                        }
                    }
                    if (tracks.isNotEmpty()) {
                        return SpotifyImportResult(playlistName, tracks)
                    }
                }

                val items = entity.optJSONObject("tracks")?.optJSONArray("items")
                if (items != null) {
                    for (i in 0 until items.length()) {
                        val trackObj = items.getJSONObject(i).optJSONObject("track") ?: items.getJSONObject(i)
                        val title = trackObj.optString("name")
                        val artistsArray = trackObj.optJSONArray("artists")
                        val artist = if (artistsArray != null && artistsArray.length() > 0) {
                            artistsArray.getJSONObject(0).optString("name")
                        } else ""

                        if (title.isNotEmpty()) {
                            tracks.add(SpotifyTrack(title, artist))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing initial JSON payload", e)
        }

        return SpotifyImportResult(playlistName, tracks)
    }
}