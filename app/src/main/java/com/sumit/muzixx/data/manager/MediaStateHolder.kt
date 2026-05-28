package com.sumit.muzixx.data.manager

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.media3.session.MediaController
import kotlinx.coroutines.*

class MediaStateHolder(private val scope: CoroutineScope) {

    // ─── LIGHTWEIGHT TIMELINE STATES SPECIFICALLY FOR SLIDER UI ───
    var currentPosition by mutableLongStateOf(0L)
    var totalDuration by mutableLongStateOf(0L)

    private var positionSyncJob: Job? = null

    fun startTracking(mediaControllerProvider: () -> MediaController?) {
        positionSyncJob?.cancel()
        positionSyncJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                val controller = mediaControllerProvider()
                if (controller != null && controller.isPlaying) {
                    currentPosition = controller.currentPosition
                    val duration = controller.duration.coerceAtLeast(0L)
                    if (totalDuration != duration) {
                        totalDuration = duration
                    }
                }
                delay(500L) // Precision update window tick
            }
        }
    }

    fun stopTracking() {
        positionSyncJob?.cancel()
    }

    fun updateManualSeekPosition(position: Long) {
        currentPosition = position
    }

    fun release() {
        positionSyncJob?.cancel()
    }
}