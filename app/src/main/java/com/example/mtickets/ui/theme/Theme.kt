package com.example.mtickets.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Azul80,
    onPrimary = Color(0xFF0B2A47),
    primaryContainer = AzulContenedor80,
    onPrimaryContainer = AzulSobreContenedor80,
    secondary = Gris80,
    tertiary = Naranjo80
)

private val LightColorScheme = lightColorScheme(
    primary = Azul40,
    onPrimary = Color.White,
    primaryContainer = AzulContenedor40,
    onPrimaryContainer = AzulSobreContenedor40,
    secondary = Gris40,
    tertiary = Naranjo40,
    background = Color(0xFFF7F9FC),
    surface = Color(0xFFF7F9FC)
)

@Composable
fun MTicketsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
