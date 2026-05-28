package com.sumit.muzixx.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MuzixColorScheme = darkColorScheme(

    primary = NeonRed,
    onPrimary = Color.White,

    background = Color.Black,
    onBackground = Color.White,

    surface = Color.Black,
    onSurface = Color.White,

    surfaceVariant = Color(0xFF121212),
    onSurfaceVariant = Color.White,

    secondary = NeonRed,
    tertiary = NeonRed,

    scrim = Color.Black.copy(alpha = 0.72f)
)

@Composable
fun MuzixXTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MuzixColorScheme,
        typography = Typography,
        content = content
    )
}