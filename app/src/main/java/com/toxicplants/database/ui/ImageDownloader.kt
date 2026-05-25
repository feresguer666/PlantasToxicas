package com.toxicplants.database.ui

import android.content.Context
import android.net.Uri
import com.toxicplants.database.PlantEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

object ImageDownloader {

    data class DownloadProgress(
        val total: Int,
        val current: Int,
        val plantName: String,
        val success: Int,
        val failed: Int
    )

    // ✅ MÚLTIPLES FUENTES DE IMÁGENES
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun downloadAll(
        context: Context,
        plants: List<PlantEntity>,
        onProgress: (DownloadProgress) -> Unit
    ): Pair<Int, Int> {
        var success = 0
        var failed = 0

        for ((index, plant) in plants.withIndex()) {
            withContext(Dispatchers.Main) {
                onProgress(
                    DownloadProgress(
                        total = plants.size,
                        current = index + 1,
                        plantName = plant.commonName,
                        success = success,
                        failed = failed
                    )
                )
            }

            if (LocalImageCache.hasLocalImage(context, plant.id)) {
                success++
                continue
            }

            // ✅ INTENTAR MÚLTIPLES FUENTES
            var downloaded = tryMultipleSources(context, plant)

            if (downloaded) {
                success++
            } else {
                failed++
            }
        }

        return Pair(success, failed)
    }

    // ✅ FUNCIÓN MEJORADA CON MÚLTIPLES FUENTES
    private suspend fun tryMultipleSources(context: Context, plant: PlantEntity): Boolean {
        // Fuentes organizadas por prioridad
        val sources = listOf(
            // 1. Imagen local ya guardada
            SourceInfo("Local", 0) { _, _ ->
                if (LocalImageCache.hasLocalImage(context, plant.id)) {
                    LocalImageCache.getLocalImagePath(context, plant.id)
                } else null
            },

            // 2. WikiImageFetcher (PlantNet/Wikipedia)
            SourceInfo("Wiki", 1) { _, _ ->
                WikiImageFetcher.getImageUrl(plant.scientificName).takeIf { it.isNotBlank() }
            },

            // 3. Nombre común en Wikipedia
            SourceInfo("WikiComún", 2) { _, _ ->
                if (plant.commonName != plant.scientificName) {
                    WikiImageFetcher.getImageUrl(plant.commonName).takeIf { it.isNotBlank() }
                } else null
            },

            // 4. Wikimedia Commons API
            SourceInfo("Commons", 3) { _, _ ->
                fetchFromWikimediaCommons(plant.scientificName)
            },

            // 5. Wikimedia Commons con nombre común
            SourceInfo("CommonsComún", 4) { _, _ ->
                if (plant.commonName != plant.scientificName) {
                    fetchFromWikimediaCommons(plant.commonName)
                } else null
            },

            // 6. BioStor API
            SourceInfo("BioStor", 5) { _, _ ->
                fetchFromBioStor(plant.scientificName)
            },

            // 7. Encyclopedia of Life
            SourceInfo("EOL", 6) { _, _ ->
                fetchFromEncyclopediaOfLife(plant.scientificName)
            },

            // 8. iNaturalist
            SourceInfo("iNat", 7) { _, _ ->
                fetchFromINaturalist(plant.scientificName)

            }
        )

        for (source in sources) {
            try {
                val imageUrl = source.getUrl(context, plant)
                if (imageUrl != null && imageUrl.isNotBlank()) {
                    val downloaded = LocalImageCache.downloadAndSave(context, plant.id, imageUrl)
                    if (downloaded) {
                        return true
                    }
                }
            } catch (e: Exception) {
                // Continuar con la siguiente fuente
            }
        }

        return false
    }

