package com.toxicplants.database.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.toxicplants.database.MushroomDataSource
import com.toxicplants.database.MushroomEntity
import com.toxicplants.database.MushroomUserStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ViewModel de Setas tóxicas.
 *
 * Carga el catálogo base desde assets/fallback y guarda las ediciones del usuario
 * en almacenamiento local de la app. Así se puede añadir/editar sin depender de
 * migraciones Room durante el arranque.
 */
class MushroomViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application

    private val _allMushrooms = MutableLiveData<List<MushroomEntity>>(emptyList())
    val allMushrooms: LiveData<List<MushroomEntity>> = _allMushrooms

    private val _allSyndromes = MutableLiveData<List<String>>(emptyList())
    val allSyndromes: LiveData<List<String>> = _allSyndromes

    private val _favorites = MutableLiveData<List<MushroomEntity>>(emptyList())
    val favorites: LiveData<List<MushroomEntity>> = _favorites

    private val _loadError = MutableLiveData<String?>(null)
    val loadError: LiveData<String?> = _loadError

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val baseCatalog = MushroomDataSource.loadAll(app)
                val savedCatalog = MushroomUserStore.load(app)
                val mushrooms = if (savedCatalog == null) {
                    baseCatalog
                } else {
                    mergeSavedCatalogWithBase(savedCatalog, baseCatalog).also { merged ->
                        // Si el JSON base incorpora nuevas especies, las añadimos al
                        // catálogo editable ya guardado sin perder favoritos/ediciones.
                        if (merged.size != savedCatalog.size) MushroomUserStore.save(app, merged)
                    }
                }
                publish(mushrooms)
                _loadError.postValue(null)
            } catch (e: Exception) {
                _allMushrooms.postValue(emptyList())
                _allSyndromes.postValue(emptyList())
                _favorites.postValue(emptyList())
                _loadError.postValue(e.message ?: "No se pudo cargar la base de setas")
            }
        }
    }

    fun toggleFavorite(mushroom: MushroomEntity) {
        val updated = _allMushrooms.value.orEmpty().map {
            if (it.id == mushroom.id) it.copy(isFavorite = !it.isFavorite) else it
        }
        publishAndPersist(updated)
    }

    fun addMushroom(mushroom: MushroomEntity) {
        val current = _allMushrooms.value.orEmpty()
        val nextId = (current.maxOfOrNull { it.id } ?: 0) + 1
        val item = mushroom.copy(id = if (mushroom.id == 0) nextId else mushroom.id)
        publishAndPersist(current.filterNot { it.id == item.id } + item)
    }

    fun updateMushroom(mushroom: MushroomEntity) {
        val updated = _allMushrooms.value.orEmpty().map {
            if (it.id == mushroom.id) mushroom else it
        }
        publishAndPersist(updated)
    }

    fun deleteMushroom(mushroom: MushroomEntity) {
        val updated = _allMushrooms.value.orEmpty().filterNot { it.id == mushroom.id }
        publishAndPersist(updated)
    }

    fun resetToBaseCatalog() {
        viewModelScope.launch(Dispatchers.IO) {
            val base = MushroomDataSource.loadAll(app)
            MushroomUserStore.save(app, base)
            publish(base)
        }
    }

    private fun publishAndPersist(items: List<MushroomEntity>) {
        val sorted = items.sortedWith(compareByDescending<MushroomEntity> { it.isDeadly }.thenBy { it.scientificName })
        publish(sorted)
        viewModelScope.launch(Dispatchers.IO) {
            MushroomUserStore.save(app, sorted)
        }
    }

    private fun mergeSavedCatalogWithBase(
        savedCatalog: List<MushroomEntity>,
        baseCatalog: List<MushroomEntity>,
    ): List<MushroomEntity> {
        val merged = savedCatalog.toMutableList()
        val scientificNames = merged
            .map { it.scientificName.trim().lowercase() }
            .filter { it.isNotBlank() }
            .toMutableSet()
        val usedIds = merged.map { it.id }.toMutableSet()
        var nextId = ((savedCatalog + baseCatalog).maxOfOrNull { it.id } ?: 0) + 1

        baseCatalog.forEach { baseItem ->
            val key = baseItem.scientificName.trim().lowercase()
            if (key.isBlank() || key in scientificNames) return@forEach

            val item = if (baseItem.id != 0 && baseItem.id !in usedIds) {
                baseItem
            } else {
                baseItem.copy(id = nextId++)
            }
            merged += item
            scientificNames += key
            usedIds += item.id
        }
        return merged
    }

    private fun publish(items: List<MushroomEntity>) {
        val sorted = items.sortedWith(compareByDescending<MushroomEntity> { it.isDeadly }.thenBy { it.scientificName })
        _allMushrooms.postValue(sorted)
        _allSyndromes.postValue(
            sorted.map { it.syndrome }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
        )
        _favorites.postValue(sorted.filter { it.isFavorite })
    }
}
