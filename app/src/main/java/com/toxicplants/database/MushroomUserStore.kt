package com.toxicplants.database

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Persistencia local editable del catálogo de setas. */
object MushroomUserStore {

    private const val PREFS = "mushroom_catalog_prefs"
    private const val KEY_CATALOG = "mushroom_catalog_json_v1"

    fun load(context: Context): List<MushroomEntity>? {
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_CATALOG, null)
            ?: return null
        val userList = runCatching { parse(JSONArray(json)) }.getOrNull() ?: return null

        val assets = MushroomDataSource.loadAll(context)
        val userMap = userList.associateBy { it.id }

        val merged = assets.map { asset ->
            val userItem = userMap[asset.id]
            if (userItem != null) {
                asset.copy(isFavorite = userItem.isFavorite, notes = userItem.notes)
            } else {
                asset
            }
        }.toMutableList()

        val assetIds = assets.map { it.id }.toSet()
        merged.addAll(userList.filter { it.id !in assetIds })
        return merged
    }

    fun save(context: Context, items: List<MushroomEntity>) {
        val arr = JSONArray()
        items.sortedWith(compareByDescending<MushroomEntity> { it.isDeadly }.thenBy { it.scientificName })
            .forEach { arr.put(it.toJson()) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CATALOG, arr.toString())
            .apply()
    }

    private fun parse(arr: JSONArray): List<MushroomEntity> {
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
        return out
    }

    private fun MushroomEntity.toJson(): JSONObject = JSONObject().apply {
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
        put("season", season)
        put("geographicDistribution", geographicDistribution)
        put("edibleConfusions", edibleConfusions)
        put("firstAid", firstAid)
        put("treatment", treatment)
        put("notes", notes)
        put("imageUrl", imageUrl)
        put("isDeadly", isDeadly)
        put("isFavorite", isFavorite)
    }
}
