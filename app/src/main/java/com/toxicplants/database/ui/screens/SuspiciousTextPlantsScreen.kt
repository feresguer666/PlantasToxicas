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

private enum class SuspiciousTextFilter(val label: String) {
    All("Todos"),
    English("Inglés"),
    Placeholder("Placeholder"),
    Html("HTML"),
    TooShort("Muy corto"),
    TooManyUrls("Muchas URLs"),
    WeirdTokens("Texto raro")
}

private data class SuspiciousTextIssue(
    val plant: PlantEntity,
    val fieldName: String,
    val fieldLabel: String,
    val filters: Set<SuspiciousTextFilter>,
    val messages: List<String>,
    val preview: String
)

private val textFieldsToInspect: List<Pair<String, PlantEntity.() -> String>> = listOf(
    "Nombre común" to { commonName },
    "Nombre científico" to { scientificName },
    "Descripción" to { description },
    "Síntomas" to { symptoms },
    "Primeros auxilios" to { firstAid },
    "Partes tóxicas" to { toxicParts },
    "Hábitat" to { habitat },
    "Distribución" to { geographicDistribution },
    "Categoría" to { category },
    "Mitos" to { mythsAndLegends }
)

private val englishSignals = listOf(
    "non-toxic", "non toxicity", "non-toxicity", "to dogs", "to cats", "to horses",
    "poisonous", "poisoning", "ingestion", "skin irritation", "native to", "may cause",
    "toxic to", "warning", "symptoms include", "do not", "contact with", "plant care",
    "houseplant", "edible", "not edible"
)

private val placeholderSignals = listOf(
    "unknown", "desconocido", "n/a", "na", "no data", "no disponible", "sin datos",
    "lorem ipsum", "todo", "pendiente", "por completar", "null", "none"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuspiciousTextPlantsScreen(
    viewModel: PlantViewModel,
    onPlantClick: (PlantEntity) -> Unit,
    onEditPlant: (Int) -> Unit,
    onBack: () -> Unit
) {
    val allPlants by viewModel.allPlants.observeAsState(emptyList())
    var selectedFilter by remember { mutableStateOf(SuspiciousTextFilter.All) }
    var query by remember { mutableStateOf("") }

    val allIssues = remember(allPlants) { buildSuspiciousTextIssues(allPlants) }
    val visibleIssues = remember(allIssues, selectedFilter, query) {
        allIssues
            .filter { selectedFilter == SuspiciousTextFilter.All || selectedFilter in it.filters }
            .filter { issue ->
                query.isBlank() ||
                    issue.plant.commonName.contains(query, ignoreCase = true) ||
                    issue.plant.scientificName.contains(query, ignoreCase = true) ||
                    issue.fieldLabel.contains(query, ignoreCase = true) ||
                    issue.preview.contains(query, ignoreCase = true) ||
                    issue.messages.any { it.contains(query, ignoreCase = true) }
            }
            .sortedWith(compareBy<SuspiciousTextIssue> { it.plant.commonName.lowercase() }.thenBy { it.fieldLabel })
    }

    val counts = remember(allIssues) {
        SuspiciousTextFilter.entries.associateWith { filter ->
            if (filter == SuspiciousTextFilter.All) allIssues.size else allIssues.count { filter in it.filters }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("🧯 Texto sospechoso", fontWeight = FontWeight.Bold)
                        Text("${visibleIssues.size} avisos · ${allIssues.map { it.plant.id }.distinct().size} plantas", fontSize = 12.sp, color = Color.White.copy(alpha = 0.82f))
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
                        label = { Text("Buscar texto sospechoso") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(SuspiciousTextFilter.entries) { filter ->
                            val count = counts[filter] ?: 0
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = { Text("${filter.label} ($count)", fontSize = 12.sp) },
                                enabled = count > 0 || filter == SuspiciousTextFilter.All,
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
                        Text("No hay avisos para este filtro", fontWeight = FontWeight.Bold)
                        Text("Prueba otro filtro o búsqueda", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(visibleIssues, key = { "${it.plant.id}:${it.fieldName}:${it.preview.hashCode()}" }) { issue ->
                        SuspiciousTextIssueCard(
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
private fun SuspiciousTextIssueCard(
    issue: SuspiciousTextIssue,
    onOpen: () -> Unit,
    onEdit: () -> Unit
) {
    val severityColor = when {
        SuspiciousTextFilter.English in issue.filters -> Color(0xFFE65100)
        SuspiciousTextFilter.Html in issue.filters -> Color(0xFFC62828)
        SuspiciousTextFilter.WeirdTokens in issue.filters -> Color(0xFFC62828)
        else -> Color(0xFF1565C0)
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
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                issue.messages.forEach { msg ->
                    Surface(color = severityColor.copy(alpha = 0.12f), shape = RoundedCornerShape(4.dp)) {
                        Text(msg, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, color = severityColor)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(issue.preview, fontSize = 13.sp, maxLines = 5, overflow = TextOverflow.Ellipsis)
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

private fun buildSuspiciousTextIssues(plants: List<PlantEntity>): List<SuspiciousTextIssue> {
    val out = mutableListOf<SuspiciousTextIssue>()
    for (plant in plants) {
        for ((label, getter) in textFieldsToInspect) {
            val value = plant.getter().trim()
            val issue = inspectTextField(plant, label, value)
            if (issue != null) out += issue
        }
    }
    return out
}

private fun inspectTextField(plant: PlantEntity, label: String, value: String): SuspiciousTextIssue? {
    if (value.isBlank()) return null
    val lower = value.lowercase()
    val filters = mutableSetOf<SuspiciousTextFilter>()
    val messages = mutableListOf<String>()

    val englishHits = englishSignals.filter { lower.contains(it) }
    if (englishHits.isNotEmpty()) {
        filters += SuspiciousTextFilter.English
        messages += "Inglés: ${englishHits.take(2).joinToString(", ")}"
    }

    val placeholderHits = placeholderSignals.filter { lower == it || lower.contains(it) }
    if (placeholderHits.isNotEmpty()) {
        filters += SuspiciousTextFilter.Placeholder
        messages += "Placeholder"
    }

    if (lower.contains("<br") || lower.contains("<p") || lower.contains("</") || lower.contains("&nbsp;")) {
        filters += SuspiciousTextFilter.Html
        messages += "HTML"
    }

    if (value.length <= 2 && label !in listOf("Categoría")) {
        filters += SuspiciousTextFilter.TooShort
        messages += "Muy corto"
    }

    val urlCount = Regex("https?://|www\\.").findAll(lower).count()
    if (urlCount >= 2) {
        filters += SuspiciousTextFilter.TooManyUrls
        messages += "Muchas URLs"
    }

    if (lower.contains("non-toxicity") || lower.contains("non-toxic to") || lower.contains("to dogs, non-toxic")) {
        filters += SuspiciousTextFilter.WeirdTokens
        messages += "Texto de base externa"
    }
    if (value.count { it == ':' } >= 3 || value.count { it == '|' } >= 2) {
        filters += SuspiciousTextFilter.WeirdTokens
        messages += "Separadores raros"
    }

    return if (filters.isEmpty()) null
    else SuspiciousTextIssue(
        plant = plant,
        fieldName = label,
        fieldLabel = label,
        filters = filters,
        messages = messages.distinct(),
        preview = value
    )
}
