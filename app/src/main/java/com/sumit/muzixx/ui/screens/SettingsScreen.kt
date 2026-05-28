package com.sumit.muzixx.ui.screens

import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sumit.muzixx.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MusicViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val customRed = remember { Color(0xFFE50914) }
    val customGrey = remember { Color(0xFF121212) }
    val customLightGrey = remember { Color(0xFFB3B3B3) }

    val streamOverWifiOnly = viewModel.settings.streamWifiOnly
    val downloadOverWifiOnly = viewModel.settings.downloadWifiOnly
    val showLyricsOnPlayer = viewModel.settings.showLyrics
    val normalizeAudio = viewModel.settings.normalizeAudio
    val skipSilence = viewModel.settings.skipSilence

    val currentAudioQuality = viewModel.settings.audioQuality
    var showQualityDialog by remember { mutableStateOf(false) }

    val appVersion = remember {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (e: PackageManager.NameNotFoundException) {
            "1.0.0"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ─── SETTINGS ───
        TopAppBar(
            title = {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Black,
                titleContentColor = Color.White
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // ─── SECTION 1: CONTENT & DISPLAY ───
            item { SettingsHeader(title = "Content & Display") }
            item {
                SettingsSwitchItem(
                    title = "Show Floating Lyrics",
                    subtitle = "Display synchronized lyrics on the full player deck when available",
                    icon = Icons.Default.LibraryMusic,
                    checked = showLyricsOnPlayer,
                    onCheckedChange = { viewModel.settings.updateShowLyrics(it) }
                )
            }
            item {
                SettingsClickableItem(
                    title = "Theme Options",
                    subtitle = "Pitch Black (AMOLED optimized active)",
                    icon = Icons.Default.Palette
                ) {}
            }

            // ─── SECTION 2: MEDIA QUALITY SETTINGS ───
            item { SettingsHeader(title = "Media Quality Settings") }
            item {
                SettingsSwitchItem(
                    title = "Stream via Wi-Fi Only",
                    subtitle = "Restrict data consumption by blocking online streaming on cellular networks",
                    icon = Icons.Default.Wifi,
                    checked = streamOverWifiOnly,
                    onCheckedChange = { viewModel.settings.updateStreamWifiOnly(it) }
                )
            }
            item {
                SettingsSwitchItem(
                    title = "Download over Wi-Fi Only",
                    subtitle = "Save local cached tracks exclusively when connected to stable Wi-Fi",
                    icon = Icons.Default.Download,
                    checked = downloadOverWifiOnly,
                    onCheckedChange = { viewModel.settings.updateDownloadWifiOnly(it) }
                )
            }
            item {
                SettingsClickableItem(
                    title = "Audio Streaming Quality",
                    subtitle = if (currentAudioQuality == "320kbps") {
                        "High (320kbps AAC audio streaming layout standard)"
                    } else {
                        "Standard (120kbps data-saving stream setup)"
                    },
                    icon = Icons.Default.HighQuality
                ) {
                    showQualityDialog = true
                }
            }

            // ─── SECTION 3: ADVANCED AUDIO ENGINE ───
            item { SettingsHeader(title = "Audio Engine (Advanced)") }
            item {
                SettingsSwitchItem(
                    title = "Audio Normalization",
                    subtitle = "Adjust volume levels across all tracks to match uniformly",
                    icon = Icons.Default.Equalizer,
                    checked = normalizeAudio,
                    onCheckedChange = { viewModel.settings.updateNormalizeAudio(it) }
                )
            }
            item {
                SettingsSwitchItem(
                    title = "Skip Silence",
                    subtitle = "Automatically skip dead air/silent parts at the end or start of files",
                    icon = Icons.Default.SkipNext,
                    checked = skipSilence,
                    onCheckedChange = { viewModel.settings.updateSkipSilence(it) }
                )
            }

            // ─── SECTION 4: STORAGE & CACHE ───
            item { SettingsHeader(title = "Storage & Cache") }
            item {
                SettingsClickableItem(
                    title = "Clear Audio Cache",
                    subtitle = "Free up storage by clearing temporarily streamed image arts and audio buffers",
                    icon = Icons.Default.Delete
                ) {
                    //Cache layer logic placement
                }
            }

            // ─── SECTION 5: SYSTEM AND UPDATES ───
            item { SettingsHeader(title = "System") }
            item {
                SettingsSwitchItem(
                    title = "Check Updates on Start",
                    subtitle = "Automatically check GitHub deployment branches when launching MuzixX",
                    icon = Icons.Default.CloudDownload,
                    checked = viewModel.settings.checkUpdatesOnStart,
                    onCheckedChange = { viewModel.settings.updateCheckUpdatesOnStart(it) }
                )
            }
            item {
                SettingsClickableItem(
                    title = "Check for Updates",
                    subtitle = "Verify build references with server deployment branch logs",
                    icon = Icons.Default.Refresh
                ) {
                    viewModel.triggerUpdateCheck(context)
                }
            }

            // ─── SECTION 6: ABOUT IN-APP DETAILS ───
            item { SettingsHeader(title = "About") }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = customGrey),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = customRed,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "MuzixX Player",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color(0xFF222222))
                        Spacer(modifier = Modifier.height(12.dp))

                        AboutRowItem(label = "Version", value = "v$appVersion", valueColor = customRed)
                        AboutRowItem(label = "Build Variant", value = "Release Stable", valueColor = customLightGrey)
                        AboutRowItem(label = "Developer Architecture", value = "Sumit Singh", valueColor = Color.White)
                        AboutRowItem(label = "Framework", value = "Jetpack Compose (Android)", valueColor = customLightGrey)

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "© 2026 MuzixX Inc. All code execution blocks optimized for high performance playback mechanics.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray
                        )
                    }
                }
            }
        }
    }

    //DYNAMIC AUDIO BITRATE SELECTION DIALOG
    if (showQualityDialog) {
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            containerColor = Color(0xFF1E1E1E),
            title = {
                Text(
                    text = "Streaming Quality",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.settings.updateAudioQuality("120kbps")
                                showQualityDialog = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (currentAudioQuality == "120kbps"),
                            onClick = {
                                viewModel.settings.updateAudioQuality("120kbps")
                                showQualityDialog = false
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = customRed, unselectedColor = Color.Gray)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Standard (120kbps)", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                            Text("Saves mobile data limits", color = customLightGrey, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.settings.updateAudioQuality("320kbps")
                                showQualityDialog = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (currentAudioQuality == "320kbps"),
                            onClick = {
                                viewModel.settings.updateAudioQuality("320kbps")
                                showQualityDialog = false
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = customRed, unselectedColor = Color.Gray)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("High (320kbps)", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                            Text("Crystal clear high-fidelity audio", color = customLightGrey, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQualityDialog = false }) {
                    Text("Dismiss", color = customRed)
                }
            }
        )
    }
}

//SUB-ITEMS

@Composable
private fun SettingsHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = Color(0xFFE50914),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB3B3B3),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFFE50914),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color(0xFF222222)
            )
        )
    }
}

@Composable
private fun SettingsClickableItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB3B3B3),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AboutRowItem(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFB3B3B3)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}