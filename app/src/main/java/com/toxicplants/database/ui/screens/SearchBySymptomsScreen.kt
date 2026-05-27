package com.toxicplants.database.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

    var symptomsQuery by remember { mutableStateOf("") }
    var selectedToxicity by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    var toxicityExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }

    var filtersExpanded by remember { mutableStateOf(true) }

    val toxicityLevels = listOf("Mortal", "Muy alto", "Alto", "Moderado", "Bajo", "Desconocido")

    val categories by remember(allPlants) {
        derivedStateOf {
            allPlants
                .map { plant -> plant.category }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
        }
    }

    val filteredPlants = remember(symptomsQuery, selectedToxicity, selectedCategory, allPlants) {
        allPlants.filter { plant ->
            val matchesSymptoms = symptomsQuery.isBlank() ||
                    plant.symptoms.contains(symptomsQuery, ignoreCase = true) ||
                    plant.commonName.contains(symptomsQuery, ignoreCase = true) ||
                    plant.scientificName.contains(symptomsQuery, ignoreCase = true) ||
                    plant.toxicParts.contains(symptomsQuery, ignoreCase = true)

            val matchesToxicity = selectedToxicity == null ||
                    plant.toxicityLevel.equals(selectedToxicity, ignoreCase = true)

            val matchesCategory = selectedCategory == null ||
                    plant.category.equals(selectedCategory, ignoreCase = true)

            matchesSymptoms && matchesToxicity && matchesCategory
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Barra de título compacta con fondo marrón
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF5D4037)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    "Síntomas y toxicidad",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = { filtersExpanded = !filtersExpanded },
                    modifier = Modifier.size(36.dp)
                ) {
                    Text(
                        if (filtersExpanded) "▼" else "▶",
                        color = Color.White,
                        fontSize = 18.sp
                    )
                }
            }
        }

        // Filtros con fondo verde clarito
        if (filtersExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFC8E6C9))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Campo de síntomas
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
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6A1B9A),
                        cursorColor = Color(0xFF6A1B9A)
                    )
                )

                // Row con los dos dropdowns usando Box en vez de OutlinedTextField
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Selector toxicidad
                    Box(modifier = Modifier.weight(1f)) {
                        DropdownMenu(
                            expanded = toxicityExpanded,
                            onDismissRequest = { toxicityExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Todos") },
                                onClick = { selectedToxicity = null; toxicityExpanded = false }
                            )
                            toxicityLevels.forEach { level ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(getToxicityEmoji(level))
                                            Spacer(Modifier.width(8.dp))
                                            Text(level)
                                        }
                                    },
                                    onClick = { selectedToxicity = level; toxicityExpanded = false }
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { toxicityExpanded = true },
                            shape = RoundedCornerShape(4.dp),
                            color = Color.White,
                            border = ButtonDefaults.outlinedButtonBorder
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedToxicity ?: "Toxicidad",
                                    modifier = Modifier.weight(1f),
                                    color = if (selectedToxicity != null) Color.Black else Color.Gray
                                )
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = Color.Gray
                                )
                            }
                        }
                    }

                    // Selector categoría
                    Box(modifier = Modifier.weight(1f)) {
                        DropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Todos") },
                                onClick = { selectedCategory = null; categoryExpanded = false }
                            )
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category) },
                                    onClick = { selectedCategory = category; categoryExpanded = false }
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { categoryExpanded = true },
                            shape = RoundedCornerShape(4.dp),
                            color = Color.White,
                            border = ButtonDefaults.outlinedButtonBorder
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedCategory ?: "Categoría",
                                    modifier = Modifier.weight(1f),
                                    color = if (selectedCategory != null) Color.Black else Color.Gray
                                )
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = Color.Gray
                                )
                            }
                        }
                    }
                }

                // Botón limpiar
                if (symptomsQuery.isNotEmpty() || selectedToxicity != null || selectedCategory != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = {
                            symptomsQuery = ""
                            selectedToxicity = null
                            selectedCategory = null
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Limpiar")
                        }
                    }
                }
            }
        }

        // Contador de resultados
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFE8E8E8)
        ) {
            Text(
                "📋 ${filteredPlants.size} plantas",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = Color(0xFF333333)
            )
        }

        // Lista de resultados
        if (filteredPlants.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredPlants) { plant ->
                    CompactPlantCard(
                        plant = plant,
                        onClick = { onPlantClick(plant) }
                    )
                }
            }
        } else if (symptomsQuery.isNotEmpty() || selectedToxicity != null || selectedCategory != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔍", fontSize = 50.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Sin resultados", fontWeight = FontWeight.Bold)
                    Text("Prueba otros términos", color = Color.Gray)
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("💡 Escribe síntomas o selecciona filtros", color = Color.Gray)
            }
        }
    }
}

@Composable
fun CompactPlantCard(plant: PlantEntity, onClick: () -> Unit) {
    val toxicityColor = when (plant.toxicityLevel) {
        "Mortal" -> Color(0xFFB71C1C)
        "Alto" -> Color(0xFFE65100)
        "Muy alto" -> Color(0xFFFF5722)
        "Moderado" -> Color(0xFFF57C00)
        "Bajo" -> Color(0xFF388E3C)
        else -> Color.Gray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(toxicityColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    when (plant.toxicityLevel) {
                        "Mortal" -> "💀"
                        "Muy alto" -> "☠️"
                        "Alto" -> "⚠️"
                        "Moderado" -> "⚡"
                        "Bajo" -> "🟢"
                        else -> "ℹ️"
                    },
                    fontSize = 20.sp
                )
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    plant.commonName,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    plant.scientificName,
                    color = Color.Gray,
                    fontStyle = FontStyle.Italic,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (plant.symptoms.isNotBlank()) {
                    Text(
                        plant.symptoms,
                        color = Color(0xFF666666),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Surface(
                color = toxicityColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    plant.toxicityLevel,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    fontSize = 10.sp,
                    color = toxicityColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun getToxicityEmoji(level: String): String = when (level.lowercase()) {
    "mortal" -> "💀"
    "muy alto" -> "☠️"
    "alto" -> "⚠️"
    "moderado" -> "⚡"
    "bajo" -> "🟢"
    else -> "❓"
}