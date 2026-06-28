package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Light Mode Color Scheme (matches Activity Tracker design)
private val LightColorSchemeBase = lightColorScheme(
    primary = PrimaryAccent,
    onPrimary = SurfaceLight,
    primaryContainer = PrimaryAccent.copy(alpha = 0.1f),
    onPrimaryContainer = PrimaryAccent,
    secondary = SuccessGreen,
    onSecondary = SurfaceLight,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = BackgroundLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = OutlineLight
)

// Dark Mode Color Scheme (matches Activity Tracker design)
private val DarkColorSchemeBase = darkColorScheme(
    primary = PrimaryAccent,
    onPrimary = SurfaceLight,
    primaryContainer = PrimaryAccent.copy(alpha = 0.2f),
    onPrimaryContainer = SurfaceLight,
    secondary = SuccessGreen,
    onSecondary = SurfaceLight,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = BackgroundDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = OutlineDark
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Changed default to false to match image
    accentColor: androidx.compose.ui.graphics.Color = PrimaryAccent,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) {
        DarkColorSchemeBase.copy(
            primary = accentColor,
            secondary = accentColor
        )
    } else {
        LightColorSchemeBase.copy(
            primary = accentColor,
            secondary = accentColor
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

