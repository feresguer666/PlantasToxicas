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

    // ── Resultados filtrados ──────────────────────────────────────
    // Importante: el fuzzy search se calcula en Dispatchers.Default y con debounce.
    // Así no bloquea el hilo principal de Compose ni provoca ANR al escribir.
    val searchQuery = remember(query) { buildSearchQuery(query) }
    var plantResults by remember { mutableStateOf<List<PlantEntity>>(emptyList()) }
    var compoundResults by remember { mutableStateOf<List<CompoundEntity>>(emptyList()) }
    var familyResults by remember { mutableStateOf<List<String>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    LaunchedEffect(searchQuery, allPlants, allCompounds) {
        if (searchQuery.normalized.length < 2) {
            plantResults = emptyList()
            compoundResults = emptyList()
            familyResults = emptyList()
            isSearching = false
            return@LaunchedEffect
        }

        isSearching = true
        delay(250)
        val plantsSnapshot = allPlants
        val compoundsSnapshot = allCompounds

        val result = withContext(Dispatchers.Default) {
            val plants = plantsSnapshot
                .mapNotNull { plant ->
                    currentCoroutineContext().ensureActive()
                    val score = plantGlobalSearchScore(plant, searchQuery)
                    if (score > 0) score to plant else null
                }
                .sortedByDescending { it.first }
                .take(250)
                .map { it.second }

            val compounds = compoundsSnapshot
                .mapNotNull { compound ->
                    currentCoroutineContext().ensureActive()
                    val score = compoundGlobalSearchScore(compound, searchQuery)
                    if (score > 0) score to compound else null
                }
                .sortedByDescending { it.first }
                .take(120)
                .map { it.second }

            val families = plantsSnapshot.map { it.family }.filter { it.isNotBlank() }.distinct()
                .mapNotNull { family ->
                    val score = fuzzyTextScore(family, searchQuery)
                    if (score > 0) score to family else null
                }
                .sortedByDescending { it.first }
                .take(80)
                .map { it.second }

            GlobalSearchResultSet(plants, compounds, families)
        }

        plantResults = result.plants
        compoundResults = result.compounds
        familyResults = result.families
        isSearching = false
    }

    val showPlants = selectedFilter == GlobalSearchFilter.All || selectedFilter == GlobalSearchFilter.Plants
    val showCompounds = selectedFilter == GlobalSearchFilter.All || selectedFilter == GlobalSearchFilter.Compounds
    val showFamilies = selectedFilter == GlobalSearchFilter.All || selectedFilter == GlobalSearchFilter.Families

    val visiblePlantResults = if (showPlants) plantResults else emptyList()
    val visibleCompoundResults = if (showCompounds) compoundResults else emptyList()
    val visibleFamilyResults = if (showFamilies) familyResults else emptyList()
    val totalResults = visiblePlantResults.size + visibleCompoundResults.size + visibleFamilyResults.size

    Column(modifier = Modifier.fillMaxSize()) {
        // ── TopBar ───────────────────────────────────────────────
        Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSecondaryContainer) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.Black)
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Buscar plantas, compuestos, síntomas…", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Filled.Search, null, tint = Color.Black.copy(alpha = 0.7f)) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Filled.Clear, "Limpiar", tint = Color.Black.copy(alpha = 0.7f))
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        cursorColor = Color.Black,
                        focusedBorderColor = Color.Black.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.Black.copy(alpha = 0.3f),
                        focusedPlaceholderColor = Color.Black.copy(alpha = 0.5f),
                        unfocusedPlaceholderColor = Color.Black.copy(alpha = 0.5f),
                    ),
                    shape = RoundedCornerShape(24.dp),
                )
                Spacer(Modifier.width(4.dp))
            }
        }

        // ── Filtros ──────────────────────────────────────────────
        Surface(color = colors.surface, modifier = Modifier.fillMaxWidth()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(GlobalSearchFilter.entries) { filter ->
                    val count = when (filter) {
                        GlobalSearchFilter.All -> plantResults.size + compoundResults.size + familyResults.size
                        GlobalSearchFilter.Plants -> plantResults.size
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
            Surface(color = colors.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (isSearching) "🔎 Buscando…"
                    else "📋 $totalResults resultados · filtro: ${selectedFilter.label} · \"$query\"",
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
                    Text("Escribe al menos 2 caracteres", color = colors.onSurfaceVariant, fontSize = 14.sp)
                    Text("o usa el micrófono para buscar por voz", color = colors.onSurfaceVariant.copy(alpha = 0.6f), fontSize = 12.sp)
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
                // ── Familias ─────────────────────────────────────
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
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1565C0).copy(alpha = 0.08f)),
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

                // ── Plantas ──────────────────────────────────────
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
                        CompactPlantCard(
                            plant = plant,
                            query = query,
                            onClick = { onPlantClick(plant) }
                        )
                    }
                }

                // ── Compuestos ───────────────────────────────────
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
                            onClick = { onCompoundClick(compound) }
                        )
                    }
                }
            }
        }
    }
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
    val families: List<String>
)

