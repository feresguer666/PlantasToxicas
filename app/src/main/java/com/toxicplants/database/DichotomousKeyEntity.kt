package com.toxicplants.database

/**
 * Modelos para Claves Dicotómicas Interactivas (versión 2 — basada en FILTROS).
 *
 * Cada opción aplica un filtro acumulativo sobre el catálogo. Al llegar a una
 * hoja terminal (sin nextNodeId), la lista de plantas candidatas se calcula
 * dinámicamente como la intersección de todos los filtros del camino más
 * (opcionalmente) los IDs declarados a mano en resultPlantIds.
 *
 * Estructura del JSON:
 * {
 *   "version": 2,
 *   "keys": [
 *     {
 *       "id": "general",
 *       "title": "...",
 *       "scope": "general" | "family" | "category",
 *       "rootNodeId": "n_root",
 *       "nodes": [
 *         {
 *           "id": "n_root",
 *           "question": "¿...?",
 *           "options": [
 *             {
 *               "label": "Opción A",
 *               "image": "key_images/xxx.png",
 *               "filter": {
 *                 "families": ["Solanaceae"],
 *                 "anyKeyword": ["trompeta", "tubular"],
 *                 "categories": ["Silvestre"]
 *               },
 *               "nextNodeId": "n_2"
 *             }
 *           ]
 *         }
 *       ]
 *     }
 *   ]
 * }
 */

/**
 * Filtro declarativo sobre el catálogo. Una planta pasa el filtro si
 * cumple TODOS los criterios definidos (los criterios vacíos se ignoran).
 *
 * - families:        debe coincidir con alguna (exacto, case-insensitive, prefijo).
 * - notFamilies:     no debe coincidir con ninguna.
 * - categories:      debe estar en alguna (case-insensitive).
 * - toxicityLevels:  debe ser uno de estos niveles.
 * - genera:          el primer término del scientificName debe estar aquí (Datura, Aconitum...).
 * - allKeywords:     TODAS las keywords deben aparecer en algún campo textual.
 * - anyKeyword:      AL MENOS UNA debe aparecer en algún campo textual.
 * - noneKeyword:     NINGUNA debe aparecer.
 *
 * Los keywords se buscan en commonName, commonNames, scientificName, family,
 * description, habitat, toxicParts, symptoms, geographicDistribution.
 */
data class KeyFilter(
    val families: List<String> = emptyList(),
    val notFamilies: List<String> = emptyList(),
    val categories: List<String> = emptyList(),
    val toxicityLevels: List<String> = emptyList(),
    val genera: List<String> = emptyList(),
    val allKeywords: List<String> = emptyList(),
    val anyKeyword: List<String> = emptyList(),
    val noneKeyword: List<String> = emptyList()
) {
    fun isEmpty(): Boolean =
        families.isEmpty() && notFamilies.isEmpty() && categories.isEmpty() &&
                toxicityLevels.isEmpty() && genera.isEmpty() &&
                allKeywords.isEmpty() && anyKeyword.isEmpty() && noneKeyword.isEmpty()
}

/** Una opción dentro de una pregunta (una "rama" del nodo). */
data class KeyOptionEntity(
    val label: String,
    val description: String = "",
    /** Ruta a una imagen de apoyo dentro de assets/ (ej: "key_images/leaf_simple.png"). */
    val image: String? = null,
    /** Si lleva a otro nodo (rama interna). Mutuamente excluyente con resultPlantIds/hoja. */
    val nextNodeId: String? = null,
    /** Filtro que aplica esta opción al elegirla (se acumula con los anteriores). */
    val filter: KeyFilter = KeyFilter(),
    /** IDs adicionales que se añaden SIEMPRE al resultado (incluso si no pasan los filtros). */
    val resultPlantIds: List<Int> = emptyList(),
    /** Texto opcional que se muestra al llegar al resultado (notas, advertencias). */
    val resultNote: String = "",
    /** Si true: en la pantalla de resultado se ignoran filtros anteriores y solo se usa este filtro + resultPlantIds. */
    val resetFilters: Boolean = false
)

/** Un nodo del árbol = una pregunta con sus opciones. */
data class KeyNodeEntity(
    val id: String,
    val question: String,
    /** Texto adicional de ayuda contextual (puede estar vacío). */
    val help: String = "",
    val options: List<KeyOptionEntity> = emptyList()
)

