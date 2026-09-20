package com.sumit.muzixx.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sumit.muzixx.data.network.SongLyrics
import com.sumit.muzixx.utils.glassEffect
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

enum class LyricsDisplayMode {
    FULLSCREEN,
    CARD,
    MINI
}

@Composable
fun LyricsView(
    lyrics: SongLyrics?,
    currentPositionMs: Long,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    displayMode: LyricsDisplayMode = LyricsDisplayMode.FULLSCREEN,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onSeekTo: ((Long) -> Unit)? = null
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            lyrics == null -> {
                Text(
                    text = "No lyrics available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }

            lyrics.isInstrumental -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Instrumental Track",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            lyrics.syncedLyrics.isNotEmpty() -> {
                val activeIndex = remember(currentPositionMs, lyrics.syncedLyrics) {
                    val idx = lyrics.syncedLyrics.indexOfLast { it.timeMs <= currentPositionMs }
                    if (idx == -1) 0 else idx
                }

                when (displayMode) {
                    LyricsDisplayMode.FULLSCREEN -> {
                        FullscreenSyncedLyrics(
                            lyrics = lyrics,
                            activeIndex = activeIndex,
                            onSeekTo = onSeekTo
                        )
                    }
                    LyricsDisplayMode.CARD -> {
                        CardLyricsSnippet(
                            lyrics = lyrics,
                            activeIndex = activeIndex,
                            accentColor = accentColor,
                            onSeekTo = onSeekTo
                        )
                    }
                    LyricsDisplayMode.MINI -> {
                        val currentText = lyrics.syncedLyrics.getOrNull(activeIndex)?.text
                        MiniLyricsTicker(
                            activeLineText = if (currentText.isNullOrBlank()) "♪ ♪ ♪" else currentText
                        )
                    }
                }
            }

            !lyrics.plainLyrics.isNullOrBlank() -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = lyrics.plainLyrics,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            lineHeight = 28.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FullscreenSyncedLyrics(
    lyrics: SongLyrics,
    activeIndex: Int,
    onSeekTo: ((Long) -> Unit)?
) {
    val listState = rememberLazyListState()
    val isUserDragging by listState.interactionSource.collectIsDraggedAsState()
    var isUserInteracting by remember { mutableStateOf(false) }

    LaunchedEffect(isUserDragging) {
        if (isUserDragging) {
            isUserInteracting = true
        } else if (isUserInteracting) {
            delay(3500.milliseconds)
            isUserInteracting = false
        }
    }

    LaunchedEffect(activeIndex, isUserInteracting) {
        if (!isUserInteracting && activeIndex >= 0) {
            listState.animateScrollToItem((activeIndex - 2).coerceAtLeast(0))
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 140.dp, horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        itemsIndexed(lyrics.syncedLyrics) { index, line ->
            val isCurrent = index == activeIndex
            val displayText = line.text.ifBlank { "♪ ♪ ♪" }

            val textColor by animateColorAsState(
                targetValue = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                label = "LyricTextColor"
            )

            val textScale by animateFloatAsState(
                targetValue = if (isCurrent) 1.06f else 0.96f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
                label = "LyricScale"
            )

            Text(
                text = displayText,
                fontSize = if (isCurrent) 22.sp else 18.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                color = textColor,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(textScale)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(enabled = onSeekTo != null) { onSeekTo?.invoke(line.timeMs) }
                    .padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun CardLyricsSnippet(
    lyrics: SongLyrics,
    activeIndex: Int,
    onSeekTo: ((Long) -> Unit)?,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    val beforePrevLine = lyrics.syncedLyrics.getOrNull(activeIndex - 2)
    val prevLine = lyrics.syncedLyrics.getOrNull(activeIndex - 1)
    val currentLine = lyrics.syncedLyrics.getOrNull(activeIndex)
    val nextLine = lyrics.syncedLyrics.getOrNull(activeIndex + 1)
    val afterNextLine = lyrics.syncedLyrics.getOrNull(activeIndex + 2)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .glassEffect(RoundedCornerShape(24.dp))
            .padding(20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            if (beforePrevLine != null) {
                Text(
                    text = beforePrevLine.text.ifBlank { "♪ ♪ ♪" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (prevLine != null) {
                Text(
                    text = prevLine.text.ifBlank { "♪ ♪ ♪" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Check if current line exists AND has non-blank text
            if (currentLine != null && currentLine.text.isNotBlank()) {
                Text(
                    text = currentLine.text,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    lineHeight = 30.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable(enabled = onSeekTo != null) {
                        onSeekTo?.invoke(currentLine.timeMs)
                    }
                )
            } else {
                Text(
                    text = "♪ ♪ ♪",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    lineHeight = 30.sp,
                    modifier = Modifier.clickable(enabled = onSeekTo != null && currentLine != null) {
                        currentLine?.let { onSeekTo?.invoke(it.timeMs) }
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (nextLine != null) {
                Text(
                    text = nextLine.text.ifBlank { "♪ ♪ ♪" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (afterNextLine != null) {
                Text(
                    text = afterNextLine.text.ifBlank { "♪ ♪ ♪" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun MiniLyricsTicker(activeLineText: String) {
    AnimatedContent(
        targetState = activeLineText,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "MiniLyricAnimation"
    ) { text ->
        Text(
            text = text.ifBlank { "♪ ♪ ♪" },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}