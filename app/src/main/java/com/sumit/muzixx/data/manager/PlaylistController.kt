package com.sumit.muzixx.data.manager

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sumit.muzixx.data.Playlist
import com.sumit.muzixx.data.Song
import java.util.UUID

class PlaylistController(private val onSavePlaylists: () -> Unit) {

    val playlists = mutableStateListOf<Playlist>()
    var selectedPlaylist by mutableStateOf<Playlist?>(null)
    private val gson = Gson()

    fun loadPlaylistsFromJson(json: String?) {
        if (json.isNullOrEmpty()) return
        try {
            val type = object : TypeToken<List<Playlist>>() {}.type
            val savedPlaylists: List<Playlist> = gson.fromJson(json, type)

            playlists.removeAll { it.id != "local_songs" && !it.id.startsWith("folder_") }

            savedPlaylists.forEach { playlist ->
                val cleanedSongs = playlist.songs.map { song ->
                    if (song.id.startsWith("yt_") || song.uri.contains("saavn") || song.isStreaming) {
                        song.copy(uri = "")
                    } else {
                        song
                    }
                }
                playlists.add(Playlist(id = playlist.id, name = playlist.name, songs = cleanedSongs))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    fun getCustomPlaylistsJson(): String {
        val customLists = playlists.filter { it.id != "local_songs" && !it.id.startsWith("folder_") }

        val sanitizedLists = customLists.map { playlist ->
            val sanitizedSongs = playlist.songs.map { song ->
                if (song.id.startsWith("yt_") || song.uri.contains("saavn") || song.isStreaming) {
                    song.copy(uri = "")
                } else {
                    song
                }
            }
            playlist.copy(songs = sanitizedSongs)
        }

        return gson.toJson(sanitizedLists)
    }

    fun createCustomPlaylist(name: String) {
        if (name.isNotBlank()) {
            val newPlaylist = Playlist(id = UUID.randomUUID().toString(), name = name, songs = emptyList())
            playlists.add(newPlaylist)
            onSavePlaylists()
        }
    }

    fun addSongToPlaylist(playlistId: String, song: Song) {
        val index = playlists.indexOfFirst { it.id == playlistId }
        if (index != -1) {
            val targetPlaylist = playlists[index]
            if (!targetPlaylist.songs.any { it.id == song.id }) {
                val songToSave = if (song.id.startsWith("yt_") || song.uri.contains("saavn") || song.isStreaming) {
                    song.copy(uri = "")
                } else {
                    song
                }

                val updatedSongs = targetPlaylist.songs + songToSave
                playlists[index] = targetPlaylist.copy(songs = updatedSongs)
                onSavePlaylists()

                if (selectedPlaylist?.id == playlistId) {
                    selectedPlaylist = playlists[index]
                }
            }
        }
    }

    fun removeSongFromPlaylist(playlistId: String, song: Song) {
        val index = playlists.indexOfFirst { it.id == playlistId }
        if (index != -1) {
            val targetPlaylist = playlists[index]
            val updatedSongs = targetPlaylist.songs.filter { it.id != song.id }
            playlists[index] = targetPlaylist.copy(songs = updatedSongs)
            onSavePlaylists()

            if (selectedPlaylist?.id == playlistId) {
                selectedPlaylist = playlists[index]
            }
        }
    }

    fun renamePlaylist(playlistId: String, newName: String) {
        if (playlistId == "local_songs" || playlistId.startsWith("folder_") || newName.isBlank()) return

        val index = playlists.indexOfFirst { it.id == playlistId }
        if (index != -1) {
            playlists[index] = playlists[index].copy(name = newName)
            onSavePlaylists()

            if (selectedPlaylist?.id == playlistId) {
                selectedPlaylist = playlists[index]
            }
        }
    }

    fun deletePlaylist(playlistId: String) {
        if (playlistId == "local_songs" || playlistId.startsWith("folder_")) return

        playlists.removeAll { it.id == playlistId }
        onSavePlaylists()

        if (selectedPlaylist?.id == playlistId) {
            selectedPlaylist = null
        }
    }

    fun initializeLocalSongsPlaylist(localSongs: List<Song>) {
        val localIndex = playlists.indexOfFirst { it.id == "local_songs" }
        if (localIndex != -1) {
            playlists[localIndex] = playlists[localIndex].copy(songs = localSongs)
        } else {
            playlists.add(0, Playlist(id = "local_songs", name = "Local Songs", songs = localSongs))
        }

        playlists.removeAll { it.id.startsWith("folder_") }

        val groupedFolders = localSongs.groupBy { it.folderName }
        groupedFolders.forEach { (folderName, folderSongs) ->
            val generatedFolderId = "folder_$folderName"
            playlists.add(Playlist(id = generatedFolderId, name = folderName, songs = folderSongs))
        }

        selectedPlaylist?.let { current ->
            val freshIndex = playlists.indexOfFirst { it.id == current.id }
            if (freshIndex != -1) {
                selectedPlaylist = playlists[freshIndex]
            }
        }
    }
}