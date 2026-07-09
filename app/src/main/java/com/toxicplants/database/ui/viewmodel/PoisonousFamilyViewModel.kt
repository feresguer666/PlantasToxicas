package com.toxicplants.database.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.toxicplants.database.PlantDatabase
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.PoisonousFamilyCatalogType
import com.toxicplants.database.PoisonousFamilyGenusEntity
import com.toxicplants.database.PoisonousFamilySeedDataSource
import com.toxicplants.database.PoisonousFamilySummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PoisonousFamilyViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application
    private val dao = PlantDatabase.getDatabase(application).poisonousFamilyDao()
    private val prefs = application.getSharedPreferences("poisonous_families", Context.MODE_PRIVATE)

    private val _bulkCreationMessage = MutableLiveData<String?>(null)
    val bulkCreationMessage: LiveData<String?> = _bulkCreationMessage

    fun clearBulkCreationMessage() {
        _bulkCreationMessage.postValue(null)
    }

    val allGenera: LiveData<List<PoisonousFamilyGenusEntity>> = dao.getAll()

    private val _allFamilySummaries = MediatorLiveData<List<PoisonousFamilySummary>>().apply {
        value = emptyList()
        addSource(allGenera) { items -> value = buildFamilySummaries(items.orEmpty()) }
    }
    val familySummaries: LiveData<List<PoisonousFamilySummary>> = _allFamilySummaries

    val allPoisonousFamilySummaries: LiveData<List<PoisonousFamilySummary>> =
        dao.getByCatalogType(PoisonousFamilyCatalogType.ALL).map { buildFamilySummaries(it.orEmpty()) }

    val partialPoisonousFamilySummaries: LiveData<List<PoisonousFamilySummary>> =
        dao.getByCatalogType(PoisonousFamilyCatalogType.PARTIAL).map { buildFamilySummaries(it.orEmpty()) }

    init {
        seedInitialCatalogIfNeeded()
    }

    fun familySummariesFor(catalogType: String): LiveData<List<PoisonousFamilySummary>> =
        dao.getByCatalogType(catalogType).map { buildFamilySummaries(it.orEmpty()) }

    fun generaForFamily(familyName: String, catalogType: String): LiveData<List<PoisonousFamilyGenusEntity>> {
        return dao.getByFamily(familyName, catalogType)
    }

    /** Compatibilidad con rutas antiguas. */
    fun generaForFamily(familyName: String): LiveData<List<PoisonousFamilyGenusEntity>> {
        return dao.getByFamily(familyName)
    }

    fun genusById(id: Int): LiveData<PoisonousFamilyGenusEntity?> = dao.getById(id)

    fun saveGenus(item: PoisonousFamilyGenusEntity) {
        val cleaned = item.copy(
            familyName = item.familyName.trim(),
            genusName = item.genusName.trim(),
            genusSpeciesCount = item.genusSpeciesCount.coerceAtLeast(0),
            toxins = item.toxins.trim(),
            symptoms = item.symptoms.trim(),
            toxicParts = item.toxicParts.trim(),
            notes = item.notes.trim(),
            catalogType = item.catalogType.ifBlank { PoisonousFamilyCatalogType.ALL },
            updatedAt = System.currentTimeMillis()
        )
        if (cleaned.familyName.isBlank() || cleaned.genusName.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            if (cleaned.id == 0) dao.insert(cleaned) else dao.update(cleaned)
        }
    }

    fun deleteGenus(item: PoisonousFamilyGenusEntity) {
        viewModelScope.launch(Dispatchers.IO) { dao.delete(item) }
    }

    fun deleteFamily(familyName: String, catalogType: String) {
        viewModelScope.launch(Dispatchers.IO) { dao.deleteFamily(familyName.trim(), catalogType) }
    }

    fun deleteFamily(familyName: String) {
        viewModelScope.launch(Dispatchers.IO) { dao.deleteFamily(familyName.trim()) }
    }

    fun resetToSeedCatalog() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteAll()
            dao.insertAll(PoisonousFamilySeedDataSource.loadEverything(app))
            prefs.edit()
                .putBoolean(SEED_PREF_KEY, true)
                .putBoolean(OLD_SEED_PREF_KEY, true)
                .apply()
        }
    }

    private fun seedInitialCatalogIfNeeded() {
        viewModelScope.launch(Dispatchers.IO) {
            val alreadySeeded = prefs.getBoolean(SEED_PREF_KEY, false)
            val currentCount = dao.count()
            val needsPartial = dao.countByCatalogType(PoisonousFamilyCatalogType.PARTIAL) == 0

            if (alreadySeeded && !needsPartial) { upsertVerifiedOnlineGenera(); return@launch }

            if (currentCount == 0) {
                dao.insertAll(PoisonousFamilySeedDataSource.loadEverything(app))
            } else {
                if (dao.countByCatalogType(PoisonousFamilyCatalogType.ALL) == 0) {
                    dao.insertAll(PoisonousFamilySeedDataSource.loadAllCatalog(app))
                }
                if (needsPartial) {
                    dao.insertAll(PoisonousFamilySeedDataSource.loadPartialCatalog(app))
                }
            }

            prefs.edit()
                .putBoolean(SEED_PREF_KEY, true)
                .putBoolean(OLD_SEED_PREF_KEY, true)
                .apply()
            upsertVerifiedOnlineGenera()
        }
    }

    private suspend fun upsertVerifiedOnlineGenera() {
        val verified = PoisonousFamilySeedDataSource.loadVerifiedOnlineGenera(app)
        if (verified.isEmpty()) return

        val current = dao.getAllSync()
        val byKey = current.associateBy { item ->
            item.catalogType.lowercase() + "|" +
                    item.familyName.trim().lowercase() + "|" +
                    item.genusName.trim().lowercase()
        }

        for (seed in verified) {
            val key = seed.catalogType.lowercase() + "|" +
                    seed.familyName.trim().lowercase() + "|" +
                    seed.genusName.trim().lowercase()

            val existing = byKey[key]
            if (existing == null) {
                dao.insert(seed)
            } else if (!existing.notes.contains("VERIFICADO ONLINE", ignoreCase = true)) {
                val extraNote = seed.notes.trim()
                val mergedNotes = listOf(existing.notes.trim(), extraNote)
                    .filter { it.isNotBlank() }
                    .joinToString(" | ")

                dao.update(
                    existing.copy(
                        notes = mergedNotes,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    private fun genusFromScientificName(scientificName: String): String {
        return scientificName
            .trim()
            .split(Regex("\\s+"))
            .firstOrNull()
            ?.trim(' ', ',', '.', ';', ':', '(', ')', '[', ']')
            .orEmpty()
    }

    fun createMissingPlantSheetsFromGenera(catalogType: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val database = PlantDatabase.getDatabase(app)
            val plantDao = database.plantDao()
            val currentPlants = plantDao.getAllPlantsSync()

            val existingFamilyGenusKeys = currentPlants
                .map { plant ->
                    plant.family.trim().lowercase() + "|" + genusFromScientificName(plant.scientificName).lowercase()
                }
                .toHashSet()

            val genera = dao.getAllSync()
                .asSequence()
                .filter { item -> catalogType == null || item.catalogType == catalogType }
                .filter { item -> item.familyName.isNotBlank() && item.genusName.isNotBlank() }
                .distinctBy { item -> item.familyName.trim().lowercase() + "|" + item.genusName.trim().lowercase() }
                .filter { item ->
                    val key = item.familyName.trim().lowercase() + "|" + item.genusName.trim().lowercase()
                    key !in existingFamilyGenusKeys
                }
                .toList()

            if (genera.isEmpty()) {
                _bulkCreationMessage.postValue("No faltan fichas por crear en esta lista.")
                return@launch
            }

            val newPlants = genera.map { genus ->
                PlantEntity(
                    id = 0,
                    commonName = genus.genusName,
                    commonNames = "",
                    scientificName = "${genus.genusName} sp.",
                    family = genus.familyName,
                    toxicityLevel = if (genus.catalogType == PoisonousFamilyCatalogType.ALL) "Alto" else "Moderado",
                    toxicParts = genus.toxicParts,
                    symptoms = genus.symptoms,
                    description = listOf(
                        "Ficha creada automáticamente desde Familias venenosas.",
                        "Género: ${genus.genusName}",
                        "Familia: ${genus.familyName}",
                        "Toxinas/principios: ${genus.toxins}",
                        "Notas: ${genus.notes}"
                    ).filter { it.isNotBlank() }.joinToString("\n\n"),
                    habitat = "",
                    geographicDistribution = "",
                    firstAid = "",
                    imageUrl = "",
                    isFavorite = false,
                    category = "Familias venenosas",
                    latitude = null,
                    longitude = null,
                    locationName = null,
                    foundDate = null,
                    notes = genus.notes,
                    floweringMonths = "",
                    fruitingMonths = "",
                    maxToxicityMonths = "",
                    mythsAndLegends = ""
                )
            }

            plantDao.insertAll(newPlants)
            _bulkCreationMessage.postValue("Creadas ${newPlants.size} fichas nuevas de géneros faltantes.")
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
        const val SEED_PREF_KEY = "seeded_two_catalogs_v3"
    }
}
