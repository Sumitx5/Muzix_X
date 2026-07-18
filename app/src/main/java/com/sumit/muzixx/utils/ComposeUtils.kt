package com.sumit.muzixx.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.glassEffect(shape: Shape): Modifier {
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainer
    val borderColor = MaterialTheme.colorScheme.onSurface

    return this
        .clip(shape)
        .background(
            Brush.verticalGradient(
                colors = listOf(
                    surfaceColor.copy(alpha = 0.85f),
                    surfaceColor.copy(alpha = 0.64f)
                )
            )
        )
        .blur(radius = 24.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
        .border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    borderColor.copy(alpha = 0.12f),
                    borderColor.copy(alpha = 0.04f)
                )
            ),
            shape = shape
        )
}