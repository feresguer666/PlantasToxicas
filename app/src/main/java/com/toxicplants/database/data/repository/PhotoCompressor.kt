package com.toxicplants.database.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * Recompresión de fotos (JPG/PNG) para reducir el tamaño del backup
 * o liberar espacio en el móvil.
 *
 * Estrategia:
 *  1. Decodifica el bitmap con un inSampleSize para no cargar la imagen
 *     completa en memoria si es muy grande.
 *  2. Si el lado mayor sigue siendo > maxSide, lo redimensiona.
 *  3. Comprime a JPEG con la calidad dada.
 *
 * Esto evita OOM con fotos enormes (móviles modernos sacan JPGs de 8-20 MB).
 */
object PhotoCompressor {

    /** Presets de recompresión disponibles. */
    enum class Preset(val maxSide: Int, val quality: Int, val label: String) {
        ORIGINAL(0, 100, "Sin comprimir (original)"),
        HIGH(1600, 85, "Alta (1600 px / 85%)"),
        MEDIUM(1200, 75, "Media (1200 px / 75%)"),
        LOW(800, 65, "Baja (800 px / 65%)");

        val isCompressing: Boolean get() = this != ORIGINAL
    }

    /**
     * Recomprime [input] y devuelve los bytes del JPG resultante.
     * Si no se pudiera decodificar, devuelve los bytes originales del fichero.
     */
    fun recompressToBytes(input: File, preset: Preset): ByteArray {
        if (preset == Preset.ORIGINAL || !input.exists() || input.length() == 0L) {
            return input.readBytes()
        }
        return try {
            // Paso 1: leer solo dimensiones para calcular sampleSize sin cargar bitmap.
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(input.absolutePath, opts)
            val (w, h) = opts.outWidth to opts.outHeight
            if (w <= 0 || h <= 0) return input.readBytes()

            opts.inSampleSize = computeSampleSize(w, h, preset.maxSide)
            opts.inJustDecodeBounds = false
            // Usamos RGB_565 si la imagen no tiene alpha → ahorra memoria
            opts.inPreferredConfig = Bitmap.Config.RGB_565

            val raw = BitmapFactory.decodeFile(input.absolutePath, opts)
                ?: return input.readBytes()

            // Paso 2: redimensionar si tras sampleSize aún excede maxSide.
            val resized = scaleIfNeeded(raw, preset.maxSide)
            if (resized !== raw) raw.recycle()

            // Paso 3: comprimir a JPEG.
            val out = ByteArrayOutputStream(64 * 1024)
            resized.compress(Bitmap.CompressFormat.JPEG, preset.quality, out)
            resized.recycle()
            val result = out.toByteArray()

            // Si por casualidad la recompresión salió MÁS grande que el original
            // (puede pasar con fotos ya muy optimizadas), devolvemos el original.
            if (result.size >= input.length()) input.readBytes() else result
        } catch (oom: OutOfMemoryError) {
            // Si aún hay OOM, salimos con el original
            System.gc()
            input.readBytes()
        } catch (t: Throwable) {
            t.printStackTrace()
            input.readBytes()
        }
    }

    /**
     * Recomprime el fichero IN-PLACE: sobreescribe [input] con la versión
     * comprimida si efectivamente es más pequeña.
     * Devuelve (sizeAntes, sizeDespues) o null si no se pudo.
     */
    fun recompressInPlace(input: File, preset: Preset): Pair<Long, Long>? {
        if (preset == Preset.ORIGINAL) return null
        val sizeBefore = input.length()
        val newBytes = recompressToBytes(input, preset)
        if (newBytes.size.toLong() >= sizeBefore) return sizeBefore to sizeBefore
        return try {
            FileOutputStream(input).use { it.write(newBytes) }
            sizeBefore to newBytes.size.toLong()
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private fun computeSampleSize(w: Int, h: Int, maxSide: Int): Int {
        if (maxSide <= 0) return 1
        var sample = 1
        var cw = w
        var ch = h
        // Doblamos el sampleSize mientras el lado mayor exceda 2× maxSide.
        // Esto deja la imagen "un poco grande" para luego escalarla finamente.
        while (cw / 2 >= maxSide && ch / 2 >= maxSide) {
            cw /= 2
            ch /= 2
            sample *= 2
        }
        return sample
    }

    private fun scaleIfNeeded(bmp: Bitmap, maxSide: Int): Bitmap {
        if (maxSide <= 0) return bmp
        val w = bmp.width
        val h = bmp.height
        val largest = maxOf(w, h)
        if (largest <= maxSide) return bmp
        val ratio = maxSide.toFloat() / largest
        val nw = (w * ratio).toInt().coerceAtLeast(1)
        val nh = (h * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bmp, nw, nh, true)
    }
}
