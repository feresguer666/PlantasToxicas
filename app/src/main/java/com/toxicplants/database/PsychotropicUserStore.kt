package com.toxicplants.database

import android.content.Context
import com.google.gson.Gson
import java.io.File

/**
 * Capa editable de "Plantas psicotrópicas" sobre el índice fijo de assets.
 *
 * El catálogo base vive en assets/psychotropic_plants.json y no se puede modificar
 * en tiempo de ejecución. Este fichero interno guarda solo los cambios del usuario:
 *  - ids ocultos/eliminados del catálogo base
 *  - entradas añadidas o editadas por el usuario
 */
data class PsychotropicOverrides(
    val version: Int = 1,
    val updatedAt: Long = System.currentTimeMillis(),
    val hiddenPlantIds: List<Int> = emptyList(),
    val customItems: List<PsychotropicOverrideItem> = emptyList()
)

data class PsychotropicOverrideItem(
    val plant: PlantEntity,
    val categories: List<String>,
    val compounds: List<String> = emptyList(),
    val reasons: List<String> = emptyList(),
    val score: Int = 10_000
)

object PsychotropicUserStore {
    private const val FILENAME = "psychotropic_overrides.json"
    private val gson = Gson()

    fun file(context: Context): File = File(context.filesDir, FILENAME)

    fun load(context: Context): PsychotropicOverrides = try {
        val f = file(context)
        if (!f.exists() || f.length() == 0L) PsychotropicOverrides()
        else gson.fromJson(f.readText(Charsets.UTF_8), PsychotropicOverrides::class.java)
            ?: PsychotropicOverrides()
    } catch (t: Throwable) {
        t.printStackTrace()
        PsychotropicOverrides()
    }

    fun save(context: Context, overrides: PsychotropicOverrides) {
        try {
            file(context).writeText(
                gson.toJson(overrides.copy(updatedAt = System.currentTimeMillis())),
                Charsets.UTF_8
            )
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    fun clear(context: Context) {
        runCatching { file(context).delete() }
    }
}
