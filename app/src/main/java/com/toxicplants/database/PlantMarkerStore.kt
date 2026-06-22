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
    private const val KEY_CUSTOM_MARKERS = "custom_markers"

    val DEFAULT_MARKERS = listOf(
        "Revisar",
        "Pendiente foto",
        "Confirmar toxicidad",
        "Interesante",
        "Cultivada cerca",
        "Dato dudoso",
        "Fuente pendiente"
    )

    fun loadCustomMarkers(context: Context): List<String> =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_CUSTOM_MARKERS, emptySet())
            .orEmpty()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }

    fun allAvailableMarkers(context: Context): List<String> =
        (DEFAULT_MARKERS + loadCustomMarkers(context))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }

    fun addCustomMarker(context: Context, marker: String): String? {
        val cleaned = marker.trim()
        if (cleaned.isBlank()) return null
        val existing = loadCustomMarkers(context).toMutableList()
        if (DEFAULT_MARKERS.any { it.equals(cleaned, ignoreCase = true) } || existing.any { it.equals(cleaned, ignoreCase = true) }) {
            return cleaned
        }
        existing += cleaned
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_CUSTOM_MARKERS, existing.toSet())
            .apply()
        return cleaned
    }

    fun deleteCustomMarker(context: Context, marker: String, removeFromPlants: Boolean = false) {
        val cleaned = marker.trim()
        if (cleaned.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val updated = loadCustomMarkers(context)
            .filterNot { it.equals(cleaned, ignoreCase = true) }
            .toSet()
        val editor = prefs.edit().putStringSet(KEY_CUSTOM_MARKERS, updated)
        if (removeFromPlants) {
            prefs.all.keys
                .filter { it.startsWith(KEY_PREFIX) }
                .forEach { key ->
                    val markers = prefs.getStringSet(key, emptySet()).orEmpty()
                        .filterNot { it.equals(cleaned, ignoreCase = true) }
                        .toSet()
                    if (markers.isEmpty()) editor.remove(key) else editor.putStringSet(key, markers)
                }
        }
        editor.apply()
    }

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
        val customMarkers = prefs.getStringSet(KEY_CUSTOM_MARKERS, emptySet()).orEmpty().toSet()
        val editor = prefs.edit().clear().putStringSet(KEY_CUSTOM_MARKERS, customMarkers)
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
