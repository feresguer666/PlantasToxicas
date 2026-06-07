package com.toxicplants.database.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.toxicplants.database.LichenDataSource
import com.toxicplants.database.LichenEntity
import com.toxicplants.database.LichenUserStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LichenViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application

    private val _allLichens = MutableLiveData<List<LichenEntity>>(emptyList())
    val allLichens: LiveData<List<LichenEntity>> = _allLichens

    private val _allSyndromes = MutableLiveData<List<String>>(emptyList())
    val allSyndromes: LiveData<List<String>> = _allSyndromes

    private val _favorites = MutableLiveData<List<LichenEntity>>(emptyList())
    val favorites: LiveData<List<LichenEntity>> = _favorites

    private val _loadError = MutableLiveData<String?>(null)
    val loadError: LiveData<String?> = _loadError

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val lichens = LichenUserStore.load(app) ?: LichenDataSource.loadAll(app)
                publish(lichens)
                _loadError.postValue(null)
            } catch (e: Exception) {
                _loadError.postValue(e.message ?: "No se pudo cargar la base de líquenes")
            }
        }
    }

    fun toggleFavorite(lichen: LichenEntity) {
        val updated = _allLichens.value.orEmpty().map {
            if (it.id == lichen.id) it.copy(isFavorite = !it.isFavorite) else it
        }
        publishAndPersist(updated)
    }

    fun addLichen(lichen: LichenEntity) {
        val current = _allLichens.value.orEmpty()
        val nextId = (current.maxOfOrNull { it.id } ?: 0) + 1
        val item = lichen.copy(id = if (lichen.id == 0) nextId else lichen.id)
        publishAndPersist(current.filterNot { it.id == item.id } + item)
    }

    fun updateLichen(lichen: LichenEntity) {
        val updated = _allLichens.value.orEmpty().map { if (it.id == lichen.id) lichen else it }
        publishAndPersist(updated)
    }

    fun deleteLichen(lichen: LichenEntity) {
        publishAndPersist(_allLichens.value.orEmpty().filterNot { it.id == lichen.id })
    }

    private fun publishAndPersist(items: List<LichenEntity>) {
        val sorted = items.sortedWith(compareByDescending<LichenEntity> { it.isHighRisk }.thenBy { it.scientificName })
        publish(sorted)
        viewModelScope.launch(Dispatchers.IO) { LichenUserStore.save(app, sorted) }
    }

    private fun publish(items: List<LichenEntity>) {
        val sorted = items.sortedWith(compareByDescending<LichenEntity> { it.isHighRisk }.thenBy { it.scientificName })
        _allLichens.postValue(sorted)
        _allSyndromes.postValue(sorted.map { it.syndrome }.filter { it.isNotBlank() }.distinct().sorted())
        _favorites.postValue(sorted.filter { it.isFavorite })
    }
}
