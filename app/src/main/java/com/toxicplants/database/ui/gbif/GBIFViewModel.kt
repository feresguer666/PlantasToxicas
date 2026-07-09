package com.toxicplants.database.ui.gbif

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GBIFViewModel : ViewModel() {
    
    private val repository = GBIFRepository()
    
    private val _searchResult = MutableStateFlow<SearchResultState>(SearchResultState.Idle)
    val searchResult: StateFlow<SearchResultState> = _searchResult.asStateFlow()
    
    private val _vernacularNames = MutableStateFlow<List<GBIFVernacularName>>(emptyList())
    val vernacularNames: StateFlow<List<GBIFVernacularName>> = _vernacularNames.asStateFlow()
    
    private val _occurrences = MutableStateFlow<List<GBIFOccurrence>>(emptyList())
    val occurrences: StateFlow<List<GBIFOccurrence>> = _occurrences.asStateFlow()
    
    fun searchSpecies(scientificName: String) {
        viewModelScope.launch {
            _searchResult.value = SearchResultState.Loading
            val result = repository.matchSpecies(scientificName)
            
            _searchResult.value = result.fold(
                onSuccess = { match ->
                    if (match.usageKey != null) {
                        SearchResultState.Success(match)
                    } else {
                        SearchResultState.NotFound(scientificName)
                    }
                },
                onFailure = { error ->
                    SearchResultState.Error(error.message ?: "Error desconocido")
                }
            )
        }
    }
    
    fun loadVernacularNames(gbifKey: Long) {
        viewModelScope.launch {
            val result = repository.getVernacularNames(gbifKey)
            if (result.isSuccess) {
                _vernacularNames.value = result.getOrNull() ?: emptyList()
            }
        }
    }
    
    fun loadOccurrences(gbifKey: Long) {
        viewModelScope.launch {
            val result = repository.getOccurrences(gbifKey)
            if (result.isSuccess) {
                _occurrences.value = result.getOrNull() ?: emptyList()
            }
        }
    }
    
    fun resetSearch() {
        _searchResult.value = SearchResultState.Idle
        _vernacularNames.value = emptyList()
        _occurrences.value = emptyList()
    }
}

sealed class SearchResultState {
    object Idle : SearchResultState()
    object Loading : SearchResultState()
    data class Success(val match: GBIFSpeciesMatch) : SearchResultState()
    data class NotFound(val query: String) : SearchResultState()
    data class Error(val message: String) : SearchResultState()
}