package com.toxicplants.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Eventos del calendario de tóxicos: alertas, intoxicaciones y notas del usuario.
 */
@Entity(tableName = "calendar_events")
data class ToxicCalendarEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    /** Título del evento. */
    val title: String,
    /** Descripción detallada. */
    val description: String = "",
    /** Fecha en formato ISO: yyyy-MM-dd */
    val date: String,
    /** ID de planta relacionada (opcional). */
    val plantId: Int? = null,
    /** Nombre de la planta relacionada (desnormalizado para consultas rápidas). */
    val plantName: String? = null,
    /**
     * Tipo de evento:
     * - "alert"   → Alerta/aviso preventivo
     * - "incident"→ Intoxicación registrada
     * - "note"    → Nota personal
     */
    val eventType: String = "note",
    /** Marca de tiempo de creación. */
    val createdAt: Long = System.currentTimeMillis()
)
