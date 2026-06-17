package com.toxicplants.database.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
fun PlantCompareScreen(
    viewModel: PlantViewModel,
    onPlantClick: (PlantEntity) -> Unit,
    onBack: () -> Unit
) {
    val allPlants by viewModel.allPlants.observeAsState(emptyList())
    var leftPlant by remember { mutableStateOf<PlantEntity?>(null) }
    var rightPlant by remember { mutableStateOf<PlantEntity?>(null) }
    var leftQuery by remember { mutableStateOf("") }
    var rightQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("⚖️ Comparador de plantas", fontWeight = FontWeight.Bold)
                        Text("Elige dos fichas para comparar", fontSize = 12.sp, color = Color.White.copy(alpha = 0.82f))
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
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PlantSelectorBox(
                title = "Planta A",
                query = leftQuery,
                selected = leftPlant,
                allPlants = allPlants,
                onQueryChange = {
                    leftQuery = it
                    leftPlant = null
                },
                onSelect = {
                    leftPlant = it
                    leftQuery = it.commonName
                }
            )

            PlantSelectorBox(
                title = "Planta B",
                query = rightQuery,
                selected = rightPlant,
                allPlants = allPlants,
                onQueryChange = {
                    rightQuery = it
                    rightPlant = null
                },
                onSelect = {
                    rightPlant = it
                    rightQuery = it.commonName
                }
            )

            if (leftPlant == null || rightPlant == null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🌿", fontSize = 42.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Selecciona dos plantas", fontWeight = FontWeight.Bold)
                        Text(
                            "Busca por nombre común o científico para ver la comparación.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                val a = leftPlant!!
                val b = rightPlant!!

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            viewModel.setDetailNavigationPlants(listOf(a, b))
                            onPlantClick(a)
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Abrir A") }
                    Button(
                        onClick = {
                            viewModel.setDetailNavigationPlants(listOf(a, b))
                            onPlantClick(b)
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Abrir B") }
                }

                CompareImages(a, b)
                CompareRow("Nombre común", a.commonName, b.commonName)
                CompareRow("Nombre científico", a.scientificName, b.scientificName, italic = true)
                CompareRow("Familia", a.family, b.family)
                CompareRow("Categoría", a.category, b.category)
                CompareRow("Toxicidad", a.toxicityLevel, b.toxicityLevel, emphasizeDifferent = true)
                CompareRow("Partes tóxicas", a.toxicParts, b.toxicParts)
                CompareRow("Síntomas", a.symptoms, b.symptoms)
                CompareRow("Primeros auxilios", a.firstAid, b.firstAid)
                CompareRow("Hábitat", a.habitat, b.habitat)
                CompareRow("Distribución", a.geographicDistribution, b.geographicDistribution)
                if (!a.notes.isNullOrBlank() || !b.notes.isNullOrBlank()) {
                    CompareRow("Mis notas", a.notes.orEmpty(), b.notes.orEmpty())
                }
            }
        }
    }
}

@Composable
private fun PlantSelectorBox(
    title: String,
    query: String,
    selected: PlantEntity?,
    allPlants: List<PlantEntity>,
    onQueryChange: (String) -> Unit,
    onSelect: (PlantEntity) -> Unit
) {
    val results = remember(query, allPlants, selected) {
        if (selected != null || query.length < 2) emptyList()
        else allPlants
            .filter {
                it.commonName.contains(query, ignoreCase = true) ||
                    it.scientificName.contains(query, ignoreCase = true) ||
                    it.commonNames.contains(query, ignoreCase = true)
            }
            .sortedBy { it.commonName.lowercase() }
            .take(8)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Buscar planta") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
            )
            if (selected != null) {
                Spacer(Modifier.height(6.dp))
                Text(selected.scientificName, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (results.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                results.forEach { plant ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(plant) }
                            .padding(vertical = 2.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                            Text(plant.commonName, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(plant.scientificName, fontStyle = FontStyle.Italic, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompareImages(a: PlantEntity, b: PlantEntity) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Fotos", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF2E7D32))
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("A", fontWeight = FontWeight.Bold)
                    PlantImageCard(plant = a, height = 160.dp, modifier = Modifier.fillMaxWidth())
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("B", fontWeight = FontWeight.Bold)
                    PlantImageCard(plant = b, height = 160.dp, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun CompareRow(
    label: String,
    left: String,
    right: String,
    italic: Boolean = false,
    emphasizeDifferent: Boolean = false
) {
    val same = left.trim().equals(right.trim(), ignoreCase = true)
    val color = when {
        same -> Color(0xFF2E7D32)
        emphasizeDifferent -> Color(0xFFE65100)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color, modifier = Modifier.weight(1f))
                Text(if (same) "Igual" else "Diferente", fontSize = 11.sp, color = color)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                CompareValue("A", left, italic, Modifier.weight(1f))
                CompareValue("B", right, italic, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CompareValue(label: String, value: String, italic: Boolean, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
        Text(
            value.ifBlank { "Sin datos" },
            fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
            fontSize = 13.sp,
            color = if (value.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
        )
    }
}
