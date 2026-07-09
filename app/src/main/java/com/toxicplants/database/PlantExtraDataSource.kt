package com.toxicplants.database

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object PlantExtraDataSource {

    private var cache: MutableMap<String, PlantExtraInfo>? = null

    private fun getFile(context: Context): File =
        File(context.filesDir, "plant_extra.json")

    private fun ensureFile(context: Context) {
        val dest = getFile(context)
        if (!dest.exists()) {
            try {
                val text = context.assets.open("plant_extra.json")
                    .bufferedReader(Charsets.UTF_8).use { it.readText() }
                dest.writeText(text, Charsets.UTF_8)
            } catch (e: Exception) {
                dest.writeText("[]", Charsets.UTF_8)
            }
        }
    }

    fun loadAll(context: Context): MutableMap<String, PlantExtraInfo> {
        cache?.let { return it }
        ensureFile(context)
        val result = mutableMapOf<String, PlantExtraInfo>()
        try {
            val text = getFile(context).readText(Charsets.UTF_8)
            val arr  = JSONArray(text)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val confusable = mutableListOf<String>()
                val confArr = o.optJSONArray("confusableWith")
                if (confArr != null) {
                    for (j in 0 until confArr.length()) confusable.add(confArr.getString(j))
                }
                val info = PlantExtraInfo(
                    scientificName  = o.optString("scientificName", ""),
                    toxicDogs       = o.optBoolean("toxicDogs", false),
                    toxicCats       = o.optBoolean("toxicCats", false),
                    toxicHorses     = o.optBoolean("toxicHorses", false),
                    toxicCattle     = o.optBoolean("toxicCattle", false),
                    toxicChildren   = o.optBoolean("toxicChildren", false),
                    flowerColor     = o.optString("flowerColor", ""),
                    fruitColor      = o.optString("fruitColor", ""),
                    confusableWith  = confusable,
                    confusionReason = o.optString("confusionReason", "")
                )
                result[info.scientificName] = info
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        cache = result
        return result
    }

    fun get(context: Context, scientificName: String): PlantExtraInfo? =
        loadAll(context)[scientificName]

    fun clearCache() { cache = null }

    private fun saveAll(context: Context, map: MutableMap<String, PlantExtraInfo>) {
        val arr = JSONArray()
        map.values.forEach { info ->
            val o = JSONObject()
            o.put("scientificName",  info.scientificName)
            o.put("toxicDogs",       info.toxicDogs)
            o.put("toxicCats",       info.toxicCats)
            o.put("toxicHorses",     info.toxicHorses)
            o.put("toxicCattle",     info.toxicCattle)
            o.put("toxicChildren",   info.toxicChildren)
            o.put("flowerColor",     info.flowerColor)
            o.put("fruitColor",      info.fruitColor)
            val confArr = JSONArray()
            info.confusableWith.forEach { confArr.put(it) }
            o.put("confusableWith",  confArr)
            o.put("confusionReason", info.confusionReason)
            arr.put(o)
        }
        getFile(context).writeText(arr.toString(2), Charsets.UTF_8)
    }

    private fun updateField(context: Context, scientificName: String, block: PlantExtraInfo.() -> PlantExtraInfo) {
        val map  = loadAll(context)
        val info = map[scientificName] ?: PlantExtraInfo(scientificName = scientificName)
        map[scientificName] = info.block()
        saveAll(context, map)
        cache = map
    }

    fun setToxicDogs    (context: Context, name: String, v: Boolean) = updateField(context, name) { copy(toxicDogs     = v) }
    fun setToxicCats    (context: Context, name: String, v: Boolean) = updateField(context, name) { copy(toxicCats     = v) }
    fun setToxicHorses  (context: Context, name: String, v: Boolean) = updateField(context, name) { copy(toxicHorses   = v) }
    fun setToxicCattle  (context: Context, name: String, v: Boolean) = updateField(context, name) { copy(toxicCattle   = v) }
    fun setToxicChildren(context: Context, name: String, v: Boolean) = updateField(context, name) { copy(toxicChildren = v) }
    fun setFruitColor   (context: Context, name: String, v: String)  = updateField(context, name) { copy(fruitColor    = v) }
    fun setFlowerColor  (context: Context, name: String, v: String)  = updateField(context, name) { copy(flowerColor   = v) }
}
