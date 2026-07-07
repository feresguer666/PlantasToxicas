package com.toxicplants.database.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.toxicplants.database.CompoundDataSource
import com.toxicplants.database.PlantDataSource
import com.toxicplants.database.PlantDatabase
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.data.repository.PlantRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlantViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PlantRepository
    val allPlants: LiveData<List<PlantEntity>>
    val favoritePlants: LiveData<List<PlantEntity>>
    val allCategories: LiveData<List<String>>
    val allFamilies: LiveData<List<String>>

    private val isLoading = MutableLiveData<Boolean>()
    val isLoadingData: LiveData<Boolean> = isLoading

    private val currentFilter = MutableLiveData<String>("Todas")
    val currentFilterData: LiveData<String> = currentFilter

    private val selectedPlant = MutableStateFlow<PlantEntity?>(null)
    val selectedPlantData: StateFlow<PlantEntity?> = selectedPlant

    private val detailNavigationPlants = MutableStateFlow<List<PlantEntity>>(emptyList())
    val detailNavigationPlantsData: StateFlow<List<PlantEntity>> = detailNavigationPlants

    private val selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategoryData: StateFlow<String?> = selectedCategory

    private val mortalPlants = MutableStateFlow<List<PlantEntity>>(emptyList())
    val mortalPlantsData: StateFlow<List<PlantEntity>> = mortalPlants

    private val searchQuery = MutableStateFlow("")
    val searchQueryData: StateFlow<String> = searchQuery

    private val toxicityFilter = MutableStateFlow<String?>(null)
    val toxicityFilterData: StateFlow<String?> = toxicityFilter

    private val plants = MutableStateFlow<List<PlantEntity>>(emptyList())
    val plantsData: StateFlow<List<PlantEntity>> = plants

    init {
        val db = PlantDatabase.getDatabase(application)
        val plantDao = db.plantDao()
        val compoundDao = db.compoundDao()
        repository = PlantRepository(plantDao)
        allPlants = repository.allPlants
        favoritePlants = repository.favoritePlants
        allCategories = repository.allCategories
        allFamilies = repository.allFamilies

        viewModelScope.launch(Dispatchers.IO) {
            isLoading.postValue(true)
            // Siembra inicial y actualización incremental de datos locales.
            // Si el usuario ya tenía la app instalada, añadimos las plantas nuevas del JSON
            // sin sobrescribir las existentes ni sus favoritos/ubicaciones/notas.
            val seedPlants = PlantDataSource.loadAll(application)
            if (repository.getPlantCount() == 0) {
                repository.insertAll(seedPlants)
            } else {
                val existingList = repository.getAllPlantsSync()
                val existingIds = existingList.map { it.id }.toHashSet()

                val missingPlants = seedPlants.filter { it.id != 0 && it.id !in existingIds }
                if (missingPlants.isNotEmpty()) {
                    repository.insertAll(missingPlants)
                }

                // ✅ FORZAR ACTUALIZACIÓN DE MITOS Y LEYENDAS (Smart Merge)
                val seedMap = seedPlants.associateBy { it.id }
                var needsUpdate = false

                val updatedList = existingList.map { p ->
                    val seed = seedMap[p.id]
                    if (seed != null && p.mythsAndLegends != seed.mythsAndLegends) {
                        needsUpdate = true
                        p.copy(mythsAndLegends = seed.mythsAndLegends)
                    } else {
                        p
                    }
                }

                if (needsUpdate) {
                    repository.insertAll(updatedList) // Insert(REPLACE) sobrescribe solo el campo que cambia
                }
            }
            val seedCompounds = CompoundDataSource.loadAll(application)
            if (compoundDao.count() == 0) {
                compoundDao.insertAll(seedCompounds)
            } else {
                // Añade compuestos nuevos del JSON sin tocar los existentes.
                val existingCompoundIds = compoundDao.getAllSync().map { it.id }.toHashSet()
                val missingCompounds =
                    seedCompounds.filter { it.id != 0 && it.id !in existingCompoundIds }
                if (missingCompounds.isNotEmpty()) {
                    compoundDao.insertAll(missingCompounds)
                }
                // Actualiza los pubchemCid desde el JSON para compuestos que aún tengan CID = 0
                for (c in seedCompounds) {
                    if (c.pubchemCid != 0) {
                        compoundDao.updatePubchemCid(c.id, c.pubchemCid)
                    }
                }
            }
            mortalPlants.value = repository.getPlantsByToxicitySync("Mortal")
            plants.value = repository.getAllPlantsSync()
            isLoading.postValue(false)
        }
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
        filterPlants()
    }

    fun setToxicityFilter(toxicity: String?) {
        toxicityFilter.value = toxicity
        filterPlants()
    }

    private fun filterPlants() {
        viewModelScope.launch(Dispatchers.IO) {
            val allPlantsList = repository.getAllPlantsSync()
            val query = searchQuery.value
            val toxicity = toxicityFilter.value

            plants.value = allPlantsList.filter { plant ->
                val matchesQuery = query.isEmpty() ||
                        plant.commonName.contains(query, ignoreCase = true) ||
                        plant.scientificName.contains(query, ignoreCase = true) ||
                        plant.category.contains(query, ignoreCase = true)

                val matchesToxicity = toxicity == null || plant.toxicityLevel == toxicity

                matchesQuery && matchesToxicity
            }
        }
    }

    fun selectPlant(plant: PlantEntity) {
        selectedPlant.value = plant
    }

    fun setDetailNavigationPlants(plants: List<PlantEntity>) {
        detailNavigationPlants.value = plants
    }

    fun clearDetailNavigationPlants() {
        detailNavigationPlants.value = emptyList()
    }

    suspend fun getDeletedSeedPlants(): List<PlantEntity> = withContext(Dispatchers.IO) {
        val app = getApplication<Application>()
        val seedPlants = PlantDataSource.loadAll(app)
        val existingIds = repository.getAllPlantsSync()
            .map { it.id }
            .toHashSet()

        seedPlants
            .filter { it.id != 0 && it.id !in existingIds }
            .sortedBy { it.commonName.lowercase() }
    }

    fun restoreDeletedPlant(plantId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            val seedPlant = PlantDataSource.loadAll(app)
                .firstOrNull { it.id == plantId }

            if (seedPlant != null) {
                repository.insert(seedPlant)
                plants.value = repository.getAllPlantsSync()
            }
        }
    }

    fun restoreAllDeletedPlants() {
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            val seedPlants = PlantDataSource.loadAll(app)
            val existingIds = repository.getAllPlantsSync()
                .map { it.id }
                .toHashSet()

            val deletedPlants = seedPlants
                .filter { it.id != 0 && it.id !in existingIds }

            if (deletedPlants.isNotEmpty()) {
                repository.insertAll(deletedPlants)
                plants.value = repository.getAllPlantsSync()
            }
        }
    }

    fun updatePlantNotes(plantId: Int, notes: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val plant = repository.getPlantById(plantId)
            if (plant != null) {
                repository.insert(plant.copy(notes = notes))
                plants.value = repository.getAllPlantsSync()
            }
        }
    }

    fun getOrnamentalDangerPlantsCached(): List<PlantEntity> {
        val source = plants.value.ifEmpty {
            allPlants.value ?: emptyList()
        }

        return source
            .filter { plant ->
                val text = listOf(
                    plant.commonName,
                    plant.commonNames,
                    plant.scientificName,
                    plant.category,
                    plant.description,
                    plant.habitat,
                    plant.geographicDistribution
                ).joinToString(" ").lowercase()

                val isOrnamental =
                    text.contains("ornamental") ||
                            text.contains("jardín") ||
                            text.contains("jardin") ||
                            text.contains("cultivada") ||
                            text.contains("cultivado") ||
                            text.contains("maceta") ||
                            text.contains("decorativa") ||
                            text.contains("seto")

                val isDangerous =
                    plant.toxicityLevel.equals("Mortal", ignoreCase = true) ||
                            plant.toxicityLevel.equals("Muy alto", ignoreCase = true) ||
                            plant.toxicityLevel.equals("Alto", ignoreCase = true)

                isOrnamental && isDangerous
            }
            .sortedWith(
                compareByDescending<PlantEntity> {
                    when (it.toxicityLevel) {
                        "Mortal" -> 5
                        "Muy alto" -> 4
                        "Alto" -> 3
                        "Moderado" -> 2
                        "Bajo" -> 1
                        else -> 0
                    }
                }.thenBy { it.commonName.lowercase() }
            )
    }

    fun setCategory(category: String) {
        selectedCategory.value = category
    }

    // ❌ Antes:
    fun getPlantById(id: Int): LiveData<PlantEntity> = repository.getPlantByIdLiveData(id)

