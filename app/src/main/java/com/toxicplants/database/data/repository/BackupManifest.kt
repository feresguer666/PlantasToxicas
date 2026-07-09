package com.toxicplants.database.data.repository

import android.content.Context
import com.google.gson.Gson
import com.toxicplants.database.PlantEntity
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/**
 * Manifiesto persistente con hashes MD5 de las plantas y de las fotos
 * en el momento del último backup. Sirve para hacer backups incrementales:
 *
 *  - plants: { "<plantId>": "<md5 del JSON de la planta>" }
 *  - photos: { "<plant_<id>.jpg>": "<md5 del binario>" }
 *
 * Vive en: filesDir/backup_manifest.json
 *
 * Operaciones típicas:
 *   val current = BackupManifest.load(context)
 *   val (changedPlants, changedPhotos) = current.diff(allPlants, allPhotosDir)
 *   ... exportar solo lo cambiado ...
 *   BackupManifest.rebuild(allPlants, allPhotosDir).save(context)
 */
data class BackupManifest(
    val version: Int = 1,
    val timestamp: Long = 0L,
    val plants: MutableMap<Int, String> = mutableMapOf(),
    val photos: MutableMap<String, String> = mutableMapOf()
) {

    /**
     * Compara el estado actual con el manifiesto.
     * @return (idsDePlantasCambiadas, nombresDeFotosCambiadasONuevas)
     */
    fun diff(currentPlants: List<PlantEntity>, photosDir: File): Pair<Set<Int>, Set<String>> {
        val changedPlants = HashSet<Int>()
        for (p in currentPlants) {
            val h = md5OfPlant(p)
            if (plants[p.id] != h) changedPlants += p.id
        }
        val changedPhotos = HashSet<String>()
        if (photosDir.exists()) {
            for (f in photosDir.listFiles()?.filter { it.isFile && it.length() > 0L } ?: emptyList()) {
                val h = md5OfFile(f)
                if (photos[f.name] != h) changedPhotos += f.name
            }
        }
        return changedPlants to changedPhotos
    }

    /** Guarda el manifiesto en filesDir. */
    fun save(context: Context) {
        try {
            val gson = Gson()
            val file = File(context.filesDir, FILENAME)
            file.writeText(gson.toJson(this))
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    companion object {
        private const val FILENAME = "backup_manifest.json"

        /** Carga el manifiesto guardado o devuelve uno vacío si no existe. */
        fun load(context: Context): BackupManifest {
            return try {
                val file = File(context.filesDir, FILENAME)
                if (!file.exists()) return BackupManifest()
                val txt = file.readText()
                if (txt.isBlank()) return BackupManifest()
                Gson().fromJson(txt, BackupManifest::class.java) ?: BackupManifest()
            } catch (t: Throwable) {
                t.printStackTrace()
                BackupManifest()
            }
        }

        /**
         * Reconstruye el manifiesto desde cero a partir del estado actual.
         * Llamar tras un backup completo o tras una recompresión.
         */
        fun rebuild(currentPlants: List<PlantEntity>, photosDir: File): BackupManifest {
            val m = BackupManifest(timestamp = System.currentTimeMillis())
            for (p in currentPlants) m.plants[p.id] = md5OfPlant(p)
            if (photosDir.exists()) {
                for (f in photosDir.listFiles()?.filter { it.isFile && it.length() > 0L } ?: emptyList()) {
                    m.photos[f.name] = md5OfFile(f)
                }
            }
            return m
        }

        /** Borra el manifiesto (forzará un backup completo la próxima vez). */
        fun clear(context: Context) {
            runCatching { File(context.filesDir, FILENAME).delete() }
        }

        // ── Hashing ─────────────────────────────────────────────────────

        fun md5OfPlant(p: PlantEntity): String {
            // Serializamos los campos significativos en orden estable.
            val o = JSONObject().apply {
                put("id", p.id)
                put("commonName", p.commonName)
                put("commonNames", p.commonNames)
                put("scientificName", p.scientificName)
                put("family", p.family)
                put("toxicityLevel", p.toxicityLevel)
                put("toxicParts", p.toxicParts)
                put("symptoms", p.symptoms)
                put("description", p.description)
                put("habitat", p.habitat)
                put("geographicDistribution", p.geographicDistribution)
                put("firstAid", p.firstAid)
                put("imageUrl", p.imageUrl)
                put("isFavorite", p.isFavorite)
                put("category", p.category)
                put("latitude", p.latitude ?: JSONObject.NULL)
                put("longitude", p.longitude ?: JSONObject.NULL)
                put("locationName", p.locationName ?: JSONObject.NULL)
                put("foundDate", p.foundDate ?: JSONObject.NULL)
                put("notes", p.notes ?: JSONObject.NULL)
            }
            return md5(o.toString().toByteArray(Charsets.UTF_8))
        }

        fun md5OfFile(f: File): String = try {
            val md = MessageDigest.getInstance("MD5")
            f.inputStream().use { ins ->
                val buf = ByteArray(64 * 1024)
                while (true) {
                    val n = ins.read(buf)
                    if (n <= 0) break
                    md.update(buf, 0, n)
                }
            }
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (t: Throwable) {
            t.printStackTrace()
            ""
        }

        private fun md5(bytes: ByteArray): String = MessageDigest.getInstance("MD5")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }
    }
}
