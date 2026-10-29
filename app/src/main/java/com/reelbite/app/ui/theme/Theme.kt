package com.reelbite.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

// Warm "spice" palette on a near-black canvas.
val Ink = Color(0xFF0D0D10)
val Charcoal = Color(0xFF15151A)
val Graphite = Color(0xFF1D1D24)
val Slate = Color(0xFF262630)
val Smoke = Color(0xFF34343F)
val Saffron = Color(0xFFFF7A45)
val Turmeric = Color(0xFFFFC14D)
val Chili = Color(0xFFE5395E)
val Plum = Color(0xFF9B4DFF)
val Mint = Color(0xFF4FD1A5)
val TextPrimary = Color(0xFFF3F3F6)
val TextSecondary = Color(0xFFA8A8B6)

/** Instagram-inspired accent gradient used for the brand title and primary actions. */
val BrandGradient = Brush.linearGradient(listOf(Turmeric, Saffron, Chili, Plum))
val BrandGradientHorizontal = Brush.horizontalGradient(listOf(Saffron, Chili, Plum))

private val DarkColors = darkColorScheme(
    primary = Saffron,
    onPrimary = Color(0xFF1C0D06),
    primaryContainer = Color(0xFF3B2116),
    onPrimaryContainer = Color(0xFFFFD6C4),
    secondary = Turmeric,
    onSecondary = Color(0xFF231700),
    secondaryContainer = Color(0xFF3A2D10),
    onSecondaryContainer = Color(0xFFFFE3A8),
    tertiary = Chili,
    onTertiary = Color.White,
    background = Ink,
    onBackground = TextPrimary,
    surface = Charcoal,
    onSurface = TextPrimary,
    surfaceVariant = Graphite,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = Ink,
    surfaceContainerLow = Charcoal,
    surfaceContainer = Graphite,
    surfaceContainerHigh = Slate,
    surfaceContainerHighest = Smoke,
    outline = Color(0xFF4A4A57),
    outlineVariant = Color(0xFF2E2E38),
    error = Color(0xFFFF6B7A),
    onError = Color(0xFF2B0007),
    inverseSurface = TextPrimary,
    inverseOnSurface = Ink,
)

private val AppTypography = Typography().run {
    copy(
        displaySmall = displaySmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold),
        bodyMedium = bodyMedium.copy(lineHeight = 20.sp),
    )
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** The app is dark-first by design, so the dark scheme is always used. */
@Composable
fun ReelBiteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
