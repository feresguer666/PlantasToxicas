package com.toxicplants.database.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.android.gms.location.LocationServices
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.SightingEntity
import com.toxicplants.database.SightingStore
import com.toxicplants.database.ui.viewmodel.PlantViewModel
import com.toxicplants.database.ui.viewmodel.SightingViewModel
import java.io.File
import java.text.Normalizer
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SightingsHistoryScreen(
    viewModel: SightingViewModel,
    plantViewModel: PlantViewModel,
    onPlantClick: (PlantEntity) -> Unit,
    onEditPlantLocation: (Int) -> Unit = {},
    onBack: () -> Unit
) {
    LocalContext.current
    val sightings by viewModel.sightings.observeAsState(emptyList())
    val allPlants by plantViewModel.allPlants.observeAsState(emptyList())

    var selectedTab by remember { mutableIntStateOf(0) }
    var focusedSightingId by remember { mutableStateOf<Int?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<SightingEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<SightingEntity?>(null) }
    var fullScreenPhoto by remember { mutableStateOf<SightingEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("📍 Historial de avistamientos", fontWeight = FontWeight.Bold)
                        Text("${sightings.size} registros", fontSize = 12.sp, color = Color.White.copy(alpha = 0.82f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1565C0),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showEditor = true },
                containerColor = Color(0xFF2E7D32),
                contentColor = Color.White
            ) { Icon(Icons.Filled.Add, contentDescription = "Añadir") }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Historial") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Mapa") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Plantas") })
            }
            if (selectedTab == 0) {
                SightingsList(
                    sightings = sightings,
                    allPlants = allPlants,
                    onPlantClick = onPlantClick,
                    onPhotoClick = { fullScreenPhoto = it },
                    onShowOnMap = { sighting ->
                        focusedSightingId = sighting.id
                        selectedTab = 1
                    },
                    onEdit = { editTarget = it },
                    onDelete = { deleteTarget = it }
                )
            } else if (selectedTab == 1) {
                SightingsMap(
                    sightings = sightings,
                    focusedSightingId = focusedSightingId
                )
            } else {
                PlantsWithLocationInSightingsTab(
                    plants = allPlants,
                    onPlantClick = onPlantClick,
                    onEditLocation = onEditPlantLocation
                )
            }
        }
    }

    fullScreenPhoto?.let { target ->
        FullScreenPhotoDialog(
            sighting = target,
            onDismiss = { fullScreenPhoto = null }
        )
    }

    if (showEditor) {
        SightingEditorDialog(
            sighting = null,
            onDismiss = { showEditor = false },
            onSave = {
                viewModel.addSighting(it.withCatalogLink(allPlants))
                showEditor = false
            }
        )
    }

    editTarget?.let { target ->
        SightingEditorDialog(
            sighting = target,
            onDismiss = { editTarget = null },
            onSave = {
                viewModel.updateSighting(it.withCatalogLink(allPlants))
                editTarget = null
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Eliminar avistamiento") },
            text = { Text("¿Eliminar el avistamiento de ${target.commonName}?") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteSighting(target); deleteTarget = null }) {
                    Text("Eliminar", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancelar") }
            }
        )
    }
}


