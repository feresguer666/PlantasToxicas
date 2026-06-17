package com.toxicplants.database.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.ui.viewmodel.PlantViewModel

private enum class ScientificNameIssueFilter(val label: String) {
    All("Todos"),
    Empty("Vacíos"),
    TooShort("Muy cortos"),
    TooLong("Muy largos"),
    NoSpecies("Sin especie"),
    Spp("spp./sp."),
    NonToxicity("Non-Toxicity"),
    Digits("Con números"),
    WeirdChars("Símbolos raros"),
    Duplicate("Duplicados exactos")
}

private data class ScientificNameIssue(
    val plant: PlantEntity,
    val labels: List<ScientificNameIssueFilter>,
    val messages: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScientificNameReviewScreen(
    viewModel: PlantViewModel,
    onPlantClick: (PlantEntity) -> Unit,
    onEditPlant: (Int) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val allPlants by viewModel.allPlants.observeAsState(emptyList())
    var selectedFilter by remember { mutableStateOf(ScientificNameIssueFilter.All) }
    var query by remember { mutableStateOf("") }

    val issues = remember(allPlants) { buildScientificNameIssues(allPlants) }
    val visibleIssues = remember(issues, selectedFilter, query) {
        issues
            .filter { selectedFilter == ScientificNameIssueFilter.All || selectedFilter in it.labels }
            .filter { issue ->
                query.isBlank() ||
                    issue.plant.commonName.contains(query, ignoreCase = true) ||
                    issue.plant.scientificName.contains(query, ignoreCase = true) ||
                    issue.plant.family.contains(query, ignoreCase = true) ||
                    issue.messages.any { it.contains(query, ignoreCase = true) }
            }
            .sortedWith(compareBy<ScientificNameIssue> { it.plant.scientificName.lowercase() }.thenBy { it.plant.id })
    }

    val counts = remember(issues) {
        ScientificNameIssueFilter.entries.associateWith { filter ->
            if (filter == ScientificNameIssueFilter.All) issues.size else issues.count { filter in it.labels }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("🔬 Revisión de nombres", fontWeight = FontWeight.Bold)
                        Text(
                            "${visibleIssues.size} avisos · ${issues.size} plantas sospechosas",
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
                        label = { Text("Buscar aviso") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(ScientificNameIssueFilter.entries) { filter ->
                            val count = counts[filter] ?: 0
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = { Text("${filter.label} ($count)", fontSize = 12.sp) },
                                enabled = count > 0 || filter == ScientificNameIssueFilter.All,
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
                        Text("No hay nombres para este filtro", fontWeight = FontWeight.Bold)
                        Text("Prueba otro filtro o búsqueda", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(visibleIssues, key = { it.plant.id }) { issue ->
                        ScientificNameIssueCard(
                            issue = issue,
                            onOpen = {
                                viewModel.setDetailNavigationPlants(visibleIssues.map { it.plant })
                                onPlantClick(issue.plant)
                            },
                            onEdit = { onEditPlant(issue.plant.id) },
                            onGbif = {
                                val queryText = issue.plant.scientificName.ifBlank { issue.plant.commonName }
                                val url = "https://www.gbif.org/species/search?q=${Uri.encode(queryText)}"
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            },
                            onWikipedia = {
                                val queryText = issue.plant.scientificName.ifBlank { issue.plant.commonName }
                                val url = "https://es.wikipedia.org/wiki/${Uri.encode(queryText)}"
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScientificNameIssueCard(
    issue: ScientificNameIssue,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onGbif: () -> Unit,
    onWikipedia: () -> Unit
) {
    val plant = issue.plant
    val severityColor = when {
        ScientificNameIssueFilter.Empty in issue.labels -> Color(0xFFC62828)
        ScientificNameIssueFilter.NonToxicity in issue.labels -> Color(0xFFE65100)
        ScientificNameIssueFilter.WeirdChars in issue.labels -> Color(0xFFE65100)
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
                    Text("#${plant.id} · ${plant.commonName}", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        plant.scientificName.ifBlank { "Sin nombre científico" },
                        fontStyle = FontStyle.Italic,
                        color = severityColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (plant.family.isNotBlank()) {
                        Text(plant.family, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onOpen, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("Ver", fontSize = 12.sp)
                }
                OutlinedButton(onClick = onGbif, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("GBIF", fontSize = 12.sp)
                }
                OutlinedButton(onClick = onWikipedia, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("Wiki", fontSize = 12.sp)
                }
            }
        }
    }
}

private fun buildScientificNameIssues(plants: List<PlantEntity>): List<ScientificNameIssue> {
    val exactDuplicateNames = plants
        .mapNotNull { plant ->
            val key = normalizeExactScientificName(plant.scientificName)
            if (key.isBlank()) null else key to plant.id
        }
        .groupBy({ it.first }, { it.second })
        .filterValues { it.size > 1 }
        .keys

    return plants.mapNotNull { plant ->
        val labels = mutableListOf<ScientificNameIssueFilter>()
        val messages = mutableListOf<String>()
        val sci = plant.scientificName.trim()
        val lower = sci.lowercase()

        if (sci.isBlank()) {
            labels += ScientificNameIssueFilter.Empty
            messages += "Vacío"
        }
        if (sci.isNotBlank() && sci.length <= 2) {
            labels += ScientificNameIssueFilter.TooShort
            messages += "Muy corto"
        }
        if (sci.length > 80) {
            labels += ScientificNameIssueFilter.TooLong
            messages += "Muy largo"
        }
        val parts = sci.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (sci.isNotBlank() && parts.size < 2) {
            labels += ScientificNameIssueFilter.NoSpecies
            messages += "Sin especie"
        }
        if (lower.contains("spp") || lower.contains(" sp.")) {
            labels += ScientificNameIssueFilter.Spp
            messages += "spp./sp."
        }
        if (lower.contains("non-toxicity") || lower.contains("non-toxic")) {
            labels += ScientificNameIssueFilter.NonToxicity
            messages += "Texto Non-Toxicity"
        }
        if (sci.any { it.isDigit() }) {
            labels += ScientificNameIssueFilter.Digits
            messages += "Con números"
        }
        if (sci.contains(Regex("[:;|{}\\[\\]<>]") )) {
            labels += ScientificNameIssueFilter.WeirdChars
            messages += "Símbolos raros"
        }
        val exactKey = normalizeExactScientificName(sci)
        if (exactKey.isNotBlank() && exactKey in exactDuplicateNames) {
            labels += ScientificNameIssueFilter.Duplicate
            messages += "Duplicado exacto"
        }

        if (labels.isEmpty()) null else ScientificNameIssue(plant, labels.distinct(), messages.distinct())
    }
}

private fun normalizeExactScientificName(value: String): String =
    value
        .lowercase()
        .replace(Regex("\\s+"), " ")
        .trim()
