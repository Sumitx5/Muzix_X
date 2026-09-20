package com.sumit.muzixx.data.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.math.abs

data class LyricLine(
    val timeMs: Long,
    val text: String
)

data class SongLyrics(
    val plainLyrics: String?,
    val syncedLyrics: List<LyricLine>,
    val isInstrumental: Boolean = false
)

object LyricsService {
    private const val TAG = "LyricsService"
    private const val BASE_URL = "https://lrclib.net/api"

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    suspend fun fetchLyrics(
        trackName: String,
        artistName: String,
        durationSeconds: Long? = null
    ): SongLyrics? = withContext(Dispatchers.IO) {
        val cleanTitle = sanitizeTitle(trackName)
        val cleanArtist = sanitizeArtist(artistName)

        if (cleanTitle.isBlank()) return@withContext null

        val exactResult = tryFetchExactLyrics(cleanTitle, cleanArtist, durationSeconds)
        if (exactResult != null) return@withContext exactResult

        Log.d(TAG, "Exact match missed for '$cleanTitle'. Attempting fuzzy search fallback...")
        return@withContext trySearchFallback(cleanTitle, cleanArtist, durationSeconds)
    }

    private fun tryFetchExactLyrics(
        title: String,
        artist: String,
        durationSeconds: Long?
    ): SongLyrics? {
        try {
            val encodedTitle = URLEncoder.encode(title, "UTF-8")
            val encodedArtist = URLEncoder.encode(artist, "UTF-8")

            var url = "$BASE_URL/get?track_name=$encodedTitle&artist_name=$encodedArtist"
            if (durationSeconds != null && durationSeconds > 0) {
                url += "&duration=$durationSeconds"
            }

            val request = buildRequest(url)
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                return parseLyricsJson(JSONObject(body))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Direct lookup failed: ${e.message}")
            return null
        }
    }

    private fun trySearchFallback(
        title: String,
        artist: String,
        durationSeconds: Long?
    ): SongLyrics? {
        try {
            val query = "$title $artist".trim()
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "$BASE_URL/search?q=$encodedQuery"

            val request = buildRequest(url)
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val jsonArray = JSONArray(body)

                if (jsonArray.length() == 0) return null

                var bestMatchJson: JSONObject? = null
                var minDurationDelta = Long.MAX_VALUE

                for (i in 0 until jsonArray.length()) {
                    val candidate = jsonArray.getJSONObject(i)
                    val hasSynced = candidate.optString("syncedLyrics", "").isNotBlank()
                    val hasPlain = candidate.optString("plainLyrics", "").isNotBlank()
                    val isInstrumental = candidate.optBoolean("instrumental", false)

                    if (!hasSynced && !hasPlain && !isInstrumental) continue

                    val candidateDuration = candidate.optDouble("duration", 0.0).toLong()

                    if (durationSeconds != null && durationSeconds > 0 && candidateDuration > 0) {
                        val delta = abs(candidateDuration - durationSeconds)
                        if (delta < minDurationDelta) {
                            minDurationDelta = delta
                            bestMatchJson = candidate
                            if (delta <= 3) break
                        }
                    } else {
                        bestMatchJson = candidate
                        break
                    }
                }

                return bestMatchJson?.let { parseLyricsJson(it) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Search fallback error: ${e.message}")
            return null
        }
    }

    private fun parseLyricsJson(json: JSONObject): SongLyrics {
        val isInstrumental = json.optBoolean("instrumental", false)
        val plain = json.optString("plainLyrics", "").ifBlank { null }
        val syncedRaw = json.optString("syncedLyrics", "").ifBlank { null }

        val parsedSynced = if (!syncedRaw.isNullOrBlank()) {
            parseLrc(syncedRaw)
        } else {
            emptyList()
        }

        return SongLyrics(
            plainLyrics = plain,
            syncedLyrics = parsedSynced,
            isInstrumental = isInstrumental
        )
    }

    private fun parseLrc(lrcText: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        val timeTagRegex = Regex("\\[(\\d{1,2}):(\\d{2})(?:[.:](\\d{1,3}))?]")

        lrcText.lines().forEach { rawLine ->
            val trimmed = rawLine.trim()
            if (trimmed.isBlank()) return@forEach

            val matches = timeTagRegex.findAll(trimmed).toList()
            if (matches.isNotEmpty()) {
                val lyricText = trimmed.replace(timeTagRegex, "").trim()

                for (match in matches) {
                    val min = match.groupValues[1].toLongOrNull() ?: 0L
                    val sec = match.groupValues[2].toLongOrNull() ?: 0L
                    val fracStr = match.groupValues[3]

                    val ms = when (fracStr.length) {
                        1 -> fracStr.toLong() * 100
                        2 -> fracStr.toLong() * 10
                        3 -> fracStr.toLong()
                        else -> 0L
                    }

                    val totalMs = (min * 60 * 1000L) + (sec * 1000L) + ms
                    lines.add(LyricLine(totalMs, lyricText))
                }
            }
        }
        return lines.sortedBy { it.timeMs }
    }

    private fun sanitizeTitle(title: String): String {
        return title
            .replace(Regex("(?i)\\[(official\\s*video|official\\s*audio|4k|hd|video|audio|lyrics?|remastered)]"), "")
            .replace(Regex("(?i)\\((official\\s*video|official\\s*audio|4k|hd|video|audio|lyrics?|full\\s*song|remastered)\\)"), "")
            .replace(Regex("(?i)(feat\\.|ft\\.).*"), "")
            .replace(Regex("(?i)\\|.*"), "")
            .trim()
    }

    private fun sanitizeArtist(artist: String): String {
        return artist
            .replace(Regex("(?i)\\s*-\\s*topic"), "")
            .replace("VEVO", "")
            .trim()
    }

    private fun buildRequest(url: String): Request {
        return Request.Builder()
            .url(url)
            .header("User-Agent", "MuzixX-AndroidApp/1.0 (github.com/Sumit282698/Muzix_X)")
            .build()
    }
}