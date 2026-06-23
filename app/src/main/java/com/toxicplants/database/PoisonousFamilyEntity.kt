package com.toxicplants.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Registro editable de un género perteneciente a una familia venenosa.
 *
 * La pantalla de "Familias venenosas" agrupa estos registros por `familyName`.
 * Así se puede mostrar:
 * - familias totales,
 * - géneros por familia,
 * - especies totales estimadas por familia,
 * y abrir cada género para editar toxinas, síntomas, partes tóxicas y notas.
 */
@Entity(
    tableName = "poisonous_family_genera",
    indices = [
        Index("familyName"),
        Index("genusName")
    ]
)
data class PoisonousFamilyGenusEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val familyName: String = "",
    val genusName: String = "",
    val genusSpeciesCount: Int = 0,
    val toxins: String = "",
    val symptoms: String = "",
    val toxicParts: String = "",
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
)

data class PoisonousFamilySummary(
    val familyName: String,
    val generaCount: Int,
    val speciesCount: Int,
)
