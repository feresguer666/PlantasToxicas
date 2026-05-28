package com.toxicplants.database.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.ui.viewmodel.PlantViewModel

private object GreenScale {
    val bg0    = Color(0xFF060F07)
    val bg1    = Color(0xFF0A1A0C)
    val bg2    = Color(0xFF0D2410)
    val topBar = Color(0xFF0D3311)
}
private val brownStart = Color(0xFF4E2A04)
private val brownEnd   = Color(0xFF7B4A1A)
private val redStart   = Color(0xFF7F0000)
private val redEnd     = Color(0xFFB71C1C)

@Suppress("UNUSED_PARAMETER")
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
    onNavigateToBerries: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToNotes: () -> Unit = {},
    onPlantClick: (PlantEntity) -> Unit,
) {
    val allPlants   by viewModel.allPlants.observeAsState(emptyList())
    val allFamilies by viewModel.allFamilies.observeAsState(emptyList())
    val mortalCount     = allPlants.count { it.toxicityLevel == "Mortal" }
    val altoRiesgoCount = allPlants.count { it.toxicityLevel == "Alto" }
    var showSearchDialog   by remember { mutableStateOf(false) }
    var showIdentifyDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🌿 Plantas Tóxicas", fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenScale.topBar),
                actions = {
                    IconButton(onClick = onNavigateToNotes) {
                        Icon(Icons.AutoMirrored.Filled.Notes, "Bloc de notas", modifier = Modifier.size(26.dp), tint = Color.White)
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Settings, "Ajustes", modifier = Modifier.size(26.dp), tint = Color.White)
                    }
                    IconButton(onClick = onNavigateToNewPlant) {
                        Text("+", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).background(Brush.verticalGradient(listOf(GreenScale.bg0, GreenScale.bg1, GreenScale.bg2))),
            contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                BannerCard(Brush.horizontalGradient(listOf(redStart, redEnd)), 80.dp, onNavigateToEmergency) {
                    Icon(Icons.Filled.Warning, null, tint = Color.White, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("EMERGENCIA", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("☎ 91 562 04 20", color = Color.White.copy(alpha = 0.92f), fontSize = 14.sp)
                    }
                }
            }
            item {
                BannerCard(Brush.horizontalGradient(listOf(brownStart, brownEnd)), 80.dp, onNavigateToOnlineDatabases) {
                    Icon(Icons.Filled.Language, null, tint = Color.White, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("RECURSOS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Bases de datos y referencias online", color = Color.White.copy(alpha = 0.92f), fontSize = 14.sp)
                    }
                }
            }
            item { StatsRow(allPlants.size, mortalCount, altoRiesgoCount, allFamilies.size) }
            item {
                NavigationGrid(
                    onNavigateToList = onNavigateToList, onNavigateToCategories = onNavigateToCategories,
                    onNavigateToPhytochemistry = onNavigateToPhytochemistry, onNavigateToBerries = onNavigateToBerries,
                    onNavigateToSearch = { showSearchDialog = true }, onNavigateToIdentify = { showIdentifyDialog = true }
                )
            }
        }
    }

    if (showSearchDialog) {
        SearchTypeDialog(onDismiss = { showSearchDialog = false },
            onSearchByName = { showSearchDialog = false; onNavigateToSearch() },
            onSearchBySymptoms = { showSearchDialog = false; onNavigateToSearchBySymptoms() })
    }
    if (showIdentifyDialog) {
        IdentifyTypeDialog(onDismiss = { showIdentifyDialog = false },
            onIdentifyByCamera = { showIdentifyDialog = false; onNavigateToCamera() },
            onIdentifyByAR = { showIdentifyDialog = false; onNavigateToAR() })
    }
}

@Composable
fun BannerCard(gradient: Brush, height: Dp, onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(20.dp)).background(gradient).clickable { onClick() }) {
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically, content = content)
    }
}

