package com.toxicplants.database.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.ui.theme.carbonEffectSubtle
import com.toxicplants.database.PlantExtraDataSource
import com.toxicplants.database.ui.viewmodel.PlantViewModel

// ── Datos de colores ──────────────────────────────────────────────────────

private data class ColorInfo(
    val name: String,
    val emoji: String,
    val color: Color
)

private val flowerColors = listOf(
    ColorInfo("blanco",   "🤍", Color(0xFFF5F5F5)),
    ColorInfo("amarillo", "💛", Color(0xFFFDD835)),
    ColorInfo("rojo",     "❤️", Color(0xFFE53935)),
    ColorInfo("morado",   "💜", Color(0xFF8E24AA)),
    ColorInfo("rosa",     "🌸", Color(0xFFEC407A)),
    ColorInfo("azul",     "💙", Color(0xFF1E88E5)),
    ColorInfo("naranja",  "🧡", Color(0xFFFB8C00)),
    ColorInfo("verde",    "💚", Color(0xFF43A047)),
    ColorInfo("sin flor", "🌿", Color(0xFF78909C))
)

private val fruitColors = listOf(
    ColorInfo("rojo",      "🔴", Color(0xFFE53935)),
    ColorInfo("negro",     "⚫", Color(0xFF212121)),
    ColorInfo("naranja",   "🟠", Color(0xFFFB8C00)),
    ColorInfo("amarillo",  "🟡", Color(0xFFFDD835)),
    ColorInfo("verde",     "🟢", Color(0xFF43A047)),
    ColorInfo("azul",      "🔵", Color(0xFF1E88E5)),
    ColorInfo("blanco",    "⚪", Color(0xFFF5F5F5)),
    ColorInfo("sin fruto", "🚫", Color(0xFF78909C))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorSearchScreen(
    viewModel: PlantViewModel,
    onPlantClick: (PlantEntity) -> Unit,
    onBack: () -> Unit,
    onAddPlant: (mode: String, color: String) -> Unit = { _, _ -> },
    onEditPlant: (Int) -> Unit = {}
) {
    val context   = LocalContext.current
    val allPlants by viewModel.allPlants.observeAsState(emptyList())
    val extraMap  = remember { PlantExtraDataSource.loadAll(context).toMutableMap() }

    // Estado de selección
    var selectedTab     by remember { mutableIntStateOf(0) }
    var refreshKey      by remember { mutableIntStateOf(0) }   // 0=flor, 1=fruto
    var selectedColor   by remember { mutableStateOf<String?>(null) }

    val colorList = if (selectedTab == 0) flowerColors else fruitColors

    // Plantas filtradas
    val filteredPlants = remember(allPlants, extraMap, selectedTab, selectedColor, refreshKey) {
        if (selectedColor == null) emptyList()
        else allPlants.filter { plant ->
            val extra = extraMap[plant.scientificName] ?: return@filter false
            if (selectedTab == 0)
                extra.flowerColor.equals(selectedColor, ignoreCase = true)
            else
                extra.fruitColor.equals(selectedColor, ignoreCase = true)
        }.sortedByDescending {
            when (it.toxicityLevel) {
                "Mortal" -> 5; "Muy alto" -> 4; "Alto" -> 3; "Moderado" -> 2; "Bajo" -> 1; else -> 0
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("🌸 Buscar por color", fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            if (selectedColor == null) "Selecciona un color"
                            else "${filteredPlants.size} plantas encontradas",
                            fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val mode  = if (selectedTab == 0) "flowerColor" else "fruitColor"
                        val color = selectedColor ?: ""
                        if (color.isNotBlank()) {
                            onAddPlant(mode, color)
                        }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Añadir", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF880E4F))
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().carbonEffectSubtle().padding(paddingValues)) {

            // Tabs flor / fruto
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor   = Color(0xFF880E4F),
                contentColor     = Color.White
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick  = { selectedTab = 0; selectedColor = null },
                    text     = { Text("🌸 Color de flor", fontSize = 13.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick  = { selectedTab = 1; selectedColor = null },
                    text     = { Text("🍒 Color de fruto", fontSize = 13.sp) }
                )
            }

            // Paleta de colores
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "Selecciona un color:",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 14.sp,
                    modifier   = Modifier.padding(bottom = 8.dp)
                )

                // Primera fila (5 colores)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    colorList.take(5).forEach { info ->
                        ColorChip(
                            modifier  = Modifier.weight(1f),
                            info      = info,
                            selected  = selectedColor == info.name,
                            onClick   = {
                                selectedColor = if (selectedColor == info.name) null else info.name
                            }
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Segunda fila (resto)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    colorList.drop(5).forEach { info ->
                        ColorChip(
                            modifier = Modifier.weight(1f),
                            info     = info,
                            selected = selectedColor == info.name,
                            onClick  = {
                                selectedColor = if (selectedColor == info.name) null else info.name
                            }
                        )
                    }
                    // Relleno para alinear si hay menos de 5 en la segunda fila
                    repeat(5 - colorList.drop(5).size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            HorizontalDivider()

            // Resultados
            when {
                selectedColor == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🌸", fontSize = 56.sp)
                            Spacer(Modifier.height(12.dp))
                            Text("Pulsa un color para ver las plantas", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                }
                filteredPlants.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔍", fontSize = 48.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("Sin plantas con ese color", color = Color.Gray)
                            Text("(datos limitados al catálogo extra)", color = Color.Gray, fontSize = 11.sp)
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding      = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredPlants) { plant ->
                            ColorPlantCard(
                                plant    = plant,
                                onClick  = { onPlantClick(plant) },
                                onRemove = {
                                    if (selectedTab == 0) PlantExtraDataSource.setFlowerColor(context, plant.scientificName, "")
                                    else PlantExtraDataSource.setFruitColor(context, plant.scientificName, "")
                                    PlantExtraDataSource.clearCache()
                                    extraMap.clear()
                                    extraMap.putAll(PlantExtraDataSource.loadAll(context))
                                    refreshKey++
                                }
                            )
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

// ── Chip de color ─────────────────────────────────────────────────────────

@Composable
private fun ColorChip(
    modifier: Modifier = Modifier,
    info: ColorInfo,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bgColor  = if (selected) info.color else info.color.copy(alpha = 0.15f)
    val txtColor = if (selected) Color.White else info.color.copy(alpha = 0.8f)
    val border   = if (selected) 3.dp else 0.dp

    Box(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(info.emoji, fontSize = 20.sp)
            Spacer(Modifier.height(2.dp))
            Text(
                text      = info.name,
                fontSize  = 9.sp,
                color     = txtColor,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines  = 1
            )
        }
    }
}

// ── Tarjeta de planta en resultado ────────────────────────────────────────

@Composable
private fun ColorPlantCard(plant: PlantEntity, onClick: () -> Unit, onRemove: () -> Unit = {}) {
    val toxColor = when (plant.toxicityLevel) {
        "Mortal"   -> Color(0xFFB71C1C)
        "Muy alto" -> Color(0xFFFF5722)
        "Alto"     -> Color(0xFFE65100)
        "Moderado" -> Color(0xFFF57C00)
        "Bajo"     -> Color(0xFF388E3C)
        else       -> Color.Gray
    }

    Card(
        modifier  = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(toxColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    when (plant.toxicityLevel) {
                        "Mortal" -> "☠️"; "Muy alto" -> "💀"; "Alto" -> "⚠️"
                        "Moderado" -> "⚡"; "Bajo" -> "🟢"; else -> "❓"
                    }, fontSize = 22.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(plant.commonName, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(plant.scientificName, fontSize = 12.sp, fontStyle = FontStyle.Italic,
                    color = Color.Gray, maxLines = 1)
            }
            Surface(color = toxColor.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                Text(
                    plant.toxicityLevel,
                    modifier   = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    fontSize   = 10.sp,
                    color      = toxColor,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(
                onClick  = onRemove,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Quitar", tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
            }
        }
    }
}
