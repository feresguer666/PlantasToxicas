package com.toxicplants.database.ui.screens.toxicgenera

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class ToxicGenusUser(
    val genus: String,
    val family: String = "",
    val commonNameEs: String = "",
    val speciesCount: Int = 0,
    val toxicityNote: String = "",
    val gbifGenusKey: Long? = null
) {
    fun toCatalog() =
        ToxicGenus(genus, family, commonNameEs, speciesCount, toxicityNote, gbifGenusKey)
}

@Serializable
data class ToxicGeneraStore(
    val custom: List<ToxicGenusUser> = emptyList(),
    val overrides: List<ToxicGenusUser> = emptyList(),
    val hidden: List<String> = emptyList()
)

class ToxicGeneraUserStore(context: Context) {
    private val file = File(context.filesDir, "toxic_genera_user.json")
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    private val _state = MutableStateFlow(load())
    val state: StateFlow<ToxicGeneraStore> get() = _state

    private fun load(): ToxicGeneraStore = try {
        if (file.exists()) json.decodeFromString<ToxicGeneraStore>(file.readText()) else ToxicGeneraStore()
    } catch (_: Exception) {
        ToxicGeneraStore()
    }

    private fun save(s: ToxicGeneraStore) {
        _state.value = s
        try {
            file.writeText(json.encodeToString(s))
        } catch (_: Exception) {
        }
    }

    fun upsert(item: ToxicGenusUser) {
        val cur = _state.value
        val isBase = ToxicGeneraCatalog.all.any { it.genus.equals(item.genus, true) }
        if (isBase) {
            val overrides = cur.overrides.filterNot { it.genus.equals(item.genus, true) } + item
            save(
                cur.copy(
                    overrides = overrides,
                    hidden = cur.hidden.filterNot { it.equals(item.genus, true) })
            )
        } else {
            val custom = cur.custom.filterNot { it.genus.equals(item.genus, true) } + item
            save(
                cur.copy(
                    custom = custom,
                    hidden = cur.hidden.filterNot { it.equals(item.genus, true) })
            )
        }
    }

    fun delete(genus: String) {
        val cur = _state.value
        val isBase = ToxicGeneraCatalog.all.any { it.genus.equals(genus, true) }
        if (isBase) {
            save(
                cur.copy(
                hidden = (cur.hidden + genus).distinct(),
                overrides = cur.overrides.filterNot { it.genus.equals(genus, true) }
            ))
        } else {
            save(cur.copy(custom = cur.custom.filterNot { it.genus.equals(genus, true) }))
        }
    }

    fun getMerged(): List<ToxicGenus> {
        val s = _state.value
        val overrideMap = s.overrides.associateBy { it.genus.lowercase() }
        val hiddenSet = s.hidden.map { it.lowercase() }.toSet()
        val base = ToxicGeneraCatalog.all
            .filterNot { hiddenSet.contains(it.genus.lowercase()) }
            .map { b -> overrideMap[b.genus.lowercase()]?.toCatalog() ?: b }
        val custom = s.custom.map { it.toCatalog() }
            .filterNot { c -> base.any { it.genus.equals(c.genus, true) } }
        return (base + custom).distinctBy { it.genus.lowercase() }.sortedBy { it.genus.lowercase() }
    }
}