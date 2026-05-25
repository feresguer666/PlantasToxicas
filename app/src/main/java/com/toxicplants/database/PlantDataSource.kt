package com.toxicplants.database

import android.content.Context
import org.json.JSONArray

/**
 * Carga el catálogo inicial de plantas desde `assets/plants.json`.
 *
 * Antes este archivo contenía ~25.000 líneas de Kotlin generadas a mano.
 * Ahora el dataset vive en `app/src/main/assets/plants.json`, lo que:
 *   - Acelera la compilación (KSP / Kotlin compiler).
 *   - Evita el riesgo de `MethodTooLargeException` (límite JVM de 64KB por método).
 *   - Permite actualizar el dataset sin tocar código Kotlin.
 *
 * El JSON se lee una sola vez, en el `Room.Callback.onCreate()` (ver [PlantDatabase]).
 */
object PlantDataSource {

    private const val ASSET_FILE = "plants.json"

    fun loadAll(context: Context): List<PlantEntity> {
        val text = context.assets.open(ASSET_FILE).bufferedReader(Charsets.UTF_8).use { it.readText() }
        val arr = JSONArray(text)
        val out = ArrayList<PlantEntity>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out += PlantEntity(
                id = o.optInt("id", 0),
                commonName = o.optString("commonName", ""),
                scientificName = o.optString("scientificName", ""),
                family = o.optString("family", ""),
                toxicityLevel = o.optString("toxicityLevel", ""),
                toxicParts = o.optString("toxicParts", ""),
                symptoms = o.optString("symptoms", ""),
                description = o.optString("description", ""),
                habitat = o.optString("habitat", ""),
                geographicDistribution = o.optString("geographicDistribution", ""),
                firstAid = o.optString("firstAid", ""),
                imageUrl = o.optString("imageUrl", ""),
                isFavorite = o.optBoolean("isFavorite", false),
                category = o.optString("category", ""),
                latitude = if (o.isNull("latitude")) null else o.optDouble("latitude"),
                longitude = if (o.isNull("longitude")) null else o.optDouble("longitude"),
                locationName = o.optStringOrNull("locationName"),
                foundDate = o.optStringOrNull("foundDate"),
                notes = o.optStringOrNull("notes"),
            )
        }
        return out
    }

    private fun org.json.JSONObject.optStringOrNull(key: String): String? =
        if (isNull(key)) null else optString(key, "").takeIf { it.isNotEmpty() }
}
