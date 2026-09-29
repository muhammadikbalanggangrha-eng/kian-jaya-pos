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
    primary = PosTealLight,
    onPrimary = Color(0xFF003831),
    primaryContainer = PosTealDark,
    onPrimaryContainer = PosTealContainer,
    secondary = PosAmber,
    onSecondary = Color(0xFF452B00),
    secondaryContainer = Color(0xFF633F00),
    onSecondaryContainer = PosAmberContainer,
    tertiary = PosEmerald,
    background = PosBackgroundDark,
    surface = PosSurfaceDark,
    surfaceVariant = PosSurfaceVariantDark,
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF1F5F9),
    error = PosRose,
    errorContainer = Color(0xFF93000A)
)

private val LightColorScheme = lightColorScheme(
    primary = PosTealPrimary,
    onPrimary = Color.White,
    primaryContainer = PosTealContainer,
    onPrimaryContainer = Color(0xFF00201C),
    secondary = PosNavy,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E8F0),
    onSecondaryContainer = PosNavyDark,
    tertiary = PosEmerald,
    background = PosBackgroundLight,
    surface = PosSurfaceLight,
    surfaceVariant = PosSurfaceVariantLight,
    onBackground = PosNavyDark,
    onSurface = PosNavyDark,
    error = PosRose,
    errorContainer = PosRoseContainer
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set false to prioritize custom POS branding
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