/** Una clave dicotómica completa (general, por familia, por categoría...). */
data class DichotomousKeyEntity(
    val id: String,
    val title: String,
    val subtitle: String = "",
    /** "general" | "family" | "category" | otro */
    val scope: String = "general",
    /** Si scope == "family", nombre de la familia (filtro base auto-aplicado). */
    val family: String = "",
    /** Si scope == "category", nombre de la categoría (filtro base auto-aplicado). */
    val category: String = "",
    /** Nombre de un icono Material (filled) opcional. */
    val icon: String = "filter_alt",
    val rootNodeId: String,
    val nodes: List<KeyNodeEntity> = emptyList()
) {
    /** Acceso rápido por id de nodo. */
    val nodesById: Map<String, KeyNodeEntity> by lazy { nodes.associateBy { it.id } }

    fun rootNode(): KeyNodeEntity? = nodesById[rootNodeId]

    /** Filtro base que aplica esta clave a todo su recorrido (por familia/categoría). */
    fun baseFilter(): KeyFilter = KeyFilter(
        families = if (scope == "family" && family.isNotBlank()) listOf(family) else emptyList(),
        categories = if (scope == "category" && category.isNotBlank()) listOf(category) else emptyList()
    )
}

/**
 * Lógica de aplicación de filtros sobre la lista de plantas.
 * Vive aquí para que el ViewModel sea fino y la testabilidad sea más fácil.
 */
object PlantFilterEngine {

    /** Une varios filtros en uno (AND lógico de todos sus criterios). */
    fun combine(filters: List<KeyFilter>): KeyFilter {
        val nonEmpty = filters.filter { !it.isEmpty() }
        if (nonEmpty.isEmpty()) return KeyFilter()
        return KeyFilter(
            families = nonEmpty.flatMap { it.families }.distinct(),
            notFamilies = nonEmpty.flatMap { it.notFamilies }.distinct(),
            categories = nonEmpty.flatMap { it.categories }.distinct(),
            toxicityLevels = nonEmpty.flatMap { it.toxicityLevels }.distinct(),
            genera = nonEmpty.flatMap { it.genera }.distinct(),
            allKeywords = nonEmpty.flatMap { it.allKeywords }.distinct(),
            anyKeyword = nonEmpty.flatMap { it.anyKeyword }.distinct(),
            noneKeyword = nonEmpty.flatMap { it.noneKeyword }.distinct()
        )
    }

    /** Aplica el filtro al catálogo. */
    fun apply(plants: List<PlantEntity>, filter: KeyFilter): List<PlantEntity> {
        if (filter.isEmpty()) return plants
        return plants.filter { matches(it, filter) }
    }

    private fun matches(p: PlantEntity, f: KeyFilter): Boolean {
        // Familia
        if (f.families.isNotEmpty() &&
            f.families.none { p.family.equals(it, ignoreCase = true) ||
                    p.family.startsWith(it, ignoreCase = true) }) return false
        if (f.notFamilies.isNotEmpty() &&
            f.notFamilies.any { p.family.equals(it, ignoreCase = true) ||
                    p.family.startsWith(it, ignoreCase = true) }) return false

        // Categoría
        if (f.categories.isNotEmpty() &&
            f.categories.none { p.category.equals(it, ignoreCase = true) }) return false

        // Toxicidad
        if (f.toxicityLevels.isNotEmpty() &&
            f.toxicityLevels.none { p.toxicityLevel.equals(it, ignoreCase = true) }) return false

        // Género (primer término del scientificName)
        if (f.genera.isNotEmpty()) {
            val first = p.scientificName.trim().substringBefore(' ').lowercase()
            if (f.genera.none { it.lowercase() == first }) return false
        }

        // Keywords
        if (f.allKeywords.isNotEmpty() || f.anyKeyword.isNotEmpty() || f.noneKeyword.isNotEmpty()) {
            val haystack = buildHaystack(p)
            if (f.allKeywords.any { !haystack.contains(it.lowercase()) }) return false
            if (f.anyKeyword.isNotEmpty() &&
                f.anyKeyword.none { haystack.contains(it.lowercase()) }) return false
            if (f.noneKeyword.any { haystack.contains(it.lowercase()) }) return false
        }
        return true
    }

    private fun buildHaystack(p: PlantEntity): String = buildString {
        append(p.commonName).append(' ')
        append(p.commonNames).append(' ')
        append(p.scientificName).append(' ')
        append(p.family).append(' ')
        append(p.description).append(' ')
        append(p.habitat).append(' ')
        append(p.toxicParts).append(' ')
        append(p.symptoms).append(' ')
        append(p.geographicDistribution).append(' ')
        append(p.category)
    }.lowercase()
}
