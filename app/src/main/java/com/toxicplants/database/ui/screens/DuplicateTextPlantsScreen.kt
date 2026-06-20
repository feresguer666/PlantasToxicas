package com.toxicplants.database.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
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
import java.text.Normalizer

private enum class DuplicateTextFilter(val label: String, val fieldLabel: String? = null) {
    All("Todos"),
    Description("Descripción", "Descripción"),
    Symptoms("Síntomas", "Síntomas"),
    FirstAid("Primeros auxilios", "Primeros auxilios"),
    ToxicParts("Partes tóxicas", "Partes tóxicas"),
    Habitat("Hábitat", "Hábitat"),
    Distribution("Distribución", "Distribución"),
    Myths("Mitos", "Mitos")
}

private data class DuplicateTextIssue(
    val plant: PlantEntity,
    val filter: DuplicateTextFilter,
    val fieldLabel: String,
    val repeatedFragment: String,
    val occurrences: Int,
    val preview: String
)

private data class DuplicateTextField(
    val filter: DuplicateTextFilter,
    val label: String,
    val getter: PlantEntity.() -> String
)

private val duplicateTextFields = listOf(
    DuplicateTextField(DuplicateTextFilter.Description, "Descripción", { description }),
    DuplicateTextField(DuplicateTextFilter.Symptoms, "Síntomas", { symptoms }),
    DuplicateTextField(DuplicateTextFilter.FirstAid, "Primeros auxilios", { firstAid }),
    DuplicateTextField(DuplicateTextFilter.ToxicParts, "Partes tóxicas", { toxicParts }),
    DuplicateTextField(DuplicateTextFilter.Habitat, "Hábitat", { habitat }),
    DuplicateTextField(DuplicateTextFilter.Distribution, "Distribución", { geographicDistribution }),
    DuplicateTextField(DuplicateTextFilter.Myths, "Mitos", { mythsAndLegends })
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicateTextPlantsScreen(
    viewModel: PlantViewModel,
    onPlantClick: (PlantEntity) -> Unit,
    onEditPlant: (Int) -> Unit,
    onBack: () -> Unit
) {
    val allPlants by viewModel.allPlants.observeAsState(emptyList())
    var selectedFilter by remember { mutableStateOf(DuplicateTextFilter.All) }
    var query by remember { mutableStateOf("") }

    val allIssues = remember(allPlants) { buildDuplicateTextIssues(allPlants) }
    val visibleIssues = remember(allIssues, selectedFilter, query) {
        allIssues
            .filter { selectedFilter == DuplicateTextFilter.All || it.filter == selectedFilter }
            .filter { issue ->
                query.isBlank() ||
                    issue.plant.commonName.contains(query, ignoreCase = true) ||
                    issue.plant.scientificName.contains(query, ignoreCase = true) ||
                    issue.fieldLabel.contains(query, ignoreCase = true) ||
                    issue.repeatedFragment.contains(query, ignoreCase = true) ||
                    issue.preview.contains(query, ignoreCase = true)
            }
            .sortedWith(compareBy<DuplicateTextIssue> { it.plant.commonName.lowercase() }.thenBy { it.fieldLabel })
    }

    val counts = remember(allIssues) {
        DuplicateTextFilter.entries.associateWith { filter ->
            if (filter == DuplicateTextFilter.All) allIssues.size else allIssues.count { it.filter == filter }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("📄 Texto duplicado", fontWeight = FontWeight.Bold)
                        Text(
                            "${visibleIssues.size} avisos · ${allIssues.map { it.plant.id }.distinct().size} plantas",
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
                Column {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        singleLine = true,
                        label = { Text("Buscar texto duplicado") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(DuplicateTextFilter.entries) { filter ->
                            val count = counts[filter] ?: 0
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = { Text("${filter.label} ($count)", fontSize = 12.sp) },
                                enabled = count > 0 || filter == DuplicateTextFilter.All,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF2E7D32),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            if (allPlants.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF2E7D32))
                }
            } else if (visibleIssues.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✅", fontSize = 48.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("No hay texto duplicado para este filtro", fontWeight = FontWeight.Bold)
                        Text("Prueba otro filtro o búsqueda", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(visibleIssues, key = { "${it.plant.id}:${it.fieldLabel}:${it.repeatedFragment.hashCode()}" }) { issue ->
                        DuplicateTextIssueCard(
                            issue = issue,
                            onOpen = {
                                viewModel.setDetailNavigationPlants(visibleIssues.map { it.plant }.distinctBy { it.id })
                                onPlantClick(issue.plant)
                            },
                            onEdit = { onEditPlant(issue.plant.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DuplicateTextIssueCard(
    issue: DuplicateTextIssue,
    onOpen: () -> Unit,
    onEdit: () -> Unit
) {
    val severityColor = when (issue.filter) {
        DuplicateTextFilter.Description -> Color(0xFF1565C0)
        DuplicateTextFilter.Symptoms -> Color(0xFFE65100)
        DuplicateTextFilter.FirstAid -> Color(0xFF2E7D32)
        DuplicateTextFilter.ToxicParts -> Color(0xFFC62828)
        else -> Color(0xFF6A1B9A)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("#${issue.plant.id} · ${issue.plant.commonName}", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(issue.plant.scientificName, fontStyle = FontStyle.Italic, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(issue.fieldLabel, fontSize = 12.sp, color = severityColor, fontWeight = FontWeight.SemiBold)
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color(0xFF2E7D32))
                }
            }

            Spacer(Modifier.height(6.dp))
            Surface(color = severityColor.copy(alpha = 0.12f), shape = RoundedCornerShape(4.dp)) {
                Text(
                    "Repetido ${issue.occurrences} veces: ${issue.repeatedFragment}",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    fontSize = 11.sp,
                    color = severityColor,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(issue.preview, fontSize = 13.sp, maxLines = 6, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onOpen, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("Ver", fontSize = 12.sp)
                }
                Button(onClick = onEdit, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) {
                    Text("Editar", fontSize = 12.sp)
                }
            }
        }
    }
}

private fun buildDuplicateTextIssues(plants: List<PlantEntity>): List<DuplicateTextIssue> {
    val out = mutableListOf<DuplicateTextIssue>()
    for (plant in plants) {
        for (field in duplicateTextFields) {
            val value = field.getter(plant).trim()
            val duplicate = findDuplicateFragment(value) ?: continue
            out += DuplicateTextIssue(
                plant = plant,
                filter = field.filter,
                fieldLabel = field.label,
                repeatedFragment = duplicate.first,
                occurrences = duplicate.second,
                preview = value
            )
        }
    }
    return out
}

private fun findDuplicateFragment(value: String): Pair<String, Int>? {
    if (value.length < 40) return null

    // 1) Frases o cláusulas repetidas.
    val chunks = value
        .split(Regex("[.!?;\\n]+"))
        .flatMap { it.split(Regex("\\s*,\\s*")) }
        .map { it.trim() }
        .filter { it.length >= 22 }

    val grouped = chunks.groupBy { normalizeTextDuplicateKey(it) }
        .filterKeys { it.isNotBlank() }
        .filterValues { it.size > 1 }

    val best = grouped.maxByOrNull { it.value.size }
    if (best != null) {
        return best.value.first().take(140) to best.value.size
    }

    // 2) Texto compuesto por dos mitades iguales o casi iguales.
    val normalized = normalizeTextDuplicateKey(value)
    if (normalized.length >= 60) {
        val half = normalized.length / 2
        val left = normalized.take(half).trim()
        val right = normalized.drop(half).trim()
        if (left.length >= 30 && (left == right || left in right || right in left)) {
            return value.take(value.length / 2).trim().take(140) to 2
        }
    }

    // 3) Repetición clara de bloques separados por doble espacio o punto y coma largos.
    val blockParts = value
        .split(Regex("\\s{2,}|;"))
        .map { it.trim() }
        .filter { it.length >= 35 }
    val blockGrouped = blockParts.groupBy { normalizeTextDuplicateKey(it) }
        .filterKeys { it.isNotBlank() }
        .filterValues { it.size > 1 }
    val blockBest = blockGrouped.maxByOrNull { it.value.size }
    if (blockBest != null) {
        return blockBest.value.first().take(140) to blockBest.value.size
    }

    return null
}

private fun normalizeTextDuplicateKey(value: String): String {
    val noAccents = Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
    return noAccents
        .replace(Regex("[^a-z0-9 ]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}
