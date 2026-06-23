package com.toxicplants.database.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import com.toxicplants.database.PlantDatabase
import com.toxicplants.database.PoisonousFamilyGenusEntity
import com.toxicplants.database.PoisonousFamilySeedDataSource
import com.toxicplants.database.PoisonousFamilySummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PoisonousFamilyViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application
    private val dao = PlantDatabase.getDatabase(application).poisonousFamilyDao()
    private val prefs = application.getSharedPreferences("poisonous_families", Context.MODE_PRIVATE)

    val allGenera: LiveData<List<PoisonousFamilyGenusEntity>> = dao.getAll()

    private val _familySummaries = MediatorLiveData<List<PoisonousFamilySummary>>().apply {
        value = emptyList()
        addSource(allGenera) { items -> value = buildFamilySummaries(items.orEmpty()) }
    }
    val familySummaries: LiveData<List<PoisonousFamilySummary>> = _familySummaries

    init {
        seedInitialCatalogIfNeeded()
    }

    fun generaForFamily(familyName: String): LiveData<List<PoisonousFamilyGenusEntity>> {
        return dao.getByFamily(familyName)
    }

    fun genusById(id: Int): LiveData<PoisonousFamilyGenusEntity?> {
        return dao.getById(id)
    }

    fun saveGenus(item: PoisonousFamilyGenusEntity) {
        val cleaned = item.copy(
            familyName = item.familyName.trim(),
            genusName = item.genusName.trim(),
            genusSpeciesCount = item.genusSpeciesCount.coerceAtLeast(0),
            toxins = item.toxins.trim(),
            symptoms = item.symptoms.trim(),
            toxicParts = item.toxicParts.trim(),
            notes = item.notes.trim(),
            updatedAt = System.currentTimeMillis()
        )
        if (cleaned.familyName.isBlank() || cleaned.genusName.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            if (cleaned.id == 0) dao.insert(cleaned) else dao.update(cleaned)
        }
    }

    fun deleteGenus(item: PoisonousFamilyGenusEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.delete(item)
        }
    }

    fun deleteFamily(familyName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteFamily(familyName.trim())
        }
    }

    fun resetToSeedCatalog() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteAll()
            dao.insertAll(PoisonousFamilySeedDataSource.loadAll(app))
            prefs.edit()
                .putBoolean(SEED_PREF_KEY, true)
                .putBoolean(OLD_SEED_PREF_KEY, true)
                .apply()
        }
    }

    private fun seedInitialCatalogIfNeeded() {
        viewModelScope.launch(Dispatchers.IO) {
            if (prefs.getBoolean(SEED_PREF_KEY, false)) return@launch

            val currentCount = dao.count()
            val shouldReplaceOldSmallSeed = prefs.getBoolean(OLD_SEED_PREF_KEY, false) && currentCount in 1..500

            if (currentCount > 0 && !shouldReplaceOldSmallSeed) {
                // Si el usuario ya tiene datos propios, no los pisamos.
                prefs.edit().putBoolean(SEED_PREF_KEY, true).apply()
                return@launch
            }

            val seed = PoisonousFamilySeedDataSource.loadAll(app)
            if (seed.isNotEmpty()) {
                if (shouldReplaceOldSmallSeed) dao.deleteAll()
                dao.insertAll(seed)
                prefs.edit()
                    .putBoolean(SEED_PREF_KEY, true)
                    .putBoolean(OLD_SEED_PREF_KEY, true)
                    .apply()
            }
        }
    }

    private fun buildFamilySummaries(items: List<PoisonousFamilyGenusEntity>): List<PoisonousFamilySummary> {
        return items
            .groupBy { it.familyName.trim() }
            .filterKeys { it.isNotBlank() }
            .map { (family, genera) ->
                PoisonousFamilySummary(
                    familyName = family,
                    generaCount = genera
                        .map { it.genusName.trim().lowercase() }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .size,
                    speciesCount = genera.sumOf { it.genusSpeciesCount.coerceAtLeast(0) }
                )
            }
            .sortedBy { it.familyName.lowercase() }
    }

    private companion object {
        const val OLD_SEED_PREF_KEY = "seeded_v1"
        const val SEED_PREF_KEY = "seeded_gbif_v2"
    }
}
