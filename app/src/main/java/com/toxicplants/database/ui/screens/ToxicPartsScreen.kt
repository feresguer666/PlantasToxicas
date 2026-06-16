package com.toxicplants.database.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.ui.viewmodel.PlantViewModel

// ── Definición de filtros ─────────────────────────────────────────────────

private data class PartFilter(
    val label: String,
    val emoji: String,
    val color: Color,
    val keywords: List<String>   // palabras que busca en toxicParts
)

private val PART_FILTERS = listOf(
    PartFilter("Toda la planta", "🌿", Color(0xFF2E7D32),
        listOf("toda", "planta entera", "toda la planta", "todas las partes")),
    PartFilter("Fruto / Baya", "🍒", Color(0xFFE53935),
        listOf("fruto", "baya", "bayas", "frutos", "fruta", "baya", "cereza", "drupa")),
    PartFilter("Semillas", "🌰", Color(0xFF795548),
        listOf("semilla", "semillas", "pepita", "pepitas", "grano", "granos")),
    PartFilter("Raíz", "🪱", Color(0xFF6D4C41),
        listOf("raíz", "raiz", "raíces", "raices", "tubérculo", "tuberculo", "bulbo", "rizoma")),
    PartFilter("Hojas", "🍃", Color(0xFF388E3C),
        listOf("hoja", "hojas", "follaje", "agujas", "filodio")),
    PartFilter("Corteza", "🪵", Color(0xFF8D6E63),
        listOf("corteza", "cortezas", "cáscara", "cascara")),
    PartFilter("Flores", "🌸", Color(0xFFAD1457),
        listOf("flor", "flores", "inflorescencia", "néctar", "nectar", "polen")),
    PartFilter("Tallo / Savia", "🌱", Color(0xFF558B2F),
        listOf("tallo", "tallos", "savia", "látex", "latex", "resina", "jugo")),
    PartFilter("Por contacto", "🤚", Color(0xFFE65100),
        listOf("contacto", "contacto con la piel", "roce", "piel", "dermatitis", "urticante")),
    PartFilter("Por ingestión", "🍽️", Color(0xFFB71C1C),
        listOf("ingestión", "ingestion", "ingerir", "ingerido", "comer", "consumir", "beber")),
)

