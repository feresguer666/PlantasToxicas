package com.toxicplants.database.data.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import com.toxicplants.database.CompoundEntity
import com.toxicplants.database.LichenDataSource
import com.toxicplants.database.LichenEntity
import com.toxicplants.database.LichenUserStore
import com.toxicplants.database.MushroomDataSource
import com.toxicplants.database.MushroomEntity
import com.toxicplants.database.MushroomUserStore
import com.toxicplants.database.PlantDatabase
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.PsychotropicOverrides
import com.toxicplants.database.PsychotropicUserStore
import com.toxicplants.database.SightingEntity
import com.toxicplants.database.SightingStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Backup en STREAMING (usa los streams JSON de Gson):
 *  - El export escribe el JSON directamente al fichero sin materializar todo
 *    en memoria. Las imágenes se leen y codifican en Base64 una a una.
 *  - El import lee con JsonReader y restaura por lotes.
 *  - Soporta GZIP opcional (.json.gz) → reduce tamaño y evita OOM con muchos datos.
 *
 * Evita OutOfMemoryError típicos con miles de plantas y cientos de fotos.
 */
class BackupRepository(private val context: Context, private val db: PlantDatabase) {

    private val gson = Gson()

    // ── Progreso ────────────────────────────────────────────────────────

    /** Reporte de progreso para la UI. Se invoca en el hilo IO. */
    fun interface ProgressListener {
        fun onProgress(phase: String, current: Int, total: Int)
    }

    // ── Modelos ─────────────────────────────────────────────────────────

    data class BackupPlantLocation(
        val plantId: Int,
        val latitude: Double?,
        val longitude: Double?,
        val locationName: String?,
        val foundDate: String?,
        val notes: String?
    )

    // ── Helpers ─────────────────────────────────────────────────────────

    private fun countImages(dir: File): Int =
        if (!dir.exists()) 0
        else dir.listFiles()?.count { it.isFile && it.length() > 0L } ?: 0

    private fun shouldGzip(uri: Uri): Boolean {
        val s = uri.toString().lowercase()
        return s.endsWith(".gz") || s.endsWith(".gzip")
    }

    private fun isGzipStream(bytes: ByteArray): Boolean =
        bytes.size >= 2 && (bytes[0].toInt() and 0xFF) == 0x1F && (bytes[1].toInt() and 0xFF) == 0x8B

    // ── EXPORT (streaming) ──────────────────────────────────────────────

