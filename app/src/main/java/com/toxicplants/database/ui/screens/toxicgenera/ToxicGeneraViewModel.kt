package com.toxicplants.database.ui.screens.toxicgenera

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.toxicplants.database.PlantDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class GenusWithCount(val genus: ToxicGenus, val fichasLocal: Int, val isUserEdited: Boolean = false)

class ToxicGeneraViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = PlantDatabase.getDatabase(application).plantDao()
    private val userStore = ToxicGeneraUserStore(application)
    private val _genera = MutableStateFlow<List<GenusWithCount>>(emptyList())
    val genera: StateFlow<List<GenusWithCount>> = _genera
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query
    fun setQuery(q: String) { _query.value = q }
    private fun extractGenus(sci: String): String = sci.trim().split(Regex("[\\s_]+")).firstOrNull() ?: ""
    init { load() }
    private fun load() = viewModelScope.launch {
        combine(dao.getAllPlantsFlow(), userStore.state) { plants, _ -> plants }.collect { plants ->
            val countByGenus = plants.groupBy { extractGenus(it.scientificName) }
                .mapKeys { it.key.lowercase() }.mapValues { it.value.size }
            val catalog = userStore.getMerged()
            val userGenusSet = (userStore.state.value.custom.map { it.genus.lowercase() } +
                    userStore.state.value.overrides.map { it.genus.lowercase() }).toSet()
            _genera.value = catalog.map { g ->
                GenusWithCount(g, countByGenus[g.genus.lowercase()] ?: 0, userGenusSet.contains(g.genus.lowercase()))
            }
        }
    }
    fun saveGenus(originalGenus: String?, genus: String, family: String, commonNameEs: String, speciesCount: Int, toxicityNote: String, gbifKey: Long?) {
        val item = ToxicGenusUser(genus.trim().replaceFirstChar { it.uppercase() }, family.trim(), commonNameEs.trim(), speciesCount, toxicityNote.trim(), gbifKey)
        if (originalGenus != null && !originalGenus.equals(genus, true)) { userStore.delete(originalGenus) }
        userStore.upsert(item)
    }
    fun deleteGenus(genus: String) { userStore.delete(genus) }
}
