package com.sumit.muzixx.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import com.sumit.muzixx.data.Song
import com.sumit.muzixx.ui.components.PlaylistSelectorContent
import com.sumit.muzixx.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }

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

    val userCreatedPlaylists by remember {
        derivedStateOf {
            viewModel.playlists.filter { playlist ->
                playlist.id != "local_songs" &&
                        !playlist.id.startsWith("folder_") &&
                        playlist.name != "Local Songs"
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search songs, artists...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            saavnResults.clear()
                            youtubeResults.clear()
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
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
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
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
                        text = { Text(title, style = MaterialTheme.typography.titleSmall) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                if (selectedTab == 0) {
                    when {
                        viewModel.isSaavnLoading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        !hasSaavnData -> EmptyStatePlaceholder("Search your favorite tracks on JioSaavn")
                        else -> SongResultsList(
                            songs = saavnResults,
                            bottomPadding = bottomPadding,
                            onSongClick = { index -> viewModel.playSaavnSong(saavnResults, index) },
                            onAddClick = { song -> activeSongForPlaylist = song }
                        )
                    }
                } else {
                    when {
                        viewModel.isSearchLoading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        !hasYoutubeData -> EmptyStatePlaceholder("Search your favorite videos on YouTube")
                        else -> SongResultsList(
                            songs = youtubeResults,
                            bottomPadding = bottomPadding,
                            onSongClick = { index -> viewModel.playYouTubeSong(youtubeResults, index) },
                            onAddClick = { song -> activeSongForPlaylist = song }
                        )
                    }
                }
            }
        }

        if (activeSongForPlaylist != null) {
            ModalBottomSheet(
                onDismissRequest = { activeSongForPlaylist = null },
                containerColor = Color(0xFF0F0F0F),
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
fun SongResultsList(
    songs: List<Song>,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onSongClick: (Int) -> Unit,
    onAddClick: (Song) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomPadding)
    ) {
        itemsIndexed(songs, key = { index, song -> "${song.id}_$index" }) { index, song ->
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
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
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

                IconButton(onClick = { onAddClick(song) }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add song",
                        tint = MaterialTheme.colorScheme.primary
                    )
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
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}