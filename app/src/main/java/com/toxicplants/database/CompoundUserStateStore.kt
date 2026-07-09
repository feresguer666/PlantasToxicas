package com.toxicplants.database

import android.content.Context

/**
 * Estado local de compuestos del catálogo base modificados por el usuario.
 *
 * El catálogo base vive en assets/compounds.json y se reinyecta al arrancar para
 * añadir compuestos nuevos. Estas listas evitan que un compuesto borrado vuelva
 * a aparecer y protegen compuestos editados frente a actualizaciones automáticas.
 */
object CompoundUserStateStore {
    private const val PREFS_NAME = "compound_user_state_store"
    private const val KEY_DELETED_IDS = "deleted_compound_ids"
    private const val KEY_EDITED_IDS = "edited_compound_ids"

    fun loadDeleted(context: Context): Set<Int> = loadIds(context, KEY_DELETED_IDS)
    fun loadEdited(context: Context): Set<Int> = loadIds(context, KEY_EDITED_IDS)

    fun markDeleted(context: Context, compoundId: Int) {
        if (compoundId == 0) return
        addId(context, KEY_DELETED_IDS, compoundId)
        removeId(context, KEY_EDITED_IDS, compoundId)
    }

    fun unmarkDeleted(context: Context, compoundId: Int) {
        if (compoundId == 0) return
        removeId(context, KEY_DELETED_IDS, compoundId)
    }

    fun markEdited(context: Context, compoundId: Int) {
        if (compoundId == 0) return
        addId(context, KEY_EDITED_IDS, compoundId)
        removeId(context, KEY_DELETED_IDS, compoundId)
    }

    fun replaceDeleted(context: Context, ids: Set<Int>) = replaceIds(context, KEY_DELETED_IDS, ids)
    fun replaceEdited(context: Context, ids: Set<Int>) = replaceIds(context, KEY_EDITED_IDS, ids)

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_DELETED_IDS)
            .remove(KEY_EDITED_IDS)
            .apply()
    }

    private fun loadIds(context: Context, key: String): Set<Int> =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(key, emptySet())
            .orEmpty()
            .mapNotNull { it.toIntOrNull() }
            .toSet()

    private fun addId(context: Context, key: String, id: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val updated = prefs.getStringSet(key, emptySet())
            .orEmpty()
            .toMutableSet()
            .apply { add(id.toString()) }
        prefs.edit().putStringSet(key, updated).apply()
    }

    private fun removeId(context: Context, key: String, id: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val updated = prefs.getStringSet(key, emptySet())
            .orEmpty()
            .toMutableSet()
            .apply { remove(id.toString()) }
        prefs.edit().putStringSet(key, updated).apply()
    }

    private fun replaceIds(context: Context, key: String, ids: Set<Int>) {
        val safeIds = ids
            .filter { it != 0 }
            .map { it.toString() }
            .toSet()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(key, safeIds)
            .apply()
    }
}
