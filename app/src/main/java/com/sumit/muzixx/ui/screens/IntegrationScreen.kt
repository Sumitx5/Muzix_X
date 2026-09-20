package com.sumit.muzixx.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sumit.muzixx.viewmodel.MusicViewModel
import com.sumit.muzixx.utils.glassEffect

private enum class ImportSource(
    val title: String,
    val description: String,
    val placeholder: String
) {
    SPOTIFY(
        title = "Import Spotify Playlist",
        description = "Sync tracks from public Spotify playlist links.",
        placeholder = "https://open.spotify.com/playlist/..."
    ),
    YOUTUBE_MUSIC(
        title = "Import YouTube Music Playlist",
        description = "Import your curated YouTube Music collections directly.",
        placeholder = "https://music.youtube.com/playlist?list=..."
    ),
    YOUTUBE(
        title = "Import YouTube Playlist",
        description = "Extract videos from YouTube playlists into streamable tracks.",
        placeholder = "https://www.youtube.com/playlist?list=..."
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntegrationScreen(
    viewModel: MusicViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeImportSource by remember { mutableStateOf<ImportSource?>(null) }
    var inputUrl by remember { mutableStateOf("") }
    var isImporting by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Integrations",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Go Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Import your external playlists directly into MuzixX seamlessly. up to (400 Song's)",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                    )
                }

                item {
                    IntegrationCard(
                        title = "Import Spotify Playlist",
                        description = "Sync tracks from public Spotify playlist links.",
                        brandColor = Color(0xFF1DB954),
                        onClick = {
                            activeImportSource = ImportSource.SPOTIFY
                            inputUrl = ""
                        }
                    )
                }

                item {
                    IntegrationCard(
                        title = "Import YouTube Music Playlist",
                        description = "Bring over your curated YouTube Music queues.",
                        brandColor = Color(0xFFFF0000),
                        onClick = {
                            activeImportSource = ImportSource.YOUTUBE_MUSIC
                            inputUrl = ""
                        }
                    )
                }

                item {
                    IntegrationCard(
                        title = "Import YouTube Playlist",
                        description = "Convert public video playlists directly into streamable queues.",
                        brandColor = Color(0xFFE62117),
                        onClick = {
                            activeImportSource = ImportSource.YOUTUBE
                            inputUrl = ""
                        }
                    )
                }
            }

            // Universal Playlist Import Dialog
            activeImportSource?.let { source ->
                AlertDialog(
                    onDismissRequest = {
                        if (!isImporting) {
                            activeImportSource = null
                            inputUrl = ""
                        }
                    },
                    title = { Text(source.title, fontWeight = FontWeight.Bold) },
                    containerColor = Color.Transparent,
                    modifier = Modifier.glassEffect(RoundedCornerShape(16.dp)),
                    text = {
                        Column {
                            Text(
                                source.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = inputUrl,
                                onValueChange = { inputUrl = it },
                                placeholder = { Text(source.placeholder) },
                                singleLine = true,
                                enabled = !isImporting,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            if (isImporting) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    Text(
                                        "Extracting playlist and tracks...",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            enabled = !isImporting && inputUrl.isNotBlank(),
                            onClick = {
                                val cleanUrl = inputUrl.trim()
                                isImporting = true

                                when (source) {
                                    ImportSource.SPOTIFY -> {
                                        viewModel.contentManager.importSpotifyPlaylist(
                                            url = cleanUrl,
                                            onSuccess = { name, count ->
                                                isImporting = false
                                                activeImportSource = null
                                                inputUrl = ""
                                                Toast.makeText(
                                                    context,
                                                    "Imported '$name' ($count tracks)!",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            },
                                            onError = { errorMsg ->
                                                isImporting = false
                                                Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }

                                    ImportSource.YOUTUBE, ImportSource.YOUTUBE_MUSIC -> {
                                        viewModel.contentManager.importYouTubePlaylist(
                                            url = cleanUrl,
                                            onSuccess = { name, count ->
                                                isImporting = false
                                                activeImportSource = null
                                                inputUrl = ""
                                                Toast.makeText(
                                                    context,
                                                    "Imported '$name' ($count tracks)!",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            },
                                            onError = { errorMsg ->
                                                isImporting = false
                                                Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }
                            }
                        ) {
                            Text("Import")
                        }
                    },
                    dismissButton = {
                        if (!isImporting) {
                            TextButton(onClick = {
                                activeImportSource = null
                                inputUrl = ""
                            }) {
                                Text("Cancel")
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun IntegrationCard(
    title: String,
    description: String,
    brandColor: Color,
    onClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassEffect(shape = cardShape)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 38.dp)
                    .background(brandColor, RoundedCornerShape(2.dp))
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.CallReceived,
                contentDescription = "Import Arrow",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}