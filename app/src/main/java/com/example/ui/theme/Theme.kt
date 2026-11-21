package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val XShoppingDarkColorScheme = darkColorScheme(
    primary = BrandOrange,
    onPrimary = PureWhite,
    primaryContainer = BrandOrangeContainerDark,
    onPrimaryContainer = BrandOrangeContainer,
    secondary = PureWhite,
    onSecondary = ObsidianBlack,
    secondaryContainer = CharcoalCard,
    onSecondaryContainer = PureWhite,
    tertiary = VerifiedEmerald,
    onTertiary = PureWhite,
    background = ObsidianBlack,
    onBackground = PureWhite,
    surface = CharcoalSurface,
    onSurface = PureWhite,
    surfaceVariant = CharcoalCard,
    onSurfaceVariant = MutedSlateDark,
    outline = CharcoalBorder,
    error = DangerRed,
    onError = PureWhite
)

private val XShoppingLightColorScheme = lightColorScheme(
    primary = BrandOrange,
    onPrimary = PureWhite,
    primaryContainer = BrandOrangeContainer,
    onPrimaryContainer = BrandOrangeDark,
    secondary = ObsidianBlack,
    onSecondary = PureWhite,
    secondaryContainer = CloudBackground,
    onSecondaryContainer = ObsidianBlack,
    tertiary = VerifiedEmerald,
    onTertiary = PureWhite,
    background = CloudBackground,
    onBackground = ObsidianBlack,
    surface = PureWhite,
    onSurface = ObsidianBlack,
    surfaceVariant = CloudBackground,
    onSurfaceVariant = MutedSlate,
    outline = LightBorder,
    error = DangerRed,
    onError = PureWhite
)

val XShoppingShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun XShoppingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) XShoppingDarkColorScheme else XShoppingLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = XShoppingShapes,
        content = content
    )
}
