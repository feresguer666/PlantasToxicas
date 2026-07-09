package com.toxicplants.database.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.toxicplants.database.ui.screens.families.FamilyToxicityScope
import com.toxicplants.database.ui.screens.families.FamilyViewModel
import com.toxicplants.database.ui.screens.families.ToxicFamily
import com.toxicplants.database.ui.viewmodel.PlantViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FamilyListScreen(
    viewModel: PlantViewModel, // se mantiene por compatibilidad, no se usa
    onBack: () -> Unit,
    onFamilyClick: (String) -> Unit
) {
    val vm: FamilyViewModel = viewModel()
    val families by vm.families.collectAsState()
    val query by vm.query.collectAsState()
    var sortByFichas by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ToxicFamily?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<ToxicFamily?>(null) }

    val filtered = remember(families, query, sortByFichas) {
        val q = query.lowercase()
        val list = if (q.isBlank()) families else families.filter {
            it.family.family.lowercase().contains(q) ||
                    it.family.commonNameEs.lowercase().contains(q) ||
                    it.family.description.lowercase().contains(q)
        }
        if (sortByFichas) list.sortedByDescending { it.fichasLocal } else list.sortedBy { it.family.family }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📚 Familias tóxicas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            null
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showAdd = true }) {
                        Icon(
                            Icons.Default.Add,
                            "Añadir",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1565C0),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }, containerColor = Color(0xFF1565C0)) {
                Icon(Icons.Default.Add, "Añadir", tint = Color.White)
            }
        }
    ) { padding ->
        Column(Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(12.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = vm::setQuery,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar familia…") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${filtered.size} familias", fontSize = 12.sp, color = Color.Gray)
                TextButton(onClick = { sortByFichas = !sortByFichas }) {
                    Text(if (sortByFichas) "Orden: fichas ↓" else "Orden: A-Z")
                }
            }
            Text(
                "Toca para ver plantas • Mantén pulsado para editar",
                fontSize = 11.sp,
                color = Color.Gray
            )
            Spacer(Modifier.height(4.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered, key = { it.family.family }) { item ->
                    val f = item.family
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { onFamilyClick(f.family) },
                                onLongClick = { editing = f }),
                        colors = CardDefaults.cardColors(
                            containerColor = if (item.isUserEdited) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(Modifier
                            .fillMaxWidth()
                            .padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    f.family,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    fontStyle = FontStyle.Italic
                                )
                                if (f.commonNameEs.isNotBlank()) {
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "– ${f.commonNameEs}",
                                        fontSize = 13.sp,
                                        color = Color.Gray
                                    )
                                }
                                Spacer(Modifier.weight(1f))
                                AssistChip(
                                    onClick = {},
                                    label = {
                                        Text(
                                            "fichas=${item.fichasLocal}",
                                            fontSize = 11.sp
                                        )
                                    })
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Géneros: ${f.generaCount} · Especies: ${f.speciesCount}" +
                                        if (f.distribution.isNotBlank()) " · ${f.distribution}" else "",
                                fontSize = 12.sp, color = Color.Gray
                            )
                            if (f.description.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(f.description, fontSize = 13.sp, maxLines = 3)
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val (chipColor, chipLabel) = when (f.toxicityScope) {
                                    FamilyToxicityScope.ALL_GENERA -> Color(0xFFB71C1C) to "Todos los géneros tóxicos"
                                    FamilyToxicityScope.SOME_GENERA -> Color(0xFFE65100) to "Algunos géneros tóxicos"
                                    FamilyToxicityScope.SOME_SPECIES -> Color(0xFFF57C00) to "Algunas especies tóxicas"
                                    FamilyToxicityScope.UNKNOWN -> Color.Gray to "Toxicidad desconocida"
                                }
                                AssistChip(
                                    onClick = {},
                                    label = {
                                        Text(
                                            chipLabel,
                                            fontSize = 11.sp,
                                            color = chipColor
                                        )
                                    })
                                Spacer(Modifier.weight(1f))
                                IconButton(onClick = { editing = f }) {
                                    Icon(
                                        Icons.Default.Edit,
                                        "Editar",
                                        tint = Color(0xFF1565C0),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(onClick = { toDelete = f }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        "Borrar",
                                        tint = Color(0xFFD32F2F),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd || editing != null) {
        EditFamilyDialog(
            initial = editing,
            onDismiss = { showAdd = false; editing = null },
            onSave = { fam ->
                vm.save(fam, editing?.family)
                showAdd = false; editing = null
            }
        )
    }
    toDelete?.let { f ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("¿Eliminar ${f.family}?") },
            text = { Text("Se ocultará del catálogo. Tus fichas de plantas NO se borran.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteGenus(f.family); toDelete = null
                }) { Text("Eliminar", color = Color.Red) }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancelar") } })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditFamilyDialog(
    initial: ToxicFamily?,
    onDismiss: () -> Unit,
    onSave: (ToxicFamily) -> Unit
) {
    var family by remember(initial) { mutableStateOf(initial?.family ?: "") }
    var common by remember(initial) { mutableStateOf(initial?.commonNameEs ?: "") }
    var genera by remember(initial) { mutableStateOf(initial?.generaCount?.toString() ?: "") }
    var species by remember(initial) { mutableStateOf(initial?.speciesCount?.toString() ?: "") }
    var distribution by remember(initial) { mutableStateOf(initial?.distribution ?: "") }
    var description by remember(initial) { mutableStateOf(initial?.description ?: "") }
    var notes by remember(initial) { mutableStateOf(initial?.notes ?: "") }
    var toxicity by remember(initial) {
        mutableStateOf(
            initial?.toxicityScope ?: FamilyToxicityScope.SOME_SPECIES
        )
    }
    var toxExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Añadir familia" else "Editar ${initial.family}") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    family,
                    { family = it },
                    label = { Text("Familia *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    common,
                    { common = it },
                    label = { Text("Nombre común ES") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        genera,
                        { genera = it.filter { c -> c.isDigit() } },
                        label = { Text("Géneros") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        species,
                        { species = it.filter { c -> c.isDigit() } },
                        label = { Text("Especies") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    distribution,
                    { distribution = it },
                    label = { Text("Distribución") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    description,
                    { description = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
                ExposedDropdownMenuBox(
                    expanded = toxExpanded,
                    onExpandedChange = { toxExpanded = it }) {
                    OutlinedTextField(
                        readOnly = true,
                        value = toxicity.label,
                        onValueChange = {},
                        label = { Text("Toxicidad") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toxExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = toxExpanded,
                        onDismissRequest = { toxExpanded = false }) {
                        FamilyToxicityScope.entries.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt.label) },
                                onClick = { toxicity = opt; toxExpanded = false })
                        }
                    }
                }
                OutlinedTextField(
                    notes,
                    { notes = it },
                    label = { Text("Notas") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 1
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = family.isNotBlank(),
                onClick = {
                    onSave(
                        ToxicFamily(
                            family = family.trim(),
                            commonNameEs = common.trim(),
                            generaCount = genera.toIntOrNull() ?: 0,
                            speciesCount = species.toIntOrNull() ?: 0,
                            distribution = distribution.trim(),
                            description = description.trim(),
                            toxicityScope = toxicity,
                            notes = notes.trim()
                        )
                    )
                }
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

// helper para el diálogo de borrar – reutiliza el VM
private fun FamilyViewModel.deleteGenus(family: String) = delete(family)