package com.toxicplants.database.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
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
import com.toxicplants.database.CompoundEntity
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.ui.viewmodel.CompoundViewModel
import com.toxicplants.database.ui.viewmodel.PlantViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchScreen(
    plantViewModel: PlantViewModel,
    compoundViewModel: CompoundViewModel,
    initialQuery: String = "",
    onPlantClick: (PlantEntity) -> Unit,
    onCompoundClick: (CompoundEntity) -> Unit,
    onBack: () -> Unit,
) {
    val allPlants by plantViewModel.allPlants.observeAsState(emptyList())
    val allCompounds by compoundViewModel.allCompounds.observeAsState(emptyList())
    val colors = MaterialTheme.colorScheme

    var query by remember { mutableStateOf(initialQuery) }
    var selectedFilter by remember { mutableStateOf(GlobalSearchFilter.All) }

    // ── Resultados filtrados ──────────────────────────────────────
    val plantResults = remember(query, allPlants) {
        if (query.length < 2) emptyList()
        else {
            val q = query.trim()
            allPlants.filter {
                it.commonName.contains(q, ignoreCase = true) ||
                        it.scientificName.contains(q, ignoreCase = true) ||
                        it.family.contains(q, ignoreCase = true) ||
                        it.symptoms.contains(q, ignoreCase = true) ||
                        it.toxicParts.contains(q, ignoreCase = true) ||
                        it.description.contains(q, ignoreCase = true) ||
                        it.firstAid.contains(q, ignoreCase = true)
            }
        }
    }

    val compoundResults = remember(query, allCompounds) {
        if (query.length < 2) emptyList()
        else {
            val q = query.trim()
            allCompounds.filter {
                it.commonName.contains(q, ignoreCase = true) ||
                        it.iupacName.contains(q, ignoreCase = true) ||
                        it.groupName.contains(q, ignoreCase = true) ||
                        it.subgroup.contains(q, ignoreCase = true) ||
                        it.mechanism.contains(q, ignoreCase = true) ||
                        it.sourcePlants.contains(q, ignoreCase = true) ||
                        it.clinicalNeuro.contains(q, ignoreCase = true) ||
                        it.clinicalCardio.contains(q, ignoreCase = true) ||
                        it.clinicalDigestive.contains(q, ignoreCase = true)
            }
        }
    }

    val familyResults = remember(query, allPlants) {
        if (query.length < 2) emptyList()
        else {
            val q = query.trim()
            allPlants.map { it.family }.filter { it.isNotBlank() }.distinct()
                .filter { it.contains(q, ignoreCase = true) }
                .sorted()
        }
    }

    val showPlants = selectedFilter == GlobalSearchFilter.All || selectedFilter == GlobalSearchFilter.Plants
    val showCompounds = selectedFilter == GlobalSearchFilter.All || selectedFilter == GlobalSearchFilter.Compounds
    val showFamilies = selectedFilter == GlobalSearchFilter.All || selectedFilter == GlobalSearchFilter.Families

    val visiblePlantResults = if (showPlants) plantResults else emptyList()
    val visibleCompoundResults = if (showCompounds) compoundResults else emptyList()
    val visibleFamilyResults = if (showFamilies) familyResults else emptyList()
    val totalResults = visiblePlantResults.size + visibleCompoundResults.size + visibleFamilyResults.size

    Column(modifier = Modifier.fillMaxSize()) {
        // ── TopBar ───────────────────────────────────────────────
        Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSecondaryContainer) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.Black)
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Buscar plantas, compuestos, síntomas…", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Filled.Search, null, tint = Color.Black.copy(alpha = 0.7f)) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Filled.Clear, "Limpiar", tint = Color.Black.copy(alpha = 0.7f))
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        cursorColor = Color.Black,
                        focusedBorderColor = Color.Black.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.Black.copy(alpha = 0.3f),
                        focusedPlaceholderColor = Color.Black.copy(alpha = 0.5f),
                        unfocusedPlaceholderColor = Color.Black.copy(alpha = 0.5f),
                    ),
                    shape = RoundedCornerShape(24.dp),
                )
                Spacer(Modifier.width(4.dp))
            }
        }

        // ── Filtros ──────────────────────────────────────────────
        Surface(color = colors.surface, modifier = Modifier.fillMaxWidth()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(GlobalSearchFilter.entries) { filter ->
                    val count = when (filter) {
                        GlobalSearchFilter.All -> plantResults.size + compoundResults.size + familyResults.size
                        GlobalSearchFilter.Plants -> plantResults.size
                        GlobalSearchFilter.Compounds -> compoundResults.size
                        GlobalSearchFilter.Families -> familyResults.size
                    }
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text("${filter.label} ($count)", fontSize = 12.sp) },
                        leadingIcon = { Text(filter.icon, fontSize = 14.sp) }
                    )
                }
            }
        }

        // ── Contador ─────────────────────────────────────────────
        if (query.length >= 2) {
            Surface(color = colors.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth()) {
                Text(
                    "📋 $totalResults resultados · filtro: ${selectedFilter.label} · \"$query\"",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.onBackground
                )
            }
        }

        // ── Resultados ───────────────────────────────────────────
        if (query.length < 2) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔍", fontSize = 48.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Escribe al menos 2 caracteres", color = colors.onSurfaceVariant, fontSize = 14.sp)
                    Text("o usa el micrófono para buscar por voz", color = colors.onSurfaceVariant.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            }
        } else if (totalResults == 0) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔍", fontSize = 48.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Sin resultados", fontWeight = FontWeight.Bold)
                    Text("Prueba con otros términos", color = colors.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // ── Familias ─────────────────────────────────────
                if (visibleFamilyResults.isNotEmpty()) {
                    item {
                        Text(
                            "📚 Familias (${visibleFamilyResults.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
                        )
                    }
                    items(visibleFamilyResults) { family ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1565C0).copy(alpha = 0.08f)),
                            elevation = CardDefaults.cardElevation(1.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("📚", fontSize = 16.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(family, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                        }
                    }
                }

                // ── Plantas ──────────────────────────────────────
                if (visiblePlantResults.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "🌿 Plantas (${visiblePlantResults.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
                        )
                    }
                    items(visiblePlantResults) { plant ->
                        CompactPlantCard(
                            plant = plant,
                            query = query,
                            onClick = { onPlantClick(plant) }
                        )
                    }
                }

                // ── Compuestos ───────────────────────────────────
                if (visibleCompoundResults.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "🧪 Compuestos (${visibleCompoundResults.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
                        )
                    }
                    items(visibleCompoundResults) { compound ->
                        CompactCompoundCard(
                            compound = compound,
                            query = query,
                            onClick = { onCompoundClick(compound) }
                        )
                    }
                }
            }
        }
    }
}

private enum class GlobalSearchFilter(val label: String, val icon: String) {
    All("Todo", "🔎"),
    Plants("Plantas", "🌿"),
    Compounds("Compuestos", "🧪"),
    Families("Familias", "📚")
}
