package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = RoseGold,
    secondary = VioletWine,
    tertiary = AmberGlow,
    background = ArtisticDark,
    surface = ArtisticCard,
    onPrimary = ArtisticDark,
    onSecondary = ArtisticDark,
    onBackground = LightArtSurface,
    onSurface = LightArtSurface
)

private val LightColorScheme = lightColorScheme(
    primary = RoseGold,
    secondary = VioletWine,
    tertiary = AmberGlow,
    background = LightArtBackground,
    surface = LightArtSurface,
    onPrimary = LightArtSurface,
    onSecondary = LightArtSurface,
    onBackground = ArtisticDark,
    onSurface = ArtisticDark
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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
