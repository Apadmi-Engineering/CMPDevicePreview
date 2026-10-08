package com.apadmi.sample

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF00696D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9CF1F5),
    onPrimaryContainer = Color(0xFF002021),
    secondary = Color(0xFF8B5000),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDCBE),
    onSecondaryContainer = Color(0xFF2C1600),
    tertiary = Color(0xFFB4234B),
    background = Color(0xFFF6FAFA),
    onBackground = Color(0xFF171D1D),
    surface = Color(0xFFF6FAFA),
    onSurface = Color(0xFF171D1D),
    surfaceVariant = Color(0xFFDAE4E5),
    onSurfaceVariant = Color(0xFF3F4949),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF0F4F4),
    surfaceContainer = Color(0xFFEAEFEF),
    surfaceContainerHigh = Color(0xFFE4E9E9),
    surfaceContainerHighest = Color(0xFFDFE3E3),
    outline = Color(0xFF6F7979),
    outlineVariant = Color(0xFFBEC8C9),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF80D4D8),
    onPrimary = Color(0xFF003738),
    primaryContainer = Color(0xFF004F52),
    onPrimaryContainer = Color(0xFF9CF1F5),
    secondary = Color(0xFFFFB870),
    onSecondary = Color(0xFF4A2800),
    secondaryContainer = Color(0xFF6A3C00),
    onSecondaryContainer = Color(0xFFFFDCBE),
    tertiary = Color(0xFFFFB2BE),
    background = Color(0xFF0F1415),
    onBackground = Color(0xFFDEE3E3),
    surface = Color(0xFF0F1415),
    onSurface = Color(0xFFDEE3E3),
    surfaceVariant = Color(0xFF3F4949),
    onSurfaceVariant = Color(0xFFBEC8C9),
    surfaceContainerLowest = Color(0xFF0A0F10),
    surfaceContainerLow = Color(0xFF171D1D),
    surfaceContainer = Color(0xFF1B2121),
    surfaceContainerHigh = Color(0xFF252B2B),
    surfaceContainerHighest = Color(0xFF303636),
    outline = Color(0xFF899393),
    outlineVariant = Color(0xFF3F4949),
)

/** Gold for star ratings, in either theme. */
val RatingColor = Color(0xFFFFB300)

@Composable
fun WanderTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors, content = content)
}
