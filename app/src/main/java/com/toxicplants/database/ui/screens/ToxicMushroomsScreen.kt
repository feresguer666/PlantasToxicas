package com.toxicplants.database.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.toxicplants.database.MushroomEntity
import com.toxicplants.database.ui.WikiImageFetcher
import com.toxicplants.database.ui.viewmodel.MushroomViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToxicMushroomsScreen(
    viewModel: MushroomViewModel,
    onBack: () -> Unit,
) {
    val allMushrooms by viewModel.allMushrooms.observeAsState(emptyList())
    val syndromes by viewModel.allSyndromes.observeAsState(emptyList())
    val loadError by viewModel.loadError.observeAsState(null)

    var query by remember { mutableStateOf("") }
    var selectedToxicity by remember { mutableStateOf("Todas") }
    var selectedSyndrome by remember { mutableStateOf("Todos") }
    var selectedMushroom by remember { mutableStateOf<MushroomEntity?>(null) }
    var editorTarget by remember { mutableStateOf<MushroomEntity?>(null) }
    var showNewEditor by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<MushroomEntity?>(null) }

    val toxicityLevels = listOf("Todas", "Mortal", "Muy alta", "Alta", "Moderada", "Baja")

    val filtered = remember(allMushrooms, query, selectedToxicity, selectedSyndrome) {
        allMushrooms.filter { mushroom ->
            val q = query.trim()
            val matchesQuery = q.isBlank() ||
                    mushroom.commonName.contains(q, ignoreCase = true) ||
                    mushroom.scientificName.contains(q, ignoreCase = true) ||
                    mushroom.family.contains(q, ignoreCase = true) ||
                    mushroom.syndrome.contains(q, ignoreCase = true) ||
                    mushroom.toxicCompounds.contains(q, ignoreCase = true) ||
                    mushroom.edibleConfusions.contains(q, ignoreCase = true)
            val matchesToxicity = selectedToxicity == "Todas" || mushroom.toxicityLevel == selectedToxicity
            val matchesSyndrome = selectedSyndrome == "Todos" || mushroom.syndrome == selectedSyndrome
            matchesQuery && matchesToxicity && matchesSyndrome
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("🍄 Setas tóxicas", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(
                            "${allMushrooms.size} especies · ${syndromes.size} síndromes",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.82f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showNewEditor = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Añadir seta", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF5D4037),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                placeholder = { Text("Buscar seta, toxina, síndrome o confusión…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Limpiar")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp)
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(toxicityLevels) { level ->
                    FilterChip(
                        selected = selectedToxicity == level,
                        onClick = { selectedToxicity = level },
                        label = { Text(level, fontSize = 12.sp) },
                        leadingIcon = if (level == "Mortal") ({ Text("☠️") }) else null
                    )
                }
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedSyndrome == "Todos",
                        onClick = { selectedSyndrome = "Todos" },
                        label = { Text("Todos los síndromes", fontSize = 12.sp) }
                    )
                }
                items(syndromes) { syndrome ->
                    FilterChip(
                        selected = selectedSyndrome == syndrome,
                        onClick = { selectedSyndrome = syndrome },
                        label = { Text(syndrome, fontSize = 12.sp, maxLines = 1) }
                    )
                }
            }

            if (loadError != null) {
                LoadMushroomsErrorMessage(message = loadError ?: "Error desconocido")
            } else if (allMushrooms.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF5D4037))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        MushroomSummaryCard(
                            total = filtered.size,
                            deadly = filtered.count { it.isDeadly },
                            syndromes = filtered.map { it.syndrome }.distinct().size
                        )
                    }

                    if (filtered.isEmpty()) {
                        item { EmptyMushroomsMessage() }
                    } else {
                        items(filtered, key = { it.id }) { mushroom ->
                            MushroomCard(
                                mushroom = mushroom,
                                onClick = { selectedMushroom = mushroom },
                                onEditClick = { editorTarget = mushroom },
                                onDeleteClick = { deleteTarget = mushroom },
                                onFavoriteClick = { viewModel.toggleFavorite(mushroom) }
                            )
                        }
                    }
                }
            }
        }
    }

    selectedMushroom?.let { mushroom ->
        MushroomDetailDialog(
            mushroom = mushroom,
            onDismiss = { selectedMushroom = null },
            onEditClick = {
                selectedMushroom = null
                editorTarget = mushroom
            },
            onFavoriteClick = { viewModel.toggleFavorite(mushroom) }
        )
    }

    if (showNewEditor) {
        MushroomEditorDialog(
            mushroom = null,
            onDismiss = { showNewEditor = false },
            onSave = { newItem ->
                viewModel.addMushroom(newItem)
                showNewEditor = false
            }
        )
    }

    editorTarget?.let { target ->
        MushroomEditorDialog(
            mushroom = target,
            onDismiss = { editorTarget = null },
            onSave = { edited ->
                viewModel.updateMushroom(edited)
                editorTarget = null
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Eliminar seta") },
            text = { Text("¿Eliminar ${target.commonName} (${target.scientificName}) de tu catálogo local?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteMushroom(target)
                    deleteTarget = null
                }) { Text("Eliminar", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun MushroomSummaryCard(total: Int, deadly: Int, syndromes: Int) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SummaryItem("🍄", total.toString(), "setas")
            SummaryItem("☠️", deadly.toString(), "mortales")
            SummaryItem("🧬", syndromes.toString(), "síndromes")
        }
    }
}

@Composable
private fun SummaryItem(icon: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 22.sp)
        Text(value, fontWeight = FontWeight.Bold, color = Color(0xFF5D4037), fontSize = 18.sp)
        Text(label, color = Color.Gray, fontSize = 11.sp)
    }
}

