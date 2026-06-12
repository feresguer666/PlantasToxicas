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

    suspend fun resolveImageUrl(context: Context, plant: PlantEntity): String {
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
