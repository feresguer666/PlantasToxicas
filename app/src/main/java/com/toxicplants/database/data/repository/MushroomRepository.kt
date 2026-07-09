package com.toxicplants.database.data.repository

import androidx.lifecycle.LiveData
import com.toxicplants.database.MushroomDao
import com.toxicplants.database.MushroomEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MushroomRepository(private val dao: MushroomDao) {

    val all: LiveData<List<MushroomEntity>> = dao.getAll()
    val syndromes: LiveData<List<String>> = dao.getAllSyndromes()
    val favorites: LiveData<List<MushroomEntity>> = dao.getFavorites()

    fun search(query: String): LiveData<List<MushroomEntity>> = dao.search("%$query%")
    fun byToxicity(level: String): LiveData<List<MushroomEntity>> = dao.getByToxicity(level)
    fun bySyndrome(syndrome: String): LiveData<List<MushroomEntity>> = dao.getBySyndrome(syndrome)
    fun byId(id: Int): LiveData<MushroomEntity> = dao.getByIdLive(id)

    suspend fun count(): Int = withContext(Dispatchers.IO) { dao.count() }
    suspend fun insertAll(items: List<MushroomEntity>) = withContext(Dispatchers.IO) { dao.insertAll(items) }
    suspend fun insert(item: MushroomEntity) = withContext(Dispatchers.IO) { dao.insert(item) }
    suspend fun update(item: MushroomEntity) = withContext(Dispatchers.IO) { dao.update(item) }
    suspend fun delete(item: MushroomEntity) = withContext(Dispatchers.IO) { dao.delete(item) }
    suspend fun toggleFavorite(id: Int, fav: Boolean) = withContext(Dispatchers.IO) { dao.setFavorite(id, fav) }
}
