@file:Suppress("DEPRECATION")

package com.sumit.muzixx.ui.screens

import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sumit.muzixx.viewmodel.MusicViewModel
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MusicViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val backGroundColor = MaterialTheme.colorScheme.background

    var showThemeDialog by remember { mutableStateOf(false) }

    val streamOverWifiOnly = viewModel.settings.streamWifiOnly
    val downloadOverWifiOnly = viewModel.settings.downloadWifiOnly
    val showLyricsOnPlayer = viewModel.settings.showLyrics
    val normalizeAudio = viewModel.settings.normalizeAudio
    val skipSilence = viewModel.settings.skipSilence
    val currentTheme by remember { derivedStateOf { viewModel.settings.appTheme } }

    val currentAudioQuality = viewModel.settings.audioQuality
    var showQualityDialog by remember { mutableStateOf(false) }

    val profileUrl = "https://github.com/Sumitx5"

    val appVersion = remember {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (_: PackageManager.NameNotFoundException) {
            "1.0.0"
        }
    }

    LaunchedEffect(Unit) {
        viewModel.calculateCurrentCacheSize(context)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backGroundColor)
    ) {
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
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
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
                    subtitle = "Active Accent: $currentTheme",
                    icon = Icons.Default.Palette
                ) {
                    showThemeDialog = true
                }
            }

            //SECTION 2: MEDIA QUALITY SETTINGS
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
                val qualitySubtitle = when {
                    currentAudioQuality.contains("320") -> "Extreme (320kbps high-fidelity layout standard)"
                    currentAudioQuality.contains("160") -> "Standard (160kbps balanced audio stream setup)"
                    currentAudioQuality.contains("96") -> "Data Saver (96kbps low data consumption setup)"
                    else -> "Standard ($currentAudioQuality standard configuration layout)"
                }

                SettingsClickableItem(
                    title = "Audio Streaming Quality",
                    subtitle = qualitySubtitle,
                    icon = Icons.Default.HighQuality
                ) {
                    showQualityDialog = true
                }
            }

            //SECTION 3: ADVANCED AUDIO ENGINE
            item { SettingsHeader(title = "Audio Engine (Advanced)") }
            item {
                SettingsSwitchItem(
                    title = "Audio Normalization",
                    subtitle = "Adjust volume levels across all tracks to match uniformly",
                    icon = Icons.Default.Equalizer,
                    checked = normalizeAudio,
                    onCheckedChange = { viewModel.updateNormalizeAudioLive(it) }
                )
            }
            item {
                SettingsSwitchItem(
                    title = "Skip Silence",
                    subtitle = "Automatically skip dead air/silent parts at the end or start of files",
                    icon = Icons.Default.SkipNext,
                    checked = skipSilence,
                    onCheckedChange = { viewModel.updateSkipSilenceLive(it) }
                )
            }

            //SECTION 4: STORAGE & CACHE
            item { SettingsHeader(title = "Storage & Cache") }
            item {
                SettingsClickableItem(
                    title = "Clear Audio Cache",
                    subtitle = "Free up storage by clearing temporarily streamed image arts and audio buffers (${viewModel.cacheSizeText})",
                    icon = Icons.Default.Delete
                ) {
                    viewModel.clearAudioCache(context)
                }
            }

            //SECTION 5: SYSTEM AND UPDATES
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

            //SECTION 6: ABOUT
            item { SettingsHeader(title = "About") }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
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
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "MuzixX Player",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = 0.12f
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        AboutRowItem(
                            label = "Version",
                            value = "v$appVersion",
                            valueColor = MaterialTheme.colorScheme.primary
                        )
                        AboutRowItem(
                            label = "Build Variant",
                            value = "Release Stable",
                            valueColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )

                        Box(
                            modifier = Modifier.clickable {
                                val intent = Intent(Intent.ACTION_VIEW, profileUrl.toUri())
                                context.startActivity(intent)
                            }
                        ) {
                            AboutRowItem(
                                label = "Developer Architecture",
                                value = "Sumit Singh",
                                valueColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        AboutRowItem(
                            label = "Framework",
                            value = "Jetpack Compose (Android)",
                            valueColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "© 2026 MuzixX Inc. All code execution blocks optimized for high performance playback mechanics.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
    // Theme-Box
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = {
                Text(
                    text = "Select Accent Color",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(modifier = Modifier.selectableGroup()) {
                    val themeOptions = listOf(
                        "Electric Blue / Cyan",
                        "Lime Green",
                        "Vibrant Yellow",
                        "Neon Pink / Magenta",
                        "Bright Orange",
                        "Neon Red",
                        "Match System"
                    )

                    themeOptions.forEach { theme ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .selectable(
                                    selected = (theme == currentTheme),
                                    onClick = {
                                        viewModel.updateAppTheme(theme)
                                        showThemeDialog = false
                                    },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (theme == currentTheme),
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = theme, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    //DYNAMIC AUDIO BITRATE SELECTION DIALOG
    if (showQualityDialog) {
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            title = {
                Text(
                    text = "Streaming Quality",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                viewModel.settings.updateAudioQuality("96kbps")
                                showQualityDialog = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (currentAudioQuality == "96kbps"),
                            onClick = {
                                viewModel.settings.updateAudioQuality("96kbps")
                                showQualityDialog = false
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    alpha = 0.6f
                                )
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Data Saver (96kbps)",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Aggressive data compression saving layout",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.settings.updateAudioQuality("160kbps")
                                showQualityDialog = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (currentAudioQuality == "160kbps"),
                            onClick = {
                                viewModel.settings.updateAudioQuality("160kbps")
                                showQualityDialog = false
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    alpha = 0.6f
                                )
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Balanced (160kbps)",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Standard balanced stream performance quality",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall
                            )
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
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    alpha = 0.6f
                                )
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Extreme High (320kbps)",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Crystal clear high-fidelity audio stream",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQualityDialog = false }) {
                    Text(
                        text = "Dismiss",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )
    }
}

// SUB-ITEMS
@Composable
private fun SettingsHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
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
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primaryContainer,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
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
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
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
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}