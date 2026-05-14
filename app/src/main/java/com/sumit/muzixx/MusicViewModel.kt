package com.sumit.muzixx

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class MusicViewModel : ViewModel() {
    val songs = mutableStateListOf<Song>()
    var selectedSong by mutableStateOf<Song?>(null)
    fun loadSongs(songList: List<Song>) {
        songs.clear()
        songs.addAll(songList)
    }
}