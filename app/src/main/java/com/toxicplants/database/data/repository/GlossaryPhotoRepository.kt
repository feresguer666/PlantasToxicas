package com.toxicplants.database.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Gestión de fotos REALES asociadas a cada término del glosario.
 *
 * Las fotos viven en:  filesDir/glossary_photos/<term_id>/<nombre>.jpg
 *
 * Tipos de fotos:
 *   - "seed": descargadas automáticamente de Wikimedia Commons la primera
 *             vez que se abre el término (atribución preservada).
 *   - "user": añadidas por el usuario desde cámara/galería.
 *
 * Se incluyen en el backup completo de la app.
 */
object GlossaryPhotoRepository {

    private const val ROOT_DIR = "glossary_photos"
    private const val MANIFEST = "manifest.json"

    /** Carpeta de fotos de un término. */
    fun termDir(context: Context, termId: String): File =
        File(File(context.filesDir, ROOT_DIR), sanitize(termId)).apply { mkdirs() }

    /** Lista actual de fotos (ordenadas: seed primero, después user, por nombre).
     *  Intenta copiar las seeds empaquetadas en assets antes de listar. */
    fun listPhotos(context: Context, termId: String): List<GlossaryPhoto> {
        val dir = termDir(context, termId)
        copyBundledSeedsIfNeeded(context, termId)
        if (!dir.exists()) return emptyList()
        val manifest = loadManifest(dir)
        val files = dir.listFiles { f -> f.isFile && f.extension.lowercase() in setOf("jpg", "jpeg", "png", "webp") }
            ?: return emptyList()
        return files
            .map { f ->
                val info = manifest[f.name]
                GlossaryPhoto(
                    file = f,
                    isSeed = info?.optBoolean("seed", false) == true,
                    sourceUrl = info?.optString("sourceUrl", "") ?: "",
                    attribution = info?.optString("attribution", "") ?: ""
                )
            }
            .sortedWith(compareByDescending<GlossaryPhoto> { it.isSeed }.thenBy { it.file.name })
    }

    /** Añade una foto desde una Uri local (cámara/galería). */
    suspend fun addUserPhotoFromUri(
        context: Context,
        termId: String,
        uri: Uri
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val dir = termDir(context, termId)
            val name = "user_${System.currentTimeMillis()}.jpg"
            val dest = File(dir, name)
            context.contentResolver.openInputStream(uri)?.use { input ->
                val bmp = BitmapFactory.decodeStream(input)
                    ?: return@withContext Result.failure(IllegalStateException("No se pudo decodificar la imagen"))
                FileOutputStream(dest).use { out ->
                    bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                bmp.recycle()
            } ?: return@withContext Result.failure(IllegalStateException("No se pudo abrir la imagen"))

            updateManifest(dir) { json ->
                json.put(name, JSONObject().apply {
                    put("seed", false)
                    put("addedAt", System.currentTimeMillis())
                })
            }
            Result.success(dest)
        } catch (t: Throwable) {
            t.printStackTrace()
            Result.failure(t)
        }
    }

    /** Borra una foto. */
    fun deletePhoto(context: Context, termId: String, photo: GlossaryPhoto): Boolean {
        val dir = termDir(context, termId)
        val deleted = photo.file.delete()
        if (deleted) {
            updateManifest(dir) { json -> json.remove(photo.file.name) }
        }
        return deleted
    }

