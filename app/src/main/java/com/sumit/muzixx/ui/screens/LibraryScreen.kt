package com.sumit.muzixx.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sumit.muzixx.data.Playlist
import com.sumit.muzixx.data.Song
import com.sumit.muzixx.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var activeSongForMenu by remember { mutableStateOf<Song?>(null) }
    var showPlaylistSelector by remember { mutableStateOf(false) }

    var playlistToEdit by remember { mutableStateOf<String?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInputText by remember { mutableStateOf("") }

    var playlistPendingActionsMenu by remember { mutableStateOf<Playlist?>(null) }

    val currentPlaylist = viewModel.selectedPlaylist
    val isPlayerActive = viewModel.selectedSong != null
    val dynamicallyCalculatedBottomPadding = if (isPlayerActive) 92.dp else 24.dp
    val accentColor = MaterialTheme.colorScheme.primary

    val executeAutoRoutedPlayback: (List<Song>, Int) -> Unit = { targetedList, indexPointer ->
        if (targetedList.isNotEmpty() && indexPointer in targetedList.indices) {
            val sampleTrack = targetedList[indexPointer]

            val isJioSaavn = sampleTrack.id.all { it.isDigit() } || sampleTrack.isStreaming

            when {
                sampleTrack.id.startsWith("yt_") -> viewModel.playYouTubeSong(targetedList, indexPointer)
                isJioSaavn -> viewModel.playSaavnSong(targetedList, indexPointer)
                else -> viewModel.playLocalSong(targetedList, indexPointer)
            }
        }
    }

    if (viewModel.isLocalSongsLoading) {
        LibraryLoadingOverlay()
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        endY = 600f
                    )
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (currentPlaylist != null) {
                    PlaylistDetailView(
                        currentPlaylist = currentPlaylist,
                        viewModel = viewModel,
                        bottomPadding = dynamicallyCalculatedBottomPadding,
                        onPlaybackRequest = executeAutoRoutedPlayback,
                        onEditRequest = { id, name ->
                            playlistToEdit = id
                            renameInputText = name
                            showRenameDialog = true
                        },
                        onSongActionClick = { song ->
                            activeSongForMenu = song
                            showPlaylistSelector = true
                        },
                        onBackClick = {
                            viewModel.selectedPlaylist = null
                        }
                    )
                } else {
                    PlaylistRootListView(
                        playlists = viewModel.playlists,
                        playlistPendingActionsMenu = playlistPendingActionsMenu,
                        bottomPadding = dynamicallyCalculatedBottomPadding,
                        onCreateClick = { showDialog = true },
                        onPlaylistSelect = { viewModel.selectedPlaylist = it },
                        onPlaylistLongClick = { playlist ->
                            playlistToEdit = playlist.id
                            renameInputText = playlist.name
                            playlistPendingActionsMenu = playlist
                        },
                        onMenuDismiss = { playlistPendingActionsMenu = null },
                        onRenameTrigger = {
                            playlistPendingActionsMenu = null
                            showRenameDialog = true
                        },
                        onDeleteTrigger = { id -> viewModel.deletePlaylist(id) }
                    )
                }
            }
        }
    }

    if (showPlaylistSelector && activeSongForMenu != null) {
        PlaylistSelectorDialog(
            playlists = viewModel.playlists,
            onDismiss = { showPlaylistSelector = false },
            onPlaylistChosen = { targetId ->
                viewModel.addSongToPlaylist(targetId, activeSongForMenu!!)
                showPlaylistSelector = false
            }
        )
    }

    if (showRenameDialog && playlistToEdit != null) {
        RenamePlaylistDialog(
            nameInput = renameInputText,
            onNameChange = { renameInputText = it },
            onDismiss = {
                showRenameDialog = false
                playlistToEdit = null
            },
            onSave = {
                if (renameInputText.isNotBlank()) {
                    viewModel.renamePlaylist(playlistToEdit!!, renameInputText.trim())
                    if (currentPlaylist?.id == playlistToEdit) {
                        viewModel.selectedPlaylist = viewModel.playlists.find { it.id == playlistToEdit }
                    }
                }
                showRenameDialog = false
                playlistToEdit = null
            }
        )
    }

    if (showDialog) {
        CreatePlaylistDialog(
            nameInput = newPlaylistName,
            onNameChange = { newPlaylistName = it },
            onDismiss = {
                showDialog = false
                newPlaylistName = ""
            },
            onCreate = {
                if (newPlaylistName.isNotBlank()) {
                    viewModel.createCustomPlaylist(newPlaylistName.trim())
                }
                showDialog = false
                newPlaylistName = ""
            }
        )
    }
}