// ✅ Después - getPlantByIdLiveData devuelve LiveData<PlantEntity> (sin ?)
// Muéstrame el repositorio para confirmarlo:

    fun searchPlants(query: String): LiveData<List<PlantEntity>> = repository.searchPlants(query)

    fun getPlantsByToxicity(level: String): LiveData<List<PlantEntity>> =
        repository.getPlantsByToxicity(level)

    fun getPlantsByCategory(category: String): LiveData<List<PlantEntity>> =
        repository.getPlantsByCategory(category)

    fun getPlantsByFamily(family: String): LiveData<List<PlantEntity>> =
        repository.getPlantsByFamily(family)

    fun toggleFavorite(plantId: Int, currentStatus: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleFavorite(plantId, !currentStatus)
        }
    }

    fun setFilter(filter: String) {
        currentFilter.value = filter
    }

    fun insertPlant(plant: PlantEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insert(plant)
        }
    }

    /**
     * Fuerza la siembra/actualización del catálogo local desde assets.
     * Útil si una instalación previa dejó la base Room sin plantas.
     */
    fun ensureCatalogSeeded() {
        viewModelScope.launch(Dispatchers.IO) {
            isLoading.postValue(true)
            val app = getApplication<Application>()

            val seedPlants = PlantDataSource.loadAll(app)
            val existingPlants = repository.getAllPlantsSync()
            if (existingPlants.isEmpty()) {
                repository.insertAll(seedPlants)
            } else {
                val existingIds = existingPlants.map { it.id }.toHashSet()
                val missingPlants = seedPlants.filter { it.id != 0 && it.id !in existingIds }
                if (missingPlants.isNotEmpty()) {
                    repository.insertAll(missingPlants)
                }
            }

            val compoundDao = PlantDatabase.getDatabase(app).compoundDao()
            val seedCompounds = CompoundDataSource.loadAll(app)
            val existingCompounds = compoundDao.getAllSync()
            if (existingCompounds.isEmpty()) {
                compoundDao.insertAll(seedCompounds)
            } else {
                val existingCompoundIds = existingCompounds.map { it.id }.toHashSet()
                val missingCompounds =
                    seedCompounds.filter { it.id != 0 && it.id !in existingCompoundIds }
                if (missingCompounds.isNotEmpty()) {
                    compoundDao.insertAll(missingCompounds)
                }
            }

            plants.value = repository.getAllPlantsSync()
            isLoading.postValue(false)
        }
    }

    // ✅ VERSIÓN SUSPEND PARA ESPERAR LA ESCRITURA
    suspend fun insertPlantSync(plant: PlantEntity) {
        withContext(Dispatchers.IO) {
            repository.insert(plant)
        }
    }

    fun deletePlant(plant: PlantEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(plant)
        }
    }

    fun deletePlants(plantsToDelete: List<PlantEntity>) {
        if (plantsToDelete.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            plantsToDelete.forEach { repository.delete(it) }
            plants.value = repository.getAllPlantsSync()
        }
    }

    fun bulkUpdatePlants(
        plantsToUpdate: List<PlantEntity>,
        field: String,
        value: String,
        append: Boolean = false
    ) {
        if (plantsToUpdate.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            fun merged(oldValue: String): String = when {
                !append -> value
                oldValue.isBlank() -> value
                value.isBlank() -> oldValue
                oldValue.split(';', ',').map { it.trim() }
                    .any { it.equals(value.trim(), ignoreCase = true) } -> oldValue

                else -> "$oldValue; $value"
            }

            val updatedPlants = plantsToUpdate.map { plant ->
                when (field) {
                    "commonNames" -> plant.copy(commonNames = merged(plant.commonNames))
                    "family" -> plant.copy(family = merged(plant.family))
                    "toxicityLevel" -> plant.copy(toxicityLevel = merged(plant.toxicityLevel))
                    "toxicParts" -> plant.copy(toxicParts = merged(plant.toxicParts))
                    "symptoms" -> plant.copy(symptoms = merged(plant.symptoms))
                    "description" -> plant.copy(description = merged(plant.description))
                    "category" -> plant.copy(category = merged(plant.category))
                    "habitat" -> plant.copy(habitat = merged(plant.habitat))
                    "geographicDistribution" -> plant.copy(geographicDistribution = merged(plant.geographicDistribution))
                    "firstAid" -> plant.copy(firstAid = merged(plant.firstAid))
                    "floweringMonths" -> plant.copy(floweringMonths = merged(plant.floweringMonths))
                    "fruitingMonths" -> plant.copy(fruitingMonths = merged(plant.fruitingMonths))
                    "maxToxicityMonths" -> plant.copy(maxToxicityMonths = merged(plant.maxToxicityMonths))
                    "notes" -> plant.copy(notes = merged(plant.notes.orEmpty()))
                    "mythsAndLegends" -> plant.copy(mythsAndLegends = merged(plant.mythsAndLegends))
                    else -> plant
                }
            }
            repository.insertAll(updatedPlants)
            plants.value = repository.getAllPlantsSync()
        }
    }

    suspend fun getAllPlantsForDownload(): List<PlantEntity> = withContext(Dispatchers.IO) {
        repository.getAllPlantsSync()
    }

    // ✅ FUNCIÓN PARA ACTUALIZAR UBICACIÓN
    fun updatePlantLocation(
        plantId: Int,
        latitude: Double?,
        longitude: Double?,
        locationName: String?,
        notes: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateLocation(plantId, latitude, longitude, locationName, notes)
        }
    }

    // ✅ FORZAR REGENERACIÓN DE IMAGEN IA
    suspend fun forceAiImageGeneration(
        plantId: Int,
        context: android.content.Context,
        plant: PlantEntity
    ) {
        // 1. Descargamos la imagen de la IA y la guardamos en disco
        val success = com.toxicplants.database.ui.ImageDownloader.forceAiImage(context, plant)

        if (success) {
            // 2. ACTUALIZAMOS LA BASE DE DATOS y ESPERAMOS (sync)
            val localPath = "file://${
                com.toxicplants.database.ui.LocalImageCache.getLocalImagePath(
                    context,
                    plantId
                )
            }"
            insertPlantSync(plant.copy(imageUrl = localPath))
        }
    }

    // ✅ OBTENER PLANTAS CON UBICACIÓN
    fun getPlantsWithLocation(): LiveData<List<PlantEntity>> = repository.getPlantsWithLocation()

    // ✅ SEMBRAR DATOS FENOLÓGICOS (se llama desde PlantDetailScreen y ToxicCalendarScreen)
    private var phenologySeeded = false
    fun seedPhenologyIfNeeded() {
        if (phenologySeeded) return
        phenologySeeded = true
        viewModelScope.launch(Dispatchers.IO) {
            val all = repository.getAllPlantsSync()
            val needsSeed = all.any {
                it.floweringMonths.isBlank() && it.toxicityLevel in listOf(
                    "Mortal",
                    "Muy alto",
                    "Alto"
                )
            }
            if (!needsSeed) return@launch

            val phenologyMap =
                ToxicCalendarViewModel.getPhenologySeedData()
            var updated = 0
            for (plant in all) {
                if (plant.floweringMonths.isNotBlank()) continue
                val norm =
                    plant.scientificName.trim().lowercase().split(Regex("\\s+")).let { parts ->
                        if (parts.size >= 2) "${parts[0]} ${parts[1]}" else plant.scientificName.trim()
                            .lowercase()
                    }
                val entry =
                    phenologyMap[norm] ?: phenologyMap[plant.scientificName.lowercase().trim()]
                if (entry != null) {
                    repository.insert(
                        plant.copy(
                            floweringMonths = entry.flowering,
                            fruitingMonths = entry.fruiting,
                            maxToxicityMonths = entry.maxToxicity
                        )
                    )
                    updated++
                }
            }
            if (updated > 0) {
                plants.value = repository.getAllPlantsSync()
            }
        }
    }
}