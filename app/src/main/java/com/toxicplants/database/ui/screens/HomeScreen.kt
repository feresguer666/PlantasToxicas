package com.toxicplants.database.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
    onNavigateToPhytochemistry: () -> Unit,
    onNavigateToSearchBySymptoms: () -> Unit,
    onNavigateToAR: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onPlantClick: (PlantEntity) -> Unit,
) {
    val allPlants by viewModel.allPlants.observeAsState(emptyList())
    val allFamilies by viewModel.allFamilies.observeAsState(emptyList())

    val mortalCount = allPlants.count { it.toxicityLevel == "Mortal" }
    val altoRiesgoCount = allPlants.count { it.toxicityLevel == "Alto" }

    var showSearchDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("🌿 Plantas Tóxicas", fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, color = Color.White)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D3311)),
                actions = {
                    // Lupa para descargar imágenes (antes engranaje)
                    IconButton(onClick = onNavigateToDownloadImages) {
                        Icon(
                            Icons.Filled.Download,
                            contentDescription = "Descargar imágenes",
                            modifier = Modifier.size(26.dp),
                            tint = Color.White
                        )
                    }
                    // Engranaje para ajustes
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "Ajustes",
                            modifier = Modifier.size(26.dp),
                            tint = Color.White
                        )
                    }
                    // Botón + para nueva planta
                    IconButton(onClick = onNavigateToNewPlant) {
                        Text("+", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).background(
                Brush.verticalGradient(colors = listOf(Color(0xFF0D1F0F), Color(0xFF0A1A0C), Color(0xFF081608)))
            ),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Emergencia
                    Card(
                        modifier = Modifier.fillMaxWidth().height(100.dp).clickable { onNavigateToEmergency() },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFB71C1C)),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                            Spacer(Modifier.width(20.dp))
                            Column {
                                Text("EMERGENCIA", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                                Text("☎ 91 562 04 20", color = Color.White.copy(alpha = 0.95f), fontSize = 16.sp)
                            }
                        }
                    }

                    // Fitoquímica
                    Card(
                        modifier = Modifier.fillMaxWidth().height(100.dp).clickable { onNavigateToPhytochemistry() },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF4A148C)),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Science, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                            Spacer(Modifier.width(20.dp))
                            Column {
                                Text("FITOQUÍMICA", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                                Text("Alcaloides, glicósidos y más", color = Color.White.copy(alpha = 0.95f), fontSize = 16.sp)
                            }
                        }
                    }
                }
            }

            // Stats
            item { StatsRow(allPlants.size, mortalCount, altoRiesgoCount, allFamilies.size) }

            // Navegación
            item {
                NavigationGrid(
                    onNavigateToList = onNavigateToList,
                    onNavigateToCategories = onNavigateToCategories,
                    onNavigateToOnlineDatabases = onNavigateToOnlineDatabases,
                    onNavigateToSearch = { showSearchDialog = true },
                    onNavigateToAR = onNavigateToAR,
                    onNavigateToCamera = onNavigateToCamera
                )
            }
        }
    }

    // Diálogo de búsqueda
    if (showSearchDialog) {
        SearchTypeDialog(
            onDismiss = { showSearchDialog = false },
            onSearchByName = { showSearchDialog = false; onNavigateToSearch() },
            onSearchBySymptoms = { showSearchDialog = false; onNavigateToSearchBySymptoms() }
        )
    }
}

@Composable
fun SearchTypeDialog(onDismiss: () -> Unit, onSearchByName: () -> Unit, onSearchBySymptoms: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🔍 ¿Cómo quieres buscar?", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onSearchByName() },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("🔤", fontSize = 28.sp)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Buscar por nombre", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Nombre común o científico", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onSearchBySymptoms() },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF6A1B9A)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("🔬", fontSize = 28.sp)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Buscar por síntomas", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Síntomas, toxicidad y categoría", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun StatsRow(totalPlants: Int, mortalCount: Int, altoRiesgoCount: Int, familiesCount: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        StatCard("🌿", totalPlants.toString(), "Plantas", Color(0xFF4CAF50))
        StatCard("☠️", mortalCount.toString(), "Mortales", Color(0xFFEF5350))
        StatCard("⚠️", altoRiesgoCount.toString(), "Alto riesgo", Color(0xFFFFA726))
        StatCard("📚", familiesCount.toString(), "Familias", Color(0xFF42A5F5))
    }
}

@Composable
fun StatCard(emoji: String, value: String, label: String, color: Color) {
    Card(
        modifier = Modifier.size(width = 82.dp, height = 82.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(emoji, fontSize = 22.sp)
            Text(value, fontWeight = FontWeight.Bold, color = color, fontSize = 18.sp)
            Text(label, fontSize = 11.sp, color = Color.LightGray)
        }
    }
}

@Composable
fun NavigationGrid(onNavigateToList: () -> Unit, onNavigateToCategories: () -> Unit, onNavigateToOnlineDatabases: () -> Unit, onNavigateToSearch: () -> Unit, onNavigateToAR: () -> Unit, onNavigateToCamera: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            NavButton(Modifier.weight(1f), "📋", "Todas las Plantas", Color(0xFF2E7D32), onNavigateToList)
            NavButton(Modifier.weight(1f), "🗂", "Categorías", Color(0xFF1565C0), onNavigateToCategories)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            NavButton(Modifier.weight(1f), "🔍", "Buscar", Color(0xFF6A1B9A), onNavigateToSearch)
            NavButton(Modifier.weight(1f), "🌐", "Recursos", Color(0xFFC62828), onNavigateToOnlineDatabases)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            NavButton(Modifier.weight(1f), "🎯", "AR Detección", Color(0xFF1A237E), onNavigateToAR)
            NavButton(Modifier.weight(1f), "📷", "Identificar", Color(0xFF00695C), onNavigateToCamera)
        }
    }
}

@Composable
fun NavButton(modifier: Modifier, icon: String, text: String, color: Color, onClick: () -> Unit) {
    Card(
        modifier = modifier.height(110.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(icon, fontSize = 32.sp)
            Spacer(Modifier.height(8.dp))
            Text(text, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
        }
    }
}