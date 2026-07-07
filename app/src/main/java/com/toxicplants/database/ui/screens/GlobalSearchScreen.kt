package com.toxicplants.database.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toxicplants.database.CompoundEntity
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.ui.search.SearchQuery
import com.toxicplants.database.ui.search.buildSearchQuery
import com.toxicplants.database.ui.search.fuzzyTextScore
import com.toxicplants.database.ui.search.normalizeForSearch
import com.toxicplants.database.ui.search.searchTokens
import com.toxicplants.database.ui.viewmodel.CompoundViewModel
import com.toxicplants.database.ui.viewmodel.PlantViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchScreen(
    plantViewModel: PlantViewModel,
    compoundViewModel: CompoundViewModel,
    initialQuery: String = "",
    onPlantClick: (PlantEntity) -> Unit,
    onCompoundClick: (CompoundEntity) -> Unit,
    onBack: () -> Unit,
) {
    val allPlants by plantViewModel.allPlants.observeAsState(emptyList())
    val allCompounds by compoundViewModel.allCompounds.observeAsState(emptyList())
    val colors = MaterialTheme.colorScheme

    var query by remember { mutableStateOf(initialQuery) }
    var selectedFilter by remember { mutableStateOf(GlobalSearchFilter.All) }
    var nameSearchMode by remember { mutableStateOf(GlobalNameSearchMode.All) }
    var alphabetFilter by remember { mutableStateOf(GlobalAlphabetFilter.All) }

    val searchQuery = remember(query) { buildSearchQuery(query) }
    var plantResults by remember { mutableStateOf<List<PlantEntity>>(emptyList()) }
    var compoundResults by remember { mutableStateOf<List<CompoundEntity>>(emptyList()) }
    var familyResults by remember { mutableStateOf<List<String>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var usedFuzzySearch by remember { mutableStateOf(false) }
    var selectionMode by remember { mutableStateOf(false) }
    val selectedPlantIds = remember { mutableStateListOf<Int>() }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }
    var showBulkEditDialog by remember { mutableStateOf(false) }

    LaunchedEffect(searchQuery, allPlants, allCompounds, nameSearchMode, alphabetFilter) {
        if (searchQuery.normalized.length < 2) {
            plantResults = emptyList()
            compoundResults = emptyList()
            familyResults = emptyList()
            isSearching = false
            usedFuzzySearch = false
            return@LaunchedEffect
        }

        isSearching = true
        delay(250)
        val plantsSnapshot = allPlants
        val compoundsSnapshot = allCompounds

        val result = withContext(Dispatchers.Default) {
            val exactPlantMatches = plantsSnapshot
                .mapNotNull { plant ->
                    currentCoroutineContext().ensureActive()
                    val score = plantGlobalExactSearchScore(plant, searchQuery, nameSearchMode)
                    if (score > 0) score to plant else null
                }

            val exactCompoundMatches = compoundsSnapshot
                .mapNotNull { compound ->
                    currentCoroutineContext().ensureActive()
                    val score = compoundGlobalExactSearchScore(compound, searchQuery)
                    if (score > 0) score to compound else null
                }

            val exactFamilyMatches =
                plantsSnapshot.map { it.family }.filter { it.isNotBlank() }.distinct()
                    .mapNotNull { family ->
                        currentCoroutineContext().ensureActive()
                        val score = familyGlobalExactSearchScore(family, searchQuery)
                        if (score > 0) score to family else null
                    }

            val exactTotal =
                exactPlantMatches.size + exactCompoundMatches.size + exactFamilyMatches.size
            val shouldUseFuzzy = exactTotal < 20 && searchQuery.normalized.length >= 4

            val exactPlantIds = exactPlantMatches.map { it.second.id }.toHashSet()
            val fuzzyPlantMatches = if (shouldUseFuzzy) {
                plantsSnapshot.mapNotNull { plant ->
                    currentCoroutineContext().ensureActive()
                    if (plant.id in exactPlantIds) return@mapNotNull null
                    val score = plantGlobalFuzzyFallbackScore(plant, searchQuery, nameSearchMode)
                    if (score > 0) score to plant else null
                }
            } else emptyList()

            val exactCompoundIds = exactCompoundMatches.map { it.second.id }.toHashSet()
            val fuzzyCompoundMatches = if (shouldUseFuzzy) {
                compoundsSnapshot.mapNotNull { compound ->
                    currentCoroutineContext().ensureActive()
                    if (compound.id in exactCompoundIds) return@mapNotNull null
                    val score = compoundGlobalFuzzyFallbackScore(compound, searchQuery)
                    if (score > 0) score to compound else null
                }
            } else emptyList()

            val exactFamilyNames = exactFamilyMatches.map { it.second }.toHashSet()
            val fuzzyFamilyMatches = if (shouldUseFuzzy) {
                plantsSnapshot.map { it.family }.filter { it.isNotBlank() }.distinct()
                    .mapNotNull { family ->
                        currentCoroutineContext().ensureActive()
                        if (family in exactFamilyNames) return@mapNotNull null
                        val score = familyGlobalFuzzyFallbackScore(family, searchQuery)
                        if (score > 0) score to family else null
                    }
            } else emptyList()

            val plants = (exactPlantMatches + fuzzyPlantMatches)
                .sortedByDescending { it.first }
                .take(250)
                .map { it.second }

            val compounds = (exactCompoundMatches + fuzzyCompoundMatches)
                .sortedByDescending { it.first }
                .take(120)
                .map { it.second }

            val families = (exactFamilyMatches + fuzzyFamilyMatches)
                .sortedByDescending { it.first }
                .take(80)
                .map { it.second }

            GlobalSearchResultSet(
                plants = plants,
                compounds = compounds,
                families = families,
                usedFuzzy = shouldUseFuzzy
            )
        }

        plantResults = result.plants
        compoundResults = result.compounds
        familyResults = result.families
        usedFuzzySearch = result.usedFuzzy
        isSearching = false
    }

    val filteredPlants = remember(plantResults, alphabetFilter, nameSearchMode) {
        if (alphabetFilter == GlobalAlphabetFilter.All) {
            plantResults
        } else {
            plantResults.filter { plant ->
                val firstChar = when (nameSearchMode) {
                    GlobalNameSearchMode.CommonName -> plant.commonName.firstOrNull()
                        ?.uppercaseChar()

                    GlobalNameSearchMode.ScientificName -> plant.scientificName.firstOrNull()
                        ?.uppercaseChar()

                    GlobalNameSearchMode.All -> plant.commonName.firstOrNull()?.uppercaseChar()
                        ?: plant.scientificName.firstOrNull()?.uppercaseChar()
                }
                firstChar == alphabetFilter.letter
            }
        }
    }

    val showPlants =
        selectedFilter == GlobalSearchFilter.All || selectedFilter == GlobalSearchFilter.Plants
    val showCompounds =
        selectedFilter == GlobalSearchFilter.All || selectedFilter == GlobalSearchFilter.Compounds
    val showFamilies =
        selectedFilter == GlobalSearchFilter.All || selectedFilter == GlobalSearchFilter.Families

    val visiblePlantResults = if (showPlants) filteredPlants else emptyList()
    val visibleCompoundResults = if (showCompounds) compoundResults else emptyList()
    val visibleFamilyResults = if (showFamilies) familyResults else emptyList()
    val totalResults =
        visiblePlantResults.size + visibleCompoundResults.size + visibleFamilyResults.size

    val selectedPlants = remember(visiblePlantResults, selectedPlantIds.toList()) {
        visiblePlantResults.filter { it.id in selectedPlantIds }
    }

    LaunchedEffect(visiblePlantResults) {
        val visibleIds = visiblePlantResults.map { it.id }.toSet()
        selectedPlantIds.removeAll { it !in visibleIds }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── TopBar ───────────────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSecondaryContainer
        ) {
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (selectionMode) {
                            selectionMode = false
                            selectedPlantIds.clear()
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            if (selectionMode) "Cancelar selección" else "Volver",
                            tint = Color.Black
                        )
                    }
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.weight(1f),
                        enabled = !selectionMode,
                        placeholder = { Text("", fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Search,
                                null,
                                tint = Color.Black.copy(alpha = 0.7f)
                            )
                        },
                        trailingIcon = {
                            if (query.isNotEmpty() && !selectionMode) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(
                                        Icons.Filled.Clear,
                                        "Limpiar",
                                        tint = Color.Black.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            disabledTextColor = Color.Black.copy(alpha = 0.65f),
                            cursorColor = Color.Black,
                            focusedBorderColor = Color.Black.copy(alpha = 0.5f),
                            unfocusedBorderColor = Color.Black.copy(alpha = 0.3f),
                            disabledBorderColor = Color.Black.copy(alpha = 0.2f),
                            focusedPlaceholderColor = Color.Black.copy(alpha = 0.5f),
                            unfocusedPlaceholderColor = Color.Black.copy(alpha = 0.5f),
                        ),
                        shape = RoundedCornerShape(24.dp),
                    )
                    TextButton(onClick = {
                        if (selectionMode) {
                            selectionMode = false
                            selectedPlantIds.clear()
                        } else {
                            selectedFilter = GlobalSearchFilter.Plants
                            selectionMode = true
                        }
                    }) {
                        Text(
                            if (selectionMode) "Cancelar" else "Seleccionar",
                            color = Color.Black,
                            fontSize = 12.sp
                        )
                    }
                }

                if (selectionMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, end = 8.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "${selectedPlantIds.size} plantas seleccionadas",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = {
                            val visibleIds = visiblePlantResults.map { it.id }
                            val allVisibleSelected =
                                visibleIds.isNotEmpty() && visibleIds.all { it in selectedPlantIds }
                            if (allVisibleSelected) {
                                selectedPlantIds.removeAll(visibleIds.toSet())
                            } else {
                                visibleIds.forEach { id ->
                                    if (id !in selectedPlantIds) selectedPlantIds.add(
                                        id
                                    )
                                }
                            }
                        }) { Text("Todas", color = Color.Black, fontSize = 12.sp) }
                        TextButton(
                            enabled = selectedPlantIds.isNotEmpty(),
                            onClick = { showBulkEditDialog = true }
                        ) {
                            Text(
                                "Editar",
                                color = if (selectedPlantIds.isNotEmpty()) Color.Black else Color.Black.copy(
                                    alpha = 0.35f
                                ),
                                fontSize = 12.sp
                            )
                        }
                        TextButton(
                            enabled = selectedPlantIds.isNotEmpty(),
                            onClick = { showBulkDeleteDialog = true }
                        ) {
                            Text(
                                "Eliminar",
                                color = if (selectedPlantIds.isNotEmpty()) Color.Black else Color.Black.copy(
                                    alpha = 0.35f
                                ),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // ── Filtro de Nombre (Modo) ──────────────────────────────────────────────
        Surface(
            color = colors.surfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.fillMaxWidth()
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .padding(end = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "🔤 Nombre:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
                items(GlobalNameSearchMode.entries) { mode ->
                    FilterChip(
                        selected = nameSearchMode == mode,
                        onClick = { nameSearchMode = mode },
                        label = { Text(mode.label, fontSize = 11.sp) },
                        leadingIcon = { Text(mode.icon, fontSize = 12.sp) },
                        modifier = Modifier.height(28.dp)
                    )
                }
            }
        }

        // ── Filtro Alfabético ──────────────────────────────────────────────
        Surface(color = colors.surface, modifier = Modifier.fillMaxWidth()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .padding(end = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "A-Z:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
                items(GlobalAlphabetFilter.entries) { letter ->
                    FilterChip(
                        selected = alphabetFilter == letter,
                        onClick = { alphabetFilter = letter },
                        label = {
                            Text(
                                letter.label,
                                fontSize = 12.sp,
                                fontWeight = if (letter == GlobalAlphabetFilter.All) FontWeight.Normal else FontWeight.Bold
                            )
                        },
                        modifier = Modifier.height(28.dp),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = colors.primaryContainer)
                    )
                }
            }
        }

        // ── Filtros principales ──────────────────────────────────────────────
        Surface(
            color = colors.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(GlobalSearchFilter.entries) { filter ->
                    val count = when (filter) {
                        GlobalSearchFilter.All -> plantResults.size + compoundResults.size + familyResults.size
                        GlobalSearchFilter.Plants -> filteredPlants.size
                        GlobalSearchFilter.Compounds -> compoundResults.size
                        GlobalSearchFilter.Families -> familyResults.size
                    }
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text("${filter.label} ($count)", fontSize = 12.sp) },
                        leadingIcon = { Text(filter.icon, fontSize = 14.sp) }
                    )
                }
            }
        }

        // ── Contador ─────────────────────────────────────────────
        if (query.length >= 2) {
            Surface(
                color = colors.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (isSearching) "🔎 Buscando…"
                    else if (selectionMode) "☑️ ${selectedPlantIds.size} seleccionadas de ${visiblePlantResults.size} plantas visibles"
                    else buildString {
                        append("📋 $totalResults resultados")
                        if (alphabetFilter != GlobalAlphabetFilter.All) append(" · $alphabetFilter")
                        if (nameSearchMode != GlobalNameSearchMode.All) append(" · ${nameSearchMode.label}")
                        append(" · \"$query\"")
                        if (usedFuzzySearch) append(" · búsqueda ampliada")
                    },
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.onBackground
                )
            }
        }

        // ── Resultados ───────────────────────────────────────────
        if (query.length < 2) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔍", fontSize = 48.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("", color = colors.onSurfaceVariant, fontSize = 14.sp)
                    Text("", color = colors.onSurfaceVariant.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            }
        } else if (isSearching && totalResults == 0) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(8.dp))
                    Text("Buscando…", color = colors.onSurfaceVariant)
                }
            }
        } else if (totalResults == 0) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔍", fontSize = 48.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Sin resultados", fontWeight = FontWeight.Bold)
                    Text("Prueba con otros términos", color = colors.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (visibleFamilyResults.isNotEmpty()) {
                    item {
                        Text(
                            "📚 Familias (${visibleFamilyResults.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
                        )
                    }
                    items(visibleFamilyResults) { family ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF1565C0).copy(
                                    alpha = 0.08f
                                )
                            ),
                            elevation = CardDefaults.cardElevation(1.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("📚", fontSize = 16.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(family, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                        }
                    }
                }

                if (visiblePlantResults.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "🌿 Plantas (${visiblePlantResults.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
                        )
                    }
                    items(visiblePlantResults) { plant ->
                        GlobalSelectablePlantResultCard(
                            plant = plant,
                            query = query,
                            onClick = {
                                plantViewModel.setDetailNavigationPlants(visiblePlantResults)
                                onPlantClick(plant)
                            },
                            selectionMode = selectionMode,
                            selected = plant.id in selectedPlantIds,
                            onSelectionChange = { checked ->
                                if (checked) {
                                    if (plant.id !in selectedPlantIds) selectedPlantIds.add(plant.id)
                                } else {
                                    selectedPlantIds.remove(plant.id)
                                }
                            }
                        )
                    }
                }

                if (visibleCompoundResults.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "🧪 Compuestos (${visibleCompoundResults.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
                        )
                    }
                    items(visibleCompoundResults) { compound ->
                        CompactCompoundCard(
                            compound = compound,
                            query = query,
                            onClick = { onCompoundClick(compound) })
                    }
                }
            }
        }
    }

    if (showBulkDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteDialog = false },
            title = { Text("¿Eliminar ${selectedPlantIds.size} plantas?") },
            text = { Text("Se eliminarán las plantas marcadas dentro de la búsqueda global. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    plantViewModel.deletePlants(selectedPlants)
                    selectedPlantIds.clear()
                    selectionMode = false
                    showBulkDeleteDialog = false
                }) { Text("Eliminar", color = colors.error) }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (showBulkEditDialog) {
        GlobalBulkEditPlantsDialog(
            selectedCount = selectedPlantIds.size,
            onDismiss = { showBulkEditDialog = false },
            onConfirm = { field, value, append ->
                plantViewModel.bulkUpdatePlants(selectedPlants, field.id, value, append)
                selectedPlantIds.clear()
                selectionMode = false
                showBulkEditDialog = false
            }
        )
    }
}

