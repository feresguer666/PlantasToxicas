package com.toxicplants.database.ui

import android.content.Context
import android.net.Uri
import com.toxicplants.database.PlantEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder
import java.util.Locale
import java.util.concurrent.TimeUnit

object ImageDownloader {

    private const val FAILED_PREFS = "image_download_failures"
    private const val FAILED_IDS_KEY = "failed_plant_ids"

    data class DownloadProgress(
        val total: Int,
        val current: Int,
        val plantName: String,
        val success: Int,
        val failed: Int
    )

    private data class TaxonQuery(
        val raw: String,
        val canonical: String,
        val genus: String,
        val epithet: String?,
        val isSpecies: Boolean,
        val isHybrid: Boolean = false
    )

    private data class ImageSource(
        val name: String,
        val getUrls: (PlantEntity) -> List<String>
    )

    private data class DownloadedImage(
        val url: String,
        val source: String
    )

    private const val USER_AGENT =
        "PlantasToxicas/2.0 (Android; botanical image resolver; contact: app-local)"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    // GBIF puede tardar bastante en búsquedas multimedia; usamos tiempos más cortos
    // para no bloquear una descarga masiva durante demasiados segundos por planta.
    private val gbifClient = httpClient.newBuilder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    fun getFailedPlantIds(context: Context): Set<Int> =
        context.getSharedPreferences(FAILED_PREFS, Context.MODE_PRIVATE)
            .getStringSet(FAILED_IDS_KEY, emptySet())
            .orEmpty()
            .mapNotNull { it.toIntOrNull() }
            .toSet()

    fun failedPlantCount(context: Context): Int = getFailedPlantIds(context).size

    fun clearFailedPlants(context: Context) {
        context.getSharedPreferences(FAILED_PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(FAILED_IDS_KEY)
            .apply()
    }

    private fun markImageResult(context: Context, plantId: Int, failed: Boolean) {
        val prefs = context.getSharedPreferences(FAILED_PREFS, Context.MODE_PRIVATE)
        val ids = prefs.getStringSet(FAILED_IDS_KEY, emptySet())
            .orEmpty()
            .toMutableSet()
        if (failed) ids += plantId.toString() else ids -= plantId.toString()
        prefs.edit().putStringSet(FAILED_IDS_KEY, ids).apply()
    }

    /**
     * Descarga imágenes para una lista de plantas.
     *
     * El modo normal conserva las imágenes ya guardadas. Con [overwriteExisting] se intenta
     * resolver de nuevo cada planta y, si se encuentra una foto mejor, se sobrescribe. Si no se
     * encuentra nada, la foto antigua no se borra.
     *
     * Además mantiene una lista persistente de plantas fallidas para poder usar
     * "Reintentar solo fallidas" sin recorrer todo el catálogo otra vez.
     */
    suspend fun downloadAll(
        context: Context,
        plants: List<PlantEntity>,
        overwriteExisting: Boolean = false,
        rescueMode: Boolean = false,
        onProgress: (DownloadProgress) -> Unit
    ): Pair<Int, Int> {
        var success = 0
        var failed = 0

        // Evita que en una misma descarga masiva se asigne exactamente la misma URL remota
        // a especies distintas. Es una defensa extra contra fotos genéricas por nombre común.
        val usedRemoteUrls = mutableSetOf<String>()

        for ((index, plant) in plants.withIndex()) {
            withContext(Dispatchers.Main) {
                onProgress(
                    DownloadProgress(
                        total = plants.size,
                        current = index + 1,
                        plantName = displayName(plant),
                        success = success,
                        failed = failed
                    )
                )
            }

            if (!overwriteExisting && LocalImageCache.hasLocalImage(context, plant.id)) {
                markImageResult(context, plant.id, failed = false)
                success++
                continue
            }

            val downloaded = tryMultipleSources(context, plant, usedRemoteUrls, rescueMode)
            if (downloaded != null || LocalImageCache.hasLocalImage(context, plant.id)) {
                markImageResult(context, plant.id, failed = false)
                success++
            } else {
                markImageResult(context, plant.id, failed = true)
                failed++
            }
        }

        return Pair(success, failed)
    }

    fun forceAiImageUrl(plant: PlantEntity): String =
        AiImageService.generateBotanicalImageUrl(
            plant.scientificName,
            plant.family,
            plant.commonName
        )

    /** Fuerza generación de imagen con IA saltándose la cascada de fuentes reales. */
    suspend fun forceAiImage(context: Context, plant: PlantEntity): Boolean {
        val saved = LocalImageCache.downloadAndSave(context, plant.id, forceAiImageUrl(plant))
        if (saved) markImageResult(context, plant.id, failed = false)
        return saved
    }

    /** Resuelve y guarda la imagen de una sola planta. */
    suspend fun resolveImageUrl(context: Context, plant: PlantEntity): String? {
        if (LocalImageCache.hasLocalImage(context, plant.id)) {
            markImageResult(context, plant.id, failed = false)
            return "file://${LocalImageCache.getLocalImagePath(context, plant.id)}"
        }

        val downloaded = tryMultipleSources(context, plant, mutableSetOf(), rescueMode = false)
        return if (downloaded != null) {
            markImageResult(context, plant.id, failed = false)
            "file://${LocalImageCache.getLocalImagePath(context, plant.id)}"
        } else {
            markImageResult(context, plant.id, failed = true)
            null
        }
    }

    /**
     * Indica si una URL del JSON parece específica de la especie.
     *
     * Muchas entradas del catálogo comparten una foto genérica de ASPCA por nombre común
     * (por ejemplo, "azalea", "aloe", "garlic"). Esas URL se dejan como último recurso,
     * no como primera opción.
     */
    fun isLikelySpeciesSpecificUrl(plant: PlantEntity, imageUrl: String): Boolean {
        val url = imageUrl.trim()
        if (url.isBlank()) return false
        val lower = url.lowercase(Locale.ROOT)
        if (isLocalReference(url)) return true
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) return false
        if (PlantImageHelper.isInvalidUrl(url)) return false

        val query = taxonQuery(plant)
        if (!query.isSpecies) return true

        // Las URLs de ASPCA suelen estar nombradas por el nombre común y se repiten por género.
        if (lower.contains("aspca.org/")) {
            return containsExactTaxon(url, query)
        }

        // Para Wikimedia o ficheros ya generados, si el nombre de archivo contiene el binomio,
        // normalmente es una foto correcta de esa especie.
        return containsExactTaxon(url, query)
    }

