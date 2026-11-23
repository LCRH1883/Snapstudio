package com.snapswipe.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.snapswipe.app.data.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD6D6D6),
    onPrimary = Color(0xFF232323),
    primaryContainer = Color(0xFF424242),
    onPrimaryContainer = Color(0xFFF5F5F5),
    secondary = Color(0xFFC6C6C6),
    onSecondary = Color(0xFF292929),
    secondaryContainer = Color(0xFF3A3A3A),
    onSecondaryContainer = Color(0xFFECECEC),
    tertiary = Color(0xFFCBCBCB),
    onTertiary = Color(0xFF2C2C2C),
    tertiaryContainer = Color(0xFF404040),
    onTertiaryContainer = Color(0xFFEFEFEF),
    background = Color(0xFF101010),
    onBackground = Color(0xFFF2F2F2),
    surface = Color(0xFF181818),
    onSurface = Color(0xFFF2F2F2),
    surfaceVariant = Color(0xFF303030),
    onSurfaceVariant = Color(0xFFC9C9C9),
    surfaceTint = Color(0xFF9C9C9C),
    inverseSurface = Color(0xFFEAEAEA),
    inverseOnSurface = Color(0xFF222222),
    inversePrimary = Color(0xFF484848),
    outline = Color(0xFF969696),
    outlineVariant = Color(0xFF505050)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF414141),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCDCDC),
    onPrimaryContainer = Color(0xFF171717),
    secondary = Color(0xFF515151),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDEDEDE),
    onSecondaryContainer = Color(0xFF222222),
    tertiary = Color(0xFF535353),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE2E2E2),
    onTertiaryContainer = Color(0xFF242424),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF202020),
    surface = Color(0xFFF6F6F6),
    onSurface = Color(0xFF202020),
    surfaceVariant = Color(0xFFE5E5E5),
    onSurfaceVariant = Color(0xFF464646),
    surfaceTint = Color(0xFF606060),
    inverseSurface = Color(0xFF303030),
    inverseOnSurface = Color(0xFFF5F5F5),
    inversePrimary = Color(0xFFD6D6D6),
    outline = Color(0xFF747474),
    outlineVariant = Color(0xFFC4C4C4)
)

@Composable
fun SnapSwipeTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDarkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (isDarkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
