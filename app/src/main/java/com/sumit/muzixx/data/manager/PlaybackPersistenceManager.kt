package com.sumit.muzixx.data.manager

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.sumit.muzixx.data.model.Song
import org.json.JSONArray
import org.json.JSONObject

class PlaybackPersistenceManager(context: Context) {

    companion object {
        private const val TAG = "PersistenceManager"

        private const val PREFS_NAME = "muzix_prefs"

        // Last played song
        private const val KEY_LAST_SONG_ID = "last_song_id"
        private const val KEY_LAST_SONG_TITLE = "last_song_title"
        private const val KEY_LAST_SONG_ARTIST = "last_song_artist"
        private const val KEY_LAST_SONG_URI = "last_song_uri"
        private const val KEY_LAST_SONG_ART_URI = "last_song_art_uri"
        private const val KEY_LAST_SONG_DURATION = "last_song_duration"
        private const val KEY_LAST_SONG_IS_STREAMING = "last_song_is_streaming"
        private const val KEY_LAST_SONG_FOLDER = "last_song_folder"
        private const val KEY_LAST_SONG_TYPE = "last_song_type"

        // Playback recovery
        private const val KEY_PLAYBACK_POSITION = "last_song_playback_position"
        private const val KEY_PLAYBACK_POSITION_SONG_ID = "last_song_playback_position_song_id"
        private const val KEY_PLAYBACK_POSITION_SAVED_AT = "last_song_playback_position_saved_at"

        // Recently played
        private const val KEY_RECENTLY_PLAYED = "recently_heard_songs_json"

        // Search history
        private const val KEY_SEARCH_HISTORY = "search_history_set"

        // Cloud stats
        private const val KEY_CLOUD_STATS_SYNCED = "cloud_stats_synced_v1"

        // Playlists
        private const val KEY_CUSTOM_PLAYLISTS = "custom_playlists"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveLastPlayedSong(song: Song) {
        prefs.edit {
            putString(KEY_LAST_SONG_ID, song.id)
            putString(KEY_LAST_SONG_TITLE, song.title)
            putString(KEY_LAST_SONG_ARTIST, song.artist)
            putString(KEY_LAST_SONG_URI, song.uri)
            putString(KEY_LAST_SONG_ART_URI, song.artUri)
            putLong(KEY_LAST_SONG_DURATION, song.duration)
            putBoolean(KEY_LAST_SONG_IS_STREAMING, song.isStreaming)
            putString(KEY_LAST_SONG_FOLDER, song.folderName)
            putString(KEY_LAST_SONG_TYPE, song.type)
        }

        Log.d(TAG, "Saved last played song: ${song.id} - ${song.title}")
    }

    fun loadLastPlayedSong(): Song? {
        val id = prefs.getString(KEY_LAST_SONG_ID, null) ?: return null

        val title =
            prefs.getString(KEY_LAST_SONG_TITLE, "Unknown Track")
                ?: "Unknown Track"

        val artist =
            prefs.getString(KEY_LAST_SONG_ARTIST, "Unknown Artist")
                ?: "Unknown Artist"

        val uri =
            prefs.getString(KEY_LAST_SONG_URI, "")
                ?: ""

        val artUri =
            prefs.getString(KEY_LAST_SONG_ART_URI, null)

        val duration =
            prefs.getLong(KEY_LAST_SONG_DURATION, 0L)

        val isStreaming =
            prefs.getBoolean(KEY_LAST_SONG_IS_STREAMING, false)

        val folderName =
            prefs.getString(KEY_LAST_SONG_FOLDER, "Unknown")
                ?: "Unknown"

        val type =
            prefs.getString(KEY_LAST_SONG_TYPE, "local")
                ?: "local"

        return Song(
            id = id,
            title = title,
            artist = artist,
            uri = uri,
            artUri = artUri,
            duration = duration,
            isStreaming = isStreaming,
            folderName = folderName,
            type = type
        )
    }

    fun saveCurrentPlaybackPosition(
        songId: String,
        positionMs: Long
    ) {
        if (songId.isBlank()) {
            Log.w(TAG, "Ignoring playback position save because song ID is blank.")
            return
        }

        val safePosition = positionMs.coerceAtLeast(0L)
        val savedAt = System.currentTimeMillis()

        prefs.edit {
            putString(KEY_PLAYBACK_POSITION_SONG_ID, songId)
            putLong(KEY_PLAYBACK_POSITION, safePosition)
            putLong(KEY_PLAYBACK_POSITION_SAVED_AT, savedAt)
        }

        Log.d(
            TAG,
            "Saved playback position: songId=$songId, position=${safePosition}ms"
        )
    }

    fun getLastPlaybackPosition(songId: String? = null): Long {
        val savedSongId = prefs.getString(KEY_PLAYBACK_POSITION_SONG_ID, null)

        if (songId != null && savedSongId != songId) {
            Log.d(
                TAG,
                "Ignoring saved position because song mismatch. requested=$songId, saved=$savedSongId"
            )
            return 0L
        }

        return prefs
            .getLong(KEY_PLAYBACK_POSITION, 0L)
            .coerceAtLeast(0L)
    }

    fun saveRecentlyPlayed(songs: List<Song>) {
        val jsonArray = JSONArray()

        for (song in songs) {
            val jsonObject = JSONObject().apply {
                put("id", song.id)
                put("title", song.title)
                put("artist", song.artist)
                put("uri", song.uri)
                put("artUri", song.artUri ?: "")
                put("duration", song.duration)
                put("isStreaming", song.isStreaming)
                put("folderName", song.folderName)
                put("type", song.type)
            }

            jsonArray.put(jsonObject)
        }

        prefs.edit {
            putString(
                KEY_RECENTLY_PLAYED,
                jsonArray.toString()
            )
        }
    }

    fun loadRecentlyPlayed(): List<Song> {
        val rawJson =
            prefs.getString(KEY_RECENTLY_PLAYED, null)
                ?: return emptyList()

        val tempHistoryList = mutableListOf<Song>()

        try {
            val jsonArray = JSONArray(rawJson)

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)

                val artString =
                    jsonObject.optString("artUri", "")

                val resolvedArt =
                    artString.ifBlank { null }

                tempHistoryList.add(
                    Song(
                        id = jsonObject.getString("id"),
                        title = jsonObject.getString("title"),
                        artist = jsonObject.getString("artist"),
                        uri = jsonObject.getString("uri"),
                        artUri = resolvedArt,
                        duration = jsonObject.getLong("duration"),
                        isStreaming = jsonObject.getBoolean("isStreaming"),
                        folderName = jsonObject.getString("folderName"),
                        type = jsonObject.getString("type")
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Corrupt recently played JSON error",
                e
            )
        }

        return tempHistoryList
    }

    fun loadSearchHistory(): List<String> {
        return prefs
            .getStringSet(KEY_SEARCH_HISTORY, emptySet())
            ?.toList()
            ?.reversed()
            ?: emptyList()
    }

    fun saveSearchHistory(history: List<String>) {
        prefs.edit {
            putStringSet(
                KEY_SEARCH_HISTORY,
                history.toSet()
            )
        }
    }

    fun isCloudStatsSynced(): Boolean {
        return prefs.getBoolean(
            KEY_CLOUD_STATS_SYNCED,
            false
        )
    }

    fun markCloudStatsSynced() {
        prefs.edit {
            putBoolean(
                KEY_CLOUD_STATS_SYNCED,
                true
            )
        }
    }

    fun resetCloudSyncFlag() {
        prefs.edit {
            putBoolean(
                KEY_CLOUD_STATS_SYNCED,
                false
            )
        }
    }

    fun loadCustomPlaylistsJson(): String? {
        return prefs.getString(
            KEY_CUSTOM_PLAYLISTS,
            null
        )
    }

    fun saveCustomPlaylistsJson(json: String) {
        prefs.edit {
            putString(
                KEY_CUSTOM_PLAYLISTS,
                json
            )
        }
    }

    fun clearCustomPlaylistsStorage() {
        prefs.edit {
            remove(KEY_CUSTOM_PLAYLISTS)
        }
    }
}