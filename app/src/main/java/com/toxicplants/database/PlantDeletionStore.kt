package com.toxicplants.database

import android.content.Context

/**
 * Guarda los IDs de plantas del catálogo base que el usuario ha eliminado manualmente.
 *
 * El catálogo se vuelve a sembrar desde assets al arrancar para añadir especies nuevas.
 * Sin esta lista, una planta borrada por el usuario se interpreta como "faltante" y se
 * vuelve a insertar desde plants_N.json en el siguiente inicio de la app.
 */
object PlantDeletionStore {
    private const val PREFS_NAME = "plant_deletion_store"
    private const val KEY_DELETED_IDS = "deleted_seed_plant_ids"

    fun load(context: Context): Set<Int> =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_DELETED_IDS, emptySet())
            .orEmpty()
            .mapNotNull { it.toIntOrNull() }
            .toSet()

    fun isDeleted(context: Context, plantId: Int): Boolean = plantId in load(context)

    fun markDeleted(context: Context, plantId: Int) {
        if (plantId == 0) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val updated = prefs.getStringSet(KEY_DELETED_IDS, emptySet())
            .orEmpty()
            .toMutableSet()
            .apply { add(plantId.toString()) }
        prefs.edit().putStringSet(KEY_DELETED_IDS, updated).apply()
    }

    fun unmarkDeleted(context: Context, plantId: Int) {
        if (plantId == 0) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val updated = prefs.getStringSet(KEY_DELETED_IDS, emptySet())
            .orEmpty()
            .toMutableSet()
            .apply { remove(plantId.toString()) }
        prefs.edit().putStringSet(KEY_DELETED_IDS, updated).apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_DELETED_IDS)
            .apply()
    }
}
