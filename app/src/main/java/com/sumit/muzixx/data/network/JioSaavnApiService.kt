package com.sumit.muzixx.data.network

import com.sumit.muzixx.data.model.SaavnDirectSongResponse
import com.sumit.muzixx.data.model.SaavnPlaylistResponse
import com.sumit.muzixx.data.model.SaavnPlaylistSearchResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface JioSaavnApiService {

    //Dedicated Song Search
    @GET("api/search/songs")
    suspend fun searchSongs(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 40
    ): SaavnPlaylistResponse

    //Auto Suggestions
    @GET("api/songs/{id}/suggestions")
    suspend fun getSongSuggestions(
        @Path("id") id: String,
        @Query("limit") limit: Int = 10
    ): SaavnPlaylistResponse

    //Playlist Search
    @GET("/api/search/playlists")
    suspend fun searchPlaylists(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10
    ): SaavnPlaylistSearchResponse

    //Playlist Details
    @GET("api/playlists")
    suspend fun getPlaylistDetails(
        @Query("id") id: String,
        @Query("limit") limit: Int = 40
    ): SaavnPlaylistResponse

    //Individual Song
    @GET("api/songs/{id}")
    suspend fun getSongDetailsById(
        @Path("id") id: String
    ): SaavnDirectSongResponse

    companion object {
        private const val BASE_URL = "https://saavn.sumit.co/"

        fun create(): JioSaavnApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(JioSaavnApiService::class.java)
        }
    }
}