package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light Mode Color Scheme (matches Light Theme in mockups)
private val LightColorSchemeBase = lightColorScheme(
    primary = Color(0xFF10B981), // Emerald default
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF065F46),
    secondary = Color(0xFF10B981),
    onSecondary = Color.White,
    background = Color(0xFFF8FAFC), // Warm off-white
    onBackground = Color(0xFF0F172A), // Dark slate text
    surface = Color.White, // Pure white cards
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9), // Very light gray borders/inputs
    onSurfaceVariant = Color(0xFF64748B), // Muted text
    outline = Color(0xFFE2E8F0) // Subtle card borders
)

// Dark Mode Color Scheme (matches Dark Theme in mockups)
private val DarkColorSchemeBase = darkColorScheme(
    primary = Color(0xFF10B981), // Emerald default
    onPrimary = Color.White,
    primaryContainer = Color(0xFF065F46),
    onPrimaryContainer = Color(0xFFD1FAE5),
    secondary = Color(0xFF10B981),
    onSecondary = Color.White,
    background = Color(0xFF090D16), // Dark slate/black
    onBackground = Color(0xFFF8FAFC), // Off-white text
    surface = Color(0xFF131C2E), // Deep dark cards
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1E293B), // Dark slate borders/inputs
    onSurfaceVariant = Color(0xFF94A3B8), // Muted text
    outline = Color(0xFF1E293B) // Dark border stroke
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    accentColor: Color = Color(0xFF10B981),
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

