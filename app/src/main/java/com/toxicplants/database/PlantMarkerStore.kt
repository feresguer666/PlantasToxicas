package com.toxicplants.database

import android.content.Context

/**
 * Marcadores personales por planta, independientes de favoritos y notas.
 *
 * Se guardan en SharedPreferences para evitar migraciones Room.
 */
object PlantMarkerStore {
    private const val PREFS_NAME = "plant_marker_store"
    private const val KEY_PREFIX = "plant_markers_"

    val DEFAULT_MARKERS = listOf(
        "Revisar",
        "Pendiente foto",
        "Confirmar toxicidad",
        "Interesante",
        "Cultivada cerca",
        "Dato dudoso",
        "Fuente pendiente"
    )

    fun load(context: Context, plantId: Int): Set<String> {
        if (plantId == 0) return emptySet()
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_PREFIX + plantId, emptySet())
            .orEmpty()
            .filter { it.isNotBlank() }
            .toSet()
    }

    fun loadAll(context: Context): Map<Int, Set<String>> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.all.mapNotNull { (key, value) ->
            if (!key.startsWith(KEY_PREFIX)) return@mapNotNull null
            val plantId = key.removePrefix(KEY_PREFIX).toIntOrNull() ?: return@mapNotNull null
            val markers = (value as? Set<*>)
                .orEmpty()
                .mapNotNull { it as? String }
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .toSet()
            if (markers.isEmpty()) null else plantId to markers
        }.toMap()
    }

    fun save(context: Context, plantId: Int, markers: Set<String>) {
        if (plantId == 0) return
        val cleaned = markers
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (cleaned.isEmpty()) {
            prefs.edit().remove(KEY_PREFIX + plantId).apply()
        } else {
            prefs.edit().putStringSet(KEY_PREFIX + plantId, cleaned).apply()
        }
    }

    fun toggle(context: Context, plantId: Int, marker: String): Set<String> {
        val current = load(context, plantId).toMutableSet()
        if (marker in current) current.remove(marker) else current.add(marker)
        save(context, plantId, current)
        return current
    }

    fun clear(context: Context, plantId: Int) {
        if (plantId == 0) return
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_PREFIX + plantId)
            .apply()
    }

    /** Sustituye todos los marcadores. Se usa al restaurar backups. */
    fun replaceAll(context: Context, markersByPlant: Map<Int, Set<String>>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit().clear()
        markersByPlant.forEach { (plantId, markers) ->
            if (plantId != 0) {
                val cleaned = markers
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .toSet()
                if (cleaned.isNotEmpty()) {
                    editor.putStringSet(KEY_PREFIX + plantId, cleaned)
                }
            }
        }
        editor.apply()
    }

    fun clearAll(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}
