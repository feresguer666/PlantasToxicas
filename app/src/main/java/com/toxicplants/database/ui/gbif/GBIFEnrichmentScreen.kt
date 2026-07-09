package com.toxicplants.database.ui.gbif

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GBIFEnrichmentScreen(
    viewModel: GBIFViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val searchState by viewModel.searchResult.collectAsState()
    val vernacularNames by viewModel.vernacularNames.collectAsState()
    val occurrences by viewModel.occurrences.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🌍 Buscar en GBIF") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Nombre científico") },
                placeholder = { Text("Ej: Atropa belladonna") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (searchQuery.isNotBlank()) {
                            viewModel.searchSpecies(searchQuery.trim())
                            focusManager.clearFocus()
                        }
                    }
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (searchQuery.isNotBlank()) {
                        viewModel.searchSpecies(searchQuery.trim())
                        focusManager.clearFocus()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = searchQuery.isNotBlank()
            ) {
                Icon(Icons.Default.Search, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Buscar en GBIF")
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (val state = searchState) {
                is SearchResultState.Idle -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Science,
                                null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Escribe un nombre científico y presiona Buscar")
                        }
                    }
                }
                is SearchResultState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is SearchResultState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        SpeciesMatchCard(
                            match = state.match,
                            vernacularNames = vernacularNames,
                            occurrences = occurrences,
                            onLoadVernacular = { viewModel.loadVernacularNames(state.match.usageKey!!) },
                            onLoadOccurrences = { viewModel.loadOccurrences(state.match.usageKey!!) }
                        )
                    }
                }
                is SearchResultState.NotFound -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        NotFoundCard(query = state.query)
                    }
                }
                is SearchResultState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        ErrorCard(message = state.message)
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeciesMatchCard(
    match: GBIFSpeciesMatch,
    vernacularNames: List<GBIFVernacularName>,
    occurrences: List<GBIFOccurrence>,
    onLoadVernacular: () -> Unit,
    onLoadOccurrences: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("✅ Encontrado", fontWeight = FontWeight.Bold)
                match.confidence?.let {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(" $it% ", modifier = Modifier.padding(4.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = match.scientificName ?: "—",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text("📂 Clasificación:", fontWeight = FontWeight.Bold)
            Text("Reino: ${match.kingdom ?: "—"}")
            Text("Familia: ${match.family ?: "—"}")
            Text("Género: ${match.genus ?: "—"}")
            Text("Estado: ${match.status ?: "—"}")

            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onLoadVernacular) {
                    Icon(Icons.Default.Translate, null, Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nombres")
                }
                OutlinedButton(onClick = onLoadOccurrences) {
                    Icon(Icons.Default.Map, null, Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Mapa")
                }
            }

            if (vernacularNames.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("🌍 Nombres comunes:", fontWeight = FontWeight.Bold)
                vernacularNames.forEach { name ->
                    Text("• ${name.name} (${name.language ?: "?"})")
                }
            }

            if (occurrences.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("📍 Avistamientos (${occurrences.size}):", fontWeight = FontWeight.Bold)
                occurrences.forEach { occ ->
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text("📍 ")
                        Text(
                            text = "${occ.decimalLatitude?.let { "%.2f".format(it) } ?: "?"}, " +
                                    "${occ.decimalLongitude?.let { "%.2f".format(it) } ?: "?"}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    occ.countryCode?.let {
                        Text("   País: $it", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
private fun NotFoundCard(query: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.SearchOff, null, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("No encontrado: $query")
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Error, null)
            Spacer(modifier = Modifier.width(12.dp))
            Text(message)
        }
    }
}