package com.sumit.muzixx.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sumit.muzixx.R
import com.sumit.muzixx.data.Song

@Composable
fun MiniPlayer(
    song: Song?,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onMiniPlayerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (song == null) return

    val accentColor = MaterialTheme.colorScheme.primary
    val containerBgColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f)
    val onContainerColor = MaterialTheme.colorScheme.onSurface
    val onContainerVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier
            .padding(horizontal = 14.dp)
            .fillMaxWidth()
            .shadow(
                elevation = 24.dp,
                shape = RoundedCornerShape(20.dp),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.2f),
                spotColor = Color.Black.copy(alpha = 0.35f)
            ),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        border = BorderStroke(width = 1.dp, color = onContainerColor.copy(alpha = 0.08f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(containerBgColor)
                .clickable { onMiniPlayerClick() }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = song.artUri,
                contentDescription = "Mini Player Album Art",
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(onContainerColor.copy(alpha = 0.1f)),
                error = painterResource(id = R.drawable.default_music),
                placeholder = painterResource(id = R.drawable.default_music),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = onContainerColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = onContainerVariantColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isPlaying) accentColor else onContainerColor.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onPlayPause) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "Play Pause",
                        tint = if (isPlaying) MaterialTheme.colorScheme.onPrimary else accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}