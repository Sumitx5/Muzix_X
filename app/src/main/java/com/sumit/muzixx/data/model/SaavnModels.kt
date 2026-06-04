package com.sumit.muzixx.data.model

import com.google.gson.annotations.SerializedName

//Wrapper for search endpoints and playlist tracks
data class SaavnPlaylistResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: SaavnPlaylistData?
)

data class SaavnDirectSongResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: List<SaavnTrackData>?
)

data class SaavnPlaylistData(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,

    // 💡 FIXED: This tells Gson to look for EITHER "results" (Search API) OR "songs" (Playlist/Home API)
    @SerializedName("songs", alternate = ["results"]) val songs: List<SaavnTrackData>?
)

// Main Track Data Class used across Search, Playlists, and Recommendations
data class SaavnTrackData(
    @SerializedName("id") val id: String?,
    @SerializedName("name", alternate = ["title"]) val name: String?,
    @SerializedName("duration") val duration: Int?,
    @SerializedName("image") val image: List<SaavnImageObject>?,
    @SerializedName("artists") val artists: SaavnArtistGroup?,
    @SerializedName("downloadUrl") val downloadUrl: List<SaavnDownloadUrlObject>?
)

data class SaavnImageObject(
    @SerializedName("quality") val quality: String?,
    @SerializedName("url") val url: String?
)

data class SaavnArtistGroup(
    @SerializedName("primary") val primary: List<SaavnArtistObject>?
)

data class SaavnArtistObject(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?
)

data class SaavnDownloadUrlObject(
    @SerializedName("quality") val quality: String?,
    @SerializedName("url") val url: String?
)