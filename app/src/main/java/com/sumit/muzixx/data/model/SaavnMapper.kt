package com.sumit.muzixx.data.model

import com.sumit.muzixx.data.Song

fun SaavnTrackData.toSong(fallbackFolder: String): Song {
    // Look for 320kbps first, drop down gracefully if unavailable
    val directStreamingUrl = this.downloadUrl?.find { it.quality == "320kbps" }?.url
        ?: this.downloadUrl?.find { it.quality == "160kbps" }?.url
        ?: this.downloadUrl?.lastOrNull()?.url
        ?: "" // Fallback to empty string if absolutely nothing is found

    return Song(
        id = this.id ?: "",
        title = this.name ?: "Unknown Title",
        artist = this.artists?.primary?.firstOrNull()?.name ?: "Unknown Artist",
        uri = directStreamingUrl, // Direct CDN link goes straight into uri
        artUri = this.image?.lastOrNull()?.url ?: "",
        duration = (this.duration ?: 0).toLong() * 1000L, // Convert seconds to millis
        isStreaming = true,
        folderName = fallbackFolder
    )
}