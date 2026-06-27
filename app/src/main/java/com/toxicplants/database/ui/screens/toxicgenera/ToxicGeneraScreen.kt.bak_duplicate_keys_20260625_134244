package com.toxicplants.database.ui.screens.toxicgenera

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ToxicGeneraScreen(onBack: () -> Unit, onGenusClick: (String) -> Unit, vm: ToxicGeneraViewModel = viewModel()) {
    val genera by vm.genera.collectAsState()
    val query by vm.query.collectAsState()
    var sortByFichas by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ToxicGenus?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<ToxicGenus?>(null) }

    val filtered = remember(genera, query, sortByFichas) {
        val q = query.lowercase()
        val list = if (q.isBlank()) genera else genera.filter {
            it.genus.genus.lowercase().contains(q) || it.genus.commonNameEs.lowercase().contains(q) || it.genus.family.lowercase().contains(q)
        }
        if (sortByFichas) list.sortedByDescending { it.fichasLocal } else list.sortedBy { it.genus.genus }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("☠️ Géneros Tóxicos", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = { IconButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, "Añadir", tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFB71C1C), titleContentColor = Color.White, navigationIconContentColor = Color.White, actionIconContentColor = Color.White))
        },
        floatingActionButton = { FloatingActionButton(onClick = { showAdd = true }, containerColor = Color(0xFFB71C1C)) { Icon(Icons.Default.Add, contentDescription = "Añadir género", tint = Color.White) } }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            OutlinedTextField(value = query, onValueChange = vm::setQuery, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar género, familia…") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true)
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${filtered.size} géneros", fontSize = 12.sp, color = Color.Gray)
                TextButton(onClick = { sortByFichas = !sortByFichas }) { Text(if (sortByFichas) "Orden: fichas ↓" else "Orden: A-Z") }
            }
            Text("Mantén pulsado un género para editar / borrar", fontSize = 11.sp, color = Color.Gray)
            Spacer(Modifier.height(4.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered, key = { it.genus.genus }) { item ->
                    Card(modifier = Modifier.fillMaxWidth().combinedClickable(
                        onClick = { onGenusClick(item.genus.genus) }, onLongClick = { editing = item.genus }),
                        colors = CardDefaults.cardColors(containerColor = if (item.isUserEdited) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${item.genus.genus} spp.", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    if (item.isUserEdited) { Spacer(Modifier.width(6.dp)); AssistChip(onClick = {}, enabled = false, label = { Text("editado", fontSize = 9.sp) }) }
                                }
                                Text(item.genus.commonNameEs, fontSize = 13.sp, color = Color.Gray)
                                Text(item.genus.family, fontSize = 11.sp, color = Color.Gray)
                                Text(item.genus.toxicityNote, fontSize = 11.sp, color = Color(0xFFD32F2F))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Comprende", fontSize = 10.sp, color = Color.Gray)
                                Text("${item.genus.speciesCount} especies", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Spacer(Modifier.height(4.dp))
                                AssistChip(onClick = {}, label = { Text("fichas=${item.fichasLocal}", fontSize = 11.sp) })
                                Row {
                                    IconButton(onClick = { editing = item.genus }) { Icon(Icons.Default.Edit, "Editar", modifier = Modifier.size(18.dp), tint = Color(0xFF1565C0)) }
                                    IconButton(onClick = { toDelete = item.genus }) { Icon(Icons.Default.Delete, "Borrar", modifier = Modifier.size(18.dp), tint = Color(0xFFD32F2F)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (showAdd || editing != null) {
        EditGenusDialog(initial = editing, onDismiss = { showAdd = false; editing = null },
            onSave = { genus, family, common, count, note, gbif ->
                vm.saveGenus(editing?.genus, genus, family, common, count, note, gbif); showAdd = false; editing = null
            })
    }
    toDelete?.let { g ->
        AlertDialog(onDismissRequest = { toDelete = null }, title = { Text("¿Eliminar ${g.genus}?") },
            text = { Text("Se ocultará de la lista. Tus fichas de plantas NO se borran.") },
            confirmButton = { TextButton(onClick = { vm.deleteGenus(g.genus); toDelete = null }) { Text("Eliminar", color = Color.Red) } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancelar") } })
    }
}

@Composable fun EditGenusDialog(initial: ToxicGenus?, onDismiss: () -> Unit, onSave: (genus: String, family: String, commonNameEs: String, speciesCount: Int, toxicityNote: String, gbifKey: Long?) -> Unit) {
    var genus by remember(initial) { mutableStateOf(initial?.genus ?: "") }
    var family by remember(initial) { mutableStateOf(initial?.family ?: "") }
    var common by remember(initial) { mutableStateOf(initial?.commonNameEs ?: "") }
    var countStr by remember(initial) { mutableStateOf(initial?.speciesCount?.toString() ?: "") }
    var note by remember(initial) { mutableStateOf(initial?.toxicityNote ?: "") }
    var gbifStr by remember(initial) { mutableStateOf(initial?.gbifGenusKey?.toString() ?: "") }
    val canSave = genus.isNotBlank()
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (initial == null) "Añadir género tóxico" else "Editar ${initial.genus}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(genus, { genus = it }, label = { Text("Género *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(family, { family = it }, label = { Text("Familia") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(common, { common = it }, label = { Text("Nombre común ES") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(countStr, { countStr = it.filter { c -> c.isDigit() } }, label = { Text("Nº especies") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(note, { note = it }, label = { Text("Nota toxicológica") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(gbifStr, { gbifStr = it.filter { c -> c.isDigit() } }, label = { Text("GBIF genusKey (opcional)") }, supportingText = { Text("gbif.org, ej. Aconitum = 3033662", fontSize = 10.sp) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(enabled = canSave, onClick = { onSave(genus, family, common, countStr.toIntOrNull() ?: 0, note, gbifStr.toLongOrNull()) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}