    /**
     * Copia las seeds empaquetadas en `assets/glossary_seed_photos/<termId>/`
     * a `filesDir/glossary_photos/<termId>/`.
     *
     * Estrategia:
     *  - Si en assets hay fotos (seed_1.jpg, seed_2.jpg…), se consideran la
     *    fuente de verdad y se copian SIEMPRE que no estén ya copiadas
     *    (identificadas por nombre).
     *  - No borra las fotos del usuario ni otras seeds previas de Wikimedia
     *    distintas, simplemente añade/sobrescribe las suyas.
     *  - Cuando una seed bundled se copia, se marca en el manifest con la
     *    fuente "assets://" para que sea reconocible.
     *
     * @return número de fotos copiadas en esta llamada.
     */
    fun copyBundledSeedsIfNeeded(context: Context, termId: String): Int {
        val dir = termDir(context, termId)
        val assetTermPath = "glossary_seed_photos/${sanitize(termId)}"

        return try {
            val files = context.assets.list(assetTermPath) ?: emptyArray()
            val imageFiles = files.filter {
                it.lowercase().endsWith(".jpg") ||
                        it.lowercase().endsWith(".jpeg") ||
                        it.lowercase().endsWith(".png")
            }
            if (imageFiles.isEmpty()) return 0

            // Marca de "ya copiado este conjunto exacto" para evitar trabajo en cada apertura.
            // El identificador es el listado ordenado de nombres de assets.
            val signature = imageFiles.sorted().joinToString("|")
            val marker = File(dir, ".bundled_seeds.signature")
            if (marker.exists() && marker.readText().trim() == signature) {
                return 0  // ya está sincronizado
            }

            // Antes de copiar, borrar SEEDS antiguas (las del usuario se respetan).
            // Identificamos seeds antiguas por el manifest (seed = true) o por
            // empezar con "seed_" en el nombre (convención de descarga Wikimedia).
            val existingManifest = loadManifest(dir)
            dir.listFiles()?.forEach { f ->
                if (!f.isFile) return@forEach
                val info = existingManifest[f.name]
                val isOldSeed = (info?.optBoolean("seed", false) == true) ||
                        f.name.startsWith("seed_")
                if (isOldSeed) {
                    f.delete()
                    updateManifest(dir) { json -> json.remove(f.name) }
                }
            }
            // Borrar también el marker de "Wikimedia ya intentado" para resetear estado.
            runCatching { File(dir, ".seed_attempted").delete() }

            var copied = 0
            for (name in imageFiles) {
                runCatching {
                    context.assets.open("$assetTermPath/$name").use { input ->
                        FileOutputStream(File(dir, name)).use { out ->
                            input.copyTo(out, 32 * 1024)
                        }
                    }
                    updateManifest(dir) { json ->
                        json.put(name, JSONObject().apply {
                            put("seed", true)
                            put("sourceUrl", "assets://$assetTermPath/$name")
                            put("attribution", "Empaquetado con la app")
                            put("addedAt", System.currentTimeMillis())
                        })
                    }
                    copied++
                }
            }

            // Guardar la firma de lo copiado.
            runCatching { marker.writeText(signature) }
            copied
        } catch (_: Throwable) {
            0
        }
    }

