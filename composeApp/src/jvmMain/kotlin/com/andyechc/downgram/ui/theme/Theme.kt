package com.andyechc.downgram.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalThemeMode = staticCompositionLocalOf { ThemeMode.SYSTEM }

private val DarkColorScheme = darkColorScheme(
    primary = Purple400,
    onPrimary = DarkTextPrimary,
    primaryContainer = Purple700,
    onPrimaryContainer = Purple200,
    secondary = Purple500,
    onSecondary = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkCard,
    onSurfaceVariant = DarkTextSecondary,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    error = CupertinoRed,
    onError = DarkTextPrimary,
    outline = DarkSeparator,
    outlineVariant = DarkSeparator
)

private val LightColorScheme = lightColorScheme(
    primary = Purple500,
    onPrimary = LightTextPrimary,
    primaryContainer = Purple100,
    onPrimaryContainer = Purple700,
    secondary = Purple400,
    onSecondary = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightCard,
    onSurfaceVariant = LightTextSecondary,
    background = LightBackground,
    onBackground = LightTextPrimary,
    error = CupertinoRed,
    onError = LightTextPrimary,
    outline = LightSeparator,
    outlineVariant = LightSeparator
)

@Composable
fun DowngramTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalThemeMode provides themeMode) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CupertinoTypography,
            content = content
        )
    }
}

object DowngramThemeColors {
    val isDark: Boolean
        @Composable
        get() = MaterialTheme.colorScheme.background == DarkBackground

    val cardColor
        @Composable
        get() = if (isDark) DarkCard else LightCard

    val cardElevatedColor
        @Composable
        get() = if (isDark) DarkCardElevated else LightCardElevated

    val separatorColor
        @Composable
        get() = if (isDark) DarkSeparator else LightSeparator

    val textTertiary
        @Composable
        get() = if (isDark) DarkTextTertiary else LightTextTertiary
}
