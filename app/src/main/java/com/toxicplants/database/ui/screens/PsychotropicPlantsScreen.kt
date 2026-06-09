package com.toxicplants.database.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.content.Context
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toxicplants.database.CompoundEntity
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.PsychotropicOverrideItem
import com.toxicplants.database.PsychotropicOverrides
import com.toxicplants.database.PsychotropicUserStore
import com.toxicplants.database.ui.viewmodel.CompoundViewModel
import com.toxicplants.database.ui.viewmodel.PlantViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer

private const val CAT_ALL = "todas"
private const val CAT_HALLUCINOGENS = "alucinogenos"
private const val CAT_IMAO = "imao"
private const val CAT_DEPRESSANTS = "depresores"
private const val CAT_STIMULANTS = "estimulantes"
private const val CAT_TROPANES = "tropanicos"

private data class PsychotropicCategoryInfo(
    val id: String,
    val label: String,
    val icon: String,
    val subtitle: String,
    val color: Color,
)

private data class PsychotropicPlantItem(
    val plant: PlantEntity,
    val categories: List<String>,
    val compounds: List<String>,
    val reasons: List<String>,
    val score: Int,
    val searchText: String,
)

private data class PsychotropicCompoundMatch(
    val compound: CompoundEntity,
    val categories: Set<String>,
)

private data class PsychotropicCompoundIndex(
    val exactByTaxon: Map<String, List<PsychotropicCompoundMatch>>,
    val genericByGenus: Map<String, List<PsychotropicCompoundMatch>>,
)

private data class AssetCatalogResult(
    val plants: List<PlantEntity>,
    val compounds: List<CompoundEntity>,
    val message: String,
)

private data class PrecomputedPsychotropicCatalogResult(
    val items: List<PsychotropicPlantItem>,
    val message: String,
)

private val psychotropicCategories = listOf(
    PsychotropicCategoryInfo(
        id = CAT_ALL,
        label = "Todas",
        icon = "🧠",
        subtitle = "Catálogo psicotrópico completo",
        color = Color(0xFF455A64),
    ),
    PsychotropicCategoryInfo(
        id = CAT_HALLUCINOGENS,
        label = "Alucinógenos",
        icon = "🌈",
        subtitle = "DMT, mescalina, LSA, cannabinoides…",
        color = Color(0xFF7B1FA2),
    ),
    PsychotropicCategoryInfo(
        id = CAT_IMAO,
        label = "IMAO",
        icon = "🧬",
        subtitle = "Harmina, harmalina y beta-carbolinas",
        color = Color(0xFF00897B),
    ),
    PsychotropicCategoryInfo(
        id = CAT_DEPRESSANTS,
        label = "Depresores",
        icon = "🌙",
        subtitle = "Opiáceos, sedantes y acción GABA/opioide",
        color = Color(0xFF3949AB),
    ),
    PsychotropicCategoryInfo(
        id = CAT_STIMULANTS,
        label = "Estimulantes",
        icon = "⚡",
        subtitle = "Cafeína, nicotina, efedrina, xantinas…",
        color = Color(0xFFEF6C00),
    ),
    PsychotropicCategoryInfo(
        id = CAT_TROPANES,
        label = "Tropánicos",
        icon = "🎭",
        subtitle = "Atropina, escopolamina, hiosciamina",
        color = Color(0xFFC62828),
    ),
)

private val categoryById = psychotropicCategories.associateBy { it.id }
private val categoryOrder = psychotropicCategories.map { it.id }
private val riskFilters = listOf("Todos", "Mortal", "Alto/Muy alto", "Moderado/Bajo")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PsychotropicPlantsScreen(
    plantViewModel: PlantViewModel,
    compoundViewModel: CompoundViewModel,
    onPlantClick: (PlantEntity) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val livePlants by plantViewModel.allPlants.observeAsState(emptyList())
    val cachedPlants by plantViewModel.plantsData.collectAsState()
    val roomPlants = if (livePlants.isNotEmpty()) livePlants else cachedPlants

    var userOverrides by remember { mutableStateOf(PsychotropicUserStore.load(context)) }
    var itemToEdit by remember { mutableStateOf<PsychotropicPlantItem?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<PsychotropicPlantItem?>(null) }

    fun saveUserOverrides(next: PsychotropicOverrides) {
        userOverrides = next
        PsychotropicUserStore.save(context, next)
    }

    var precomputedItems by remember { mutableStateOf(buildSimplePsychotropicFallback(fallbackPsychotropicSeedPlants())) }
    var assetLoadMessage by remember { mutableStateOf("Catálogo psicotrópico precalculado: fallback inicial") }
    var isLoadingPrecomputed by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val appContext = context.applicationContext
        runCatching {
            withContext(Dispatchers.IO) {
                loadPrecomputedPsychotropicCatalog(appContext)
            }
        }.onSuccess { result ->
            precomputedItems = result.items.ifEmpty { buildSimplePsychotropicFallback(fallbackPsychotropicSeedPlants()) }
            assetLoadMessage = result.message
        }.onFailure { precomputedError ->
            // Si el índice precalculado no está empaquetado en la APK, generamos la lista
            // desde los assets antiguos como plan B. Es más lento, pero evita quedarse en 34.
            runCatching {
                withContext(Dispatchers.Default) {
                    val catalog = loadCatalogDirectlyFromAssets(appContext)
                    val built = buildPsychotropicItems(catalog.plants, catalog.compounds)
                    PrecomputedPsychotropicCatalogResult(
                        items = built.ifEmpty { buildSimplePsychotropicFallback(fallbackPsychotropicSeedPlants()) },
                        message = "Índice precalculado no disponible; generado desde assets: ${catalog.message}"
                    )
                }
            }.onSuccess { generated ->
                precomputedItems = generated.items
                assetLoadMessage = generated.message
            }.onFailure { e ->
                precomputedItems = buildSimplePsychotropicFallback(fallbackPsychotropicSeedPlants())
                assetLoadMessage = "Fallback interno activo: ${(e.message ?: precomputedError.message) ?: e::class.java.simpleName}"
            }
        }
        isLoadingPrecomputed = false
    }

    val roomPlantMap = remember(roomPlants) { roomPlants.associateBy { it.id } }
    val rawItemsForDisplay = remember(precomputedItems, roomPlantMap, userOverrides) {
        val hiddenIds = userOverrides.hiddenPlantIds.toHashSet()
        val customById = userOverrides.customItems.associateBy { it.plant.id }
        val baseItems = precomputedItems.filter { item ->
            item.plant.id !in hiddenIds && item.plant.id !in customById
        }
        val customItems = customById.values.map { it.toPsychotropicPlantItem() }

        (baseItems + customItems).map { item ->
            val roomPlant = roomPlantMap[item.plant.id]
            if (roomPlant != null) {
                val searchText = buildPsychotropicSearchText(roomPlant, item.categories, item.compounds, item.reasons)
                item.copy(plant = roomPlant, searchText = searchText)
            } else item
        }
    }
    val isPreparing = isLoadingPrecomputed && rawItemsForDisplay.isEmpty()

    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(CAT_ALL) }
    var selectedRisk by remember { mutableStateOf("Todos") }
    var collapseDuplicates by remember { mutableStateOf(true) }

    val allItems = remember(rawItemsForDisplay, collapseDuplicates) {
        val base = if (collapseDuplicates) collapseDuplicateTaxa(rawItemsForDisplay) else rawItemsForDisplay
        base.sortedWith(
            compareByDescending<PsychotropicPlantItem> { toxicityWeight(it.plant.toxicityLevel) }
                .thenByDescending { it.score }
                .thenBy { it.plant.scientificName.normalizedForSearch() }
        )
    }

    val filteredItems = remember(allItems, query, selectedCategory, selectedRisk) {
        val q = query.normalizedForSearch()
        allItems.filter { item ->
            val matchesCategory = selectedCategory == CAT_ALL || item.categories.contains(selectedCategory)
            val matchesRisk = matchesRiskFilter(item.plant.toxicityLevel, selectedRisk)
            val matchesQuery = q.isBlank() || item.searchText.contains(q)
            matchesCategory && matchesRisk && matchesQuery
        }
    }

    val categoryCounts = remember(allItems) {
        psychotropicCategories.associate { category ->
            category.id to if (category.id == CAT_ALL) allItems.size else allItems.count { it.categories.contains(category.id) }
        }
    }

    if (showAddDialog || itemToEdit != null) {
        PsychotropicEditDialog(
            initialItem = itemToEdit,
            availablePlants = roomPlants,
            onDismiss = {
                showAddDialog = false
                itemToEdit = null
            },
            onSave = { overrideItem ->
                val editedId = overrideItem.plant.id
                val nextCustom = userOverrides.customItems
                    .filterNot { it.plant.id == editedId } + overrideItem
                val nextHidden = userOverrides.hiddenPlantIds.filterNot { it == editedId }
                saveUserOverrides(
                    userOverrides.copy(
                        hiddenPlantIds = nextHidden.distinct(),
                        customItems = nextCustom
                    )
                )
                showAddDialog = false
                itemToEdit = null
            }
        )
    }

    itemToDelete?.let { deleteTarget ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("¿Eliminar de psicotrópicas?") },
            text = { Text("Se ocultará ${deleteTarget.plant.commonName} de esta sección. La ficha de la planta no se borra.") },
            confirmButton = {
                TextButton(onClick = {
                    val id = deleteTarget.plant.id
                    saveUserOverrides(
                        userOverrides.copy(
                            hiddenPlantIds = (userOverrides.hiddenPlantIds + id).distinct(),
                            customItems = userOverrides.customItems.filterNot { it.plant.id == id }
                        )
                    )
                    itemToDelete = null
                }) { Text("Eliminar", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("🧠 Plantas psicotrópicas", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(
                            "Alucinógenos · IMAO · Depresores · Estimulantes · Tropánicos",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.82f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Añadir psicotrópica", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF263238),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF07110A), Color(0xFF0D1B12), Color(0xFF141422))
                    )
                ),
            contentPadding = PaddingValues(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                PsychotropicSearchBox(
                    query = query,
                    onQueryChange = { query = it },
                    resultCount = filteredItems.size,
                    totalCount = allItems.size,
                )
            }

            item {
                PsychotropicCategoryDashboard(
                    selectedCategory = selectedCategory,
                    counts = categoryCounts,
                    onCategorySelected = { selectedCategory = it },
                )
            }

            item {
                PsychotropicFilters(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it },
                    selectedRisk = selectedRisk,
                    onRiskSelected = { selectedRisk = it },
                    collapseDuplicates = collapseDuplicates,
                    onCollapseDuplicatesChange = { collapseDuplicates = it },
                )
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF101E14),
                    shadowElevation = 2.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        val label = categoryById[selectedCategory]?.label ?: "Todas"
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isLoadingPrecomputed) "Cargando índice… ${filteredItems.size}/${allItems.size}" else "${filteredItems.size} resultado${if (filteredItems.size == 1) "" else "s"}",
                                color = Color(0xFFB9F6CA),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "· $label · $selectedRisk",
                                color = Color.White.copy(alpha = 0.72f),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Text(
                            text = "Índice precalculado: ${rawItemsForDisplay.size} plantas · Room: ${roomPlants.size} · ${allItems.size} visibles",
                            color = Color.White.copy(alpha = 0.52f),
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = assetLoadMessage,
                            color = Color(0xFFFFE0B2).copy(alpha = 0.78f),
                            fontSize = 10.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            when {
                isPreparing -> item {
                    Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                        EmptyPsychotropicState(
                            icon = "🧠",
                            title = "Preparando buscador…",
                            subtitle = "Clasificando plantas y compuestos en segundo plano.",
                        )
                    }
                }
                filteredItems.isEmpty() -> item {
                    Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                        EmptyPsychotropicState(
                            icon = "🔎",
                            title = "Sin resultados",
                            subtitle = "Prueba con otro nombre, compuesto, familia o categoría.",
                        )
                    }
                }
                else -> items(filteredItems, key = { it.plant.id }) { item ->
                    PsychotropicPlantCard(
                        item = item,
                        onClick = { onPlantClick(item.plant) },
                        onEdit = { itemToEdit = item },
                        onDelete = { itemToDelete = item }
                    )
                }
            }
        }
    }
}


