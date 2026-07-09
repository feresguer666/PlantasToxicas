package com.toxicplants.database

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Carga las claves dicotómicas desde `assets/dichotomous_keys.json`.
 *
 * Soporta formato v2 (basado en filtros). Si falla cualquier cosa, devuelve
 * un fallback mínimo para que la sección nunca se caiga.
 */
object DichotomousKeyDataSource {

    private const val ASSET_FILE = "dichotomous_keys.json"

    /** Caché en memoria — el JSON puede pesar varios cientos de KB, no lo releemos en cada navegación. */
    @Volatile
    private var cache: List<DichotomousKeyEntity>? = null

    fun loadAll(context: Context): List<DichotomousKeyEntity> {
        cache?.let { return it }
        return try {
            val loaded = loadFromAssets(context).ifEmpty { fallbackKeys() }
            cache = loaded
            loaded
        } catch (t: Throwable) {
            t.printStackTrace()
            val fb = fallbackKeys()
            cache = fb
            fb
        }
    }

    fun loadById(context: Context, keyId: String): DichotomousKeyEntity? =
        loadAll(context).firstOrNull { it.id == keyId }

    /** Útil para tests o si se actualizan los assets dinámicamente. */
    fun invalidateCache() { cache = null }

    // ── Parser ──────────────────────────────────────────────────────────

    private fun loadFromAssets(context: Context): List<DichotomousKeyEntity> {
        val text = context.assets.open(ASSET_FILE)
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
        val root = JSONObject(text)
        val keysArr = root.optJSONArray("keys") ?: return emptyList()
        val out = ArrayList<DichotomousKeyEntity>(keysArr.length())
        for (i in 0 until keysArr.length()) {
            parseKey(keysArr.optJSONObject(i))?.let { out += it }
        }
        return out
    }

    private fun parseKey(o: JSONObject?): DichotomousKeyEntity? {
        if (o == null) return null
        val id = o.optString("id").takeIf { it.isNotBlank() } ?: return null
        val nodes = parseNodes(o.optJSONArray("nodes"))
        val rootId = o.optString("rootNodeId").takeIf { it.isNotBlank() }
            ?: nodes.firstOrNull()?.id
            ?: return null
        return DichotomousKeyEntity(
            id = id,
            title = o.optString("title", id),
            subtitle = o.optString("subtitle", ""),
            scope = o.optString("scope", "general"),
            family = o.optString("family", ""),
            category = o.optString("category", ""),
            icon = o.optString("icon", "filter_alt"),
            rootNodeId = rootId,
            nodes = nodes
        )
    }

    private fun parseNodes(arr: JSONArray?): List<KeyNodeEntity> {
        if (arr == null) return emptyList()
        val out = ArrayList<KeyNodeEntity>(arr.length())
        for (i in 0 until arr.length()) {
            val n = arr.optJSONObject(i) ?: continue
            val id = n.optString("id").takeIf { it.isNotBlank() } ?: continue
            out += KeyNodeEntity(
                id = id,
                question = n.optString("question", ""),
                help = n.optString("help", ""),
                options = parseOptions(n.optJSONArray("options"))
            )
        }
        return out
    }

    private fun parseOptions(arr: JSONArray?): List<KeyOptionEntity> {
        if (arr == null) return emptyList()
        val out = ArrayList<KeyOptionEntity>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val results = mutableListOf<Int>()
            o.optJSONArray("resultPlantIds")?.let { ids ->
                for (j in 0 until ids.length()) {
                    val v = ids.optInt(j, 0)
                    if (v > 0) results += v
                }
            }
            out += KeyOptionEntity(
                label = o.optString("label", "Opción"),
                description = o.optString("description", ""),
                image = o.optString("image", "").takeIf { it.isNotBlank() },
                nextNodeId = o.optString("nextNodeId", "").takeIf { it.isNotBlank() },
                filter = parseFilter(o.optJSONObject("filter")),
                resultPlantIds = results,
                resultNote = o.optString("resultNote", ""),
                resetFilters = o.optBoolean("resetFilters", false)
            )
        }
        return out
    }

    private fun parseFilter(o: JSONObject?): KeyFilter {
        if (o == null) return KeyFilter()
        return KeyFilter(
            families = stringList(o.optJSONArray("families")),
            notFamilies = stringList(o.optJSONArray("notFamilies")),
            categories = stringList(o.optJSONArray("categories")),
            toxicityLevels = stringList(o.optJSONArray("toxicityLevels")),
            genera = stringList(o.optJSONArray("genera")),
            allKeywords = stringList(o.optJSONArray("allKeywords")),
            anyKeyword = stringList(o.optJSONArray("anyKeyword")),
            noneKeyword = stringList(o.optJSONArray("noneKeyword"))
        )
    }

    private fun stringList(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        val out = ArrayList<String>(arr.length())
        for (i in 0 until arr.length()) {
            val s = arr.optString(i, "")
            if (s.isNotBlank()) out += s
        }
        return out
    }

    // ── Fallback mínimo ─────────────────────────────────────────────────

    private fun fallbackKeys(): List<DichotomousKeyEntity> = listOf(
        DichotomousKeyEntity(
            id = "general",
            title = "Clave general (mínima)",
            subtitle = "Fallback de seguridad — el JSON de claves no se encontró",
            scope = "general",
            icon = "filter_alt",
            rootNodeId = "n_root",
            nodes = listOf(
                KeyNodeEntity(
                    id = "n_root",
                    question = "¿Qué porte tiene la planta?",
                    options = listOf(
                        KeyOptionEntity(
                            label = "Árbol o arbusto leñoso",
                            filter = KeyFilter(anyKeyword = listOf("árbol", "arbusto", "leñoso"))
                        ),
                        KeyOptionEntity(
                            label = "Hierba o planta blanda",
                            filter = KeyFilter(anyKeyword = listOf("hierba", "herbácea"))
                        )
                    )
                )
            )
        )
    )
}
