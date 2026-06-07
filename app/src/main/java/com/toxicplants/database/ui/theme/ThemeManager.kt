package com.toxicplants.database.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Gestiona el modo oscuro de forma global y persistente.
 *
 * Valores posibles:
 *  - "system" → sigue el tema del sistema
 *  - "dark"   → siempre oscuro
 *  - "light"  → siempre claro
 */
object ThemeManager {

    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_MODE = "dark_mode"

    private val _themeMode = MutableStateFlow("system")
    val themeMode: StateFlow<String> = _themeMode

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _themeMode.value = prefs.getString(KEY_MODE, "system") ?: "system"
    }

    fun setMode(context: Context, mode: String) {
        _themeMode.value = mode
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODE, mode)
            .apply()
    }
}
