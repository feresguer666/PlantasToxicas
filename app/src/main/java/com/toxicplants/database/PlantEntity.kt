package com.toxicplants.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plants")
data class PlantEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val commonName: String,
    /** Nombres comunes / populares / regionales adicionales, separados por comas. Ej: "belladona, tabaco bordo". */
    val commonNames: String = "",
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
    val notes: String? = null,

    // ✅ CAMPOS DE FENOLOGÍA (meses como números separados por coma, ej: "3,4,5,6")
    val floweringMonths: String = "",
    val fruitingMonths: String = "",
    val maxToxicityMonths: String = "",

    // ✅ CAMPO CULTURAL (Historia, Mitos y Curiosidades)
    val mythsAndLegends: String = ""
)