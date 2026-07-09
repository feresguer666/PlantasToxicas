package com.toxicplants.database

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer

/**
 * Carga el glosario botánico ilustrado desde assets/glossary.json.
 * Construye además un índice rápido para detectar términos en cualquier
 * texto (descripción, hábitat, etc.) y resaltarlos en la UI.
 */
object GlossaryDataSource {

    private const val ASSET_FILE = "glossary.json"

    @Volatile
    private var cache: GlossaryData? = null

    data class GlossaryData(
        val terms: List<GlossaryTerm>,
        val categories: List<GlossaryCategory>,
        /** Mapa normalizado: forma → término al que apunta. */
        val byForm: Map<String, GlossaryTerm>,
        /** Mapa por id para enlaces directos. */
        val byId: Map<String, GlossaryTerm>
    )

    fun load(context: Context): GlossaryData {
        cache?.let { return it }
        return try {
            val text = context.assets.open(ASSET_FILE)
                .bufferedReader(Charsets.UTF_8).use { it.readText() }
            parse(text).also { cache = it }
        } catch (t: Throwable) {
            t.printStackTrace()
            // Fallback vacío para que la UI no se rompa
            val empty = GlossaryData(emptyList(), emptyList(), emptyMap(), emptyMap())
            cache = empty
            empty
        }
    }

    fun invalidateCache() { cache = null }

    // ── Parser ──────────────────────────────────────────────────────────

    private fun parse(text: String): GlossaryData {
        val root = JSONObject(text)
        val cats = parseCategories(root.optJSONArray("categories"))
        val terms = parseTerms(root.optJSONArray("terms"))

        val byForm = HashMap<String, GlossaryTerm>(terms.size * 3)
        for (t in terms) {
            for (form in t.allForms) {
                val key = normalize(form)
                if (key.isNotEmpty()) byForm[key] = t
            }
        }
        return GlossaryData(
            terms = terms,
            categories = cats,
            byForm = byForm,
            byId = terms.associateBy { it.id }
        )
    }

    private fun parseCategories(arr: JSONArray?): List<GlossaryCategory> {
        if (arr == null) return emptyList()
        val out = ArrayList<GlossaryCategory>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            out += GlossaryCategory(
                id = o.optString("id"),
                label = o.optString("label"),
                icon = o.optString("icon", "")
            )
        }
        return out
    }

    private fun parseTerms(arr: JSONArray?): List<GlossaryTerm> {
        if (arr == null) return emptyList()
        val out = ArrayList<GlossaryTerm>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val syn = ArrayList<String>()
            o.optJSONArray("synonyms")?.let { sa ->
                for (j in 0 until sa.length()) sa.optString(j).takeIf { it.isNotBlank() }?.let { syn += it }
            }
            out += GlossaryTerm(
                id = o.optString("id").takeIf { it.isNotBlank() } ?: continue,
                category = o.optString("category"),
                term = o.optString("term"),
                synonyms = syn,
                definition = o.optString("definition", ""),
                image = o.optString("image", "").takeIf { it.isNotBlank() },
                wikimediaSearch = o.optString("wikimediaSearch", "").takeIf { it.isNotBlank() }
            )
        }
        return out
    }

    // ── Helpers públicos ────────────────────────────────────────────────

    /** Normaliza una palabra a su forma de búsqueda (minúsculas, sin acentos). */
    fun normalize(s: String): String {
        val nfd = Normalizer.normalize(s.trim().lowercase(), Normalizer.Form.NFD)
        val noAccents = nfd.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        return noAccents
    }

    /**
     * Busca términos del glosario dentro de [text] y devuelve una lista de
     * coincidencias [Match] (con índices originales) para poder pintar
     * subrayados clicables.
     */
    data class Match(
        val start: Int,
        val end: Int,
        val term: GlossaryTerm
    )

    fun findMatches(text: String, glossary: GlossaryData): List<Match> {
        if (text.isBlank() || glossary.byForm.isEmpty()) return emptyList()
        val matches = ArrayList<Match>()
        // Normalizamos texto pero conservamos índices originales: lo más simple
        // es buscar cada forma como palabra completa con regex case-insensitive.
        // Ordenamos formas por longitud descendente para que "compuesta palmeada"
        // gane a "palmeada".
        val formsSorted = glossary.byForm.entries.sortedByDescending { it.key.length }
        val taken = BooleanArray(text.length)

        for ((normForm, term) in formsSorted) {
            // Patrón: límites de palabra (\b) — usamos lookbehind/lookahead que
            // no exijan acento.
            val pattern = Regex(
                "(?<![\\p{L}\\p{Nd}])" +
                        formToRegexCaseInsensitive(normForm) +
                        "(?![\\p{L}\\p{Nd}])",
                RegexOption.IGNORE_CASE
            )
            for (m in pattern.findAll(text)) {
                val s = m.range.first
                val e = m.range.last + 1
                // Saltar si solapamos algo ya marcado
                if ((s until e).any { it < taken.size && taken[it] }) continue
                matches += Match(s, e, term)
                for (i in s until e) if (i < taken.size) taken[i] = true
            }
        }
        return matches.sortedBy { it.start }
    }

    /**
     * Crea un regex que ignora acentos pero respeta el resto.
     * Ej: "umbela" → "[uúûü]m[bß]el[aá]" (simplificado).
     */
    private fun formToRegexCaseInsensitive(normForm: String): String {
        val sb = StringBuilder()
        for (c in normForm) {
            when (c) {
                'a' -> sb.append("[aáàâä]")
                'e' -> sb.append("[eéèêë]")
                'i' -> sb.append("[iíìîï]")
                'o' -> sb.append("[oóòôö]")
                'u' -> sb.append("[uúùûü]")
                'n' -> sb.append("[nñ]")
                ' ' -> sb.append("\\s+")
                else -> {
                    if (c.isLetterOrDigit()) sb.append(c)
                    else sb.append(Regex.escape(c.toString()))
                }
            }
        }
        return sb.toString()
    }
}
