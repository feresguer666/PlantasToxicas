package com.toxicplants.database

import android.content.Context

/**
 * Historial local de fichas de plantas vistas recientemente.
 *
 * Guarda pares plantId/timestamp en SharedPreferences para evitar migraciones Room.
 */
object RecentPlantStore {
    private const val PREFS_NAME = "recent_plant_store"
    private const val KEY_RECENT = "recent_plant_entries"
    private const val MAX_ITEMS = 50

    data class Entry(
        val plantId: Int,
        val viewedAt: Long
    )

    fun add(context: Context, plantId: Int, timestamp: Long = System.currentTimeMillis()) {
        if (plantId == 0) return
        val updated = load(context)
            .filterNot { it.plantId == plantId }
            .toMutableList()
            .apply { add(0, Entry(plantId, timestamp)) }
            .take(MAX_ITEMS)
        save(context, updated)
    }

    fun load(context: Context): List<Entry> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_RECENT, "")
            .orEmpty()
        if (raw.isBlank()) return emptyList()
        return raw.split("|")
            .mapNotNull { token ->
                val parts = token.split(":")
                val id = parts.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
                val ts = parts.getOrNull(1)?.toLongOrNull() ?: 0L
                Entry(id, ts)
            }
            .distinctBy { it.plantId }
            .take(MAX_ITEMS)
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_RECENT)
            .apply()
    }

    private fun save(context: Context, entries: List<Entry>) {
        val raw = entries
            .take(MAX_ITEMS)
            .joinToString("|") { "${it.plantId}:${it.viewedAt}" }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_RECENT, raw)
            .apply()
    }
}
