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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toxicplants.database.DuplicateReviewStore
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.ui.viewmodel.PlantViewModel

private enum class DuplicateFilter(val label: String) {
    All("Todos"),
    Scientific("Nombre científico"),
    Common("Nombre común")
}

private data class DuplicateGroup(
    val type: DuplicateFilter,
    val key: String,
    val plants: List<PlantEntity>
)

private fun DuplicateGroup.reviewKey(): String = "${type.name}:${key}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicatePlantsScreen(
    viewModel: PlantViewModel,
    onPlantClick: (PlantEntity) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val allPlants by viewModel.allPlants.observeAsState(emptyList())
    var selectedFilter by remember { mutableStateOf(DuplicateFilter.All) }
    var query by remember { mutableStateOf("") }
    var plantToDelete by remember { mutableStateOf<PlantEntity?>(null) }
    var reviewedGroups by remember { mutableStateOf(DuplicateReviewStore.load(context)) }
    var showReviewed by remember { mutableStateOf(false) }

    val duplicateGroups = remember(allPlants) { buildDuplicateGroups(allPlants) }
    val visibleGroups = remember(duplicateGroups, selectedFilter, query, reviewedGroups, showReviewed) {
        duplicateGroups
            .filter { showReviewed || it.reviewKey() !in reviewedGroups }
            .filter { selectedFilter == DuplicateFilter.All || it.type == selectedFilter }
            .filter { group ->
                query.isBlank() ||
                    group.key.contains(query, ignoreCase = true) ||
                    group.plants.any {
                        it.commonName.contains(query, ignoreCase = true) ||
                            it.scientificName.contains(query, ignoreCase = true) ||
                            it.family.contains(query, ignoreCase = true)
                    }
            }
            .sortedWith(compareBy<DuplicateGroup> { it.type.ordinal }.thenBy { it.key })
    }

    val scientificCount = remember(duplicateGroups) { duplicateGroups.count { it.type == DuplicateFilter.Scientific } }
    val commonCount = remember(duplicateGroups) { duplicateGroups.count { it.type == DuplicateFilter.Common } }
    val reviewedCount = remember(duplicateGroups, reviewedGroups) { duplicateGroups.count { it.reviewKey() in reviewedGroups } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("🔁 Posibles duplicados", fontWeight = FontWeight.Bold)
                        Text(
                            "${visibleGroups.size} grupos · $reviewedCount revisados ocultos",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.82f)
                        )
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ) {
                Column {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        singleLine = true,
                        label = { Text("Buscar duplicados") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedFilter == DuplicateFilter.All,
                                onClick = { selectedFilter = DuplicateFilter.All },
                                label = { Text("Todos (${duplicateGroups.size})", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF2E7D32),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedFilter == DuplicateFilter.Scientific,
                                onClick = { selectedFilter = DuplicateFilter.Scientific },
                                label = { Text("Científico ($scientificCount)", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF2E7D32),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedFilter == DuplicateFilter.Common,
                                onClick = { selectedFilter = DuplicateFilter.Common },
                                label = { Text("Común ($commonCount)", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF2E7D32),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }

                        item {
                            FilterChip(
                                selected = showReviewed,
                                onClick = { showReviewed = !showReviewed },
                                label = { Text("Mostrar revisados ($reviewedCount)", fontSize = 12.sp) },
                                enabled = reviewedCount > 0,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF607D8B),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            if (allPlants.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF2E7D32))
                }
            } else if (visibleGroups.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✅", fontSize = 48.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("No hay grupos para este filtro", fontWeight = FontWeight.Bold)
                        Text("Prueba otro filtro o búsqueda", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(visibleGroups, key = { "${it.type.name}:${it.key}" }) { group ->
                        DuplicateGroupCard(
                            group = group,
                            reviewed = group.reviewKey() in reviewedGroups,
                            onMarkReviewed = {
                                DuplicateReviewStore.markReviewed(context, group.reviewKey())
                                reviewedGroups = DuplicateReviewStore.load(context)
                            },
                            onUnmarkReviewed = {
                                DuplicateReviewStore.unmarkReviewed(context, group.reviewKey())
                                reviewedGroups = DuplicateReviewStore.load(context)
                            },
                            onOpenGroup = {
                                viewModel.setDetailNavigationPlants(group.plants)
                                onPlantClick(group.plants.first())
                            },
                            onOpenPlant = { plant ->
                                viewModel.setDetailNavigationPlants(group.plants)
                                onPlantClick(plant)
                            },
                            onDeletePlant = { plant -> plantToDelete = plant }
                        )
                    }
                }
            }
        }
    }

    plantToDelete?.let { plant ->
        AlertDialog(
            onDismissRequest = { plantToDelete = null },
            title = { Text("¿Eliminar planta duplicada?") },
            text = {
                Text("Se eliminará ${plant.commonName} (#${plant.id}) de la base local y quedará en la papelera de plantas.")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePlant(plant)
                    plantToDelete = null
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { plantToDelete = null }) { Text("Cancelar") }
            }
        )
    }

}

