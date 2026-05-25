package com.toxicplants.database.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object LocalImageCache {

    private fun getImageFile(context: Context, plantId: Int): File {
        val dir = File(context.filesDir, "plant_images")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "plant_$plantId.jpg")
    }

    fun hasLocalImage(context: Context, plantId: Int): Boolean {
        return getImageFile(context, plantId).exists()
    }

    fun getLocalImagePath(context: Context, plantId: Int): String {
        return getImageFile(context, plantId).absolutePath
    }

    fun deleteLocalImage(context: Context, plantId: Int) {
        val file = getImageFile(context, plantId)
        if (file.exists()) file.delete()
    }

    suspend fun downloadAndSave(
        context: Context,
        plantId: Int,
        imageUrl: String
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val file = getImageFile(context, plantId)

                // Si ya existe, borrarla para reemplazar
                if (file.exists()) file.delete()

                // Convertir URL de pagina wiki a URL directa
                val directUrl = convertToDirectUrl(imageUrl)
                if (directUrl.isBlank()) return@withContext false

                val connection = URL(directUrl).openConnection() as HttpURLConnection
                connection.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36"
                )
                connection.setRequestProperty(
                    "Accept",
                    "image/webp,image/apng,image/*,*/*;q=0.8"
                )
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.instanceFollowRedirects = true

                val responseCode = connection.responseCode
                if (responseCode != 200) {
                    connection.disconnect()
                    return@withContext false
                }

                val inputStream = connection.inputStream
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                connection.disconnect()

                if (bitmap != null) {
                    val outputStream = FileOutputStream(file)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                    outputStream.flush()
                    outputStream.close()
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun convertToDirectUrl(url: String): String {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return ""

        // Si es URL directa de imagen
        val lower = trimmed.lowercase()
        if (
            lower.endsWith(".jpg") ||
            lower.endsWith(".jpeg") ||
            lower.endsWith(".png") ||
            lower.endsWith(".webp") ||
            lower.endsWith(".gif") ||
            lower.endsWith(".svg")
        ) {
            return trimmed
        }

        // Si es pagina de Commons: https://commons.wikimedia.org/wiki/File:NOMBRE.jpg
        if (lower.contains("commons.wikimedia.org/wiki/file:")) {
            val fileName = trimmed.substringAfter("File:").substringAfter("file:")
            if (fileName.isNotBlank()) {
                return "https://commons.wikimedia.org/wiki/Special:FilePath/${URLEncoder.encode(fileName, "UTF-8")}"
            }
        }

        // Si es Special:FilePath ya
        if (lower.contains("special:filepath")) {
            return trimmed
        }

        // Si es upload.wikimedia.org con thumb
        if (lower.contains("upload.wikimedia.org") && lower.contains("/thumb/")) {
            val marker = "/wikipedia/commons/thumb/"
            if (trimmed.contains(marker)) {
                val parts = trimmed.substringAfter(marker).split("/")
                if (parts.size >= 3) {
                    return "https://upload.wikimedia.org/wikipedia/commons/${parts[0]}/${parts[1]}/${parts[2]}"
                }
            }
        }

        // Si es upload.wikimedia.org sin thumb
        if (lower.contains("upload.wikimedia.org")) {
            return trimmed
        }

        // Cualquier otra URL de imagen, intentar directamente
        return trimmed
    }
}