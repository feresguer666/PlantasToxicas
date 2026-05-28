package com.toxicplants.database.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.ui.viewmodel.PlantViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BerriesGuideScreen(viewModel: PlantViewModel, onBack: () -> Unit) {
    val allPlants by viewModel.allPlants.observeAsState(emptyList())
    val uriHandler = LocalUriHandler.current

    val berries = allPlants.filter {
        it.category.contains("Baya", ignoreCase = true) ||
        it.commonName.contains("Baya", ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🍒 Guía de Bayas Tóxicas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor             = Color(0xFFC62828),
                    titleContentColor          = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (berries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No se encontraron bayas en la base de datos.")
            }
        } else {
            LazyColumn(
                modifier        = Modifier.fillMaxSize().padding(padding),
                contentPadding  = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(berries) { plant ->
                    BerryGuideItem(plant) {
                        val name    = plant.scientificName.ifBlank { plant.commonName }
                        val wikiUrl = "https://es.wikipedia.org/wiki/${name.replace(" ", "_")}"
                        uriHandler.openUri(wikiUrl)
                    }
                }
            }
        }
    }
}

@Composable
fun BerryGuideItem(plant: PlantEntity, onWikiClick: () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth().clickable { onWikiClick() },
        colors    = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🍒", fontSize = 30.sp)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(plant.commonName,     fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(plant.scientificName, fontStyle  = FontStyle.Italic, fontSize = 13.sp, color = Color.Gray)
                Spacer(Modifier.height(4.dp))
                Text("Ver en Wikipedia ↗", fontSize = 12.sp, color = Color(0xFF1976D2), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
