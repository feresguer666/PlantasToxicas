package com.toxicplants.database

/** Liquen tóxico o potencialmente irritante del catálogo liquenológico. */
data class LichenEntity(
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
    val geographicDistribution: String = "",
    val confusions: String = "",
    val firstAid: String = "",
    val treatment: String = "",
    val notes: String = "",
    val imageUrl: String = "",
    val isHighRisk: Boolean = false,
    val isFavorite: Boolean = false,
)
