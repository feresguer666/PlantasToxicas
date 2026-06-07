package com.toxicplants.database

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Persistencia local editable del catálogo de líquenes. */
object LichenUserStore {

    private const val PREFS = "lichen_catalog_prefs"
    private const val KEY_CATALOG = "lichen_catalog_json_v1"

    fun load(context: Context): List<LichenEntity>? {
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_CATALOG, null)
            ?: return null
        return runCatching { parse(JSONArray(json)) }.getOrNull()
    }

    fun save(context: Context, items: List<LichenEntity>) {
        val arr = JSONArray()
        items.sortedWith(compareByDescending<LichenEntity> { it.isHighRisk }.thenBy { it.scientificName })
            .forEach { arr.put(it.toJson()) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CATALOG, arr.toString())
            .apply()
    }

    private fun parse(arr: JSONArray): List<LichenEntity> {
        val out = ArrayList<LichenEntity>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out += LichenEntity(
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
                geographicDistribution = o.optString("geographicDistribution", ""),
                confusions = o.optString("confusions", ""),
                firstAid = o.optString("firstAid", ""),
                treatment = o.optString("treatment", ""),
                notes = o.optString("notes", ""),
                imageUrl = o.optString("imageUrl", ""),
                isHighRisk = o.optBoolean("isHighRisk", false),
                isFavorite = o.optBoolean("isFavorite", false),
            )
        }
        return out
    }

    private fun LichenEntity.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("commonName", commonName)
        put("scientificName", scientificName)
        put("family", family)
        put("toxicityLevel", toxicityLevel)
        put("syndrome", syndrome)
        put("toxicCompounds", toxicCompounds)
        put("onsetTime", onsetTime)
        put("symptoms", symptoms)
        put("description", description)
        put("habitat", habitat)
        put("geographicDistribution", geographicDistribution)
        put("confusions", confusions)
        put("firstAid", firstAid)
        put("treatment", treatment)
        put("notes", notes)
        put("imageUrl", imageUrl)
        put("isHighRisk", isHighRisk)
        put("isFavorite", isFavorite)
    }
}