    /**
     * Descarga 2-3 fotos seed desde Wikimedia Commons si todavía no se han
     * descargado para este término. Devuelve cuántas fotos hay tras el intento.
     *
     * Antes de salir a Internet, intenta copiar las seeds empaquetadas
     * en assets/glossary_seed_photos/ si existen.
     */
    suspend fun ensureSeedPhotos(
        context: Context,
        termId: String,
        wikimediaQuery: String?,
        maxSeed: Int = 3
    ): Int = withContext(Dispatchers.IO) {
        val dir = termDir(context, termId)

        // 0. SIEMPRE preferir las empaquetadas en assets.
        //    Si existen, las copia (o ya están sincronizadas) y NUNCA bajamos
        //    de Wikimedia para este término — las del repo son la fuente de verdad.
        val assetTermPath = "glossary_seed_photos/${sanitize(termId)}"
        val bundledExist = try {
            (context.assets.list(assetTermPath) ?: emptyArray()).any {
                it.lowercase().endsWith(".jpg") ||
                        it.lowercase().endsWith(".jpeg") ||
                        it.lowercase().endsWith(".png")
            }
        } catch (_: Throwable) { false }

        if (bundledExist) {
            copyBundledSeedsIfNeeded(context, termId)
            return@withContext listPhotos(context, termId).size
        }

        // 1. Si no hay empaquetadas, intentamos Wikimedia (modo original).
        val current = listPhotos(context, termId)
        val seedCount = current.count { it.isSeed }
        if (seedCount >= maxSeed || wikimediaQuery.isNullOrBlank()) return@withContext current.size

        // Marcador para no reintentar continuamente si Commons no devuelve nada
        val marker = File(dir, ".seed_attempted")
        if (marker.exists()) return@withContext current.size
        marker.createNewFile()

        try {
            val titles = searchCommons(wikimediaQuery, maxSeed)
            for ((idx, title) in titles.withIndex()) {
                if (listPhotos(context, termId).count { it.isSeed } >= maxSeed) break
                val downloaded = downloadCommonsThumb(title, dir, "seed_${idx + 1}.jpg")
                if (downloaded != null) {
                    updateManifest(dir) { json ->
                        json.put(downloaded.name, JSONObject().apply {
                            put("seed", true)
                            put("sourceUrl", "https://commons.wikimedia.org/wiki/$title")
                            put("attribution", "Wikimedia Commons")
                            put("addedAt", System.currentTimeMillis())
                        })
                    }
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
        listPhotos(context, termId).size
    }

    // ── Wikimedia API ───────────────────────────────────────────────────

    private fun searchCommons(query: String, limit: Int): List<String> {
        val q = URLEncoder.encode(query, "UTF-8")
        val url = "https://commons.wikimedia.org/w/api.php" +
                "?action=query&list=search&srnamespace=6" +
                "&srsearch=$q&srlimit=$limit" +
                "&format=json&origin=*"
        val text = httpGet(url) ?: return emptyList()
        val titles = mutableListOf<String>()
        try {
            val root = JSONObject(text)
            val arr = root.optJSONObject("query")?.optJSONArray("search") ?: return emptyList()
            for (i in 0 until arr.length()) {
                val t = arr.optJSONObject(i)?.optString("title") ?: continue
                if (t.startsWith("File:")) {
                    titles += t.removePrefix("File:")
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
        return titles
    }

    private fun downloadCommonsThumb(fileTitle: String, dir: File, destName: String): File? {
        // Special:FilePath redirige al binario; pedimos 800px máximo para no
        // descargar imágenes enormes.
        val encoded = URLEncoder.encode(fileTitle, "UTF-8")
        val url = "https://commons.wikimedia.org/wiki/Special:FilePath/$encoded?width=800"
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 12000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", "PlantasToxicas/1.0 (educational)")
            conn.connect()
            if (conn.responseCode !in 200..299) return null

            val dest = File(dir, destName)
            conn.inputStream.use { ins ->
                FileOutputStream(dest).use { out ->
                    ins.copyTo(out, 32 * 1024)
                }
            }
            // Validar que es una imagen decodificable
            val bmp = BitmapFactory.decodeFile(dest.absolutePath)
            if (bmp == null) {
                dest.delete()
                null
            } else {
                bmp.recycle()
                dest
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }

    private fun httpGet(url: String): String? = try {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 6000
        conn.readTimeout = 10000
        conn.setRequestProperty("User-Agent", "PlantasToxicas/1.0 (educational)")
        conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    } catch (t: Throwable) {
        t.printStackTrace()
        null
    }

    // ── Manifest helpers ────────────────────────────────────────────────

    private fun loadManifest(dir: File): Map<String, JSONObject> {
        val f = File(dir, MANIFEST)
        if (!f.exists()) return emptyMap()
        return try {
            val root = JSONObject(f.readText())
            val out = HashMap<String, JSONObject>()
            for (key in root.keys()) {
                root.optJSONObject(key)?.let { out[key] = it }
            }
            out
        } catch (t: Throwable) {
            t.printStackTrace()
            emptyMap()
        }
    }

    private fun updateManifest(dir: File, mutate: (JSONObject) -> Unit) {
        val f = File(dir, MANIFEST)
        val root = if (f.exists()) {
            try { JSONObject(f.readText()) } catch (_: Throwable) { JSONObject() }
        } else JSONObject()
        mutate(root)
        try {
            f.writeText(root.toString())
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    private fun sanitize(s: String): String =
        s.lowercase().replace(Regex("[^a-z0-9_]+"), "_").trim('_').take(60).ifBlank { "x" }

    data class GlossaryPhoto(
        val file: File,
        val isSeed: Boolean,
        val sourceUrl: String,
        val attribution: String
    )
}
