package com.sumit.muzixx.data.manager

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

@OptIn(UnstableApi::class)
object MuzixCacheManager {
    private var cacheInstance: SimpleCache? = null

    //Allocate 150MB maximum space for sliding track pre-cache buffer windows
    private const val CACHE_SIZE = 150 * 1024 * 1024L

    @Synchronized
    fun getCache(context: Context): SimpleCache {
        if (cacheInstance == null) {
            val cacheDir = File(context.cacheDir, "muzixx_audio_cache")
            val evictor = LeastRecentlyUsedCacheEvictor(CACHE_SIZE)
            val databaseProvider = StandaloneDatabaseProvider(context)
            cacheInstance = SimpleCache(cacheDir, evictor, databaseProvider)
        }
        return cacheInstance!!
    }

    fun createCacheDataSourceFactory(context: Context): CacheDataSource.Factory {
        val baseHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        val httpDataSourceFactory = OkHttpDataSource.Factory(baseHttpClient)
            .setUserAgent("MuzixX/1.0")

        val upstreamFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
        return CacheDataSource.Factory()
            .setCache(getCache(context))
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }
}