@Composable
fun GradientNavButton(modifier: Modifier, icon: String, text: String, gradient: Brush, height: Dp = 100.dp, onClick: () -> Unit) {
    Box(modifier = modifier.height(height).clip(RoundedCornerShape(20.dp)).background(gradient).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(icon, fontSize = 30.sp)
            Spacer(Modifier.height(6.dp))
            Text(text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun NavigationGrid(onNavigateToList: () -> Unit, onNavigateToCategories: () -> Unit, onNavigateToPhytochemistry: () -> Unit, onNavigateToBerries: () -> Unit, onNavigateToSearch: () -> Unit, onNavigateToIdentify: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GradientNavButton(Modifier.weight(1f), "📋", "Todas las Plantas", Brush.linearGradient(listOf(Color(0xFF1B3A1E), Color(0xFF2E5232))), 100.dp, onNavigateToList)
            GradientNavButton(Modifier.weight(1f), "🗂", "Categorías",        Brush.linearGradient(listOf(Color(0xFF1E4423), Color(0xFF2A5C30))), 100.dp, onNavigateToCategories)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GradientNavButton(Modifier.weight(1f), "🔬", "Fitoquímica", Brush.linearGradient(listOf(Color(0xFF2D6A30), Color(0xFF3D8841))), 100.dp, onNavigateToPhytochemistry)
            GradientNavButton(Modifier.weight(1f), "🫐", "Bayas",       Brush.linearGradient(listOf(Color(0xFF338034), Color(0xFF4A9E4C))), 100.dp, onNavigateToBerries)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GradientNavButton(Modifier.weight(1f), "🔍", "Buscar",      Brush.linearGradient(listOf(Color(0xFF4CAF50), Color(0xFF60C264))), 100.dp, onNavigateToSearch)
            GradientNavButton(Modifier.weight(1f), "📷", "Identificar", Brush.linearGradient(listOf(Color(0xFF57B85B), Color(0xFF72CC76))), 100.dp, onNavigateToIdentify)
        }
    }
}

@Composable
fun StatsRow(totalPlants: Int, mortalCount: Int, altoRiesgoCount: Int, familiesCount: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        StatCard("🌿", totalPlants.toString(), "Plantas", Color(0xFF4CAF50))
        StatCard("☠️", mortalCount.toString(), "Mortales", Color(0xFFEF5350))
        StatCard("⚠️", altoRiesgoCount.toString(), "Alto riesgo", Color(0xFFFFA726))
        StatCard("📚", familiesCount.toString(), "Familias", Color(0xFF81C784))
    }
}

@Composable
fun StatCard(emoji: String, value: String, label: String, color: Color) {
    Box(modifier = Modifier.size(width = 80.dp, height = 80.dp).clip(RoundedCornerShape(16.dp)).background(color.copy(alpha = 0.13f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 20.sp)
            Text(value, fontWeight = FontWeight.Bold, color = color, fontSize = 17.sp)
            Text(label, fontSize = 10.sp, color = Color.LightGray)
        }
    }
}

@Composable
fun SearchTypeDialog(onDismiss: () -> Unit, onSearchByName: () -> Unit, onSearchBySymptoms: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("🔍 ¿Cómo quieres buscar?", fontWeight = FontWeight.Bold) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DialogOptionCard(Brush.horizontalGradient(listOf(Color(0xFF1B5E20), Color(0xFF2E7D32))), "🔤", "Buscar por nombre", "Nombre común o científico", onSearchByName)
            DialogOptionCard(Brush.horizontalGradient(listOf(Color(0xFF2E7D32), Color(0xFF388E3C))), "🔬", "Buscar por síntomas", "Síntomas, toxicidad y categoría", onSearchBySymptoms)
        }}, confirmButton = {}, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

@Composable
fun IdentifyTypeDialog(onDismiss: () -> Unit, onIdentifyByCamera: () -> Unit, onIdentifyByAR: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("📷 ¿Cómo quieres identificar?", fontWeight = FontWeight.Bold) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DialogOptionCard(Brush.horizontalGradient(listOf(Color(0xFF1B5E20), Color(0xFF388E3C))), "📷", "Identificar por foto", "Cámara o galería con Pl@ntNet", onIdentifyByCamera)
            DialogOptionCard(Brush.horizontalGradient(listOf(Color(0xFF33691E), Color(0xFF558B2F))), "🎯", "AR Detección", "Realidad aumentada en tiempo real", onIdentifyByAR)
        }}, confirmButton = {}, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

@Composable
fun DialogOptionCard(gradient: Brush, icon: String, title: String, subtitle: String, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(gradient).clickable { onClick() }) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 28.sp)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(subtitle, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            }
        }
    }
}
