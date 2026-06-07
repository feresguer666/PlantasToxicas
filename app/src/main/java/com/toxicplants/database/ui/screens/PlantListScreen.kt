package com.toxicplants.database.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.ui.viewmodel.PlantViewModel

/**
 * Regiones para filtrar. Cada región tiene una etiqueta (con emoji) y una lista de
 * palabras clave que se buscan dentro del campo geographicDistribution de cada planta.
 */
private data class Region(val label: String, val keywords: List<String>)

private val REGIONS = listOf(
    Region("🌍 Todas", emptyList()),
    Region("🇪🇸 España", listOf("España", "Ibérica", "Iberia", "Península", "Baleares", "Canarias")),
    Region("🌊 Mediterráneo", listOf("Mediterráneo", "Mediterránea")),
    Region("🇪🇺 Europa", listOf("Europa", "Europea", "Eurasia")),
    Region("🌎 América", listOf("América", "Americano", "Americana", "Norteamérica", "Sudamérica", "EE.UU", "Estados Unidos")),
    Region("🇲🇽 México", listOf("México", "Mexicano", "Mexicana", "Mesoamérica")),
    Region("🌐 Sudamérica", listOf("Sudamérica", "Sudamericano", "Argentina", "Chile", "Perú", "Colombia", "Brasil", "Andes")),
    Region("🌏 Asia", listOf("Asia", "Asiático", "Asiática", "China", "Japón", "India", "Eurasia")),
    Region("🌍 África", listOf("África", "Africano", "Africana")),
    Region("🦘 Oceanía", listOf("Australia", "Oceanía", "Nueva Zelanda", "Nueva Guinea")),
    Region("🌴 Tropical", listOf("Tropical", "Trópico", "Pan-tropical")),
    Region("🌐 Cosmopolita", listOf("mundial", "cosmopolita", "Hemisferio", "todo el mundo"))
)

private fun PlantEntity.matchesRegion(region: Region): Boolean {
    if (region.keywords.isEmpty()) return true
    val g = geographicDistribution.lowercase()
    return region.keywords.any { g.contains(it.lowercase()) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantListScreen(
    viewModel: PlantViewModel,
    onPlantClick: (PlantEntity) -> Unit,
    onBack: () -> Unit
) {
    val plants by viewModel.allPlants.observeAsState(emptyList())
    var plantToDelete by remember { mutableStateOf<PlantEntity?>(null) }
    var selectedRegion by remember { mutableStateOf(REGIONS.first()) }

    val filtered = remember(plants, selectedRegion) {
        if (selectedRegion.keywords.isEmpty()) plants
        else plants.filter { it.matchesRegion(selectedRegion) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("📋 Plantas (${filtered.size}/${plants.size})", fontWeight = FontWeight.Bold)
                        if (selectedRegion.keywords.isNotEmpty()) {
                            Text(selectedRegion.label, fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
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
            // ── Filtro de región (chips desplazables) ──
            RegionFilterBar(
                selected = selectedRegion,
                onSelect = { selectedRegion = it }
            )

            if (plants.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF2E7D32))
                }
            } else if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Public, null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Sin plantas para ${selectedRegion.label}", color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered) { plant ->
                        PlantCard(
                            plant = plant,
                            onClick = { onPlantClick(plant) },
                            onDeleteClick = { plantToDelete = plant }
                        )
                    }
                }
            }
        }
    }

    // Diálogo de confirmación para eliminar
    plantToDelete?.let { plant ->
        AlertDialog(
            onDismissRequest = { plantToDelete = null },
            title = { Text("¿Eliminar planta?") },
            text = { Text("¿Estás seguro de eliminar ${plant.commonName}?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePlant(plant)
                    plantToDelete = null
                }) { Text("Eliminar", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { plantToDelete = null }) { Text("Cancelar") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegionFilterBar(
    selected: Region,
    onSelect: (Region) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        REGIONS.forEach { region ->
            FilterChip(
                selected = region == selected,
                onClick = { onSelect(region) },
                label = { Text(region.label, fontSize = 13.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF2E7D32),
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}
