package com.toxicplants.database.ui.search

import java.text.Normalizer
import kotlin.math.max
import kotlin.math.min

/** Utilidades pequeñas para búsquedas tolerantes a errores.
 *
 * Objetivos:
 *  - Ignorar acentos y mayúsculas: "vomitos" == "vómitos".
 *  - Tolerar errores cortos: "beladona" encuentra "belladonna".
 *  - Permitir búsquedas multi-término: "vómitos + taquicardia".
 */
data class SearchQuery(
    val raw: String,
    val normalized: String,
    val tokens: List<String>,
    val terms: List<String>
) {
    val isBlank: Boolean get() = tokens.isEmpty()
}

fun buildSearchQuery(raw: String): SearchQuery {
    val normalized = raw.normalizeForSearch()
    val terms = raw
        .split(Regex("\\s*(?:\\+|,|;|/|\\n|\\by\\b|\\band\\b)\\s*", RegexOption.IGNORE_CASE))
        .map { it.normalizeForSearch() }
        .filter { it.length >= 2 }
        .distinct()
        .ifEmpty { listOf(normalized).filter { it.isNotBlank() } }

    return SearchQuery(
        raw = raw,
        normalized = normalized,
        tokens = normalized.searchTokens(),
        terms = terms
    )
}

fun String.normalizeForSearch(): String {
    val withoutAccents = Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
    return withoutAccents
        .lowercase()
        .replace('×', 'x')
        .replace(Regex("[^a-z0-9ñ]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

fun String.searchTokens(): List<String> =
    split(' ')
        .map { it.trim() }
        .filter { it.length >= 2 }
        .distinct()

/**
 * Puntuación de coincidencia entre [text] y [query]. 0 significa sin coincidencia.
 */
fun fuzzyTextScore(text: String, query: SearchQuery): Int {
    if (query.isBlank) return 0
    val normalizedText = text.normalizeForSearch()
    if (normalizedText.isBlank()) return 0

    val textTokens = normalizedText.searchTokens()
    if (textTokens.isEmpty()) return 0

    var score = 0

    // Coincidencia de frase completa: muy fuerte.
    if (query.normalized.length >= 2 && normalizedText.contains(query.normalized)) {
        score += 80 + query.normalized.length.coerceAtMost(40)
    }

    // Todos los tokens contribuyen. Permitimos distancia Levenshtein baja.
    for (queryToken in query.tokens) {
        val tokenScore = bestTokenScore(queryToken, textTokens, normalizedText)
        if (tokenScore == 0) return 0
        score += tokenScore
    }

    return score
}

/** Igual que [fuzzyTextScore], pero no exige que todos los tokens coincidan. */
fun partialFuzzyTextScore(text: String, query: SearchQuery): Int {
    if (query.isBlank) return 0
    val normalizedText = text.normalizeForSearch()
    if (normalizedText.isBlank()) return 0
    val textTokens = normalizedText.searchTokens()
    if (textTokens.isEmpty()) return 0

    var score = 0
    if (query.normalized.length >= 2 && normalizedText.contains(query.normalized)) {
        score += 80 + query.normalized.length.coerceAtMost(40)
    }
    for (queryToken in query.tokens) {
        score += bestTokenScore(queryToken, textTokens, normalizedText)
    }
    return score
}

fun splitSymptomTerms(query: String): List<SearchQuery> =
    buildSearchQuery(query).terms
        .map { buildSearchQuery(it) }
        .filter { !it.isBlank }

data class SymptomMatchScore(
    val score: Int,
    val matchedTerms: Int,
    val totalTerms: Int
) {
    val isMatch: Boolean get() = score > 0 && matchedTerms > 0
}

/**
 * Puntúa búsquedas por varios síntomas. Devuelve coincidencias parciales,
 * pero premia mucho las plantas que coinciden con más síntomas.
 */
fun multiTermFieldScore(
    terms: List<SearchQuery>,
    weightedFields: List<Pair<String, Int>>
): SymptomMatchScore {
    if (terms.isEmpty()) return SymptomMatchScore(0, 0, 0)

    var totalScore = 0
    var matched = 0

    for (term in terms) {
        var bestForTerm = 0
        for ((field, weight) in weightedFields) {
            val s = partialFuzzyTextScore(field, term)
            if (s > 0) bestForTerm = max(bestForTerm, s * weight)
        }
        if (bestForTerm > 0) {
            matched++
            totalScore += bestForTerm
        }
    }

    // Premio para coincidencias múltiples.
    totalScore += matched * 350
    if (matched == terms.size && terms.size > 1) totalScore += 900

    return SymptomMatchScore(totalScore, matched, terms.size)
}

private fun bestTokenScore(queryToken: String, textTokens: List<String>, normalizedText: String): Int {
    if (queryToken.length >= 2 && normalizedText.contains(queryToken)) {
        return 22 + queryToken.length.coerceAtMost(18)
    }

    var best = 0

    // Primera pasada: coincidencias baratas. No usa Levenshtein.
    for (textToken in textTokens) {
        val current = when {
            textToken == queryToken -> 60
            textToken.startsWith(queryToken) && queryToken.length >= 3 -> 48
            queryToken.startsWith(textToken) && textToken.length >= 4 -> 38
            textToken.contains(queryToken) && queryToken.length >= 3 -> 34
            queryToken.contains(textToken) && textToken.length >= 4 -> 28
            else -> 0
        }
        if (current > best) best = current
    }
    if (best > 0) return best

    // Segunda pasada: errores tipográficos. Es lo caro, así que se limita mucho.
    if (queryToken.length < 4) return 0
    for (textToken in textTokens) {
        if (!couldBeTypoCandidate(queryToken, textToken)) continue
        if (isCloseTypo(queryToken, textToken)) return 24
    }
    return 0
}

private fun couldBeTypoCandidate(a: String, b: String): Boolean {
    val minLen = min(a.length, b.length)
    val maxLen = max(a.length, b.length)
    if (minLen < 4) return false
    if (maxLen - minLen > 2) return false
    if (a.firstOrNull() != b.firstOrNull()) return false
    if (minLen >= 5 && commonPrefixLength(a, b) < 2) return false
    return true
}

private fun commonPrefixLength(a: String, b: String): Int {
    val limit = min(a.length, b.length)
    var count = 0
    while (count < limit && a[count] == b[count]) count++
    return count
}

private fun isCloseTypo(a: String, b: String): Boolean {
    val maxLen = max(a.length, b.length)
    val limit = if (maxLen <= 6) 1 else 2
    val distance = levenshteinAtMost(a, b, limit)
    return distance <= limit
}

private fun levenshteinAtMost(a: String, b: String, maxDistance: Int): Int {
    if (kotlin.math.abs(a.length - b.length) > maxDistance) return maxDistance + 1

    var previous = IntArray(b.length + 1) { it }
    var current = IntArray(b.length + 1)

    for (i in 1..a.length) {
        current[0] = i
        var rowMin = current[0]
        for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            current[j] = minOf(
                previous[j] + 1,
                current[j - 1] + 1,
                previous[j - 1] + cost
            )
            rowMin = min(rowMin, current[j])
        }
        if (rowMin > maxDistance) return maxDistance + 1
        val tmp = previous
        previous = current
        current = tmp
    }
    return previous[b.length]
}
