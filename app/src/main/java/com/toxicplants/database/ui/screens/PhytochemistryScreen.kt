package com.toxicplants.database.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyColumnItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toxicplants.database.CompoundEntity
import com.toxicplants.database.ui.viewmodel.CompoundViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhytochemistryScreen(
    viewModel: CompoundViewModel,
    onCompoundClick: (CompoundEntity) -> Unit = {},
    onGroupClick: (String) -> Unit,
    onAddCompoundClick: () -> Unit,
    onBack: () -> Unit,
) {
    val all by viewModel.allCompounds.observeAsState(emptyList())
    val groups by viewModel.allGroups.observeAsState(emptyList())
    var query by remember { mutableStateOf("") }

    val filteredGroups = remember(groups, query) {
        if (query.isBlank()) groups
        else groups.filter { it.contains(query, ignoreCase = true) }
    }

    val filteredCompounds = remember(all, query) {
        if (query.isBlank()) emptyList()
        else all.filter {
            it.commonName.contains(query, ignoreCase = true) ||
            it.iupacName.contains(query, ignoreCase = true) ||
            it.groupName.contains(query, ignoreCase = true)
        }
    }

    val countByGroup = remember(all) { all.groupingBy { it.groupName }.eachCount() }
    val colorByGroup = remember(all) { all.associate { it.groupName to it.groupColor } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Fitoquímica", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(
                            "${all.size} componentes, ${groups.size} grupos",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = onAddCompoundClick) {
                        Icon(Icons.Filled.Add, contentDescription = "Añadir")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF512DA8),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                placeholder = { Text("Buscar grupo o componente…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Limpiar")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
            )

            if (query.isNotBlank() && (filteredGroups.isNotEmpty() || filteredCompounds.isNotEmpty())) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (filteredGroups.isNotEmpty()) {
                        item {
                            Text("Grupos Encontrados", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                        }
                        lazyColumnItems(filteredGroups) { group ->
                            val color = parseColor(colorByGroup[group] ?: "#7B1FA2")
                            Card(
                                onClick = { onGroupClick(group) },
                                colors = CardDefaults.cardColors(containerColor = color),
                                modifier = Modifier.fillMaxWidth().height(60.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.Science, contentDescription = null, tint = Color.White)
                                    Spacer(Modifier.width(8.dp))
                                    Text(text = group, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    if (filteredCompounds.isNotEmpty()) {
                        item {
                            Text("Componentes Encontrados", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                        }
                        lazyColumnItems(filteredCompounds) { compound ->
                            val mainColor = parseColor(compound.groupColor)
                            CompoundCard(
                                compound = compound,
                                onClick = { onCompoundClick(compound) },
                                mainColor = mainColor,
                                onEdit = { /* Opcional, podría ir a edit desde aquí */ },
                                onDelete = { /* Opcional */ }
                            )
                        }
                    }
                }
            } else if (query.isNotBlank() && filteredGroups.isEmpty() && filteredCompounds.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No se encontraron resultados", color = Color.Gray)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filteredGroups, key = { it }) { group ->
                        val color = parseColor(colorByGroup[group] ?: "#7B1FA2")
                        val count = countByGroup[group] ?: 0

                        Card(
                            onClick = { onGroupClick(group) },
                            colors = CardDefaults.cardColors(containerColor = color),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(108.dp),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Row {
                                    Icon(
                                        Icons.Filled.Science,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = group,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 3,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "$count sustancia${if (count == 1) "" else "s"}",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 11.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
