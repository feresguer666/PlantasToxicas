package com.toxicplants.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface PlantDao {

    @Query("SELECT * FROM plants ORDER BY commonName ASC")
    fun getAllPlants(): LiveData<List<PlantEntity>>

    @Query("SELECT * FROM plants ORDER BY commonName ASC")
    suspend fun getAllPlantsSync(): List<PlantEntity>

    @Query("SELECT * FROM plants WHERE isFavorite = 1 ORDER BY commonName ASC")
    fun getFavoritePlants(): LiveData<List<PlantEntity>>

    @Query("SELECT * FROM plants WHERE id = :id")
    suspend fun getPlantById(id: Int): PlantEntity?

    @Query("SELECT * FROM plants WHERE id = :id")
    fun getPlantByIdLiveData(id: Int): LiveData<PlantEntity>

    @Query("SELECT * FROM plants WHERE id = :id")
    fun getPlantByIdLiveDataFlow(id: Int): LiveData<PlantEntity>

    @Query("SELECT COUNT(*) FROM plants")
    suspend fun getPlantCount(): Int

    @Query("SELECT * FROM plants WHERE commonName LIKE :query OR scientificName LIKE :query OR category LIKE :query")
    fun searchPlants(query: String): LiveData<List<PlantEntity>>

    @Query("SELECT * FROM plants WHERE toxicityLevel = :level ORDER BY commonName ASC")
    fun getPlantsByToxicity(level: String): LiveData<List<PlantEntity>>

    @Query("SELECT * FROM plants WHERE toxicityLevel = :level ORDER BY commonName ASC")
    suspend fun getPlantsByToxicitySync(level: String): List<PlantEntity>

    @Query("SELECT * FROM plants WHERE category = :category ORDER BY commonName ASC")
    fun getPlantsByCategory(category: String): LiveData<List<PlantEntity>>

    @Query("SELECT * FROM plants WHERE family = :family ORDER BY commonName ASC")
    fun getPlantsByFamily(family: String): LiveData<List<PlantEntity>>

    @Query("SELECT DISTINCT category FROM plants ORDER BY category ASC")
    fun getAllCategories(): LiveData<List<String>>

    @Query("SELECT DISTINCT family FROM plants ORDER BY family ASC")
    fun getAllFamilies(): LiveData<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plant: PlantEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(plants: List<PlantEntity>)

    @Update
    suspend fun update(plant: PlantEntity)

    @Delete
    suspend fun delete(plant: PlantEntity)

    @Query("UPDATE plants SET isFavorite = :isFavorite WHERE id = :plantId")
    suspend fun toggleFavorite(plantId: Int, isFavorite: Boolean)

    // ✅ NUEVOS MÉTODOS PARA UBICACIÓN
    @Query("UPDATE plants SET latitude = :lat, longitude = :lng, locationName = :name, foundDate = :date, notes = :notes WHERE id = :plantId")
    suspend fun updateLocation(plantId: Int, lat: Double?, lng: Double?, name: String?, date: String?, notes: String?)

    @Query("SELECT * FROM plants WHERE latitude IS NOT NULL AND longitude IS NOT NULL")
    fun getPlantsWithLocation(): LiveData<List<PlantEntity>>
}