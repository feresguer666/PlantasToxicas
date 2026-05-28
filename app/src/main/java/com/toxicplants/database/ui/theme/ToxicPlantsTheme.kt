package com.toxicplants.database.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ==================== COLORES CLAROS ====================
private val LightPrimary = Color(0xFF2E7D32)
private val LightOnPrimary = Color.White
private val LightPrimaryContainer = Color(0xFFA5D6A7)
private val LightOnPrimaryContainer = Color(0xFF1B5E20)

private val LightSecondary = Color(0xFF388E3C)
private val LightOnSecondary = Color.White
private val LightSecondaryContainer = Color(0xFFC8E6C9)
private val LightOnSecondaryContainer = Color(0xFF2E7D32)

private val LightTertiary = Color(0xFF6A1B9A)
private val LightOnTertiary = Color.White
private val LightTertiaryContainer = Color(0xFFE1BEE7)
private val LightOnTertiaryContainer = Color(0xFF4A148C)

private val LightError = Color(0xFFB71C1C)
private val LightOnError = Color.White
private val LightErrorContainer = Color(0xFFFFCDD2)
private val LightOnErrorContainer = Color(0xFF7F0000)

private val LightBackground = Color(0xFFF1F8E9)
private val LightOnBackground = Color(0xFF1C1B1F)
private val LightSurface = Color.White
private val LightOnSurface = Color(0xFF1C1B1F)
private val LightSurfaceVariant = Color(0xFFE7E0EC)
private val LightOnSurfaceVariant = Color(0xFF49454F)

private val LightOutline = Color(0xFF79747E)

// ==================== COLORES OSCUROS ====================
private val DarkPrimary = Color(0xFF81C784)
private val DarkOnPrimary = Color(0xFF003300)
private val DarkPrimaryContainer = Color(0xFF1B5E20)
private val DarkOnPrimaryContainer = Color(0xFFA5D6A7)

private val DarkSecondary = Color(0xFFA5D6A7)
private val DarkOnSecondary = Color(0xFF003300)
private val DarkSecondaryContainer = Color(0xFF2E7D32)
private val DarkOnSecondaryContainer = Color(0xFFC8E6C9)

private val DarkTertiary = Color(0xFFCE93D8)
private val DarkOnTertiary = Color(0xFF3E0057)
private val DarkTertiaryContainer = Color(0xFF6A1B9A)
private val DarkOnTertiaryContainer = Color(0xFFE1BEE7)

private val DarkError = Color(0xFFFF6B6B)
private val DarkOnError = Color(0xFF690005)
private val DarkErrorContainer = Color(0xFF93000A)
private val DarkOnErrorContainer = Color(0xFFFFDAD6)

private val DarkBackgroundColor = Color(0xFF0D1F0F)
private val DarkOnBackground = Color(0xFFE6E1E5)
private val DarkSurfaceColor = Color(0xFF121F14)
private val DarkOnSurface = Color(0xFFE6E1E5)
private val DarkSurfaceVariant = Color(0xFF1A2F1E)
private val DarkOnSurfaceVariant = Color(0xFFCAC4D0)

private val DarkOutline = Color(0xFF938F99)

// ==================== ESQUEMAS ====================
private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    background = DarkBackgroundColor,
    onBackground = DarkOnBackground,
    surface = DarkSurfaceColor,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline
)

@Composable
fun ToxicPlantsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, true)
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}