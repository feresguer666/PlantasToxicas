package com.toxicplants.database.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toxicplants.database.PlantDeletionStore
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.PlantMarkerStore
import com.toxicplants.database.RecentPlantStore
import com.toxicplants.database.ui.viewmodel.PlantViewModel

private fun PlantEntity.hasQualityIssue(): Boolean =
    imageUrl.isBlank() || scientificName.isBlank() || family.isBlank() || category.isBlank() ||
            description.isBlank() || toxicParts.isBlank() || symptoms.isBlank() ||
            firstAid.isBlank() || habitat.isBlank() || geographicDistribution.isBlank()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewCenterScreen(
    viewModel: PlantViewModel,
    onIncompletePlants: () -> Unit,
    onPlantsWithNotes: () -> Unit,
    onPlantsWithMarkers: () -> Unit,
    onRecentPlants: () -> Unit,
    onDeletedPlants: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val allPlants by viewModel.allPlants.observeAsState(emptyList())
    val markerMap = remember(allPlants) { PlantMarkerStore.loadAll(context) }
    val recentCount = remember(allPlants) { RecentPlantStore.load(context).size }
    val deletedCount = remember(allPlants) { PlantDeletionStore.load(context).size }

    val incompleteCount = remember(allPlants) { allPlants.count { it.hasQualityIssue() } }
    val noImageCount = remember(allPlants) { allPlants.count { it.imageUrl.isBlank() } }
    val notesCount = remember(allPlants) { allPlants.count { !it.notes.isNullOrBlank() } }
    val markerCount = markerMap.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("🧭 Centro de revisión", fontWeight = FontWeight.Bold)
                        Text("Mantenimiento del catálogo", fontSize = 12.sp, color = Color.White.copy(alpha = 0.82f))
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Resumen", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ReviewStatCard("🌿", allPlants.size.toString(), "Plantas", Color(0xFF2E7D32), Modifier.weight(1f))
                ReviewStatCard("⚠️", incompleteCount.toString(), "Incompletas", Color(0xFFF57C00), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ReviewStatCard("📷", noImageCount.toString(), "Sin imagen", Color(0xFF1565C0), Modifier.weight(1f))
                ReviewStatCard("📝", notesCount.toString(), "Con notas", Color(0xFF6A1B9A), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ReviewStatCard("🏷️", markerCount.toString(), "Marcadas", Color(0xFF00897B), Modifier.weight(1f))
                ReviewStatCard("🗑️", deletedCount.toString(), "Borradas", Color(0xFFD32F2F), Modifier.weight(1f))
            }

            Spacer(Modifier.height(6.dp))
            Text("Herramientas", fontWeight = FontWeight.Bold, fontSize = 18.sp)

            ReviewToolCard(
                icon = "🧹",
                title = "Plantas incompletas",
                subtitle = "Revisar fichas sin imagen, familia, síntomas o datos clave",
                color = Color(0xFFF57C00),
                onClick = onIncompletePlants
            )
            ReviewToolCard(
                icon = "📝",
                title = "Plantas con notas",
                subtitle = "Ver y buscar tus notas personales",
                color = Color(0xFF6A1B9A),
                onClick = onPlantsWithNotes
            )
            ReviewToolCard(
                icon = "🏷️",
                title = "Plantas con marcadores",
                subtitle = "Filtrar por Revisar, Pendiente foto, Interesante, etc.",
                color = Color(0xFF00897B),
                onClick = onPlantsWithMarkers
            )
            ReviewToolCard(
                icon = "🕘",
                title = "Vistas recientemente",
                subtitle = "Volver rápido a las últimas fichas abiertas",
                color = Color(0xFF1565C0),
                onClick = onRecentPlants
            )

            ReviewToolCard(
                icon = "🗑️",
                title = "Papelera de plantas",
                subtitle = "Restaurar fichas eliminadas manualmente",
                color = Color(0xFFD32F2F),
                onClick = onDeletedPlants
            )
        }
    }
}

@Composable
private fun ReviewStatCard(icon: String, value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 24.sp)
            Text(value, fontWeight = FontWeight.Bold, color = color, fontSize = 18.sp)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ReviewToolCard(icon: String, title: String, subtitle: String, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(10.dp), modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) { Text(icon, fontSize = 24.sp) }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
