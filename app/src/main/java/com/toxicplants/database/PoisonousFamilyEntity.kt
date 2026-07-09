package com.toxicplants.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Registro editable de un género venenoso dentro de una familia.
 *
 * catalogType:
 * - "all": familias tratadas como venenosas completas.
 * - "partial": familias mixtas con algunos géneros venenosos.
 */
@Entity(
    tableName = "poisonous_family_genera",
    indices = [
        Index("familyName"),
        Index("genusName"),
        Index("catalogType")
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
    val catalogType: String = PoisonousFamilyCatalogType.ALL,
    val updatedAt: Long = System.currentTimeMillis(),
)

data class PoisonousFamilySummary(
    val familyName: String,
    val generaCount: Int,
    val speciesCount: Int,
)

object PoisonousFamilyCatalogType {
    const val ALL = "all"
    const val PARTIAL = "partial"

    fun label(value: String): String = when (value) {
        ALL -> "Todos sus géneros venenosos"
        PARTIAL -> "Algunos géneros venenosos"
        else -> value
    }
}
