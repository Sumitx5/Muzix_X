package com.sumit.muzixx.utils

import com.sumit.muzixx.data.Song

object PlayerQueueManager {

    fun determineSource(song: Song?): TrackSource {
        if (song == null) return TrackSource.UNKNOWN
        return when {
            song.id.startsWith("yt_") -> TrackSource.YOUTUBE
            song.id.all { it.isDigit() } -> TrackSource.SAAVN
            else -> TrackSource.LOCAL
        }
    }


    fun resolveActiveQueue(
        currentSong: Song?,
        trending: List<Song>,
        newReleases: List<Song>,
        searchResults: List<Song>,
        localSongs: List<Song>
    ): List<Song> {
        if (currentSong == null) return emptyList()

        return when {
            trending.any { it.id == currentSong.id } -> trending
            newReleases.any { it.id == currentSong.id } -> newReleases
            searchResults.any { it.id == currentSong.id } -> searchResults
            else -> localSongs
        }
    }
}

enum class TrackSource {
    LOCAL,
    SAAVN,
    YOUTUBE,
    UNKNOWN
}