// ── Enums para los nuevos filtros (prefijados con "Global" para evitar conflictos) ──

private enum class GlobalNameSearchMode(val label: String, val icon: String) {
    All("Todos", "🌐"),
    CommonName("Nombre común", "🏷️"),
    ScientificName("Nombre latino", "🔬")
}

private enum class GlobalAlphabetFilter(val label: String, val letter: Char?) {
    All("All", null),
    A("A", 'A'), B("B", 'B'), C("C", 'C'), D("D", 'D'),
    E("E", 'E'), F("F", 'F'), G("G", 'G'), H("H", 'H'),
    I("I", 'I'), J("J", 'J'), K("K", 'K'), L("L", 'L'),
    M("M", 'M'), N("N", 'N'), O("O", 'O'), P("P", 'P'),
    Q("Q", 'Q'), R("R", 'R'), S("S", 'S'), T("T", 'T'),
    U("U", 'U'), V("V", 'V'), W("W", 'W'), X("X", 'X'),
    Y("Y", 'Y'), Z("Z", 'Z')
}

private enum class GlobalSearchFilter(val label: String, val icon: String) {
    All("Todo", "🔎"),
    Plants("Plantas", "🌿"),
    Compounds("Compuestos", "🧪"),
    Families("Familias", "📚")
}

