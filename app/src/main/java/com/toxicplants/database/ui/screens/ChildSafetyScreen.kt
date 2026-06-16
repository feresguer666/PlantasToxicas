package com.toxicplants.database.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
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
fun ChildSafetyScreen(
    viewModel: PlantViewModel,
    onPlantClick: (PlantEntity) -> Unit,
    onBack: () -> Unit,
    onAddPlant: () -> Unit = {},
    onEditPlant: (Int) -> Unit = {}
) {
    val context   = LocalContext.current
    val allPlants by viewModel.allPlants.observeAsState(emptyList())
    val extraMap  = remember { PlantExtraDataSource.loadAll(context).toMutableMap() }

    var refreshKey by remember { mutableIntStateOf(0) }
    val filteredPlants = remember(allPlants, extraMap, refreshKey) {
        allPlants.filter { extraMap[it.scientificName]?.toxicChildren == true }
            .sortedByDescending { when (it.toxicityLevel) { "Mortal" -> 5; "Muy alto" -> 4; "Alto" -> 3; "Moderado" -> 2; "Bajo" -> 1; else -> 0 } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("👶 Peligrosas para niños", fontWeight = FontWeight.Bold, color = Color.White)
                        Text("${filteredPlants.size} plantas · Niños < 12 años", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White) } },
                actions = {
                    IconButton(onClick = onAddPlant) {
                        Icon(Icons.Default.Add, contentDescription = "Añadir planta", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFE65100))
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().carbonEffectSubtle().padding(paddingValues)) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF050505), contentColor = Color.White)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("👶", fontSize = 28.sp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Plantas peligrosas para niños menores de 12 años", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                        Spacer(Modifier.height(4.dp))
                        Text("En caso de ingesta llama al 112 o al Centro de Información Toxicológica: 91 562 04 20", fontSize = 11.sp, color = Color(0xFFFFCCBC))
                    }
                }
            }
            LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredPlants) { plant ->
                    val toxColor = when (plant.toxicityLevel) { "Mortal" -> Color(0xFFB71C1C); "Muy alto" -> Color(0xFFFF5722); "Alto" -> Color(0xFFE65100); "Moderado" -> Color(0xFFF57C00); "Bajo" -> Color(0xFF388E3C); else -> Color.Gray }
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onPlantClick(plant) },
                        elevation = CardDefaults.cardElevation(2.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF050505), contentColor = Color.White)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(50.dp).background(toxColor.copy(alpha = 0.24f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                Text(when (plant.toxicityLevel) { "Mortal" -> "☠️"; "Muy alto" -> "💀"; "Alto" -> "⚠️"; "Moderado" -> "⚡"; "Bajo" -> "🟢"; else -> "❓" }, fontSize = 22.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(plant.commonName, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(plant.scientificName, fontSize = 12.sp, fontStyle = FontStyle.Italic, color = Color(0xFFBDBDBD), maxLines = 1)
                                Spacer(Modifier.height(4.dp))
                                Text("🚑 ${plant.firstAid.take(80)}${if (plant.firstAid.length > 80) "…" else ""}", fontSize = 11.sp, color = Color(0xFFE0E0E0))
                            }
                            Spacer(Modifier.width(8.dp))
                            Surface(color = toxColor.copy(alpha = 0.24f), shape = RoundedCornerShape(4.dp)) {
                                Text(plant.toxicityLevel, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp), fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            IconButton(
                                onClick = {
                                    PlantExtraDataSource.setToxicChildren(context, plant.scientificName, false)
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
