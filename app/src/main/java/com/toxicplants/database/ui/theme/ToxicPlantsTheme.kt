package com.toxicplants.database.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ═══════════════════════════════════════════════════════════════
// ESQUEMA CLARO
// ═══════════════════════════════════════════════════════════════
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA5D6A7),
    onPrimaryContainer = Color(0xFF1B5E20),
    secondary = Color(0xFF388E3C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC8E6C9),
    onSecondaryContainer = Color(0xFF2E7D32),
    tertiary = Color(0xFF6A1B9A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE1BEE7),
    onTertiaryContainer = Color(0xFF4A148C),
    error = Color(0xFFB71C1C),
    onError = Color.White,
    errorContainer = Color(0xFFFFCDD2),
    onErrorContainer = Color(0xFF7F0000),
    background = Color(0xFFF1F8E9),
    onBackground = Color(0xFF1C1B1F),
    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E)
)

// ═══════════════════════════════════════════════════════════════
// ESQUEMA OSCURO DE LUXE — AMOLED + Neón
// ═══════════════════════════════════════════════════════════════
private val DarkColorScheme = darkColorScheme(
    primary = NeonGreen,
    onPrimary = Color(0xFF003300),
    primaryContainer = Color(0xFF0D3311),
    onPrimaryContainer = NeonMint,
    secondary = NeonGreenSoft,
    onSecondary = Color(0xFF003300),
    secondaryContainer = Color(0xFF1B5E20),
    onSecondaryContainer = Color(0xFFC8E6C9),
    tertiary = NeonPurple,
    onTertiary = Color(0xFF3E0057),
    tertiaryContainer = Color(0xFF4A148C),
    onTertiaryContainer = Color(0xFFF3E5F5),
    error = NeonRed,
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = DeLuxeBlack,
    onBackground = Color.White, // Forzamos blanco puro
    surface = DeLuxeSurface,
    onSurface = Color.White,    // Forzamos blanco puro
    surfaceVariant = DeLuxeSurfaceVariant,
    onSurfaceVariant = Color(0xFFB0BEC5), // Gris muy claro para lectura
    outline = DeLuxeOutline,
    inverseSurface = Color(0xFFE6E1E5),
    inverseOnSurface = Color(0xFF1C1B1F),
    inversePrimary = Color(0xFF2E7D32),
    surfaceTint = NeonGreen,
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
            if (darkTheme) {
                @Suppress("DEPRECATION")
                window.statusBarColor = DeLuxeBlack.toArgb()
                @Suppress("DEPRECATION")
                window.navigationBarColor = DeLuxeBlack.toArgb()
            }
            WindowCompat.setDecorFitsSystemWindows(window, true)
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
