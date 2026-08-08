package com.sumit.muzixx.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sumit.muzixx.R
import com.sumit.muzixx.data.model.Song
import com.sumit.muzixx.ui.components.HomeNavigationDrawer
import com.sumit.muzixx.utils.glassEffect
import com.sumit.muzixx.viewmodel.AuthViewModel
import com.sumit.muzixx.viewmodel.MusicViewModel
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
    onPermClick: () -> Unit,
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

    val hindiHits = viewModel.contentManager.saavnHminiHits
    val chuddyBuddies = viewModel.contentManager.saavnTrendingSongs
    val baarish = viewModel.contentManager.saavnNewReleases
    val selectedSong = viewModel.selectedSong

    val recentlyHeard = remember(viewModel.recentlyPlayedSongs) {
        viewModel.recentlyPlayedSongs
    }

    LaunchedEffect(recentlyHeard.size) {
        if (recentlyHeard.isNotEmpty()) {
            viewModel.contentManager.fetchRecommendationsFromHistory(recentlyHeard)
        }
    }

    val recommendedSongs = viewModel.contentManager.recommendedSongs

    val (isLastDayOfMonth, currentMonthName) = remember {
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val lastDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val monthLabel = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: "Month"
        Pair(currentDay == lastDay, monthLabel)
    }

    var featured90sPlaylists by remember { mutableStateOf<List<com.sumit.muzixx.data.model.SaavnCloudPlaylistObject>>(emptyList()) }
    var romancePlaylists by remember { mutableStateOf<List<com.sumit.muzixx.data.model.SaavnCloudPlaylistObject>>(emptyList()) }
    var partyHitsPlaylists by remember { mutableStateOf<List<com.sumit.muzixx.data.model.SaavnCloudPlaylistObject>>(emptyList()) } // Example: easily add more!

    var is90sLoading by remember { mutableStateOf(true) }
    var isRomanceLoading by remember { mutableStateOf(true) }
    var isPartyLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.contentManager.loadYouTubeTrendingSongs()

        launch {
            is90sLoading = true
            featured90sPlaylists = viewModel.contentManager.searchJioSaavnPlaylists("90s Hindi")
            is90sLoading = false
        }

        launch {
            isRomanceLoading = true
            romancePlaylists = viewModel.contentManager.searchJioSaavnPlaylists("Romance")
            isRomanceLoading = false
        }

        launch {
            isPartyLoading = true
            partyHitsPlaylists = viewModel.contentManager.searchJioSaavnPlaylists("Party Hits")
            isPartyLoading = false
        }
    }

    val ytTrendingSongs = viewModel.contentManager.youtubeTrendingSongs

    BackHandler(drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    BackHandler(viewModel.contentManager.currentCloudPlaylistName != null) {
        viewModel.contentManager.closeCloudPlaylistDetails()
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
        onPermClick = {
            scope.launch { drawerState.close() }
            onPermClick()
        },
        userName = currentUserName
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header
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
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(modifier = Modifier.padding(start = 4.dp)) {
                        Text(
                            text = "Hello, $currentUserName",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Content Area
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        bottom = if (selectedSong != null) 144.dp else 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Monthly Recap
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

                    // Recommended Songs
                    item(key = "song_recomends") {
                        if (recommendedSongs.isNotEmpty() || viewModel.contentManager.isRecommendationsLoading) {
                            SongSection(
                                title = "Recommended For You",
                                songs = recommendedSongs,
                                isLoading = viewModel.contentManager.isRecommendationsLoading,
                                isGrid = true,
                                onClick = { index ->
                                    viewModel.playYouTubeSearchResultWithAutoplay(
                                        recommendedSongs,
                                        index
                                    )
                                }
                            )
                        }
                    }

                    // Trending Today
                    item(key = "trending_songs") {
                        SongSection(
                            title = "Trending Today",
                            songs = ytTrendingSongs,
                            isLoading = viewModel.contentManager.isYouTubeTrendingLoading,
                            onClick = { index ->
                                viewModel.playYouTubeSearchResultWithAutoplay(ytTrendingSongs, index)
                            }
                        )
                    }

                    // Recently Heard
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

                    item(key = "saavn_baarish") {
                        SongSection(
                            title = "Baarish Or Dance",
                            songs = baarish,
                            isLoading = viewModel.contentManager.isNewReleasesLoading,
                            onClick = { index -> viewModel.playSaavnSong(baarish, index) }
                        )
                    }

                    item(key = "hindi_hits") {
                        SongSection(
                            title = "Hindi: India Superhit's",
                            songs = hindiHits,
                            isLoading = viewModel.contentManager.isHindiHitLoading,
                            onClick = { index -> viewModel.playSaavnSong(hindiHits, index) }
                        )
                    }

                    //Cloud Playlists 90
                    item(key = "cloud_playlists_90") {
                        CloudPlaylistSection(
                            title = "Best of 90's Playlists",
                            playlists = featured90sPlaylists,
                            isLoading = is90sLoading,
                            accentColor = accentColor,
                            playlistId = { item -> item.id ?: "" },
                            playlistTitle = { item -> item.name ?: "Cloud Playlist" },
                            imageUrl = { item -> item.image?.lastOrNull()?.url },
                            trackCount = { item -> item.songCount },
                            onPlaylistClick = { id, name ->
                                viewModel.contentManager.loadCloudPlaylistDetails(playlistId = id, playlistName = name)
                            }
                        )
                    }

                    item(key = "cloud_playlists_best_romance") {
                        CloudPlaylistSection(
                            title = "Best of Romance Playlists",
                            playlists = romancePlaylists,
                            isLoading = isRomanceLoading,
                            accentColor = accentColor,
                            playlistId = { item -> item.id ?: "" },
                            playlistTitle = { item -> item.name ?: "Cloud Playlist" },
                            imageUrl = { item -> item.image?.lastOrNull()?.url },
                            trackCount = { item -> item.songCount },
                            onPlaylistClick = { id, name ->
                                viewModel.contentManager.loadCloudPlaylistDetails(playlistId = id, playlistName = name)
                            }
                        )
                    }

                    item(key = "saavn_buddies") {
                        SongSection(
                            title = "Chuddy Buddies",
                            songs = chuddyBuddies,
                            isLoading = viewModel.contentManager.isTrendingLoading,
                            onClick = { index -> viewModel.playSaavnSong(chuddyBuddies, index) }
                        )
                    }

                    item(key = "cloud_playlists_party") {
                        CloudPlaylistSection(
                            title = "Top Party Playlists",
                            playlists = partyHitsPlaylists,
                            isLoading = isPartyLoading,
                            accentColor = accentColor,
                            playlistId = { item -> item.id ?: "" },
                            playlistTitle = { item -> item.name ?: "Cloud Playlist" },
                            imageUrl = { item -> item.image?.lastOrNull()?.url },
                            trackCount = { item -> item.songCount },
                            onPlaylistClick = { id, name ->
                                viewModel.contentManager.loadCloudPlaylistDetails(playlistId = id, playlistName = name)
                            }
                        )
                    }
                }
            }

            //Playlist Details Modal Overlay
            AnimatedVisibility(
                visible = viewModel.contentManager.currentCloudPlaylistName != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                val playlistName = viewModel.contentManager.currentCloudPlaylistName ?: ""
                val playlistSongs = viewModel.contentManager.currentCloudPlaylistSongs
                val headerCover = playlistSongs.firstOrNull()?.artUri

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.contentManager.closeCloudPlaylistDetails() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Text(
                            text = "Cloud Playlist",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (viewModel.contentManager.isCloudPlaylistLoading) {
                            CircularProgressIndicator(color = accentColor)
                        } else if (playlistSongs.isEmpty()) {
                            Text(
                                text = "No tracks found inside this playlist.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = if (selectedSong != null) 92.dp else 24.dp)
                            ) {
                                item(key = "playlist_header") {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp, vertical = 12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        AsyncImage(
                                            model = headerCover,
                                            contentDescription = playlistName,
                                            modifier = Modifier
                                                .size(160.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentScale = ContentScale.Crop,
                                            error = painterResource(R.drawable.default_music),
                                            placeholder = painterResource(R.drawable.default_music)
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = playlistName,
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${playlistSongs.size} Songs",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Button(
                                                onClick = { viewModel.playSaavnSong(playlistSongs.toList().shuffled(), 0) },
                                                enabled = playlistSongs.isNotEmpty(),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = accentColor,
                                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                                ),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(48.dp)
                                            ) {
                                                Icon(Icons.Rounded.Shuffle, "Shuffle", modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Shuffle", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                            }

                                            Button(
                                                onClick = { viewModel.playSaavnSong(playlistSongs.toList(), 0) },
                                                enabled = playlistSongs.isNotEmpty(),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = accentColor,
                                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                                ),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(48.dp)
                                            ) {
                                                Icon(Icons.Rounded.PlayArrow, "Play All", modifier = Modifier.size(22.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Play All", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }

                                itemsIndexed(
                                    items = playlistSongs,
                                    key = { _, song -> song.id }
                                ) { index, song ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.playSaavnSong(playlistSongs, index) }
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.width(28.dp)
                                        )

                                        AsyncImage(
                                            model = song.artUri,
                                            contentDescription = song.title,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop,
                                            error = painterResource(R.drawable.default_music),
                                            placeholder = painterResource(R.drawable.default_music)
                                        )

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = song.title,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.SemiBold,
                                                style = MaterialTheme.typography.bodyLarge,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = song.artist,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                style = MaterialTheme.typography.bodyMedium,
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
    isGrid: Boolean = false,
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
                    text = "No tracks found here.\nMust Be Server Error.\nWill Be Back Soon.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (isGrid) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                songs.chunked(2).forEachIndexed { rowIndex, pair ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val leftIndex = rowIndex * 2
                        Box(modifier = Modifier.weight(1f)) {
                            SongCompactChip(song = pair[0]) { onClick(leftIndex) }
                        }
                        if (pair.size > 1) {
                            val rightIndex = leftIndex + 1
                            Box(modifier = Modifier.weight(1f)) {
                                SongCompactChip(song = pair[1]) { onClick(rightIndex) }
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
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
private fun SongCompactChip(
    song: Song,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .glassEffect(RoundedCornerShape(12.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
        ) {
            AsyncImage(
                model = song.artUri,
                contentDescription = song.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SongCard(song: Song, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(100.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = song.artUri,
            contentDescription = "Song cover art",
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(20.dp))
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

@Composable
fun CloudPlaylistCard(
    title: String,
    imageUrl: String?,
    trackCount: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(90.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = title,
            modifier = Modifier
                .size(90.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            error = painterResource(R.drawable.default_music),
            placeholder = painterResource(R.drawable.default_music),
            contentScale = ContentScale.Crop
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = "${trackCount ?: 0} Tracks",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun <T> CloudPlaylistSection(
    title: String,
    playlists: List<T>,
    isLoading: Boolean,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    playlistId: (T) -> String,
    playlistTitle: (T) -> String,
    imageUrl: (T) -> String?,
    trackCount: (T) -> Int?,
    onPlaylistClick: (playlistId: String, playlistName: String) -> Unit
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
                    .height(90.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = accentColor)
            }
        } else if (playlists.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "No playlists found.\nMust Be Server Error.\nWill Be Back Soon.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(
                    items = playlists,
                    key = { index, item -> playlistId(item).ifEmpty { "playlist_$index" } }
                ) { _, playlist ->
                    val id = playlistId(playlist)
                    val name = playlistTitle(playlist)
                    CloudPlaylistCard(
                        title = name,
                        imageUrl = imageUrl(playlist),
                        trackCount = trackCount(playlist),
                        onClick = { onPlaylistClick(id, name) }
                    )
                }
            }
        }
    }
}