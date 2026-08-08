package com.sumit.muzixx.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.FeaturedPlayList
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderDelete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistRemove
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
import com.sumit.muzixx.data.model.Playlist
import com.sumit.muzixx.data.model.Song
import com.sumit.muzixx.viewmodel.MusicViewModel
import com.sumit.muzixx.utils.glassEffect

@Composable
fun LibraryLoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Scanning your library tracks...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PlaylistDetailView(
    currentPlaylist: Playlist,
    viewModel: MusicViewModel,
    bottomPadding: androidx.compose.ui.unit.Dp,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onPlaybackRequest: (List<Song>, Int) -> Unit,
    onEditRequest: (String, String) -> Unit,
    onSongActionClick: (Song) -> Unit,
    onBackClick: () -> Unit
) {
    BackHandler {
        viewModel.selectedPlaylist = null
        onBackClick()
    }

    val playlistSongs = currentPlaylist.songs
    val headerCover = playlistSongs.firstOrNull()?.artUri
    val isSystemPlaylist = currentPlaylist.id == "local_songs" || currentPlaylist.id.startsWith("folder_")

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
            IconButton(onClick = {
                viewModel.selectedPlaylist = null
                onBackClick()
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Playlist",
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
            if (playlistSongs.isEmpty()) {
                Text(
                    text = "This playlist is empty.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = bottomPadding)
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
                                contentDescription = currentPlaylist.name,
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
                                text = currentPlaylist.name,
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
                                    onClick = { onPlaybackRequest(playlistSongs.toList().shuffled(), 0) },
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
                                    onClick = { onPlaybackRequest(playlistSongs.toList(), 0) },
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

                                if (!isSystemPlaylist) {
                                    FilledTonalButton(
                                        onClick = { onEditRequest(currentPlaylist.id, currentPlaylist.name) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.size(48.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Edit,
                                            contentDescription = "Rename",
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    itemsIndexed(
                        items = playlistSongs,
                        key = { index, song -> "${song.id}_$index" }
                    ) { index, song ->
                        LibrarySongItem(
                            song = song,
                            index = index,
                            isSelected = viewModel.selectedSong?.id == song.id,
                            currentPlaylistId = currentPlaylist.id,
                            onActionClick = { onSongActionClick(song) },
                            onRemoveClick = {
                                viewModel.removeSongFromPlaylist(currentPlaylist.id, song)
                                viewModel.selectedPlaylist = viewModel.playlists.find { it.id == currentPlaylist.id }
                            },
                            onClick = { onPlaybackRequest(playlistSongs.toList(), index) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LibrarySongItem(
    song: Song,
    index: Int,
    isSelected: Boolean,
    currentPlaylistId: String,
    onActionClick: () -> Unit = {},
    onRemoveClick: () -> Unit = {},
    onClick: () -> Unit
) {
    val animatedTextColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(durationMillis = 250),
        label = "SongItemTextColor"
    )
    var showSongMenu by remember { mutableStateOf(false) }
    val isSystemPlaylist = currentPlaylistId == "local_songs" || currentPlaylistId.startsWith("folder_")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
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
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop,
            error = painterResource(R.drawable.default_music),
            placeholder = painterResource(R.drawable.default_music)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = animatedTextColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
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

        Box {
            IconButton(onClick = { showSongMenu = true }) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "Options Menu",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DropdownMenu(
                expanded = showSongMenu,
                onDismissRequest = { showSongMenu = false },
                containerColor = Color.Transparent,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.glassEffect(RoundedCornerShape(16.dp))
            ) {
                DropdownMenuItem(
                    text = { Text("Add to Playlist") },
                    onClick = {
                        showSongMenu = false
                        onActionClick()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                            contentDescription = null
                        )
                    }
                )

                if (!isSystemPlaylist) {
                    DropdownMenuItem(
                        text = { Text("Remove from this playlist", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showSongMenu = false
                            onRemoveClick()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.PlaylistRemove,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlaylistRootListView(
    playlists: List<Playlist>,
    playlistPendingActionsMenu: Playlist?,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onCreateClick: () -> Unit,
    onPlaylistSelect: (Playlist) -> Unit,
    onPlaylistLongClick: (Playlist) -> Unit,
    onMenuDismiss: () -> Unit,
    onRenameTrigger: () -> Unit,
    onDeleteTrigger: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Text(
                        text = "Library",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(
                        onClick = onCreateClick,
                        modifier = Modifier
                            .glassEffect(CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Create Playlist"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = bottomPadding),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(playlists, key = { it.id }) { playlist ->
                    val isSystemPlaylist = playlist.id == "local_songs" || playlist.id.startsWith("folder_")

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassEffect(RoundedCornerShape(20.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { onPlaylistSelect(playlist) },
                                    onLongClick = { onPlaylistLongClick(playlist) }
                                )
                                .padding(vertical = 16.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.FeaturedPlayList,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = playlist.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${playlist.songs.size} songs",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (!isSystemPlaylist) {
                                IconButton(onClick = { onPlaylistLongClick(playlist) }) {
                                    Icon(
                                        imageVector = Icons.Rounded.MoreVert,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    DropdownMenu(
                                        expanded = playlistPendingActionsMenu?.id == playlist.id,
                                        onDismissRequest = onMenuDismiss,
                                        shape = RoundedCornerShape(16.dp),
                                        containerColor = Color.Transparent,
                                        modifier = Modifier.glassEffect(RoundedCornerShape(16.dp))
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Rename Playlist") },
                                            onClick = onRenameTrigger,
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Rounded.DriveFileRenameOutline,
                                                    contentDescription = null
                                                )
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Delete Playlist", color = MaterialTheme.colorScheme.error) },
                                            onClick = { onDeleteTrigger(playlist.id) },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Rounded.FolderDelete,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
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
fun PlaylistSelectorDialog(
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onPlaylistChosen: (String) -> Unit
) {
    val customPlaylistsOnly = playlists.filter { it.id != "local_songs" && !it.id.startsWith("folder_") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.Transparent,
        modifier = Modifier.glassEffect(RoundedCornerShape(24.dp)),
        title = {
            Text(
                text = "Add to Playlist",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (customPlaylistsOnly.isEmpty()) {
                Text(
                    text = "Please create a custom playlist first using the '+' button.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 250.dp)) {
                    items(customPlaylistsOnly, key = { it.id }) { targetPlaylist ->
                        Text(
                            text = targetPlaylist.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPlaylistChosen(targetPlaylist.id) }
                                .padding(vertical = 14.dp, horizontal = 8.dp)
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Close", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun RenamePlaylistDialog(
    nameInput: String,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.Transparent,
        modifier = Modifier.glassEffect(RoundedCornerShape(24.dp)),
        title = {
            Text(
                text = "Rename Playlist",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            OutlinedTextField(
                value = nameInput,
                onValueChange = onNameChange,
                label = { Text("Playlist Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text(text = "Save", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}

@Composable
fun CreatePlaylistDialog(
    nameInput: String,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onCreate: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        containerColor = Color.Transparent,
        modifier = Modifier.glassEffect(RoundedCornerShape(24.dp)),
        title = {
            Text(
                text = "New Playlist",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            OutlinedTextField(
                value = nameInput,
                onValueChange = onNameChange,
                label = { Text("Playlist Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        confirmButton = {
            TextButton(onClick = onCreate) {
                Text(text = "Create", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}