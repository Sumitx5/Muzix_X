package com.sumit.muzixx.data.network

import com.sumit.muzixx.data.model.SaavnPlaylistResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface JioSaavnApiService {

    //Global Search
    @GET("api/search")
    suspend fun globalSearch(
        @Query("query") query: String
    ): SaavnPlaylistResponse

    //Dedicated Song Search (Paged)
    @GET("api/search/songs")
    suspend fun searchSongs(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): SaavnPlaylistResponse

    //Auto Suggestions
    @GET("api/songs/{id}/suggestions")
    suspend fun getSongSuggestions(
        @Path("id") id: String,
        @Query("limit") limit: Int = 10
    ): SaavnPlaylistResponse

    //Playlist Details
    @GET("api/playlists")
    suspend fun getPlaylistDetails(
        @Query("id") id: String
    ): SaavnPlaylistResponse

    //Individual Song
    @GET("api/songs/{id}")
    suspend fun getSongDetailsById(
        @Path("id") id: String
    ): SaavnPlaylistResponse

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