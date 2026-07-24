package com.sumit.muzixx.ui.screens

import android.content.Context
import android.widget.Toast
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
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.sumit.muzixx.utils.glassEffect
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MusicViewModel,
    authViewModel: AuthViewModel,
    context: Context,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onIntegrationClick: () -> Unit,
    onListenTogetherClick: () -> Unit,
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

    val recentlyHeard = remember(viewModel.recentlyPlayedSongs) {
        viewModel.recentlyPlayedSongs
    }

    val (isLastDayOfMonth, currentMonthName) = remember {
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val lastDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val monthLabel = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: "Month"
        Pair(currentDay == lastDay, monthLabel)
    }

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
            viewModel.triggerUpdateCheck()
        },
        onSettingsClick = {
            scope.launch { drawerState.close() }
            onSettingsClick()
        },
        onIntegrationsClick = {
            scope.launch { drawerState.close() }
            onIntegrationClick()
        },
        onListenTogetherClick = {
            scope.launch { drawerState.close() }
            onListenTogetherClick()
        },
        userName = currentUserName
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
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
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Column(modifier = Modifier.padding(start = 4.dp)) {
                        Text(
                            text = "Hello, $currentUserName",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Home",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        bottom = if (selectedSong != null) 144.dp else 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    //Monthly Recap
                    if (isLastDayOfMonth) {
                        item(key = "monthly_recap_section") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .glassEffect(RoundedCornerShape(20.dp))
                                    .clickable {
                                        Toast.makeText(context, "Recap is coming soon!", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CalendarMonth,
                                        contentDescription = "Recap Icon",
                                        tint = accentColor,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Your $currentMonthName Recap is Ready!",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Take a look back at your listening habits, top tracks, and statistics this past month.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Trending Today
                    item(key = "trending_songs") {
                        SongSection(
                            title = "Trending Today",
                            songs = trending,
                            isLoading = viewModel.isTrendingLoading,
                            onClick = { index -> viewModel.playSaavnSong(trending, index) }
                        )
                    }

                    // RECENTLY HEARD
                    if (recentlyHeard.isNotEmpty()) {
                        item(key = "recently_heard_songs") {
                            SongSection(
                                title = "Recently Played",
                                songs = recentlyHeard,
                                isLoading = false,
                                onClick = { index ->
                                    val targetTrack = recentlyHeard[index]
                                    if (targetTrack.id.startsWith("yt_")) {
                                        viewModel.playYouTubeSong(recentlyHeard, index)
                                    } else if (targetTrack.id.all { it.isDigit() } || targetTrack.isStreaming) {
                                        viewModel.playSaavnSong(recentlyHeard, index)
                                    } else {
                                        viewModel.playLocalSong(recentlyHeard, index)
                                    }
                                }
                            )
                        }
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
                                color = MaterialTheme.colorScheme.onSurface,
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
                                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                                error = painterResource(R.drawable.default_music),
                                                placeholder = painterResource(R.drawable.default_music),
                                                contentScale = ContentScale.Crop
                                            )
                                            Spacer(Modifier.height(8.dp))
                                            Text(
                                                text = playlist.name ?: "Unknown Playlist",
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                fontWeight = FontWeight.SemiBold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = "${playlist.songCount ?: 0} Tracks",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        .background(MaterialTheme.colorScheme.background)
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
                                tint = MaterialTheme.colorScheme.onBackground
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
                            color = MaterialTheme.colorScheme.onSurface
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
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
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
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, "Play All", modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play All", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(top = 12.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )

                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (viewModel.isCloudPlaylistLoading) {
                            CircularProgressIndicator(color = accentColor)
                        } else if (playlistSongs.isEmpty()) {
                            Text("No tracks found inside this playlist.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = song.artist,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            color = MaterialTheme.colorScheme.onSurface,
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                .background(MaterialTheme.colorScheme.surfaceVariant),
            error = painterResource(R.drawable.default_music),
            placeholder = painterResource(R.drawable.default_music),
            contentScale = ContentScale.Crop
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = song.title,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 2.dp)
        )

        Text(
            text = song.artist,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}