@Composable
private fun PsychotropicEditDialog(
    initialItem: PsychotropicPlantItem?,
    availablePlants: List<PlantEntity>,
    onDismiss: () -> Unit,
    onSave: (PsychotropicOverrideItem) -> Unit,
) {
    var plantQuery by remember(initialItem) { mutableStateOf("") }
    var selectedPlant by remember(initialItem) { mutableStateOf(initialItem?.plant) }
    var selectedCategories by remember(initialItem) { mutableStateOf(initialItem?.categories?.toSet() ?: emptySet()) }
    var compoundsText by remember(initialItem) { mutableStateOf(initialItem?.compounds?.joinToString(", ") ?: "") }
    var reasonText by remember(initialItem) {
        mutableStateOf(initialItem?.reasons?.joinToString("; ") ?: "Editado por usuario")
    }

    val plantMatches = remember(availablePlants, plantQuery, selectedPlant) {
        val q = plantQuery.normalizedForSearch()
        if (selectedPlant != null) emptyList()
        else if (q.length < 2) availablePlants.take(25)
        else availablePlants.filter { plant ->
            plant.fullSearchText().contains(q)
        }.take(40)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialItem == null) "Añadir psicotrópica" else "Editar psicotrópica") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().height(520.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text("Planta", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    if (selectedPlant == null) {
                        OutlinedTextField(
                            value = plantQuery,
                            onValueChange = { plantQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("Buscar planta existente…") }
                        )
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF1B5E20).copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(selectedPlant!!.commonName, fontWeight = FontWeight.Bold)
                                    Text(selectedPlant!!.scientificName, fontStyle = FontStyle.Italic, fontSize = 12.sp, color = Color.Gray)
                                }
                                if (initialItem == null) {
                                    TextButton(onClick = { selectedPlant = null }) { Text("Cambiar") }
                                }
                            }
                        }
                    }
                }

                if (selectedPlant == null) {
                    if (availablePlants.isEmpty()) {
                        item {
                            Text(
                                "La base Room aún no tiene plantas cargadas. Entra primero al catálogo o espera a que termine la carga inicial.",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        items(plantMatches, key = { it.id }) { plant ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { selectedPlant = plant },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(Modifier.padding(10.dp)) {
                                    Text(plant.commonName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(plant.scientificName, fontStyle = FontStyle.Italic, fontSize = 12.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }

                item {
                    Text("Categorías", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        psychotropicCategories.drop(1).forEach { category ->
                            val selected = category.id in selectedCategories
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    selectedCategories = if (selected) selectedCategories - category.id
                                    else selectedCategories + category.id
                                },
                                label = { Text("${category.icon} ${category.label}", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = category.color,
                                    selectedLabelColor = Color.White,
                                    containerColor = category.color.copy(alpha = 0.14f)
                                )
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = compoundsText,
                        onValueChange = { compoundsText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Compuestos (separados por coma)") },
                        minLines = 1,
                        maxLines = 3
                    )
                }

                item {
                    OutlinedTextField(
                        value = reasonText,
                        onValueChange = { reasonText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nota / motivo") },
                        minLines = 1,
                        maxLines = 3
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = selectedPlant != null && selectedCategories.isNotEmpty(),
                onClick = {
                    val plant = selectedPlant ?: return@Button
                    val cats = selectedCategories.sortedBy { categoryOrder.indexOf(it).let { idx -> if (idx < 0) 999 else idx } }
                    val compounds = compoundsText.split(',', ';', '|')
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()
                    val reasons = reasonText.split(';')
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .ifEmpty { listOf("Editado por usuario") }
                    onSave(
                        PsychotropicOverrideItem(
                            plant = plant,
                            categories = cats,
                            compounds = compounds,
                            reasons = reasons,
                            score = (initialItem?.score ?: 10_000) + 1
                        )
                    )
                }
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun PsychotropicSearchBox(
    query: String,
    onQueryChange: (String) -> Unit,
    resultCount: Int,
    totalCount: Int,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        placeholder = { Text("Buscar por planta, familia, síntoma o compuesto…") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = "Limpiar")
                }
            } else {
                Text(
                    "$resultCount/$totalCount",
                    modifier = Modifier.padding(end = 12.dp),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFFB9F6CA),
            unfocusedBorderColor = Color.White.copy(alpha = 0.35f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Color(0xFFB9F6CA),
            focusedContainerColor = Color.Black.copy(alpha = 0.24f),
            unfocusedContainerColor = Color.Black.copy(alpha = 0.18f),
            focusedLeadingIconColor = Color(0xFFB9F6CA),
            unfocusedLeadingIconColor = Color.White.copy(alpha = 0.7f),
            focusedTrailingIconColor = Color(0xFFB9F6CA),
            unfocusedTrailingIconColor = Color.White.copy(alpha = 0.7f),
        ),
    )
}

@Composable
private fun SafetyNoticeCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF3E2723)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Text(
            text = "⚠️ Información toxicológica y preventiva. No se muestran dosis, preparación ni pautas de consumo.",
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            color = Color(0xFFFFE0B2),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun PsychotropicCategoryDashboard(
    selectedCategory: String,
    counts: Map<String, Int>,
    onCategorySelected: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        psychotropicCategories.drop(1).forEach { category ->
            val selected = selectedCategory == category.id
            Card(
                onClick = { onCategorySelected(category.id) },
                modifier = Modifier.width(168.dp).height(92.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) category.color else category.color.copy(alpha = 0.42f),
                ),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(category.icon, fontSize = 22.sp)
                        Spacer(Modifier.width(7.dp))
                        Text(
                            category.label,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${counts[category.id] ?: 0} planta${if ((counts[category.id] ?: 0) == 1) "" else "s"}",
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        category.subtitle,
                        color = Color.White.copy(alpha = 0.78f),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun PsychotropicFilters(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    selectedRisk: String,
    onRiskSelected: (String) -> Unit,
    collapseDuplicates: Boolean,
    onCollapseDuplicatesChange: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            psychotropicCategories.forEach { category ->
                val selected = selectedCategory == category.id
                FilterChip(
                    selected = selected,
                    onClick = { onCategorySelected(category.id) },
                    label = { Text("${category.icon} ${category.label}", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = category.color,
                        selectedLabelColor = Color.White,
                        containerColor = category.color.copy(alpha = 0.16f),
                        labelColor = Color.White.copy(alpha = 0.86f),
                    ),
                )
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            riskFilters.forEach { risk ->
                val selected = selectedRisk == risk
                val color = when (risk) {
                    "Mortal" -> Color(0xFFD32F2F)
                    "Alto/Muy alto" -> Color(0xFFFF6D00)
                    "Moderado/Bajo" -> Color(0xFF43A047)
                    else -> Color(0xFF607D8B)
                }
                FilterChip(
                    selected = selected,
                    onClick = { onRiskSelected(risk) },
                    label = { Text(risk, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = color,
                        selectedLabelColor = Color.White,
                        containerColor = color.copy(alpha = 0.16f),
                        labelColor = Color.White.copy(alpha = 0.84f),
                    ),
                )
            }

            FilterChip(
                selected = collapseDuplicates,
                onClick = { onCollapseDuplicatesChange(!collapseDuplicates) },
                label = { Text(if (collapseDuplicates) "Duplicados: fusionados" else "Duplicados: visibles", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF00695C),
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFF00695C).copy(alpha = 0.16f),
                    labelColor = Color.White.copy(alpha = 0.84f),
                ),
            )
        }
    }
}

@Composable
private fun PsychotropicPlantCard(
    item: PsychotropicPlantItem,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val plant = item.plant
    val riskColor = toxicityColor(plant.toxicityLevel)

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111B16)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            PlantThumbnail(
                plant = plant,
                toxicityColor = riskColor,
                modifier = Modifier.size(62.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        plant.commonName.ifBlank { plant.scientificName },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Surface(color = riskColor.copy(alpha = 0.18f), shape = RoundedCornerShape(8.dp)) {
                        Text(
                            plant.toxicityLevel.ifBlank { "Sin nivel" },
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            color = riskColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = "Editar psicotrópica", tint = Color(0xFFB9F6CA), modifier = Modifier.size(17.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = "Eliminar de psicotrópicas", tint = Color(0xFFFF8A80), modifier = Modifier.size(17.dp))
                    }
                }

                Text(
                    plant.scientificName,
                    color = Color.White.copy(alpha = 0.72f),
                    fontStyle = FontStyle.Italic,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (plant.family.isNotBlank()) {
                    Text(
                        plant.family,
                        color = Color(0xFFB9F6CA).copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(Modifier.height(7.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    item.categories.forEach { categoryId ->
                        PsychotropicCategoryChip(categoryId)
                    }
                }

                if (item.compounds.isNotEmpty()) {
                    Spacer(Modifier.height(7.dp))
                    Text(
                        "Compuestos: ${item.compounds.take(4).joinToString(", ")}${if (item.compounds.size > 4) "…" else ""}",
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                val reason = item.reasons.firstOrNull().orEmpty()
                if (reason.isNotBlank()) {
                    Spacer(Modifier.height(5.dp))
                    Text(
                        reason,
                        color = Color.White.copy(alpha = 0.62f),
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun PsychotropicCategoryChip(categoryId: String) {
    val category = categoryById[categoryId] ?: return
    Surface(
        color = category.color.copy(alpha = 0.22f),
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = "${category.icon} ${category.label}",
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun EmptyPsychotropicState(
    icon: String,
    title: String,
    subtitle: String,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Text(icon, fontSize = 56.sp)
            Spacer(Modifier.height(12.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = Color.White.copy(alpha = 0.72f), fontSize = 13.sp)
        }
    }
}



private fun PsychotropicOverrideItem.toPsychotropicPlantItem(): PsychotropicPlantItem = PsychotropicPlantItem(
    plant = plant,
    categories = categories,
    compounds = compounds,
    reasons = reasons,
    score = score,
    searchText = buildPsychotropicSearchText(plant, categories, compounds, reasons),
)

private fun loadPrecomputedPsychotropicCatalog(context: Context): PrecomputedPsychotropicCatalogResult {
    val items = ArrayList<PsychotropicPlantItem>()
    val text = context.assets.open("psychotropic_plants.json")
        .bufferedReader(Charsets.UTF_8)
        .use { it.readText() }
    val arr = JSONArray(text)
    for (i in 0 until arr.length()) {
        val item = arr.getJSONObject(i).toPsychotropicPlantItemFromAsset()
        if (item.categories.isNotEmpty()) items += item
    }

    val finalItems = items.ifEmpty { buildSimplePsychotropicFallback(fallbackPsychotropicSeedPlants()) }
    return PrecomputedPsychotropicCatalogResult(
        items = finalItems,
        message = if (items.isNotEmpty()) {
            "psychotropic_plants.json cargado: ${items.size} plantas precalculadas"
        } else {
            "psychotropic_plants.json vacío; usando fallback interno"
        },
    )
}

private fun JSONObject.toPsychotropicPlantItemFromAsset(): PsychotropicPlantItem {
    val plantObject = optJSONObject("plant") ?: this
    val plant = plantObject.toPlantEntityFromAsset()
    val categories = optJSONArray("categories").toStringListCompat()
    val compounds = optJSONArray("compounds").toStringListCompat().take(8)
    val reasons = optJSONArray("reasons").toStringListCompat().take(4)
    val score = optInt("score", plant.description.length + plant.symptoms.length)
    val searchText = optString("searchText", "")
        .takeIf { it.isNotBlank() }
        ?: buildPsychotropicSearchText(plant, categories, compounds, reasons)

    return PsychotropicPlantItem(
        plant = plant,
        categories = categories,
        compounds = compounds,
        reasons = reasons,
        score = score,
        searchText = searchText.normalizedForSearch(),
    )
}

private fun JSONArray?.toStringListCompat(): List<String> {
    if (this == null) return emptyList()
    val out = ArrayList<String>(length())
    for (i in 0 until length()) {
        val value = optString(i, "").trim()
        if (value.isNotBlank()) out += value
    }
    return out.distinct()
}

private fun loadCatalogDirectlyFromAssets(context: Context): AssetCatalogResult {
    val assetManager = context.assets
    val rootAssets = assetManager.list("")?.toList().orEmpty()
    val listedPlantFiles = rootAssets
        .filter { Regex("""plants_\d+\.json""").matches(it) }
        .sortedBy { it.substringAfter("plants_").substringBefore(".json").toIntOrNull() ?: Int.MAX_VALUE }

    val plantFiles = listedPlantFiles.ifEmpty { (1..80).map { "plants_$it.json" } }
    val plants = ArrayList<PlantEntity>()
    var loadedPlantFiles = 0
    var missingInARow = 0
    var firstPlantError = ""

    for (fileName in plantFiles) {
        try {
            val text = assetManager.open(fileName).bufferedReader(Charsets.UTF_8).use { it.readText() }
            val arr = JSONArray(text)
            for (i in 0 until arr.length()) {
                plants += arr.getJSONObject(i).toPlantEntityFromAsset()
            }
            loadedPlantFiles++
            missingInARow = 0
        } catch (e: Exception) {
            if (firstPlantError.isBlank()) firstPlantError = "$fileName: ${e.message ?: e::class.java.simpleName}"
            if (listedPlantFiles.isEmpty()) {
                missingInARow++
                if (missingInARow >= 3 && plants.isNotEmpty()) break
                if (fileName == "plants_1.json") break
            }
        }
    }

    val compounds = ArrayList<CompoundEntity>()
    var compoundError = ""
    try {
        val text = assetManager.open("compounds.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        val arr = JSONArray(text)
        for (i in 0 until arr.length()) {
            compounds += arr.getJSONObject(i).toCompoundEntityFromAsset()
        }
    } catch (e: Exception) {
        compoundError = e.message ?: e::class.java.simpleName
    }

    val finalPlants = plants.ifEmpty { fallbackPsychotropicSeedPlants() }
    val usedFallback = plants.isEmpty() && finalPlants.isNotEmpty()
    val message = when {
        usedFallback -> "⚠️ Assets/Room a 0 (${rootAssets.size} assets, ${listedPlantFiles.size} plant json). Fallback interno activo. $firstPlantError"
        plants.isNotEmpty() -> "Assets OK: ${plants.size} plantas · ${compounds.size} compuestos · $loadedPlantFiles ficheros"
        else -> "No hay catálogo: ${rootAssets.size} assets. $firstPlantError $compoundError"
    }

    return AssetCatalogResult(
        plants = finalPlants,
        compounds = compounds,
        message = message,
    )
}

private fun JSONObject.toPlantEntityFromAsset(): PlantEntity = PlantEntity(
    id = optInt("id", 0),
    commonName = optString("commonName", ""),
    commonNames = optString("commonNames", ""),
    scientificName = optString("scientificName", ""),
    family = optString("family", ""),
    toxicityLevel = optString("toxicityLevel", ""),
    toxicParts = optString("toxicParts", ""),
    symptoms = optString("symptoms", ""),
    description = optString("description", ""),
    habitat = optString("habitat", ""),
    geographicDistribution = optString("geographicDistribution", ""),
    firstAid = optString("firstAid", ""),
    imageUrl = optString("imageUrl", ""),
    isFavorite = optBoolean("isFavorite", false),
    category = optString("category", ""),
    latitude = if (isNull("latitude")) null else optDouble("latitude"),
    longitude = if (isNull("longitude")) null else optDouble("longitude"),
    locationName = optStringOrNullCompat("locationName"),
    foundDate = optStringOrNullCompat("foundDate"),
    notes = optStringOrNullCompat("notes"),
    floweringMonths = optString("floweringMonths", ""),
    fruitingMonths = optString("fruitingMonths", ""),
    maxToxicityMonths = optString("maxToxicityMonths", ""),
)

private fun JSONObject.toCompoundEntityFromAsset(): CompoundEntity = CompoundEntity(
    id = optInt("id", 0),
    commonName = optString("commonName", ""),
    iupacName = optString("iupacName", ""),
    groupName = optString("groupName", ""),
    subgroup = optString("subgroup", ""),
    molecularFormula = optString("molecularFormula", ""),
    molecularWeight = optDoubleOrNullCompat("molecularWeight"),
    sourcePlants = optString("sourcePlants", ""),
    concentration = optString("concentration", ""),
    mechanism = optString("mechanism", ""),
    ld50 = optString("ld50", ""),
    toxicDose = optString("toxicDose", ""),
    clinicalNeuro = optString("clinicalNeuro", ""),
    clinicalCardio = optString("clinicalCardio", ""),
    clinicalDigestive = optString("clinicalDigestive", ""),
    clinicalRespiratory = optString("clinicalRespiratory", ""),
    clinicalDermal = optString("clinicalDermal", ""),
    clinicalOther = optString("clinicalOther", ""),
    onsetTime = optString("onsetTime", ""),
    duration = optString("duration", ""),
    treatment = optString("treatment", ""),
    notes = optString("notes", ""),
    groupColor = optString("groupColor", "#7B1FA2"),
    isFavorite = optBoolean("isFavorite", false),
    pubchemCid = optInt("pubchemCid", 0),
)

private fun JSONObject.optStringOrNullCompat(key: String): String? =
    if (isNull(key) || !has(key)) null else optString(key, "").takeIf { it.isNotBlank() }

private fun JSONObject.optDoubleOrNullCompat(key: String): Double? =
    if (isNull(key) || !has(key)) null else optDouble(key).takeIf { !it.isNaN() }

private fun fallbackPsychotropicSeedPlants(): List<PlantEntity> = listOf(
    seedPsychPlant(900001, "Belladona", "Atropa belladonna", "Solanaceae", "Mortal", "Todas las partes, bayas", "Atropina, hiosciamina y escopolamina. Síndrome anticolinérgico: midriasis, taquicardia, delirio, alucinaciones, convulsiones y coma.", "Planta tropánica clásica de alta toxicidad."),
    seedPsychPlant(900002, "Estramonio / Hierba del diablo", "Datura stramonium", "Solanaceae", "Mortal", "Toda la planta, especialmente semillas", "Atropina, escopolamina e hiosciamina. Delirio anticolinérgico, alucinaciones, hipertermia, retención urinaria, convulsiones, coma y muerte.", "Planta tropánica muy peligrosa."),
    seedPsychPlant(900003, "Floripondio / Trompeta de ángel", "Brugmansia suaveolens", "Solanaceae", "Mortal", "Toda la planta", "Escopolamina, atropina e hiosciamina. Delirio, alucinaciones, amnesia, taquicardia y coma.", "Árbol ornamental con alcaloides tropánicos."),
    seedPsychPlant(900004, "Beleño negro", "Hyoscyamus niger", "Solanaceae", "Alto", "Toda la planta", "Hiosciamina, escopolamina y atropina. Delirio anticolinérgico, alucinaciones, sequedad extrema y taquicardia.", "Solanácea tropánica histórica."),
    seedPsychPlant(900005, "Mandrágora", "Mandragora officinarum", "Solanaceae", "Alto", "Raíz y hojas", "Atropina, escopolamina e hiosciamina. Sedación, delirio, alucinaciones y coma.", "Planta tropánica mediterránea."),
    seedPsychPlant(900006, "Ayahuasca", "Banisteriopsis caapi", "Malpighiaceae", "Alto", "Tallo y corteza", "Harmina, harmalina y tetrahidroharmina. IMAO; riesgo de síndrome serotoninérgico e interacciones graves.", "Liana con beta-carbolinas IMAO."),
    seedPsychPlant(900007, "Chacruna", "Psychotria viridis", "Rubiaceae", "Alto", "Hojas", "DMT. Alucinaciones intensas, taquicardia, hipertensión, ansiedad y riesgo serotoninérgico.", "Fuente vegetal de DMT."),
    seedPsychPlant(900008, "Mimosa tenuiflora / Jurema", "Mimosa tenuiflora", "Fabaceae", "Alto", "Corteza de raíz", "DMT y NMT. Alucinaciones, taquicardia, hipertensión y riesgo serotoninérgico.", "Árbol rico en triptaminas."),
    seedPsychPlant(900009, "Peganum harmala / Alharma", "Peganum harmala", "Zygophyllaceae", "Alto", "Semillas", "Harmina y harmalina. IMAO, náuseas, convulsiones, alucinaciones e interacciones peligrosas.", "Ruda siria con beta-carbolinas."),
    seedPsychPlant(900010, "Pasionaria", "Passiflora incarnata", "Passifloraceae", "Bajo", "Partes aéreas", "Harman, harmina y sedantes suaves. Posible interacción con IMAO y depresores.", "Sedante vegetal de baja potencia."),
    seedPsychPlant(900011, "Peyote", "Lophophora williamsii", "Cactaceae", "Alto", "Botones", "Mescalina. Alucinaciones intensas, náuseas, taquicardia, hipertensión, ansiedad y psicosis.", "Cactus alucinógeno con fenetilaminas."),
    seedPsychPlant(900012, "Cactus San Pedro", "Echinopsis pachanoi", "Cactaceae", "Alto", "Corteza verde", "Mescalina. Alucinaciones, náuseas, taquicardia, hipertensión y ansiedad.", "Cactus andino con mescalina."),
    seedPsychPlant(900013, "Cannabis", "Cannabis sativa", "Cannabaceae", "Moderado", "Flores y resina", "THC y cannabinoides. Alteración de percepción, ansiedad, taquicardia, somnolencia y paranoia.", "Planta con cannabinoides psicoactivos."),
    seedPsychPlant(900014, "Adormidera / Amapola del opio", "Papaver somniferum", "Papaveraceae", "Mortal", "Látex y cápsulas", "Morfina, codeína, tebaína y papaverina. Opioides: depresión respiratoria, coma, dependencia y muerte por sobredosis.", "Fuente del opio."),
    seedPsychPlant(900015, "Kratom", "Mitragyna speciosa", "Rubiaceae", "Alto", "Hojas", "Mitragynina y 7-hidroximitraginina. Efectos opioides, sedación, dependencia, convulsiones y hepatotoxicidad.", "Árbol con alcaloides opioides."),
    seedPsychPlant(900016, "Iboga", "Tabernanthe iboga", "Apocynaceae", "Mortal", "Corteza de raíz", "Ibogaina y alcaloides de iboga. Alucinaciones, arritmias, convulsiones y muerte súbita.", "Arbusto africano de alto riesgo cardiaco."),
    seedPsychPlant(900017, "Efedra", "Ephedra sinica", "Ephedraceae", "Alto", "Toda la planta", "Efedrina y pseudoefedrina. Estimulante: hipertensión, taquicardia, arritmias, infarto e ictus.", "Arbusto estimulante."),
    seedPsychPlant(900018, "Cafeto", "Coffea arabica", "Rubiaceae", "Bajo", "Semillas", "Cafeína. Estimulante: insomnio, nerviosismo, taquicardia, temblores y ansiedad en exceso.", "Fuente de cafeína."),
    seedPsychPlant(900019, "Planta del té", "Camellia sinensis", "Theaceae", "Bajo", "Hojas", "Cafeína/teína y teofilina. Estimulación, palpitaciones, nerviosismo e insomnio.", "Fuente de metilxantinas."),
    seedPsychPlant(900020, "Guaraná", "Paullinia cupana", "Sapindaceae", "Moderado", "Semillas", "Cafeína. Estimulación intensa, hipertensión, taquicardia y convulsiones en exceso.", "Trepadora amazónica rica en cafeína."),
    seedPsychPlant(900021, "Cacao", "Theobroma cacao", "Malvaceae", "Bajo", "Semillas", "Teobromina y cafeína. Estimulante suave; puede causar taquicardia, nerviosismo y toxicidad en animales.", "Árbol con metilxantinas."),
    seedPsychPlant(900022, "Tabaco", "Nicotiana tabacum", "Solanaceae", "Alto", "Hojas", "Nicotina y nornicotina. Estimulante colinérgico: vómitos, hipertensión, convulsiones, arritmias y muerte.", "Solanácea rica en nicotina."),
    seedPsychPlant(900023, "Betel", "Areca catechu", "Arecaceae", "Alto", "Semillas", "Arecolina. Estimulante colinérgico, náuseas, salivación, taquicardia y riesgo carcinógeno crónico.", "Palmera estimulante."),
    seedPsychPlant(900024, "Khat", "Catha edulis", "Celastraceae", "Alto", "Hojas", "Catinona y catina. Estimulante: euforia, insomnio, hipertensión, taquicardia y ansiedad.", "Arbusto estimulante."),
    seedPsychPlant(900025, "Gloria de la mañana", "Ipomoea tricolor", "Convolvulaceae", "Moderado", "Semillas", "Ergina/LSA. Alucinaciones, náuseas, vasoconstricción, confusión y ansiedad.", "Enredadera con alcaloides ergolínicos."),
    seedPsychPlant(900026, "Hawaiian baby woodrose", "Argyreia nervosa", "Convolvulaceae", "Alto", "Semillas", "LSA/ergina. Alucinaciones, náuseas, vasoconstricción y alteraciones cardiovasculares.", "Convolvulácea ergolínica."),
    seedPsychPlant(900027, "Yopo", "Anadenanthera peregrina", "Fabaceae", "Mortal", "Semillas", "Bufotenina, 5-MeO-DMT y DMT. Alucinaciones extremas, convulsiones, arritmias y paro cardiaco.", "Árbol con triptaminas."),
    seedPsychPlant(900028, "Virola", "Virola theiodora", "Myristicaceae", "Alto", "Corteza y resina", "DMT y 5-MeO-DMT. Alucinaciones, hipertensión, convulsiones y síndrome serotoninérgico.", "Árbol amazónico con triptaminas."),
    seedPsychPlant(900029, "Chaliponga", "Diplopterys cabrerana", "Malpighiaceae", "Alto", "Hojas", "DMT y 5-MeO-DMT. Alucinaciones intensas, taquicardia e interacciones con IMAO.", "Liana/fuente de triptaminas."),
    seedPsychPlant(900030, "Acacia confusa", "Acacia confusa", "Fabaceae", "Alto", "Corteza", "DMT y NMT. Alucinaciones, hipertensión, náuseas y riesgo serotoninérgico.", "Acacia con triptaminas."),
    seedPsychPlant(900031, "Phalaris arundinacea", "Phalaris arundinacea", "Poaceae", "Moderado", "Hojas y rizoma", "DMT, 5-MeO-DMT, bufotenina y gramina. Neurotoxicidad en animales y efectos psicoactivos.", "Gramínea con triptaminas variables."),
    seedPsychPlant(900032, "Caña común", "Arundo donax", "Poaceae", "Moderado", "Rizoma", "DMT, bufotenina y gramina. Efectos psicoactivos y neurotóxicos.", "Caña con alcaloides indólicos."),
    seedPsychPlant(900033, "Salvia divinorum", "Salvia divinorum", "Lamiaceae", "Alto", "Hojas", "Salvinorina A. Alucinógeno disociativo potente, confusión, ansiedad y pérdida de coordinación.", "Lamiácea con diterpenos psicoactivos."),
    seedPsychPlant(900034, "Coca", "Erythroxylum coca", "Erythroxylaceae", "Alto", "Hojas", "Cocaína y alcaloides relacionados. Estimulante: hipertensión, taquicardia, arritmias, ansiedad y convulsiones.", "Arbusto estimulante."),
)

private fun seedPsychPlant(
    id: Int,
    commonName: String,
    scientificName: String,
    family: String,
    toxicityLevel: String,
    toxicParts: String,
    symptoms: String,
    description: String,
): PlantEntity = PlantEntity(
    id = id,
    commonName = commonName,
    commonNames = "",
    scientificName = scientificName,
    family = family,
    toxicityLevel = toxicityLevel,
    toxicParts = toxicParts,
    symptoms = symptoms,
    description = description,
    habitat = "Catálogo interno de emergencia si Room/assets aparecen vacíos.",
    geographicDistribution = "Variable según especie; revisar ficha completa cuando la base local esté disponible.",
    firstAid = "No ingerir. En exposición accidental, contactar con toxicología/urgencias y aportar identificación de la planta.",
    imageUrl = "",
    isFavorite = false,
    category = "Plantas psicotrópicas",
)

private fun classifyPsychotropicPlant(
    plant: PlantEntity,
    compoundIndex: PsychotropicCompoundIndex,
): PsychotropicPlantItem? {
    if (isLikelyFungus(plant)) return null

    val haystack = plant.fullSearchText()
    val scientific = plant.scientificName.normalizedForSearch()
    val common = "${plant.commonName} ${plant.commonNames}".normalizedForSearch()

    val categories = linkedSetOf<String>()
    val compounds = linkedSetOf<String>()
    val reasons = linkedSetOf<String>()

    val plantCanonical = canonicalTaxonKey(plant)
    val plantGenus = plant.scientificName.cleanedTaxonText().firstWord()
    val matchedCompounds = buildList {
        compoundIndex.exactByTaxon[plantCanonical]?.let { addAll(it) }
        compoundIndex.genericByGenus[plantGenus]?.let { addAll(it) }
    }.distinctBy { it.compound.id }

    matchedCompounds.forEach { match ->
        categories.addAll(match.categories)
        compounds += match.compound.commonName
    }
    if (matchedCompounds.isNotEmpty()) {
        reasons += "Cruce con fitoquímica: ${matchedCompounds.take(3).joinToString(", ") { it.compound.commonName }}"
    }

    if (matchesAnyTaxon(scientific, common, HALLUCINOGEN_TAXA) || containsAny(haystack, HALLUCINOGEN_KEYWORDS)) {
        categories += CAT_HALLUCINOGENS
        reasons += "Indicadores de alucinógenos/psicodélicos en la ficha"
        compounds += extractDetectedCompounds(haystack, HALLUCINOGEN_COMPOUND_LABELS)
    }

    if (matchesAnyTaxon(scientific, common, IMAO_TAXA) || containsAny(haystack, IMAO_KEYWORDS)) {
        categories += CAT_IMAO
        reasons += "Indicadores de IMAO o beta-carbolinas"
        compounds += extractDetectedCompounds(haystack, IMAO_COMPOUND_LABELS)
    }

    if (matchesAnyTaxon(scientific, common, DEPRESSANT_TAXA) || containsAny(haystack, DEPRESSANT_KEYWORDS)) {
        categories += CAT_DEPRESSANTS
        reasons += "Indicadores de depresores, sedantes u opioides"
        compounds += extractDetectedCompounds(haystack, DEPRESSANT_COMPOUND_LABELS)
    }

    if (matchesAnyTaxon(scientific, common, STIMULANT_TAXA) || containsAny(haystack, STIMULANT_KEYWORDS)) {
        categories += CAT_STIMULANTS
        reasons += "Indicadores de estimulantes del sistema nervioso"
        compounds += extractDetectedCompounds(haystack, STIMULANT_COMPOUND_LABELS)
    }

    if (matchesAnyTaxon(scientific, common, TROPANE_TAXA) || containsAny(haystack, TROPANE_KEYWORDS)) {
        categories += CAT_TROPANES
        reasons += "Indicadores de alcaloides tropánicos/anticolinérgicos"
        compounds += extractDetectedCompounds(haystack, TROPANE_COMPOUND_LABELS)
    }

    if (categories.isEmpty()) return null

    val orderedCategories = categories.sortedBy { categoryOrder.indexOf(it).let { idx -> if (idx < 0) 999 else idx } }
    val detailScore = plant.description.length + plant.symptoms.length + plant.toxicParts.length + compounds.size * 90 + categories.size * 45 + if (plant.id >= 10000) 900 else 0

    val compoundList = compounds.filter { it.isNotBlank() }.distinct().take(8)
    val reasonList = reasons.filter { it.isNotBlank() }.distinct().take(4)

    return PsychotropicPlantItem(
        plant = plant,
        categories = orderedCategories,
        compounds = compoundList,
        reasons = reasonList,
        score = detailScore,
        searchText = buildPsychotropicSearchText(plant, orderedCategories, compoundList, reasonList),
    )
}

private fun buildPsychotropicItems(
    plants: List<PlantEntity>,
    compounds: List<CompoundEntity>,
): List<PsychotropicPlantItem> {
    val compoundIndex = buildPsychotropicCompoundIndex(compounds)
    val primary = plants.mapNotNull { plant -> classifyPsychotropicPlant(plant, compoundIndex) }
    return primary.ifEmpty { buildSimplePsychotropicFallback(plants) }
}

private fun buildSimplePsychotropicFallback(plants: List<PlantEntity>): List<PsychotropicPlantItem> = plants.mapNotNull { plant ->
    val text = listOf(
        plant.commonName,
        plant.commonNames,
        plant.scientificName,
        plant.family,
        plant.toxicParts,
        plant.symptoms,
        plant.description,
        plant.category,
    ).joinToString(" ").normalizedForSearch()

    val categories = linkedSetOf<String>()
    val compounds = linkedSetOf<String>()
    val reasons = linkedSetOf<String>()

    fun hasAny(words: List<String>) = words.any { text.contains(it) }

    if (hasAny(SIMPLE_HALLUCINOGEN_TERMS)) {
        categories += CAT_HALLUCINOGENS
        reasons += "Coincidencia directa con términos psicodélicos/alucinógenos"
        compounds += extractSimpleLabels(text, SIMPLE_HALLUCINOGEN_LABELS)
    }
    if (hasAny(SIMPLE_IMAO_TERMS)) {
        categories += CAT_IMAO
        reasons += "Coincidencia directa con IMAO/beta-carbolinas"
        compounds += extractSimpleLabels(text, SIMPLE_IMAO_LABELS)
    }
    if (hasAny(SIMPLE_DEPRESSANT_TERMS)) {
        categories += CAT_DEPRESSANTS
        reasons += "Coincidencia directa con depresores/sedantes/opioides"
        compounds += extractSimpleLabels(text, SIMPLE_DEPRESSANT_LABELS)
    }
    if (hasAny(SIMPLE_STIMULANT_TERMS)) {
        categories += CAT_STIMULANTS
        reasons += "Coincidencia directa con estimulantes"
        compounds += extractSimpleLabels(text, SIMPLE_STIMULANT_LABELS)
    }
    if (hasAny(SIMPLE_TROPANE_TERMS)) {
        categories += CAT_TROPANES
        reasons += "Coincidencia directa con alcaloides tropánicos"
        compounds += extractSimpleLabels(text, SIMPLE_TROPANE_LABELS)
    }

    if (categories.isEmpty()) return@mapNotNull null

    val orderedCategories = categories.sortedBy { categoryOrder.indexOf(it).let { idx -> if (idx < 0) 999 else idx } }
    val compoundList = compounds.filter { it.isNotBlank() }.distinct().take(8)
    val reasonList = reasons.distinct().take(4)

    PsychotropicPlantItem(
        plant = plant,
        categories = orderedCategories,
        compounds = compoundList,
        reasons = reasonList,
        score = plant.description.length + plant.symptoms.length + compoundList.size * 80 + orderedCategories.size * 40,
        searchText = buildPsychotropicSearchText(plant, orderedCategories, compoundList, reasonList),
    )
}

private fun extractSimpleLabels(text: String, labels: Map<String, String>): List<String> = labels.mapNotNull { (needle, label) ->
    if (text.contains(needle)) label else null
}.distinct()

private fun buildPsychotropicCompoundIndex(compounds: List<CompoundEntity>): PsychotropicCompoundIndex {
    val exact = mutableMapOf<String, MutableList<PsychotropicCompoundMatch>>()
    val generic = mutableMapOf<String, MutableList<PsychotropicCompoundMatch>>()

    compounds.forEach { compound ->
        val categories = psychotropicCategoriesForCompound(compound)
        if (categories.isEmpty()) return@forEach

        val match = PsychotropicCompoundMatch(compound = compound, categories = categories)
        compound.sourcePlants
            .split('|', ',', ';')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .forEach { source ->
                val sourceClean = source.cleanedTaxonText()
                val sourceGenus = sourceClean.firstWord()
                val genericSource = sourceClean.contains(" spp") ||
                        sourceClean.endsWith(" sp") ||
                        sourceClean.contains(" sp ")

                if (genericSource) {
                    if (sourceGenus.isNotBlank() && sourceGenus in TRUSTED_GENERIC_SOURCE_GENERA) {
                        generic.getOrPut(sourceGenus) { mutableListOf() }.add(match)
                    }
                } else {
                    val sourceCanonical = canonicalTaxonFromText(source)
                    if (sourceCanonical.isNotBlank()) {
                        exact.getOrPut(sourceCanonical) { mutableListOf() }.add(match)
                    }
                }
            }
    }

    return PsychotropicCompoundIndex(
        exactByTaxon = exact.mapValues { (_, value) -> value.distinctBy { it.compound.id } },
        genericByGenus = generic.mapValues { (_, value) -> value.distinctBy { it.compound.id } },
    )
}

private fun collapseDuplicateTaxa(items: List<PsychotropicPlantItem>): List<PsychotropicPlantItem> =
    items.groupBy { canonicalTaxonKey(it.plant).ifBlank { "id:${it.plant.id}" } }
        .values
        .mapNotNull { group ->
            group.maxWithOrNull(
                compareBy<PsychotropicPlantItem> { it.score }
                    .thenBy { toxicityWeight(it.plant.toxicityLevel) }
                    .thenBy { it.plant.id }
            )
        }

private fun buildPsychotropicSearchText(
    plant: PlantEntity,
    categories: List<String>,
    compounds: List<String>,
    reasons: List<String>,
): String = buildString {
    append(plant.fullSearchText())
    append(' ')
    append(categories.joinToString(" ") { categoryById[it]?.label.orEmpty() })
    append(' ')
    append(compounds.joinToString(" "))
    append(' ')
    append(reasons.joinToString(" "))
}.normalizedForSearch()

private fun PlantEntity.fullSearchText(): String = listOf(
    commonName,
    commonNames,
    scientificName,
    family,
    toxicityLevel,
    toxicParts,
    symptoms,
    description,
    habitat,
    geographicDistribution,
    category,
    notes.orEmpty(),
).joinToString(" ").normalizedForSearch()

private fun matchesRiskFilter(level: String, filter: String): Boolean = when (filter) {
    "Mortal" -> level.equals("Mortal", ignoreCase = true)
    "Alto/Muy alto" -> level.equals("Alto", ignoreCase = true) || level.equals("Muy alto", ignoreCase = true)
    "Moderado/Bajo" -> level.equals("Moderado", ignoreCase = true) || level.equals("Bajo", ignoreCase = true)
    else -> true
}

private fun toxicityWeight(level: String): Int = when (level) {
    "Mortal" -> 5
    "Muy alto" -> 4
    "Alto" -> 3
    "Moderado" -> 2
    "Bajo" -> 1
    else -> 0
}

private fun toxicityColor(level: String): Color = when (level) {
    "Mortal" -> Color(0xFFD32F2F)
    "Muy alto" -> Color(0xFFFF5722)
    "Alto" -> Color(0xFFFF6D00)
    "Moderado" -> Color(0xFFFFB300)
    "Bajo" -> Color(0xFF43A047)
    else -> Color(0xFF90A4AE)
}

private fun compoundSourceMatchesPlant(compound: CompoundEntity, plant: PlantEntity): Boolean {
    val plantScientific = plant.scientificName.cleanedTaxonText()
    val plantCanonical = canonicalTaxonKey(plant)
    val plantGenus = plantScientific.firstWord()
    if (plantScientific.isBlank() && plantCanonical.isBlank()) return false

    return compound.sourcePlants
        .split('|', ',', ';')
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .any { source ->
            val sourceClean = source.cleanedTaxonText()
            val sourceCanonical = canonicalTaxonFromText(source)
            val sourceGenus = sourceClean.firstWord()
            val genericSource = sourceClean.contains(" spp") || sourceClean.endsWith(" sp") || sourceClean.contains(" sp ")

            if (genericSource) {
                sourceGenus.isNotBlank() && sourceGenus == plantGenus && sourceGenus in TRUSTED_GENERIC_SOURCE_GENERA
            } else {
                sourceCanonical.isNotBlank() && (
                        sourceCanonical == plantCanonical ||
                                plantScientific == sourceCanonical ||
                                plantScientific.startsWith("$sourceCanonical ")
                        )
            }
        }
}

private fun psychotropicCategoriesForCompound(compound: CompoundEntity): Set<String> {
    val text = listOf(
        compound.commonName,
        compound.iupacName,
        compound.groupName,
        compound.subgroup,
        compound.mechanism,
        compound.clinicalNeuro,
        compound.clinicalOther,
        compound.notes,
    ).joinToString(" ").normalizedForSearch()

    val categories = linkedSetOf<String>()
    if (containsAny(text, HALLUCINOGEN_COMPOUND_KEYWORDS)) categories += CAT_HALLUCINOGENS
    if (containsAny(text, IMAO_KEYWORDS)) categories += CAT_IMAO
    if (containsAny(text, DEPRESSANT_COMPOUND_KEYWORDS)) categories += CAT_DEPRESSANTS
    if (containsAny(text, STIMULANT_COMPOUND_KEYWORDS)) categories += CAT_STIMULANTS
    if (containsAny(text, TROPANE_COMPOUND_KEYWORDS)) categories += CAT_TROPANES
    return categories
}

private fun matchesAnyTaxon(scientific: String, common: String, taxa: List<String>): Boolean = taxa.any { raw ->
    val taxon = raw.normalizedForSearch().trim()
    if (taxon.isBlank()) return@any false
    val isGenusOnly = !taxon.contains(' ')
    if (isGenusOnly) {
        scientific == taxon || scientific.startsWith("$taxon ") || common.contains(" $taxon ") || common.startsWith("$taxon ")
    } else {
        scientific == taxon || scientific.startsWith("$taxon ") || common.contains(taxon)
    }
}

private fun containsAny(text: String, needles: List<String>): Boolean = needles.any { needle ->
    text.contains(needle.normalizedForSearch())
}

private fun extractDetectedCompounds(text: String, labels: Map<String, String>): List<String> = labels.mapNotNull { (needle, label) ->
    if (text.contains(needle.normalizedForSearch())) label else null
}.distinct()

private fun canonicalTaxonKey(plant: PlantEntity): String = canonicalTaxonFromText(plant.scientificName)

private fun canonicalTaxonFromText(text: String): String {
    val parts = text.cleanedTaxonText()
        .split(' ')
        .filter { it.isNotBlank() && it !in TAXON_STOP_WORDS }
    return when {
        parts.size >= 2 -> "${parts[0]} ${parts[1]}"
        parts.size == 1 -> parts[0]
        else -> ""
    }
}

private fun String.cleanedTaxonText(): String = normalizedForSearch()
    .replace(Regex("[^a-z0-9 ]"), " ")
    .replace(Regex("\\s+"), " ")
    .trim()

private fun String.normalizedForSearch(): String {
    val noAccents = Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
    return noAccents
        .lowercase()
        .replace('β', 'b')
        .replace('‐', '-')
        .replace('‑', '-')
        .replace('–', '-')
        .replace('—', '-')
}

private fun String.firstWord(): String = trim().split(Regex("\\s+")).firstOrNull().orEmpty()

private fun isLikelyFungus(plant: PlantEntity): Boolean {
    val scientific = plant.scientificName.normalizedForSearch()
    val family = plant.family.normalizedForSearch()
    return FUNGAL_GENERA.any { scientific == it || scientific.startsWith("$it ") } ||
            family in FUNGAL_FAMILIES
}

private val TAXON_STOP_WORDS = setOf(
    "l", "mill", "aggr", "spp", "sp", "syn", "var", "subsp", "cf", "aff",
)

private val FUNGAL_GENERA = setOf(
    "amanita", "psilocybe", "panaeolus", "gymnopilus", "inocybe", "clitocybe", "cortinarius",
)

private val FUNGAL_FAMILIES = setOf(
    "amanitaceae", "hymenogastraceae", "strophariaceae", "bolbitiaceae", "cortinariaceae", "inocybaceae",
)

private val TRUSTED_GENERIC_SOURCE_GENERA = setOf(
    "brugmansia", "datura", "hyoscyamus", "scopolia", "duboisia",
    "ephedra", "nicotiana", "coffea", "paullinia", "theobroma",
    "phalaris", "virola", "anadenanthera", "passiflora", "peganum",
    "cannabis", "lophophora", "papaver", "mitragyna",
)

private val HALLUCINOGEN_TAXA = listOf(
    "lophophora williamsii",
    "echinopsis pachanoi",
    "echinopsis peruviana",
    "echinopsis lageniformis",
    "echinopsis macrogona",
    "echinopsis bridgesii",
    "trichocereus pachanoi",
    "trichocereus peruvianus",
    "trichocereus bridgesii",
    "cannabis sativa",
    "cannabis indica",
    "cannabis ruderalis",
    "mimosa tenuiflora",
    "mimosa ophthalmocentra",
    "mimosa jurema",
    "mimosa verrucosa",
    "psychotria viridis",
    "psychotria carthagenensis",
    "diplopterys cabrerana",
    "banisteriopsis caapi",
    "anadenanthera",
    "virola",
    "prestonia amazonica",
    "voacanga africana",
    "tabernanthe iboga",
    "delosperma",
    "acaciella angustissima",
    "acacia confusa",
    "acacia phlebophylla",
    "acacia maidenii",
    "acacia obtusifolia",
    "acacia simplex",
    "senegalia berlandieri",
    "desmanthus illinoensis",
    "lespedeza bicolor",
    "arundo donax",
    "phalaris aquatica",
    "phalaris arundinacea",
    "phragmites australis",
    "ipomoea tricolor",
    "ipomoea violacea",
    "argyreia nervosa",
    "turbina corymbosa",
    "rivea corymbosa",
    "salvia divinorum",
)

private val IMAO_TAXA = listOf(
    "banisteriopsis caapi",
    "peganum harmala",
    "passiflora incarnata",
    "passiflora caerulea",
    "tribulus terrestris",
)

private val DEPRESSANT_TAXA = listOf(
    "papaver somniferum",
    "papaver rhoeas",
    "mitragyna speciosa",
    "piper methysticum",
    "valeriana officinalis",
    "humulus lupulus",
    "lactuca virosa",
    "eschscholzia californica",
    "passiflora incarnata",
    "passiflora caerulea",
)

private val STIMULANT_TAXA = listOf(
    "coffea",
    "camellia sinensis",
    "paullinia cupana",
    "theobroma cacao",
    "ilex paraguariensis",
    "ephedra",
    "nicotiana",
    "areca catechu",
    "erythroxylum coca",
    "catha edulis",
    "kola nitida",
    "cola nitida",
)

private val TROPANE_TAXA = listOf(
    "atropa",
    "datura",
    "brugmansia",
    "hyoscyamus",
    "mandragora",
    "scopolia",
    "duboisia",
    "latua",
    "solandra",
)

private val HALLUCINOGEN_KEYWORDS = listOf(
    "dmt", "5-meo-dmt", "5 meo dmt", "n,n-dimetiltriptamina", "dimetiltriptamina",
    "bufotenina", "mescalina", "psilocibina", "psilocina", "baeocistina",
    "ergina", "lsa", "amida del acido lisergico", "acido lisergico",
    "ibogaina", "voacangina", "cannabinoide", "cannabinoides", "thc", "tetrahidrocannabinol",
    "salvinorina", "muscimol", "acido ibotenico", "triptamina", "alucinogeno", "alucinogena",
)

private val IMAO_KEYWORDS = listOf(
    "imao", "inhibicion mao", "inhibidor mao", "inhibidores mao", "monoaminooxidasa",
    "harmina", "harmalina", "harman", "tetrahidroharmina", "beta-carbolina", "beta carbolina", "b-carbolina",
)

private val DEPRESSANT_KEYWORDS = listOf(
    "morfina", "codeina", "tebaina", "papaverina", "noscapina", "opio", "opiaceo", "opiaceos",
    "opioide", "opioides", "depresion respiratoria", "sedacion", "sedante", "sedantes",
    "narcotico", "narcoticos", "gabaergico", "gaba", "kavalactona", "valerenico", "mitraginina",
)

private val STIMULANT_KEYWORDS = listOf(
    "cafeina", "teina", "teobromina", "teofilina", "xantina", "metilxantina",
    "efedrina", "pseudoefedrina", "nicotina", "nornicotina", "anabasina", "arecolina",
    "cocaina", "catinona", "catina", "yohimbina", "lobelina", "estimulante", "estimulantes",
)

private val TROPANE_KEYWORDS = listOf(
    "atropina", "escopolamina", "hiosciamina", "hioscina", "homatropina",
    "alcaloides tropanicos", "alcaloide tropanico", "tropanico", "tropanicos", "tropano",
    "sindrome anticolinergico", "anticolinergico",
)

private val HALLUCINOGEN_COMPOUND_KEYWORDS = HALLUCINOGEN_KEYWORDS
private val IMAO_COMPOUND_KEYWORDS = IMAO_KEYWORDS
private val DEPRESSANT_COMPOUND_KEYWORDS = DEPRESSANT_KEYWORDS + listOf("agonista gaba", "agonista opioide")
private val STIMULANT_COMPOUND_KEYWORDS = STIMULANT_KEYWORDS + listOf("alcaloides xanticos", "metilxantina")
private val TROPANE_COMPOUND_KEYWORDS = TROPANE_KEYWORDS + listOf("alcaloides tropanicos")

private val SIMPLE_HALLUCINOGEN_TERMS = listOf(
    "alucin", "psicodel", "dmt", "5-meo", "bufotenina", "mescalina", "peyote",
    "san pedro", "lophophora", "echinopsis", "trichocereus", "cannabis", "thc",
    "ayahuasca", "chacruna", "psychotria", "mimosa", "jurema", "anadenanthera",
    "yopo", "cebil", "virola", "iboga", "ibogaina", "lsa", "ergina", "ipomoea",
    "gloria de la mañana", "salvia divinorum", "muscimol", "psilocib"
)
private val SIMPLE_IMAO_TERMS = listOf(
    "imao", "inhibicion mao", "inhibidor mao", "monoaminooxidasa", "harmina",
    "harmalina", "harman", "tetrahidroharmina", "beta-carbolina", "beta carbolina",
    "banisteriopsis", "peganum harmala", "passiflora incarnata"
)
private val SIMPLE_DEPRESSANT_TERMS = listOf(
    "morfina", "codeina", "tebaina", "papaverina", "opio", "opiace", "opioide",
    "depresion respiratoria", "sedacion", "sedante", "adormidera", "papaver somniferum",
    "kratom", "mitragyna", "gaba", "kava", "valeriana"
)
private val SIMPLE_STIMULANT_TERMS = listOf(
    "cafeina", "teina", "teobromina", "teofilina", "efedrina", "pseudoefedrina",
    "nicotina", "tabaco", "nicotiana", "arecolina", "cocaina", "catha edulis",
    "khat", "guarana", "paullinia", "coffea", "camellia sinensis", "theobroma cacao",
    "ilex paraguariensis", "estimulante"
)
private val SIMPLE_TROPANE_TERMS = listOf(
    "atropina", "escopolamina", "hiosciamina", "hioscina", "tropan", "anticolinerg",
    "belladona", "atropa", "datura", "estramonio", "brugmansia", "floripondio",
    "hyoscyamus", "beleño", "mandragora", "mandrágora", "scopolia"
)

private val SIMPLE_HALLUCINOGEN_LABELS = mapOf(
    "dmt" to "DMT", "5-meo" to "5-MeO-DMT", "bufotenina" to "Bufotenina",
    "mescalina" to "Mescalina", "thc" to "THC", "ibogaina" to "Ibogaina",
    "lsa" to "LSA", "ergina" to "Ergina/LSA", "psilocib" to "Psilocibina/Psilocina",
    "muscimol" to "Muscimol"
)
private val SIMPLE_IMAO_LABELS = mapOf(
    "harmina" to "Harmina", "harmalina" to "Harmalina", "harman" to "Harman",
    "tetrahidroharmina" to "Tetrahidroharmina", "imao" to "IMAO"
)
private val SIMPLE_DEPRESSANT_LABELS = mapOf(
    "morfina" to "Morfina", "codeina" to "Codeína", "tebaina" to "Tebaína",
    "papaverina" to "Papaverina", "opio" to "Opioides", "mitragyna" to "Mitragynina"
)
private val SIMPLE_STIMULANT_LABELS = mapOf(
    "cafeina" to "Cafeína", "teina" to "Teína", "teobromina" to "Teobromina",
    "teofilina" to "Teofilina", "efedrina" to "Efedrina", "nicotina" to "Nicotina",
    "arecolina" to "Arecolina", "cocaina" to "Cocaína"
)
private val SIMPLE_TROPANE_LABELS = mapOf(
    "atropina" to "Atropina", "escopolamina" to "Escopolamina", "hiosciamina" to "Hiosciamina",
    "hioscina" to "Hioscina", "tropan" to "Alcaloides tropánicos"
)

private val HALLUCINOGEN_COMPOUND_LABELS = mapOf(
    "dmt" to "DMT",
    "nmt" to "NMT",
    "5-meo-dmt" to "5-MeO-DMT",
    "bufotenina" to "Bufotenina",
    "mescalina" to "Mescalina",
    "psilocibina" to "Psilocibina",
    "BAEOSCINA" to "baeoscina",
    "psilocina" to "Psilocina",
    "ergina" to "Ergina",
    "lsd" to "LSD",
    "ibogaina" to "Ibogaina",
    "thc" to "THC",
    "tetrahidrocannabinol" to "THC",
    "salvinorina" to "Salvinorina",
    "muscimol" to "Muscimol",
)

private val IMAO_COMPOUND_LABELS = mapOf(
    "harmina" to "Harmina",
    "harmalina" to "Harmalina",

    "tetrahidroharmina" to "Tetrahidroharmina",
    "beta-carbolina" to "Beta-carbolinas",
    "imao" to "IMAO",
)

private val DEPRESSANT_COMPOUND_LABELS = mapOf(
    "morfina" to "Morfina",
    "codeina" to "Codeína",
    "tebaina" to "Tebaína",
    "papaverina" to "Papaverina",
    "noscapina" to "Noscapina",
    "mitraginina" to "Mitragynina",
    "opio" to "Opioides",
    "gaba" to "GABA",
)

private val STIMULANT_COMPOUND_LABELS = mapOf(
    "cafeina" to "Cafeína",
    "teina" to "Teína",
    "teobromina" to "Teobromina",
    "teofilina" to "Teofilina",
    "efedrina" to "Efedrina",
    "pseudoefedrina" to "Pseudoefedrina",
    "nicotina" to "Nicotina",
    "arecolina" to "Arecolina",
    "cocaina" to "Cocaína",
    "yohimbina" to "Yohimbina",
)

private val TROPANE_COMPOUND_LABELS = mapOf(
    "atropina" to "Atropina",
    "escopolamina" to "Escopolamina",
    "hiosciamina" to "Hiosciamina",
    "hioscina" to "Hioscina",
    "tropanico" to "Alcaloides tropánicos",
    "tropanicos" to "Alcaloides tropánicos",
)
