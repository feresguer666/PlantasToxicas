package com.toxicplants.database

/**
 * Entrada del glosario botánico ilustrado.
 *
 * - term: palabra principal a mostrar como título.
 * - synonyms: alternativas también detectables en textos.
 * - image: ilustración esquemática (PNG dibujado, opcional).
 * - wikimediaSearch: término de búsqueda en Wikimedia Commons para
 *   descargar fotos reales automáticamente la primera vez.
 *   (ej: "Umbel Apiaceae" para 'umbela').
 */
data class GlossaryTerm(
    val id: String,
    val category: String,
    val term: String,
    val synonyms: List<String> = emptyList(),
    val definition: String = "",
    val image: String? = null,
    val wikimediaSearch: String? = null
) {
    val allForms: List<String> get() = listOf(term) + synonyms
}

data class GlossaryCategory(
    val id: String,
    val label: String,
    val icon: String = ""
)
