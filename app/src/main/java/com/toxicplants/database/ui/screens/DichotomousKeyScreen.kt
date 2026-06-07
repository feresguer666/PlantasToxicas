package com.toxicplants.database.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.toxicplants.database.KeyOptionEntity
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.ui.viewmodel.DichotomousKeyViewModel
import com.toxicplants.database.ui.viewmodel.PlantViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DichotomousKeyScreen(
    keyId: String,
    keyViewModel: DichotomousKeyViewModel,
    plantViewModel: PlantViewModel,
    onBack: () -> Unit,
    onPlantClick: (PlantEntity) -> Unit
) {
    val state by keyViewModel.state.collectAsState()
    val allPlants by plantViewModel.plantsData.collectAsState()
    val colors = MaterialTheme.colorScheme

    // Pasar plantas al VM cuando estén disponibles
    LaunchedEffect(allPlants) {
        if (allPlants.isNotEmpty()) keyViewModel.setPlants(allPlants)
    }
    // Iniciar la clave (esperando a que haya plantas para que el contador inicial sea correcto)
    LaunchedEffect(keyId, allPlants.size) {
        if (allPlants.isNotEmpty() && (state.key == null || state.key?.id != keyId)) {
            keyViewModel.startKey(keyId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            state.key?.title ?: "Clave dicotómica",
                            fontSize = 16.sp,
                            maxLines = 1
                        )
                        val sub = when {
                            state.resultPlants != null ->
                                "Resultado · ${state.matchCount} planta(s)"
                            state.history.isNotEmpty() ->
                                "Paso ${state.history.size + 1} · ${state.matchCount} candidatas"
                            else ->
                                "${state.matchCount} candidatas"
                        }
                        Text(sub, fontSize = 11.sp, color = colors.onPrimary.copy(alpha = 0.7f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (state.history.isNotEmpty() || state.resultPlants != null) {
                        IconButton(onClick = { keyViewModel.restart() }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Reiniciar")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.primary,
                    titleContentColor = colors.onPrimary,
                    navigationIconContentColor = colors.onPrimary,
                    actionIconContentColor = colors.onPrimary
                )
            )
        }
    ) { padding ->

        when {
            state.loading || (state.key == null && state.errorMessage == null) -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            state.errorMessage != null -> {
                Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(state.errorMessage!!, color = colors.error)
                }
            }

            state.resultPlants != null -> {
                ResultView(
                    padding = padding,
                    plants = state.resultPlants!!,
                    note = state.resultNote,
                    history = state.history.map { it.chosenLabel },
                    onPlantClick = onPlantClick,
                    onRestart = { keyViewModel.restart() },
                    onBack = { keyViewModel.goBack() }
                )
            }

            state.currentNode != null -> {
                QuestionView(
                    padding = padding,
                    question = state.currentNode!!.question,
                    help = state.currentNode!!.help,
                    options = state.currentNode!!.options,
                    history = state.history.map { it.chosenLabel },
                    matchCount = state.matchCount,
                    canGoBack = state.history.isNotEmpty(),
                    onChoose = { idx -> keyViewModel.chooseOption(idx) },
                    onGoBack = { keyViewModel.goBack() }
                )
            }
        }
    }
}

@Composable
private fun QuestionView(
    padding: PaddingValues,
    question: String,
    help: String,
    options: List<KeyOptionEntity>,
    history: List<String>,
    matchCount: Int,
    canGoBack: Boolean,
    onChoose: (Int) -> Unit,
    onGoBack: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var showHelp by remember(question) { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        if (history.isNotEmpty()) {
            Breadcrumb(history = history, matchCount = matchCount)
        }

        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // Pregunta
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = colors.primaryContainer
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                question,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.onPrimaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            if (help.isNotBlank()) {
                                IconButton(onClick = { showHelp = !showHelp }) {
                                    Icon(
                                        Icons.Filled.Help,
                                        contentDescription = "Ayuda",
                                        tint = colors.onPrimaryContainer
                                    )
                                }
                            }
                        }
                        if (showHelp && help.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                help,
                                fontSize = 13.sp,
                                fontStyle = FontStyle.Italic,
                                color = colors.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            items(options.size) { idx ->
                OptionCard(option = options[idx], onClick = { onChoose(idx) })
            }
        }

        if (canGoBack) {
            Surface(modifier = Modifier.fillMaxWidth(), color = colors.surface, shadowElevation = 8.dp) {
                TextButton(
                    onClick = onGoBack,
                    modifier = Modifier.fillMaxWidth().padding(12.dp)
                ) {
                    Icon(Icons.Filled.Undo, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Retroceder al paso anterior")
                }
            }
        }
    }
}

@Composable
private fun OptionCard(option: KeyOptionEntity, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val isLeaf = option.nextNodeId.isNullOrBlank()

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Imagen de apoyo
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(colors.primary.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .border(1.dp, colors.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                val assetUri = remember(option.image) { resolveAssetUri(context, option.image) }
                if (assetUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(assetUri).build(),
                        contentDescription = option.label,
                        modifier = Modifier.fillMaxSize().padding(6.dp)
                    )
                } else {
                    Text("🌱", fontSize = 32.sp, color = colors.primary.copy(alpha = 0.5f))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    option.label,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface
                )
                if (option.description.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        option.description,
                        fontSize = 13.sp,
                        color = colors.onSurface.copy(alpha = 0.7f)
                    )
                }
                if (isLeaf) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "→ ver candidatas",
                        fontSize = 11.sp,
                        color = colors.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultView(
    padding: PaddingValues,
    plants: List<PlantEntity>,
    note: String,
    history: List<String>,
    onPlantClick: (PlantEntity) -> Unit,
    onRestart: () -> Unit,
    onBack: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Column(Modifier.fillMaxSize().padding(padding)) {
        Breadcrumb(history = history, matchCount = plants.size)

        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (plants.isEmpty()) colors.errorContainer else colors.primaryContainer,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            if (plants.isEmpty())
                                "Sin candidatos para esta combinación"
                            else
                                "🌿 ${plants.size} planta(s) candidata(s)",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (plants.isEmpty()) colors.onErrorContainer else colors.onPrimaryContainer
                        )
                        if (note.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = if (plants.isEmpty()) colors.onErrorContainer else colors.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp).padding(top = 2.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    note,
                                    fontSize = 13.sp,
                                    color = if (plants.isEmpty()) colors.onErrorContainer else colors.onPrimaryContainer
                                )
                            }
                        }
                        if (plants.size > 100) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Hay muchos candidatos. Si conoces la familia, prueba con una clave por familia para afinar más.",
                                fontSize = 12.sp,
                                fontStyle = FontStyle.Italic,
                                color = colors.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            items(plants) { plant ->
                ResultPlantCard(plant = plant, onClick = { onPlantClick(plant) })
            }
        }

        Surface(modifier = Modifier.fillMaxWidth(), color = colors.surface, shadowElevation = 8.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Undo, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Atrás")
                }
                Button(onClick = onRestart, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Reiniciar")
                }
            }
        }
    }
}