@Composable
private fun DuplicateGroupCard(
    group: DuplicateGroup,
    reviewed: Boolean,
    onMarkReviewed: () -> Unit,
    onUnmarkReviewed: () -> Unit,
    onOpenGroup: () -> Unit,
    onOpenPlant: (PlantEntity) -> Unit,
    onDeletePlant: (PlantEntity) -> Unit
) {
    val color = when (group.type) {
        DuplicateFilter.Scientific -> Color(0xFF1565C0)
        DuplicateFilter.Common -> Color(0xFF6A1B9A)
        DuplicateFilter.All -> Color(0xFF2E7D32)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (group.type == DuplicateFilter.Scientific) "🔬 Mismo científico" else "🏷️ Mismo nombre común",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = color
                    )
                    Text(
                        group.key,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (reviewed) {
                        Text("✓ Grupo revisado", fontSize = 11.sp, color = Color(0xFF607D8B), fontWeight = FontWeight.Medium)
                    }
                }
                TextButton(onClick = if (reviewed) onUnmarkReviewed else onMarkReviewed) {
                    Text(if (reviewed) "Reactivar" else "Marcar revisado")
                }
                TextButton(onClick = onOpenGroup) { Text("Abrir grupo") }
            }

            Spacer(Modifier.height(8.dp))
            group.plants.forEach { plant ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clickable { onOpenPlant(plant) },
                    color = color.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("#${plant.id} · ${plant.commonName}", fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(plant.scientificName, fontStyle = FontStyle.Italic, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (plant.family.isNotBlank()) {
                                Text(plant.family, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        IconButton(onClick = { onDeletePlant(plant) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Eliminar planta duplicada",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        Text("Ver →", fontSize = 12.sp, color = color, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun buildDuplicateGroups(plants: List<PlantEntity>): List<DuplicateGroup> {
    val scientificGroups = plants
        .mapNotNull { plant ->
            val key = normalizeScientificDuplicateKey(plant.scientificName)
            if (key.isBlank()) null else key to plant
        }
        .groupBy({ it.first }, { it.second })
        .filterValues { it.size > 1 }
        .map { (key, groupPlants) -> DuplicateGroup(DuplicateFilter.Scientific, key, groupPlants.sortedBy { it.id }) }

    val commonGroups = plants
        .mapNotNull { plant ->
            val key = normalizeCommonDuplicateKey(plant.commonName)
            if (key.isBlank()) null else key to plant
        }
        .groupBy({ it.first }, { it.second })
        .filterValues { it.size > 1 }
        .map { (key, groupPlants) -> DuplicateGroup(DuplicateFilter.Common, key, groupPlants.sortedBy { it.id }) }

    return scientificGroups + commonGroups
}

private fun normalizeScientificDuplicateKey(value: String): String {
    val cleaned = value
        .lowercase()
        .replace(Regex("\\s+"), " ")
        .trim()
    if (cleaned.length < 4) return ""
    val parts = cleaned.split(" ").filter { it.isNotBlank() }
    if (parts.size < 2) return cleaned
    // Para detectar duplicados probables agrupamos por binomio género+especie.
    // Variedades/subespecies pueden aparecer aquí: por eso son "posibles" duplicados.
    return "${parts[0]} ${parts[1]}"
}

private fun normalizeCommonDuplicateKey(value: String): String =
    value
        .lowercase()
        .replace(Regex("[^a-záéíóúüñ0-9 ]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
