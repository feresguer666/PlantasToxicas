package com.toxicplants.database.ui.gbif

import com.google.gson.annotations.SerializedName

object GBIFConstants {
    const val BASE_URL = "https://api.gbif.org/v1/"
    const val SPECIES_MATCH = "species/match"
    const val SPECIES_DETAIL = "species"
    const val OCCURRENCE_SEARCH = "occurrence/search"
}

data class GBIFSpeciesMatch(
    @SerializedName("matchType") val matchType: String?,
    @SerializedName("confidence") val confidence: Int?,
    @SerializedName("usageKey") val usageKey: Long?,
    @SerializedName("scientificName") val scientificName: String?,
    @SerializedName("canonicalName") val canonicalName: String?,
    @SerializedName("rank") val rank: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("kingdom") val kingdom: String?,
    @SerializedName("phylum") val phylum: String?,
    @SerializedName("order") val order: String?,
    @SerializedName("family") val family: String?,
    @SerializedName("genus") val genus: String?,
    @SerializedName("synonym") val synonym: Boolean?
)

data class GBIFVernacularName(
    @SerializedName("language") val language: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("countryCode") val countryCode: String?
)

data class GBIFOccurrence(
    @SerializedName("key") val key: Long?,
    @SerializedName("speciesKey") val speciesKey: Long?,
    @SerializedName("scientificName") val scientificName: String?,
    @SerializedName("decimalLatitude") val decimalLatitude: Double?,
    @SerializedName("decimalLongitude") val decimalLongitude: Double?,
    @SerializedName("countryCode") val countryCode: String?,
    @SerializedName("eventDate") val eventDate: String?,
    @SerializedName("datasetName") val datasetName: String?
)