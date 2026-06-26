package com.sumit.muzixx.data.repository

import android.content.Context
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import com.sumit.muzixx.data.ProfileStatsManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlin.time.Duration.Companion.milliseconds

class PlaybackStatsRepository(
    context: Context,
    private val repositoryScope: CoroutineScope
) {
    private val statsManager = ProfileStatsManager(context.applicationContext)

    val totalSongsHeardState = mutableIntStateOf(0)
    val totalPlaySecondsState = mutableLongStateOf(0L)
    val monthlySongsHeardState = mutableIntStateOf(0)
    val monthlyPlaySecondsState = mutableLongStateOf(0L)
    val yearlySongsHeardState = mutableIntStateOf(0)
    val yearlyPlaySecondsState = mutableLongStateOf(0L)

    private var playbackTrackerJob: Job? = null

    init {
        repositoryScope.launch(Dispatchers.IO) {
            launch { statsManager.totalSongsHeardFlow.collectLatest { totalSongsHeardState.intValue = it } }
            launch { statsManager.totalPlaySecondsFlow.collectLatest { totalPlaySecondsState.longValue = it } }
            launch { statsManager.monthlySongsHeardFlow.collectLatest { monthlySongsHeardState.intValue = it } }
            launch { statsManager.monthlyPlaySecondsFlow.collectLatest { monthlyPlaySecondsState.longValue = it } }
            launch { statsManager.yearlySongsHeardFlow.collectLatest { yearlySongsHeardState.intValue = it } }
            launch { statsManager.yearlyPlaySecondsFlow.collectLatest { yearlyPlaySecondsState.longValue = it } }
        }
    }

    //REAL-TIME ENGINE INTERACTIONS
    fun startPlaybackTimer(isPlayingProvider: () -> Boolean) {
        playbackTrackerJob?.cancel()
        playbackTrackerJob = repositoryScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(1000L.milliseconds)
                if (isPlayingProvider()) {
                    statsManager.addPlayDuration(1L)
                }
            }
        }
    }
    fun overwriteLocalStatsWithCloud(
        totalHeard: Int,
        monthlyHeard: Int,
        yearlyHeard: Int,
        totalSec: Long,
        monthlySec: Long,
        yearlySec: Long
    ) {
        repositoryScope.launch(Dispatchers.IO) {
            statsManager.updateAbsoluteStats(
                totalHeard = totalHeard,
                monthlyHeard = monthlyHeard,
                yearlyHeard = yearlyHeard,
                totalSec = totalSec,
                monthlySec = monthlySec,
                yearlySec = yearlySec
            )
        }
    }

    fun stopPlaybackTimer() {
        playbackTrackerJob?.cancel()
    }

    fun incrementSongsHeardCount() {
        repositoryScope.launch(Dispatchers.IO) {
            statsManager.incrementSongsHeard()
        }
    }
}