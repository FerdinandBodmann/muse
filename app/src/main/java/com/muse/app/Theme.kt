package com.muse.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Color(0xFFC96442),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF3DDD2),
    onPrimaryContainer = Color(0xFF5A2412),
    secondary = Color(0xFF6B675E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEBE8DE),
    onSecondaryContainer = Color(0xFF1F1E1D),
    background = Color(0xFFF5F4EE),
    onBackground = Color(0xFF1F1E1D),
    surface = Color(0xFFFAF9F5),
    onSurface = Color(0xFF1F1E1D),
    surfaceVariant = Color(0xFFEBE8DE),
    onSurfaceVariant = Color(0xFF6B675E),
    outline = Color(0xFFD3CFC2),
    outlineVariant = Color(0xFFE6E2D6),
    error = Color(0xFFB3402A),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFAF9F5),
    surfaceContainer = Color(0xFFF0EEE6),
    surfaceContainerHigh = Color(0xFFEBE8DE),
    surfaceContainerHighest = Color(0xFFE6E2D6),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFD97757),
    onPrimary = Color(0xFF1F1E1D),
    primaryContainer = Color(0xFF5A2E1F),
    onPrimaryContainer = Color(0xFFF3DDD2),
    secondary = Color(0xFFB5B1A5),
    onSecondary = Color(0xFF1F1E1D),
    secondaryContainer = Color(0xFF3A3935),
    onSecondaryContainer = Color(0xFFECEAE2),
    background = Color(0xFF262624),
    onBackground = Color(0xFFECEAE2),
    surface = Color(0xFF2E2D2B),
    onSurface = Color(0xFFECEAE2),
    surfaceVariant = Color(0xFF3A3935),
    onSurfaceVariant = Color(0xFFB5B1A5),
    outline = Color(0xFF55534C),
    outlineVariant = Color(0xFF45433E),
    error = Color(0xFFE07A62),
    surfaceContainerLowest = Color(0xFF1F1E1D),
    surfaceContainerLow = Color(0xFF2B2A28),
    surfaceContainer = Color(0xFF30302E),
    surfaceContainerHigh = Color(0xFF363532),
    surfaceContainerHighest = Color(0xFF3D3C38),
)

private val MuseShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun MuseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        shapes = MuseShapes,
        content = content,
    )
}
