package com.sumit.muzixx.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sumit.muzixx.data.Song
import com.sumit.muzixx.ui.components.PlaylistSelectorContent
import com.sumit.muzixx.viewmodel.MusicViewModel
import com.sumit.muzixx.utils.glassEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    var isSearchFocused by remember { mutableStateOf(false) }

    var activeSongForPlaylist by remember { mutableStateOf<Song?>(null) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val tabs = listOf("JioSaavn", "YouTube Music")
    val saavnResults = viewModel.saavnSearchResults
    val youtubeResults = viewModel.searchResults

    val hasSaavnData by remember { derivedStateOf { saavnResults.isNotEmpty() } }
    val hasYoutubeData by remember { derivedStateOf { youtubeResults.isNotEmpty() } }

    val isPlayerActive = viewModel.selectedSong != null
    val bottomPadding = if (isPlayerActive) 92.dp else 24.dp

    val searchRecommendations by remember(searchQuery) {
        derivedStateOf {
            if (searchQuery.isBlank()) emptyList()
            else listOf(
                searchQuery.trim(),
                "${searchQuery.trim()} song",
                "${searchQuery.trim()} remix",
                "${searchQuery.trim()} lofi acoustic"
            ).distinct()
        }
    }

    val userCreatedPlaylists by remember {
        derivedStateOf {
            viewModel.playlists.filter { playlist ->
                playlist.id != "local_songs" &&
                        !playlist.id.startsWith("folder_") &&
                        playlist.name != "Local Songs"
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(top = 8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .onFocusChanged { focusState ->
                        isSearchFocused = focusState.isFocused
                    },
                placeholder = { Text("Search songs, artists...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            saavnResults.clear()
                            youtubeResults.clear()
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (searchQuery.isNotBlank()) {
                            if (selectedTab == 0) viewModel.searchJioSaavn(searchQuery.trim())
                            else viewModel.searchOnlineSongs(searchQuery.trim())
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )

            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth(),
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            if (searchQuery.isNotBlank()) {
                                if (index == 0) viewModel.searchJioSaavn(searchQuery.trim())
                                else viewModel.searchOnlineSongs(searchQuery.trim())
                            }
                        },
                        text = {
                            Text(
                                title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.SemiBold
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                if (selectedTab == 0) {
                    when {
                        viewModel.isSaavnLoading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)

                        isSearchFocused || !hasSaavnData -> {
                            InteractiveSearchContextPanel(
                                query = searchQuery,
                                history = viewModel.searchHistory.take(5),
                                recommendations = searchRecommendations,
                                placeholderText = "Search your favorite tracks on JioSaavn",
                                onSelection = { chosenQuery ->
                                    searchQuery = chosenQuery
                                    viewModel.searchJioSaavn(chosenQuery)
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                },
                                onDeleteHistory = { query -> viewModel.deleteSearchQuery(query) }
                            )
                        }
                        else -> SongResultsList(
                            songs = saavnResults,
                            bottomPadding = bottomPadding,
                            onSongClick = { index -> viewModel.playSaavnSongWithYouTubeAutoplay(saavnResults, index) },
                            onAddToPlaylistClick = { song -> activeSongForPlaylist = song },
                            onAddToQueueClick = { song ->
                                viewModel.addSongToQueue(song)
                            }
                        )
                    }
                } else {
                    when {
                        viewModel.isSearchLoading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)

                        isSearchFocused || !hasYoutubeData -> {
                            InteractiveSearchContextPanel(
                                query = searchQuery,
                                history = viewModel.searchHistory.take(5),
                                recommendations = searchRecommendations,
                                placeholderText = "Search your favorite videos on YouTube",
                                onSelection = { chosenQuery ->
                                    searchQuery = chosenQuery
                                    viewModel.searchOnlineSongs(chosenQuery)
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                },
                                onDeleteHistory = { query -> viewModel.deleteSearchQuery(query) }
                            )
                        }
                        else -> SongResultsList(
                            songs = youtubeResults,
                            bottomPadding = bottomPadding,
                            onSongClick = { index -> viewModel.playYouTubeSearchResultWithAutoplay(youtubeResults, index) },
                            onAddToPlaylistClick = { song -> activeSongForPlaylist = song },
                            onAddToQueueClick = { song ->
                                viewModel.addSongToQueue(song)
                            },
                            onItemVisible = { song ->
                                val cleanYtId = song.id.replace("yt_", "").trim()
                                viewModel.preloadYouTubeStream(cleanYtId)
                            }
                        )
                    }
                }
            }
        }

        if (activeSongForPlaylist != null) {
            ModalBottomSheet(
                onDismissRequest = { activeSongForPlaylist = null },
                containerColor = MaterialTheme.colorScheme.background,
                dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.2f)) }
            ) {
                PlaylistSelectorContent(
                    song = activeSongForPlaylist,
                    playlists = userCreatedPlaylists,
                    onPlaylistSelected = { playlist ->
                        activeSongForPlaylist?.let { song ->
                            viewModel.addSongToPlaylist(playlist.id, song)
                        }
                        activeSongForPlaylist = null
                    },
                    onCloseClick = { activeSongForPlaylist = null },
                    modifier = Modifier.navigationBarsPadding()
                )
            }
        }
    }
}

@Composable
fun InteractiveSearchContextPanel(
    query: String,
    history: List<String>,
    recommendations: List<String>,
    placeholderText: String,
    onSelection: (String) -> Unit,
    onDeleteHistory: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (query.isBlank()) {
            if (history.isNotEmpty()) {
                SearchHistoryLayout(
                    history = history,
                    onHistoryClick = onSelection,
                    onDeleteClick = onDeleteHistory
                )
            } else {
                EmptyStatePlaceholder(placeholderText)
            }
        } else {
            SearchRecommendationsLayout(
                recommendations = recommendations,
                onRecommendationClick = onSelection
            )
        }
    }
}

@Composable
fun SearchRecommendationsLayout(
    recommendations: List<String>,
    onRecommendationClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Text(
            text = "Suggestions",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 6.dp, bottom = 10.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassEffect(shape = RoundedCornerShape(24.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(recommendations) { suggestion ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onRecommendationClick(suggestion) }
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.TrendingUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = suggestion,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchHistoryLayout(
    history: List<String>,
    onHistoryClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Text(
            text = "Recent Searches",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 6.dp, bottom = 10.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassEffect(shape = RoundedCornerShape(24.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(history) { query ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onHistoryClick(query) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = query,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(
                            onClick = { onDeleteClick(query) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Delete history item",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SongResultsList(
    songs: List<Song>,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onSongClick: (Int) -> Unit,
    onAddToPlaylistClick: (Song) -> Unit,
    onAddToQueueClick: (Song) -> Unit,
    onItemVisible: (Song) -> Unit = {}
) {
    var expandedMenuIndex by remember { mutableStateOf<Int?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomPadding)
    ) {
        itemsIndexed(songs, key = { index, song -> "${song.id}_$index" }) { index, song ->
            LaunchedEffect(song.id) {
                onItemVisible(song)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSongClick(index) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = song.artUri,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box {
                    IconButton(onClick = { expandedMenuIndex = index }) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "Options Menu",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = expandedMenuIndex == index,
                        onDismissRequest = { expandedMenuIndex = null },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Add to Playlist", fontWeight = FontWeight.Medium) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = {
                                expandedMenuIndex = null
                                onAddToPlaylistClick(song)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Add to Queue", fontWeight = FontWeight.Medium) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = {
                                expandedMenuIndex = null
                                onAddToQueueClick(song)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStatePlaceholder(message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 32.dp),
            textAlign = TextAlign.Center
        )
    }
}