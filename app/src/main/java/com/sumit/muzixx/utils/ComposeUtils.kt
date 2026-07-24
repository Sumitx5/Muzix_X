package com.sumit.muzixx.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.glassEffect(shape: Shape): Modifier {
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainer
    val borderColor = MaterialTheme.colorScheme.onSurface

    return this
        .shadow(
            elevation = 16.dp,
            shape = shape,
            clip = false,
            ambientColor = Color.Black.copy(alpha = 0.25f),
            spotColor = Color.Black.copy(alpha = 0.4f)
        )
        .clip(shape)
        .background(
            Brush.verticalGradient(
                colors = listOf(
                    surfaceColor.copy(alpha = 0.86f),
                    surfaceColor.copy(alpha = 0.82f),
                    surfaceColor.copy(alpha = 0.84f)
                )
            )
        )
        .background(
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.05f),
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.03f)
                )
            )
        )
        .border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    borderColor.copy(alpha = 0.22f),
                    borderColor.copy(alpha = 0.06f)
                )
            ),
            shape = shape
        )
}