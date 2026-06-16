package com.toxicplants.database.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
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
fun PlantsWithNotesScreen(
    viewModel: PlantViewModel,
    onPlantClick: (PlantEntity) -> Unit,
    onBack: () -> Unit
) {
    val allPlants by viewModel.allPlants.observeAsState(emptyList())
    var query by remember { mutableStateOf("") }

    val plantsWithNotes = remember(allPlants, query) {
        allPlants
            .filter { !it.notes.isNullOrBlank() }
            .filter { plant ->
                query.isBlank() ||
                    plant.commonName.contains(query, ignoreCase = true) ||
                    plant.scientificName.contains(query, ignoreCase = true) ||
                    plant.notes.orEmpty().contains(query, ignoreCase = true)
            }
            .sortedBy { it.commonName.lowercase() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("📝 Plantas con notas", fontWeight = FontWeight.Bold)
                        Text(
                            "${plantsWithNotes.size} de ${allPlants.count { !it.notes.isNullOrBlank() }} con notas",
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
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    singleLine = true,
                    label = { Text("Buscar en notas") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                )
            }

            if (allPlants.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF2E7D32))
                }
            } else if (plantsWithNotes.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.AutoMirrored.Filled.Notes,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (query.isBlank()) "No hay plantas con notas" else "Sin resultados",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (query.isBlank()) "Añade notas desde una ficha de planta" else "Prueba otro texto",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(plantsWithNotes, key = { it.id }) { plant ->
                        PlantWithNoteCard(
                            plant = plant,
                            onClick = {
                                viewModel.setDetailNavigationPlants(plantsWithNotes)
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
private fun PlantWithNoteCard(
    plant: PlantEntity,
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
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                color = Color(0xFF2E7D32).copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Filled.Notes,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32)
                    )
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
                Spacer(Modifier.height(8.dp))
                Text(
                    plant.notes.orEmpty(),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
