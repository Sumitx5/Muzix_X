package com.sumit.muzixx.ui.screens

import android.annotation.SuppressLint
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sumit.muzixx.R
import com.sumit.muzixx.data.model.SaavnCloudPlaylistObject
import com.sumit.muzixx.data.model.Song
import com.sumit.muzixx.utils.glassEffect
import com.sumit.muzixx.viewmodel.AuthViewModel
import com.sumit.muzixx.viewmodel.MusicViewModel
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

private fun String?.toLowResArtUrl(): String? {
    if (this == null) return null
    return when {
        contains("500x500") -> replace("500x500", "250x250")
        contains("350x350") -> replace("350x350", "250x250")
        contains("maxresdefault.jpg") -> replace("maxresdefault.jpg", "mqdefault.jpg")
        contains("hqdefault.jpg") -> replace("hqdefault.jpg", "mqdefault.jpg")
        else -> this
    }
}

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MusicViewModel,
    authViewModel: AuthViewModel,
    context: Context,
    modifier: Modifier = Modifier
) {
    val accentColor = MaterialTheme.colorScheme.primary

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val itemHorizontalSpacing = 12.dp
    val horizontalScreenPadding = 16.dp
    val dynamicCardWidth = (screenWidth - (horizontalScreenPadding * 2) - (itemHorizontalSpacing * 2)) / 3f

    val currentUserName = remember(authViewModel.currentUser, viewModel.settings.userName) {
        when {
            authViewModel.currentUser?.displayName?.isNotBlank() == true -> authViewModel.currentUser?.displayName ?: "User"
            viewModel.isSettingsInitialized() -> viewModel.settings.userName
            else -> "User"
        }
    }
    val message = listOf("Welcome back $currentUserName. Ready for some music?","Good to See You $currentUserName. Lets change the vibe!","$currentUserName, back for more beats?","$currentUserName! Step into your soundscape")

    val hindiHits = viewModel.contentManager.saavnHminiHits
    val chuddyBuddies = viewModel.contentManager.saavnTrendingSongs
    val baarish = viewModel.contentManager.saavnNewReleases
    val selectedSong = viewModel.selectedSong
    val ytTrendingSongs = viewModel.contentManager.youtubeTrendingSongs
    val recommendedSongs = viewModel.contentManager.recommendedSongs
    val recentlyHeard = viewModel.recentlyPlayedSongs

    LaunchedEffect(recentlyHeard.size) {
        if (recentlyHeard.isNotEmpty()) {
            viewModel.contentManager.fetchRecommendationsFromHistory(recentlyHeard)
        }
    }

    val (isLastDayOfMonth, currentMonthName) = remember {
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val lastDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val monthLabel = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: "Month"
        Pair(currentDay == lastDay, monthLabel)
    }

    var featured90sPlaylists by remember { mutableStateOf<List<SaavnCloudPlaylistObject>>(emptyList()) }
    var romancePlaylists by remember { mutableStateOf<List<SaavnCloudPlaylistObject>>(emptyList()) }
    var partyHitsPlaylists by remember { mutableStateOf<List<SaavnCloudPlaylistObject>>(emptyList()) }

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

    BackHandler(viewModel.contentManager.currentCloudPlaylistName != null) {
        viewModel.contentManager.closeCloudPlaylistDetails()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = message.random(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    bottom = if (selectedSong != null) 80.dp else 60.dp
                ),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
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

                item(key = "song_recomends") {
                    if (recommendedSongs.isNotEmpty() || viewModel.contentManager.isRecommendationsLoading) {
                        SongSection(
                            title = "Recommended For You",
                            songs = viewModel.contentManager.recommendedSongs,
                            isLoading = viewModel.contentManager.isRecommendationsLoading,
                            isGrid = true,
                            cardWidth = dynamicCardWidth,
                            onClick = { index ->
                                viewModel.playYouTubeSearchResultWithAutoplay(recommendedSongs, index)
                            }
                        )
                    }
                }

                item(key = "trending_songs") {
                    SongSection(
                        title = "Trending Today",
                        songs = ytTrendingSongs,
                        isLoading = viewModel.contentManager.isYouTubeTrendingLoading,
                        cardWidth = dynamicCardWidth,
                        onClick = { index ->
                            viewModel.playYouTubeSearchResultWithAutoplay(ytTrendingSongs, index)
                        }
                    )
                }

                if (recentlyHeard.isNotEmpty()) {
                    item(key = "recently_heard_songs") {
                        SongSection(
                            title = "Recently Played",
                            songs = recentlyHeard,
                            isLoading = false,
                            cardWidth = dynamicCardWidth,
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
                        cardWidth = dynamicCardWidth,
                        onClick = { index -> viewModel.playSaavnSong(baarish, index) }
                    )
                }

                item(key = "hindi_hits") {
                    SongSection(
                        title = "Hindi: India Superhit's",
                        songs = hindiHits,
                        isLoading = viewModel.contentManager.isHindiHitLoading,
                        cardWidth = dynamicCardWidth,
                        onClick = { index -> viewModel.playSaavnSong(hindiHits, index) }
                    )
                }

                item(key = "cloud_playlists_90") {
                    CloudPlaylistSection(
                        title = "Best of 90's Playlists",
                        playlists = featured90sPlaylists,
                        isLoading = is90sLoading,
                        accentColor = accentColor,
                        cardWidth = dynamicCardWidth,
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
                        cardWidth = dynamicCardWidth,
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
                        cardWidth = dynamicCardWidth,
                        onClick = { index -> viewModel.playSaavnSong(chuddyBuddies, index) }
                    )
                }

                item(key = "cloud_playlists_party") {
                    CloudPlaylistSection(
                        title = "Top Party Playlists",
                        playlists = partyHitsPlaylists,
                        isLoading = isPartyLoading,
                        accentColor = accentColor,
                        cardWidth = dynamicCardWidth,
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

        AnimatedVisibility(
            visible = viewModel.contentManager.currentCloudPlaylistName != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            val playlistName = viewModel.contentManager.currentCloudPlaylistName ?: ""
            val playlistSongs = viewModel.contentManager.currentCloudPlaylistSongs
            val headerCover = remember(playlistSongs) { playlistSongs.firstOrNull()?.artUri.toLowResArtUrl() }

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
                            contentPadding = PaddingValues(bottom = if (selectedSong != null) 122.dp else 44.dp)
                        ) {
                            item(key = "playlist_header") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(headerCover)
                                            .size(200, 200)
                                            .crossfade(true)
                                            .build(),
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
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(song.artUri.toLowResArtUrl())
                                            .size(80, 80)
                                            .crossfade(true)
                                            .build(),
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

@Composable
private fun SongSection(
    title: String,
    songs: List<Song>,
    isLoading: Boolean,
    cardWidth: Dp,
    isGrid: Boolean = false,
    onClick: (Int) -> Unit
) {
    val chunkedSongs = remember(songs) { songs.chunked(2) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(12.dp))

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            songs.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "No tracks found here.\nMust Be Server Error.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            isGrid -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    chunkedSongs.forEachIndexed { rowIndex, pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val leftIndex = rowIndex * 2

                            Box(modifier = Modifier.weight(1f)) {
                                SongCompactChip(
                                    song = pair[0],
                                    onClick = { onClick(leftIndex) }
                                )
                            }

                            if (pair.size > 1) {
                                val rightIndex = leftIndex + 1
                                Box(modifier = Modifier.weight(1f)) {
                                    SongCompactChip(
                                        song = pair[1],
                                        onClick = { onClick(rightIndex) }
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            else -> {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(
                        items = songs,
                        key = { index, song -> song.id.ifEmpty { "song_$index" } }
                    ) { index, song ->
                        SongCard(
                            song = song,
                            cardWidth = cardWidth,
                            onClick = { onClick(index) }
                        )
                    }
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
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(song.artUri.toLowResArtUrl())
                    .size(80, 80)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp)),
                placeholder = painterResource(R.drawable.default_music),
                error = painterResource(R.drawable.default_music)
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
private fun SongCard(
    song: Song,
    cardWidth: Dp,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(cardWidth)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(song.artUri.toLowResArtUrl())
                .size(120, 120)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .size(cardWidth)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(R.drawable.default_music),
            error = painterResource(R.drawable.default_music)
        )

        Spacer(Modifier.height(6.dp))

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
    cardWidth: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(cardWidth)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl.toLowResArtUrl())
                .size(120, 120)
                .crossfade(true)
                .build(),
            contentDescription = title,
            modifier = Modifier
                .size(cardWidth)
                .clip(RoundedCornerShape(16.dp))
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
    cardWidth: Dp,
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
                    .height(cardWidth),
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
                    text = "No playlists found.\nMust Be Server Error.",
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
                    key = { index, item ->
                        val id = playlistId(item)
                        id.ifEmpty { "playlist_$index" }
                    }
                ) { _, playlist ->
                    val id = playlistId(playlist)
                    val name = playlistTitle(playlist)
                    CloudPlaylistCard(
                        title = name,
                        imageUrl = imageUrl(playlist),
                        trackCount = trackCount(playlist),
                        cardWidth = cardWidth,
                        onClick = { onPlaylistClick(id, name) }
                    )
                }
            }
        }
    }
}