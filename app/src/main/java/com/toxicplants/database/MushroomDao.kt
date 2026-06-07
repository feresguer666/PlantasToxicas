package com.toxicplants.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface MushroomDao {

    @Query("SELECT * FROM mushrooms ORDER BY isDeadly DESC, scientificName ASC")
    fun getAll(): LiveData<List<MushroomEntity>>

    @Query("SELECT * FROM mushrooms ORDER BY isDeadly DESC, scientificName ASC")
    suspend fun getAllSync(): List<MushroomEntity>

    @Query("SELECT COUNT(*) FROM mushrooms")
    suspend fun count(): Int

    @Query("SELECT * FROM mushrooms WHERE id = :id")
    fun getByIdLive(id: Int): LiveData<MushroomEntity>

    @Query("SELECT DISTINCT syndrome FROM mushrooms ORDER BY syndrome ASC")
    fun getAllSyndromes(): LiveData<List<String>>

    @Query("SELECT * FROM mushrooms WHERE toxicityLevel = :level ORDER BY isDeadly DESC, scientificName ASC")
    fun getByToxicity(level: String): LiveData<List<MushroomEntity>>

    @Query("SELECT * FROM mushrooms WHERE syndrome = :syndrome ORDER BY isDeadly DESC, scientificName ASC")
    fun getBySyndrome(syndrome: String): LiveData<List<MushroomEntity>>

    @Query(
        "SELECT * FROM mushrooms " +
                "WHERE commonName LIKE :q " +
                "OR scientificName LIKE :q " +
                "OR family LIKE :q " +
                "OR toxicityLevel LIKE :q " +
                "OR syndrome LIKE :q " +
                "OR toxicCompounds LIKE :q " +
                "OR edibleConfusions LIKE :q " +
                "ORDER BY isDeadly DESC, scientificName ASC"
    )
    fun search(q: String): LiveData<List<MushroomEntity>>

    @Query("SELECT * FROM mushrooms WHERE isFavorite = 1 ORDER BY scientificName ASC")
    fun getFavorites(): LiveData<List<MushroomEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MushroomEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: MushroomEntity): Long

    @Update
    suspend fun update(item: MushroomEntity)

    @Delete
    suspend fun delete(item: MushroomEntity)

    @Query("UPDATE mushrooms SET isFavorite = :fav WHERE id = :id")
    suspend fun setFavorite(id: Int, fav: Boolean)
}
