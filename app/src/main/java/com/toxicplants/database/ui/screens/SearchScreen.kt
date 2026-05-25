// ui/screens/SearchScreen.kt
package com.toxicplants.database.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.ui.viewmodel.PlantViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: PlantViewModel,
    onPlantClick: (PlantEntity) -> Unit,
    onBack: () -> Unit
) {
    val plants by viewModel.plantsData.collectAsState()
    val searchQuery by viewModel.searchQueryData.collectAsState()
    val focusRequester = remember { FocusRequester() }
    var plantToDelete by remember { mutableStateOf<PlantEntity?>(null) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        placeholder = { Text("Buscar plantas...") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color.White)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = Color.White)
                                }
                            }
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2E7D32),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ToxicityFilterChips(
                onFilterSelect = { toxicity ->
                    viewModel.setToxicityFilter(toxicity)
                }
            )

            if (plants.isEmpty() && searchQuery.isNotEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔍", fontSize = 64.sp)
                        Text(
                            "No se encontraron plantas para\n\"$searchQuery\"",
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                Text(
                    "${plants.size} plantas encontradas",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(plants) { plant ->
                        PlantCard(
                            plant = plant,
                            onClick = { onPlantClick(plant) },
                            onDeleteClick = { plantToDelete = plant }
                        )
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
                TextButton(
                    onClick = {
                        viewModel.deletePlant(plant)
                        plantToDelete = null
                    }
                ) {
                    Text("Eliminar", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { plantToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun ToxicityFilterChips(onFilterSelect: (String?) -> Unit) {
    val toxicityLevels = listOf("Mortal", "Alto", "Moderado", "Bajo")
    var selectedLevel by remember { mutableStateOf<String?>(null) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        toxicityLevels.forEach { level ->
            FilterChip(
                selected = selectedLevel == level,
                onClick = {
                    selectedLevel = if (selectedLevel == level) null else level
                    onFilterSelect(selectedLevel)
                },
                label = { Text(level, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = when (level) {
                        "Mortal" -> Color(0xFFB71C1C)
                        "Alto" -> Color(0xFFE65100)
                        "Moderado" -> Color(0xFFF57C00)
                        else -> Color(0xFF388E3C)
                    },
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}