    // ✅ WIKIMEDIA COMMONS API
    private fun fetchFromWikimediaCommons(searchName: String): String? {
        return try {
            val encodedName = Uri.encode(searchName)
            val url = "https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=$encodedName&format=json&srlimit=5&origin=*"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "PlantasToxicas/1.0")
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: return null

            val json = JSONObject(body)
            val searchResults = json.optJSONObject("query")?.optJSONArray("search")

            if (searchResults != null) {
                for (i in 0 until searchResults.length()) {
                    val result = searchResults.getJSONObject(i)
                    val title = result.getString("title")

                    // Obtener la imagen de la página
                    val imageUrl = getImageFromWikimediaPage(title)
                    if (imageUrl != null) return imageUrl
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun getImageFromWikimediaPage(pageTitle: String): String? {
        return try {
            val encodedTitle = Uri.encode(pageTitle)
            val url = "https://en.wikipedia.org/w/api.php?action=query&titles=$encodedTitle&prop=pageimages&format=json&pithumbsize=500&origin=*"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "PlantasToxicas/1.0")
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: return null

            val json = JSONObject(body)
            val pages = json.optJSONObject("query")?.optJSONObject("pages")

            if (pages != null) {
                val pageIds = pages.keys()
                while (pageIds.hasNext()) {
                    val pageId = pageIds.next()
                    val page = pages.getJSONObject(pageId)
                    val thumbnail = page.optJSONObject("thumbnail")
                    if (thumbnail != null) {
                        return thumbnail.getString("source")
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    // ✅ BIOSTOR API
    private fun fetchFromBioStor(searchName: String): String? {
        return try {
            val encodedName = Uri.encode(searchName)
            val url = "https://biostor.org/reference/$encodedName"
            // BioStor no tiene API pública fácil, retornar null
            null
        } catch (e: Exception) {
            null
        }
    }

    // ✅ ENCYCLOPEDIA OF LIFE
    private fun fetchFromEncyclopediaOfLife(searchName: String): String? {
        return try {
            val encodedName = Uri.encode(searchName)
            val url = "https://api.eol.org/api/v2/search?q=$encodedName&key=INFO&page=1&per_page=5"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "PlantasToxicas/1.0")
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: return null

            val json = JSONObject(body)
            val results = json.optJSONArray("results")

            if (results != null && results.length() > 0) {
                for (i in 0 until results.length()) {
                    val result = results.getJSONObject(i)
                    val title = result.optString("title", "")
                    if (title.contains(searchName.split(" ").first(), ignoreCase = true)) {
                        // Intentar obtener imagen del taxón
                        val taxonId = result.optInt("id", 0)
                        if (taxonId > 0) {
                            val imageUrl = fetchEOLImage(taxonId)
                            if (imageUrl != null) return imageUrl
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchEOLImage(taxonId: Int): String? {
        return try {
            val url = "https://api.eol.org/api/v2/pages/$taxonId?images_per_page=1&language=es&key=INFO"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "PlantasToxicas/1.0")
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: return null

            val json = JSONObject(body)
            val dataObjects = json.optJSONArray("dataObjects")

            if (dataObjects != null && dataObjects.length() > 0) {
                val dataObj = dataObjects.getJSONObject(0)
                return dataObj.optString("mediaURL", null)
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    // ✅ INATURALIST API
    private fun fetchFromINaturalist(searchName: String): String? {
        return try {
            val encodedName = Uri.encode(searchName)
            val url = "https://api.inaturalist.org/v1/search?q=$encodedName&sources=taxa&per_page=5"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "PlantasToxicas/1.0")
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: return null

            val json = JSONObject(body)
            val results = json.optJSONArray("results")

            if (results != null) {
                for (i in 0 until results.length()) {
                    val result = results.getJSONObject(i)
                    val taxon = result.optJSONObject("taxon")
                    if (taxon != null) {
                        val defaultPhoto = taxon.optJSONObject("default_photo")
                        if (defaultPhoto != null) {
                            return defaultPhoto.optString("medium_url", null)
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private data class SourceInfo(
        val name: String,
        val priority: Int,
        val getUrl: suspend (Context, PlantEntity) -> String?
    )

    private fun extractFileName(url: String): String {
        if (url.isBlank()) return ""

        val lower = url.lowercase()
        if (lower.contains("gstatic.com") || lower.contains("googleusercontent.com")) {
            return ""
        }

        val marker = "/wikipedia/commons/thumb/"
        return if (url.contains(marker)) {
            val parts = url.substringAfter(marker).split("/")
            if (parts.size >= 3) parts[2] else ""
        } else if (url.contains("/wikipedia/commons/")) {
            url.substringAfterLast("/")
        } else {
            ""
        }
    }
}