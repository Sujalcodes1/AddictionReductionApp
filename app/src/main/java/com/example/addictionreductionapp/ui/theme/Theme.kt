package com.example.addictionreductionapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RegainDarkColorScheme = darkColorScheme(
    primary = RegainTeal,
    onPrimary = Color.Black,
    primaryContainer = RegainTealDark,
    onPrimaryContainer = RegainTealLight,
    secondary = RegainPurple,
    onSecondary = Color.White,
    secondaryContainer = DarkCardLight,
    onSecondaryContainer = Color.White,
    tertiary = RegainBlue,
    background = DarkBackground,
    onBackground = TextWhite,
    surface = DarkSurface,
    onSurface = TextWhite,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextGray,
    outline = DarkCardLight,
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun RegainTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RegainDarkColorScheme,
        typography = Typography,
        content = content
    )
}