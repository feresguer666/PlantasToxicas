package com.toxicplants.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface PoisonousFamilyDao {

    @Query("SELECT * FROM poisonous_family_genera ORDER BY familyName COLLATE NOCASE ASC, genusName COLLATE NOCASE ASC")
    fun getAll(): LiveData<List<PoisonousFamilyGenusEntity>>

    @Query("SELECT * FROM poisonous_family_genera ORDER BY familyName COLLATE NOCASE ASC, genusName COLLATE NOCASE ASC")
    suspend fun getAllSync(): List<PoisonousFamilyGenusEntity>

    @Query("SELECT * FROM poisonous_family_genera WHERE catalogType = :catalogType ORDER BY familyName COLLATE NOCASE ASC, genusName COLLATE NOCASE ASC")
    fun getByCatalogType(catalogType: String): LiveData<List<PoisonousFamilyGenusEntity>>

    @Query("SELECT * FROM poisonous_family_genera WHERE familyName = :familyName AND catalogType = :catalogType ORDER BY genusName COLLATE NOCASE ASC")
    fun getByFamily(familyName: String, catalogType: String): LiveData<List<PoisonousFamilyGenusEntity>>

    @Query("SELECT * FROM poisonous_family_genera WHERE familyName = :familyName ORDER BY genusName COLLATE NOCASE ASC")
    fun getByFamily(familyName: String): LiveData<List<PoisonousFamilyGenusEntity>>

    @Query("SELECT * FROM poisonous_family_genera WHERE id = :id LIMIT 1")
    fun getById(id: Int): LiveData<PoisonousFamilyGenusEntity?>

    @Query("SELECT COUNT(*) FROM poisonous_family_genera")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM poisonous_family_genera WHERE catalogType = :catalogType")
    suspend fun countByCatalogType(catalogType: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PoisonousFamilyGenusEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PoisonousFamilyGenusEntity): Long

    @Update
    suspend fun update(item: PoisonousFamilyGenusEntity)

    @Delete
    suspend fun delete(item: PoisonousFamilyGenusEntity)

    @Query("DELETE FROM poisonous_family_genera WHERE familyName = :familyName AND catalogType = :catalogType")
    suspend fun deleteFamily(familyName: String, catalogType: String)

    @Query("DELETE FROM poisonous_family_genera WHERE familyName = :familyName")
    suspend fun deleteFamily(familyName: String)

    @Query("DELETE FROM poisonous_family_genera WHERE catalogType = :catalogType")
    suspend fun deleteAllByCatalogType(catalogType: String)

    @Query("DELETE FROM poisonous_family_genera")
    suspend fun deleteAll()
}
