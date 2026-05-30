package com.toxicplants.database.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.toxicplants.database.PlantDatabase
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.PlantDataSource
import com.toxicplants.database.CompoundDataSource
import com.toxicplants.database.data.repository.PlantRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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
            // Siembra inicial de datos (solo la primera vez que se crea la BD)
            if (repository.getPlantCount() == 0) {
                repository.insertAll(PlantDataSource.loadAll(application))
            }
            if (compoundDao.count() == 0) {
                compoundDao.insertAll(CompoundDataSource.loadAll(application))
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

    fun setCategory(category: String) {
        selectedCategory.value = category
    }

    // ❌ Antes:
    fun getPlantById(id: Int): LiveData<PlantEntity> = repository.getPlantByIdLiveData(id)

// ✅ Después - getPlantByIdLiveData devuelve LiveData<PlantEntity> (sin ?)
// Muéstrame el repositorio para confirmarlo:

    fun searchPlants(query: String): LiveData<List<PlantEntity>> = repository.searchPlants(query)

    fun getPlantsByToxicity(level: String): LiveData<List<PlantEntity>> = repository.getPlantsByToxicity(level)

    fun getPlantsByCategory(category: String): LiveData<List<PlantEntity>> = repository.getPlantsByCategory(category)

    fun getPlantsByFamily(family: String): LiveData<List<PlantEntity>> = repository.getPlantsByFamily(family)

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

    fun deletePlant(plant: PlantEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(plant)
        }
    }

    fun getAllPlantsForDownload(): List<PlantEntity> {
        return plants.value
    }

    // ✅ FUNCIÓN PARA ACTUALIZAR UBICACIÓN
    fun updatePlantLocation(plantId: Int, latitude: Double?, longitude: Double?, locationName: String?, notes: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateLocation(plantId, latitude, longitude, locationName, notes)
        }
    }

    // ✅ OBTENER PLANTAS CON UBICACIÓN
    fun getPlantsWithLocation(): LiveData<List<PlantEntity>> = repository.getPlantsWithLocation()
}