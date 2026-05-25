package com.toxicplants.database.data.repository

import androidx.lifecycle.LiveData
import com.toxicplants.database.PlantDao
import com.toxicplants.database.PlantEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlantRepository(private val plantDao: PlantDao) {

    val allPlants: LiveData<List<PlantEntity>> = plantDao.getAllPlants()
    val favoritePlants: LiveData<List<PlantEntity>> = plantDao.getFavoritePlants()
    val allCategories: LiveData<List<String>> = plantDao.getAllCategories()
    val allFamilies: LiveData<List<String>> = plantDao.getAllFamilies()

    suspend fun insert(plant: PlantEntity) {
        plantDao.insert(plant)
    }

    suspend fun insertAll(plants: List<PlantEntity>) {
        plantDao.insertAll(plants)
    }

    suspend fun update(plant: PlantEntity) {
        plantDao.update(plant)
    }

    suspend fun delete(plant: PlantEntity) {
        plantDao.delete(plant)
    }

    suspend fun getPlantById(id: Int): PlantEntity? {
        return withContext(Dispatchers.IO) {
            plantDao.getPlantById(id)
        }
    }

    fun getPlantByIdLiveData(id: Int): LiveData<PlantEntity> {
        return plantDao.getPlantByIdLiveData(id)
    }

    fun searchPlants(query: String): LiveData<List<PlantEntity>> {
        return plantDao.searchPlants("%$query%")
    }

    fun getPlantsByToxicity(level: String): LiveData<List<PlantEntity>> {
        return plantDao.getPlantsByToxicity(level)
    }

    fun getPlantsByCategory(category: String): LiveData<List<PlantEntity>> {
        return plantDao.getPlantsByCategory(category)
    }

    fun getPlantsByFamily(family: String): LiveData<List<PlantEntity>> {
        return plantDao.getPlantsByFamily(family)
    }

    suspend fun getPlantCount(): Int {
        return withContext(Dispatchers.IO) {
            plantDao.getPlantCount()
        }
    }

    suspend fun getAllPlantsSync(): List<PlantEntity> {
        return withContext(Dispatchers.IO) {
            plantDao.getAllPlantsSync()
        }
    }

    suspend fun getPlantsByToxicitySync(level: String): List<PlantEntity> {
        return withContext(Dispatchers.IO) {
            plantDao.getPlantsByToxicitySync(level)
        }
    }

    suspend fun toggleFavorite(plantId: Int, isFavorite: Boolean) {
        withContext(Dispatchers.IO) {
            plantDao.toggleFavorite(plantId, isFavorite)
        }
    }

    // ✅ FUNCIÓN PARA ACTUALIZAR UBICACIÓN
    suspend fun updateLocation(plantId: Int, latitude: Double?, longitude: Double?, locationName: String?, notes: String?) {
        withContext(Dispatchers.IO) {
            plantDao.updateLocation(plantId, latitude, longitude, locationName, null, notes)
        }
    }

    // ✅ OBTENER PLANTAS CON UBICACIÓN
    fun getPlantsWithLocation(): LiveData<List<PlantEntity>> = plantDao.getPlantsWithLocation()
}