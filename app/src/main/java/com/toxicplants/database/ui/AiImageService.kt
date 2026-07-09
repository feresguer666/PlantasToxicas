package com.toxicplants.database.ui

import android.util.Log

/**
 * Servicio para generar imágenes botánicas utilizando Pollinations.ai.
 * No requiere API Key y ofrece resultados de alta calidad para fines educativos.
 */
object AiImageService {
    private const val BASE_URL = "https://image.pollinations.ai/prompt/"

    /**
     * Genera una URL de imagen basada en los datos de la planta.
     * La imagen se genera dinámicamente al cargar la URL en Coil.
     */
    fun generateBotanicalImageUrl(scientificName: String, family: String, commonName: String): String {
        val prompt = "Scientific botanical illustration of $scientificName ($commonName), family $family, " +
                "detailed morphology, white background, high resolution, encyclopedia style, " +
                "accurate botanical details, professional biology drawing, 4k"

        // Codificar el prompt para URL
        val encodedPrompt = java.net.URLEncoder.encode(prompt, "UTF-8")

        // Usamos el modelo 'flux' que es el más preciso actualmente en Pollinations
        return "$BASE_URL$encodedPrompt?width=1024&height=1024&nologo=true&model=flux"
    }
}
