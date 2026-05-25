package com.toxicplants.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Sustancia tóxica de origen vegetal/fúngico (alcaloides, glucósidos, saponinas, etc.).
 *
 * Forma parte de la pantalla Fitoquímica. Está separada de [PlantEntity] porque una misma
 * sustancia suele estar en varias plantas y porque la información farmacológica/toxicológica
 * es muy distinta a la información botánica.
 */
@Entity(
    tableName = "compounds",
    indices = [
        Index("groupName"),
        Index("commonName")
    ]
)
data class CompoundEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    /** Nombre común con el que se conoce ("Atropina", "Oleandrina"...). */
    val commonName: String,
    /** Nombre IUPAC u oficial completo si aplica. */
    val iupacName: String = "",
    /** Grupo fitoquímico: "Alcaloides", "Glucósidos cardiotónicos", "Saponinas"... */
    val groupName: String,
    /** Subgrupo opcional ("Tropánico", "Pirrolizidínico"...). */
    val subgroup: String = "",
    /** Fórmula molecular ("C17H23NO3"). */
    val molecularFormula: String = "",
    /** Peso molecular en g/mol. */
    val molecularWeight: Double? = null,

    /**
     * Plantas que lo contienen separadas por "|".
     * Cada elemento es preferentemente el nombre científico en latín
     * ("Atropa belladonna|Hyoscyamus niger|Datura stramonium").
     */
    val sourcePlants: String,

    /** Texto libre sobre concentraciones típicas en la planta. */
    val concentration: String = "",
    /** Mecanismo molecular de toxicidad. */
    val mechanism: String,
    /** Dosis letal mediana (LD50) y especie/ruta de referencia. */
    val ld50: String = "",
    /** Dosis tóxica orientativa para humanos. */
    val toxicDose: String = "",

    /** Cuadro clínico organizado por sistema. */
    val clinicalNeuro: String = "",
    val clinicalCardio: String = "",
    val clinicalDigestive: String = "",
    val clinicalRespiratory: String = "",
    val clinicalDermal: String = "",
    val clinicalOther: String = "",

    /** Tiempo de latencia hasta aparición de síntomas. */
    val onsetTime: String = "",
    /** Duración esperada y pronóstico. */
    val duration: String = "",
    /** Tratamiento / antídoto. */
    val treatment: String = "",
    /** Notas adicionales (historia, usos médicos, curiosidades). */
    val notes: String = "",
    /** Color hex sugerido para el chip del grupo ("#7B1FA2"). */
    val groupColor: String = "#7B1FA2",
    val isFavorite: Boolean = false,
)
