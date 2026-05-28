package com.sumit.muzixx.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sumit.muzixx.R
import com.sumit.muzixx.viewmodel.MusicViewModel
import com.sumit.muzixx.utils.formatTime
import kotlin.math.roundToInt

@Suppress("AssignedValueIsNeverRead")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayerScreen(
    viewModel: MusicViewModel,
    onDismiss: () -> Unit
) {
    val song = viewModel.selectedSong

    // Automatically resolve the active playing playlist content source context
    val currentQueue = remember(song, viewModel.saavnTrendingSongs, viewModel.saavnNewReleases, viewModel.saavnSearchResults, viewModel.songs) {
        when {
            viewModel.saavnTrendingSongs.any { it.id == song?.id } -> viewModel.saavnTrendingSongs
            viewModel.saavnNewReleases.any { it.id == song?.id } -> viewModel.saavnNewReleases
            viewModel.saavnSearchResults.any { it.id == song?.id } -> viewModel.saavnSearchResults
            else -> viewModel.songs
        }
    }

    var isDraggingSlider by remember { mutableStateOf(false) }
    var localSliderPosition by remember { mutableStateOf(0f) }
    val sliderValue = if (isDraggingSlider) localSliderPosition else viewModel.currentPosition.toFloat()

    // Material 3 Sheet State configuration
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showQueueSheet by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }

    val customRed = remember { Color(0xFFE50914) }
    val customGrey = remember { Color(0xFF1E1E1E) }
    val customLightGrey = remember { Color(0xFFB3B3B3) }

    // Gesture tracking variables
    var rawOffsetY by remember { mutableStateOf(0f) }
    var isGestureActive by remember { mutableStateOf(false) }

    // SPRING DRAG INTERPOLATION: Snaps back or drags down with fluid elastic physics
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
            .background(MaterialTheme.colorScheme.background)
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
            // ================= TOP BAR WITH DRAG HANDLE =================
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp, bottom = 12.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        IconButton(onClick = { showOptionsMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false },
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            DropdownMenuItem(
                                text = { Text("Add to Playlist") },
                                onClick = { showOptionsMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("About Song") },
                                onClick = { showOptionsMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Download") },
                                onClick = { showOptionsMenu = false }
                            )
                        }
                    }
                }
            }

            // ================= ALBUM ART =================
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = song?.artUri,
                    contentDescription = "Album Art",
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .aspectRatio(1f)
                        .shadow(elevation = 24.dp, shape = RoundedCornerShape(28.dp), ambientColor = customRed.copy(alpha = 0.25f))
                        .clip(RoundedCornerShape(28.dp))
                        .background(customGrey),
                    error = painterResource(id = R.drawable.default_music),
                    placeholder = painterResource(id = R.drawable.default_music),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ================= SONG INFO =================
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = song?.title ?: "No Track Playing",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = song?.artist ?: "Unknown Artist",
                    style = MaterialTheme.typography.bodyLarge,
                    color = customLightGrey,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ================= SEEK BAR =================
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
                        activeTrackColor = customRed,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        thumbColor = customRed
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = formatTime(sliderValue.toLong()), style = MaterialTheme.typography.bodyMedium, color = customLightGrey)
                    Text(text = formatTime(viewModel.totalDuration), style = MaterialTheme.typography.bodyMedium, color = customLightGrey)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ================= CONTROLS =================
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isRepeatOneEnabled = false

                IconButton(onClick = { /* Implement repeat toggle routing if needed */ }) {
                    Icon(
                        imageVector = if (isRepeatOneEnabled) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = "Repeat",
                        tint = customLightGrey,
                        modifier = Modifier.size(28.dp)
                    )
                }

                IconButton(onClick = { viewModel.playPrevious() }) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(42.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (viewModel.isPlaying) customRed else customGrey)
                        .clickable { viewModel.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (viewModel.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play Pause",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(onClick = { viewModel.playNext() }) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(42.dp)
                    )
                }

                IconButton(onClick = { showQueueSheet = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = "Queue",
                        tint = customLightGrey,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }

        // ================= NATIVE MODAL QUEUE PANEL =================
        if (showQueueSheet) {
            ModalBottomSheet(
                onDismissRequest = { showQueueSheet = false },
                sheetState = sheetState,
                containerColor = Color(0xFF121212),
                scrimColor = Color.Black.copy(alpha = 0.6f),
                dragHandle = {
                    BottomSheetDefaults.DragHandle(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.65f)
                        .padding(horizontal = 20.dp)
                ) {
                    Text(
                        text = "Up Next",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        itemsIndexed(currentQueue) { index, queueSong ->
                            val isCurrentlyPlaying = queueSong.id == song?.id

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isCurrentlyPlaying) customRed.copy(alpha = 0.12f) else Color.Transparent
                                    )
                                    .clickable {
                                        // 💡 FIXED: Dynamically matches the queue item source platform ID
                                        // and routes it cleanly to the explicit single player functions.
                                        when {
                                            queueSong.id.startsWith("yt_") -> {
                                                viewModel.playYouTubeSong(currentQueue, index)
                                            }
                                            queueSong.id.all { it.isDigit() } -> {
                                                viewModel.playSaavnSong(currentQueue, index)
                                            }
                                            else -> {
                                                viewModel.playLocalSong(currentQueue, index)
                                            }
                                        }
                                        showQueueSheet = false
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = queueSong.artUri,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    error = painterResource(id = R.drawable.default_music),
                                    placeholder = painterResource(id = R.drawable.default_music),
                                    contentScale = ContentScale.Crop
                                )

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = queueSong.title,
                                        color = if (isCurrentlyPlaying) customRed else Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isCurrentlyPlaying) FontWeight.Bold else FontWeight.Normal
                                    )

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