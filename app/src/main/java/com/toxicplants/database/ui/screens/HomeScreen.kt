package com.toxicplants.database.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.ui.viewmodel.PlantViewModel
import androidx.compose.runtime.collectAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: PlantViewModel,
    onNavigateToList: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToEmergency: () -> Unit,
    onNavigateToOnlineDatabases: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToDownloadImages: () -> Unit,
    onNavigateToNewPlant: () -> Unit,
    onNavigateToCamera: () -> Unit,
    onPlantClick: (PlantEntity) -> Unit
) {
    val allPlants by viewModel.allPlants.observeAsState(emptyList())
    val mortalPlants by viewModel.mortalPlantsData.collectAsState()  // mortalPlantsData
    val allFamilies by viewModel.allFamilies.observeAsState(emptyList())
    var plantToDelete by remember { mutableStateOf<PlantEntity?>(null) }

    val mortalCount = allPlants.count { it.toxicityLevel == "Mortal" }
    val altoRiesgoCount = allPlants.count { it.toxicityLevel == "Alto" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Plantas Toxicas", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("Base de datos completa", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                    }
                },
                actions = {
                    // ✅ BOTÓN CÁMARA (usando emoji como alternativa)
                    IconButton(onClick = onNavigateToCamera) {
                        Text("📷", fontSize = 22.sp)
                    }

                    IconButton(onClick = onNavigateToDownloadImages) {
                        Icon(Icons.Filled.Search, contentDescription = "Descargar", modifier = Modifier.size(26.dp))
                    }

                    IconButton(onClick = onNavigateToNewPlant) {
                        Text("+", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { EmergencyBanner(onClick = onNavigateToEmergency) }
            item { StatsRow(allPlants.size, mortalCount, altoRiesgoCount, allFamilies.size) }
            item {
                NavigationGrid(
                    onNavigateToList = onNavigateToList,
                    onNavigateToCategories = onNavigateToCategories,
                    onNavigateToOnlineDatabases = onNavigateToOnlineDatabases,
                    onNavigateToSearch = onNavigateToSearch
                )
            }
            item {
                Text("Plantas Mortales", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFFB71C1C), modifier = Modifier.padding(vertical = 8.dp))
            }
            items(mortalPlants.take(10)) { plant ->
                PlantCard(plant = plant, onClick = { onPlantClick(plant) }, onDeleteClick = { plantToDelete = plant })
            }
            if (mortalPlants.size > 10) {
                item {
                    TextButton(onClick = onNavigateToList, modifier = Modifier.fillMaxWidth()) {
                        Text("Ver todas (${mortalPlants.size})")
                    }
                }
            }
        }
    }

    plantToDelete?.let { plant ->
        AlertDialog(
            onDismissRequest = { plantToDelete = null },
            title = { Text("¿Eliminar planta?") },
            text = { Text("¿Eliminar ${plant.commonName}?") },
            confirmButton = {
                TextButton(onClick = { viewModel.deletePlant(plant); plantToDelete = null }) {
                    Text("Eliminar", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { plantToDelete = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun EmergencyBanner(onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = Color(0xFFB71C1C)), shape = RoundedCornerShape(16.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("⚠️", fontSize = 40.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("EMERGENCIA TOXICOLOGICA", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Toca aqui si hay intoxicacion", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
                Text("☎ Tox. Espana: 91 562 04 20", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun StatsRow(totalPlants: Int, mortalCount: Int, altoRiesgoCount: Int, familiesCount: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        StatCard("🌿", totalPlants.toString(), "Plantas", Color(0xFF388E3C))
        StatCard("☠️", mortalCount.toString(), "Mortales", Color(0xFFB71C1C))
        StatCard("⚠️", altoRiesgoCount.toString(), "Alto riesgo", Color(0xFFF57C00))
        StatCard("📚", familiesCount.toString(), "Familias", Color(0xFF1976D2))
    }
}

@Composable
fun StatCard(emoji: String, value: String, label: String, color: Color) {
    Card(modifier = Modifier.size(width = 80.dp, height = 80.dp), colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(emoji, fontSize = 20.sp)
            Text(value, fontWeight = FontWeight.Bold, color = color, fontSize = 16.sp)
            Text(label, fontSize = 10.sp, color = Color.Gray)
        }
    }
}

@Composable
fun NavigationGrid(onNavigateToList: () -> Unit, onNavigateToCategories: () -> Unit, onNavigateToOnlineDatabases: () -> Unit, onNavigateToSearch: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NavButton(Modifier.weight(1f), "📋", "Todas", Color(0xFF388E3C), onNavigateToList)
        NavButton(Modifier.weight(1f), "🗂", "Categorias", Color(0xFF1976D2), onNavigateToCategories)
        NavButton(Modifier.weight(1f), "🔍", "Buscar", Color(0xFF7B1FA2), onNavigateToSearch)
        NavButton(Modifier.weight(1f), "🌐", "Recursos", Color(0xFFB71C1C), onNavigateToOnlineDatabases)
    }
}

@Composable
fun NavButton(modifier: Modifier, icon: String, text: String, color: Color, onClick: () -> Unit) {
    Card(modifier = modifier.height(80.dp).clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = color)) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(icon, fontSize = 24.sp)
            Text(text, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun PlantCard(plant: PlantEntity, onClick: () -> Unit, onDeleteClick: () -> Unit) {
    val toxicityColor = when (plant.toxicityLevel) {
        "Mortal" -> Color(0xFFB71C1C)
        "Alto" -> Color(0xFFE65100)
        "Moderado" -> Color(0xFFF57C00)
        "Bajo" -> Color(0xFF388E3C)
        else -> Color.Gray
    }

    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)).background(toxicityColor.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Text(when (plant.toxicityLevel) { "Mortal" -> "☠️"; "Alto" -> "⚠️"; "Moderado" -> "⚡"; else -> "ℹ️" }, fontSize = 24.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(plant.commonName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(plant.scientificName, style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = toxicityColor.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                        Text(plant.toxicityLevel, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 11.sp, color = toxicityColor, fontWeight = FontWeight.Bold)
                    }
                    Surface(color = Color.Gray.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                        Text(plant.category, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
        }
    }
}