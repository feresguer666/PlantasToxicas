package com.toxicplants.database.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.toxicplants.database.DichotomousKeyDataSource
import com.toxicplants.database.DichotomousKeyEntity
import com.toxicplants.database.KeyFilter
import com.toxicplants.database.KeyNodeEntity
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.PlantFilterEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Un paso del recorrido: el usuario llegó a un nodo y eligió una opción.
 * Guardamos el filtro que esa opción aplicó para poder rehacer/deshacer.
 */
data class KeyStep(
    val nodeId: String,
    val question: String,
    val chosenOptionIndex: Int,
    val chosenLabel: String,
    val appliedFilter: KeyFilter,
    val extraResultIds: List<Int>
)

/** Estado completo de la pantalla DichotomousKeyScreen. */
data class DichotomousKeyUiState(
    val loading: Boolean = false,
    val key: DichotomousKeyEntity? = null,
    val currentNode: KeyNodeEntity? = null,
    val history: List<KeyStep> = emptyList(),
    /** Recuento de plantas que cumplen los filtros acumulados (se actualiza en cada paso). */
    val matchCount: Int = 0,
    /** Cuando se llega a hoja terminal: lista de plantas candidatas calculada. */
    val resultPlants: List<PlantEntity>? = null,
    val resultNote: String = "",
    val errorMessage: String? = null
)

class DichotomousKeyViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(DichotomousKeyUiState())
    val state: StateFlow<DichotomousKeyUiState> = _state.asStateFlow()

    private val _allKeys = MutableStateFlow<List<DichotomousKeyEntity>>(emptyList())
    val allKeys: StateFlow<List<DichotomousKeyEntity>> = _allKeys.asStateFlow()

    /** Catálogo completo (se carga una sola vez). */
    private var allPlants: List<PlantEntity> = emptyList()

    init {
        loadAllKeys()
    }

    private fun loadAllKeys() {
        viewModelScope.launch(Dispatchers.IO) {
            _allKeys.value = DichotomousKeyDataSource.loadAll(getApplication())
        }
    }

    /** El ViewModel necesita la lista de plantas para calcular filtros. */
    fun setPlants(plants: List<PlantEntity>) {
        allPlants = plants
        // Si ya hay una clave activa, recalcular el contador.
        val s = _state.value
        if (s.key != null && s.resultPlants == null) {
            val filter = currentCombinedFilter()
            _state.value = s.copy(matchCount = PlantFilterEngine.apply(allPlants, filter).size)
        }
    }

    fun startKey(keyId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = DichotomousKeyUiState(loading = true)
            val key = DichotomousKeyDataSource.loadById(getApplication(), keyId)
            if (key == null) {
                _state.value = DichotomousKeyUiState(errorMessage = "No se encontró la clave '$keyId'.")
                return@launch
            }
            val root = key.rootNode()
            if (root == null) {
                _state.value = DichotomousKeyUiState(errorMessage = "La clave '$keyId' no tiene nodo raíz válido.")
                return@launch
            }
            // matchCount inicial = plantas que pasan el filtro BASE de la clave
            val baseCount = PlantFilterEngine.apply(allPlants, key.baseFilter()).size
            _state.value = DichotomousKeyUiState(
                loading = false,
                key = key,
                currentNode = root,
                history = emptyList(),
                matchCount = baseCount
            )
        }
    }

    fun chooseOption(optionIndex: Int) {
        val s = _state.value
        val key = s.key ?: return
        val node = s.currentNode ?: return
        val option = node.options.getOrNull(optionIndex) ?: return

        val newStep = KeyStep(
            nodeId = node.id,
            question = node.question,
            chosenOptionIndex = optionIndex,
            chosenLabel = option.label,
            appliedFilter = if (option.resetFilters) KeyFilter() else option.filter,
            extraResultIds = option.resultPlantIds
        )

        // ¿Es hoja terminal?
        if (option.nextNodeId.isNullOrBlank()) {
            val newHistory = s.history + newStep
            val combined = combineHistoryFilters(key, newHistory, option.resetFilters)
            val filtered = PlantFilterEngine.apply(allPlants, combined)
            val extras = option.resultPlantIds.toSet()
            val extraPlants = allPlants.filter { it.id in extras }
            val result = (filtered + extraPlants).distinctBy { it.id }
                .sortedWith(compareBy({ toxicityRank(it.toxicityLevel) }, { it.commonName }))

            _state.value = s.copy(
                history = newHistory,
                currentNode = null,
                resultPlants = result,
                resultNote = option.resultNote,
                matchCount = result.size
            )
            return
        }

        val nextNode = key.nodesById[option.nextNodeId]
        if (nextNode == null) {
            _state.value = s.copy(
                history = s.history + newStep,
                currentNode = null,
                resultPlants = emptyList(),
                resultNote = "⚠️ La rama apuntaba a '${option.nextNodeId}' pero no se encontró ese nodo.",
                matchCount = 0
            )
            return
        }

        // Rama interna: solo actualizamos historia, nodo actual y contador
        val newHistory = s.history + newStep
        val combined = combineHistoryFilters(key, newHistory, false)
        val count = PlantFilterEngine.apply(allPlants, combined).size

        _state.value = s.copy(
            history = newHistory,
            currentNode = nextNode,
            resultPlants = null,
            resultNote = "",
            matchCount = count
        )
    }

    fun goBack() {
        val s = _state.value
        if (s.history.isEmpty()) return
        val key = s.key ?: return

        val lastStep = s.history.last()
        val newHistory = s.history.dropLast(1)
        val targetNode = key.nodesById[lastStep.nodeId] ?: key.rootNode()
        val combined = combineHistoryFilters(key, newHistory, false)
        val count = PlantFilterEngine.apply(allPlants, combined).size

        _state.value = s.copy(
            history = newHistory,
            currentNode = targetNode,
            resultPlants = null,
            resultNote = "",
            matchCount = count
        )
    }

    fun restart() {
        val key = _state.value.key ?: return
        val baseCount = PlantFilterEngine.apply(allPlants, key.baseFilter()).size
        _state.value = DichotomousKeyUiState(
            key = key,
            currentNode = key.rootNode(),
            history = emptyList(),
            matchCount = baseCount
        )
    }

    fun clear() {
        _state.value = DichotomousKeyUiState()
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private fun currentCombinedFilter(): KeyFilter {
        val s = _state.value
        val key = s.key ?: return KeyFilter()
        return combineHistoryFilters(key, s.history, false)
    }

    /**
     * Combina el filtro base de la clave + los filtros aplicados en cada paso.
     * Si en algún paso una opción tenía resetFilters=true, descartamos los filtros
     * anteriores hasta ese punto.
     */
    private fun combineHistoryFilters(
        key: DichotomousKeyEntity,
        history: List<KeyStep>,
        lastResets: Boolean
    ): KeyFilter {
        // Encontrar el último reset (si lo hay) y empezar desde ahí
        val resetIdx = history.indexOfLast { it.appliedFilter.isEmpty() && resetsAt(key, it) }
        val effective = if (resetIdx >= 0) history.drop(resetIdx) else history
        val baseList = if (resetIdx >= 0) emptyList() else listOf(key.baseFilter())
        val all = baseList + effective.map { it.appliedFilter }
        return PlantFilterEngine.combine(all)
    }

    /** Detecta si en un paso la opción elegida tenía resetFilters=true. */
    private fun resetsAt(key: DichotomousKeyEntity, step: KeyStep): Boolean {
        val node = key.nodesById[step.nodeId] ?: return false
        return node.options.getOrNull(step.chosenOptionIndex)?.resetFilters == true
    }

    private fun toxicityRank(level: String): Int = when (level.lowercase()) {
        "mortal" -> 0
        "muy alto" -> 1
        "alto" -> 2
        "moderado" -> 3
        "bajo" -> 4
        else -> 5
    }
}
