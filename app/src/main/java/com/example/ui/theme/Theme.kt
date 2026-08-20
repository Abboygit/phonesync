package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = VibrantBlueDark,
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF0369A1),
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = Color(0xFFA78BFA),
    background = CanvasBackgroundDark,
    surface = CardSurfaceDark,
    surfaceVariant = Color(0xFF334155),
    error = Color(0xFFF87171)
)

private val LightColorScheme = lightColorScheme(
    primary = VibrantBlue,
    onPrimary = Color.White,
    primaryContainer = VibrantBlueHero,
    onPrimaryContainer = VibrantBlueHeroSub,
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF075985),
    tertiary = Color(0xFF7C3AED),
    background = CanvasBackgroundLight,
    surface = CardSurfaceLight,
    surfaceVariant = BorderSubtleLight,
    error = AlertRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
