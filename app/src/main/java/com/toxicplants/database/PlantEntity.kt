package com.toxicplants.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plants")
data class PlantEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val commonName: String,
    val scientificName: String,
    val family: String,
    val toxicityLevel: String,
    val toxicParts: String,
    val symptoms: String,
    val description: String,
    val habitat: String,
    val geographicDistribution: String,
    val firstAid: String,
    val imageUrl: String,
    val isFavorite: Boolean = false,
    val category: String,

    // ✅ NUEVOS CAMPOS DE UBICACIÓN
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String? = null,
    val foundDate: String? = null,
    val notes: String? = null
)