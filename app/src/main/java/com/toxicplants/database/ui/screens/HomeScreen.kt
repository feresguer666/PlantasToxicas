package com.toxicplants.database.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.ui.viewmodel.PlantViewModel

// ─────────────────────────────────────────────
// Paleta de verdes (escala oscuro → claro)
// ─────────────────────────────────────────────
private object GreenScale {
    // Fondo general
    val bg0 = Color(0xFF060F07)
    val bg1 = Color(0xFF0A1A0C)
    val bg2 = Color(0xFF0D2410)

    // Botones — de más oscuro (fila 1) a más claro (fila 3)
    // Fila 1: verde bosque muy oscuro
    val row1Start = Color(0xFF1B3A1E)
    val row1End   = Color(0xFF245228)
    // Fila 2: verde musgo medio
    val row2Start = Color(0xFF2E6B34)
    val row2End   = Color(0xFF3A8A41)
    // Fila 3: verde hoja claro-medio
    val row3Start = Color(0xFF4CAF50)
    val row3End   = Color(0xFF66BB6A)

    // TopBar
    val topBar = Color(0xFF0D3311)
}

// Marrón para Recursos
private val brownStart = Color(0xFF4E2A04)
private val brownEnd   = Color(0xFF7B4A1A)

// Rojo Emergencia
private val redStart = Color(0xFF7F0000)
private val redEnd   = Color(0xFFB71C1C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: PlantViewModel,
    onNavigateToList: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToEmergency: () -> Unit,
    onNavigateToOnlineDatabases: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToDownloadImages: () -> Unit,
    onNavigateToNewPlant: () -> Unit,
    onNavigateToCamera: () -> Unit,
    onNavigateToPhytochemistry: () -> Unit,
    onNavigateToSearchBySymptoms: () -> Unit,
    onNavigateToAR: () -> Unit,
    onNavigateToBerries: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onPlantClick: (PlantEntity) -> Unit,
) {
    val allPlants  by viewModel.allPlants.observeAsState(emptyList())
    val allFamilies by viewModel.allFamilies.observeAsState(emptyList())

    val mortalCount     = allPlants.count { it.toxicityLevel == "Mortal" }
    val altoRiesgoCount = allPlants.count { it.toxicityLevel == "Alto" }

    var showSearchDialog   by remember { mutableStateOf(false) }
    var showIdentifyDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "🌿 Plantas Tóxicas",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 26.sp,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenScale.topBar),
                actions = {
                    IconButton(onClick = onNavigateToDownloadImages) {
                        Icon(Icons.Filled.Download, contentDescription = "Descargar imágenes",
                            modifier = Modifier.size(26.dp), tint = Color.White)
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Ajustes",
                            modifier = Modifier.size(26.dp), tint = Color.White)
                    }
                    IconButton(onClick = onNavigateToNewPlant) {
                        Text("+", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(GreenScale.bg0, GreenScale.bg1, GreenScale.bg2)
                    )
                ),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {

            // ── Banner EMERGENCIA ──────────────────────────────────────
            item {
                BannerCard(
                    gradient = Brush.horizontalGradient(listOf(redStart, redEnd)),
                    height   = 80.dp,
                    onClick  = onNavigateToEmergency
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = null,
                        tint = Color.White, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("EMERGENCIA", color = Color.White,
                            fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("☎ 91 562 04 20",
                            color = Color.White.copy(alpha = 0.92f), fontSize = 14.sp)
                    }
                }
            }

            // ── Banner RECURSOS ────────────────────────────────────────
            item {
                BannerCard(
                    gradient = Brush.horizontalGradient(listOf(brownStart, brownEnd)),
                    height   = 80.dp,
                    onClick  = onNavigateToOnlineDatabases
                ) {
                    Icon(Icons.Filled.Language, contentDescription = null,
                        tint = Color.White, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("RECURSOS", color = Color.White,
                            fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Bases de datos y referencias online",
                            color = Color.White.copy(alpha = 0.92f), fontSize = 14.sp)
                    }
                }
            }

            // ── Estadísticas ───────────────────────────────────────────
            item {
                StatsRow(allPlants.size, mortalCount, altoRiesgoCount, allFamilies.size)
            }

            // ── Grid de navegación ─────────────────────────────────────
            item {
                NavigationGrid(
                    onNavigateToList          = onNavigateToList,
                    onNavigateToCategories    = onNavigateToCategories,
                    onNavigateToPhytochemistry = onNavigateToPhytochemistry,
                    onNavigateToBerries       = onNavigateToBerries,
                    onNavigateToSearch        = { showSearchDialog = true },
                    onNavigateToIdentify      = { showIdentifyDialog = true },
                )
            }
        }
    }

    if (showSearchDialog) {
        SearchTypeDialog(
            onDismiss        = { showSearchDialog = false },
            onSearchByName   = { showSearchDialog = false; onNavigateToSearch() },
            onSearchBySymptoms = { showSearchDialog = false; onNavigateToSearchBySymptoms() }
        )
    }

    if (showIdentifyDialog) {
        IdentifyTypeDialog(
            onDismiss         = { showIdentifyDialog = false },
            onIdentifyByCamera = { showIdentifyDialog = false; onNavigateToCamera() },
            onIdentifyByAR    = { showIdentifyDialog = false; onNavigateToAR() }
        )
    }
}

// ─────────────────────────────────────────────
// Banner genérico con gradiente horizontal
// ─────────────────────────────────────────────
@Composable
fun BannerCard(
    gradient: Brush,
    height: Dp,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(20.dp))
            .background(gradient)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

