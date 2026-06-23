package com.toxicplants.database

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object PoisonousFamilySeedDataSource {

    private const val ASSET_NAME = "poisonous_family_genera_seed.json"

    data class SeedItem(
        val familyName: String = "",
        val genusName: String = "",
        val genusSpeciesCount: Int = 0,
        val toxins: String = "",
        val symptoms: String = "",
        val toxicParts: String = "",
        val notes: String = "",
    )

    fun loadAll(context: Context): List<PoisonousFamilyGenusEntity> {
        val json = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        val type = object : TypeToken<List<SeedItem>>() {}.type
        val seeds: List<SeedItem> = Gson().fromJson(json, type)
        return seeds
            .map { seed ->
                PoisonousFamilyGenusEntity(
                    familyName = seed.familyName.trim(),
                    genusName = seed.genusName.trim(),
                    genusSpeciesCount = seed.genusSpeciesCount.coerceAtLeast(0),
                    toxins = seed.toxins.trim(),
                    symptoms = seed.symptoms.trim(),
                    toxicParts = seed.toxicParts.trim(),
                    notes = seed.notes.trim(),
                    updatedAt = System.currentTimeMillis()
                )
            }
            .filter { it.familyName.isNotBlank() && it.genusName.isNotBlank() }
            .distinctBy { it.familyName.lowercase() + "|" + it.genusName.lowercase() }
    }
}
