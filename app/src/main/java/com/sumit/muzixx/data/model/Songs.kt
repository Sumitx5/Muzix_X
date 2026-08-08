package com.sumit.muzixx.data.model

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val uri: String,
    val artUri: String?,
    val duration: Long,
    val isStreaming: Boolean,
    val folderName: String = "Unknown",
    val type: String
)