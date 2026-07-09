package com.toxicplants.database.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

/**
 * WikiImageFetcher mejorado.
 *
 * Busca imágenes de plantas en múltiples fuentes (por orden de prioridad):
 *  1. Wikipedia REST API  (en / es)
 *  2. MediaWiki API pageimages  (en / es)
 *  3. Wikimedia Commons (búsqueda + fileinfo)
 *  4. iNaturalist API
 *  5. Encyclopedia of Life (EOL)
 *  6. Fallback: solo el género
 */
object WikiImageFetcher {

    // ─── Fuente principal de búsqueda pública ───────────────────────────
    suspend fun getImageUrl(name: String, allowGenusFallback: Boolean = false): String =
        withContext(Dispatchers.IO) {
            if (name.isBlank()) return@withContext ""
            try {
                val slug = name.trim().replace(" ", "_")

                // 1. Wikipedia REST API en / es
                for (lang in listOf("en", "es")) {
                    val url = fromWikipediaRest(slug, lang)
                    if (url.isNotBlank()) return@withContext url
                }

                // 2. MediaWiki pageimages en / es
                for (lang in listOf("en", "es")) {
                    val url = fromMediaWikiPageimages(slug, lang)
                    if (url.isNotBlank()) return@withContext url
                }

                // 3. Wikimedia Commons
                val commonsUrl = fromWikimediaCommons(name.trim())
                if (commonsUrl.isNotBlank()) return@withContext commonsUrl

                // 4. iNaturalist
                val inatUrl = fromINaturalist(name.trim())
                if (inatUrl.isNotBlank()) return@withContext inatUrl

                // 5. EOL
                val eolUrl = fromEOL(name.trim())
                if (eolUrl.isNotBlank()) return@withContext eolUrl

                // 6. Fallback opcional: solo género.
                //    Desactivado por defecto para evitar repetir la misma foto en especies
                //    distintas de un mismo género.
                val genus = name.trim().split(" ").firstOrNull() ?: ""
                if (allowGenusFallback && genus.isNotBlank() && genus != slug) {
                    for (lang in listOf("en", "es")) {
                        val url = fromWikipediaRest(genus, lang)
                        if (url.isNotBlank()) return@withContext url
                    }
                    val inatGenus = fromINaturalist(genus)
                    if (inatGenus.isNotBlank()) return@withContext inatGenus
                }

                ""
            } catch (e: Exception) {
                ""
            }
        }

    // ────────────────────── Wikipedia REST API ──────────────────────────
    private fun fromWikipediaRest(slug: String, lang: String): String {
        return try {
            val conn = URL("https://$lang.wikipedia.org/api/rest_v1/page/summary/$slug")
                .openConnection()
            conn.setRequestProperty("User-Agent", "PlantasToxicasApp/2.0")
            conn.connectTimeout = 10_000
            conn.readTimeout    = 10_000
            val json = JSONObject(conn.getInputStream().bufferedReader().readText())
            // Preferir imagen original → thumbnail
            for (key in listOf("originalimage", "thumbnail")) {
                if (json.has(key)) {
                    val src = json.getJSONObject(key).optString("source", "")
                    if (src.isNotBlank()) return normalizeUrl(src)
                }
            }
            ""
        } catch (e: Exception) { "" }
    }

    // ────────────────────── MediaWiki pageimages ─────────────────────────
    private fun fromMediaWikiPageimages(slug: String, lang: String): String {
        return try {
            val encoded = URLEncoder.encode(slug.replace("_", " "), "UTF-8")
            val apiUrl  = "https://$lang.wikipedia.org/w/api.php?" +
                    "action=query&titles=$encoded&prop=pageimages" +
                    "&format=json&pithumbsize=800"
            val conn = URL(apiUrl).openConnection()
            conn.setRequestProperty("User-Agent", "PlantasToxicasApp/2.0")
            conn.connectTimeout = 10_000
            conn.readTimeout    = 10_000
            val json  = JSONObject(conn.getInputStream().bufferedReader().readText())
            val pages = json.getJSONObject("query").getJSONObject("pages")
            val page  = pages.getJSONObject(pages.keys().next())
            val src   = page.optJSONObject("thumbnail")?.optString("source", "") ?: ""
            if (src.isNotBlank()) normalizeUrl(src) else ""
        } catch (e: Exception) { "" }
    }

    // ───────────────────── Wikimedia Commons ────────────────────────────
    private fun fromWikimediaCommons(name: String): String {
        return try {
            val encoded = URLEncoder.encode(name, "UTF-8")
            val searchUrl = "https://commons.wikimedia.org/w/api.php?" +
                    "action=query&list=search&srsearch=$encoded" +
                    "&srnamespace=6&format=json&srlimit=3"
            val conn = URL(searchUrl).openConnection()
            conn.setRequestProperty("User-Agent", "PlantasToxicasApp/2.0")
            conn.connectTimeout = 10_000
            conn.readTimeout    = 10_000
            val json    = JSONObject(conn.getInputStream().bufferedReader().readText())
            val results = json.getJSONObject("query").getJSONArray("search")
            for (i in 0 until results.length()) {
                val title = results.getJSONObject(i).optString("title", "")
                if (title.startsWith("File:")) {
                    val fileUrl = getCommonsFileUrl(title)
                    if (fileUrl.isNotBlank()) return fileUrl
                }
            }
            ""
        } catch (e: Exception) { "" }
    }

