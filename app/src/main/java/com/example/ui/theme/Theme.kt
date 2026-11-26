package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

val LocalLdroidTokens = staticCompositionLocalOf { LdroidThemes.DesertLuxe }

// Consistent component radii: buttons ~12dp, cards ~16dp, dialogs ~24dp
val LdroidShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun LdroidTheme(
    tokens: LdroidTokens = LdroidThemes.DesertLuxe,
    content: @Composable () -> Unit
) {
    val colorScheme = if (tokens.isDark) {
        darkColorScheme(
            primary = tokens.primaryAccent,
            onPrimary = tokens.onPrimaryAccent,
            primaryContainer = tokens.elevatedSurface,
            onPrimaryContainer = tokens.primaryAccent,
            secondary = tokens.secondaryAccent,
            onSecondary = tokens.onPrimaryAccent,
            secondaryContainer = tokens.surface2,
            onSecondaryContainer = tokens.textPrimary,
            tertiary = tokens.tertiaryAccent,
            background = tokens.background,
            onBackground = tokens.textPrimary,
            surface = tokens.surface1,
            onSurface = tokens.textPrimary,
            surfaceVariant = tokens.surface2,
            onSurfaceVariant = tokens.textMuted,
            outline = tokens.border,
            outlineVariant = tokens.divider,
            error = tokens.error,
            onError = tokens.onPrimaryAccent
        )
    } else {
        lightColorScheme(
            primary = tokens.primaryAccent,
            onPrimary = tokens.onPrimaryAccent,
            primaryContainer = tokens.elevatedSurface,
            onPrimaryContainer = tokens.textPrimary,
            secondary = tokens.secondaryAccent,
            onSecondary = tokens.onPrimaryAccent,
            secondaryContainer = tokens.surface2,
            onSecondaryContainer = tokens.textPrimary,
            tertiary = tokens.tertiaryAccent,
            background = tokens.background,
            onBackground = tokens.textPrimary,
            surface = tokens.surface1,
            onSurface = tokens.textPrimary,
            surfaceVariant = tokens.surface2,
            onSurfaceVariant = tokens.textMuted,
            outline = tokens.border,
            outlineVariant = tokens.divider,
            error = tokens.error,
            onError = tokens.onPrimaryAccent
        )
    }

    CompositionLocalProvider(LocalLdroidTokens provides tokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LdroidTypography,
            shapes = LdroidShapes,
            content = content
        )
    }
}