@Composable
private fun MushroomCard(
    mushroom: MushroomEntity,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onFavoriteClick: () -> Unit,
) {
    val color = toxicityColor(mushroom.toxicityLevel, mushroom.isDeadly)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MushroomThumbnail(mushroom = mushroom, color = color)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    mushroom.commonName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    mushroom.scientificName,
                    fontStyle = FontStyle.Italic,
                    color = Color.Gray,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(5.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SmallChip(mushroom.toxicityLevel, color)
                    SmallChip(mushroom.syndrome, Color(0xFF5D4037))
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Filled.Edit, contentDescription = "Editar", tint = Color.Gray)
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = Color.Red.copy(alpha = 0.75f))
                }
                IconButton(onClick = onFavoriteClick) {
                    Icon(
                        imageVector = if (mushroom.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favorito",
                        tint = if (mushroom.isFavorite) Color(0xFFD32F2F) else Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun MushroomThumbnail(mushroom: MushroomEntity, color: Color) {
    var resolvedUrl by remember(mushroom.id, mushroom.imageUrl) { mutableStateOf(mushroom.imageUrl) }

    LaunchedEffect(mushroom.id, mushroom.scientificName, mushroom.imageUrl) {
        if (resolvedUrl.isBlank()) {
            resolvedUrl = WikiImageFetcher.getImageUrl(mushroom.scientificName)
                .ifBlank { WikiImageFetcher.getImageUrl(mushroom.commonName) }
        }
    }

    Box(
        modifier = Modifier
            .size(58.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        if (resolvedUrl.isBlank()) {
            Text(if (mushroom.isDeadly) "☠️" else "🍄", fontSize = 26.sp)
        } else {
            AsyncImage(
                model = resolvedUrl,
                contentDescription = mushroom.commonName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onError = { resolvedUrl = "" }
            )
        }
    }
}

@Composable
private fun MushroomLargeImage(mushroom: MushroomEntity, color: Color) {
    var resolvedUrl by remember(mushroom.id, mushroom.imageUrl) { mutableStateOf(mushroom.imageUrl) }

    LaunchedEffect(mushroom.id, mushroom.scientificName, mushroom.imageUrl) {
        if (resolvedUrl.isBlank()) {
            resolvedUrl = WikiImageFetcher.getImageUrl(mushroom.scientificName)
                .ifBlank { WikiImageFetcher.getImageUrl(mushroom.commonName) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        if (resolvedUrl.isBlank()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (mushroom.isDeadly) "☠️" else "🍄", fontSize = 42.sp)
                Text("Foto no disponible", color = Color.Gray, fontSize = 12.sp)
            }
        } else {
            AsyncImage(
                model = resolvedUrl,
                contentDescription = mushroom.commonName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onError = { resolvedUrl = "" }
            )
        }
    }
}

@Composable
private fun MushroomDetailDialog(
    mushroom: MushroomEntity,
    onDismiss: () -> Unit,
    onEditClick: () -> Unit,
    onFavoriteClick: () -> Unit,
) {
    val color = toxicityColor(mushroom.toxicityLevel, mushroom.isDeadly)
    val context = LocalContext.current
    val wikiUrl = remember(mushroom.scientificName) { mushroom.wikiUrl() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (mushroom.isDeadly) "☠️" else "🍄", fontSize = 26.sp)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(mushroom.commonName, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                        Text(mushroom.scientificName, fontStyle = FontStyle.Italic, color = Color.Gray, fontSize = 13.sp)
                    }
                    IconButton(onClick = onFavoriteClick) {
                        Icon(
                            imageVector = if (mushroom.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorito",
                            tint = if (mushroom.isFavorite) Color(0xFFD32F2F) else Color.Gray
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, label = { Text(mushroom.toxicityLevel) }, leadingIcon = { Text(if (mushroom.isDeadly) "☠️" else "⚠️") })
                    AssistChip(onClick = {}, label = { Text(mushroom.syndrome) })
                }

                MushroomLargeImage(mushroom = mushroom, color = color)

                TextButton(
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(wikiUrl)))
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF1565C0))
                ) {
                    Text("🌐 Ver en Wikipedia")
                }

                if (mushroom.description.isNotBlank()) DetailBlock("Descripción", mushroom.description, color)
                DetailBlock("Toxinas", mushroom.toxicCompounds, Color(0xFF6A1B9A))
                DetailBlock("Latencia", mushroom.onsetTime, Color(0xFF1976D2))
                DetailBlock("Síntomas", mushroom.symptoms, Color(0xFFB71C1C))
                DetailBlock("Hábitat", mushroom.habitat, Color(0xFF2E7D32))
                DetailBlock("Temporada", mushroom.season, Color(0xFF795548))
                DetailBlock("Distribución", mushroom.geographicDistribution, Color(0xFF455A64))
                DetailBlock("Confusiones comestibles", mushroom.edibleConfusions, Color(0xFFE65100))
                DetailBlock("Primeros auxilios", mushroom.firstAid, Color(0xFFB71C1C))
                DetailBlock("Tratamiento", mushroom.treatment, Color(0xFF2E7D32))
                if (mushroom.notes.isNotBlank()) DetailBlock("Notas", mushroom.notes, Color.DarkGray)

                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFB71C1C), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "No consumas setas identificadas solo por una app. Ante sospecha de intoxicación, llama a emergencias o al Instituto Nacional de Toxicología: 91 562 04 20.",
                            fontSize = 12.sp,
                            color = Color(0xFFB71C1C),
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        },
        dismissButton = {
            TextButton(onClick = onEditClick) { Text("Editar") }
        }
    )
}

