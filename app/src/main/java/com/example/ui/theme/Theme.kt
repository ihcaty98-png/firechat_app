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
    primary = FirePurplePrimaryDark,
    onPrimary = Color.White,
    secondary = FirePurpleSecondary,
    onSecondary = Color.White,
    background = FireBackgroundDark,
    onBackground = FireTextPrimaryDark,
    surface = FireSurfaceDark,
    onSurface = FireTextPrimaryDark,
    surfaceVariant = FireSurfaceDarkCard,
    onSurfaceVariant = FireTextSecondaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = FirePurplePrimary,
    onPrimary = Color.White,
    secondary = FirePurpleSecondary,
    onSecondary = Color.White,
    background = FireBackgroundLight,
    onBackground = FireTextPrimaryLight,
    surface = FireSurfaceLight,
    onSurface = FireTextPrimaryLight,
    surfaceVariant = FirePurpleLight,
    onSurfaceVariant = FireTextSecondaryLight
)

@Composable
fun FireonlineChatiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to false to preserve the signature purple-white visual identity
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

// Backward compatibility alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) = FireonlineChatiTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
