package com.sumit.muzixx.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sumit.muzixx.data.network.UpdateChecker
import com.sumit.muzixx.utils.glassEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateDialog() {
    if (UpdateChecker.showUpdateDialog) {
        val uriHandler = LocalUriHandler.current

        val isUpToDate = !UpdateChecker.isUpdateChecking &&
                UpdateChecker.updateStatusMessage.contains("up to date", ignoreCase = true)

        BasicAlertDialog(
            onDismissRequest = {
                if (!UpdateChecker.isUpdateChecking) UpdateChecker.dismissDialog()
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(28.dp),
                        clip = false,
                        ambientColor = Color.Black.copy(alpha = 0.2f),
                        spotColor = Color.Black.copy(alpha = 0.4f)
                    )
                    .glassEffect(shape = RoundedCornerShape(28.dp))
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = when {
                            UpdateChecker.isUpdateChecking -> "Checking for updates..."
                            isUpToDate -> "You are Already Updated"
                            else -> "New Update Available!"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    if (UpdateChecker.isUpdateChecking) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    Text(
                        text = UpdateChecker.updateStatusMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!UpdateChecker.isUpdateChecking && !isUpToDate) {
                            TextButton(
                                onClick = { UpdateChecker.dismissDialog() },
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text("Dismiss")
                            }
                        }

                        when {
                            UpdateChecker.isUpdateChecking -> {
                                Button(enabled = false, onClick = {}) {
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
                    }
                }
            }
        }
    }
}