private data class GlobalSearchResultSet(
    val plants: List<PlantEntity>,
    val compounds: List<CompoundEntity>,
    val families: List<String>,
    val usedFuzzy: Boolean
)

// ── Funciones de búsqueda actualizadas ─────────────────────────────────

private fun plantGlobalExactSearchScore(
    plant: PlantEntity,
    query: SearchQuery,
    nameMode: GlobalNameSearchMode
): Int {
    val q = query.normalized
    if (q.length < 2) return 0

    var score = 0
    when (nameMode) {
        GlobalNameSearchMode.All -> {
            score = maxOf(score, exactFieldScore(plant.commonName, q, 12_000))
            score = maxOf(score, exactFieldScore(plant.scientificName, q, 11_500))
            score = maxOf(score, exactFieldScore(plant.commonNames, q, 10_500))
        }

        GlobalNameSearchMode.CommonName -> {
            score = maxOf(score, exactFieldScore(plant.commonName, q, 14_000))
            score = maxOf(score, exactFieldScore(plant.commonNames, q, 12_000))
            score = maxOf(score, exactFieldScore(plant.scientificName, q, 7_000))
        }

        GlobalNameSearchMode.ScientificName -> {
            score = maxOf(score, exactFieldScore(plant.scientificName, q, 14_000))
            score = maxOf(score, exactFieldScore(plant.commonName, q, 7_000))
            score = maxOf(score, exactFieldScore(plant.commonNames, q, 6_000))
        }
    }

    score = maxOf(score, exactFieldScore(plant.family, q, 7_000))
    score = maxOf(score, exactFieldScore(plant.category, q, 5_000))

    val nameText =
        listOf(plant.commonName, plant.commonNames, plant.scientificName).joinToString(" ")
            .normalizeForSearch()
    if (score == 0 && allTokensMatchCheap(nameText, query.tokens)) {
        score = 9_000 + query.tokens.size * 200
    }

    score = maxOf(score, exactFieldScore(plant.symptoms, q, 3_500))
    score = maxOf(score, exactFieldScore(plant.toxicParts, q, 3_000))
    score = maxOf(score, exactFieldScore(plant.description, q, 2_000))
    score = maxOf(score, exactFieldScore(plant.firstAid, q, 1_600))
    score = maxOf(score, exactFieldScore(plant.habitat, q, 1_200))
    score = maxOf(score, exactFieldScore(plant.geographicDistribution, q, 1_000))
    score = maxOf(score, exactFieldScore(plant.mythsAndLegends, q, 800))

    return score
}