@Composable
private fun PlantsWithLocationInSightingsTab(
    plants: List<PlantEntity>,
    onPlantClick: (PlantEntity) -> Unit,
    onEditLocation: (Int) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val locatedPlants = remember(plants, query) {
        plants
            .filter { it.latitude != null && it.longitude != null }
            .filter { plant ->
                query.isBlank() ||
                    plant.commonName.contains(query, ignoreCase = true) ||
                    plant.scientificName.contains(query, ignoreCase = true) ||
                    plant.locationName.orEmpty().contains(query, ignoreCase = true) ||
                    plant.notes.orEmpty().contains(query, ignoreCase = true)
            }
            .sortedBy { it.commonName.lowercase() }
    }

    Column(Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                singleLine = true,
                label = { Text("Buscar por planta, lugar o nota") },
                leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null) }
            )
        }

        if (plants.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF1565C0))
            }
        } else if (locatedPlants.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📍", fontSize = 54.sp)
                    Text(
                        if (query.isBlank()) "No hay plantas con ubicación" else "Sin resultados",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (query.isBlank()) "Añade coordenadas desde una ficha de planta" else "Prueba otro texto",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        "📍 ${locatedPlants.size} plantas localizadas",
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1565C0)
                    )
                }
                items(locatedPlants, key = { it.id }, contentType = { "located_plant" }) { plant ->
                    LocatedPlantInSightingsCard(
                        plant = plant,
                        onOpenPlant = { onPlantClick(plant) },
                        onEditLocation = { onEditLocation(plant.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LocatedPlantInSightingsCard(
    plant: PlantEntity,
    onOpenPlant: () -> Unit,
    onEditLocation: () -> Unit
) {
    val context = LocalContext.current
    val hasLocation = plant.latitude != null && plant.longitude != null

    Card(elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE3F2FD)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Color(0xFF1565C0), modifier = Modifier.size(34.dp))
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = plant.commonName.ifBlank { "Sin nombre" },
                    modifier = Modifier.clickable { onOpenPlant() },
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (plant.scientificName.isNotBlank()) {
                    Text(
                        plant.scientificName,
                        modifier = Modifier.clickable { onOpenPlant() },
                        fontStyle = FontStyle.Italic,
                        color = Color.Gray,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text("Abrir ficha ↗", modifier = Modifier.clickable { onOpenPlant() }, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                if (!plant.locationName.isNullOrBlank()) {
                    Text("📍 ${plant.locationName}", fontSize = 12.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (hasLocation) {
                    Text(
                        "${"%.5f".format(plant.latitude)}, ${"%.5f".format(plant.longitude)}",
                        fontSize = 11.sp,
                        color = Color(0xFF1565C0)
                    )
                }
                if (!plant.notes.isNullOrBlank()) {
                    Text("📝 ${plant.notes}", fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (hasLocation) {
                    IconButton(onClick = { openPlantInExternalMap(context, plant) }) {
                        Icon(Icons.Filled.LocationOn, contentDescription = "Abrir en mapa", tint = Color(0xFF1565C0))
                    }
                }
                IconButton(onClick = onEditLocation) {
                    Icon(Icons.Filled.Edit, contentDescription = "Editar ubicación", tint = Color.Gray)
                }
            }
        }
    }
}

private fun openPlantInExternalMap(context: Context, plant: PlantEntity) {
    val lat = plant.latitude ?: return
    val lng = plant.longitude ?: return
    val label = Uri.encode(plant.commonName.ifBlank { "Planta" })
    val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($label)")
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
}


@Composable
private fun SightingsList(
    sightings: List<SightingEntity>,
    allPlants: List<PlantEntity>,
    onPlantClick: (PlantEntity) -> Unit,
    onPhotoClick: (SightingEntity) -> Unit,
    onShowOnMap: (SightingEntity) -> Unit,
    onEdit: (SightingEntity) -> Unit,
    onDelete: (SightingEntity) -> Unit
) {
    if (sightings.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📍", fontSize = 54.sp)
                Text("Sin avistamientos", fontWeight = FontWeight.Bold)
                Text("Pulsa + para añadir uno con foto, ubicación y notas", color = Color.Gray, fontSize = 13.sp)
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(sightings, key = { it.id }, contentType = { "sighting" }) { s ->
            SightingCard(
                sighting = s,
                onPlantNameClick = {
                    findLinkedPlant(s, allPlants)?.let(onPlantClick)
                },
                onPhotoClick = { onPhotoClick(s) },
                onShowOnMap = { onShowOnMap(s) },
                onEdit = { onEdit(s) },
                onDelete = { onDelete(s) }
            )
        }
    }
}

@Composable
private fun SightingCard(
    sighting: SightingEntity,
    onPlantNameClick: () -> Unit,
    onPhotoClick: () -> Unit,
    onShowOnMap: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val photoFile = remember(sighting.photoPath) { File(sighting.photoPath) }
    val hasPhoto = sighting.photoPath.isNotBlank() && photoFile.exists()
    val hasLocation = sighting.latitude != null && sighting.longitude != null

    Card(elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE3F2FD))
                    .clickable(enabled = hasPhoto) { onPhotoClick() },
                contentAlignment = Alignment.Center
            ) {
                if (hasPhoto) {
                    // Miniatura: evita decodificar fotos enormes en la lista.
                    val thumbRequest = remember(photoFile.absolutePath) {
                        ImageRequest.Builder(context)
                            .data(photoFile)
                            .size(160)
                            .crossfade(false)
                            .build()
                    }
                    AsyncImage(
                        model = thumbRequest,
                        contentDescription = sighting.commonName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Surface(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
                        color = Color.Black.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("🔍", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), fontSize = 10.sp)
                    }
                } else {
                    Text(typeEmoji(sighting.type), fontSize = 30.sp)
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = sighting.commonName.ifBlank { "Sin nombre" },
                    modifier = Modifier.clickable { onPlantNameClick() },
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (sighting.scientificName.isNotBlank()) {
                    Text(
                        sighting.scientificName,
                        modifier = Modifier.clickable { onPlantNameClick() },
                        fontStyle = FontStyle.Italic,
                        color = Color.Gray,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    "Abrir ficha ↗",
                    modifier = Modifier.clickable { onPlantNameClick() },
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text("${typeEmoji(sighting.type)} ${sighting.type} · ${sighting.date}", fontSize = 12.sp, color = Color(0xFF1565C0))
                if (sighting.locationName.isNotBlank()) {
                    Text(
                        "📍 ${sighting.locationName}",
                        modifier = Modifier.clickable(enabled = hasLocation) { onShowOnMap() },
                        fontSize = 12.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (hasLocation) {
                    Text(
                        "${"%.5f".format(sighting.latitude)}, ${"%.5f".format(sighting.longitude)}",
                        modifier = Modifier.clickable { onShowOnMap() },
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
                if (sighting.notes.isNotBlank()) {
                    Text("📝 ${sighting.notes}", fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (hasLocation) {
                    IconButton(onClick = onShowOnMap) {
                        Icon(Icons.Filled.LocationOn, contentDescription = "Ver en mapa", tint = Color(0xFF1565C0))
                    }
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Editar", tint = Color.Gray)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = Color.Red)
                }
            }
        }
    }
}

@Composable
private fun FullScreenPhotoDialog(
    sighting: SightingEntity,
    onDismiss: () -> Unit
) {
    val photoFile = remember(sighting.photoPath) { File(sighting.photoPath) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AsyncImage(
                model = photoFile,
                contentDescription = sighting.commonName,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .clickable { onDismiss() },
                contentScale = ContentScale.Fit
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(50))
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = Color.White)
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                color = Color.Black.copy(alpha = 0.62f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(sighting.commonName.ifBlank { "Avistamiento" }, color = Color.White, fontWeight = FontWeight.Bold)
                    if (sighting.scientificName.isNotBlank()) {
                        Text(sighting.scientificName, color = Color.White.copy(alpha = 0.78f), fontStyle = FontStyle.Italic, fontSize = 12.sp)
                    }
                    Text("Toca la imagen o la X para cerrar", color = Color.White.copy(alpha = 0.70f), fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun SightingsMap(
    sightings: List<SightingEntity>,
    focusedSightingId: Int?
) {
    val context = LocalContext.current
    val located = remember(sightings) { sightings.filter { it.latitude != null && it.longitude != null } }
    val ordered = remember(located, focusedSightingId) {
        if (focusedSightingId == null) located
        else located.sortedBy { if (it.id == focusedSightingId) 0 else 1 }
    }

    if (located.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay avistamientos con coordenadas", color = Color.Gray)
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Text("🗺️ Mapa ligero", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text(
                        "Para evitar cierres y carga lenta, se abre la ubicación en la app de mapas del móvil.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                    )
                }
            }
        }

        items(ordered, key = { it.id }, contentType = { "map_sighting" }) { s ->
            val focused = s.id == focusedSightingId
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { openSightingInExternalMap(context, s) },
                elevation = CardDefaults.cardElevation(defaultElevation = if (focused) 6.dp else 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (focused) Color(0xFFE3F2FD) else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📍", fontSize = 28.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.commonName.ifBlank { "Avistamiento" }, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (s.scientificName.isNotBlank()) {
                            Text(s.scientificName, fontStyle = FontStyle.Italic, fontSize = 12.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (s.locationName.isNotBlank()) {
                            Text(s.locationName, fontSize = 12.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(
                            "${"%.5f".format(s.latitude)}, ${"%.5f".format(s.longitude)}",
                            fontSize = 11.sp,
                            color = Color(0xFF1565C0)
                        )
                    }
                    Text("Abrir ›", color = Color(0xFF1565C0), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

private fun openSightingInExternalMap(context: Context, sighting: SightingEntity) {
    val lat = sighting.latitude ?: return
    val lng = sighting.longitude ?: return
    val label = Uri.encode(sighting.commonName.ifBlank { "Avistamiento" })
    val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($label)")
    val intent = Intent(Intent.ACTION_VIEW, uri)
    runCatching { context.startActivity(intent) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SightingEditorDialog(
    sighting: SightingEntity?,
    onDismiss: () -> Unit,
    onSave: (SightingEntity) -> Unit
) {
    val context = LocalContext.current
    var type by remember(sighting?.id) { mutableStateOf(sighting?.type ?: "Planta") }
    var commonName by remember(sighting?.id) { mutableStateOf(sighting?.commonName ?: "") }
    var scientificName by remember(sighting?.id) { mutableStateOf(sighting?.scientificName ?: "") }
    var toxicity by remember(sighting?.id) { mutableStateOf(sighting?.toxicityLevel ?: "") }
    var latText by remember(sighting?.id) { mutableStateOf(sighting?.latitude?.toString() ?: "") }
    var lngText by remember(sighting?.id) { mutableStateOf(sighting?.longitude?.toString() ?: "") }
    var locationName by remember(sighting?.id) { mutableStateOf(sighting?.locationName ?: "") }
    var notes by remember(sighting?.id) { mutableStateOf(sighting?.notes ?: "") }
    var photoPath by remember(sighting?.id) { mutableStateOf(sighting?.photoPath ?: "") }
    var status by remember { mutableStateOf("") }

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            runCatching { photoPath = SightingStore.copyPhotoToInternal(context, it) }
                .onFailure { status = "No se pudo copiar la foto" }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val ok = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (ok) fillLastLocation(context, onResult = { lat, lng ->
            if (lat != null && lng != null) {
                latText = lat.toString(); lngText = lng.toString(); status = "GPS aplicado"
            } else status = "No se pudo obtener GPS"
        }) else status = "Permiso de ubicación denegado"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (sighting == null) "Nuevo avistamiento" else "Editar avistamiento", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("Planta", "Seta", "Liquen", "Otro")) { option ->
                        FilterChip(selected = type == option, onClick = { type = option }, label = { Text("${typeEmoji(option)} $option") })
                    }
                }
                SightingEditorTextField("Nombre común", commonName) { commonName = it }
                SightingEditorTextField("Nombre científico", scientificName) { scientificName = it }
                SightingEditorTextField("Toxicidad / riesgo", toxicity) { toxicity = it }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = latText, onValueChange = { latText = it }, label = { Text("Latitud") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = lngText, onValueChange = { lngText = it }, label = { Text("Longitud") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                TextButton(onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        fillLastLocation(context) { lat, lng ->
                            if (lat != null && lng != null) { latText = lat.toString(); lngText = lng.toString(); status = "GPS aplicado" } else status = "No se pudo obtener GPS"
                        }
                    } else {
                        permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    }
                }) { Text("📍 Usar última ubicación GPS") }
                SightingEditorTextField("Nombre del lugar", locationName) { locationName = it }
                SightingEditorTextField("Notas", notes, minLines = 3) { notes = it }
                TextButton(onClick = { photoLauncher.launch("image/*") }) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Elegir foto")
                }
                if (photoPath.isNotBlank()) Text("Foto guardada: ${File(photoPath).name}", fontSize = 11.sp, color = Color.Gray)
                if (status.isNotBlank()) Text(status, fontSize = 12.sp, color = Color(0xFF1565C0))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    SightingEntity(
                        id = sighting?.id ?: 0,
                        type = type,
                        itemId = sighting?.itemId,
                        commonName = commonName.trim(),
                        scientificName = scientificName.trim(),
                        toxicityLevel = toxicity.trim(),
                        latitude = latText.replace(',', '.').toDoubleOrNull(),
                        longitude = lngText.replace(',', '.').toDoubleOrNull(),
                        locationName = locationName.trim(),
                        notes = notes.trim(),
                        photoPath = photoPath,
                        date = sighting?.date ?: SightingStore.nowString()
                    )
                )
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Suppress("MissingPermission")
private fun fillLastLocation(context: Context, onResult: (Double?, Double?) -> Unit) {
    LocationServices.getFusedLocationProviderClient(context).lastLocation
        .addOnSuccessListener { onResult(it?.latitude, it?.longitude) }
        .addOnFailureListener { onResult(null, null) }
}

private fun SightingEntity.withCatalogLink(plants: List<PlantEntity>): SightingEntity {
    if (!type.equals("Planta", ignoreCase = true)) return copy(itemId = null)
    val textMatch = findLinkedPlant(copy(itemId = null), plants)
    val currentMatch = findLinkedPlant(this, plants)
    return copy(itemId = textMatch?.id ?: currentMatch?.id ?: itemId)
}

private fun findLinkedPlant(sighting: SightingEntity, plants: List<PlantEntity>): PlantEntity? {
    sighting.itemId?.let { id ->
        plants.firstOrNull { it.id == id }?.let { return it }
    }

    val sightingScientific = sighting.scientificName.canonicalScientificName()
    if (sightingScientific.isNotBlank()) {
        plants.firstOrNull { it.scientificName.canonicalScientificName() == sightingScientific }?.let { return it }
    }

    val sightingCommon = sighting.commonName.catalogSearchKey()
    if (sightingCommon.isNotBlank()) {
        plants.firstOrNull { plant ->
            plant.catalogCommonNameKeys().any { it == sightingCommon }
        }?.let { return it }

        // Fallback flexible para pequeños cambios de escritura: "adelfa roja" ↔ "adelfa".
        plants.firstOrNull { plant ->
            plant.catalogCommonNameKeys().any { key ->
                key.length >= 4 && sightingCommon.length >= 4 && (key.contains(sightingCommon) || sightingCommon.contains(key))
            }
        }?.let { return it }
    }

    return null
}

private fun PlantEntity.catalogCommonNameKeys(): List<String> =
    (listOf(commonName) + commonNames.split(","))
        .map { it.catalogSearchKey() }
        .filter { it.isNotBlank() }

private fun String.canonicalScientificName(): String {
    val parts = catalogSearchKey().split(Regex("\\s+")).filter { it.isNotBlank() }
    return parts.take(2).joinToString(" ")
}

private fun String.catalogSearchKey(): String {
    val withoutAccents = Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
    return withoutAccents
        .lowercase(Locale.getDefault())
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
}

private fun typeEmoji(type: String): String = when (type) {
    "Planta" -> "🌿"
    "Seta" -> "🍄"
    "Liquen" -> "🪨"
    else -> "📍"
}

@Composable
private fun SightingEditorTextField(
    label: String,
    value: String,
    minLines: Int = 1,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        minLines = minLines,
        maxLines = if (minLines == 1) 1 else 5
    )
}
