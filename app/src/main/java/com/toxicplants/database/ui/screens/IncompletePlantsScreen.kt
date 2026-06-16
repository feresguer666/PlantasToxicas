package com.toxicplants.database.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
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

private enum class IncompletePlantFilter(
    val label: String,
    val fieldLabel: String,
    val isMissing: (PlantEntity) -> Boolean
) {
    Any("Todas", "Cualquier campo", { plant -> REQUIRED_QUALITY_FILTERS.any { it.isMissing(plant) } }),
    Image("Sin imagen", "Imagen", { it.imageUrl.isBlank() }),
    ScientificName("Sin nombre científico", "Nombre científico", { it.scientificName.isBlank() }),
    Family("Sin familia", "Familia", { it.family.isBlank() }),
    Category("Sin categoría", "Categoría", { it.category.isBlank() }),
    Description("Sin descripción", "Descripción", { it.description.isBlank() }),
    ToxicParts("Sin partes tóxicas", "Partes tóxicas", { it.toxicParts.isBlank() }),
    Symptoms("Sin síntomas", "Síntomas", { it.symptoms.isBlank() }),
    FirstAid("Sin primeros auxilios", "Primeros auxilios", { it.firstAid.isBlank() }),
    Habitat("Sin hábitat", "Hábitat", { it.habitat.isBlank() }),
    Distribution("Sin distribución", "Distribución", { it.geographicDistribution.isBlank() })
}

private val REQUIRED_QUALITY_FILTERS = listOf(
    IncompletePlantFilter.Image,
    IncompletePlantFilter.ScientificName,
    IncompletePlantFilter.Family,
    IncompletePlantFilter.Category,
    IncompletePlantFilter.Description,
    IncompletePlantFilter.ToxicParts,
    IncompletePlantFilter.Symptoms,
    IncompletePlantFilter.FirstAid,
    IncompletePlantFilter.Habitat,
    IncompletePlantFilter.Distribution
)

private fun PlantEntity.missingQualityLabels(): List<String> =
    REQUIRED_QUALITY_FILTERS
        .filter { it.isMissing(this) }
        .map { it.fieldLabel }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncompletePlantsScreen(
    viewModel: PlantViewModel,
    onPlantClick: (PlantEntity) -> Unit,
    onBack: () -> Unit
) {
    val allPlants by viewModel.allPlants.observeAsState(emptyList())
    var selectedFilter by remember { mutableStateOf(IncompletePlantFilter.Any) }

    val incompletePlants = remember(allPlants, selectedFilter) {
        allPlants
            .filter { selectedFilter.isMissing(it) }
            .sortedWith(compareBy<PlantEntity> { it.missingQualityLabels().size }.thenBy { it.commonName.lowercase() })
    }

    val totalWithAnyIssue = remember(allPlants) {
        allPlants.count { plant -> IncompletePlantFilter.Any.isMissing(plant) }
    }

    val countsByFilter = remember(allPlants) {
        IncompletePlantFilter.entries.associateWith { filter -> allPlants.count { filter.isMissing(it) } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("🧹 Plantas incompletas", fontWeight = FontWeight.Bold)
                        Text(
                            "$totalWithAnyIssue de ${allPlants.size} con avisos",
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
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(IncompletePlantFilter.entries) { filter ->
                        val count = countsByFilter[filter] ?: 0
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text("${filter.label} ($count)", fontSize = 12.sp) },
                            leadingIcon = if (count > 0) { { Text("⚠️", fontSize = 12.sp) } } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF2E7D32),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Text(
                    "${incompletePlants.size} plantas · Filtro: ${selectedFilter.label}",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (allPlants.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF2E7D32))
                }
            } else if (incompletePlants.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✅", fontSize = 48.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("No hay plantas para este filtro", fontWeight = FontWeight.Bold)
                        Text("Prueba otro campo", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(incompletePlants, key = { it.id }) { plant ->
                        IncompletePlantCard(
                            plant = plant,
                            onClick = {
                                viewModel.setDetailNavigationPlants(incompletePlants)
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
private fun IncompletePlantCard(
    plant: PlantEntity,
    onClick: () -> Unit
) {
    val missing = plant.missingQualityLabels()
    val severityColor = when {
        missing.any { it in listOf("Nombre científico", "Descripción", "Síntomas", "Partes tóxicas") } -> Color(0xFFE65100)
        missing.any { it in listOf("Imagen", "Familia", "Primeros auxilios") } -> Color(0xFFF57C00)
        else -> Color(0xFF607D8B)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = severityColor.copy(alpha = 0.14f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = severityColor)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    plant.commonName.ifBlank { "#${plant.id}" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    plant.scientificName.ifBlank { "Sin nombre científico" },
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    missing.take(5).forEach { label ->
                        Surface(
                            color = severityColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                label,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                color = severityColor,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    if (missing.size > 5) {
                        Text("+${missing.size - 5}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