private fun plantGlobalFuzzyFallbackScore(
    plant: PlantEntity,
    query: SearchQuery,
    nameMode: GlobalNameSearchMode
): Int {
    return when (nameMode) {
        GlobalNameSearchMode.All -> maxOf(
            fuzzyTextScore(plant.commonName, query) * 80,
            fuzzyTextScore(plant.scientificName, query) * 80,
            fuzzyTextScore(plant.commonNames, query) * 60,
            fuzzyTextScore(plant.family, query) * 35
        )

        GlobalNameSearchMode.CommonName -> maxOf(
            fuzzyTextScore(plant.commonName, query) * 100,
            fuzzyTextScore(plant.commonNames, query) * 85,
            fuzzyTextScore(plant.scientificName, query) * 40,
            fuzzyTextScore(plant.family, query) * 20
        )

        GlobalNameSearchMode.ScientificName -> maxOf(
            fuzzyTextScore(plant.scientificName, query) * 100,
            fuzzyTextScore(plant.commonName, query) * 40,
            fuzzyTextScore(plant.commonNames, query) * 35,
            fuzzyTextScore(plant.family, query) * 20
        )
    }
}

private fun compoundGlobalExactSearchScore(compound: CompoundEntity, query: SearchQuery): Int {
    val q = query.normalized
    if (q.length < 2) return 0

    var score = 0
    score = maxOf(score, exactFieldScore(compound.commonName, q, 11_000))
    score = maxOf(score, exactFieldScore(compound.iupacName, q, 9_500))
    score = maxOf(score, exactFieldScore(compound.groupName, q, 8_000))
    score = maxOf(score, exactFieldScore(compound.subgroup, q, 7_000))
    score = maxOf(score, exactFieldScore(compound.sourcePlants, q, 5_500))
    score = maxOf(score, exactFieldScore(compound.mechanism, q, 3_000))
    score = maxOf(score, exactFieldScore(compound.clinicalNeuro, q, 2_500))
    score = maxOf(score, exactFieldScore(compound.clinicalCardio, q, 2_500))
    score = maxOf(score, exactFieldScore(compound.clinicalDigestive, q, 2_500))
    score = maxOf(score, exactFieldScore(compound.clinicalRespiratory, q, 2_500))
    score = maxOf(score, exactFieldScore(compound.clinicalDermal, q, 2_500))
    score = maxOf(score, exactFieldScore(compound.clinicalOther, q, 2_000))

    return score
}

