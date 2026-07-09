package com.toxicplants.database.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
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
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.ui.viewmodel.PlantViewModel

private val dangerousLevels = listOf("Mortal", "Muy alto", "Alto", "Moderado")

private enum class OrnamentalToxicityFilter(val label: String, val levels: Set<String>) {
    All("Todas", dangerousLevels.toSet()),
    Mortal("Mortal", setOf("Mortal")),
    High("Alto/Muy alto", setOf("Alto", "Muy alto")),
    Moderate("Moderado", setOf("Moderado"))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrnamentalDangerPlantsScreen(
    viewModel: PlantViewModel,
    onPlantClick: (PlantEntity) -> Unit,
    onBack: () -> Unit
) {
    val allPlants by viewModel.allPlants.observeAsState(emptyList())
    var selectedFilter by remember { mutableStateOf(OrnamentalToxicityFilter.All) }
    var query by remember { mutableStateOf("") }
    var baseOrnamentalPlants by remember { mutableStateOf<List<PlantEntity>>(emptyList()) }
    var isLoadingCache by remember { mutableStateOf(true) }

    LaunchedEffect(allPlants.size) {
        if (allPlants.isEmpty()) {
            baseOrnamentalPlants = emptyList()
            isLoadingCache = true
        } else {
            isLoadingCache = true
            baseOrnamentalPlants = viewModel.getOrnamentalDangerPlantsCached()
            isLoadingCache = false
        }
    }

    val ornamentalPlants = remember(baseOrnamentalPlants, selectedFilter, query) {
        baseOrnamentalPlants
            .filter { it.toxicityLevel in selectedFilter.levels }
            .filter { plant ->
                query.isBlank() ||
                    plant.commonName.contains(query, ignoreCase = true) ||
                    plant.scientificName.contains(query, ignoreCase = true) ||
                    plant.family.contains(query, ignoreCase = true) ||
                    plant.category.contains(query, ignoreCase = true)
            }
    }

    val totalOrnamental = baseOrnamentalPlants.size
    val counts = remember(baseOrnamentalPlants) {
        OrnamentalToxicityFilter.entries.associateWith { filter ->
            baseOrnamentalPlants.count { it.toxicityLevel in filter.levels }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("🏡 Ornamentales peligrosas", fontWeight = FontWeight.Bold)
                        Text("${ornamentalPlants.size} de $totalOrnamental", fontSize = 12.sp, color = Color.White.copy(alpha = 0.82f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2E7D32),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ) {
                Column {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        singleLine = true,
                        label = { Text("Buscar ornamental") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(OrnamentalToxicityFilter.entries) { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = { Text("${filter.label} (${counts[filter] ?: 0})", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF2E7D32),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            if (allPlants.isEmpty() || isLoadingCache) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF2E7D32))
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (allPlants.isEmpty()) "Cargando plantas…" else "Cargando ornamentales peligrosas…",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (ornamentalPlants.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🏡", fontSize = 48.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("No hay resultados", fontWeight = FontWeight.Bold)
                        Text("Prueba otro filtro o búsqueda", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ornamentalPlants, key = { it.id }) { plant ->
                        OrnamentalDangerPlantCard(
                            plant = plant,
                            onClick = {
                                viewModel.setDetailNavigationPlants(ornamentalPlants)
                                onPlantClick(plant)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrnamentalDangerPlantCard(
    plant: PlantEntity,
    onClick: () -> Unit
) {
    val toxicityColor = when (plant.toxicityLevel) {
        "Mortal" -> Color(0xFFB71C1C)
        "Muy alto" -> Color(0xFFFF5722)
        "Alto" -> Color(0xFFE65100)
        "Moderado" -> Color(0xFFF57C00)
        else -> Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = toxicityColor.copy(alpha = 0.14f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) { Text("🏡", fontSize = 24.sp) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(plant.commonName, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(plant.scientificName, fontStyle = FontStyle.Italic, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(color = toxicityColor.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                        Text(plant.toxicityLevel, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, color = toxicityColor, fontWeight = FontWeight.Bold)
                    }
                    if (plant.category.isNotBlank()) {
                        Surface(color = Color(0xFF2E7D32).copy(alpha = 0.10f), shape = RoundedCornerShape(4.dp)) {
                            Text(plant.category, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, color = Color(0xFF2E7D32))
                        }
                    }
                }
            }
        }
    }
}
