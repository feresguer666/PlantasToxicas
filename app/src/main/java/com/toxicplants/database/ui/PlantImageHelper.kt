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
        // 1. Verificar si ya hay una imagen local
        if (LocalImageCache.hasLocalImage(context, plant.id)) {
            return "file://${LocalImageCache.getLocalImagePath(context, plant.id)}"
        }

        // 2. Si el JSON tiene una URL válida, intentar descargarla primero
        val jsonUrl = plant.imageUrl.trim()
        if (!isInvalidUrl(jsonUrl)) {
            val saved = LocalImageCache.downloadAndSave(context, plant.id, jsonUrl)
            if (saved) {
                return "file://${LocalImageCache.getLocalImagePath(context, plant.id)}"
            }
            // Si la URL del JSON falla, no nos rendimos, seguimos buscando online
        }

        // 3. Usar la cascada completa de ImageDownloader (Wiki -> Commons -> ... -> IA)
        val resolvedUrl = ImageDownloader.resolveImageUrl(context, plant)
        if (resolvedUrl != null) {
            return resolvedUrl
        }

        return ""
    }

    // Eliminamos findImageOnline ya que ahora ImageDownloader se encarga de todo
}