private fun compoundGlobalFuzzyFallbackScore(compound: CompoundEntity, query: SearchQuery): Int {
    return maxOf(
        fuzzyTextScore(compound.commonName, query) * 75,
        fuzzyTextScore(compound.iupacName, query) * 55,
        fuzzyTextScore(compound.groupName, query) * 45,
        fuzzyTextScore(compound.subgroup, query) * 35
    )
}

private fun familyGlobalExactSearchScore(family: String, query: SearchQuery): Int =
    exactFieldScore(family, query.normalized, 7_000)

private fun familyGlobalFuzzyFallbackScore(family: String, query: SearchQuery): Int =
    fuzzyTextScore(family, query) * 35

private fun exactFieldScore(field: String, normalizedQuery: String, base: Int): Int {
    if (field.isBlank()) return 0
    val normalizedField = field.normalizeForSearch()
    if (normalizedField.isBlank()) return 0

    return when {
        normalizedField == normalizedQuery -> base + 1_500
        normalizedField.startsWith(normalizedQuery) -> base + 1_000
        normalizedField.contains(normalizedQuery) -> base + 500
        else -> 0
    }
}

private fun allTokensMatchCheap(normalizedField: String, tokens: List<String>): Boolean {
    if (tokens.isEmpty() || normalizedField.isBlank()) return false
    val fieldTokens = normalizedField.searchTokens()
    return tokens.all { token ->
        normalizedField.contains(token) || fieldTokens.any { it.startsWith(token) }
    }
}