    /**
     * Exporta una copia COMPLETA o INCREMENTAL.
     *
     * @param recompression preset de recompresión de fotos para el backup.
     *                      Solo afecta a las fotos que viajan en el .json.gz;
     *                      los archivos del móvil no se tocan.
     * @param incremental si true, hace snapshot ligero (todos los textos +
     *                    solo fotos cambiadas según el manifiesto).
     */
    suspend fun exportDatabaseToUri(
        uri: Uri,
        progress: ProgressListener? = null,
        recompression: PhotoCompressor.Preset = PhotoCompressor.Preset.ORIGINAL,
        incremental: Boolean = false
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            progress?.onProgress("Preparando datos…", 0, 1)

            // 1. Cargar datos textuales (no son grandes en memoria).
            val plants = db.plantDao().getAllPlantsSync()
            val compounds = db.compoundDao().getAllSync()
            val mushrooms = MushroomUserStore.load(context) ?: MushroomDataSource.loadAll(context)
            val lichens = LichenUserStore.load(context) ?: LichenDataSource.loadAll(context)
            val sightings = SightingStore.load(context)
            val calendarEvents = db.toxicCalendarDao().getAllEventsSync()
            val psychotropicOverrides = PsychotropicUserStore.load(context)

            val plantLocations = plants
                .filter {
                    it.latitude != null || it.longitude != null ||
                            !it.locationName.isNullOrBlank() || !it.foundDate.isNullOrBlank() ||
                            !it.notes.isNullOrBlank()
                }
                .map {
                    BackupPlantLocation(
                        plantId = it.id,
                        latitude = it.latitude,
                        longitude = it.longitude,
                        locationName = it.locationName,
                        foundDate = it.foundDate,
                        notes = it.notes
                    )
                }

            val plantImagesDir = File(context.filesDir, "plant_images")
            val mushroomImagesDir = File(context.filesDir, "mushroom_images")
            val sightingImagesDir = SightingStore.photoDir(context)

            // 2. Si es incremental, calcular qué fotos cambiaron vs. el manifiesto.
            val manifestBefore = if (incremental) BackupManifest.load(context) else null
            val (_, changedPlantPhotos) = if (incremental) {
                manifestBefore!!.diff(plants, plantImagesDir)
            } else {
                emptySet<Int>() to emptySet()
            }
            // Para sighting photos no usamos manifest (son pocas); enviamos solo si es completo.
            val plantPhotoFilter: (File) -> Boolean = when {
                incremental -> { f -> f.name in changedPlantPhotos }
                else -> { _ -> true }
            }

            val nPlantImg = countImagesFiltered(plantImagesDir, plantPhotoFilter)
            val nMushroomImg = if (incremental) 0 else countImages(mushroomImagesDir)
            val nSightImg = if (incremental) 0 else countImages(sightingImagesDir)
            val totalSteps = plants.size + compounds.size + nPlantImg + nMushroomImg + nSightImg + 10
            var step = 0

            // 3. Abrir el OutputStream (con GZIP opcional).
            val rawOut: OutputStream = context.contentResolver.openOutputStream(uri)
                ?: return@withContext Result.failure(IllegalStateException("No se pudo abrir el destino"))
            val bufferedOut = BufferedOutputStream(rawOut, 64 * 1024)
            val finalOut: OutputStream = if (shouldGzip(uri)) GZIPOutputStream(bufferedOut)
            else bufferedOut

            finalOut.use { out ->
                JsonWriter(OutputStreamWriter(out, Charsets.UTF_8)).use { w ->
                    w.beginObject()
                    w.name("backupVersion").value(4)
                    w.name("backupType").value(if (incremental) "incremental" else "full")
                    w.name("exportedAt").value(
                        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                    )
                    w.name("photoRecompression").value(recompression.name)

                    // plants (siempre todas — son ligeras)
                    progress?.onProgress("Plantas…", step, totalSteps)
                    w.name("plants").beginArray()
                    for (p in plants) {
                        gson.toJson(p, PlantEntity::class.java, w)
                        step++
                        if (step % 500 == 0) progress?.onProgress("Plantas…", step, totalSteps)
                    }
                    w.endArray()

                    // compounds
                    w.name("compounds").beginArray()
                    for (c in compounds) {
                        gson.toJson(c, CompoundEntity::class.java, w)
                        step++
                    }
                    w.endArray()
                    progress?.onProgress("Compuestos…", step, totalSteps)

                    // mushrooms
                    w.name("mushrooms").beginArray()
                    for (m in mushrooms) gson.toJson(m, MushroomEntity::class.java, w)
                    w.endArray()

                    // lichens
                    w.name("lichens").beginArray()
                    for (l in lichens) gson.toJson(l, LichenEntity::class.java, w)
                    w.endArray()

                    // plantLocations
                    w.name("plantLocations").beginArray()
                    for (loc in plantLocations) gson.toJson(loc, BackupPlantLocation::class.java, w)
                    w.endArray()

                    // sightings
                    w.name("sightings").beginArray()
                    for (s in sightings) gson.toJson(s, SightingEntity::class.java, w)
                    w.endArray()

                    // calendarEvents
                    w.name("calendarEvents").beginArray()
                    for (e in calendarEvents) gson.toJson(e, com.toxicplants.database.ToxicCalendarEvent::class.java, w)
                    w.endArray()

                    // Capa editable de plantas psicotrópicas sobre el JSON fijo
                    w.name("psychotropicOverrides")
                    gson.toJson(psychotropicOverrides, PsychotropicOverrides::class.java, w)

                    // plantImages (todas o solo las cambiadas según incremental)
                    val phaseImg = if (incremental) "Fotos modificadas…" else "Fotos de plantas…"
                    progress?.onProgress(phaseImg, step, totalSteps)
                    w.name("plantImages").beginArray()
                    writeImagesStreaming(plantImagesDir, w, plantPhotoFilter, recompression) { current ->
                        step = plants.size + compounds.size + current
                        progress?.onProgress("$phaseImg ($current/$nPlantImg)", step, totalSteps)
                    }
                    w.endArray()

                    // mushroomImages: solo en backup completo
                    if (!incremental) {
                        progress?.onProgress("Fotos de setas…", step, totalSteps)
                        w.name("mushroomImages").beginArray()
                        writeImagesStreaming(mushroomImagesDir, w, { true }, recompression) { current ->
                            progress?.onProgress(
                                "Fotos de setas… ($current/$nMushroomImg)",
                                plants.size + compounds.size + nPlantImg + current,
                                totalSteps
                            )
                        }
                        w.endArray()
                    } else {
                        // Marca explícita de que en este backup no van.
                        w.name("mushroomImages").beginArray().endArray()
                    }

                    // sightingImages: solo en backup completo
                    if (!incremental) {
                        progress?.onProgress("Fotos de avistamientos…", step, totalSteps)
                        w.name("sightingImages").beginArray()
                        writeImagesStreaming(sightingImagesDir, w, { true }, recompression) { current ->
                            progress?.onProgress(
                                "Fotos de avistamientos… ($current/$nSightImg)",
                                plants.size + compounds.size + nPlantImg + nMushroomImg + current,
                                totalSteps
                            )
                        }
                        w.endArray()
                    } else {
                        // Marca explícita de que en este backup no van.
                        w.name("sightingImages").beginArray().endArray()
                    }

                    w.endObject()
                    w.flush()
                }
            }

            // 4. Actualizar el manifiesto al ESTADO ACTUAL (no al backup parcial)
            //    para que el próximo incremental sepa qué hay realmente en el móvil.
            try {
                BackupManifest.rebuild(plants, plantImagesDir).save(context)
            } catch (t: Throwable) {
                t.printStackTrace() // que un fallo de manifest no rompa el backup ya escrito
            }

            progress?.onProgress("Listo", totalSteps, totalSteps)
            Result.success(Unit)
        } catch (t: Throwable) {
            t.printStackTrace()
            Result.failure(t)
        }
    }

    private fun countImagesFiltered(dir: File, accept: (File) -> Boolean): Int {
        if (!dir.exists()) return 0
        return dir.listFiles()?.count { it.isFile && it.length() > 0L && accept(it) } ?: 0
    }

    /**
     * Escribe TODAS las imágenes de [dir] que cumplan [accept] como entradas JSON
     * {relativePath, base64} aplicando opcionalmente recompresión [preset].
     */
    private fun writeImagesStreaming(
        dir: File,
        writer: JsonWriter,
        accept: (File) -> Boolean,
        preset: PhotoCompressor.Preset,
        onItem: (current: Int) -> Unit
    ) {
        if (!dir.exists()) return
        val files = dir.listFiles()
            ?.filter { it.isFile && it.length() > 0L && accept(it) }
            ?: return
        for ((idx, file) in files.withIndex()) {
            try {
                val bytes = PhotoCompressor.recompressToBytes(file, preset)
                val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                writer.beginObject()
                writer.name("relativePath").value(file.name)
                writer.name("base64").value(b64)
                writer.endObject()
            } catch (e: Throwable) {
                // No tirar el backup por una foto rota: la saltamos y seguimos.
                e.printStackTrace()
            } finally {
                onItem(idx + 1)
            }
        }
    }

    // ── RECOMPRESIÓN IN-PLACE de las fotos del móvil ────────────────────

    data class RecompressionResult(
        val totalFiles: Int,
        val processed: Int,
        val bytesBefore: Long,
        val bytesAfter: Long
    ) {
        val savedBytes: Long get() = bytesBefore - bytesAfter
        val savedPercent: Int get() =
            if (bytesBefore <= 0) 0 else ((savedBytes * 100) / bytesBefore).toInt()
    }

    /**
     * Recomprime in-place todas las fotos de /plant_images/ con el [preset].
     * Es DESTRUCTIVO: las fotos originales se sustituyen por la versión comprimida.
     * Invalida el manifiesto (rehash al terminar).
     */
    suspend fun recompressLocalPhotos(
        preset: PhotoCompressor.Preset,
        progress: ProgressListener? = null
    ): Result<RecompressionResult> = withContext(Dispatchers.IO) {
        try {
            if (preset == PhotoCompressor.Preset.ORIGINAL) {
                return@withContext Result.failure(IllegalArgumentException("Preset ORIGINAL no recomprime nada"))
            }
            val dir = File(context.filesDir, "plant_images")
            if (!dir.exists()) {
                return@withContext Result.success(RecompressionResult(0, 0, 0L, 0L))
            }
            val files = dir.listFiles()?.filter { it.isFile && it.length() > 0L } ?: emptyList()
            val total = files.size
            var processed = 0
            var bytesBefore = 0L
            var bytesAfter = 0L

            for ((idx, f) in files.withIndex()) {
                val res = PhotoCompressor.recompressInPlace(f, preset)
                if (res != null) {
                    bytesBefore += res.first
                    bytesAfter += res.second
                    processed++
                } else {
                    bytesBefore += f.length()
                    bytesAfter += f.length()
                }
                if ((idx + 1) % 5 == 0 || idx + 1 == total) {
                    progress?.onProgress(
                        "Comprimiendo fotos (${idx + 1}/$total)…",
                        idx + 1, total
                    )
                }
            }

            // Invalidar manifest para que el próximo backup incremental detecte todo como cambiado.
            // Mejor todavía: recalcularlo ya con los nuevos hashes.
            try {
                val plants = db.plantDao().getAllPlantsSync()
                BackupManifest.rebuild(plants, dir).save(context)
            } catch (t: Throwable) {
                t.printStackTrace()
                BackupManifest.clear(context)
            }

            Result.success(RecompressionResult(total, processed, bytesBefore, bytesAfter))
        } catch (t: Throwable) {
            t.printStackTrace()
            Result.failure(t)
        }
    }

    /**
     * Estadísticas rápidas para mostrar en la UI antes de comprimir.
     */
    suspend fun localPhotosStats(): Pair<Int, Long> = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "plant_images")
        if (!dir.exists()) return@withContext 0 to 0L
        val files = dir.listFiles()?.filter { it.isFile } ?: emptyList()
        files.size to files.sumOf { it.length() }
    }

    // ── INFO sobre manifest ─────────────────────────────────────────────

    suspend fun incrementalPreview(): IncrementalPreview = withContext(Dispatchers.IO) {
        val manifest = BackupManifest.load(context)
        val plants = db.plantDao().getAllPlantsSync()
        val dir = File(context.filesDir, "plant_images")
        val (changedPlants, changedPhotos) = manifest.diff(plants, dir)
        IncrementalPreview(
            hasPreviousBackup = manifest.timestamp > 0L,
            previousBackupAt = manifest.timestamp,
            changedPlantsCount = changedPlants.size,
            changedPhotosCount = changedPhotos.size,
            totalPlantsCount = plants.size,
            totalPhotosCount = dir.listFiles()?.count { it.isFile && it.length() > 0L } ?: 0
        )
    }

    data class IncrementalPreview(
        val hasPreviousBackup: Boolean,
        val previousBackupAt: Long,
        val changedPlantsCount: Int,
        val changedPhotosCount: Int,
        val totalPlantsCount: Int,
        val totalPhotosCount: Int
    )

    // ── IMPORT (streaming) ──────────────────────────────────────────────

    suspend fun importDatabaseFromUri(
        uri: Uri,
        progress: ProgressListener? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            progress?.onProgress("Abriendo copia…", 0, 1)

            val raw = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(IllegalStateException("No se pudo abrir el origen"))

            // Sniff de los primeros 2 bytes para detectar GZIP. Usamos mark/reset.
            val pushback = BufferedInputStream(raw, 64 * 1024)
            pushback.mark(4)
            val head = ByteArray(2)
            val n = pushback.read(head, 0, 2)
            pushback.reset()
            val input: InputStream = if (n == 2 && isGzipStream(head)) GZIPInputStream(pushback)
            else pushback

            input.use { stream ->
                JsonReader(InputStreamReader(stream, Charsets.UTF_8)).use { r ->
                    parseAndRestore(r, progress)
                }
            }
            progress?.onProgress("Listo", 1, 1)
            Result.success(Unit)
        } catch (t: Throwable) {
            t.printStackTrace()
            Result.failure(t)
        }
    }

    private suspend fun parseAndRestore(r: JsonReader, progress: ProgressListener?) {
        // Limpieza previa: lo hacemos al detectar los primeros datos para no
        // borrar la BD si el fichero está corrupto.
        var cleaned = false

        r.beginObject()
        while (r.hasNext()) {
            val fieldName = r.nextName()
            when (fieldName) {
                "backupVersion", "exportedAt" -> r.skipValue()

                "plants" -> {
                    if (!cleaned) { cleanAllDb(); cleaned = true }
                    progress?.onProgress("Restaurando plantas…", 0, 1)
                    val batch = ArrayList<PlantEntity>(500)
                    var count = 0
                    r.beginArray()
                    while (r.hasNext()) {
                        val p = gson.fromJson<PlantEntity>(r, PlantEntity::class.java)
                        batch += p
                        count++
                        if (batch.size >= 500) {
                            insertPlantsSafe(batch)
                            batch.clear()
                            progress?.onProgress("Restaurando plantas… ($count)", count, count)
                        }
                    }
                    r.endArray()
                    if (batch.isNotEmpty()) insertPlantsSafe(batch)
                }

                "compounds" -> {
                    if (!cleaned) { cleanAllDb(); cleaned = true }
                    progress?.onProgress("Restaurando compuestos…", 0, 1)
                    val batch = ArrayList<CompoundEntity>(500)
                    r.beginArray()
                    while (r.hasNext()) {
                        val c = gson.fromJson<CompoundEntity>(r, CompoundEntity::class.java)
                        batch += c
                        if (batch.size >= 500) {
                            insertCompoundsSafe(batch); batch.clear()
                        }
                    }
                    r.endArray()
                    if (batch.isNotEmpty()) insertCompoundsSafe(batch)
                }

                "mushrooms" -> {
                    val list = ArrayList<MushroomEntity>()
                    r.beginArray()
                    while (r.hasNext()) list += gson.fromJson<MushroomEntity>(r, MushroomEntity::class.java)
                    r.endArray()
                    if (list.isNotEmpty()) MushroomUserStore.save(context, list)
                }

                "lichens" -> {
                    val list = ArrayList<LichenEntity>()
                    r.beginArray()
                    while (r.hasNext()) list += gson.fromJson<LichenEntity>(r, LichenEntity::class.java)
                    r.endArray()
                    if (list.isNotEmpty()) LichenUserStore.save(context, list)
                }

                "plantLocations" -> {
                    progress?.onProgress("Ubicaciones…", 0, 1)
                    r.beginArray()
                    while (r.hasNext()) {
                        val loc = gson.fromJson<BackupPlantLocation>(r, BackupPlantLocation::class.java)
                        runCatching {
                            db.plantDao().updateLocation(
                                plantId = loc.plantId,
                                lat = loc.latitude,
                                lng = loc.longitude,
                                name = loc.locationName,
                                date = loc.foundDate,
                                notes = loc.notes
                            )
                        }
                    }
                    r.endArray()
                }

                "sightings" -> {
                    val list = ArrayList<SightingEntity>()
                    r.beginArray()
                    while (r.hasNext()) list += gson.fromJson<SightingEntity>(r, SightingEntity::class.java)
                    r.endArray()
                    if (list.isNotEmpty()) {
                        val photoDir = SightingStore.photoDir(context)
                        val restored = list.map { s ->
                            if (s.photoPath.isBlank()) s
                            else s.copy(photoPath = File(photoDir, File(s.photoPath).name).absolutePath)
                        }
                        SightingStore.save(context, restored)
                    }
                }

                "calendarEvents" -> {
                    progress?.onProgress("Eventos del calendario…", 0, 1)
                    r.beginArray()
                    while (r.hasNext()) {
                        val evt = gson.fromJson<com.toxicplants.database.ToxicCalendarEvent>(r, com.toxicplants.database.ToxicCalendarEvent::class.java)
                        runCatching { db.toxicCalendarDao().insert(evt) }
                    }
                    r.endArray()
                }

                "psychotropicOverrides" -> {
                    progress?.onProgress("Restaurando psicotrópicas…", 0, 1)
                    val overrides = gson.fromJson<PsychotropicOverrides>(r, PsychotropicOverrides::class.java)
                    if (overrides != null) {
                        PsychotropicUserStore.save(context, overrides)
                    }
                }

                "plantImages" -> {
                    val dir = File(context.filesDir, "plant_images").apply { if (!exists()) mkdirs() }
                    // Borrar todas las imágenes existentes antes de restaurar
                    dir.listFiles()?.filter { it.isFile }?.forEach { it.delete() }
                    progress?.onProgress("Fotos de plantas…", 0, 1)
                    streamImageArray(r, dir) { i -> progress?.onProgress("Fotos de plantas… ($i)", i, i) }
                }

                "mushroomImages" -> {
                    val dir = File(context.filesDir, "mushroom_images").apply { if (!exists()) mkdirs() }
                    dir.listFiles()?.filter { it.isFile }?.forEach { it.delete() }
                    progress?.onProgress("Fotos de setas…", 0, 1)
                    streamImageArray(r, dir) { i -> progress?.onProgress("Fotos de setas… ($i)", i, i) }
                }

                "sightingImages" -> {
                    val dir = SightingStore.photoDir(context).apply { if (!exists()) mkdirs() }
                    dir.listFiles()?.filter { it.isFile }?.forEach { it.delete() }
                    progress?.onProgress("Fotos de avistamientos…", 0, 1)
                    streamImageArray(r, dir) { i -> progress?.onProgress("Fotos de avistamientos… ($i)", i, i) }
                }

                else -> {
                    // Campo desconocido: lo ignoramos en lugar de fallar.
                    r.skipValue()
                }
            }
        }
        r.endObject()

        if (!cleaned) {
            // Si el fichero no tenía "plants" ni "compounds", al menos limpiamos
            // para evitar mezclas raras.
            cleanAllDb()
        }
    }

    private fun streamImageArray(r: JsonReader, dir: File, onItem: (Int) -> Unit) {
        r.beginArray()
        var i = 0
        while (r.hasNext()) {
            var relativePath: String? = null
            var base64: String? = null
            r.beginObject()
            while (r.hasNext()) {
                when (r.nextName()) {
                    "relativePath" -> relativePath = r.nextString()
                    "base64" -> {
                        if (r.peek() == JsonToken.STRING) base64 = r.nextString()
                        else r.skipValue()
                    }
                    else -> r.skipValue()
                }
            }
            r.endObject()

            val safeName = relativePath
                ?.substringAfterLast('/')
                ?.substringAfterLast('\\')
                ?.takeIf { it.isNotBlank() }
            if (safeName != null && base64 != null) {
                runCatching {
                    val bytes = Base64.decode(base64, Base64.NO_WRAP)
                    File(dir, safeName).writeBytes(bytes)
                }
            }
            i++
            if (i % 25 == 0) onItem(i)
        }
        r.endArray()
        onItem(i)
    }

    private suspend fun cleanAllDb() {
        runCatching { db.plantDao().deleteAllPlants() }
        runCatching { db.compoundDao().deleteAllCompounds() }
    }

    private suspend fun insertPlantsSafe(batch: List<PlantEntity>) {
        try {
            db.plantDao().insertAll(batch)
        } catch (e: Throwable) {
            e.printStackTrace()
            // Plan B: una por una
            for (p in batch) runCatching { db.plantDao().insert(p) }
        }
    }

    private suspend fun insertCompoundsSafe(batch: List<CompoundEntity>) {
        try {
            db.compoundDao().insertAll(batch)
        } catch (e: Throwable) {
            e.printStackTrace()
            for (c in batch) runCatching { db.compoundDao().insert(c) }
        }
    }

    // ── Nombre sugerido ─────────────────────────────────────────────────

    fun getSuggestedFileName(useGzip: Boolean = true): String {
        val ext = if (useGzip) "json.gz" else "json"
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())
        return "PlantasToxicas_Backup_Completo_${dateFormat.format(Date())}.$ext"
    }
}
