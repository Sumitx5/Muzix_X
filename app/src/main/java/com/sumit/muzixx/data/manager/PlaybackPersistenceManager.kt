package com.sumit.muzixx.data.manager

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.sumit.muzixx.data.Song
import org.json.JSONArray
import org.json.JSONObject

class PlaybackPersistenceManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("muzix_prefs", Context.MODE_PRIVATE)

    fun saveLastPlayedSong(song: Song) {
        prefs.edit {
            putString("last_song_id", song.id)
            putString("last_song_title", song.title)
            putString("last_song_artist", song.artist)
            putString("last_song_uri", song.uri)
            putString("last_song_art_uri", song.artUri)
            putLong("last_song_duration", song.duration)
            putBoolean("last_song_is_streaming", song.isStreaming)
            putString("last_song_folder", song.folderName)
            putString("last_song_type", song.type)
        }
    }

    fun loadLastPlayedSong(): Song? {
        val id = prefs.getString("last_song_id", null) ?: return null
        val title = prefs.getString("last_song_title", "Unknown Track") ?: "Unknown Track"
        val artist = prefs.getString("last_song_artist", "Unknown Artist") ?: "Unknown Artist"
        val uri = prefs.getString("last_song_uri", "") ?: ""
        val artUri = prefs.getString("last_song_art_uri", null)
        val duration = prefs.getLong("last_song_duration", 0L)
        val isStreaming = prefs.getBoolean("last_song_is_streaming", false)
        val folderName = prefs.getString("last_song_folder", "Unknown") ?: "Unknown"
        val type = prefs.getString("last_song_type", "local") ?: "local"

        return Song(
            id = id, title = title, artist = artist, uri = uri, artUri = artUri,
            duration = duration, isStreaming = isStreaming, folderName = folderName, type = type
        )
    }

    fun saveCurrentPlaybackPosition(positionMs: Long) {
        prefs.edit { putLong("last_song_playback_position", positionMs) }
        Log.d("PersistenceManager", "Saved recovery playback state marker at: $positionMs ms")
    }

    fun getLastPlaybackPosition(): Long {
        return prefs.getLong("last_song_playback_position", 0L)
    }

    fun resetLastPlaybackPosition() {
        prefs.edit { putLong("last_song_playback_position", 0L) }
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
        prefs.edit { putString("recently_heard_songs_json", jsonArray.toString()) }
    }

    fun loadRecentlyPlayed(): List<Song> {
        val rawJson = prefs.getString("recently_heard_songs_json", null) ?: return emptyList()
        val tempHistoryList = mutableListOf<Song>()
        try {
            val jsonArray = JSONArray(rawJson)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val artString = jsonObject.optString("artUri", "")
                val resolvedArt = artString.ifBlank { null }

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
            Log.e("PersistenceManager", "Corrupt recently played JSON error", e)
        }
        return tempHistoryList
    }

    fun loadSearchHistory(): List<String> {
        return prefs.getStringSet("search_history_set", emptySet())?.toList()?.reversed() ?: emptyList()
    }

    fun saveSearchHistory(history: List<String>) {
        prefs.edit { putStringSet("search_history_set", history.toSet()) }
    }

    fun isCloudStatsSynced(): Boolean = prefs.getBoolean("cloud_stats_synced_v1", false)

    fun markCloudStatsSynced() {
        prefs.edit { putBoolean("cloud_stats_synced_v1", true) }
    }

    fun resetCloudSyncFlag() {
        prefs.edit { putBoolean("cloud_stats_synced_v1", false) }
    }

    fun loadCustomPlaylistsJson(): String? = prefs.getString("custom_playlists", null)

    fun saveCustomPlaylistsJson(json: String) {
        prefs.edit { putString("custom_playlists", json) }
    }

    fun clearCustomPlaylistsStorage() {
        prefs.edit { remove("custom_playlists") }
    }
}