// ── Pantalla ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToxicPartsScreen(
    viewModel: PlantViewModel,
    onPlantClick: (PlantEntity) -> Unit,
    onBack: () -> Unit
) {
    val allPlants by viewModel.allPlants.observeAsState(emptyList())

    var selectedFilter by remember { mutableStateOf<PartFilter?>(null) }

    // Plantas filtradas según la selección
    val filteredPlants = remember(allPlants, selectedFilter) {
        val filter = selectedFilter ?: return@remember emptyList()
        allPlants.filter { plant ->
            val parts = plant.toxicParts.lowercase()
            val symptoms = plant.symptoms.lowercase()
            filter.keywords.any { kw ->
                parts.contains(kw) || symptoms.contains(kw)
            }
        }.sortedByDescending {
            when (it.toxicityLevel) {
                "Mortal" -> 5; "Muy alto" -> 4; "Alto" -> 3
                "Moderado" -> 2; "Bajo" -> 1; else -> 0
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "☠️ Buscar por parte tóxica",
                            fontWeight = FontWeight.Bold,
                            color      = Color.White
                        )
                        Text(
                            if (selectedFilter == null) "Selecciona una categoría"
                            else "${filteredPlants.size} plantas — ${selectedFilter!!.label}",
                            fontSize = 11.sp,
                            color    = Color.White.copy(alpha = 0.8f)
                        )
                    }
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
                    containerColor = Color(0xFF4A148C)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            // ── Cuadrícula de filtros compacta (5 columnas × 2 filas) ────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Fila 1: primeros 5 filtros
                Row(
                    modifier             = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PART_FILTERS.take(5).forEach { filter ->
                        PartFilterButton(
                            filter   = filter,
                            selected = selectedFilter == filter,
                            modifier = Modifier.weight(1f),
                            onClick  = {
                                selectedFilter = if (selectedFilter == filter) null else filter
                            }
                        )
                    }
                }
                // Fila 2: últimos 5 filtros
                Row(
                    modifier             = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PART_FILTERS.drop(5).forEach { filter ->
                        PartFilterButton(
                            filter   = filter,
                            selected = selectedFilter == filter,
                            modifier = Modifier.weight(1f),
                            onClick  = {
                                selectedFilter = if (selectedFilter == filter) null else filter
                            }
                        )
                    }
                }

                // Indicador del filtro activo + botón limpiar
                if (selectedFilter != null) {
                    Row(
                        modifier             = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment    = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = selectedFilter!!.color.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "${selectedFilter!!.emoji} ${selectedFilter!!.label} · ${filteredPlants.size} plantas",
                                modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize   = 11.sp,
                                color      = selectedFilter!!.color,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        TextButton(
                            onClick        = { selectedFilter = null },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("✕ Limpiar", fontSize = 11.sp)
                        }
                    }
                }
            }

            HorizontalDivider()

            // ── Resultados ───────────────────────────────────────────────
            when {
                selectedFilter == null -> {
                    Box(
                        modifier         = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("☠️", fontSize = 56.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Selecciona una parte de la planta\npara ver qué especies son tóxicas",
                                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize  = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                filteredPlants.isEmpty() -> {
                    Box(
                        modifier         = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔍", fontSize = 48.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("Sin resultados para esta categoría", color = Color.Gray)
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding      = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredPlants) { plant ->
                            ToxicPartPlantCard(
                                plant          = plant,
                                filterKeywords = selectedFilter!!.keywords,
                                filterColor    = selectedFilter!!.color,
                                onClick        = { onPlantClick(plant) }
                            )
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

// ── Botón de filtro ───────────────────────────────────────────────────────

@Composable
private fun PartFilterButton(
    filter: PartFilter,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val gradient = if (selected)
        Brush.verticalGradient(listOf(filter.color, filter.color.copy(alpha = 0.75f)))
    else
        Brush.verticalGradient(listOf(filter.color.copy(alpha = 0.15f), filter.color.copy(alpha = 0.08f)))

    val textColor   = if (selected) Color.White else filter.color
    if (selected) Color.Transparent else filter.color.copy(alpha = 0.3f)

    Box(
        modifier = modifier
            .height(60.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(gradient)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.padding(horizontal = 1.dp, vertical = 4.dp)
        ) {
            Text(filter.emoji, fontSize = 18.sp)
            Text(
                text       = filter.label,
                color      = textColor,
                fontSize   = 9.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                textAlign  = TextAlign.Center,
                maxLines   = 2,
                lineHeight = 10.sp
            )
        }
    }
}

// ── Tarjeta de planta con parte tóxica resaltada ──────────────────────────

@Composable
private fun ToxicPartPlantCard(
    plant: PlantEntity,
    filterKeywords: List<String>,
    filterColor: Color,
    onClick: () -> Unit
) {
    val toxColor = when (plant.toxicityLevel) {
        "Mortal"   -> Color(0xFFB71C1C)
        "Muy alto" -> Color(0xFFFF5722)
        "Alto"     -> Color(0xFFE65100)
        "Moderado" -> Color(0xFFF57C00)
        "Bajo"     -> Color(0xFF388E3C)
        else       -> Color.Gray
    }

    // Extrae qué keyword coincidió en toxicParts para mostrarla resaltada
    filterKeywords.firstOrNull { kw ->
        plant.toxicParts.contains(kw, ignoreCase = true)
    }

    Card(
        modifier  = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier          = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono toxicidad
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(toxColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    when (plant.toxicityLevel) {
                        "Mortal"   -> "☠️"; "Muy alto" -> "💀"; "Alto"     -> "⚠️"
                        "Moderado" -> "⚡"; "Bajo"     -> "🟢"; else       -> "❓"
                    },
                    fontSize = 22.sp
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Nombre
                Text(
                    plant.commonName,
                    fontWeight = FontWeight.Bold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Text(
                    plant.scientificName,
                    fontSize  = 12.sp,
                    fontStyle = FontStyle.Italic,
                    color     = Color.Gray,
                    maxLines  = 1
                )

                Spacer(Modifier.height(4.dp))

                // Partes tóxicas con la keyword resaltada
                if (plant.toxicParts.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = filterColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "☠️ ${plant.toxicParts}",
                                modifier   = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize   = 11.sp,
                                color      = filterColor,
                                fontWeight = FontWeight.Medium,
                                maxLines   = 2,
                                overflow   = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Síntomas (preview)
                if (plant.symptoms.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        plant.symptoms,
                        fontSize  = 11.sp,
                        color     = Color(0xFF666666),
                        maxLines  = 2,
                        overflow  = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // Badge toxicidad
            Surface(
                color = toxColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    plant.toxicityLevel,
                    modifier   = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    fontSize   = 10.sp,
                    color      = toxColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
