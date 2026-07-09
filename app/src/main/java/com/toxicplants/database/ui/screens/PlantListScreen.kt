package com.toxicplants.database.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.ui.viewmodel.PlantViewModel

/**
 * Regiones para filtrar. Cada región tiene una etiqueta (con emoji) y una lista de
 * palabras clave que se buscan dentro del campo geographicDistribution de cada planta.
 */
private data class Region(val label: String, val keywords: List<String>)

private val REGIONS = listOf(
    Region("🌍 Todas", emptyList()),
    Region("🇪🇸 España", listOf("España", "Ibérica", "Iberia", "Península", "Baleares", "Canarias")),
    Region("🌊 Mediterráneo", listOf("Mediterráneo", "Mediterránea")),
    Region("🇪🇺 Europa", listOf("Europa", "Europea", "Eurasia")),
    Region("🌎 América", listOf("América", "Americano", "Americana", "Norteamérica", "Sudamérica", "EE.UU", "Estados Unidos")),
    Region("🇲🇽 México", listOf("México", "Mexicano", "Mexicana", "Mesoamérica")),
    Region("🌐 Sudamérica", listOf("Sudamérica", "Sudamericano", "Argentina", "Chile", "Perú", "Colombia", "Brasil", "Andes")),
    Region("🌏 Asia", listOf("Asia", "Asiático", "Asiática", "China", "Japón", "India", "Eurasia")),
    Region("🌍 África", listOf("África", "Africano", "Africana")),
    Region("🦘 Oceanía", listOf("Australia", "Oceanía", "Nueva Zelanda", "Nueva Guinea")),
    Region("🌴 Tropical", listOf("Tropical", "Trópico", "Pan-tropical")),
    Region("🌐 Cosmopolita", listOf("mundial", "cosmopolita", "Hemisferio", "todo el mundo"))
)

private fun PlantEntity.matchesRegion(region: Region): Boolean {
    if (region.keywords.isEmpty()) return true
    val g = geographicDistribution.lowercase()
    return region.keywords.any { g.contains(it.lowercase()) }
}

// ── NUEVOS ENUMS ────────────────────────────────────────────────

/**
 * Modo de búsqueda/ordenamiento por nombre.
 */
private enum class NameMode(val label: String, val icon: String) {
    All("Todos", "🌐"),
    CommonName("Nombre común", "🏷️"),
    ScientificName("Nombre latino", "🔬")
}

/**
 * Filtro alfabético por primera letra del nombre.
 */
private enum class AlphabetFilter(val label: String, val letter: Char?) {
    All("All", null),
    A("A", 'A'), B("B", 'B'), C("C", 'C'), D("D", 'D'),
    E("E", 'E'), F("F", 'F'), G("G", 'G'), H("H", 'H'),
    I("I", 'I'), J("J", 'J'), K("K", 'K'), L("L", 'L'),
    M("M", 'M'), N("N", 'N'), O("O", 'O'), P("P", 'P'),
    Q("Q", 'Q'), R("R", 'R'), S("S", 'S'), T("T", 'T'),
    U("U", 'U'), V("V", 'V'), W("W", 'W'), X("X", 'X'),
    Y("Y", 'Y'), Z("Z", 'Z')
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantListScreen(
    viewModel: PlantViewModel,
    onPlantClick: (PlantEntity) -> Unit,
    onBack: () -> Unit
) {
    val plants by viewModel.allPlants.observeAsState(emptyList())
    var plantToDelete by remember { mutableStateOf<PlantEntity?>(null) }
    var selectedRegion by remember { mutableStateOf(REGIONS.first()) }
    var selectionMode by remember { mutableStateOf(false) }
    val selectedPlantIds = remember { mutableStateListOf<Int>() }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }
    var showBulkEditDialog by remember { mutableStateOf(false) }

    // ── NUEVOS FILTROS ──────────────────────────────────────
    var nameMode by remember { mutableStateOf(NameMode.All) }
    var alphabetFilter by remember { mutableStateOf(AlphabetFilter.All) }

    // Filtrado por región
    val regionFiltered = remember(plants, selectedRegion) {
        if (selectedRegion.keywords.isEmpty()) plants
        else plants.filter { it.matchesRegion(selectedRegion) }
    }