// ─────────────────────────────────────────────
// Botón cuadrado con gradiente diagonal
// ─────────────────────────────────────────────
@Composable
fun GradientNavButton(
    modifier: Modifier,
    icon: String,
    text: String,
    gradient: Brush,
    height: Dp = 100.dp,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(20.dp))
            .background(gradient)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(icon, fontSize = 30.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                text,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─────────────────────────────────────────────
// Grid de navegación — escala de verdes
// ─────────────────────────────────────────────
@Composable
fun NavigationGrid(
    onNavigateToList: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToPhytochemistry: () -> Unit,
    onNavigateToBerries: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToIdentify: () -> Unit,
) {
    // Gradientes por fila (diagonal izq→der, oscuro→claro)
    val grad1L = Brush.linearGradient(listOf(Color(0xFF1B3A1E), Color(0xFF2E5232))) // verde oscuro bosque
    val grad1R = Brush.linearGradient(listOf(Color(0xFF1E4423), Color(0xFF2A5C30))) // verde oscuro bosque 2
    val grad2L = Brush.linearGradient(listOf(Color(0xFF2D6A30), Color(0xFF3D8841))) // verde musgo medio
    val grad2R = Brush.linearGradient(listOf(Color(0xFF338034), Color(0xFF4A9E4C))) // verde musgo medio 2
    val grad3L = Brush.linearGradient(listOf(Color(0xFF4CAF50), Color(0xFF60C264))) // verde hoja brillante
    val grad3R = Brush.linearGradient(listOf(Color(0xFF57B85B), Color(0xFF72CC76))) // verde hoja claro

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // Fila 1 — verde muy oscuro (bosque)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GradientNavButton(Modifier.weight(1f), "📋", "Todas las Plantas", grad1L, 100.dp, onNavigateToList)
            GradientNavButton(Modifier.weight(1f), "🗂",  "Categorías",        grad1R, 100.dp, onNavigateToCategories)
        }

        // Fila 2 — verde musgo (medio)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GradientNavButton(Modifier.weight(1f), "🔬", "Fitoquímica", grad2L, 100.dp, onNavigateToPhytochemistry)
            GradientNavButton(Modifier.weight(1f), "🫐", "Bayas",       grad2R, 100.dp, onNavigateToBerries)
        }

        // Fila 3 — verde hoja (claro-brillante)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GradientNavButton(Modifier.weight(1f), "🔍", "Buscar",      grad3L, 100.dp, onNavigateToSearch)
            GradientNavButton(Modifier.weight(1f), "📷", "Identificar", grad3R, 100.dp, onNavigateToIdentify)
        }
    }
}

// ─────────────────────────────────────────────
// Fila de estadísticas
// ─────────────────────────────────────────────
@Composable
fun StatsRow(totalPlants: Int, mortalCount: Int, altoRiesgoCount: Int, familiesCount: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        StatCard("🌿", totalPlants.toString(),     "Plantas",    Color(0xFF4CAF50))
        StatCard("☠️", mortalCount.toString(),     "Mortales",   Color(0xFFEF5350))
        StatCard("⚠️", altoRiesgoCount.toString(), "Alto riesgo",Color(0xFFFFA726))
        StatCard("📚", familiesCount.toString(),   "Familias",   Color(0xFF81C784))
    }
}

@Composable
fun StatCard(emoji: String, value: String, label: String, color: Color) {
    Box(
        modifier = Modifier
            .size(width = 80.dp, height = 80.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.13f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 20.sp)
            Text(value, fontWeight = FontWeight.Bold, color = color, fontSize = 17.sp)
            Text(label, fontSize = 10.sp, color = Color.LightGray)
        }
    }
}

// ─────────────────────────────────────────────
// Diálogo: tipo de búsqueda
// ─────────────────────────────────────────────
@Composable
fun SearchTypeDialog(
    onDismiss: () -> Unit,
    onSearchByName: () -> Unit,
    onSearchBySymptoms: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🔍 ¿Cómo quieres buscar?", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DialogOptionCard(
                    gradient = Brush.horizontalGradient(listOf(Color(0xFF1B5E20), Color(0xFF2E7D32))),
                    icon = "🔤",
                    title = "Buscar por nombre",
                    subtitle = "Nombre común o científico",
                    onClick = onSearchByName
                )
                DialogOptionCard(
                    gradient = Brush.horizontalGradient(listOf(Color(0xFF2E7D32), Color(0xFF388E3C))),
                    icon = "🔬",
                    title = "Buscar por síntomas",
                    subtitle = "Síntomas, toxicidad y categoría",
                    onClick = onSearchBySymptoms
                )
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

// ─────────────────────────────────────────────
// Diálogo: tipo de identificación
// ─────────────────────────────────────────────
@Composable
fun IdentifyTypeDialog(
    onDismiss: () -> Unit,
    onIdentifyByCamera: () -> Unit,
    onIdentifyByAR: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("📷 ¿Cómo quieres identificar?", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DialogOptionCard(
                    gradient = Brush.horizontalGradient(listOf(Color(0xFF1B5E20), Color(0xFF388E3C))),
                    icon = "📷",
                    title = "Identificar por foto",
                    subtitle = "Cámara o galería con Pl@ntNet",
                    onClick = onIdentifyByCamera
                )
                DialogOptionCard(
                    gradient = Brush.horizontalGradient(listOf(Color(0xFF33691E), Color(0xFF558B2F))),
                    icon = "🎯",
                    title = "AR Detección",
                    subtitle = "Realidad aumentada en tiempo real",
                    onClick = onIdentifyByAR
                )
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

// ─────────────────────────────────────────────
// Opción dentro de un diálogo (gradiente)
// ─────────────────────────────────────────────
@Composable
fun DialogOptionCard(
    gradient: Brush,
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(gradient)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 28.sp)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title,    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(subtitle, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            }
        }
    }
}