    private suspend fun tryMultipleSources(
        context: Context,
        plant: PlantEntity,
        usedRemoteUrls: MutableSet<String>,
        rescueMode: Boolean
    ): DownloadedImage? = withContext(Dispatchers.IO) {
        for (source in imageSources(rescueMode)) {
            val urls = try {
                source.getUrls(plant)
            } catch (_: Exception) {
                emptyList()
            }

            for (url in urls.distinctBy { normalizeRemoteUrl(it) }) {
                if (!isUsableImageUrl(url)) continue

                val normalized = normalizeRemoteUrl(url)
                val isAi = url.contains("pollinations.ai", ignoreCase = true)
                if (!isAi && normalized in usedRemoteUrls) continue

                val saved = try {
                    LocalImageCache.downloadAndSave(context, plant.id, url)
                } catch (_: Exception) {
                    false
                }

                if (saved) {
                    if (!isAi) usedRemoteUrls += normalized
                    return@withContext DownloadedImage(url = url, source = source.name)
                }
            }
        }
        null
    }

    private fun imageSources(rescueMode: Boolean): List<ImageSource> {
        val normalSources = listOf(
            // 1) URL del JSON solo si parece llevar el binomio de la especie.
            ImageSource("JSON específico") { plant ->
                val url = plant.imageUrl.trim()
                if (isLikelySpeciesSpecificUrl(plant, url) && !isLocalReference(url)) listOf(url) else emptyList()
            },

            // 2) iNaturalist exacto: gran cobertura y default_photo por taxón.
            ImageSource("iNaturalist exacto") { plant -> fetchFromINaturalistExact(plant) },

            // 3) Wikidata P18/P225: foto principal asociada al taxón exacto.
            ImageSource("Wikidata exacto") { plant -> fetchFromWikidataExact(plant) },

            // 4) Wikimedia Commons: categoría exacta de la especie.
            ImageSource("Commons categoría exacta") { plant -> fetchFromCommonsCategoryExact(plant) },

            // 5) Wikipedia por nombre científico exacto (en + es).
            ImageSource("Wikipedia exacta") { plant -> fetchFromWikipediaExact(plant) },

            // 6) Commons con datos estructurados P225.
            ImageSource("Commons P225 exacto") { plant -> fetchFromCommonsStructuredData(plant) },

            // 7) Búsqueda textual en Commons, pero verificando que aparezca el binomio.
            ImageSource("Commons búsqueda exacta") { plant -> fetchFromCommonsSearchExact(plant) },

            // 8) Encyclopedia of Life exacto.
            ImageSource("EOL exacto") { plant -> fetchFromEOLExact(plant) },

            // 9) GBIF con ocurrencias que tengan multimedia (timeout corto).
            ImageSource("GBIF multimedia") { plant -> fetchFromGBIF(plant, aggressive = false) },

            // 10) Nombre común, pero siempre acompañado y verificado con el nombre científico.
            ImageSource("Nombre común verificado") { plant -> fetchFromCommonNameVerified(plant) }
        )

        val rescueSources = if (rescueMode) listOf(
            // Fuentes más lentas y profundas: solo para reintentar fallidas.
            ImageSource("iNaturalist observaciones") { plant -> fetchFromINaturalistObservationsExact(plant) },
            ImageSource("Commons búsqueda profunda") { plant -> fetchFromCommonsSearchDeepExact(plant) },
            ImageSource("GBIF profundo") { plant -> fetchFromGBIF(plant, aggressive = true) }
        ) else emptyList()

        return normalSources + rescueSources + listOf(
            // Último recurso: imagen botánica generada por IA con el nombre científico.
            // Puede estar limitado por el proveedor, por eso se deja al final.
            ImageSource("IA") { plant -> listOf(forceAiImageUrl(plant)) }
        )
    }

