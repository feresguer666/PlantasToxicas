package com.toxicplants.database.ui.screens.families

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class FamilyUser(
    val family: String,
    val commonNameEs: String = "",
    val generaCount: Int = 0,
    val speciesCount: Int = 0,
    val distribution: String = "",
    val description: String = "",
    val toxicityScope: FamilyToxicityScope = FamilyToxicityScope.SOME_SPECIES,
    val notes: String = ""
) {
    fun toCatalog() = ToxicFamily(
        family,
        commonNameEs,
        generaCount,
        speciesCount,
        distribution,
        description,
        toxicityScope,
        notes
    )
}

@Serializable
data class FamilyStore(
    val custom: List<FamilyUser> = emptyList(),
    val overrides: List<FamilyUser> = emptyList(),
    val hidden: List<String> = emptyList()
)

class FamilyUserStore(context: Context) {
    private val appContext = context.applicationContext
    private val file = File(appContext.filesDir, "toxic_families_user.json")
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    private val _state = MutableStateFlow(load())
    val state: StateFlow<FamilyStore> get() = _state

    private fun load(): FamilyStore = try {
        if (file.exists()) json.decodeFromString<FamilyStore>(file.readText()) else FamilyStore()
    } catch (e: Exception) {
        Log.e("FamilyStore", "load", e); FamilyStore()
    }

    private fun save(s: FamilyStore) {
        _state.value = s
        try {
            file.writeText(json.encodeToString(s))
        } catch (e: Exception) {
            Log.e("FamilyStore", "save", e)
        }
    }

    fun upsert(item: FamilyUser) {
        val cur = _state.value
        val isBase = FamilyCatalog.all.any { it.family.equals(item.family, true) }
        if (isBase) {
            val overrides = cur.overrides.filterNot { it.family.equals(item.family, true) } + item
            save(
                cur.copy(
                    overrides = overrides,
                    hidden = cur.hidden.filterNot { it.equals(item.family, true) })
            )
        } else {
            val custom = cur.custom.filterNot { it.family.equals(item.family, true) } + item
            save(
                cur.copy(
                    custom = custom,
                    hidden = cur.hidden.filterNot { it.equals(item.family, true) })
            )
        }
    }

    fun delete(family: String) {
        val cur = _state.value
        val isBase = FamilyCatalog.all.any { it.family.equals(family, true) }
        if (isBase) {
            save(
                cur.copy(
                    hidden = (cur.hidden + family).distinct(),
                    overrides = cur.overrides.filterNot { it.family.equals(family, true) })
            )
        } else {
            save(cur.copy(custom = cur.custom.filterNot { it.family.equals(family, true) }))
        }
    }

    fun getMerged(): List<ToxicFamily> {
        val s = _state.value
        val overrideMap = s.overrides.associateBy { it.family.lowercase() }
        val hidden = s.hidden.map { it.lowercase() }.toSet()
        val base = FamilyCatalog.all.filterNot { hidden.contains(it.family.lowercase()) }
            .map { overrideMap[it.family.lowercase()]?.toCatalog() ?: it }
        val custom = s.custom.map { it.toCatalog() }
            .filterNot { c -> base.any { it.family.equals(c.family, true) } }
        return (base + custom).distinctBy { it.family.lowercase() }
            .sortedBy { it.family.lowercase() }
    }

    // para BackupRepository
    fun exportRawJson(): String = try {
        if (file.exists()) file.readText() else """{"custom":[],"overrides":[],"hidden":[]}"""
    } catch (_: Exception) {
        """{"custom":[],"overrides":[],"hidden":[]}"""
    }

    fun importRawJson(jsonStr: String) {
        try {
            val parsed = json.decodeFromString<FamilyStore>(jsonStr)
            save(parsed)
        } catch (e: Exception) {
            Log.e("FamilyStore", "import", e)
        }
    }
    
}