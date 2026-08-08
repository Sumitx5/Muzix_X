package com.sumit.muzixx.utils

import com.sumit.muzixx.data.model.Playlist
import com.sumit.muzixx.data.model.Song

class LikeManager(
    private val getPlaylists: () -> List<Playlist>,
    private val createPlaylist: (String) -> Unit,
    private val addSongToPlaylist: (String, Song) -> Unit,
    private val removeSongFromPlaylist: (String, Song) -> Unit
) {

    companion object {
        const val LIKED_PLAYLIST_ID = "liked_songs"
        const val LIKED_PLAYLIST_NAME = "Liked Songs"
    }

    fun getOrCreateLikedPlaylist(): Playlist? {
        val currentPlaylists = getPlaylists()
        return currentPlaylists.find {
            it.name.equals(LIKED_PLAYLIST_NAME, ignoreCase = true) || it.id == LIKED_PLAYLIST_ID
        } ?: run {
            createPlaylist(LIKED_PLAYLIST_NAME)
            getPlaylists().find {
                it.name.equals(LIKED_PLAYLIST_NAME, ignoreCase = true) || it.id == LIKED_PLAYLIST_ID
            }
        }
    }

    fun isSongLiked(songId: String?): Boolean {
        if (songId.isNullOrEmpty()) return false
        val likedPlaylist = getPlaylists().find {
            it.name.equals(LIKED_PLAYLIST_NAME, ignoreCase = true) || it.id == LIKED_PLAYLIST_ID
        }
        return likedPlaylist?.songs?.any { it.id == songId } == true
    }

    fun toggleLike(song: Song?): Boolean {
        if (song == null) return false
        val targetPlaylist = getOrCreateLikedPlaylist() ?: return false
        val currentlyLiked = isSongLiked(song.id)

        if (currentlyLiked) {
            removeSongFromPlaylist(targetPlaylist.id, song)
        } else {
            addSongToPlaylist(targetPlaylist.id, song)
        }

        return !currentlyLiked
    }
}