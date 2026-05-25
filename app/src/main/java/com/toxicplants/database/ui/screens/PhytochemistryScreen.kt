package com.toxicplants.database.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Science
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
import com.toxicplants.database.CompoundEntity
import com.toxicplants.database.ui.components.MoleculesHeader
import com.toxicplants.database.ui.viewmodel.CompoundViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhytochemistryScreen(
    viewModel: CompoundViewModel,
    onCompoundClick: (CompoundEntity) -> Unit,
    onGroupClick: (String) -> Unit,
    onBack: () -> Unit,
) {
    val all by viewModel.allCompounds.observeAsState(emptyList())
    val groups by viewModel.allGroups.observeAsState(emptyList())
    var query by remember { mutableStateOf("") }

    // Filtrado en memoria: barato (≤ algunos cientos de compuestos)
    val filtered = remember(all, query) {
        if (query.isBlank()) all
        else all.filter { c ->
            val q = query.trim()
            c.commonName.contains(q, ignoreCase = true) ||
                c.iupacName.contains(q, ignoreCase = true) ||
                c.groupName.contains(q, ignoreCase = true) ||
                c.sourcePlants.contains(q, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Fitoquímica", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(
                            "${all.size} compuestos · ${groups.size} grupos",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF512DA8),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Cabecera animada
            item {
                MoleculesHeader(height = 170.dp)
            }

            // Buscador
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    placeholder = { Text("Buscar compuesto, grupo o planta…") },
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
            }

            // Grupos (solo si no se está buscando)
            if (query.isBlank()) {
                item {
                    Text(
                        "Grupos fitoquímicos",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                item {
                    GroupsGrid(
                        groups = groups,
                        countByGroup = remember(all) { all.groupingBy { it.groupName }.eachCount() },
                        colorByGroup = remember(all) {
                            all.associate { it.groupName to it.groupColor }
                        },
                        onGroupClick = onGroupClick,
                    )
                }

                item {
                    Text(
                        "Todos los compuestos",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            } else {
                item {
                    Text(
                        "${filtered.size} resultados",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray,
                    )
                }
            }

            items(filtered, key = { it.id }) { compound ->
                CompoundRow(compound = compound, onClick = { onCompoundClick(compound) })
            }
        }
    }
}

@Composable
private fun GroupsGrid(
    groups: List<String>,
    countByGroup: Map<String, Int>,
    colorByGroup: Map<String, String>,
    onGroupClick: (String) -> Unit,
) {
    // Altura calculada: 2 cols, 80dp por fila + spacing
    val rows = ((groups.size + 1) / 2).coerceAtLeast(1)
    val gridHeight = (rows * 116).dp

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxWidth()
            .height(gridHeight)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        userScrollEnabled = false,
    ) {
        items(groups, key = { it }) { group ->
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
                    Row(verticalAlignment = Alignment.Top) {
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
                            fontSize = 12.sp,
                            lineHeight = 14.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "$count sustancia${if (count == 1) "" else "s"}",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 10.sp,
                    )
                }
            }
        }
    }
}

@Composable
fun CompoundRow(compound: CompoundEntity, onClick: () -> Unit) {
    val color = parseColor(compound.groupColor)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Science,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    compound.commonName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (compound.iupacName.isNotBlank() && compound.iupacName != compound.commonName) {
                    Text(
                        compound.iupacName,
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Surface(
                        color = color.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Text(
                            compound.groupName,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 11.sp,
                            color = color,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    if (compound.molecularFormula.isNotBlank()) {
                        Surface(
                            color = Color.Gray.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(4.dp),
                        ) {
                            Text(
                                compound.molecularFormula,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 11.sp,
                                color = Color.DarkGray,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Helper para parsear "#RRGGBB" → Color sin crash si viene mal. */
fun parseColor(hex: String): Color = try {
    val clean = hex.trim().removePrefix("#")
    Color(("FF$clean".toLong(16)).toInt())
} catch (e: Exception) {
    Color(0xFF7B1FA2)
}
