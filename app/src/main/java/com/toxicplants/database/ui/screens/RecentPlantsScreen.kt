package com.toxicplants.database.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.RecentPlantStore
import com.toxicplants.database.ui.viewmodel.PlantViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentPlantsScreen(
    viewModel: PlantViewModel,
    onPlantClick: (PlantEntity) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val allPlants by viewModel.allPlants.observeAsState(emptyList())
    var recentEntries by remember { mutableStateOf(RecentPlantStore.load(context)) }

    val recentPlants = remember(allPlants, recentEntries) {
        val byId = allPlants.associateBy { it.id }
        recentEntries.mapNotNull { entry ->
            byId[entry.plantId]?.let { plant -> entry to plant }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("🕘 Vistas recientemente", fontWeight = FontWeight.Bold)
                        Text(
                            "${recentPlants.size} fichas",
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
                actions = {
                    IconButton(
                        enabled = recentEntries.isNotEmpty(),
                        onClick = {
                            RecentPlantStore.clear(context)
                            recentEntries = emptyList()
                            viewModel.clearDetailNavigationPlants()
                        }
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Limpiar historial",
                            tint = if (recentEntries.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.35f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2E7D32),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (allPlants.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF2E7D32))
            }
        } else if (recentPlants.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🕘", fontSize = 48.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("No hay historial todavía", fontWeight = FontWeight.Bold)
                    Text("Abre fichas de plantas y aparecerán aquí", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(recentPlants, key = { it.first.plantId }) { (entry, plant) ->
                    RecentPlantCard(
                        plant = plant,
                        viewedAt = entry.viewedAt,
                        onClick = {
                            viewModel.setDetailNavigationPlants(recentPlants.map { it.second })
                            onPlantClick(plant)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentPlantCard(
    plant: PlantEntity,
    viewedAt: Long,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color(0xFF2E7D32).copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("🕘", fontSize = 22.sp)
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
                    plant.scientificName,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Visto: ${formatRecentTimestamp(viewedAt)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatRecentTimestamp(timestamp: Long): String {
    if (timestamp <= 0L) return "fecha desconocida"
    return SimpleDateFormat("d/MM/yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
}