    // ───────────────────────────── Fuentes ─────────────────────────────

    private fun fetchFromINaturalistExact(plant: PlantEntity): List<String> {
        val query = taxonQuery(plant)
        if (!query.isSpecies) return emptyList()

        val urls = mutableListOf<String>()

        val taxaUrl = "https://api.inaturalist.org/v1/taxa" +
            "?q=${Uri.encode(query.canonical)}&per_page=10&is_active=true"
        requestJson(taxaUrl)
            ?.optJSONArray("results")
            ?.let { results ->
                forEachJsonObject(results) { taxon ->
                    val name = taxon.optString("name", "")
                    val matched = taxon.optString("matched_term", "")
                    if (!matchesTaxonName(name, query) && !matchesTaxonName(matched, query)) {
                        return@forEachJsonObject
                    }
                    val rank = taxon.optString("rank", "")
                    if (rank.isNotBlank() && rank !in setOf("species", "hybrid", "subspecies", "variety", "form")) {
                        return@forEachJsonObject
                    }
                    addINatPhotoUrls(taxon.optJSONObject("default_photo"), urls)
                }
            }

        if (urls.isNotEmpty()) return urls.distinct()

        // Fallback al endpoint search, también con validación exacta.
        val searchUrl = "https://api.inaturalist.org/v1/search" +
            "?q=${Uri.encode(query.canonical)}&sources=taxa&per_page=10"
        requestJson(searchUrl)
            ?.optJSONArray("results")
            ?.let { results ->
                forEachJsonObject(results) { result ->
                    val taxon = result.optJSONObject("taxon") ?: result.optJSONObject("record") ?: return@forEachJsonObject
                    val name = taxon.optString("name", "")
                    if (!matchesTaxonName(name, query)) return@forEachJsonObject
                    addINatPhotoUrls(taxon.optJSONObject("default_photo"), urls)
                }
            }

        return urls.distinct()
    }

    private fun addINatPhotoUrls(photo: JSONObject?, out: MutableList<String>) {
        if (photo == null) return
        listOf("medium_url", "original_url", "large_url", "square_url", "url").forEach { key ->
            photo.optString(key, "").takeIf { it.isNotBlank() }?.let { url ->
                out += url
                // Muchas fotos de observación solo traen square; probamos tamaños mayores.
                if (url.contains("/square.") || url.contains("square.")) {
                    out += url.replace("/square.", "/medium.").replace("square.", "medium.")
                    out += url.replace("/square.", "/large.").replace("square.", "large.")
                }
            }
        }
    }

    /**
     * iNaturalist profundo: si el taxón no tiene default_photo, busca observaciones
     * de calidad investigación con fotos. Solo se usa en modo rescate para fallidas.
     */
    private fun fetchFromINaturalistObservationsExact(plant: PlantEntity): List<String> {
        val query = taxonQuery(plant)
        if (!query.isSpecies) return emptyList()

        val taxonId = findINaturalistTaxonId(query) ?: return emptyList()
        val out = mutableListOf<String>()
        val url = "https://api.inaturalist.org/v1/observations" +
            "?taxon_id=$taxonId&photos=true&quality_grade=research" +
            "&order_by=votes&per_page=12"

        requestJson(url)
            ?.optJSONArray("results")
            ?.let { results ->
                forEachJsonObject(results) { obs ->
                    val taxon = obs.optJSONObject("taxon")
                    val taxonName = taxon?.optString("name", "").orEmpty()
                    if (taxonName.isNotBlank() && !matchesTaxonName(taxonName, query)) return@forEachJsonObject
                    obs.optJSONArray("photos")?.let { photos ->
                        forEachJsonObject(photos) { photo -> addINatPhotoUrls(photo, out) }
                    }
                }
            }

        return out.distinct()
    }

