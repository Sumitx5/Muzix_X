package com.sumit.muzixx.data.manager

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.compose.runtime.*
import androidx.core.net.toUri
import androidx.media3.common.*
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.sumit.muzixx.data.Song
import com.sumit.muzixx.services.PlaybackService
import com.sumit.muzixx.data.network.JioSaavnApiService
import kotlinx.coroutines.*

class PlayerController(
    private val scope: CoroutineScope,
    private val onPlaybackStarted: (Boolean, MediaController?) -> Unit,
    private val onPlaybackStopped: () -> Unit,
    private val onTrackSwitched: () -> Unit,
    private val onQueueUpdated: (List<Song>) -> Unit,
    private val resolveYouTubeStream: suspend (Song) -> String?,
    private val jioSaavnApiService: JioSaavnApiService,
    private val getAudioQualityPreference: () -> String
) {

    companion object {
        private const val TAG = "PlayerController"
    }

    var mediaController: MediaController? = null
    var isPlaying by mutableStateOf(false)
    var selectedSong by mutableStateOf<Song?>(null)
    var currentPosition by mutableLongStateOf(0L)
    var totalDuration by mutableLongStateOf(0L)

    var activePlaybackQueue = mutableStateListOf<Song>()
        private set

    var activePlaylistIndex by mutableIntStateOf(0)
        private set

    fun initMediaController(context: Context) {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, token).buildAsync()
        controllerFuture.addListener({
            val controller = controllerFuture.get()
            mediaController = controller
            controller.addListener(playerListener)
            Log.d(TAG, "MediaController successfully attached to PlaybackService.")
        }, MoreExecutors.directExecutor())
    }

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)
            val controller = mediaController ?: return

            val exoIndex = controller.currentMediaItemIndex

            if (exoIndex in activePlaybackQueue.indices) {
                activePlaylistIndex = exoIndex

                val currentMediaId = mediaItem?.mediaId
                val matchedSong = activePlaybackQueue.find { it.id == currentMediaId }
                    ?: activePlaybackQueue[exoIndex]

                selectedSong = matchedSong
                onTrackSwitched()

                if (matchedSong.uri.isBlank()) {
                    resolveAndPlayTrackOnDemand(matchedSong, exoIndex)
                }
            }

            totalDuration = controller.duration.coerceAtLeast(0L)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            if (playbackState == Player.STATE_READY) {
                mediaController?.let { totalDuration = it.duration.coerceAtLeast(0L) }
            }
            if (playbackState == Player.STATE_ENDED) {
                onPlaybackStopped()
            }
        }

        override fun onIsPlayingChanged(isPlayingNow: Boolean) {
            isPlaying = isPlayingNow
            if (isPlayingNow) {
                mediaController?.let { totalDuration = it.duration.coerceAtLeast(0L) }
                onPlaybackStarted(isPlayingNow, mediaController)
            } else {
                onPlaybackStopped()
            }
        }
    }

    private suspend fun resolveSongUri(song: Song): String {
        if (song.uri.isNotBlank() && (song.uri.startsWith("http") || song.uri.startsWith("content://"))) {
            return song.uri
        }

        if (song.id.startsWith("yt_") || !song.id.all { it.isDigit() }) {
            return resolveYouTubeStream(song) ?: ""
        }

        return try {
            val response = jioSaavnApiService.getSongDetailsById(song.id)
            response.data?.songs?.firstOrNull()?.downloadUrl ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "Failed retrieving JioSaavn media endpoint", e)
            ""
        } as String
    }

    private fun resolveAndPlayTrackOnDemand(song: Song, engineIndex: Int) {
        val controller = mediaController ?: return
        scope.launch {
            val resolvedUri = resolveSongUri(song)
            if (!resolvedUri.isNullOrBlank()) {
                withContext(Dispatchers.Main) {
                    if (engineIndex in activePlaybackQueue.indices && activePlaybackQueue[engineIndex].id == song.id) {
                        activePlaybackQueue[engineIndex] = song.copy(uri = resolvedUri)

                        val updatedMediaItem = buildMediaItem(activePlaybackQueue[engineIndex], resolvedUri)
                        controller.replaceMediaItem(engineIndex, updatedMediaItem)
                    }
                }
            }
        }
    }

    private fun buildMediaItem(song: Song, streamUrl: String): MediaItem {
        // CRITICAL PROTECTION: If streamUrl is empty or matches raw ID, fallback to an explicit
        // placeholder domain so ExoPlayer fails with a network exception instead of trying to read local file paths.
        val safeUri = if (streamUrl.isBlank() || streamUrl == song.id) {
            "https://localhost/placeholder.mp3"
        } else {
            streamUrl
        }

        return MediaItem.Builder()
            .setMediaId(song.id)
            .setUri(safeUri.toUri())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setSubtitle(song.folderName)
                    .setArtworkUri(song.artUri?.toUri())
                    .build()
            )
            .build()
    }

    fun submitQueueToPlayer(songList: List<Song>, startIndex: Int) {
        if (songList.isEmpty() || startIndex !in songList.indices) return

        val controller = mediaController ?: run {
            Log.e(TAG, "MediaController framework not initialized yet.")
            return
        }

        // 1. Immediately update local tracking arrays
        activePlaybackQueue.clear()
        activePlaybackQueue.addAll(songList)
        activePlaylistIndex = startIndex
        onQueueUpdated(activePlaybackQueue.toList())

        val targetedSong = activePlaybackQueue[activePlaylistIndex]

        // 2. Launch in your ViewModel's scope to resolve the stream BEFORE giving it to ExoPlayer
        scope.launch {
            Log.d(TAG, "Resolving URL path for initial track: ${targetedSong.title}")

            // This runs your ViewModel lambda on a background worker thread
            val resolvedUri = withContext(Dispatchers.IO) {
                resolveSongUri(targetedSong)
            }

            // 3. Jump back to Main Thread to update player configuration
            withContext(Dispatchers.Main) {
                if (resolvedUri.isBlank()) {
                    Log.e(TAG, "Failed to extract valid web stream URL for track: ${targetedSong.title}")
                    return@withContext
                }

                // Inject the working streaming address directly into your queue item array
                if (activePlaylistIndex in activePlaybackQueue.indices) {
                    activePlaybackQueue[activePlaylistIndex] = targetedSong.copy(uri = resolvedUri)
                }

                // 4. Build the ExoPlayer array list using our placeholder safety mapping strategy
                val mediaItems = activePlaybackQueue.mapIndexed { index, song ->
                    val streamUrl = if (index == activePlaylistIndex) resolvedUri else song.uri
                    buildMediaItem(song, streamUrl)
                }

                // 5. Initialize engine playback safely with a verified web address
                controller.stop()
                controller.setMediaItems(mediaItems, activePlaylistIndex, 0L)
                controller.prepare()
                controller.play()
                Log.d(TAG, "ExoPlayer playback initialized successfully.")

                // 6. Pre-fetch upcoming song link in background to keep transitions lightning fast
                val nextIndex = activePlaylistIndex + 1
                if (nextIndex in activePlaybackQueue.indices) {
                    val nextSong = activePlaybackQueue[nextIndex]
                    if (nextSong.uri.isBlank()) {
                        val nextUri = withContext(Dispatchers.IO) { resolveSongUri(nextSong) }
                        if (nextUri.isNotBlank()) {
                            activePlaybackQueue[nextIndex] = nextSong.copy(uri = nextUri)
                            controller.replaceMediaItem(nextIndex, buildMediaItem(activePlaybackQueue[nextIndex], nextUri))
                        }
                    }
                }
            }
        }
    }

    fun injectTracksToQueue(suggestedSongs: List<Song>) {
        val controller = mediaController ?: return
        if (suggestedSongs.isEmpty()) return

        val oldSize = activePlaybackQueue.size
        activePlaybackQueue.addAll(suggestedSongs)
        onQueueUpdated(activePlaybackQueue.toList())

        val newMediaItems = suggestedSongs.map { buildMediaItem(it, it.uri) }
        controller.addMediaItems(oldSize, newMediaItems)
    }

    fun seekTo(position: Long) {
        mediaController?.seekTo(position)
        currentPosition = position
    }

    fun togglePlayPause() {
        mediaController?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun playNext() {
        mediaController?.let {
            if (it.hasNextMediaItem()) {
                it.seekToNextMediaItem()
            }
        }
    }

    fun playPrevious() {
        mediaController?.let {
            if (it.hasPreviousMediaItem()) {
                it.seekToPreviousMediaItem()
            }
        }
    }

    fun release() {
        mediaController?.removeListener(playerListener)
        mediaController = null
    }
}