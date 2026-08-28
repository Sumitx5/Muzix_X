package com.sumit.muzixx.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.glassEffect(
    shape: Shape,
    elevation: Dp = 12.dp,
    isDarkTheme: Boolean = isSystemInDarkTheme()
): Modifier {
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainer
    val borderColor = MaterialTheme.colorScheme.onSurface
    val shadowAmbient = if (isDarkTheme) Color.Black.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.08f)
    val shadowSpot = if (isDarkTheme) Color.Black.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.12f)
    val highlightTop = if (isDarkTheme) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.35f)
    val highlightBottom = if (isDarkTheme) Color.Black.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.03f)

    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
            ambientColor = shadowAmbient,
            spotColor = shadowSpot
        )
        .clip(shape)
        .background(
            Brush.verticalGradient(
                colors = listOf(
                    surfaceColor.copy(alpha = if (isDarkTheme) 0.75f else 0.85f),
                    surfaceColor.copy(alpha = if (isDarkTheme) 0.60f else 0.70f),
                    surfaceColor.copy(alpha = if (isDarkTheme) 0.70f else 0.80f)
                )
            )
        )
        .background(
            Brush.linearGradient(
                colors = listOf(
                    highlightTop,
                    Color.White.copy(alpha = 0.03f),
                    highlightBottom
                )
            )
        )
        .border(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    borderColor.copy(alpha = if (isDarkTheme) 0.35f else 0.20f),
                    borderColor.copy(alpha = if (isDarkTheme) 0.12f else 0.08f),
                    borderColor.copy(alpha = if (isDarkTheme) 0.04f else 0.02f)
                )
            ),
            shape = shape
        )
}