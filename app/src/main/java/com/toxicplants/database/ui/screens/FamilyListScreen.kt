package com.toxicplants.database.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toxicplants.database.ui.viewmodel.PlantViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyListScreen(
    viewModel: PlantViewModel,
    onBack: () -> Unit,
    onFamilyClick: (String) -> Unit
) {
    val allPlants  by viewModel.allPlants.observeAsState(emptyList())
    val allFamilies by viewModel.allFamilies.observeAsState(emptyList())

    // Para cada familia calculamos cuántas plantas tiene
    val familyCount = remember(allPlants) {
        allPlants.groupBy { it.family }.mapValues { it.value.size }
    }

    // Buscador de familia
    var query by remember { mutableStateOf("") }
    val familiesFiltered = remember(allFamilies, query) {
        if (query.isBlank()) allFamilies
        else allFamilies.filter { it.contains(query, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "📚 Familias botánicas",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2E7D32)
                )
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            // ── Buscador ─────────────────────────────────────────────
            OutlinedTextField(
                value         = query,
                onValueChange = { query = it },
                placeholder   = { Text("Buscar familia…") },
                singleLine    = true,
                modifier      = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                shape  = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = Color(0xFF2E7D32),
                    unfocusedBorderColor = Color(0xFFBDBDBD)
                )
            )

            // ── Contador ─────────────────────────────────────────────
            Text(
                text     = "${familiesFiltered.size} familias · ${allPlants.size} plantas",
                fontSize = 12.sp,
                color    = Color.Gray,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )

            // ── Lista ─────────────────────────────────────────────────
            if (familiesFiltered.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🌿", fontSize = 48.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("No se encontró ninguna familia", color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(familiesFiltered) { family ->
                        val count = familyCount[family] ?: 0
                        FamilyCard(
                            family    = family,
                            plantCount = count,
                            onClick   = { onFamilyClick(family) }
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun FamilyCard(
    family: String,
    plantCount: Int,
    onClick: () -> Unit
) {
    // Color de fondo según tamaño de la familia
    val accentColor = when {
        plantCount >= 100 -> Color(0xFFB71C1C)  // rojo — familia grande
        plantCount >= 50  -> Color(0xFFE65100)  // naranja
        plantCount >= 20  -> Color(0xFF2E7D32)  // verde oscuro
        else              -> Color(0xFF388E3C)  // verde
    }

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape     = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Burbuja con número de plantas
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text       = plantCount.toString(),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 16.sp,
                        color      = accentColor
                    )
                    Text(
                        text     = if (plantCount == 1) "planta" else "plantas",
                        fontSize = 8.sp,
                        color    = accentColor
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            // Nombre de la familia
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = family,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 16.sp,
                    fontStyle  = FontStyle.Italic
                )
                Text(
                    text     = "Familia botánica",
                    fontSize = 11.sp,
                    color    = Color.Gray
                )
            }

            Icon(
                imageVector        = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint               = Color.Gray
            )
        }
    }
}
