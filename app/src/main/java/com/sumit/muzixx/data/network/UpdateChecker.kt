package com.sumit.muzixx.data.network

import android.content.Context
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.Proxy
import java.net.URL

object UpdateChecker {
    var isUpdateChecking by mutableStateOf(false)
        private set

    var updateStatusMessage by mutableStateOf("")
        private set

    var showUpdateDialog by mutableStateOf(false)

    var isUpdateAvailable by mutableStateOf(false)
        private set

    fun dismissDialog() {
        showUpdateDialog = false
    }

    private fun extractVersionNumbers(versionStr: String): String {
        val clean = versionStr.split("-")[0]
        return clean.filter { it.isDigit() || it == '.' }.trim()
    }

    private fun isNewerVersion(local: String, latest: String): Boolean {
        val localParts = local.split(".").map { it.toIntOrNull() ?: 0 }
        val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }

        val maxLength = maxOf(localParts.size, latestParts.size)
        for (i in 0 until maxLength) {
            val localPart = localParts.getOrElse(i) { 0 }
            val latestPart = latestParts.getOrElse(i) { 0 }
            if (latestPart > localPart) return true
            if (localPart > latestPart) return false
        }
        return false
    }

    suspend fun check(context: Context, isManualCheck: Boolean = false) {
        val githubUser = "Sumitx5"
        val repoName = "Muzix_X"

        val localVersionNumbers = try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            extractVersionNumbers(packageInfo.versionName ?: "1.0.0")
        } catch (_: Exception) {
            "1.0.0"
        }

        isUpdateChecking = true
        isUpdateAvailable = false
        updateStatusMessage = "Checking GitHub for updates..."

        if (isManualCheck) {
            showUpdateDialog = true
        }

        try {
            val latestTag = withContext(Dispatchers.IO) {
                var connection: HttpURLConnection? = null
                try {
                    val url = URL("https://api.github.com/repos/$githubUser/$repoName/releases/latest")
                    connection = url.openConnection(Proxy.NO_PROXY) as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                    connection.setRequestProperty("User-Agent", "MuzixX-App-Updater")
                    connection.connectTimeout = 8000
                    connection.readTimeout = 8000
                    connection.doInput = true

                    if (connection.responseCode == 200) {
                        val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                        val regex = "\"tag_name\"\\s*:\\s*\"([^\"]+)\"".toRegex()
                        val matchResult = regex.find(responseText)
                        matchResult?.groups?.get(1)?.value?.trim()
                    } else {
                        null
                    }
                } catch (_: Exception) {
                    null
                } finally {
                    connection?.disconnect()
                }
            }

            if (latestTag != null) {
                val latestVersionNumbers = extractVersionNumbers(latestTag)

                if (isNewerVersion(localVersionNumbers, latestVersionNumbers)) {
                    isUpdateAvailable = true
                    updateStatusMessage = "A fresh update is available for MuzixX!\n\nLatest on GitHub: $latestTag\nYour Version: v$localVersionNumbers\n\nWould you like to head to GitHub to download the new build?"
                    showUpdateDialog = true
                } else {
                    isUpdateAvailable = false
                    updateStatusMessage = "MuzixX is up to date ($localVersionNumbers)."
                    if (isManualCheck) showUpdateDialog = true
                }
            } else {
                isUpdateAvailable = false
                updateStatusMessage = "No releases found on GitHub."
                if (isManualCheck) showUpdateDialog = true
            }
        } catch (_: Exception) {
            isUpdateAvailable = false
            updateStatusMessage = "Failed to reach server."
            if (isManualCheck) showUpdateDialog = true
        } finally {
            isUpdateChecking = false
        }
    }
}