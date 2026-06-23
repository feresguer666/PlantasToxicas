package com.toxicplants.database.ui.screens.families

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.toxicplants.database.PlantDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class FamilyWithCount(
    val family: ToxicFamily,
    val fichasLocal: Int,
    val isUserEdited: Boolean = false
)

class FamilyViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = PlantDatabase.getDatabase(application).plantDao()
    private val userStore = FamilyUserStore(application)

    private val _families = MutableStateFlow<List<FamilyWithCount>>(emptyList())
    val families: StateFlow<List<FamilyWithCount>> = _families

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query
    fun setQuery(q: String) {
        _query.value = q
    }

    init {
        load()
    }

    private fun load() = viewModelScope.launch {
        combine(dao.getAllPlantsFlow(), userStore.state) { plants, _ -> plants }
            .collect { plants ->
                val countByFamily = plants.groupBy { it.family.trim() }
                    .mapKeys { it.key.lowercase() }
                    .mapValues { it.value.size }
                val catalog = userStore.getMerged()
                val userSet = (userStore.state.value.custom.map { it.family.lowercase() } +
                        userStore.state.value.overrides.map { it.family.lowercase() }).toSet()
                _families.value = catalog.map { f ->
                    FamilyWithCount(
                        f, countByFamily[f.family.lowercase()] ?: 0,
                        userSet.contains(f.family.lowercase())
                    )
                }
            }
    }

    fun save(family: ToxicFamily, originalName: String? = null) {
        if (originalName != null && !originalName.equals(family.family, true)) {
            userStore.delete(originalName)
        }
        userStore.upsert(
            FamilyUser(
                family.family, family.commonNameEs, family.generaCount, family.speciesCount,
                family.distribution, family.description, family.toxicityScope, family.notes
            )
        )
    }

    fun delete(family: String) = userStore.delete(family)
}