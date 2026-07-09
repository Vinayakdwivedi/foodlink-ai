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
    primary = OliveLight,
    onPrimary = Color.White,
    primaryContainer = OliveDark,
    onPrimaryContainer = MintSoft,
    secondary = SoftGold,
    onSecondary = CharcoalDark,
    secondaryContainer = ForestCard,
    onSecondaryContainer = SoftGoldLight,
    tertiary = SoftGoldLight,
    onTertiary = CharcoalDark,
    background = CharcoalDark,
    onBackground = Color.White,
    surface = ForestCard,
    onSurface = Color.White,
    surfaceVariant = OliveDark,
    onSurfaceVariant = MintSoft
)

private val LightColorScheme = lightColorScheme(
    primary = OliveMain,
    onPrimary = Color.White,
    primaryContainer = MintSoft,
    onPrimaryContainer = OliveDark,
    secondary = SoftGold,
    onSecondary = CharcoalDark,
    secondaryContainer = LightGreenBg,
    onSecondaryContainer = OliveDark,
    tertiary = OliveLight,
    onTertiary = Color.White,
    background = LightGreenBg,
    onBackground = CharcoalDark,
    surface = Color.White,
    onSurface = CharcoalDark,
    surfaceVariant = MintSoft,
    onSurfaceVariant = OliveDark
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to false to force our custom nature theme branding
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

