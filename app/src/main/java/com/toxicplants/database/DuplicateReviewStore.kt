package com.toxicplants.database

import android.content.Context

/**
 * Guarda grupos de posibles duplicados que el usuario ya revisó.
 *
 * No modifica plantas ni fusiona nada: solo oculta grupos revisados para que no molesten.
 */
object DuplicateReviewStore {
    private const val PREFS_NAME = "duplicate_review_store"
    private const val KEY_REVIEWED_GROUPS = "reviewed_duplicate_groups"

    fun load(context: Context): Set<String> =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_REVIEWED_GROUPS, emptySet())
            .orEmpty()
            .filter { it.isNotBlank() }
            .toSet()

    fun markReviewed(context: Context, groupKey: String) {
        if (groupKey.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val updated = prefs.getStringSet(KEY_REVIEWED_GROUPS, emptySet())
            .orEmpty()
            .toMutableSet()
            .apply { add(groupKey) }
        prefs.edit().putStringSet(KEY_REVIEWED_GROUPS, updated).apply()
    }

    fun unmarkReviewed(context: Context, groupKey: String) {
        if (groupKey.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val updated = prefs.getStringSet(KEY_REVIEWED_GROUPS, emptySet())
            .orEmpty()
            .toMutableSet()
            .apply { remove(groupKey) }
        prefs.edit().putStringSet(KEY_REVIEWED_GROUPS, updated).apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_REVIEWED_GROUPS)
            .apply()
    }
}
