package com.sumit.muzixx.data.network

import android.content.Context
import com.sumit.muzixx.utils.NetworkUtils.isWifiConnected
import android.os.Environment
import android.widget.Toast
import com.sumit.muzixx.data.Song
import com.sumit.muzixx.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.io.File
import java.io.FileOutputStream

object AudioDownloader {
    private val client = OkHttpClient()

    suspend fun downloadTrack(context: Context, song: Song, settings: SettingsRepository) {
        withContext(Dispatchers.IO) {
            try {
                val downloadOverWifiOnly = settings.downloadWifiOnly

                if (downloadOverWifiOnly && !isWifiConnected(context)) {
                    showToast(context, "Download blocked: Wi-Fi connection required.")
                    return@withContext
                }

                val downloadUrl = if (song.id.startsWith("yt_") || song.type.lowercase().trim() == "yt") {
                    val videoId = song.id.removePrefix("yt_")
                    val videoUrl = "https://www.youtube.com/watch?v=$videoId"

                    val service = org.schabi.newpipe.extractor.ServiceList.YouTube
                    val streamInfo = StreamInfo.getInfo(service, videoUrl)

                    streamInfo.audioStreams.maxByOrNull { it.bitrate }?.url
                } else {
                    song.uri
                }

                if (downloadUrl.isNullOrBlank()) {
                    showToast(context, "Failed to resolve secure download link")
                    return@withContext
                }

                showToast(context, "Starting Download: ${song.title}")

                val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                val appFolder = File(musicDir, "MuzixX").apply { mkdirs() }

                val safeTitle = song.title.replace("[\\\\/:*?\"<>|]".toRegex(), "")
                val safeArtist = song.artist.replace("[\\\\/:*?\"<>|]".toRegex(), "")
                val destinationFile = File(appFolder, "$safeTitle - $safeArtist.mp3")

                val request = Request.Builder()
                    .url(downloadUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw Exception("Server HTTP error: ${response.code}")
                    val body = response.body ?: throw Exception("Null network response context")

                    FileOutputStream(destinationFile).use { outputStream ->
                        body.byteStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                }

                android.media.MediaScannerConnection.scanFile(
                    context,
                    arrayOf(destinationFile.absolutePath),
                    arrayOf("audio/mpeg"),
                    null
                )

                showToast(context, "Saved to Music/MuzixX!")
            } catch (e: Exception) {
                e.printStackTrace()
                showToast(context, "Download failed: ${e.localizedMessage}")
            }
        }
    }

    private suspend fun showToast(context: Context, message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}