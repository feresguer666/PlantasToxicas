package com.toxicplants.database.ui

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * LocalImageCache — Almacenamiento PERMANENTE de imágenes de plantas
 * ==================================================================
 *
 * ¿Dónde se guardan?
 * ──────────────────
 *  • context.filesDir  →  /data/data/<package>/files/plant_images/
 *    Es almacenamiento INTERNO PRIVADO de la app.
 *    ✅ NO se borra al "Borrar caché" desde Ajustes del sistema.
 *    ✅ NO se borra al reiniciar el dispositivo.
 *    ❌ SÍ se borra solo si el usuario pulsa "Borrar datos" (raro).
 *    ❌ SÍ se borra si se desinstala la app.
 *
 *  • context.cacheDir  →  /data/data/<package>/cache/
 *    ❌ Esto SÍ se borra al "Borrar caché". NO lo usamos.
 *
 * Coil por defecto guarda en cacheDir → por eso Coil se borra con la caché.
 * Esta clase usa filesDir → imágenes permanentes aunque borren la caché.
 */
object LocalImageCache {

    // ── Directorio permanente (filesDir, NO cacheDir) ─────────────────
    private fun getImageDir(context: Context): File {
        val dir = File(context.filesDir, "plant_images")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getImageFile(context: Context, plantId: Int): File =
        File(getImageDir(context), "plant_$plantId.jpg")

    // ── API pública ───────────────────────────────────────────────────

    fun hasLocalImage(context: Context, plantId: Int): Boolean =
        getImageFile(context, plantId).let { it.exists() && it.length() > 0L }

    fun getLocalImagePath(context: Context, plantId: Int): String =
        getImageFile(context, plantId).absolutePath

    fun deleteLocalImage(context: Context, plantId: Int) {
        getImageFile(context, plantId).takeIf { it.exists() }?.delete()
    }

    /**
     * Guarda como imagen local una foto elegida por el usuario desde el móvil.
     * Se copia al almacenamiento interno permanente de la app.
     */
    suspend fun saveFromUri(context: Context, plantId: Int, uri: Uri): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val file = getImageFile(context, plantId)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                } ?: return@withContext false
                file.exists() && file.length() > 0L
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

    /** Cuánto espacio ocupan todas las imágenes descargadas (en bytes). */
    fun totalSizeBytes(context: Context): Long =
        getImageDir(context).walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }

    /** Número de imágenes descargadas. */
    fun imageCount(context: Context): Int =
        getImageDir(context).listFiles()?.count { it.isFile } ?: 0

    /** Borra TODAS las imágenes locales (útil para "Borrar datos de la app"). */
    fun deleteAll(context: Context) {
        getImageDir(context).walkTopDown()
            .filter { it.isFile }
            .forEach { it.delete() }
    }

    // ── Descarga y guardado ────────────────────────────────────────────

    /**
     * Descarga una imagen desde [imageUrl] y la guarda de forma permanente.
     * Si ya existe una imagen para [plantId], la sobreescribe.
     *
     * @return true si se guardó correctamente, false si hubo algún error.
     */
    suspend fun downloadAndSave(
        context: Context,
        plantId: Int,
        imageUrl: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val directUrl = convertToDirectUrl(imageUrl)
            if (directUrl.isBlank()) return@withContext false

            val connection = URL(directUrl).openConnection() as HttpURLConnection
            connection.apply {
                setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36")
                setRequestProperty("Accept", "image/*")
                setRequestProperty("Referer", "https://en.wikipedia.org/")
                connectTimeout = 20_000
                readTimeout    = 20_000
                instanceFollowRedirects = true
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                connection.disconnect()
                return@withContext false
            }

            // ✅ GUARDADO DIRECTO DEL STREAM (Más rápido y fiable que usar Bitmap)
            val file = getImageFile(context, plantId)
            connection.inputStream.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            connection.disconnect()

            file.exists() && file.length() > 0L

        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ── Conversión de URLs de Wikipedia ───────────────────────────────

    private fun convertToDirectUrl(url: String): String {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return ""
        val lower = trimmed.lowercase()

        // Ya es URL directa de imagen
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
            lower.endsWith(".png") || lower.endsWith(".webp") ||
            lower.endsWith(".gif")) {
            return trimmed
        }

        // Página de Commons: commons.wikimedia.org/wiki/File:NOMBRE.jpg
        if (lower.contains("commons.wikimedia.org/wiki/file:")) {
            val fileName = trimmed
                .substringAfter("File:").substringAfter("file:")
                .substringBefore("?")
            if (fileName.isNotBlank()) {
                return "https://commons.wikimedia.org/wiki/Special:FilePath/" +
                        URLEncoder.encode(fileName, "UTF-8")
            }
        }

        // Special:FilePath ya es URL directa de imagen
        if (lower.contains("special:filepath")) return trimmed

        // upload.wikimedia.org con /thumb/ → convertir a URL original
        if (lower.contains("upload.wikimedia.org") && lower.contains("/thumb/")) {
            val marker = "/wikipedia/commons/thumb/"
            if (trimmed.contains(marker)) {
                val parts = trimmed.substringAfter(marker).split("/")
                if (parts.size >= 3) {
                    return "https://upload.wikimedia.org/wikipedia/commons/" +
                            "${parts[0]}/${parts[1]}/${parts[2]}"
                }
            }
            // Thumb de Wikipedia (no commons)
            val wikiMarker = "/wikipedia/"
            val wikiIdx = trimmed.indexOf(wikiMarker)
            if (wikiIdx != -1) {
                val afterWiki = trimmed.substring(wikiIdx + wikiMarker.length)
                val lang = afterWiki.substringBefore("/")
                val thumbPath = afterWiki.substringAfter("/thumb/")
                val parts = thumbPath.split("/")
                if (parts.size >= 3) {
                    return "https://upload.wikimedia.org/wikipedia/$lang/" +
                            "${parts[0]}/${parts[1]}/${parts[2]}"
                }
            }
        }

        // upload.wikimedia.org sin thumb → URL directa
        if (lower.contains("upload.wikimedia.org")) return trimmed

        // iNaturalist, EOL, etc. — intentar directamente
        return trimmed
    }
}
