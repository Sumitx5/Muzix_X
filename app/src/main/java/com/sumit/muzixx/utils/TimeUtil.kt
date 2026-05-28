package com.sumit.muzixx.utils

import java.util.Locale
import java.util.concurrent.TimeUnit

//Milliseconds to time converter
fun formatTime(ms: Long): String {
    if (ms <= 0) return "00:00"
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}