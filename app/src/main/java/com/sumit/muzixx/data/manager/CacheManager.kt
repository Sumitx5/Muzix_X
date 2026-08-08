package com.sumit.muzixx.data.manager

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CacheManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    var cacheSizeText by mutableStateOf("Calculating...")
        private set

    @OptIn(UnstableApi::class)
    fun clearAudioCache() {
        scope.launch(Dispatchers.IO) {
            try {
                val simpleCache = MuzixCacheManager.getCache(context)
                val keys = simpleCache.keys
                keys.forEach { key ->
                    simpleCache.removeResource(key)
                }

                val cacheDir = context.cacheDir
                if (cacheDir.exists() && cacheDir.isDirectory) {
                    cacheDir.listFiles()?.forEach { file ->
                        if (file.name != "muzixx_audio_cache") {
                            file.deleteRecursively()
                        }
                    }
                }

                withContext(Dispatchers.Main) { cacheSizeText = "0.00 MB" }
            } catch (e: Exception) {
                Log.e("CacheManager", "Failed to clear audio cache", e)
            }
        }
    }

    @SuppressLint("DefaultLocale")
    fun calculateCurrentCacheSize() {
        scope.launch(Dispatchers.IO) {
            try {
                val cacheDir = context.cacheDir
                var totalBytes = 0L
                if (cacheDir.exists() && cacheDir.isDirectory) {
                    cacheDir.walkTopDown().forEach { if (it.isFile) totalBytes += it.length() }
                }
                val megaBytes = totalBytes.toDouble() / (1024 * 1024)
                withContext(Dispatchers.Main) { cacheSizeText = String.format("%.2f MB", megaBytes) }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { cacheSizeText = "0.00 MB" }
            }
        }
    }
}