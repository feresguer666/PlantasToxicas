package com.toxicplants.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface CompoundDao {

    @Query("SELECT * FROM compounds ORDER BY commonName ASC")
    fun getAll(): LiveData<List<CompoundEntity>>

    @Query("SELECT * FROM compounds ORDER BY commonName ASC")
    suspend fun getAllSync(): List<CompoundEntity>

    @Query("SELECT COUNT(*) FROM compounds")
    suspend fun count(): Int

    @Query("SELECT * FROM compounds WHERE id = :id")
    suspend fun getById(id: Int): CompoundEntity?

    @Query("SELECT * FROM compounds WHERE id = :id")
    fun getByIdLive(id: Int): LiveData<CompoundEntity>

    @Query("SELECT DISTINCT groupName FROM compounds ORDER BY groupName ASC")
    fun getAllGroups(): LiveData<List<String>>

    @Query("SELECT * FROM compounds WHERE groupName = :group ORDER BY commonName ASC")
    fun getByGroup(group: String): LiveData<List<CompoundEntity>>

    @Query(
        "SELECT * FROM compounds " +
            "WHERE commonName LIKE :q " +
            "OR iupacName LIKE :q " +
            "OR groupName LIKE :q " +
            "OR sourcePlants LIKE :q " +
            "ORDER BY commonName ASC"
    )
    fun search(q: String): LiveData<List<CompoundEntity>>

    @Query("SELECT * FROM compounds WHERE isFavorite = 1 ORDER BY commonName ASC")
    fun getFavorites(): LiveData<List<CompoundEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CompoundEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CompoundEntity): Long

    @Update
    suspend fun update(item: CompoundEntity)

    @Query("UPDATE compounds SET isFavorite = :fav WHERE id = :id")
    suspend fun setFavorite(id: Int, fav: Boolean)
}
