package com.toxicplants.database.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: PlantViewModel,
    onPlantClick: (PlantEntity) -> Unit,
    onBack: () -> Unit
) {
    val plants by viewModel.plantsData.collectAsState()
    val searchQuery by viewModel.searchQueryData.collectAsState()
    var plantToDelete by remember { mutableStateOf<PlantEntity?>(null) }

    val colors = MaterialTheme.colorScheme

    Scaffold(
        topBar = {
            Surface(modifier = Modifier.fillMaxWidth(), color = colors.primary) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = colors.onPrimary)
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        modifier = Modifier.weight(1f).height(50.dp),
                        placeholder = { Text("Buscar plantas...", color = colors.onPrimary.copy(alpha = 0.6f)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.onPrimary,
                            unfocusedBorderColor = colors.onPrimary.copy(alpha = 0.5f),
                            focusedTextColor = colors.onPrimary,
                            unfocusedTextColor = colors.onPrimary,
                            cursorColor = colors.onPrimary,
                            focusedContainerColor = colors.primaryContainer.copy(alpha = 0.3f),
                            unfocusedContainerColor = colors.primaryContainer.copy(alpha = 0.2f)
                        ),
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = colors.onPrimary) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = colors.onPrimary)
                                }
                            }
                        },
                        shape = RoundedCornerShape(25.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).background(colors.background)) {
            ToxicityFilterChips(onFilterSelect = { viewModel.setToxicityFilter(it) })
            Surface(modifier = Modifier.fillMaxWidth(), color = colors.surface, shadowElevation = 2.dp) {
                Text("🌿 ${plants.size} plantas encontradas", modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), fontWeight = FontWeight.Medium, fontSize = 14.sp, color = colors.primary)
            }
            if (plants.isEmpty() && searchQuery.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔍", fontSize = 64.sp)
                        Spacer(Modifier.height(16.dp))
                        Text("No se encontraron plantas", fontWeight = FontWeight.Bold, color = colors.primary)
                        Text("para \"$searchQuery\"", color = colors.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(plants) { plant ->
                        SearchPlantCard(plant = plant, onClick = { onPlantClick(plant) }, onDeleteClick = { plantToDelete = plant })
                    }
                }
            }
        }
    }

    plantToDelete?.let { plant ->
        AlertDialog(
            onDismissRequest = { plantToDelete = null },
            title = { Text("¿Eliminar planta?", color = colors.primary) },
            text = { Text("¿Estás seguro de eliminar ${plant.commonName}?") },
            confirmButton = { TextButton(onClick = { viewModel.deletePlant(plant); plantToDelete = null }) { Text("Eliminar", color = colors.error) } },
            dismissButton = { TextButton(onClick = { plantToDelete = null }) { Text("Cancelar", color = colors.primary) } }
        )
    }
}

@Composable
fun ToxicityFilterChips(onFilterSelect: (String?) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val toxicityLevels = listOf("Mortal", "Alto", "Moderado", "Bajo")
    var selectedLevel by remember { mutableStateOf<String?>(null) }

    Row(modifier = Modifier.fillMaxWidth().background(colors.surface).padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        toxicityLevels.forEach { level ->
            val isSelected = selectedLevel == level
            val chipColor = when (level) { "Mortal" -> colors.error; "Alto" -> Color(0xFFE65100); "Moderado" -> Color(0xFFF57C00); else -> colors.primary }
            FilterChip(
                selected = isSelected,
                onClick = { selectedLevel = if (selectedLevel == level) null else level; onFilterSelect(selectedLevel) },
                label = { Text(level, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = chipColor, selectedLabelColor = Color.White, containerColor = chipColor.copy(alpha = 0.1f), labelColor = chipColor)
            )
        }
    }
}

@Composable
fun SearchPlantCard(plant: PlantEntity, onClick: () -> Unit, onDeleteClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val toxicityColor = when (plant.toxicityLevel) {
        "Mortal" -> colors.error; "Alto" -> Color(0xFFE65100); "Muy alto" -> Color(0xFFFF5722); "Moderado" -> Color(0xFFF57C00); "Bajo" -> colors.primary; else -> colors.onSurfaceVariant
    }
    val toxicityEmoji = when (plant.toxicityLevel) { "Mortal" -> "💀"; "Muy alto" -> "☠️"; "Alto" -> "⚠️"; "Moderado" -> "⚡"; "Bajo" -> "🟢"; else -> "ℹ️" }

    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = colors.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), shape = RoundedCornerShape(12.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).background(toxicityColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Text(toxicityEmoji, fontSize = 24.sp) }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(plant.commonName, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = colors.onSurface)
                Text(plant.scientificName, fontStyle = FontStyle.Italic, color = colors.onSurfaceVariant, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = toxicityColor.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) { Text(plant.toxicityLevel, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = toxicityColor) }
                    if (plant.category.isNotBlank()) { Surface(color = colors.primary.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) { Text(plant.category, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, color = colors.primary) } }
                }
            }
            IconButton(onClick = onDeleteClick) { Icon(Icons.Default.Clear, contentDescription = "Eliminar", tint = colors.error.copy(alpha = 0.5f), modifier = Modifier.size(20.dp)) }
        }
    }
}