    private fun getCommonsFileUrl(fileTitle: String): String {
        return try {
            val encoded = URLEncoder.encode(fileTitle, "UTF-8")
            val apiUrl  = "https://commons.wikimedia.org/w/api.php?" +
                    "action=query&titles=$encoded&prop=imageinfo" +
                    "&iiprop=url&format=json"
            val conn = URL(apiUrl).openConnection()
            conn.setRequestProperty("User-Agent", "PlantasToxicasApp/2.0")
            conn.connectTimeout = 10_000
            conn.readTimeout    = 10_000
            val json  = JSONObject(conn.getInputStream().bufferedReader().readText())
            val pages = json.getJSONObject("query").getJSONObject("pages")
            val page  = pages.getJSONObject(pages.keys().next())
            val info  = page.optJSONArray("imageinfo") ?: return ""
            info.getJSONObject(0).optString("url", "")
        } catch (e: Exception) { "" }
    }

    // ───────────────────────── iNaturalist ──────────────────────────────
    private fun fromINaturalist(name: String): String {
        return try {
            val encoded = URLEncoder.encode(name, "UTF-8")
            val apiUrl  = "https://api.inaturalist.org/v1/search?" +
                    "q=$encoded&sources=taxa&per_page=5"
            val conn = URL(apiUrl).openConnection()
            conn.setRequestProperty("User-Agent", "PlantasToxicasApp/2.0")
            conn.connectTimeout = 12_000
            conn.readTimeout    = 12_000
            val json    = JSONObject(conn.getInputStream().bufferedReader().readText())
            val results = json.optJSONArray("results") ?: return ""
            val canonicalQuery = canonicalBinomial(name).lowercase()
            val genus   = canonicalQuery.split(" ").firstOrNull()?.lowercase() ?: ""
            val needsExactSpecies = canonicalQuery.contains(" ")
            for (i in 0 until results.length()) {
                val taxon = results.getJSONObject(i).optJSONObject("taxon") ?: continue
                val taxonName = canonicalBinomial(taxon.optString("name", "")).lowercase()
                if (needsExactSpecies) {
                    if (taxonName != canonicalQuery) continue
                } else if (genus.isNotBlank() && !taxonName.startsWith(genus)) {
                    continue
                }
                val photo  = taxon.optJSONObject("default_photo") ?: continue
                val medUrl = photo.optString("medium_url", "")
                if (medUrl.isNotBlank()) return medUrl
                val sqUrl  = photo.optString("square_url", "")
                if (sqUrl.isNotBlank()) return sqUrl
            }
            ""
        } catch (e: Exception) { "" }
    }

    // ─────────────────── Encyclopedia of Life (EOL) ─────────────────────
    private fun fromEOL(name: String): String {
        return try {
            val encoded = URLEncoder.encode(name, "UTF-8")
            val searchUrl = "https://eol.org/api/search/1.0.json?q=$encoded&page=1&exact=true"
            val conn = URL(searchUrl).openConnection()
            conn.setRequestProperty("User-Agent", "PlantasToxicasApp/2.0")
            conn.connectTimeout = 10_000
            conn.readTimeout    = 10_000
            val json    = JSONObject(conn.getInputStream().bufferedReader().readText())
            val results = json.optJSONArray("results") ?: return ""
            if (results.length() == 0) return ""
            val taxonId = results.getJSONObject(0).optInt("id", 0)
            if (taxonId == 0) return ""

            val pageUrl = "https://eol.org/api/pages/1.0/$taxonId.json" +
                    "?images_per_page=1&language=es"
            val conn2 = URL(pageUrl).openConnection()
            conn2.setRequestProperty("User-Agent", "PlantasToxicasApp/2.0")
            conn2.connectTimeout = 10_000
            conn2.readTimeout    = 10_000
            val json2 = JSONObject(conn2.getInputStream().bufferedReader().readText())
            val dataObjects = json2.optJSONObject("taxonConcept")
                ?.optJSONArray("dataObjects") ?: return ""
            for (i in 0 until dataObjects.length()) {
                val obj = dataObjects.getJSONObject(i)
                if (obj.optString("dataType", "").contains("StillImage")) {
                    val url = obj.optString("mediaURL", "")
                    if (url.isNotBlank()) return url
                }
            }
            ""
        } catch (e: Exception) { "" }
    }

    // ─────────────────── Normalizar nombre científico ───────────────────
    private fun canonicalBinomial(name: String): String {
        val tokens = name
            .replace('×', 'x')
            .replace(Regex("[,;:()\\[\\]{}]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .split(" ")
            .map { it.trim('.', '\'', '"') }
            .filter { it.isNotBlank() }

        if (tokens.isEmpty()) return ""
        val genus = tokens[0]
        val second = tokens.getOrNull(1).orEmpty()
        val third = tokens.getOrNull(2).orEmpty()
        val bad = setOf("sp", "spp", "species", "cf", "aff")

        return when {
            second.equals("x", ignoreCase = true) && third.length > 1 ->
                "$genus x ${third.lowercase()}"
            second.length > 1 && second.firstOrNull()?.isLowerCase() == true && second.lowercase() !in bad ->
                "$genus ${second.lowercase()}"
            else -> genus
        }
    }

    // ─────────────────── Normalizar URL de thumbnail ────────────────────
    /**
     * Convierte URLs de thumbnail de Wikimedia en URL directa.
     * Ejemplo:
     *   .../thumb/a/b/Foto.jpg/300px-Foto.jpg  →  .../a/b/Foto.jpg
     */
    private fun normalizeUrl(url: String): String {
        val thumbMarker = "/wikipedia/commons/thumb/"
        if (url.contains(thumbMarker)) {
            val parts = url.substringAfter(thumbMarker).split("/")
            if (parts.size >= 3) {
                val base = "https://upload.wikimedia.org/wikipedia/commons"
                return "$base/${parts[0]}/${parts[1]}/${parts[2]}"
            }
        }
        return url
    }
}
