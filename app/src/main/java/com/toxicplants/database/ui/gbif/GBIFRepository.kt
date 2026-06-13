package com.toxicplants.database.ui.gbif

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class GBIFRepository(
    private val okHttpClient: OkHttpClient = OkHttpClient()
) {
    private val baseUrl = GBIFConstants.BASE_URL
    private val gson = Gson()
    
    suspend fun matchSpecies(scientificName: String): Result<GBIFSpeciesMatch> = 
        withContext(Dispatchers.IO) {
            try {
                val url = "$baseUrl${GBIFConstants.SPECIES_MATCH}?name=$scientificName"
                val request = Request.Builder().url(url).build()
                val response = okHttpClient.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Error HTTP: ${response.code}"))
                }
                
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Sin respuesta"))
                val match = gson.fromJson(body, GBIFSpeciesMatch::class.java)
                Result.success(match)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    suspend fun getVernacularNames(gbifKey: Long): Result<List<GBIFVernacularName>> = 
        withContext(Dispatchers.IO) {
            try {
                val url = "$baseUrl${GBIFConstants.SPECIES_DETAIL}/$gbifKey/vernacularNames"
                val request = Request.Builder().url(url).build()
                val response = okHttpClient.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Error HTTP: ${response.code}"))
                }
                
                val body = response.body?.string() ?: "{}"
                val names = mutableListOf<GBIFVernacularName>()
                
                try {
                    val json = gson.fromJson(body, Map::class.java)
                    @Suppress("UNCHECKED_CAST")
                    (json["results"] as? List<Map<String, Any>>)?.forEach { item ->
                        names.add(
                            GBIFVernacularName(
                                language = item["language"] as? String,
                                name = item["name"] as? String,
                                countryCode = item["countryCode"] as? String
                            )
                        )
                    }
                } catch (e: Exception) { }
                
                Result.success(names)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    suspend fun getOccurrences(taxonKey: Long, limit: Int = 100): Result<List<GBIFOccurrence>> = 
        withContext(Dispatchers.IO) {
            try {
                val url = "$baseUrl${GBIFConstants.OCCURRENCE_SEARCH}?taxonKey=$taxonKey&limit=$limit&hasCoordinate=true"
                val request = Request.Builder().url(url).build()
                val response = okHttpClient.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Error HTTP: ${response.code}"))
                }
                
                val body = response.body?.string() ?: "{}"
                val occurrences = mutableListOf<GBIFOccurrence>()
                
                try {
                    val json = gson.fromJson(body, Map::class.java)
                    @Suppress("UNCHECKED_CAST")
                    (json["results"] as? List<Map<String, Any>>)?.forEach { item ->
                        occurrences.add(
                            GBIFOccurrence(
                                key = (item["key"] as? Number)?.toLong(),
                                speciesKey = (item["speciesKey"] as? Number)?.toLong(),
                                scientificName = item["scientificName"] as? String,
                                decimalLatitude = (item["decimalLatitude"] as? Number)?.toDouble(),
                                decimalLongitude = (item["decimalLongitude"] as? Number)?.toDouble(),
                                countryCode = item["countryCode"] as? String,
                                eventDate = item["eventDate"] as? String,
                                datasetName = item["datasetName"] as? String
                            )
                        )
                    }
                } catch (e: Exception) { }
                
                Result.success(occurrences)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}