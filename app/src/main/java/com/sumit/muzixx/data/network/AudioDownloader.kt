package com.sumit.muzixx.data.network

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import android.util.Log
import android.widget.Toast
import com.sumit.muzixx.data.model.Song
import com.sumit.muzixx.data.repository.SettingsRepository
import com.sumit.muzixx.utils.NetworkUtils.isWifiConnected
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

object AudioDownloader {
    private const val TAG = "AudioDownloader"

    @Volatile
    private var isNewPipeInitialized = false

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private fun ensureNewPipeInitialized() {
        if (!isNewPipeInitialized) {
            synchronized(this) {
                if (!isNewPipeInitialized) {
                    try {
                        NewPipe.init(DownloaderImpl.getInstance(client))
                        isNewPipeInitialized = true
                    } catch (e: Exception) {
                        Log.e(TAG, "NewPipe initialization error", e)
                    }
                }
            }
        }
    }

    suspend fun downloadTrack(context: Context, song: Song, settings: SettingsRepository) {
        val appContext = context.applicationContext

        withContext(Dispatchers.IO) {
            try {
                if (settings.downloadWifiOnly && !isWifiConnected(appContext)) {
                    showToast(appContext, "Download blocked: Wi-Fi connection required.")
                    return@withContext
                }

                showToast(appContext, "Resolving link: ${song.title}...")

                var downloadUrl: String? = null
                var fileExtension = "mp3"
                var mimeType = "audio/mpeg"

                val isYouTube = song.id.startsWith("yt_") || song.type.lowercase().trim() == "yt"

                if (isYouTube) {
                    ensureNewPipeInitialized()
                    val videoId = song.id.removePrefix("yt_")
                    val videoUrl = "https://www.youtube.com/watch?v=$videoId"

                    val streamInfo = StreamInfo.getInfo(ServiceList.YouTube, videoUrl)
                    val bestAudioStream: AudioStream? = streamInfo.audioStreams.maxByOrNull { it.bitrate }

                    if (bestAudioStream != null) {
                        downloadUrl = bestAudioStream.url
                        val formatName = (bestAudioStream.format?.name ?: "").lowercase()

                        when {
                            formatName.contains("m4a") || formatName.contains("mp4") || formatName.contains("aac") -> {
                                fileExtension = "m4a"
                                mimeType = "audio/mp4"
                            }
                            formatName.contains("webm") || formatName.contains("opus") || formatName.contains("ogg") -> {
                                fileExtension = "opus"
                                mimeType = "audio/ogg"
                            }
                            else -> {
                                fileExtension = "m4a"
                                mimeType = "audio/mp4"
                            }
                        }
                    }
                } else {
                    downloadUrl = song.uri
                }

                if (downloadUrl.isNullOrBlank()) {
                    showToast(appContext, "Failed to resolve stream link")
                    return@withContext
                }

                showToast(appContext, "Downloading: ${song.title}")

                // Save directly to the public Music directory (/storage/emulated/0/Music/MuzixX)
                val publicMusicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                val appFolder = File(publicMusicDir, "MuzixX").apply { mkdirs() }

                val safeTitle = song.title
                    .replace("[\\\\/:*?\"<>|]".toRegex(), "_")
                    .replace("[–—]".toRegex(), "-")
                    .trim()
                val safeArtist = song.artist
                    .replace("[\\\\/:*?\"<>|]".toRegex(), "_")
                    .replace("[–—]".toRegex(), "-")
                    .trim()

                val destinationFile = File(appFolder, "$safeTitle - $safeArtist.$fileExtension")

                val requestBuilder = Request.Builder()
                    .url(downloadUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")

                if (isYouTube) {
                    requestBuilder.header("Referer", "https://www.youtube.com/")
                    requestBuilder.header("Origin", "https://www.youtube.com")
                }

                val request = requestBuilder.build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw Exception("HTTP ${response.code}: ${response.message}")
                    }

                    val body = response.body ?: throw Exception("Empty response body")

                    FileOutputStream(destinationFile).use { outputStream ->
                        body.byteStream().use { inputStream ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                outputStream.write(buffer, 0, bytesRead)
                            }
                            outputStream.flush()
                        }
                    }
                }

                MediaScannerConnection.scanFile(
                    appContext,
                    arrayOf(destinationFile.absolutePath),
                    arrayOf(mimeType)
                ) { path, uri ->
                    if (uri != null) {
                        Log.d(TAG, "Successfully scanned $path -> $uri")
                    } else {
                        Log.w(TAG, "MediaScanner returned null Uri for $path")
                    }
                }

                showToast(appContext, "Saved to Music/MuzixX!")
            } catch (e: Exception) {
                Log.e(TAG, "Download error for ${song.title}", e)
                showToast(appContext, "Download failed: ${e.localizedMessage ?: e.javaClass.simpleName}")
            }
        }
    }

    private suspend fun showToast(context: Context, message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context.applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }
}