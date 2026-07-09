package com.toxicplants.database.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
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
import com.toxicplants.database.ui.theme.carbonEffectSubtle
import com.toxicplants.database.ui.viewmodel.PlantViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: PlantViewModel,
    onPlantClick: (PlantEntity) -> Unit,
    onBack: () -> Unit,
    onIntoxicationClick: () -> Unit = {},
    onDichotomousKeysClick: () -> Unit = {}
) {
    val plants by viewModel.plantsData.collectAsState()
    val searchQuery by viewModel.searchQueryData.collectAsState()
    var plantToDelete by remember { mutableStateOf<PlantEntity?>(null) }
    var selectionMode by remember { mutableStateOf(false) }
    val selectedPlantIds = remember { mutableStateListOf<Int>() }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }
    var showBulkEditDialog by remember { mutableStateOf(false) }

    val selectedPlants = remember(plants, selectedPlantIds.toList()) {
        plants.filter { it.id in selectedPlantIds }
    }

    LaunchedEffect(plants) {
        val visibleIds = plants.map { it.id }.toSet()
        selectedPlantIds.removeAll { it !in visibleIds }
    }

    val colors = MaterialTheme.colorScheme

    Scaffold(
        topBar = {
            Surface(modifier = Modifier.fillMaxWidth(), color = colors.primary) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                if (selectionMode) {
                                    selectionMode = false
                                    selectedPlantIds.clear()
                                } else {
                                    onBack()
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = if (selectionMode) "Cancelar selección" else "Volver",
                                tint = colors.onPrimary
                            )
                        }

                        if (selectionMode) {
                            Text(
                                "${selectedPlantIds.size}/${plants.size}",
                                color = colors.onPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = {
                                val visibleIds = plants.map { it.id }
                                val allVisibleSelected = visibleIds.isNotEmpty() && visibleIds.all { it in selectedPlantIds }
                                if (allVisibleSelected) {
                                    selectedPlantIds.removeAll(visibleIds.toSet())
                                } else {
                                    visibleIds.forEach { id -> if (id !in selectedPlantIds) selectedPlantIds.add(id) }
                                }
                            }) { Text("Todas", color = colors.onPrimary, fontSize = 11.sp) }
                            TextButton(
                                enabled = selectedPlantIds.isNotEmpty(),
                                onClick = { showBulkEditDialog = true }
                            ) {
                                Text(
                                    "Editar",
                                    color = if (selectedPlantIds.isNotEmpty()) colors.onPrimary else colors.onPrimary.copy(alpha = 0.4f),
                                    fontSize = 11.sp
                                )
                            }
                            TextButton(
                                enabled = selectedPlantIds.isNotEmpty(),
                                onClick = { showBulkDeleteDialog = true }
                            ) {
                                Text(
                                    "Eliminar",
                                    color = if (selectedPlantIds.isNotEmpty()) colors.onPrimary else colors.onPrimary.copy(alpha = 0.4f),
                                    fontSize = 11.sp
                                )
                            }
                        } else {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Buscar plantas...", color = colors.onPrimary.copy(alpha = 0.6f), maxLines = 1) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colors.onPrimary,
                                    unfocusedBorderColor = colors.onPrimary.copy(alpha = 0.5f),
                                    disabledBorderColor = colors.onPrimary.copy(alpha = 0.35f),
                                    focusedTextColor = colors.onPrimary,
                                    unfocusedTextColor = colors.onPrimary,
                                    disabledTextColor = colors.onPrimary.copy(alpha = 0.65f),
                                    cursorColor = colors.onPrimary,
                                    focusedContainerColor = colors.primaryContainer.copy(alpha = 0.3f),
                                    unfocusedContainerColor = colors.primaryContainer.copy(alpha = 0.2f),
                                    disabledContainerColor = colors.primaryContainer.copy(alpha = 0.12f)
                                ),
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = colors.onPrimary) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = colors.onPrimary)
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(25.dp)
                            )

                            TextButton(onClick = { selectionMode = true }) {
                                Text("Seleccionar", color = colors.onPrimary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (!selectionMode) {
                Surface(modifier = Modifier.fillMaxWidth(), color = colors.surface, shadowElevation = 8.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDichotomousKeysClick,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("🔑 Claves dicotómicas interactivas", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().carbonEffectSubtle().padding(paddingValues).background(colors.background)) {
            if (!selectionMode) {
                ToxicityFilterChips(onFilterSelect = { viewModel.setToxicityFilter(it) })
                Surface(modifier = Modifier.fillMaxWidth(), color = colors.surface, shadowElevation = 2.dp) {
                    Text(
                        "🌿 ${plants.size} plantas encontradas",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = colors.primary
                    )
                }
            }
            if (plants.isEmpty() && searchQuery.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔍", fontSize = 64.sp)
                        Spacer(Modifier.height(16.dp))
                        Text("No se encontraron plantas", fontWeight = FontWeight.Bold, color = colors.primary)
                        Text("para \"$searchQuery\"", color = colors.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(plants) { plant ->
                        SearchPlantCard(
                            plant = plant,
                            onClick = { onPlantClick(plant) },
                            onDeleteClick = { plantToDelete = plant },
                            selectionMode = selectionMode,
                            selected = plant.id in selectedPlantIds,
                            onSelectionChange = { checked ->
                                if (checked) {
                                    if (plant.id !in selectedPlantIds) selectedPlantIds.add(plant.id)
                                } else {
                                    selectedPlantIds.remove(plant.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    plantToDelete?.let { plant ->
        AlertDialog(
            onDismissRequest = { plantToDelete = null },
            title = { Text("¿Eliminar planta?", color = colors.primary) },
            text = { Text("¿Estás seguro de eliminar ${plant.commonName}?") },
            confirmButton = { TextButton(onClick = { viewModel.deletePlant(plant); plantToDelete = null }) { Text("Eliminar", color = colors.error) } },
            dismissButton = { TextButton(onClick = { plantToDelete = null }) { Text("Cancelar", color = colors.primary) } }
        )
    }

    if (showBulkDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteDialog = false },
            title = { Text("¿Eliminar ${selectedPlantIds.size} plantas?", color = colors.primary) },
            text = { Text("Se eliminarán todas las fichas marcadas en el buscador. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePlants(selectedPlants)
                    selectedPlantIds.clear()
                    selectionMode = false
                    showBulkDeleteDialog = false
                }) { Text("Eliminar", color = colors.error) }
            },
            dismissButton = { TextButton(onClick = { showBulkDeleteDialog = false }) { Text("Cancelar", color = colors.primary) } }
        )
    }

    if (showBulkEditDialog) {
        SearchBulkEditPlantsDialog(
            selectedCount = selectedPlantIds.size,
            onDismiss = { showBulkEditDialog = false },
            onConfirm = { field, value, append ->
                viewModel.bulkUpdatePlants(selectedPlants, field.id, value, append)
                selectedPlantIds.clear()
                selectionMode = false
                showBulkEditDialog = false
            }
        )
    }
}

@Composable
fun ToxicityFilterChips(onFilterSelect: (String?) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val toxicityLevels = listOf("Todas", "Mortal", "Muy alto", "Alto", "Moderado", "Bajo", "Desconocido")
    var selectedLevel by remember { mutableStateOf<String?>(null) }

    FlowRow(
        modifier = Modifier.fillMaxWidth().background(colors.surface).padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        toxicityLevels.forEach { level ->
            val isSelected = selectedLevel == level || (level == "Todas" && selectedLevel == null)
            val chipColor = when (level) {
                "Todas" -> colors.primary
                "Mortal" -> colors.error
                "Muy alto" -> Color(0xFFFF5722)
                "Alto" -> Color(0xFFE65100)
                "Moderado" -> Color(0xFFF57C00)
                "Bajo" -> Color(0xFF388E3C)
                else -> Color.Gray
            }
            FilterChip(
                selected = isSelected,
                onClick = {
                    if (level == "Todas") {
                        selectedLevel = null
                        onFilterSelect(null)
                    } else {
                        selectedLevel = if (selectedLevel == level) null else level
                        onFilterSelect(selectedLevel)
                    }
                },
                label = { Text(level, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = chipColor,
                    selectedLabelColor = Color.White,
                    containerColor = chipColor.copy(alpha = 0.1f),
                    labelColor = chipColor
                )
            )
        }
    }
}

@Composable
fun SearchPlantCard(
    plant: PlantEntity,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onSelectionChange: (Boolean) -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    val toxicityColor = when (plant.toxicityLevel) {
        "Mortal" -> colors.error; "Alto" -> Color(0xFFE65100); "Muy alto" -> Color(0xFFFF5722); "Moderado" -> Color(0xFFF57C00); "Bajo" -> colors.primary; else -> colors.onSurfaceVariant
    }
    val toxicityEmoji = when (plant.toxicityLevel) { "Mortal" -> "💀"; "Muy alto" -> "☠️"; "Alto" -> "⚠️"; "Moderado" -> "⚡"; "Bajo" -> "🟢"; else -> "ℹ️" }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (selectionMode) onSelectionChange(!selected) else onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (selected) colors.primaryContainer.copy(alpha = 0.55f) else colors.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 6.dp else 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (selectionMode) {
                Checkbox(checked = selected, onCheckedChange = { onSelectionChange(it) })
                Spacer(Modifier.width(8.dp))
            }
            Box(modifier = Modifier.size(48.dp).background(toxicityColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Text(toxicityEmoji, fontSize = 24.sp) }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(plant.commonName, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = colors.onSurface)
                Text(plant.scientificName, fontStyle = FontStyle.Italic, color = colors.onSurfaceVariant, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = toxicityColor.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) { Text(plant.toxicityLevel, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = toxicityColor) }
                    if (plant.category.isNotBlank()) { Surface(color = colors.primary.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) { Text(plant.category, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, color = colors.primary) } }
                }
            }
            if (selectionMode) {
                Text(
                    if (selected) "✓" else "",
                    color = colors.primary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Clear, contentDescription = "Eliminar", tint = colors.error.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

private data class SearchBulkEditField(val id: String, val label: String, val hint: String)

private val SEARCH_BULK_EDIT_FIELDS = listOf(
    SearchBulkEditField("commonNames", "Otros nombres comunes", "Ej: belladona, tabaco borde"),
    SearchBulkEditField("family", "Familia", "Ej: Solanaceae"),
    SearchBulkEditField("toxicityLevel", "Nivel de toxicidad", "Ej: Alto, Mortal..."),
    SearchBulkEditField("toxicParts", "Partes tóxicas", "Ej: hojas; semillas; raíz"),
    SearchBulkEditField("symptoms", "Síntomas", "Ej: náuseas, vómitos, arritmias"),
    SearchBulkEditField("description", "Descripción", "Descripción común para las fichas"),
    SearchBulkEditField("category", "Categoría", "Ej: Ornamental"),
    SearchBulkEditField("habitat", "Hábitat", "Ej: bosques húmedos"),
    SearchBulkEditField("geographicDistribution", "Distribución", "Ej: Mediterráneo"),
    SearchBulkEditField("firstAid", "Primeros auxilios", "Texto de primeros auxilios"),
    SearchBulkEditField("floweringMonths", "Meses de floración", "Ej: 3,4,5,6"),
    SearchBulkEditField("fruitingMonths", "Meses de fructificación", "Ej: 8,9,10"),
    SearchBulkEditField("maxToxicityMonths", "Meses máxima toxicidad", "Ej: 6,7,8"),
    SearchBulkEditField("notes", "Notas", "Nota común para las fichas"),
    SearchBulkEditField("mythsAndLegends", "Mitos y curiosidades", "Texto cultural común")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBulkEditPlantsDialog(
    selectedCount: Int,
    onDismiss: () -> Unit,
    onConfirm: (SearchBulkEditField, String, Boolean) -> Unit
) {
    var selectedField by remember { mutableStateOf(SEARCH_BULK_EDIT_FIELDS.first()) }
    var value by remember { mutableStateOf("") }
    var append by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar $selectedCount fichas") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Elige el campo y el dato que quieres aplicar a todas las plantas marcadas del buscador.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedField.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Campo") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        SEARCH_BULK_EDIT_FIELDS.forEach { field ->
                            DropdownMenuItem(
                                text = { Text(field.label) },
                                onClick = {
                                    selectedField = field
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Dato a aplicar") },
                    placeholder = { Text(selectedField.hint) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Añadir sin borrar lo anterior", fontWeight = FontWeight.Medium)
                        Text(
                            if (append) "Se agrega al final si no existe" else "Se reemplaza el campo completo",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = append, onCheckedChange = { append = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = value.isNotBlank(),
                onClick = { onConfirm(selectedField, value.trim(), append) }
            ) { Text("Aplicar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
