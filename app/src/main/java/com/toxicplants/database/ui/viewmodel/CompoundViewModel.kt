package com.toxicplants.database.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.toxicplants.database.CompoundEntity
import com.toxicplants.database.CompoundUserStateStore
import com.toxicplants.database.PlantDatabase
import com.toxicplants.database.data.repository.CompoundRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ViewModel para la pantalla de Fitoquímica.
 */
class CompoundViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CompoundRepository
    val allCompounds: LiveData<List<CompoundEntity>>
    val allGroups: LiveData<List<String>>
    val favorites: LiveData<List<CompoundEntity>>

    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery

    private val _selectedGroup = MutableLiveData<String?>(null)
    val selectedGroup: LiveData<String?> = _selectedGroup

    init {
        val dao = PlantDatabase.getDatabase(application).compoundDao()
        repository = CompoundRepository(dao)
        allCompounds = repository.all
        allGroups = repository.groups
        favorites = repository.favorites
    }

    fun setSearchQuery(q: String) { _searchQuery.value = q }
    fun setGroup(group: String?) { _selectedGroup.value = group }

    fun search(query: String): LiveData<List<CompoundEntity>> = repository.search(query)
    fun byGroup(group: String): LiveData<List<CompoundEntity>> = repository.getByGroup(group)
    fun byId(id: Int): LiveData<CompoundEntity> = repository.getByIdLive(id)

    fun toggleFavorite(compound: CompoundEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleFavorite(compound.id, !compound.isFavorite)
        }
    }

    fun addCompound(compound: CompoundEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val newId = repository.insert(compound).toInt()
            if (compound.id != 0) {
                CompoundUserStateStore.markEdited(getApplication(), compound.id)
            } else if (newId != 0) {
                CompoundUserStateStore.markEdited(getApplication(), newId)
            }
        }
    }

    fun updateCompound(compound: CompoundEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.update(compound)
            CompoundUserStateStore.markEdited(getApplication(), compound.id)
        }
    }

    fun deleteCompound(compound: CompoundEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(compound)
            CompoundUserStateStore.markDeleted(getApplication(), compound.id)
        }
    }
}
