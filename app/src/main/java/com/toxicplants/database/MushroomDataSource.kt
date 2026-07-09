package com.toxicplants.database

import android.content.Context
import org.json.JSONArray

/** Carga el catálogo inicial de setas tóxicas desde `assets/mushrooms.json`. */
object MushroomDataSource {

    private const val ASSET_FILE = "mushrooms.json"

    fun loadAll(context: Context): List<MushroomEntity> {
        return try {
            loadFromAssets(context)
        } catch (_: Exception) {
            // Respaldo para evitar que la sección quede inutilizable si el asset
            // no se empaqueta, se corrompe o Android Studio instala un APK incremental
            // sin refrescar assets.
            MushroomCatalogFallback.items
        }
    }

    private fun loadFromAssets(context: Context): List<MushroomEntity> {
        val text = context.assets.open(ASSET_FILE)
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
        val arr = JSONArray(text)
        val out = ArrayList<MushroomEntity>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out += MushroomEntity(
                id = o.optInt("id", 0),
                commonName = o.optString("commonName", ""),
                scientificName = o.optString("scientificName", ""),
                family = o.optString("family", ""),
                toxicityLevel = o.optString("toxicityLevel", ""),
                syndrome = o.optString("syndrome", ""),
                toxicCompounds = o.optString("toxicCompounds", ""),
                onsetTime = o.optString("onsetTime", ""),
                symptoms = o.optString("symptoms", ""),
                description = o.optString("description", ""),
                habitat = o.optString("habitat", ""),
                season = o.optString("season", ""),
                geographicDistribution = o.optString("geographicDistribution", ""),
                edibleConfusions = o.optString("edibleConfusions", ""),
                firstAid = o.optString("firstAid", ""),
                treatment = o.optString("treatment", ""),
                notes = o.optString("notes", ""),
                imageUrl = o.optString("imageUrl", ""),
                isDeadly = o.optBoolean("isDeadly", false),
                isFavorite = o.optBoolean("isFavorite", false),
            )
        }
        return out.ifEmpty { MushroomCatalogFallback.items }
    }
}
