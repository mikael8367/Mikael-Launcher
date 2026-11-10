package com.mikael.launcher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LauncherColors = darkColorScheme(
    primary = PrimaryGreen,
    onPrimary = Color(0xFF062116),
    secondary = AccentBlue,
    tertiary = AccentPurple,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceRaised,
    onSurfaceVariant = TextSecondary,
    outline = OutlineSoft
)

@Composable
fun MikaelLauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LauncherColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