    // Filtrado alfabético + ordenamiento
    val filtered = remember(regionFiltered, alphabetFilter, nameMode) {
        var result = regionFiltered

        // Filtrar por letra
        if (alphabetFilter != AlphabetFilter.All) {
            result = result.filter { plant ->
                val firstChar = when (nameMode) {
                    NameMode.CommonName -> plant.commonName.firstOrNull()?.uppercaseChar()
                    NameMode.ScientificName -> plant.scientificName.firstOrNull()?.uppercaseChar()
                    NameMode.All -> plant.commonName.firstOrNull()?.uppercaseChar()
                        ?: plant.scientificName.firstOrNull()?.uppercaseChar()
                }
                firstChar == alphabetFilter.letter
            }
        }

        // Ordenar según modo
        result = when (nameMode) {
            NameMode.All -> result.sortedBy { it.commonName.lowercase() }
            NameMode.CommonName -> result.sortedBy { it.commonName.lowercase() }
            NameMode.ScientificName -> result.sortedBy { it.scientificName.lowercase() }
        }

        result
    }

    val selectedPlants = remember(plants, selectedPlantIds.toList()) {
        plants.filter { it.id in selectedPlantIds }
    }

    LaunchedEffect(plants) {
        val existingIds = plants.map { it.id }.toSet()
        selectedPlantIds.removeAll { it !in existingIds }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            if (selectionMode) "${selectedPlantIds.size} seleccionadas" else "📋 Plantas (${filtered.size}/${plants.size})",
                            fontWeight = FontWeight.Bold
                        )
                        val subTitle = buildString {
                            if (alphabetFilter != AlphabetFilter.All) append("${alphabetFilter.label} · ")
                            if (nameMode != NameMode.All) append("${nameMode.label} · ")
                            if (selectedRegion.keywords.isNotEmpty()) append(selectedRegion.label)
                        }.trimEnd(' ', '·')
                        if (subTitle.isNotEmpty()) {
                            Text(subTitle, fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectionMode) {
                            selectionMode = false
                            selectedPlantIds.clear()
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, if (selectionMode) "Cancelar selección" else "Volver")
                    }
                },
                actions = {
                    if (selectionMode) {
                        TextButton(onClick = {
                            val filteredIds = filtered.map { it.id }
                            val allFilteredSelected = filteredIds.isNotEmpty() && filteredIds.all { it in selectedPlantIds }
                            if (allFilteredSelected) {
                                selectedPlantIds.removeAll(filteredIds.toSet())
                            } else {
                                filteredIds.forEach { id -> if (id !in selectedPlantIds) selectedPlantIds.add(id) }
                            }
                        }) { Text("Todas", color = Color.White, fontSize = 12.sp) }
                        TextButton(
                            enabled = selectedPlantIds.isNotEmpty(),
                            onClick = { showBulkEditDialog = true }
                        ) { Text("Editar", color = if (selectedPlantIds.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.4f), fontSize = 12.sp) }
                        TextButton(
                            enabled = selectedPlantIds.isNotEmpty(),
                            onClick = { showBulkDeleteDialog = true }
                        ) { Text("Eliminar", color = if (selectedPlantIds.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.4f), fontSize = 12.sp) }
                    } else {
                        TextButton(onClick = { selectionMode = true }) {
                            Text("Seleccionar", color = Color.White, fontSize = 12.sp)
                        }
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
            // ── Filtro de Nombre (Modo) ────────────────────────────────────
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        Box(
                            modifier = Modifier.height(28.dp).padding(end = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🔤 Nombre:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    items(NameMode.entries) { mode ->
                        FilterChip(
                            selected = nameMode == mode,
                            onClick = { nameMode = mode },
                            label = { Text(mode.label, fontSize = 11.sp) },
                            leadingIcon = { Text(mode.icon, fontSize = 12.sp) },
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
            }

            // ── Filtro Alfabético ───────────────────────────────────────────
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        Box(
                            modifier = Modifier.height(28.dp).padding(end = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("A-Z:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    items(AlphabetFilter.entries) { letter ->
                        FilterChip(
                            selected = alphabetFilter == letter,
                            onClick = { alphabetFilter = letter },
                            label = {
                                Text(
                                    letter.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (letter == AlphabetFilter.All) FontWeight.Normal else FontWeight.Bold
                                )
                            },
                            modifier = Modifier.height(28.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF2E7D32),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // ── Filtro de región (chips desplazables) ──
            RegionFilterBar(
                selected = selectedRegion,
                onSelect = { selectedRegion = it }
            )

            if (plants.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF2E7D32))
                }
            } else if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Public, null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Sin plantas para los filtros seleccionados", color = Color.Gray)
                        if (alphabetFilter != AlphabetFilter.All) {
                            Text(
                                "No hay plantas que empiecen por '${alphabetFilter.label}' en ${nameMode.label}",
                                fontSize = 12.sp, color = Color.Gray.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                // ── Contador de resultados ──
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "📋 ${filtered.size} plantas" +
                                (if (alphabetFilter != AlphabetFilter.All) " · $alphabetFilter" else "") +
                                (if (nameMode != NameMode.All) " · ordenar por ${nameMode.label}" else "") +
                                (if (selectedRegion.keywords.isNotEmpty()) " · ${selectedRegion.label}" else ""),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // ── Lista de plantas (agrupadas por letra si no hay filtro de letra) ──
                if (alphabetFilter == AlphabetFilter.All) {
                    // Vista agrupada por letra
                    val groupedPlants = filtered.groupBy { plant ->
                        when (nameMode) {
                            NameMode.CommonName -> plant.commonName.firstOrNull()?.uppercaseChar() ?: '#'
                            NameMode.ScientificName -> plant.scientificName.firstOrNull()?.uppercaseChar() ?: '#'
                            NameMode.All -> plant.commonName.firstOrNull()?.uppercaseChar()
                                ?: plant.scientificName.firstOrNull()?.uppercaseChar() ?: '#'
                        }
                    }.toSortedMap()

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        groupedPlants.forEach { (letter, plantsInGroup) ->
                            item {
                                Text(
                                    "── $letter ──",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF2E7D32),
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )
                            }
                            items(plantsInGroup) { plant ->
                                PlantCard(
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
                } else {
                    // Vista simple sin agrupar (ya hay filtro de letra)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtered) { plant ->
                            PlantCard(
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
    }

    // Diálogo de confirmación para eliminar
    plantToDelete?.let { plant ->
        AlertDialog(
            onDismissRequest = { plantToDelete = null },
            title = { Text("¿Eliminar planta?") },
            text = { Text("¿Estás seguro de eliminar ${plant.commonName}?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePlant(plant)
                    plantToDelete = null
                }) { Text("Eliminar", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { plantToDelete = null }) { Text("Cancelar") }
            }
        )
    }

    if (showBulkDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteDialog = false },
            title = { Text("¿Eliminar ${selectedPlantIds.size} plantas?") },
            text = { Text("Se eliminarán todas las fichas marcadas. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePlants(selectedPlants)
                    selectedPlantIds.clear()
                    selectionMode = false
                    showBulkDeleteDialog = false
                }) { Text("Eliminar", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (showBulkEditDialog) {
        BulkEditPlantsDialog(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegionFilterBar(
    selected: Region,
    onSelect: (Region) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        REGIONS.forEach { region ->
            FilterChip(
                selected = region == selected,
                onClick = { onSelect(region) },
                label = { Text(region.label, fontSize = 13.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF2E7D32),
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}
private data class BulkEditField(val id: String, val label: String, val hint: String)

private val BULK_EDIT_FIELDS = listOf(
    BulkEditField("commonNames", "Otros nombres comunes", "Ej: belladona, tabaco borde"),
    BulkEditField("family", "Familia", "Ej: Solanaceae"),
    BulkEditField("toxicityLevel", "Nivel de toxicidad", "Ej: Alto, Mortal..."),
    BulkEditField("toxicParts", "Partes tóxicas", "Ej: hojas; semillas; raíz"),
    BulkEditField("symptoms", "Síntomas", "Ej: náuseas, vómitos, arritmias"),
    BulkEditField("description", "Descripción", "Descripción común para las fichas"),
    BulkEditField("category", "Categoría", "Ej: Ornamental"),
    BulkEditField("habitat", "Hábitat", "Ej: bosques húmedos"),
    BulkEditField("geographicDistribution", "Distribución", "Ej: Mediterráneo"),
    BulkEditField("firstAid", "Primeros auxilios", "Texto de primeros auxilios"),
    BulkEditField("floweringMonths", "Meses de floración", "Ej: 3,4,5,6"),
    BulkEditField("fruitingMonths", "Meses de fructificación", "Ej: 8,9,10"),
    BulkEditField("maxToxicityMonths", "Meses máxima toxicidad", "Ej: 6,7,8"),
    BulkEditField("notes", "Notas", "Nota común para las fichas"),
    BulkEditField("mythsAndLegends", "Mitos y curiosidades", "Texto cultural común")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BulkEditPlantsDialog(
    selectedCount: Int,
    onDismiss: () -> Unit,
    onConfirm: (BulkEditField, String, Boolean) -> Unit
) {
    var selectedField by remember { mutableStateOf(BULK_EDIT_FIELDS.first()) }
    var value by remember { mutableStateOf("") }
    var append by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar $selectedCount fichas") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Elige el campo y el dato que quieres aplicar a todas las plantas marcadas.",
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
                        BULK_EDIT_FIELDS.forEach { field ->
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