    private fun findINaturalistTaxonId(query: TaxonQuery): Int? {
        val taxaUrl = "https://api.inaturalist.org/v1/taxa" +
            "?q=${Uri.encode(query.canonical)}&per_page=8&is_active=true"
        val results = requestJson(taxaUrl)?.optJSONArray("results") ?: return null
        for (i in 0 until results.length()) {
            val taxon = results.optJSONObject(i) ?: continue
            val name = taxon.optString("name", "")
            val matched = taxon.optString("matched_term", "")
            if (matchesTaxonName(name, query) || matchesTaxonName(matched, query)) {
                val id = taxon.optInt("id", 0)
                if (id > 0) return id
            }
        }
        return null
    }

    private fun fetchFromWikidataExact(plant: PlantEntity): List<String> {
        val query = taxonQuery(plant)
        if (!query.isSpecies) return emptyList()

        val out = mutableListOf<String>()
        val searchUrl = "https://www.wikidata.org/w/api.php" +
            "?action=wbsearchentities&search=${Uri.encode(query.canonical)}" +
            "&language=en&format=json&limit=8"
        val results = requestJson(searchUrl)?.optJSONArray("search") ?: return emptyList()

        forEachJsonObject(results) { result ->
            val id = result.optString("id", "")
            if (id.isBlank()) return@forEachJsonObject

            val entity = requestJson("https://www.wikidata.org/wiki/Special:EntityData/${Uri.encode(id)}.json")
                ?.optJSONObject("entities")
                ?.optJSONObject(id)
                ?: return@forEachJsonObject

            val taxonName = claimString(entity, "P225")
                ?: result.optString("label", "")
            if (!matchesTaxonName(taxonName, query)) return@forEachJsonObject

            claimString(entity, "P18")
                ?.takeIf { it.isNotBlank() }
                ?.let { fileName -> out += commonsSpecialFilePath(fileName) }

            claimString(entity, "P373")
                ?.takeIf { it.isNotBlank() }
                ?.let { category -> out += fetchCommonsCategoryFiles("Category:$category", query) }
        }

        return out.distinct()
    }

    private fun fetchFromCommonsCategoryExact(plant: PlantEntity): List<String> {
        val query = taxonQuery(plant)
        if (!query.isSpecies) return emptyList()
        return fetchCommonsCategoryFiles("Category:${query.canonical}", query)
    }

    private fun fetchFromWikipediaExact(plant: PlantEntity): List<String> {
        val query = taxonQuery(plant)
        if (!query.isSpecies) return emptyList()

        val out = mutableListOf<String>()
        val slug = Uri.encode(query.canonical.replace(" ", "_"))

        for (lang in listOf("en", "es")) {
            val restUrl = "https://$lang.wikipedia.org/api/rest_v1/page/summary/$slug"
            val json = requestJson(restUrl) ?: continue
            val title = json.optString("title", "")
            val extract = json.optString("extract", "")
            if (!containsExactTaxon("$title $extract", query)) continue

            listOf("originalimage", "thumbnail").forEach { key ->
                json.optJSONObject(key)
                    ?.optString("source", "")
                    ?.takeIf { it.isNotBlank() }
                    ?.let(out::add)
            }
        }

        for (lang in listOf("en", "es")) {
            val apiUrl = "https://$lang.wikipedia.org/w/api.php" +
                "?action=query&titles=${Uri.encode(query.canonical)}&redirects=1" +
                "&prop=pageimages%7Cextracts&exintro=1&explaintext=1" +
                "&format=json&pithumbsize=1000"
            val pages = requestJson(apiUrl)
                ?.optJSONObject("query")
                ?.optJSONObject("pages")
                ?: continue
            val keys = pages.keys()
            while (keys.hasNext()) {
                val page = pages.optJSONObject(keys.next()) ?: continue
                val title = page.optString("title", "")
                val extract = page.optString("extract", "")
                if (!containsExactTaxon("$title $extract", query)) continue
                page.optJSONObject("thumbnail")
                    ?.optString("source", "")
                    ?.takeIf { it.isNotBlank() }
                    ?.let(out::add)
            }
        }

        return out.distinct()
    }

