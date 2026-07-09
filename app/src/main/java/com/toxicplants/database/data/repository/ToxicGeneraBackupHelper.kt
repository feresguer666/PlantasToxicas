package com.toxicplants.database.data.repository

import android.content.Context
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ToxicGeneraBackupHelper {
    private const val USER_FILE = "toxic_genera_user.json"
    private const val CACHE_DIR = "gbif_cache"

    fun exportToZip(context: Context, zos: ZipOutputStream) {
        val filesDir = context.filesDir
        // 1. user store
        val userFile = File(filesDir, USER_FILE)
        if (userFile.exists()) {
            zos.putNextEntry(ZipEntry(USER_FILE))
            userFile.inputStream().use { it.copyTo(zos) }
            zos.closeEntry()
        }
        // 2. gbif cache
        val cacheDir = File(filesDir, CACHE_DIR)
        if (cacheDir.exists()) {
            cacheDir.walkTopDown().filter { it.isFile }.forEach { f ->
                val rel = "$CACHE_DIR/${f.name}"
                zos.putNextEntry(ZipEntry(rel))
                f.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
    }

    fun importFromZip(context: Context, zis: ZipInputStream, entryName: String): Boolean {
        val filesDir = context.filesDir
        return when {
            entryName == USER_FILE -> {
                File(filesDir, USER_FILE).outputStream().use { out ->
                    zis.copyTo(out)
                }
                true
            }

            entryName.startsWith("$CACHE_DIR/") -> {
                val target = File(filesDir, entryName)
                target.parentFile?.mkdirs()
                target.outputStream().use { out -> zis.copyTo(out) }
                true
            }

            else -> false
        }
    }

    fun getStats(context: Context): Pair<Int, Int> {
        val filesDir = context.filesDir
        val userFile = File(filesDir, USER_FILE)
        val customCount = try {
            if (!userFile.exists()) 0 else {
                val txt = userFile.readText()
                // cuenta rápida: "genus":
                Regex("\"genus\"\\s*:").findAll(txt).count()
            }
        } catch (_: Exception) {
            0
        }
        val cacheCount = File(filesDir, CACHE_DIR).listFiles()?.size ?: 0
        return customCount to cacheCount
    }
}