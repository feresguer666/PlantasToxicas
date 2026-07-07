package com.toxicplants.database.ui

import android.content.Context
import com.toxicplants.database.PlantEntity

object PlantImageHelper {

    private val INVALID_URL_PREFIXES = listOf(
        "https://wikimedia.org",
        "https://www.wikimedia.org",
        "http://wikimedia.org",
    )

    fun isInvalidUrl(url: String): Boolean {
        val trimmed = url.trim()
        if (trimmed.isBlank() || trimmed.length < 20) return true
        for (prefix in INVALID_URL_PREFIXES) {
            if (trimmed.trimEnd('/') == prefix.trimEnd('/')) return true
        }
        return false
    }

    fun getModelForUrl(context: Context, url: String): Any {
        val trimmed = url.trim()
        if (trimmed.startsWith("file://", ignoreCase = true)) {
            val cleanPath = trimmed.removePrefix("file://").removePrefix("FILE://")
            val directFile = java.io.File(cleanPath)
            if (directFile.exists() && directFile.length() > 0L) {
                return android.net.Uri.fromFile(directFile)
            }
            val fileName = cleanPath.substringAfterLast("/")
            val fallbackFile = java.io.File(java.io.File(context.filesDir, "plant_images"), fileName)
            if (fallbackFile.exists() && fallbackFile.length() > 0L) {
                return android.net.Uri.fromFile(fallbackFile)
            }
            return android.net.Uri.parse(trimmed)
        } else if (trimmed.startsWith("content://", ignoreCase = true) ||
                   trimmed.startsWith("android.resource://", ignoreCase = true) ||
                   trimmed.startsWith("asset://", ignoreCase = true)) {
            return android.net.Uri.parse(trimmed)
        } else {
            return trimmed
        }
    }

    fun parseImageUrls(imageUrl: String): List<String> {
        return imageUrl.split("|").map { it.trim() }.filter { it.isNotBlank() }
    }

    suspend fun resolveAllImageUrls(context: Context, plant: PlantEntity): List<String> {
        val urls = parseImageUrls(plant.imageUrl)
        if (urls.size > 1 || (urls.size == 1 && plant.imageUrl.contains("|"))) {
            return urls
        }
        val single = resolveImageUrl(context, plant)
        return if (single.isNotBlank()) listOf(single) else emptyList()
    }

    suspend fun resolveImageUrl(context: Context, plant: PlantEntity): String {
        val urls = parseImageUrls(plant.imageUrl)
        if (urls.size > 1 || (urls.size == 1 && plant.imageUrl.contains("|"))) {
            return urls.first()
        }

        // 1. Verificar si ya hay una imagen local guardada.
        if (LocalImageCache.hasLocalImage(context, plant.id)) {
            return "file://${LocalImageCache.getLocalImagePath(context, plant.id)}"
        }

        val jsonUrl = plant.imageUrl.trim()

        // 2. Si el JSON apunta a un asset/local, devolverlo directamente.
        //    (No se puede descargar con HttpURLConnection, pero Coil sí puede mostrarlo.)
        if (jsonUrl.startsWith("file://", ignoreCase = true) ||
            jsonUrl.startsWith("android.resource://", ignoreCase = true) ||
            jsonUrl.startsWith("asset://", ignoreCase = true)
        ) {
            return jsonUrl
        }

        // 3. Resolver online priorizando nombre científico exacto.
        //    Esto evita usar primero fotos genéricas repetidas por nombre común.
        val resolvedUrl = ImageDownloader.resolveImageUrl(context, plant)
        if (resolvedUrl != null) {
            return resolvedUrl
        }

        // 4. Último recurso: si el JSON tiene una URL válida, usarla aunque sea genérica.
        if (!isInvalidUrl(jsonUrl)) {
            val saved = LocalImageCache.downloadAndSave(context, plant.id, jsonUrl)
            if (saved) {
                return "file://${LocalImageCache.getLocalImagePath(context, plant.id)}"
            }
        }

        return ""
    }
}
