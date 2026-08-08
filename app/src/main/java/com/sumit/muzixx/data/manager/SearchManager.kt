package com.sumit.muzixx.data.manager

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sumit.muzixx.data.model.Song
import com.sumit.muzixx.data.model.toSong
import com.sumit.muzixx.data.network.JioSaavnApiService
import com.sumit.muzixx.data.network.YouTubeMusicScraper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchManager(
    private val scope: CoroutineScope,
    private val jioSaavnApiService: JioSaavnApiService,
    private val ytScraper: YouTubeMusicScraper,
    private val persistenceManagerProvider: () -> PlaybackPersistenceManager?
) {
    val searchResults = mutableStateListOf<Song>()
    val saavnSearchResults = mutableStateListOf<Song>()
    val searchHistory = mutableStateListOf<String>()

    var isSearchLoading by mutableStateOf(false)
        private set
    var isSaavnLoading by mutableStateOf(false)
        private set

    fun loadSearchHistory() {
        persistenceManagerProvider()?.let { pm ->
            searchHistory.clear()
            searchHistory.addAll(pm.loadSearchHistory())
        }
    }

    fun saveSearchQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        val currentList = searchHistory.toMutableList()
        currentList.remove(trimmed)
        currentList.add(0, trimmed)
        val cappedList = if (currentList.size > 10) currentList.take(10) else currentList

        searchHistory.clear()
        searchHistory.addAll(cappedList)
        persistenceManagerProvider()?.saveSearchHistory(cappedList)
    }

    fun deleteSearchQuery(query: String) {
        searchHistory.remove(query)
        persistenceManagerProvider()?.saveSearchHistory(searchHistory)
    }

    fun searchJioSaavn(query: String) {
        if (query.isBlank()) return
        saveSearchQuery(query)
        scope.launch {
            isSaavnLoading = true
            try {
                val response = withContext(Dispatchers.IO) { jioSaavnApiService.searchSongs(query) }
                if (response.success) {
                    val tracks = response.data?.songs?.map { it.toSong("JioSaavn Search Result") } ?: emptyList()
                    saavnSearchResults.clear()
                    saavnSearchResults.addAll(tracks)
                }
            } catch (_: Exception) {
                saavnSearchResults.clear()
            } finally {
                isSaavnLoading = false
            }
        }
    }

    fun searchOnlineSongs(query: String) {
        if (query.isBlank()) return
        saveSearchQuery(query)
        scope.launch {
            isSearchLoading = true
            try {
                val scrapedResults = ytScraper.searchSongs(query.trim())
                searchResults.clear()
                searchResults.addAll(scrapedResults)
            } catch (_: Exception) {
                searchResults.clear()
            } finally {
                isSearchLoading = false
            }
        }
    }
}