@Composable
private fun ResultPlantCard(plant: PlantEntity, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val toxColor = when (plant.toxicityLevel.lowercase()) {
        "mortal" -> Color(0xFFB71C1C)
        "muy alto" -> Color(0xFFD32F2F)
        "alto" -> Color(0xFFE53935)
        "moderado" -> Color(0xFFFB8C00)
        "bajo" -> Color(0xFF43A047)
        else -> colors.outline
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(colors.primary.copy(alpha = 0.08f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (plant.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = plant.imageUrl,
                        contentDescription = plant.commonName,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text("🌿", fontSize = 24.sp)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    plant.commonName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface
                )
                Text(
                    plant.scientificName,
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic,
                    color = colors.onSurface.copy(alpha = 0.7f)
                )
                if (plant.family.isNotBlank()) {
                    Text(
                        plant.family,
                        fontSize = 11.sp,
                        color = colors.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            if (plant.toxicityLevel.isNotBlank()) {
                Surface(color = toxColor, shape = RoundedCornerShape(8.dp)) {
                    Text(
                        plant.toxicityLevel,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun Breadcrumb(history: List<String>, matchCount: Int) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier = Modifier.fillMaxWidth(), color = colors.surfaceVariant.copy(alpha = 0.5f)) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Tus elecciones",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurfaceVariant
                )
                Surface(
                    color = colors.primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "$matchCount candidatas",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            if (history.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    history.joinToString(" › "),
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Devuelve 'file:///android_asset/...' si la imagen existe en assets, o null.
 */
private fun resolveAssetUri(context: Context, path: String?): String? {
    if (path.isNullOrBlank()) return null
    return try {
        context.assets.open(path).use { /* solo comprobamos que existe */ }
        "file:///android_asset/$path"
    } catch (_: Exception) {
        null
    }
}
