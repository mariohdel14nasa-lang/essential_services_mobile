package com.essential.services.ui.theme

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

private val DarkColorScheme =
    darkColorScheme(
        primary = Purple80,
        secondary = PurpleGrey80,
        tertiary = Pink80,
    )

private val LightColorScheme =
    lightColorScheme(
        primary = Purple40,
        secondary = PurpleGrey40,
        tertiary = Pink40,
    )

@Composable
fun MeDropTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pitchBlackTheme: Boolean = false,
    dynamicColor: Boolean = true,
    remoteColors: List<Color> = emptyList(),
    content: @Composable () -> Unit,
) {
    val baseDark = DarkColorScheme
    val baseLight = LightColorScheme

    val customDark = if (remoteColors.isNotEmpty()) {
        darkColorScheme(
            primary = remoteColors.getOrNull(0) ?: baseDark.primary,
            onPrimary = remoteColors.getOrNull(1) ?: baseDark.onPrimary,
            primaryContainer = remoteColors.getOrNull(2) ?: baseDark.primaryContainer,
            onPrimaryContainer = remoteColors.getOrNull(3) ?: baseDark.onPrimaryContainer,
            secondary = remoteColors.getOrNull(4) ?: baseDark.secondary,
            onSecondary = remoteColors.getOrNull(5) ?: baseDark.onSecondary,
            secondaryContainer = remoteColors.getOrNull(6) ?: baseDark.secondaryContainer,
            onSecondaryContainer = remoteColors.getOrNull(7) ?: baseDark.onSecondaryContainer,
            tertiary = remoteColors.getOrNull(8) ?: baseDark.tertiary,
            onTertiary = remoteColors.getOrNull(9) ?: baseDark.onTertiary,
            background = if (pitchBlackTheme) Color.Black else (remoteColors.getOrNull(10) ?: baseDark.background),
            onBackground = remoteColors.getOrNull(11) ?: baseDark.onBackground,
            surface = if (pitchBlackTheme) Color.Black else (remoteColors.getOrNull(12) ?: baseDark.surface),
            onSurface = remoteColors.getOrNull(13) ?: baseDark.onSurface,
            surfaceVariant = remoteColors.getOrNull(14) ?: baseDark.surfaceVariant,
            onSurfaceVariant = remoteColors.getOrNull(15) ?: baseDark.onSurfaceVariant,
        )
    } else {
        if (pitchBlackTheme) {
            baseDark.copy(
                background = Color.Black,
                surface = Color.Black,
                surfaceContainer = Color.Black,
                surfaceContainerLowest = Color.Black,
                surfaceContainerLow = Color.Black,
            )
        } else {
            baseDark
        }
    }

    val customLight = if (remoteColors.isNotEmpty()) {
        lightColorScheme(
            primary = remoteColors.getOrNull(0) ?: baseLight.primary,
            onPrimary = remoteColors.getOrNull(1) ?: baseLight.onPrimary,
            primaryContainer = remoteColors.getOrNull(2) ?: baseLight.primaryContainer,
            onPrimaryContainer = remoteColors.getOrNull(3) ?: baseLight.onPrimaryContainer,
            secondary = remoteColors.getOrNull(4) ?: baseLight.secondary,
            onSecondary = remoteColors.getOrNull(5) ?: baseLight.onSecondary,
            secondaryContainer = remoteColors.getOrNull(6) ?: baseLight.secondaryContainer,
            onSecondaryContainer = remoteColors.getOrNull(7) ?: baseLight.onSecondaryContainer,
            tertiary = remoteColors.getOrNull(8) ?: baseLight.tertiary,
            onTertiary = remoteColors.getOrNull(9) ?: baseLight.onTertiary,
            background = remoteColors.getOrNull(10) ?: baseLight.background,
            onBackground = remoteColors.getOrNull(11) ?: baseLight.onBackground,
            surface = remoteColors.getOrNull(12) ?: baseLight.surface,
            onSurface = remoteColors.getOrNull(13) ?: baseLight.onSurface,
            surfaceVariant = remoteColors.getOrNull(14) ?: baseLight.surfaceVariant,
            onSurfaceVariant = remoteColors.getOrNull(15) ?: baseLight.onSurfaceVariant,
        )
    } else {
        baseLight
    }

    val colorScheme =
        when {
            remoteColors.isNotEmpty() -> customDark.let { if (!darkTheme) customLight else it }
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                val dynamicScheme =
                    if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                if (darkTheme && pitchBlackTheme) {
                    dynamicScheme.copy(
                        background = Color.Black,
                        surface = Color.Black,
                        surfaceContainer = Color.Black,
                        surfaceContainerLowest = Color.Black,
                        surfaceContainerLow = Color.Black,
                    )
                } else {
                    dynamicScheme
                }
            }

            darkTheme -> customDark

            else -> customLight
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}
