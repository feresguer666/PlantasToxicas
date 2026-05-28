package com.toxicplants.database.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
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
fun SearchBySymptomsScreen(
    viewModel: PlantViewModel,
    onPlantClick: (PlantEntity) -> Unit,
    onBack: () -> Unit
) {
    val allPlants by viewModel.allPlants.observeAsState(emptyList())
    val colors = MaterialTheme.colorScheme

    var symptomsQuery by remember { mutableStateOf("") }
    var selectedToxicity by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var toxicityExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var filtersExpanded by remember { mutableStateOf(true) }

    val toxicityLevels = listOf("Mortal", "Muy alto", "Alto", "Moderado", "Bajo", "Desconocido")
    val categories by remember(allPlants) { derivedStateOf { allPlants.map { it.category }.filter { it.isNotBlank() }.distinct().sorted() } }

    val filteredPlants = remember(symptomsQuery, selectedToxicity, selectedCategory, allPlants) {
        allPlants.filter { plant ->
            val matchesSymptoms = symptomsQuery.isBlank() || plant.symptoms.contains(symptomsQuery, ignoreCase = true) || plant.commonName.contains(symptomsQuery, ignoreCase = true) || plant.scientificName.contains(symptomsQuery, ignoreCase = true) || plant.toxicParts.contains(symptomsQuery, ignoreCase = true)
            val matchesToxicity = selectedToxicity == null || plant.toxicityLevel.equals(selectedToxicity, ignoreCase = true)
            val matchesCategory = selectedCategory == null || plant.category.equals(selectedCategory, ignoreCase = true)
            matchesSymptoms && matchesToxicity && matchesCategory
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(modifier = Modifier.fillMaxWidth(), color = colors.tertiary) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = colors.onTertiary)
                }
                Spacer(Modifier.width(4.dp))
                Text("Síntomas y toxicidad", color = colors.onTertiary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { filtersExpanded = !filtersExpanded }) {
                    Text(if (filtersExpanded) "Ocultar" else "Mostrar", color = colors.onTertiary, fontSize = 14.sp)
                }
            }
        }

        if (filtersExpanded) {
            Column(modifier = Modifier.fillMaxWidth().background(colors.primaryContainer.copy(alpha = 0.3f)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = symptomsQuery,
                    onValueChange = { symptomsQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Síntomas, partes tóxicas...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (symptomsQuery.isNotEmpty()) {
                            IconButton(onClick = { symptomsQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = colors.tertiary, cursorColor = colors.tertiary)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Selector toxicidad - placeholder corto
                    ExposedDropdownMenuBox(expanded = toxicityExpanded, onExpandedChange = { toxicityExpanded = !toxicityExpanded }, modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = selectedToxicity ?: "",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.menuAnchor(),
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toxicityExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = colors.tertiary),
                            placeholder = { Text("Nivel", fontSize = 13.sp, maxLines = 1) }
                        )
                        ExposedDropdownMenu(expanded = toxicityExpanded, onDismissRequest = { toxicityExpanded = false }) {
                            DropdownMenuItem(text = { Text("Todos", fontSize = 13.sp) }, onClick = { selectedToxicity = null; toxicityExpanded = false })
                            toxicityLevels.forEach { level ->
                                DropdownMenuItem(
                                    text = { Row(verticalAlignment = Alignment.CenterVertically) { Text(getToxicityEmoji(level), fontSize = 14.sp); Spacer(Modifier.width(6.dp)); Text(level, fontSize = 13.sp) } },
                                    onClick = { selectedToxicity = level; toxicityExpanded = false }
                                )
                            }
                        }
                    }

                    // Selector categoría - placeholder corto
                    ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = !categoryExpanded }, modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = selectedCategory ?: "",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.menuAnchor(),
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = colors.tertiary),
                            placeholder = { Text("Tipo", fontSize = 13.sp, maxLines = 1) }
                        )
                        ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                            DropdownMenuItem(text = { Text("Todos", fontSize = 13.sp) }, onClick = { selectedCategory = null; categoryExpanded = false })
                            categories.forEach { category ->
                                DropdownMenuItem(text = { Text(category, fontSize = 13.sp) }, onClick = { selectedCategory = category; categoryExpanded = false })
                            }
                        }
                    }
                }

                if (symptomsQuery.isNotEmpty() || selectedToxicity != null || selectedCategory != null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { symptomsQuery = ""; selectedToxicity = null; selectedCategory = null }) {
                            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp));
                            Spacer(Modifier.width(4.dp));
                            Text("Limpiar", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Surface(modifier = Modifier.fillMaxWidth(), color = colors.surfaceVariant.copy(alpha = 0.5f)) {
            Text("📋 ${filteredPlants.size} plantas", modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), fontWeight = FontWeight.Medium, fontSize = 14.sp, color = colors.onBackground)
        }

        if (filteredPlants.isNotEmpty()) {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredPlants) { plant -> CompactPlantCard(plant = plant, onClick = { onPlantClick(plant) }) }
            }
        } else if (symptomsQuery.isNotEmpty() || selectedToxicity != null || selectedCategory != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔍", fontSize = 50.sp);
                    Spacer(Modifier.height(8.dp));
                    Text("Sin resultados", fontWeight = FontWeight.Bold);
                    Text("Prueba otros términos", color = colors.onSurfaceVariant)
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("💡 Escribe síntomas o selecciona filtros", color = colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun CompactPlantCard(plant: PlantEntity, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val toxicityColor = when (plant.toxicityLevel) {
        "Mortal" -> colors.error;
        "Alto" -> Color(0xFFE65100);
        "Muy alto" -> Color(0xFFFF5722);
        "Moderado" -> Color(0xFFF57C00);
        "Bajo" -> colors.primary;
        else -> colors.onSurfaceVariant
    }
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = colors.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(44.dp).background(toxicityColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Text(when (plant.toxicityLevel) { "Mortal" -> "💀"; "Muy alto" -> "☠️"; "Alto" -> "⚠️"; "Moderado" -> "⚡"; "Bajo" -> "🟢"; else -> "ℹ️" }, fontSize = 20.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(plant.commonName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(plant.scientificName, color = colors.onSurfaceVariant, fontStyle = FontStyle.Italic, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (plant.symptoms.isNotBlank()) { Text(plant.symptoms, color = Color(0xFF666666), maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 12.sp) }
            }
            Spacer(Modifier.width(8.dp))
            Surface(color = toxicityColor.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                Text(plant.toxicityLevel, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp), fontSize = 10.sp, color = toxicityColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun getToxicityEmoji(level: String): String = when (level.lowercase()) { "mortal" -> "💀"; "muy alto" -> "☠️"; "alto" -> "⚠️"; "moderado" -> "⚡"; "bajo" -> "🟢"; else -> "❓" }