    private fun fetchFromCommonsStructuredData(plant: PlantEntity): List<String> {
        val query = taxonQuery(plant)
        if (!query.isSpecies) return emptyList()
        val srsearch = "haswbstatement:P225=\"${query.canonical}\""
        return fetchCommonsSearch(srsearch, query, requireExactText = false)
    }

    private fun fetchFromCommonsSearchExact(plant: PlantEntity): List<String> {
        val query = taxonQuery(plant)
        if (!query.isSpecies) return emptyList()

        val searches = listOf(
            "\"${query.canonical}\"",
            "${query.canonical} plant",
            query.canonical.replace(" ", "_")
        )

        return searches
            .flatMap { fetchCommonsSearch(it, query, requireExactText = true) }
            .distinct()
    }

    /** Búsqueda más amplia en Commons para fallidas: más resultados y términos morfológicos. */
    private fun fetchFromCommonsSearchDeepExact(plant: PlantEntity): List<String> {
        val query = taxonQuery(plant)
        if (!query.isSpecies) return emptyList()

        val searches = listOf(
            "\"${query.canonical}\"",
            "\"${query.canonical}\" flower",
            "\"${query.canonical}\" fruit",
            "\"${query.canonical}\" leaf",
            "\"${query.canonical}\" plant",
            "${query.canonical.replace(" ", "_")}"
        )

        return searches
            .flatMap { fetchCommonsSearch(it, query, requireExactText = true, limit = 50) }
            .distinct()
    }

    private fun fetchFromGBIF(plant: PlantEntity, aggressive: Boolean = false): List<String> {
        val query = taxonQuery(plant)
        if (!query.isSpecies) return emptyList()

        val out = mutableListOf<String>()
        val client = if (aggressive) httpClient else gbifClient
        val mediaLimit = if (aggressive) 25 else 10
        val occurrenceLimit = if (aggressive) 25 else 8

        val matchUrl = "https://api.gbif.org/v1/species/match" +
            "?name=${Uri.encode(query.canonical)}&strict=true"
        val match = requestJson(matchUrl, client) ?: return emptyList()
        val matchedName = match.optString("species", match.optString("scientificName", ""))
        val confidence = match.optInt("confidence", 0)
        val usageKey = match.optInt("usageKey", 0)
        if (usageKey <= 0 || confidence < 90 || !matchesTaxonName(matchedName, query)) return emptyList()

        // Primero media asociada a la ficha taxonómica (rápido, aunque a menudo vacío).
        val mediaUrl = "https://api.gbif.org/v1/species/$usageKey/media?limit=$mediaLimit"
        requestJson(mediaUrl, client)
            ?.optJSONArray("results")
            ?.let { results ->
                forEachJsonObject(results) { media ->
                    addGbifMediaUrl(media, out)
                }
            }

        if (out.isNotEmpty()) return out.distinct()

        // Después ocurrencias multimedia, pero con taxon_key exacto y timeout corto.
        val occurrenceUrl = "https://api.gbif.org/v1/occurrence/search" +
            "?taxon_key=$usageKey&media_type=StillImage&limit=$occurrenceLimit"
        requestJson(occurrenceUrl, client)
            ?.optJSONArray("results")
            ?.let { results ->
                forEachJsonObject(results) { occurrence ->
                    val species = occurrence.optString("species", "")
                    val scientificName = occurrence.optString("scientificName", "")
                    if (!matchesTaxonName(species, query) && !matchesTaxonName(scientificName, query)) {
                        return@forEachJsonObject
                    }
                    occurrence.optJSONArray("media")?.let { mediaArray ->
                        forEachJsonObject(mediaArray) { media ->
                            addGbifMediaUrl(media, out)
                        }
                    }
                }
            }

        return out.distinct()
    }

    private fun addGbifMediaUrl(media: JSONObject, out: MutableList<String>) {
        val type = media.optString("type", "")
        val format = media.optString("format", "")
        val identifier = media.optString("identifier", "")
        if (type.contains("StillImage", ignoreCase = true) || format.startsWith("image/")) {
            identifier.takeIf { isUsableImageUrl(it) }?.let(out::add)
        }
    }

