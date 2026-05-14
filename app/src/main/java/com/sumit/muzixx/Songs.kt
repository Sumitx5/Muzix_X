package com.sumit.muzixx

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val data: String,
    val uri: Uri,
    val duration: Int
)