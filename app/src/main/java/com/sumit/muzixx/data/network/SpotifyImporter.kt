package com.sumit.muzixx.data.network

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

    suspend fun fetchPlaylistTracks(url: String): SpotifyImportResult = withContext(Dispatchers.IO) {
        val emptyResult = SpotifyImportResult(playlistName = "Imported Playlist", tracks = emptyList())
        val playlistId = extractPlaylistId(url) ?: return@withContext emptyResult
        val embedUrl = "https://open.spotify.com/embed/playlist/$playlistId"

        val request = Request.Builder()
            .url(embedUrl)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
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

                return@withContext parsePlaylistFromJson(jsonString)
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
            e.printStackTrace()
        }

        return SpotifyImportResult(playlistName, tracks)
    }
}