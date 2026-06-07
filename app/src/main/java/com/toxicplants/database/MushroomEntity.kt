package com.toxicplants.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Seta u hongo tóxico del catálogo micológico.
 *
 * Se separa de PlantEntity porque las intoxicaciones por setas se clasifican por
 * síndromes clínicos, latencia y toxinas fúngicas, no por familia botánica.
 */
@Entity(
    tableName = "mushrooms",
    indices = [
        Index("scientificName"),
        Index("toxicityLevel"),
        Index("syndrome")
    ]
)
data class MushroomEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val commonName: String,
    val scientificName: String,
    val family: String = "",
    val toxicityLevel: String,
    val syndrome: String,
    val toxicCompounds: String = "",
    val onsetTime: String = "",
    val symptoms: String = "",
    val description: String = "",
    val habitat: String = "",
    val season: String = "",
    val geographicDistribution: String = "",
    val edibleConfusions: String = "",
    val firstAid: String = "",
    val treatment: String = "",
    val notes: String = "",
    val imageUrl: String = "",
    val isDeadly: Boolean = false,
    val isFavorite: Boolean = false,
)
