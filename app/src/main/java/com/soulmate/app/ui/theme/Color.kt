package com.soulmate.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val White = Color(0xFFFFFFFF)
val Black = Color(0xFF000000)

val customGradient = Brush.horizontalGradient(
    0.0f to Color(0xFF2A7B9B),
    0.5f to Color(0xFF57C785),
    1.0f to Color(0xFFEDDD53)
)

val CommunityTick = Color(0xFF1DA1F2)

val PrimaryGreen = Color(0xFF2CA148)
val PrimaryGreenLight = Color(0xFFE5EDFF)
val BackgroundMain = Color(0xFFFAF9FF)
val BackgroundLight = Color(0xFFE0E8FF)
val TextPrimary = Color(0xFF282C35)
val TextSecondary = Color(0xFF717784)

// Light Colors
val LightBackground = Color(0xFFF8F9FA)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF000000)
val LightOnSurfaceVariant = Color(0xFF717784)
val BrandOrange = Color(0xFFf09a37)
val BrandYellow = Color(0xfffccd51)

// Dark Colors
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val DarkOnSurface = Color(0xFFFFFFFF)
val DarkOnSurfaceVariant = Color(0xFFB0B0B0)
val DarkError = Color(0xFFFF4444)

val MoodColors = listOf(
    Color(0xFF4CAF50),
    Color(0xFF03A9F4),
    Color(0xFFFFC107),
    Color(0xFF9E9E9E),
    Color(0xFFF44336)
)