    private fun fetchFromEOLExact(plant: PlantEntity): List<String> {
        val query = taxonQuery(plant)
        if (!query.isSpecies) return emptyList()

        val searchUrl = "https://eol.org/api/search/1.0.json" +
            "?q=${Uri.encode(query.canonical)}&page=1&exact=true"
        val results = requestJson(searchUrl)?.optJSONArray("results") ?: return emptyList()

        val out = mutableListOf<String>()
        forEachJsonObject(results) { result ->
            val title = result.optString("title", "")
            if (!matchesTaxonName(title, query) && !containsExactTaxon(title, query)) return@forEachJsonObject
            val taxonId = result.optInt("id", 0)
            if (taxonId <= 0) return@forEachJsonObject

            val pageUrl = "https://eol.org/api/pages/1.0/$taxonId.json" +
                "?images_per_page=3&language=es"
            val dataObjects = requestJson(pageUrl)
                ?.optJSONObject("taxonConcept")
                ?.optJSONArray("dataObjects")
                ?: return@forEachJsonObject
            forEachJsonObject(dataObjects) { obj ->
                val dataType = obj.optString("dataType", "")
                val mediaUrl = obj.optString("mediaURL", "")
                if (dataType.contains("StillImage", ignoreCase = true) && isUsableImageUrl(mediaUrl)) {
                    out += mediaUrl
                }
            }
        }

        return out.distinct()
    }

    private fun fetchFromCommonNameVerified(plant: PlantEntity): List<String> {
        val query = taxonQuery(plant)
        if (!query.isSpecies) return emptyList()

        val names = (listOf(plant.commonName) + plant.commonNames.split(','))
            .map { it.trim() }
            .filter { it.length >= 3 }
            .distinctBy { it.lowercase(Locale.ROOT) }
            .take(3)

        if (names.isEmpty()) return emptyList()

        return names
            .flatMap { common ->
                fetchCommonsSearch("\"$common\" \"${query.canonical}\"", query, requireExactText = true)
            }
            .distinct()
    }

    // ───────────────────────── Wikimedia Commons ───────────────────────

    private fun fetchCommonsCategoryFiles(categoryTitle: String, query: TaxonQuery): List<String> {
        val apiUrl = "https://commons.wikimedia.org/w/api.php" +
            "?action=query&generator=categorymembers" +
            "&gcmtitle=${Uri.encode(categoryTitle)}&gcmnamespace=6&gcmtype=file&gcmlimit=20" +
            "&prop=imageinfo&iiprop=url%7Cmime&format=json"

        val pages = requestJson(apiUrl)
            ?.optJSONObject("query")
            ?.optJSONObject("pages")
            ?: return emptyList()

        val scored = mutableListOf<Pair<Int, String>>()
        val keys = pages.keys()
        while (keys.hasNext()) {
            val page = pages.optJSONObject(keys.next()) ?: continue
            val title = page.optString("title", "")
            if (!isUsefulImageTitle(title)) continue
            val info = page.optJSONArray("imageinfo")?.optJSONObject(0) ?: continue
            if (!isImageMime(info.optString("mime", ""))) continue
            val url = info.optString("url", "")
            if (!isUsableImageUrl(url)) continue
            val score = if (containsExactTaxon(title, query)) 0 else 1
            scored += score to url
        }

        return scored.sortedBy { it.first }.map { it.second }.distinct()
    }

    private fun fetchCommonsSearch(
        search: String,
        query: TaxonQuery,
        requireExactText: Boolean,
        limit: Int = 15
    ): List<String> {
        val safeLimit = limit.coerceIn(1, 50)
        val apiUrl = "https://commons.wikimedia.org/w/api.php" +
            "?action=query&list=search&srnamespace=6&srlimit=$safeLimit" +
            "&srsearch=${Uri.encode(search)}&format=json"

        val results = requestJson(apiUrl)
            ?.optJSONObject("query")
            ?.optJSONArray("search")
            ?: return emptyList()

        val scored = mutableListOf<Pair<Int, String>>()
        forEachJsonObject(results) { result ->
            val title = result.optString("title", "")
            val snippet = result.optString("snippet", "")
            if (!isUsefulImageTitle(title)) return@forEachJsonObject
            if (requireExactText && !containsExactTaxon("$title $snippet", query)) return@forEachJsonObject

            val url = getCommonsFileUrl(title) ?: return@forEachJsonObject
            val score = if (containsExactTaxon(title, query)) 0 else 1
            scored += score to url
        }

        return scored.sortedBy { it.first }.map { it.second }.distinct()
    }

