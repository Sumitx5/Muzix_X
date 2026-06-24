package com.sumit.muzixx.data

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val username: String = "",
    val email: String = "",
    val gender: String = "Prefer Not to Say",
    val totalSongsHeard: Int = 0,
    val monthlySongsHeard: Int = 0,
    val yearlySongsHeard: Int = 0,
    val totalPlaySeconds: Long = 0L,
    val monthlyPlaySeconds: Long = 0L,
    val yearlyPlaySeconds: Long = 0L
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "uid" to uid,
            "name" to name,
            "username" to username,
            "email" to email,
            "gender" to gender,
            "totalSongsHeard" to totalSongsHeard,
            "monthlySongsHeard" to monthlySongsHeard,
            "yearlySongsHeard" to yearlySongsHeard,
            "totalPlaySeconds" to totalPlaySeconds,
            "monthlyPlaySeconds" to monthlyPlaySeconds,
            "yearlyPlaySeconds" to yearlyPlaySeconds
        )
    }
}