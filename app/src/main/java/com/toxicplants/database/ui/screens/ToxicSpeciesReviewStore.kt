package com.toxicplants.database.ui.screens

import android.content.Context
import androidx.compose.ui.graphics.Color

object ToxicSpeciesReviewStore {
    private const val PREFS_NAME = "toxic_species_review_store"
    private const val KEY_GENUS = "genus_review_status"
    private const val KEY_SPECIES = "species_review_status"

    const val PENDIENTE = "pendiente"
    const val REVISADA = "revisada"
    const val ANADIDA = "añadida"
    const val DUDOSA = "dudosa"

    val allStatuses = listOf(PENDIENTE, REVISADA, ANADIDA, DUDOSA)

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun genusKey(familyName: String, genusName: String): String =
        "$familyName|$genusName"

    private fun speciesKey(familyName: String, genusName: String, scientificName: String): String =
        "$familyName|$genusName|$scientificName"

    fun getGenusStatus(
        context: Context,
        familyName: String,
        genusName: String
    ): String {
        return prefs(context).getString(
            "$KEY_GENUS:${genusKey(familyName, genusName)}",
            PENDIENTE
        ) ?: PENDIENTE
    }

    fun setGenusStatus(
        context: Context,
        familyName: String,
        genusName: String,
        status: String
    ) {
        prefs(context).edit()
            .putString("$KEY_GENUS:${genusKey(familyName, genusName)}", status)
            .apply()
    }

    fun getSpeciesStatus(
        context: Context,
        familyName: String,
        genusName: String,
        scientificName: String
    ): String {
        return prefs(context).getString(
            "$KEY_SPECIES:${speciesKey(familyName, genusName, scientificName)}",
            PENDIENTE
        ) ?: PENDIENTE
    }

    fun setSpeciesStatus(
        context: Context,
        familyName: String,
        genusName: String,
        scientificName: String,
        status: String
    ) {
        prefs(context).edit()
            .putString("$KEY_SPECIES:${speciesKey(familyName, genusName, scientificName)}", status)
            .apply()
    }
}

fun reviewStatusLabel(status: String): String = when (status.lowercase()) {
    ToxicSpeciesReviewStore.PENDIENTE -> "Pendiente"
    ToxicSpeciesReviewStore.REVISADA -> "Revisada"
    ToxicSpeciesReviewStore.ANADIDA -> "Añadida"
    ToxicSpeciesReviewStore.DUDOSA -> "Dudosa"
    else -> "Pendiente"
}

fun reviewStatusColor(status: String): Color = when (status.lowercase()) {
    ToxicSpeciesReviewStore.PENDIENTE -> Color(0xFFFFB300)
    ToxicSpeciesReviewStore.REVISADA -> Color(0xFF2E7D32)
    ToxicSpeciesReviewStore.ANADIDA -> Color(0xFF1565C0)
    ToxicSpeciesReviewStore.DUDOSA -> Color(0xFFC62828)
    else -> Color(0xFFFFB300)
}