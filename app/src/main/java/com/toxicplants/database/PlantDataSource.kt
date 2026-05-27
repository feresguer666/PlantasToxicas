package com.toxicplants.database

import android.content.Context
import org.json.JSONArray

object PlantDataSource {

    private const val ASSET_PREFIX = "plants"
    private const val ASSET_EXTENSION = ".json"

    fun loadAll(context: Context): List<PlantEntity> {
        val allPlants = mutableListOf<PlantEntity>()
        var fileIndex = 1

        while (true) {
            val fileName = "${ASSET_PREFIX}_$fileIndex$ASSET_EXTENSION"
            try {
                val text = context.assets.open(fileName).bufferedReader(Charsets.UTF_8).use { it.readText() }
                val arr = JSONArray(text)

                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    allPlants += PlantEntity(
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
                fileIndex++
            } catch (e: Exception) {
                break
            }
        }
        return allPlants
    }

    private fun org.json.JSONObject.optStringOrNull(key: String): String? =
        if (isNull(key)) null else optString(key, "").takeIf { it.isNotEmpty() }
}