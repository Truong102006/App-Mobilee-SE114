package com.soulmate.app.ui.theme

import android.annotation.SuppressLint
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme as MaterialTheme3

@SuppressLint("ConflictingOnColor")
private val LightColorPalette = lightColors(
    primary = BrandOrange,
    primaryVariant = BrandYellow,
    secondary = BrandYellow,
    background = LightBackground,
    surface = LightSurface,
    onPrimary = White,
    onSecondary = Black,
    onBackground = Color.Black,
    onSurface = Color.Black
)

@SuppressLint("ConflictingOnColor")
private val DarkColorPalette = darkColors(
    primary = BrandYellow,
    primaryVariant = BrandOrange,
    secondary = BrandOrange,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = Black,
    onSecondary = White,
    onBackground = Color.White,
    onSurface = Color.White,
    error = DarkError
)

private val LightColorScheme3 = lightColorScheme(
    primary = BrandOrange,
    secondary = BrandYellow,
    tertiary = PrimaryGreen,
    background = LightBackground,
    surface = LightSurface,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFF2F4F7)
)

private val DarkColorScheme3 = darkColorScheme(
    primary = BrandYellow,
    secondary = BrandOrange,
    tertiary = PrimaryGreen,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2C2C2E)
)

@Composable
fun SoulMateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorPalette else LightColorPalette
    val colorScheme3 = if (darkTheme) DarkColorScheme3 else LightColorScheme3

    MaterialTheme3(
        colorScheme = colorScheme3
    ) {
        MaterialTheme(
            colors = colors,
            typography = Typography,
            content = content
        )
    }
}
