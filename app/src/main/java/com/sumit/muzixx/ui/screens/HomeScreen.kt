package com.sumit.muzixx.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sumit.muzixx.R
import com.sumit.muzixx.data.Song
import com.sumit.muzixx.ui.components.HomeNavigationDrawer
import com.sumit.muzixx.viewmodel.AuthViewModel
import com.sumit.muzixx.viewmodel.MusicViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MusicViewModel,
    authViewModel: AuthViewModel,
    context: Context,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onIntegrationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val accentColor = MaterialTheme.colorScheme.primary

    val currentUserName = when {
        authViewModel.currentUser?.displayName?.isNotBlank() == true -> {
            authViewModel.currentUser?.displayName ?: "User"
        }
        viewModel.isSettingsInitialized() -> {
            viewModel.settings.userName
        }
        else -> "User"
    }

    val trending = viewModel.saavnTrendingSongs
    val newReleases = viewModel.saavnNewReleases
    val hindiHits = viewModel.saavnHminiHits
    val local = viewModel.songs
    val selectedSong = viewModel.selectedSong

    LaunchedEffect(Unit) {
        if (viewModel.saavnPlaylistSearchResults.isEmpty()) {
            viewModel.searchJioSaavnPlaylists("90s Hindi")
        }
    }

    BackHandler(drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    BackHandler(viewModel.currentCloudPlaylistName != null) {
        viewModel.closeCloudPlaylistDetails()
    }

    HomeNavigationDrawer(
        drawerState = drawerState,
        onProfileClick = {
            scope.launch {
                drawerState.close()
                onProfileClick()
            }
        },
        onCheckUpdatesClick = {
            scope.launch { drawerState.close() }
            viewModel.triggerUpdateCheck(context)
        },
        onSettingsClick = {
            scope.launch { drawerState.close() }
            onSettingsClick()
        },
        onIntegrationsClick = {
            scope.launch { drawerState.close() }
            onIntegrationClick()
        },
        userName = currentUserName
    ) {
        // Root container layer
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // TOP BAR AREA
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(
                            imageVector = Icons.Rounded.AccountCircle,
                            contentDescription = "Open Navigation Menu",
                            tint = Color.White
                        )
                    }

                    Column(modifier = Modifier.padding(start = 4.dp)) {
                        Text(
                            text = "Hello, $currentUserName 👋",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.Gray
                        )
                        Text(
                            text = "Discover Music",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        bottom = if (selectedSong != null) 92.dp else 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    item(key = "trending_songs") {
                        SongSection(
                            title = "Trending Today",
                            songs = trending,
                            isLoading = viewModel.isTrendingLoading,
                            onClick = { index -> viewModel.playSaavnSong(trending, index) }
                        )
                    }

                    item(key = "new_releases") {
                        SongSection(
                            title = "Now Trending",
                            songs = newReleases,
                            isLoading = viewModel.isNewReleasesLoading,
                            onClick = { index -> viewModel.playSaavnSong(newReleases, index) }
                        )
                    }

                    item(key = "cloud_playlists_section") {
                        Column {
                            Text(
                                text = "Featured 90's Playlists",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(Modifier.height(12.dp))

                            if (viewModel.saavnPlaylistSearchResults.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(60.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = accentColor)
                                }
                            } else {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    itemsIndexed(viewModel.saavnPlaylistSearchResults) { _, playlist ->
                                        Column(
                                            modifier = Modifier
                                                .width(130.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    viewModel.loadCloudPlaylistDetails(
                                                        playlistId = playlist.id ?: "",
                                                        playlistName = playlist.name ?: "Cloud Playlist"
                                                    )
                                                }
                                        ) {
                                            AsyncImage(
                                                model = playlist.image?.lastOrNull()?.url,
                                                contentDescription = playlist.name,
                                                modifier = Modifier
                                                    .size(130.dp)
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .background(Color(0xFF1A1A1A)),
                                                error = painterResource(R.drawable.default_music),
                                                placeholder = painterResource(R.drawable.default_music),
                                                contentScale = ContentScale.Crop
                                            )
                                            Spacer(Modifier.height(8.dp))
                                            Text(
                                                text = playlist.name ?: "Unknown Playlist",
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                fontWeight = FontWeight.SemiBold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = "${playlist.songCount ?: 0} Tracks",
                                                color = Color(0xFFB3B3B3),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item(key = "hindi_hits") {
                        SongSection(
                            title = "Hindi: India Superhit",
                            songs = hindiHits,
                            isLoading = viewModel.isHindiHitLoading,
                            onClick = { index -> viewModel.playSaavnSong(hindiHits, index) }
                        )
                    }

                    item(key = "local_songs_section") {
                        SongSection(
                            title = "Local Music Files",
                            songs = local,
                            isLoading = viewModel.isLocalSongsLoading,
                            onClick = { index -> viewModel.playLocalSong(local, index) }
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = viewModel.currentCloudPlaylistName != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val playlistName = viewModel.currentCloudPlaylistName ?: ""
                val playlistSongs = viewModel.currentCloudPlaylistSongs

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.closeCloudPlaylistDetails() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = playlistName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                            color = Color.White
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { viewModel.playSaavnSong(playlistSongs.toList().shuffled(), 0) },
                            enabled = playlistSongs.isNotEmpty() && !viewModel.isCloudPlaylistLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFF1C1C1C)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Rounded.Shuffle, "Shuffle", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Shuffle", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.playSaavnSong(playlistSongs.toList(), 0) },
                            enabled = playlistSongs.isNotEmpty() && !viewModel.isCloudPlaylistLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFF333333)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, "Play All", modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play All", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(top = 12.dp), color = Color(0xFF1A1A1A))

                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (viewModel.isCloudPlaylistLoading) {
                            CircularProgressIndicator(color = accentColor)
                        } else if (playlistSongs.isEmpty()) {
                            Text("No tracks found inside this playlist.", color = Color.Gray)
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = if (selectedSong != null) 92.dp else 24.dp)
                            ) {
                                itemsIndexed(playlistSongs) { index, song ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.playSaavnSong(playlistSongs, index) }
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(
                                            model = song.artUri,
                                            contentDescription = null,
                                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop,
                                            error = painterResource(R.drawable.default_music),
                                            placeholder = painterResource(R.drawable.default_music)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = song.title,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = song.artist,
                                                color = Color.Gray,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SongSection(
    title: String,
    songs: List<Song>,
    isLoading: Boolean,
    onClick: (Int) -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(12.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (songs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "No tracks found here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                itemsIndexed(items = songs) { index, song ->
                    SongCard(song = song) { onClick(index) }
                }
            }
        }
    }
}

@Composable
private fun SongCard(song: Song, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(130.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = song.artUri,
            contentDescription = "Song cover art",
            modifier = Modifier
                .size(130.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1A1A1A)),
            error = painterResource(R.drawable.default_music),
            placeholder = painterResource(R.drawable.default_music),
            contentScale = ContentScale.Crop
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = song.title,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 2.dp)
        )

        Text(
            text = song.artist,
            color = Color(0xFFB3B3B3),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}