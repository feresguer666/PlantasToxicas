package com.toxicplants.database.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.toxicplants.database.SightingEntity
import com.toxicplants.database.SightingStore

class SightingViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application

    private val _sightings = MutableLiveData<List<SightingEntity>>(SightingStore.load(app))
    val sightings: LiveData<List<SightingEntity>> = _sightings

    fun addSighting(sighting: SightingEntity) {
        val current = _sightings.value.orEmpty()
        val nextId = (current.maxOfOrNull { it.id } ?: 0) + 1
        persist(current + sighting.copy(id = nextId, date = sighting.date.ifBlank { SightingStore.nowString() }))
    }

    fun updateSighting(sighting: SightingEntity) {
        persist(_sightings.value.orEmpty().map { if (it.id == sighting.id) sighting else it })
    }

    fun deleteSighting(sighting: SightingEntity) {
        SightingStore.deletePhoto(sighting.photoPath)
        persist(_sightings.value.orEmpty().filterNot { it.id == sighting.id })
    }

    fun replaceAll(items: List<SightingEntity>) {
        persist(items)
    }

    private fun persist(items: List<SightingEntity>) {
        val sorted = items.sortedByDescending { it.date }
        _sightings.value = sorted
        SightingStore.save(app, sorted)
    }
}