@Composable
private fun MushroomEditorDialog(
    mushroom: MushroomEntity?,
    onDismiss: () -> Unit,
    onSave: (MushroomEntity) -> Unit,
) {
    var commonName by remember(mushroom?.id) { mutableStateOf(mushroom?.commonName ?: "") }
    var scientificName by remember(mushroom?.id) { mutableStateOf(mushroom?.scientificName ?: "") }
    var family by remember(mushroom?.id) { mutableStateOf(mushroom?.family ?: "") }
    var toxicityLevel by remember(mushroom?.id) { mutableStateOf(mushroom?.toxicityLevel ?: "Alta") }
    var syndrome by remember(mushroom?.id) { mutableStateOf(mushroom?.syndrome ?: "Gastritis severa") }
    var toxicCompounds by remember(mushroom?.id) { mutableStateOf(mushroom?.toxicCompounds ?: "") }
    var onsetTime by remember(mushroom?.id) { mutableStateOf(mushroom?.onsetTime ?: "") }
    var symptoms by remember(mushroom?.id) { mutableStateOf(mushroom?.symptoms ?: "") }
    var description by remember(mushroom?.id) { mutableStateOf(mushroom?.description ?: "") }
    var habitat by remember(mushroom?.id) { mutableStateOf(mushroom?.habitat ?: "") }
    var season by remember(mushroom?.id) { mutableStateOf(mushroom?.season ?: "") }
    var distribution by remember(mushroom?.id) { mutableStateOf(mushroom?.geographicDistribution ?: "") }
    var confusions by remember(mushroom?.id) { mutableStateOf(mushroom?.edibleConfusions ?: "") }
    var firstAid by remember(mushroom?.id) { mutableStateOf(mushroom?.firstAid ?: "") }
    var treatment by remember(mushroom?.id) { mutableStateOf(mushroom?.treatment ?: "") }
    var notes by remember(mushroom?.id) { mutableStateOf(mushroom?.notes ?: "") }
    var imageUrl by remember(mushroom?.id) { mutableStateOf(mushroom?.imageUrl ?: "") }
    var isDeadly by remember(mushroom?.id) { mutableStateOf(mushroom?.isDeadly ?: false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val toxicityOptions = listOf("Mortal", "Muy alta", "Alta", "Moderada", "Baja")
    val syndromeOptions = listOf(
        "Amatoxinas", "Orellanina", "Gastritis severa", "Muscarínico",
        "Iboténico-muscimol", "Giromitrínico", "Coprínico", "Rabdimiolítico",
        "Paxillus", "Nefrotóxico aminohexadienoico", "Acromelalgia",
        "Tricotecenos", "Neurotóxico", "Alucinógeno", "Ergotismo"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (mushroom == null) "Añadir seta tóxica" else "Editar seta", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                validationError?.let {
                    Text(it, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                EditorTextField("Nombre común", commonName) { commonName = it }
                EditorTextField("Nombre científico", scientificName) { scientificName = it }
                EditorTextField("Familia", family) { family = it }

                Text("Toxicidad", fontWeight = FontWeight.Bold, color = Color(0xFF5D4037), fontSize = 13.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(toxicityOptions) { option ->
                        FilterChip(
                            selected = toxicityLevel == option,
                            onClick = {
                                toxicityLevel = option
                                if (option == "Mortal") isDeadly = true
                            },
                            label = { Text(option, fontSize = 11.sp) }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isDeadly, onCheckedChange = { isDeadly = it })
                    Text("Marcada como mortal", fontSize = 13.sp)
                }

                Text("Síndrome", fontWeight = FontWeight.Bold, color = Color(0xFF5D4037), fontSize = 13.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(syndromeOptions) { option ->
                        FilterChip(
                            selected = syndrome == option,
                            onClick = { syndrome = option },
                            label = { Text(option, fontSize = 11.sp, maxLines = 1) }
                        )
                    }
                }
                EditorTextField("Síndrome personalizado", syndrome) { syndrome = it }

                EditorTextField("Toxinas / compuestos", toxicCompounds) { toxicCompounds = it }
                EditorTextField("Tiempo de latencia", onsetTime) { onsetTime = it }
                EditorTextField("Síntomas", symptoms, minLines = 3) { symptoms = it }
                EditorTextField("Descripción", description, minLines = 3) { description = it }
                EditorTextField("Hábitat", habitat) { habitat = it }
                EditorTextField("Temporada", season) { season = it }
                EditorTextField("Distribución", distribution) { distribution = it }
                EditorTextField("Confusiones comestibles", confusions, minLines = 2) { confusions = it }
                EditorTextField("Primeros auxilios", firstAid, minLines = 3) { firstAid = it }
                EditorTextField("Tratamiento", treatment, minLines = 3) { treatment = it }
                EditorTextField("Notas", notes, minLines = 2) { notes = it }
                EditorTextField("URL de imagen", imageUrl) { imageUrl = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (commonName.isBlank() || scientificName.isBlank()) {
                    validationError = "Nombre común y nombre científico son obligatorios."
                    return@TextButton
                }
                onSave(
                    MushroomEntity(
                        id = mushroom?.id ?: 0,
                        commonName = commonName.trim(),
                        scientificName = scientificName.trim(),
                        family = family.trim(),
                        toxicityLevel = toxicityLevel.trim().ifBlank { "Alta" },
                        syndrome = syndrome.trim().ifBlank { "Gastritis severa" },
                        toxicCompounds = toxicCompounds.trim(),
                        onsetTime = onsetTime.trim(),
                        symptoms = symptoms.trim(),
                        description = description.trim(),
                        habitat = habitat.trim(),
                        season = season.trim(),
                        geographicDistribution = distribution.trim(),
                        edibleConfusions = confusions.trim(),
                        firstAid = firstAid.trim(),
                        treatment = treatment.trim(),
                        notes = notes.trim(),
                        imageUrl = imageUrl.trim(),
                        isDeadly = isDeadly || toxicityLevel == "Mortal",
                        isFavorite = mushroom?.isFavorite ?: false,
                    )
                )
            }) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun EditorTextField(
    label: String,
    value: String,
    minLines: Int = 1,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        minLines = minLines,
        maxLines = if (minLines == 1) 1 else 6
    )
}

@Composable
private fun DetailBlock(title: String, value: String, color: Color) {
    if (value.isBlank()) return
    Column {
        Text(title, fontWeight = FontWeight.Bold, color = color, fontSize = 13.sp)
        Text(value, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun SmallChip(text: String, color: Color) {
    Surface(color = color.copy(alpha = 0.14f), shape = RoundedCornerShape(8.dp)) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LoadMushroomsErrorMessage(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("⚠️", fontSize = 34.sp)
                Text("No se pudo cargar la base de setas", fontWeight = FontWeight.Bold, color = Color(0xFFB71C1C))
                Text(message, color = Color(0xFFB71C1C), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun EmptyMushroomsMessage() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🔎", fontSize = 34.sp)
            Text("No hay setas con esos filtros", fontWeight = FontWeight.Bold)
            Text("Prueba otra búsqueda o cambia el síndrome.", color = Color.Gray, fontSize = 13.sp)
        }
    }
}

private fun MushroomEntity.wikiUrl(): String {
    val title = scientificName.ifBlank { commonName }.trim().replace(" ", "_")
    return "https://en.wikipedia.org/wiki/${Uri.encode(title)}"
}

private fun toxicityColor(level: String, deadly: Boolean): Color = when {
    deadly || level == "Mortal" -> Color(0xFFB71C1C)
    level == "Muy alta" -> Color(0xFFD84315)
    level == "Alta" -> Color(0xFFE65100)
    level == "Moderada" -> Color(0xFFF9A825)
    level == "Baja" -> Color(0xFF388E3C)
    else -> Color.Gray
}
