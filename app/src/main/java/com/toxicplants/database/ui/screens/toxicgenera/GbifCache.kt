package com.toxicplants.database.ui.screens.toxicgenera

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class GbifCacheEntry(
    val timestamp: Long,
    val species: List<GbifSpeciesCache>
)

@Serializable
data class GbifSpeciesCache(
    val scientificName: String,
    val canonical: String,
    val key: Long? = null
)

object GbifCache {
    private val json = Json { ignoreUnknownKeys = true }
    private const val TTL_MS = 7L * 24 * 60 * 60 * 1000 // 7 días

    private fun fileFor(context: Context, genus: String): File {
        val dir = File(context.filesDir, "gbif_cache").apply { mkdirs() }
        return File(dir, genus.lowercase().replace(Regex("[^a-z0-9]"), "_") + ".json")
    }

    fun get(context: Context, genus: String): List<GbifSpecies>? {
        val f = fileFor(context, genus)
        if (!f.exists()) return null
        return try {
            val entry = json.decodeFromString<GbifCacheEntry>(f.readText())
            if (System.currentTimeMillis() - entry.timestamp > TTL_MS) return null
            entry.species.map { GbifSpecies(it.scientificName, it.canonical, it.key) }
        } catch (_: Exception) {
            null
        }
    }

    fun put(context: Context, genus: String, species: List<GbifSpecies>) {
        try {
            val entry = GbifCacheEntry(
                System.currentTimeMillis(),
                species.map { GbifSpeciesCache(it.scientificName, it.canonical, it.key) }
            )
            fileFor(context, genus).writeText(json.encodeToString(entry))
        } catch (_: Exception) {
        }
    }
}