    private fun getCommonsFileUrl(fileTitle: String): String? {
        val apiUrl = "https://commons.wikimedia.org/w/api.php" +
            "?action=query&titles=${Uri.encode(fileTitle)}" +
            "&prop=imageinfo&iiprop=url%7Cmime&format=json"

        val pages = requestJson(apiUrl)
            ?.optJSONObject("query")
            ?.optJSONObject("pages")
            ?: return null

        val keys = pages.keys()
        while (keys.hasNext()) {
            val page = pages.optJSONObject(keys.next()) ?: continue
            val info = page.optJSONArray("imageinfo")?.optJSONObject(0) ?: continue
            if (!isImageMime(info.optString("mime", ""))) continue
            val url = info.optString("url", "")
            if (isUsableImageUrl(url)) return url
        }
        return null
    }

    private fun commonsSpecialFilePath(fileName: String): String =
        "https://commons.wikimedia.org/wiki/Special:FilePath/${Uri.encode(fileName)}"

    // ───────────────────────── Utilidades HTTP/JSON ────────────────────

    private fun requestJson(url: String, client: OkHttpClient = httpClient): JSONObject? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                JSONObject(body)
            }
        } catch (_: Exception) {
            null
        }
    }

    private inline fun forEachJsonObject(array: JSONArray, block: (JSONObject) -> Unit) {
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            block(obj)
        }
    }

    private fun claimString(entity: JSONObject, property: String): String? {
        val claims = entity.optJSONObject("claims")?.optJSONArray(property) ?: return null
        for (i in 0 until claims.length()) {
            val claim = claims.optJSONObject(i) ?: continue
            val value = claim.optJSONObject("mainsnak")
                ?.optJSONObject("datavalue")
                ?.opt("value")
            if (value is String && value.isNotBlank()) return value
        }
        return null
    }

    // ───────────────────────── Validación taxonómica ───────────────────

    private fun taxonQuery(plant: PlantEntity): TaxonQuery = parseTaxonName(plant.scientificName)

    private fun parseTaxonName(rawName: String): TaxonQuery {
        val cleaned = rawName
            .replace('×', 'x')
            .replace('_', ' ')
            .replace(Regex("[,;:()\\[\\]{}]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        val tokens = cleaned.split(' ')
            .map { it.trim().trim('.', '"', '\'', '*') }
            .filter { it.isNotBlank() }

        if (tokens.isEmpty()) {
            return TaxonQuery(raw = rawName, canonical = rawName.trim(), genus = rawName.trim(), epithet = null, isSpecies = false)
        }

        val genus = sanitizeTaxonToken(tokens[0]).replaceFirstCharForTaxon()
        val second = tokens.getOrNull(1)?.let(::sanitizeTaxonToken).orEmpty()
        val third = tokens.getOrNull(2)?.let(::sanitizeTaxonToken).orEmpty()

        val badSpeciesTokens = setOf("sp", "spp", "species", "spec", "cf", "aff")
        val isHybrid = second.equals("x", ignoreCase = true) && third.isLikelyEpithet()

        return when {
            isHybrid -> TaxonQuery(
                raw = rawName,
                canonical = "$genus x ${third.lowercase(Locale.ROOT)}",
                genus = genus,
                epithet = third.lowercase(Locale.ROOT),
                isSpecies = true,
                isHybrid = true
            )
            second.isLikelyEpithet() && second.lowercase(Locale.ROOT) !in badSpeciesTokens -> TaxonQuery(
                raw = rawName,
                canonical = "$genus ${second.lowercase(Locale.ROOT)}",
                genus = genus,
                epithet = second.lowercase(Locale.ROOT),
                isSpecies = true
            )
            else -> TaxonQuery(
                raw = rawName,
                canonical = genus,
                genus = genus,
                epithet = null,
                isSpecies = false
            )
        }
    }

    private fun sanitizeTaxonToken(token: String): String =
        token.replace(Regex("[^A-Za-zÁÉÍÓÚÜÑáéíóúüñ-]"), "")

    private fun String.isLikelyEpithet(): Boolean =
        length >= 2 && firstOrNull()?.isLowerCase() == true && any { it.isLetter() }

    private fun String.replaceFirstCharForTaxon(): String =
        lowercase(Locale.ROOT).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }

    private fun matchesTaxonName(candidateName: String, query: TaxonQuery): Boolean {
        if (candidateName.isBlank()) return false
        val parsed = parseTaxonName(candidateName)
        return if (query.isSpecies) {
            taxonKey(parsed.canonical) == taxonKey(query.canonical) ||
                (query.isHybrid && parsed.genus.equals(query.genus, ignoreCase = true) && parsed.epithet == query.epithet)
        } else {
            parsed.genus.equals(query.genus, ignoreCase = true)
        }
    }

    private fun containsExactTaxon(text: String, query: TaxonQuery): Boolean {
        if (!query.isSpecies) return text.contains(query.genus, ignoreCase = true)

        val relaxed = relaxedText(text)
        val canonical = query.canonical.lowercase(Locale.ROOT)
        val canonicalWithoutHybridX = if (query.isHybrid && query.epithet != null) {
            "${query.genus.lowercase(Locale.ROOT)} ${query.epithet}"
        } else {
            canonical
        }

        if (relaxed.contains(canonical) || relaxed.contains(canonicalWithoutHybridX)) return true

        val compactText = relaxed.replace(Regex("[^a-z0-9]"), "")
        val compactTaxon = canonical.replace(Regex("[^a-z0-9]"), "")
        val compactTaxonNoHybrid = canonicalWithoutHybridX.replace(Regex("[^a-z0-9]"), "")

        return (compactTaxon.length >= 6 && compactText.contains(compactTaxon)) ||
            (compactTaxonNoHybrid.length >= 6 && compactText.contains(compactTaxonNoHybrid))
    }

    private fun taxonKey(name: String): String = relaxedText(name)
        .replace("×", "x")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun relaxedText(text: String): String {
        val decoded = try {
            URLDecoder.decode(text, "UTF-8")
        } catch (_: Exception) {
            text
        }
        return decoded
            .replace('_', ' ')
            .replace('-', ' ')
            .replace(Regex("<[^>]+>"), " ")
            .lowercase(Locale.ROOT)
    }

    // ───────────────────────── Validación URL/imagen ───────────────────

    private fun displayName(plant: PlantEntity): String = buildString {
        append(plant.commonName.ifBlank { plant.scientificName })
        if (plant.scientificName.isNotBlank() && plant.scientificName != plant.commonName) {
            append(" · ")
            append(plant.scientificName)
        }
    }

    private fun isLocalReference(url: String): Boolean {
        val lower = url.trim().lowercase(Locale.ROOT)
        return lower.startsWith("file://") ||
            lower.startsWith("content://") ||
            lower.startsWith("android.resource://") ||
            lower.startsWith("asset://")
    }

    private fun isUsableImageUrl(url: String): Boolean {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return false
        val lower = trimmed.lowercase(Locale.ROOT)
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) return false
        if (lower.endsWith(".svg") || lower.contains(".svg?")) return false
        if (lower.endsWith(".pdf") || lower.endsWith(".djvu")) return false
        return true
    }

    private fun isImageMime(mime: String): Boolean =
        mime.startsWith("image/", ignoreCase = true) && !mime.equals("image/svg+xml", ignoreCase = true)

    private fun isUsefulImageTitle(title: String): Boolean {
        if (title.isBlank()) return false
        val lower = relaxedText(title)
        if (lower.endsWith(".svg") || lower.endsWith(".pdf") || lower.endsWith(".djvu")) return false

        // Evitar mapas, logos e iconos: suelen ser falsos positivos al buscar taxones.
        val blockedWords = listOf(
            "distribution map",
            "range map",
            " locator ",
            " logo ",
            " icon ",
            " symbol ",
            " occurrence map",
            " gbif map"
        )
        return blockedWords.none { lower.contains(it) }
    }

    private fun normalizeRemoteUrl(url: String): String {
        val trimmed = url.trim()
        val lower = trimmed.lowercase(Locale.ROOT)

        // iNaturalist usa varias tallas para el mismo photo id.
        Regex("/photos/(\\d+)/").find(lower)?.groupValues?.getOrNull(1)?.let { photoId ->
            return "inat:$photoId"
        }

        // Wikimedia thumbnails y originales deben contarse como la misma imagen.
        val withoutQuery = lower.substringBefore('?')
        val marker = "/wikipedia/commons/thumb/"
        if (withoutQuery.contains(marker)) {
            val parts = withoutQuery.substringAfter(marker).split('/')
            if (parts.size >= 3) return "wikimedia:${parts[0]}/${parts[1]}/${parts[2]}"
        }
        if (withoutQuery.contains("/wikipedia/commons/")) {
            return "wikimedia:${withoutQuery.substringAfter("/wikipedia/commons/")}"
        }

        return withoutQuery
    }
}
