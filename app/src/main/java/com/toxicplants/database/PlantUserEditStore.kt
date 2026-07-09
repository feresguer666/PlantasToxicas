package com.toxicplants.database

import android.content.Context

/**
 * Guarda los IDs de plantas editadas manualmente por el usuario.
 *
 * Sirve para proteger esas fichas frente a actualizaciones automáticas del catálogo
 * base desde assets. Por ejemplo, si el usuario modifica `mythsAndLegends`, el
 * smart-merge de arranque no debe sobrescribir ese campo con el valor del JSON base.
 */
object PlantUserEditStore {
    private const val PREFS_NAME = "plant_user_edit_store"
    private const val KEY_EDITED_IDS = "edited_seed_plant_ids"

    fun load(context: Context): Set<Int> =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_EDITED_IDS, emptySet())
            .orEmpty()
            .mapNotNull { it.toIntOrNull() }
            .toSet()

    fun isEdited(context: Context, plantId: Int): Boolean = plantId in load(context)

    fun markEdited(context: Context, plantId: Int) {
        if (plantId == 0) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val updated = prefs.getStringSet(KEY_EDITED_IDS, emptySet())
            .orEmpty()
            .toMutableSet()
            .apply { add(plantId.toString()) }
        prefs.edit().putStringSet(KEY_EDITED_IDS, updated).apply()
    }

    fun unmarkEdited(context: Context, plantId: Int) {
        if (plantId == 0) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val updated = prefs.getStringSet(KEY_EDITED_IDS, emptySet())
            .orEmpty()
            .toMutableSet()
            .apply { remove(plantId.toString()) }
        prefs.edit().putStringSet(KEY_EDITED_IDS, updated).apply()
    }

    /** Sustituye la lista completa de IDs editados. Se usa al restaurar backups. */
    fun replaceAll(context: Context, ids: Set<Int>) {
        val safeIds = ids
            .filter { it != 0 }
            .map { it.toString() }
            .toSet()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_EDITED_IDS, safeIds)
            .apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_EDITED_IDS)
            .apply()
    }
}
