package com.sumit.muzixx.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MuzixColorScheme = darkColorScheme(
    primary = NeonRed,
    background = DeepBlack,
    surface = DarkGray,
    onBackground = Color.White,
    onSurface = Color.White
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