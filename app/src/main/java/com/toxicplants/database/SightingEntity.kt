package com.toxicplants.database

/** Avistamiento local de planta, seta, liquen u otro organismo. */
data class SightingEntity(
    val id: Int = 0,
    val type: String = "Planta",
    val itemId: Int? = null,
    val commonName: String = "",
    val scientificName: String = "",
    val toxicityLevel: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String = "",
    val notes: String = "",
    val photoPath: String = "",
    val date: String = ""
)
