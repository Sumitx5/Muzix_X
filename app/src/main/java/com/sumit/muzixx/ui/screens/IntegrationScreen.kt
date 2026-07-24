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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntegrationScreen(
    viewModel: MusicViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showSpotifyDialog by remember { mutableStateOf(false) }
    var spotifyUrl by remember { mutableStateOf("") }
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
                        text = "Import your external playlists directly into MuzixX seamlessly.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                    )
                }

                item {
                    IntegrationCard(
                        title = "Import Spotify Playlist",
                        description = "Sync your favorite tracks from public Spotify links.",
                        brandColor = Color(0xFF1DB954),
                        onClick = { showSpotifyDialog = true }
                    )
                }

                item {
                    IntegrationCard(
                        title = "Import YouTube Music Playlist",
                        description = "Bring over your specialized streaming queues.",
                        brandColor = Color(0xFFFF0000),
                        onClick = {
                            Toast.makeText(context, "Coming Soon", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                item {
                    IntegrationCard(
                        title = "Import YouTube Playlist",
                        description = "Convert video collections directly to standard audio formats.",
                        brandColor = Color(0xFFE62117),
                        onClick = {
                            Toast.makeText(context, "Coming Soon", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // Spotify Link Input Dialog
            if (showSpotifyDialog) {
                AlertDialog(
                    onDismissRequest = { if (!isImporting) showSpotifyDialog = false },
                    title = { Text("Import Spotify Playlist") },
                    text = {
                        Column {
                            Text(
                                "Paste a public Spotify playlist URL below:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = spotifyUrl,
                                onValueChange = { spotifyUrl = it },
                                placeholder = { Text("https://open.spotify.com/playlist/...") },
                                singleLine = true,
                                enabled = !isImporting,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (isImporting) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    Text("Resolving and importing tracks...", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            enabled = !isImporting && spotifyUrl.isNotBlank(),
                            onClick = {
                                isImporting = true
                                viewModel.importSpotifyPlaylist(
                                    url = spotifyUrl,
                                    onSuccess = { name, count ->
                                        isImporting = false
                                        showSpotifyDialog = false
                                        spotifyUrl = ""
                                        Toast.makeText(context, "Successfully imported '$name' with $count tracks!", Toast.LENGTH_LONG).show()
                                    },
                                    onError = { errorMsg ->
                                        isImporting = false
                                        Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        ) {
                            Text("Import")
                        }
                    },
                    dismissButton = {
                        if (!isImporting) {
                            TextButton(onClick = { showSpotifyDialog = false }) {
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