@Composable
private fun GlobalSelectablePlantResultCard(
    plant: PlantEntity,
    onClick: () -> Unit,
    query: String = "",
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onSelectionChange: (Boolean) -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    val toxicityColor = when (plant.toxicityLevel) {
        "Mortal" -> colors.error
        "Alto" -> Color(0xFFE65100)
        "Muy alto" -> Color(0xFFFF5722)
        "Moderado" -> Color(0xFFF57C00)
        "Bajo" -> colors.primary
        else -> colors.onSurfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (selectionMode) onSelectionChange(!selected) else onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (selected) colors.primaryContainer.copy(alpha = 0.55f) else colors.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 6.dp else 1.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                Checkbox(checked = selected, onCheckedChange = { onSelectionChange(it) })
                Spacer(Modifier.width(6.dp))
            }
            Text(
                when (plant.toxicityLevel) {
                    "Mortal" -> "💀"
                    "Muy alto" -> "☠️"
                    "Alto" -> "⚠️"
                    "Moderado" -> "⚡"
                    "Bajo" -> "🟢"
                    else -> "ℹ️"
                },
                fontSize = 18.sp,
                modifier = Modifier.width(28.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        plant.commonName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        color = toxicityColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            plant.toxicityLevel,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            fontSize = 9.sp,
                            color = toxicityColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    plant.scientificName,
                    color = colors.onSurfaceVariant,
                    fontStyle = FontStyle.Italic,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 11.sp
                )
                if (plant.symptoms.isNotBlank()) {
                    val q = query.trim()
                    if (q.isNotBlank()) {
                        val symptomText = plant.symptoms
                        val idx = symptomText.indexOf(q, ignoreCase = true)
                        if (idx >= 0) {
                            val start = maxOf(0, idx - 15)
                            val end = minOf(symptomText.length, idx + q.length + 40)
                            val prefix = if (start > 0) "…" else ""
                            val suffix = if (end < symptomText.length) "…" else ""
                            val snippet = prefix + symptomText.substring(start, end) + suffix
                            Row {
                                val snipIdx = snippet.indexOf(q, ignoreCase = true)
                                if (snipIdx >= 0) {
                                    Text(
                                        snippet.substring(0, snipIdx),
                                        fontSize = 11.sp,
                                        color = Color(0xFF888888),
                                        maxLines = 1
                                    )
                                    Text(
                                        snippet.substring(snipIdx, snipIdx + q.length),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.tertiary,
                                        maxLines = 1
                                    )
                                    Text(
                                        snippet.substring(snipIdx + q.length),
                                        fontSize = 11.sp,
                                        color = Color(0xFF888888),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                } else {
                                    Text(
                                        snippet,
                                        fontSize = 11.sp,
                                        color = Color(0xFF888888),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        } else {
                            Text(
                                plant.symptoms,
                                color = Color(0xFF888888),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 11.sp
                            )
                        }
                    } else {
                        Text(
                            plant.symptoms,
                            color = Color(0xFF888888),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 11.sp
                        )
                    }
                }
            }
            if (selectionMode) {
                Text(
                    if (selected) "✓" else "",
                    color = colors.primary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private data class GlobalBulkEditField(val id: String, val label: String, val hint: String)

private val GLOBAL_BULK_EDIT_FIELDS = listOf(
    GlobalBulkEditField("commonNames", "Otros nombres comunes", "Ej: belladona, tabaco borde"),
    GlobalBulkEditField("family", "Familia", "Ej: Solanaceae"),
    GlobalBulkEditField("toxicityLevel", "Nivel de toxicidad", "Ej: Alto, Mortal..."),
    GlobalBulkEditField("toxicParts", "Partes tóxicas", "Ej: hojas; semillas; raíz"),
    GlobalBulkEditField("symptoms", "Síntomas", "Ej: náuseas, vómitos, arritmias"),
    GlobalBulkEditField("description", "Descripción", "Descripción común para las fichas"),
    GlobalBulkEditField("category", "Categoría", "Ej: Ornamental"),
    GlobalBulkEditField("habitat", "Hábitat", "Ej: bosques húmedos"),
    GlobalBulkEditField("geographicDistribution", "Distribución", "Ej: Mediterráneo"),
    GlobalBulkEditField("firstAid", "Primeros auxilios", "Texto de primeros auxilios"),
    GlobalBulkEditField("floweringMonths", "Meses de floración", "Ej: 3,4,5,6"),
    GlobalBulkEditField("fruitingMonths", "Meses de fructificación", "Ej: 8,9,10"),
    GlobalBulkEditField("maxToxicityMonths", "Meses máxima toxicidad", "Ej: 6,7,8"),
    GlobalBulkEditField("notes", "Notas", "Nota común para las fichas"),
    GlobalBulkEditField("mythsAndLegends", "Mitos y curiosidades", "Texto cultural común")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GlobalBulkEditPlantsDialog(
    selectedCount: Int,
    onDismiss: () -> Unit,
    onConfirm: (GlobalBulkEditField, String, Boolean) -> Unit
) {
    var selectedField by remember { mutableStateOf(GLOBAL_BULK_EDIT_FIELDS.first()) }
    var value by remember { mutableStateOf("") }
    var append by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar $selectedCount plantas") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Elige el campo y el dato que quieres aplicar a todas las plantas marcadas en la búsqueda global.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedField.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Campo") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        GLOBAL_BULK_EDIT_FIELDS.forEach { field ->
                            DropdownMenuItem(
                                text = { Text(field.label) },
                                onClick = {
                                    selectedField = field
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Dato a aplicar") },
                    placeholder = { Text(selectedField.hint) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Añadir sin borrar lo anterior", fontWeight = FontWeight.Medium)
                        Text(
                            if (append) "Se agrega al final si no existe" else "Se reemplaza el campo completo",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = append, onCheckedChange = { append = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = value.isNotBlank(),
                onClick = { onConfirm(selectedField, value.trim(), append) }
            ) { Text("Aplicar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
