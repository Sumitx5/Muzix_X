package com.sumit.muzixx.data.model

import com.sumit.muzixx.data.Song

fun SaavnTrackData.toSong(fallbackFolder: String): Song {
    val directStreamingUrl = this.downloadUrl?.find { it.quality == "320kbps" }?.url
        ?: this.downloadUrl?.find { it.quality == "160kbps" }?.url
        ?: this.downloadUrl?.lastOrNull()?.url
        ?: ""

    return Song(
        id = this.id ?: "",
        title = this.name ?: "Unknown Title",
        artist = this.artists?.primary?.firstOrNull()?.name ?: "Unknown Artist",
        uri = directStreamingUrl,
        artUri = this.image?.lastOrNull()?.url ?: "",
        duration = (this.duration ?: 0).toLong() * 1000L,
        isStreaming = true,
        folderName = fallbackFolder,
        type = "saavn"
    )
}