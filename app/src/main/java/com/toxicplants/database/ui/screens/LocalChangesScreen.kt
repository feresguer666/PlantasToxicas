package com.toxicplants.database.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.ChevronRight
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
import com.toxicplants.database.CompoundDataSource
import com.toxicplants.database.CompoundEntity
import com.toxicplants.database.CompoundUserStateStore
import com.toxicplants.database.DuplicateReviewStore
import com.toxicplants.database.PlantDataSource
import com.toxicplants.database.PlantDeletionStore
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.PlantMarkerStore
import com.toxicplants.database.PlantUserEditStore
import com.toxicplants.database.ui.viewmodel.CompoundViewModel
import com.toxicplants.database.ui.viewmodel.PlantViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalChangesScreen(
    plantViewModel: PlantViewModel,
    compoundViewModel: CompoundViewModel,
    onPlantClick: (PlantEntity) -> Unit,
    onCompoundClick: (CompoundEntity) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val allPlants by plantViewModel.allPlants.observeAsState(emptyList())
    val allCompounds by compoundViewModel.allCompounds.observeAsState(emptyList())

    val seedPlants = remember { PlantDataSource.loadAll(context) }
    val seedCompounds = remember { CompoundDataSource.loadAll(context) }

    var refreshTick by remember { mutableIntStateOf(0) }
    val editedPlantIds = remember(refreshTick) { PlantUserEditStore.load(context) }
    val deletedPlantIds = remember(refreshTick) { PlantDeletionStore.load(context) }
    val markerMap = remember(refreshTick) { PlantMarkerStore.loadAll(context) }
    val reviewedDuplicateGroups = remember(refreshTick) { DuplicateReviewStore.load(context) }
    val editedCompoundIds = remember(refreshTick) { CompoundUserStateStore.loadEdited(context) }
    val deletedCompoundIds = remember(refreshTick) { CompoundUserStateStore.loadDeleted(context) }

    val plantById = remember(allPlants, seedPlants) { (seedPlants + allPlants).associateBy { it.id } }
    val compoundById = remember(allCompounds, seedCompounds) { (seedCompounds + allCompounds).associateBy { it.id } }

    val editedPlants = editedPlantIds.mapNotNull { plantById[it] }.sortedBy { it.commonName.lowercase() }
    val deletedPlants = deletedPlantIds.mapNotNull { plantById[it] }.sortedBy { it.commonName.lowercase() }
    val plantsWithNotes = allPlants.filter { !it.notes.isNullOrBlank() }.sortedBy { it.commonName.lowercase() }
    val plantsWithMarkers = markerMap.keys.mapNotNull { plantById[it] }.sortedBy { it.commonName.lowercase() }
    val editedCompounds = editedCompoundIds.mapNotNull { compoundById[it] }.sortedBy { it.commonName.lowercase() }
    val deletedCompounds = deletedCompoundIds.mapNotNull { compoundById[it] }.sortedBy { it.commonName.lowercase() }

    val totalChanges = editedPlantIds.size + deletedPlantIds.size + plantsWithNotes.size + markerMap.size +
            editedCompoundIds.size + deletedCompoundIds.size + reviewedDuplicateGroups.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("🛠️ Cambios locales", fontWeight = FontWeight.Bold)
                        Text("$totalChanges elementos registrados", fontSize = 12.sp, color = Color.White.copy(alpha = 0.82f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    TextButton(onClick = { refreshTick++ }) { Text("Actualizar", color = Color.White) }
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
            Text("Resumen antes de backup/sincronización", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ChangeStatCard("✏️", editedPlantIds.size.toString(), "Plantas editadas", Color(0xFF1565C0), Modifier.weight(1f))
                ChangeStatCard("🗑️", deletedPlantIds.size.toString(), "Plantas borradas", Color(0xFFD32F2F), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ChangeStatCard("📝", plantsWithNotes.size.toString(), "Con notas", Color(0xFF6A1B9A), Modifier.weight(1f))
                ChangeStatCard("🏷️", markerMap.size.toString(), "Con marcadores", Color(0xFF00897B), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ChangeStatCard("🧪", editedCompoundIds.size.toString(), "Comp. editados", Color(0xFF5E35B1), Modifier.weight(1f))
                ChangeStatCard("🧪🗑️", deletedCompoundIds.size.toString(), "Comp. borrados", Color(0xFFB71C1C), Modifier.weight(1f))
            }
            ChangeStatCard("🔁", reviewedDuplicateGroups.size.toString(), "Grupos duplicados revisados", Color(0xFF607D8B), Modifier.fillMaxWidth())

            ChangeSection(
                title = "✏️ Plantas editadas",
                emptyText = "No hay plantas editadas registradas.",
                items = editedPlants,
                itemTitle = { it.commonName },
                itemSubtitle = { "#${it.id} · ${it.scientificName}" },
                onClick = { plantViewModel.setDetailNavigationPlants(editedPlants); onPlantClick(it) }
            )

            ChangeSection(
                title = "🗑️ Plantas borradas",
                emptyText = "No hay plantas borradas.",
                items = deletedPlants,
                itemTitle = { it.commonName },
                itemSubtitle = { "#${it.id} · ${it.scientificName}" },
                onClick = null
            )

            ChangeSection(
                title = "📝 Plantas con notas",
                emptyText = "No hay notas personales.",
                items = plantsWithNotes,
                itemTitle = { it.commonName },
                itemSubtitle = { it.notes.orEmpty() },
                onClick = { plantViewModel.setDetailNavigationPlants(plantsWithNotes); onPlantClick(it) }
            )

            ChangeSection(
                title = "🏷️ Plantas con marcadores",
                emptyText = "No hay marcadores personales.",
                items = plantsWithMarkers,
                itemTitle = { it.commonName },
                itemSubtitle = { markerMap[it.id].orEmpty().sorted().joinToString(", ") },
                onClick = { plantViewModel.setDetailNavigationPlants(plantsWithMarkers); onPlantClick(it) }
            )

            ChangeSection(
                title = "🧪 Compuestos editados",
                emptyText = "No hay compuestos editados.",
                items = editedCompounds,
                itemTitle = { it.commonName },
                itemSubtitle = { "#${it.id} · ${it.groupName}" },
                onClick = { onCompoundClick(it) }
            )

            ChangeSection(
                title = "🧪🗑️ Compuestos borrados",
                emptyText = "No hay compuestos borrados.",
                items = deletedCompounds,
                itemTitle = { it.commonName },
                itemSubtitle = { "#${it.id} · ${it.groupName}" },
                onClick = null
            )

            if (reviewedDuplicateGroups.isNotEmpty()) {
                SimpleStringSection(
                    title = "🔁 Duplicados revisados",
                    items = reviewedDuplicateGroups.sorted()
                )
            }
        }
    }
}

@Composable
private fun ChangeStatCard(icon: String, value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 23.sp)
            Text(value, fontWeight = FontWeight.Bold, color = color, fontSize = 18.sp)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun <T> ChangeSection(
    title: String,
    emptyText: String,
    items: List<T>,
    itemTitle: (T) -> String,
    itemSubtitle: (T) -> String,
    onClick: ((T) -> Unit)?
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF2E7D32))
            Spacer(Modifier.height(8.dp))
            if (items.isEmpty()) {
                Text(emptyText, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                items.take(12).forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (onClick != null) Modifier.clickable { onClick(item) } else Modifier)
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(itemTitle(item), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Text(itemSubtitle(item), fontStyle = FontStyle.Italic, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        }
                        if (onClick != null) Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (items.size > 12) {
                    Text("… y ${items.size - 12} más", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SimpleStringSection(title: String, items: List<String>) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF2E7D32))
            Spacer(Modifier.height(8.dp))
            items.take(20).forEach { Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (items.size > 20) Text("… y ${items.size - 20} más", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
