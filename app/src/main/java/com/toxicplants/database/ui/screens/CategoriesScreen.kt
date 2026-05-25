package com.toxicplants.database.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toxicplants.database.ui.viewmodel.PlantViewModel  // ✅ Corregido

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    viewModel: PlantViewModel,
    onCategoryClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val categories by viewModel.allCategories.observeAsState(emptyList())

    val categoryEmojis = mapOf(
        "Jardín" to "🌺",
        "Silvestre" to "🌿",
        "Interior" to "🪴",
        "Tropical" to "🌴",
        "Mediterránea" to "🫒",
        "Árbol" to "🌳",
        "Acuática" to "💧",
        "Montaña" to "⛰️",
        "Cultivo" to "🌾",
        "Ruderal" to "🏚️",
        "Costera" to "🌊",
        "Invasora" to "⚠️",
        "Hongo" to "🍄",
        "Trepadora" to "🌱",
        "Canarias" to "🏝️",
        "Cactus" to "🌵",
        "Frutal" to "🍎",
        "Culinaria" to "🌿",
        "Parásita" to "🔗"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🗂️ Categorías", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1976D2),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(categories) { category ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clickable { onCategoryClick(category) },
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1976D2).copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            categoryEmojis[category] ?: "🌿",
                            fontSize = 32.sp
                        )
                        Text(
                            category,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2)
                        )
                    }
                }
            }
        }
    }
}