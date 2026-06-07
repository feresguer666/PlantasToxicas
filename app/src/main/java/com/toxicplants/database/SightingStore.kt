package com.toxicplants.database

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SightingStore {
    private const val PREFS = "sightings_history_prefs"
    private const val KEY = "sightings_json_v1"

    fun photoDir(context: Context): File = File(context.filesDir, "sighting_photos").apply {
        if (!exists()) mkdirs()
    }

    fun load(context: Context): List<SightingEntity> {
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return emptyList()
        return runCatching { parse(JSONArray(json)) }.getOrDefault(emptyList())
    }

    fun save(context: Context, items: List<SightingEntity>) {
        val arr = JSONArray()
        items.sortedByDescending { it.date }.forEach { arr.put(it.toJson()) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, arr.toString()).apply()
    }

    fun copyPhotoToInternal(context: Context, uri: Uri): String {
        val name = "sighting_${System.currentTimeMillis()}.jpg"
        val file = File(photoDir(context), name)
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        return if (file.exists() && file.length() > 0) file.absolutePath else ""
    }

    fun deletePhoto(path: String) {
        if (path.isNotBlank()) runCatching { File(path).delete() }
    }

    fun nowString(): String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

    private fun parse(arr: JSONArray): List<SightingEntity> {
        val out = ArrayList<SightingEntity>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out += SightingEntity(
                id = o.optInt("id", 0),
                type = o.optString("type", "Planta"),
                itemId = if (o.isNull("itemId")) null else o.optInt("itemId"),
                commonName = o.optString("commonName", ""),
                scientificName = o.optString("scientificName", ""),
                toxicityLevel = o.optString("toxicityLevel", ""),
                latitude = if (o.isNull("latitude")) null else o.optDouble("latitude"),
                longitude = if (o.isNull("longitude")) null else o.optDouble("longitude"),
                locationName = o.optString("locationName", ""),
                notes = o.optString("notes", ""),
                photoPath = o.optString("photoPath", ""),
                date = o.optString("date", "")
            )
        }
        return out
    }

    private fun SightingEntity.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("type", type)
        if (itemId == null) put("itemId", JSONObject.NULL) else put("itemId", itemId)
        put("commonName", commonName)
        put("scientificName", scientificName)
        put("toxicityLevel", toxicityLevel)
        if (latitude == null) put("latitude", JSONObject.NULL) else put("latitude", latitude)
        if (longitude == null) put("longitude", JSONObject.NULL) else put("longitude", longitude)
        put("locationName", locationName)
        put("notes", notes)
        put("photoPath", photoPath)
        put("date", date)
    }
}
