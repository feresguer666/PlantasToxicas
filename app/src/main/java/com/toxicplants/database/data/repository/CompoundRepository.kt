package com.toxicplants.database.data.repository

import androidx.lifecycle.LiveData
import com.toxicplants.database.CompoundDao
import com.toxicplants.database.CompoundEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CompoundRepository(private val dao: CompoundDao) {

    val all: LiveData<List<CompoundEntity>> = dao.getAll()
    val groups: LiveData<List<String>> = dao.getAllGroups()
    val favorites: LiveData<List<CompoundEntity>> = dao.getFavorites()

    fun getByGroup(group: String): LiveData<List<CompoundEntity>> = dao.getByGroup(group)
    fun search(query: String): LiveData<List<CompoundEntity>> = dao.search("%$query%")
    fun getByIdLive(id: Int): LiveData<CompoundEntity> = dao.getByIdLive(id)

    suspend fun getById(id: Int): CompoundEntity? = withContext(Dispatchers.IO) { dao.getById(id) }
    suspend fun count(): Int = withContext(Dispatchers.IO) { dao.count() }
    suspend fun toggleFavorite(id: Int, fav: Boolean) =
        withContext(Dispatchers.IO) { dao.setFavorite(id, fav) }

    suspend fun insert(compound: CompoundEntity) = withContext(Dispatchers.IO) { dao.insert(compound) }
    suspend fun update(compound: CompoundEntity) = withContext(Dispatchers.IO) { dao.update(compound) }
    suspend fun delete(compound: CompoundEntity) = withContext(Dispatchers.IO) { dao.delete(compound) }
}
