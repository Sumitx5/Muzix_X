package com.sumit.muzixx.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
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
import com.sumit.muzixx.viewmodel.MusicViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MusicViewModel,
    context: Context,
    onProfileClick: () -> Unit,
    onMiniPlayerClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    val currentUserName = if (viewModel.isSettingsInitialized()) {
        viewModel.settings.userName
    } else {
        "User"
    }

    val trending = viewModel.saavnTrendingSongs
    val newReleases = viewModel.saavnNewReleases
    val hindiHits = viewModel.saavnHindiHits
    val local = viewModel.songs
    val selectedSong = viewModel.selectedSong

    BackHandler(drawerState.isOpen) {
        scope.launch { drawerState.close() }
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
        userName = currentUserName
    ) {
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
                            imageVector = Icons.Default.AccountCircle,
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
                            title = "Trending",
                            songs = trending,
                            isLoading = viewModel.isTrendingLoading,
                            onClick = { index -> viewModel.playSaavnSong(trending, index) }
                        )
                    }

                    item(key = "new_releases") {
                        SongSection(
                            title = "Latest Releases",
                            songs = newReleases,
                            isLoading = viewModel.isNewReleasesLoading,
                            onClick = { index -> viewModel.playSaavnSong(newReleases, index) }
                        )
                    }
                    item(key = "hindi_hits") {
                        SongSection(
                            title = "Hindi Hits",
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
            color = Color.White,
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
                CircularProgressIndicator(color = Color(0xFFE50914))
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