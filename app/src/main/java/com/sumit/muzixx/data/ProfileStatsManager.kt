package com.sumit.muzixx.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "profile_stats")

class ProfileStatsManager(private val context: Context) {

    private fun getCurrentMonthKey(): String = LocalDate.now().let { "${it.year}_${it.month}" }
    private fun getCurrentYearKey(): String = LocalDate.now().year.toString()

    //READ STATS DATA FLOWS

    val totalSongsHeardFlow: Flow<Int> = context.dataStore.data.map { pref ->
        pref[intPreferencesKey("total_songs_heard")] ?: 0
    }
    val totalPlaySecondsFlow: Flow<Long> = context.dataStore.data.map { pref ->
        pref[longPreferencesKey("total_play_seconds")] ?: 0L
    }


    val monthlySongsHeardFlow: Flow<Int> = context.dataStore.data.map { pref ->
        pref[intPreferencesKey("songs_heard_${getCurrentMonthKey()}")] ?: 0
    }
    val monthlyPlaySecondsFlow: Flow<Long> = context.dataStore.data.map { pref ->
        pref[longPreferencesKey("play_seconds_${getCurrentMonthKey()}")] ?: 0L
    }

    val yearlySongsHeardFlow: Flow<Int> = context.dataStore.data.map { pref ->
        pref[intPreferencesKey("songs_heard_${getCurrentYearKey()}")] ?: 0
    }
    val yearlyPlaySecondsFlow: Flow<Long> = context.dataStore.data.map { pref ->
        pref[longPreferencesKey("play_seconds_${getCurrentYearKey()}")] ?: 0L
    }


    suspend fun incrementSongsHeard() {
        val monthKey = intPreferencesKey("songs_heard_${getCurrentMonthKey()}")
        val yearKey = intPreferencesKey("songs_heard_${getCurrentYearKey()}")
        val totalKey = intPreferencesKey("total_songs_heard")

        context.dataStore.edit { pref ->
            pref[totalKey] = (pref[totalKey] ?: 0) + 1
            pref[monthKey] = (pref[monthKey] ?: 0) + 1
            pref[yearKey] = (pref[yearKey] ?: 0) + 1
        }
    }

    suspend fun addPlayDuration(seconds: Long) {
        val monthKey = longPreferencesKey("play_seconds_${getCurrentMonthKey()}")
        val yearKey = longPreferencesKey("play_seconds_${getCurrentYearKey()}")
        val totalKey = longPreferencesKey("total_play_seconds")

        context.dataStore.edit { pref ->
            pref[totalKey] = (pref[totalKey] ?: 0L) + seconds
            pref[monthKey] = (pref[monthKey] ?: 0L) + seconds
            pref[yearKey] = (pref[yearKey] ?: 0L) + seconds
        }
    }
}