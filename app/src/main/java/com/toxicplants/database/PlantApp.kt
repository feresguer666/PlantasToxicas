package com.toxicplants.database

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * PlantApp — Configuración global de Coil con caché PERMANENTE
 * =============================================================
 *
 * PROBLEMA ORIGINAL:
 *   Coil usaba por defecto context.cacheDir para su caché de disco.
 *   Cuando el usuario pulsa "Borrar caché" en Ajustes del sistema,
 *   cacheDir se vacía → todas las imágenes desaparecen.
 *
 * SOLUCIÓN:
 *   Coil ahora usa context.filesDir/coil_image_cache/ como caché.
 *   filesDir NO se borra con "Borrar caché". Solo se borra con
 *   "Borrar datos" (que también borra la base de datos, etc.).
 *
 * Estructura de directorios:
 *   /data/data/<package>/files/
 *   ├── plant_images/          ← LocalImageCache  (imágenes descargadas)
 *   └── coil_image_cache/      ← Coil DiskCache   (caché de Coil)
 *
 *   /data/data/<package>/cache/   ← SE BORRA con "Borrar caché" (NO usamos)
 */
class PlantApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        // CAPTURADOR DE CRASHES: guarda cualquier error fatal en un fichero de texto
        // accesible, para poder diagnosticar sin cable/Logcat.
        // Ruta: Android/data/com.toxicplants.database/files/crash_log.txt
        // (visible desde un explorador de archivos del móvil).
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val dir = getExternalFilesDir(null) ?: filesDir
                val file = File(dir, "crash_log.txt")
                val sw = java.io.StringWriter()
                throwable.printStackTrace(java.io.PrintWriter(sw))
                file.writeText(
                    "FECHA: ${java.util.Date()}\n" +
                            "HILO: ${thread.name}\n\n" +
                            sw.toString()
                )
            } catch (_: Throwable) { /* no romper el handler */ }
            previous?.uncaughtException(thread, throwable)
        }
    }

    override fun newImageLoader(): ImageLoader {

        // Directorio permanente para Coil (filesDir, NO cacheDir)
        val permanentCacheDir = File(filesDir, "coil_image_cache").also { it.mkdirs() }

        return ImageLoader.Builder(this)
            // ── Caché en memoria ──────────────────────────────────────
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.20)   // 20% de la RAM disponible
                    .build()
            }
            // ── Caché en disco PERMANENTE ─────────────────────────────
            .diskCache {
                DiskCache.Builder()
                    .directory(permanentCacheDir)          // ← KEY: filesDir
                    .maxSizeBytes(1536L * 1024L * 1024L)  // 1,5 GB  // 1000 MB máximo
                    .build()
            }
            // ── Cliente HTTP con cabeceras adecuadas ──────────────────
            .okHttpClient {
                OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .followRedirects(true)
                    .addInterceptor { chain ->
                        val request = chain.request().newBuilder()
                            .header(
                                "User-Agent",
                                "Mozilla/5.0 (Linux; Android 14) " +
                                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                        "Chrome/124.0 Mobile Safari/537.36"
                            )
                            .header("Accept", "image/webp,image/apng,image/*,*/*;q=0.8")
                            .header("Referer", "https://en.wikipedia.org/")
                            .build()
                        chain.proceed(request)
                    }
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
