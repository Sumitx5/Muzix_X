package com.sumit.muzixx.ui.screens

import android.graphics.drawable.BitmapDrawable
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.sumit.muzixx.R
import com.sumit.muzixx.viewmodel.MusicViewModel
import com.sumit.muzixx.data.network.AudioDownloader
import com.sumit.muzixx.data.model.RepeatMode
import com.sumit.muzixx.ui.components.PlaylistSelectorContent
import com.sumit.muzixx.utils.formatTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Suppress("AssignedValueIsNeverRead")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayerScreen(
    viewModel: MusicViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val song = viewModel.selectedSong
    val repeatMode = viewModel.currentRepeatMode
    val accentColor = MaterialTheme.colorScheme.primary

    val defaultAccent = remember { Color(0xFF230305) }
    var dynamicAccentColor by remember { mutableStateOf(defaultAccent) }
    //Color Accent Art Handle
    val animatedAccentColor by animateColorAsState(
        targetValue = dynamicAccentColor,
        animationSpec = tween(durationMillis = 600),
        label = "DynamicThemeAccent"
    )
    //Playlist Handle
    val userCreatedPlaylists by remember {
        derivedStateOf {
            viewModel.playlists.filter { playlist ->
                playlist.id != "local_songs" &&
                        !playlist.id.startsWith("folder_") &&
                        playlist.name != "Local Songs"
            }
        }
    }

    LaunchedEffect(song?.artUri) {
        if (song?.artUri != null) {
            withContext(Dispatchers.IO) {
                try {
                    val loader = ImageLoader(context)
                    val request = ImageRequest.Builder(context)
                        .data(song.artUri)
                        .allowHardware(false)
                        .build()

                    val result = loader.execute(request)
                    if (result is SuccessResult) {
                        val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                        bitmap?.let { bmp ->
                            val palette = Palette.from(bmp).generate()
                            val rgb = palette.getDarkVibrantColor(
                                palette.getDominantColor("#230305".toColorInt())
                            )
                            dynamicAccentColor = Color(rgb).copy(alpha = 0.2f)
                        }
                    }
                } catch (_: Exception) {
                    dynamicAccentColor = defaultAccent
                }
            }
        } else {
            dynamicAccentColor = defaultAccent
        }
    }

    val currentQueue = viewModel.currentPlaybackQueue

    var isDraggingSlider by remember { mutableStateOf(false) }
    var localSliderPosition by remember { mutableFloatStateOf(0f) }
    val sliderValue = if (isDraggingSlider) localSliderPosition else viewModel.currentPosition.toFloat()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showQueueSheet by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var navigateToAboutSong by remember {mutableStateOf(false)}
    val coroutineScope = rememberCoroutineScope()
    var showPlaylistDialog by remember { mutableStateOf(false) }

    val customLightGrey = remember { Color(0xFFB3B3B3) }

    var rawOffsetY by remember { mutableFloatStateOf(0f) }
    var isGestureActive by remember { mutableStateOf(false) }

    val animatedOffset by animateIntOffsetAsState(
        targetValue = if (isGestureActive) {
            IntOffset(0, rawOffsetY.roundToInt().coerceAtLeast(0))
        } else {
            IntOffset(0, 0)
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "PlayerSpringDismiss"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { animatedOffset }
            .pointerInput(showQueueSheet) {
                if (!showQueueSheet) {
                    detectVerticalDragGestures(
                        onDragStart = { isGestureActive = true },
                        onDragEnd = {
                            isGestureActive = false
                            if (rawOffsetY > 450f) {
                                onDismiss()
                            }
                            rawOffsetY = 0f
                        },
                        onDragCancel = {
                            isGestureActive = false
                            rawOffsetY = 0f
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            rawOffsetY += if (rawOffsetY > 0) dragAmount * 0.85f else dragAmount
                        }
                    )
                }
            }
            .background(Color(0xFF080808))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        animatedAccentColor.copy(alpha = 0.70f),
                        animatedAccentColor.copy(alpha = 0.45f),
                        Color.Transparent
                    ),
                    endY = 1400f
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            //TOP DISMISS HANDLE
            Box(
                modifier = Modifier
                    .padding(top = 4.dp, bottom = 12.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color.White.copy(alpha = 0.25f))
            )

            //ALBUM ART
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .aspectRatio(1f)
                ) {
                    AsyncImage(
                        model = song?.artUri,
                        contentDescription = "Album Art",
                        modifier = Modifier
                            .fillMaxSize()
                            .shadow(
                                elevation = 32.dp,
                                shape = RoundedCornerShape(24.dp),
                                clip = false,
                                ambientColor = animatedAccentColor.copy(alpha = 0.4f),
                                spotColor = animatedAccentColor.copy(alpha = 0.6f)
                            )
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF141414)),
                        error = painterResource(id = R.drawable.default_music),
                        placeholder = painterResource(id = R.drawable.default_music),
                        contentScale = ContentScale.Crop
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .wrapContentSize()
                    ) {
                        IconButton(
                            onClick = { showOptionsMenu = true },
                            modifier = Modifier
                                .background(
                                    color = Color.Transparent,
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Song Information Options Menu",
                                tint = Color.White
                            )
                        }

                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false },
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clip(RoundedCornerShape(12.dp))
                        ) {
                            DropdownMenuItem(
                                text = { Text("About Song", fontWeight = FontWeight.Medium) },
                                onClick = {
                                    showOptionsMenu = false
                                    navigateToAboutSong = true
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Lyrics", fontWeight = FontWeight.Medium) },
                                onClick = {
                                    showOptionsMenu = false
                                    Toast.makeText(context, "Lyrics are Unavailable", Toast.LENGTH_SHORT).show()
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Add to Playlist", fontWeight = FontWeight.Medium) },
                                onClick = {
                                    showOptionsMenu = false
                                    if (song != null) {
                                        showPlaylistDialog = true
                                    }
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Download", fontWeight = FontWeight.Medium) },
                                onClick = {
                                    showOptionsMenu = false
                                    if (song != null && viewModel.isSettingsInitialized()) {
                                        coroutineScope.launch {
                                            AudioDownloader.downloadTrack(context, song, viewModel.settings)
                                        }
                                    } else if (song == null) {
                                        Toast.makeText(context, "No active track loaded to download", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }


            //TITLE & METADATA CONTENT AREA
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = song?.title ?: "No Track Playing",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = song?.artist ?: "Unknown Artist",
                    style = MaterialTheme.typography.titleMedium,
                    color = customLightGrey,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            //AUDIO TIMELINE SEEK BAR
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = sliderValue,
                    onValueChange = {
                        isDraggingSlider = true
                        localSliderPosition = it
                    },
                    onValueChangeFinished = {
                        isDraggingSlider = false
                        viewModel.seekTo(localSliderPosition.toLong())
                    },
                    valueRange = 0f..(viewModel.totalDuration.toFloat().coerceAtLeast(1f)),
                    colors = SliderDefaults.colors(
                        activeTrackColor = accentColor,
                        inactiveTrackColor = Color.White.copy(alpha = 0.12f),
                        thumbColor = accentColor
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = formatTime(sliderValue.toLong()), style = MaterialTheme.typography.bodyMedium, color = customLightGrey)
                    Text(text = formatTime(viewModel.totalDuration), style = MaterialTheme.typography.bodyMedium, color = customLightGrey)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            //CORE PLAYBACK CONTROLS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.playPrevious() },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous Track",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.width(28.dp))

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(if (viewModel.isPlaying) accentColor else Color(0xFF2A2A2A))
                        .clickable { viewModel.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (viewModel.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play Pause Toggle",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.width(28.dp))

                IconButton(
                    onClick = { viewModel.playNext() },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            //UTILITY BAR CONTROL
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PaddingValues(bottom = 8.dp, start = 12.dp, end = 12.dp)),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Loop Toggle
                IconButton(
                    onClick = { viewModel.toggleRepeatMode() },
                    modifier = Modifier.size(48.dp)
                ) {
                    val icon = if (repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat
                    val tint = when (repeatMode) {
                        RepeatMode.OFF -> Color.White.copy(alpha = 0.5f)
                        RepeatMode.ALL -> accentColor
                        RepeatMode.ONE -> accentColor
                    }
                    Icon(
                            imageVector = icon,
                    contentDescription = "Cycle Playback Loop Repeat Settings",
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                    )
                }

                // Right: Queue Sheet Button
                IconButton(onClick = { showQueueSheet = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = "Open Playback Queue Panel",
                        tint = customLightGrey,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
        //About Song
        if (navigateToAboutSong) {
            AboutSongScreen(
                song = song,
                onBackClick = { navigateToAboutSong = false }
            )
        }

        //Playlist Add Button
        if (showPlaylistDialog && song != null) {
            ModalBottomSheet(
                onDismissRequest = { showPlaylistDialog = false },
                sheetState = sheetState,
                containerColor = Color(0xFF0F0F0F),
                dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.2f)) }
            ) {
                PlaylistSelectorContent(
                    song = song,
                    playlists = userCreatedPlaylists,
                    onPlaylistSelected = { playlist ->
                        viewModel.addSongToPlaylist(playlist.id, song)
                        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                            showPlaylistDialog = false
                        }
                    },
                    onCloseClick = {
                        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                            showPlaylistDialog = false
                        }
                    },
                    modifier = Modifier.navigationBarsPadding()
                )
            }
        }

        //NATIVE STREAM TRACK QUEUE DRAWER
        if (showQueueSheet) {
            ModalBottomSheet(
                onDismissRequest = { showQueueSheet = false },
                sheetState = sheetState,
                containerColor = Color(0xFF0F0F0F),
                scrimColor = Color.Black.copy(alpha = 0.7f),
                dragHandle = {
                    BottomSheetDefaults.DragHandle(
                        color = Color.White.copy(alpha = 0.2f)
                    )
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.68f)
                        .padding(horizontal = 20.dp)
                ) {
                    Text(
                        text = "Up Next",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(bottom = 32.dp, top = 8.dp)
                    ) {
                        itemsIndexed(currentQueue, key = { index, s -> "${s.id}_queue_${index}" }) { index, queueSong ->
                            val isCurrentlyPlaying = queueSong.id == song?.id

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isCurrentlyPlaying) accentColor.copy(alpha = 0.15f) else Color.Transparent
                                    )
                                    .clickable {
                                        when {
                                            queueSong.id.startsWith("yt_") -> viewModel.playYouTubeSong(currentQueue, index)
                                            queueSong.id.all { it.isDigit() } -> viewModel.playSaavnSong(currentQueue, index)
                                            else -> viewModel.playLocalSong(currentQueue, index)
                                        }
                                        showQueueSheet = false
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = queueSong.artUri,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF1A1A1A)),
                                    error = painterResource(id = R.drawable.default_music),
                                    placeholder = painterResource(id = R.drawable.default_music),
                                    contentScale = ContentScale.Crop
                                )

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = queueSong.title,
                                        color = if (isCurrentlyPlaying) accentColor else Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isCurrentlyPlaying) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = queueSong.artist,
                                        color = customLightGrey,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodyMedium
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
