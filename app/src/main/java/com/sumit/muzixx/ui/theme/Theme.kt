package com.sumit.muzixx.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.sumit.muzixx.viewmodel.MusicViewModel

@Composable
fun MuzixXTheme(
    viewModel: MusicViewModel,
    content: @Composable () -> Unit
) {
    val activeAccent = if (viewModel.isSettingsInitialized()) {
        when (viewModel.settings.appTheme) {
            "Electric Blue / Cyan" -> ElectricCyan
            "Lime Green"          -> LimeGreen
            "Vibrant Yellow"       -> VibrantYellow
            "Neon Pink / Magenta"  -> NeonPink
            "Bright Orange"        -> BrightOrange
            "Neon Red"        -> NeonRed
            else                   -> DefaultRed
        }
    } else {
        DefaultRed
    }
    val dynamicColorScheme = darkColorScheme(
        primary = activeAccent,
        onPrimary = Color.White,

        background = Color.Black,
        onBackground = Color.White,

        surface = Color.Black,
        onSurface = Color.White,

        surfaceVariant = DarkGray,
        onSurfaceVariant = LightGray,

        secondary = activeAccent,
        tertiary = activeAccent,

        scrim = Color.Black.copy(alpha = 0.72f)
    )

    MaterialTheme(
        colorScheme = dynamicColorScheme,
        typography = Typography,
        content = content
    )
}