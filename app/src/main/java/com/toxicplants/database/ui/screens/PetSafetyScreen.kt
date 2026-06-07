package com.toxicplants.database.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.toxicplants.database.PlantExtraDataSource
import com.toxicplants.database.ui.theme.carbonEffectSubtle
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.ui.viewmodel.PlantViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetSafetyScreen(
    viewModel: PlantViewModel,
    onPlantClick: (PlantEntity) -> Unit,
    onBack: () -> Unit,
    onAddPlant: (String) -> Unit = {},
    onEditPlant: (Int) -> Unit = {},

) {
    val context   = LocalContext.current
    val allPlants by viewModel.allPlants.observeAsState(emptyList())
    val extraMap  = remember { PlantExtraDataSource.loadAll(context).toMutableMap() }
    var selectedTab  by remember { mutableIntStateOf(0) }
    var refreshKey        by remember { mutableIntStateOf(0) }
    var showAnimalDialog  by remember { mutableStateOf(false) }

    if (showAnimalDialog) {
        AlertDialog(
            onDismissRequest = { showAnimalDialog = false },
            title = { Text("🐾 ¿Para qué mascota?", fontWeight = FontWeight.Bold) },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("🐕 Perros" to "dogs", "🐈 Gatos" to "cats").forEach { (label, mode) ->
                        TextButton(
                            onClick  = { showAnimalDialog = false; onAddPlant(mode) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(label, fontSize = 16.sp) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showAnimalDialog = false }) { Text("Cancelar") } }
        )
    }
    val tabs = listOf("🐕 Perros", "🐈 Gatos", "🐾 Ambos")

    val filteredPlants = remember(allPlants, extraMap, selectedTab, refreshKey) {
        allPlants.filter { plant ->
            val extra = extraMap[plant.scientificName] ?: return@filter false
            when (selectedTab) {
                0 -> extra.toxicDogs
                1 -> extra.toxicCats
                2 -> extra.toxicDogs && extra.toxicCats
                else -> false
            }
        }.sortedByDescending { when (it.toxicityLevel) { "Mortal" -> 5; "Muy alto" -> 4; "Alto" -> 3; "Moderado" -> 2; "Bajo" -> 1; else -> 0 } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("🐾 Plantas peligrosas para mascotas", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                        Text("${filteredPlants.size} plantas encontradas", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White) } },
                actions = { IconButton(onClick = { showAnimalDialog = true }) { Icon(Icons.Default.Add, contentDescription = "Añadir", tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF4A148C))
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().carbonEffectSubtle().padding(paddingValues)) {
            TabRow(selectedTabIndex = selectedTab, containerColor = Color(0xFF4A148C), contentColor = Color.White) {
                tabs.forEachIndexed { index, title ->
                    Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title, fontSize = 13.sp) })
                }
            }
            Card(modifier = Modifier.fillMaxWidth().padding(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("⚠️", fontSize = 24.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("Si tu mascota ingirió alguna de estas plantas llama urgentemente al veterinario. Lleva una muestra o foto de la planta.", fontSize = 12.sp, color = Color(0xFF4A148C))
                }
            }
            LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredPlants) { plant ->
                    val extra = extraMap[plant.scientificName]
                    val toxColor = when (plant.toxicityLevel) { "Mortal" -> Color(0xFFB71C1C); "Muy alto" -> Color(0xFFFF5722); "Alto" -> Color(0xFFE65100); "Moderado" -> Color(0xFFF57C00); "Bajo" -> Color(0xFF388E3C); else -> Color.Gray }
                    Card(modifier = Modifier.fillMaxWidth().clickable { onPlantClick(plant) }, elevation = CardDefaults.cardElevation(2.dp)) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(50.dp).background(toxColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                Text(when (plant.toxicityLevel) { "Mortal" -> "☠️"; "Muy alto" -> "💀"; "Alto" -> "⚠️"; "Moderado" -> "⚡"; "Bajo" -> "🟢"; else -> "❓" }, fontSize = 22.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(plant.commonName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(plant.scientificName, fontSize = 12.sp, fontStyle = FontStyle.Italic, color = Color.Gray, maxLines = 1)
                                Spacer(Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (extra?.toxicDogs == true) Surface(color = Color(0xFF795548).copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) { Text("🐕 Perros", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, color = Color(0xFF795548), fontWeight = FontWeight.Bold) }
                                    if (extra?.toxicCats == true) Surface(color = Color(0xFF5C6BC0).copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) { Text("🐈 Gatos", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, color = Color(0xFF5C6BC0), fontWeight = FontWeight.Bold) }
                                    Surface(color = toxColor.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) { Text(plant.toxicityLevel, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, color = toxColor, fontWeight = FontWeight.Bold) }
                                }
                            }
                            // Botón quitar
                            IconButton(
                                onClick  = {
                                    when (selectedTab) {
                                        0 -> PlantExtraDataSource.setToxicDogs(context, plant.scientificName, false)
                                        1 -> PlantExtraDataSource.setToxicCats(context, plant.scientificName, false)
                                        2 -> { PlantExtraDataSource.setToxicDogs(context, plant.scientificName, false); PlantExtraDataSource.setToxicCats(context, plant.scientificName, false) }
                                    }
                                    PlantExtraDataSource.clearCache()
                                    extraMap.clear()
                                    extraMap.putAll(PlantExtraDataSource.loadAll(context))
                                    refreshKey++
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Quitar", tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}
