package com.toxicplants.database.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.ui.theme.carbonEffectSubtle
import com.toxicplants.database.ui.viewmodel.PlantViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SightingsMapScreen(
    viewModel: PlantViewModel,
    onPlantClick: (PlantEntity) -> Unit,
    onBack: () -> Unit
) {
    val context   = LocalContext.current
    val allPlants by viewModel.allPlants.observeAsState(emptyList())
    val plantsWithLocation = remember(allPlants) { allPlants.filter { it.latitude != null && it.longitude != null } }

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = "PlantasToxicasApp/1.0"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("🗺️ Mapa de avistamientos", fontWeight = FontWeight.Bold, color = Color.White)
                        Text("${plantsWithLocation.size} plantas localizadas", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF2E7D32))
            )
        }
    ) { paddingValues ->
        if (plantsWithLocation.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().carbonEffectSubtle().padding(paddingValues), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🗺️", fontSize = 64.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("Sin avistamientos registrados", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Entra al detalle de una planta\ny pulsa el botón 📍 para registrar\ndónde la encontraste.", color = Color.Gray, fontSize = 14.sp)
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().carbonEffectSubtle().padding(paddingValues)) {
                AndroidView(
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            controller.setZoom(6.0)
                            plantsWithLocation.firstOrNull()?.let { controller.setCenter(GeoPoint(it.latitude!!, it.longitude!!)) }
                            plantsWithLocation.forEach { plant ->
                                val marker = Marker(this)
                                marker.position = GeoPoint(plant.latitude!!, plant.longitude!!)
                                marker.title   = plant.commonName
                                marker.snippet = "${plant.scientificName}\n⚠️ ${plant.toxicityLevel}" +
                                    if (!plant.locationName.isNullOrBlank()) "\n📍 ${plant.locationName}" else ""
                                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                overlays.add(marker)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                Card(
                    modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Avistamientos", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(Modifier.height(4.dp))
                        plantsWithLocation.groupBy { it.toxicityLevel }.forEach { (level, plants) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(when (level) { "Mortal" -> "☠️"; "Muy alto" -> "💀"; "Alto" -> "⚠️"; "Moderado" -> "⚡"; "Bajo" -> "🟢"; else -> "❓" }, fontSize = 12.sp)
                                Spacer(Modifier.width(4.dp))
                                Text("$level: ${plants.size}", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