private fun plantGlobalSearchScore(plant: PlantEntity, query: SearchQuery): Int {
    val q = query.normalized
    if (q.length < 2) return 0

    // Primero: coincidencias exactas/por prefijo en campos importantes.
    // Es mucho más fiable y rápido que buscar con fuzzy en todos los textos largos.
    var score = 0
    score = maxOf(score, exactFieldScore(plant.commonName, q, 12_000))
    score = maxOf(score, exactFieldScore(plant.scientificName, q, 11_500))
    score = maxOf(score, exactFieldScore(plant.commonNames, q, 10_500))
    score = maxOf(score, exactFieldScore(plant.family, q, 7_000))
    score = maxOf(score, exactFieldScore(plant.category, q, 5_000))

    // Multi-palabra en nombres: "atropa bella" o "nerium oleander".
    val nameText = listOf(plant.commonName, plant.commonNames, plant.scientificName)
        .joinToString(" ")
        .normalizeForSearch()
    if (score == 0 && allTokensMatchCheap(nameText, query.tokens)) {
        score = 9_000 + query.tokens.size * 200
    }

    // Tolerancia a errores SOLO en campos cortos de nombre. Así "beladona"
    // sigue encontrando "belladonna" sin bloquear al buscar en descripciones largas.
    if (score == 0) {
        score = maxOf(
            fuzzyTextScore(plant.commonName, query) * 80,
            fuzzyTextScore(plant.scientificName, query) * 80,
            fuzzyTextScore(plant.commonNames, query) * 60,
            fuzzyTextScore(plant.family, query) * 35
        )
    }

    // Después: coincidencias exactas en textos clínicos/descriptivos.
    // Menos peso para que un nombre bien escrito siempre aparezca arriba.
    score = maxOf(score, exactFieldScore(plant.symptoms, q, 3_500))
    score = maxOf(score, exactFieldScore(plant.toxicParts, q, 3_000))
    score = maxOf(score, exactFieldScore(plant.description, q, 2_000))
    score = maxOf(score, exactFieldScore(plant.firstAid, q, 1_600))
    score = maxOf(score, exactFieldScore(plant.habitat, q, 1_200))
    score = maxOf(score, exactFieldScore(plant.geographicDistribution, q, 1_000))
    score = maxOf(score, exactFieldScore(plant.mythsAndLegends, q, 800))

    return score
}

private fun compoundGlobalSearchScore(compound: CompoundEntity, query: SearchQuery): Int {
    val q = query.normalized
    if (q.length < 2) return 0

    var score = 0
    score = maxOf(score, exactFieldScore(compound.commonName, q, 11_000))
    score = maxOf(score, exactFieldScore(compound.iupacName, q, 9_500))
    score = maxOf(score, exactFieldScore(compound.groupName, q, 8_000))
    score = maxOf(score, exactFieldScore(compound.subgroup, q, 7_000))
    score = maxOf(score, exactFieldScore(compound.sourcePlants, q, 5_500))

    if (score == 0) {
        score = maxOf(
            fuzzyTextScore(compound.commonName, query) * 75,
            fuzzyTextScore(compound.iupacName, query) * 55,
            fuzzyTextScore(compound.groupName, query) * 45,
            fuzzyTextScore(compound.subgroup, query) * 35
        )
    }

    score = maxOf(score, exactFieldScore(compound.mechanism, q, 3_000))
    score = maxOf(score, exactFieldScore(compound.clinicalNeuro, q, 2_500))
    score = maxOf(score, exactFieldScore(compound.clinicalCardio, q, 2_500))
    score = maxOf(score, exactFieldScore(compound.clinicalDigestive, q, 2_500))
    score = maxOf(score, exactFieldScore(compound.clinicalRespiratory, q, 2_500))
    score = maxOf(score, exactFieldScore(compound.clinicalDermal, q, 2_500))
    score = maxOf(score, exactFieldScore(compound.clinicalOther, q, 2_000))

    return score
}

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
