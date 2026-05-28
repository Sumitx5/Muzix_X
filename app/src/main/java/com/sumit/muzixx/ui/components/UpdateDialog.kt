package com.sumit.muzixx.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.sumit.muzixx.data.network.UpdateChecker

@Composable
fun UpdateDialog() {
    if (UpdateChecker.showUpdateDialog) {
        val uriHandler = LocalUriHandler.current

        val isUpToDate = !UpdateChecker.isUpdateChecking &&
                UpdateChecker.updateStatusMessage.contains("up to date", ignoreCase = true)

        AlertDialog(
            onDismissRequest = {
                if (!UpdateChecker.isUpdateChecking) UpdateChecker.dismissDialog()
            },
            title = {
                Text(
                    text = when {
                        UpdateChecker.isUpdateChecking -> "Checking for updates..."
                        isUpToDate -> "You're all set! ✨"
                        else -> "Update Available! 🚀"
                    }
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (UpdateChecker.isUpdateChecking) {
                        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                    }
                    Text(
                        text = UpdateChecker.updateStatusMessage,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                when {
                    UpdateChecker.isUpdateChecking -> {
                        TextButton(enabled = false, onClick = {}) {
                            Text("Please wait...")
                        }
                    }
                    isUpToDate -> {
                        Button(
                            onClick = { UpdateChecker.dismissDialog() }
                        ) {
                            Text("OK")
                        }
                    }
                    else -> {
                        Button(
                            onClick = {
                                UpdateChecker.dismissDialog()
                                uriHandler.openUri("https://github.com/Sumit282698/Muzix_X/releases")
                            }
                        ) {
                            Text("Download")
                        }
                    }
                }
            },
            dismissButton = {
                if (!UpdateChecker.isUpdateChecking && !isUpToDate) {
                    TextButton(
                        onClick = { UpdateChecker.dismissDialog() }
                    ) {
                        Text("Dismiss")
                    }
                }
            }
        )
    }
}