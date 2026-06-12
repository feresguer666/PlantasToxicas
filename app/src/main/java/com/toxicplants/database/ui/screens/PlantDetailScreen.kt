package com.toxicplants.database.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toxicplants.database.CompoundEntity
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.ui.theme.carbonEffectSubtle
import com.toxicplants.database.ui.LocalImageCache
import com.toxicplants.database.ui.viewmodel.CompoundViewModel
import com.toxicplants.database.ui.viewmodel.PlantViewModel
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantDetailScreen(
    plantId: Int,
    viewModel: PlantViewModel,
    compoundViewModel: CompoundViewModel? = null,
    onBack: () -> Unit,
    onEdit: ((Int) -> Unit)? = null,
    onNavigateToLocation: ((Int) -> Unit)? = null,
    onCompoundClick: ((CompoundEntity) -> Unit)? = null
) {
    val context        = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val aiScope = rememberCoroutineScope()
    var suggestingNames by remember { mutableStateOf(false) }
    var nameSuggestMsg by remember { mutableStateOf("") }

    val allPlants by viewModel.allPlants.observeAsState(initial = emptyList())
    val selectedPlant by viewModel.selectedPlantData.collectAsState()
    val plant = remember(allPlants, plantId, selectedPlant) {
        allPlants.firstOrNull { it.id == plantId }
            ?: selectedPlant?.takeIf { it.id == plantId }
    }

    // Sembrar datos fenológicos si faltan
    LaunchedEffect(Unit) { viewModel.seedPhenologyIfNeeded() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(plant?.commonName ?: "Detalle", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor        = Color(0xFF2E7D32),
                    titleContentColor     = Color.White,
                    navigationIconContentColor = Color.White
                ),
                actions = {
                    plant?.let { p ->
                        // Botón de ubicación
                        IconButton(onClick = { onNavigateToLocation?.invoke(p.id) }) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = "Ubicación",
                                tint = if (p.latitude != null && p.longitude != null)
                                    Color.Yellow else Color.White
                            )
                        }
                        // Botón favorito
                        IconButton(onClick = { viewModel.toggleFavorite(p.id, p.isFavorite) }) {
                            Icon(
                                if (p.isFavorite) Icons.Default.Favorite
                                else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorito",
                                tint = Color.White
                            )
                        }
                        // Botón editar
                        if (onEdit != null) {
                            IconButton(onClick = { onEdit(p.id) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color.White)
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->

        if (plant == null) {
            // ── Planta no encontrada ────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxSize().carbonEffectSubtle().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("❌", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Planta no encontrada", color = Color.Gray)
                }
            }

        } else {
            val p     = plant
            val scope = rememberCoroutineScope()

            // ── Estado de imagen ────────────────────────────────────────
            var loadAttempts  by remember(p.id) { mutableIntStateOf(0) }

            // ── Estado de generación de IA ──────────────────────────
            var isGeneratingAiImg by remember { mutableStateOf(false) }

            // ── Estado del diálogo de URL manual / cambio de imagen ─────
            var showUrlDialog by remember { mutableStateOf(false) }
            var showChangeImageDialog by remember { mutableStateOf(false) }
            var manualUrl     by remember { mutableStateOf("") }
            var isSavingUrl   by remember { mutableStateOf(false) }

            val galleryLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri ->
                if (uri != null) {
                    scope.launch {
                        isSavingUrl = true
                        val saved = LocalImageCache.saveFromUri(context, p.id, uri)
                        if (saved) {
                            val localPath = "file://${LocalImageCache.getLocalImagePath(context, p.id)}"
                            viewModel.insertPlantSync(p.copy(imageUrl = localPath))
                            loadAttempts++
                            Toast.makeText(context, "Imagen guardada", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "No se pudo guardar la imagen", Toast.LENGTH_SHORT).show()
                        }
                        isSavingUrl = false
                    }
                }
            }

            // La carga visual la gestiona PlantImageCard. El contador loadAttempts fuerza
            // que se recargue después de cambiar, borrar o guardar una foto.

            // ── Diálogo principal: cambiar imagen ────────────────────────
            if (showChangeImageDialog) {
                AlertDialog(
                    onDismissRequest = { showChangeImageDialog = false },
                    title = { Text("Cambiar imagen") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                p.scientificName,
                                fontStyle = FontStyle.Italic,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = {
                                    showChangeImageDialog = false
                                    scope.launch {
                                        LocalImageCache.deleteLocalImage(context, p.id)
                                        loadAttempts++
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("🔍 Buscar otra online") }

                            OutlinedButton(
                                onClick = {
                                    showChangeImageDialog = false
                                    galleryLauncher.launch("image/*")
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("🖼️ Elegir foto del móvil") }

                            OutlinedButton(
                                onClick = {
                                    showChangeImageDialog = false
                                    showUrlDialog = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("🔗 Pegar URL de imagen") }

                            OutlinedButton(
                                onClick = {
                                    showChangeImageDialog = false
                                    scope.launch {
                                        isGeneratingAiImg = true
                                        viewModel.forceAiImageGeneration(p.id, context, p)
                                        loadAttempts++
                                        isGeneratingAiImg = false
                                    }
                                },
                                enabled = !isGeneratingAiImg,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("🤖 Generar ilustración IA") }

                            TextButton(
                                onClick = {
                                    LocalImageCache.deleteLocalImage(context, p.id)
                                    loadAttempts++
                                    showChangeImageDialog = false
                                    Toast.makeText(context, "Buscando otra imagen", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("🗑️ Borrar y buscar de nuevo") }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showChangeImageDialog = false }) { Text("Cerrar") }
                    }
                )
            }

            // ── Diálogo de pegar URL manual ──────────────────────────────
            if (showUrlDialog) {
                AlertDialog(
                    onDismissRequest = { showUrlDialog = false },
                    title = { Text("Pegar URL de imagen") },
                    text = {
                        Column {
                            Text(
                                "1. Abre Wikipedia o Google\n" +
                                        "2. Busca la planta\n" +
                                        "3. Copia la URL de la imagen\n" +
                                        "4. Pégala aquí",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = manualUrl,
                                onValueChange = { manualUrl = it },
                                label = { Text("URL de la imagen") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            TextButton(onClick = {
                                val clip = clipboardManager.getText()
                                if (clip != null) manualUrl = clip.text
                            }) {
                                Text("Pegar del portapapeles")
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (manualUrl.isNotBlank()) {
                                    isSavingUrl   = true
                                    showUrlDialog = false
                                    scope.launch {
                                        val saved = LocalImageCache.downloadAndSave(
                                            context, p.id, manualUrl
                                        )
                                        if (saved) {
                                            val localPath = "file://${LocalImageCache.getLocalImagePath(context, p.id)}"
                                            viewModel.insertPlantSync(p.copy(imageUrl = localPath))
                                            loadAttempts++
                                            Toast.makeText(context, "Imagen descargada", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "No se pudo descargar la imagen", Toast.LENGTH_SHORT).show()
                                        }
                                        manualUrl   = ""
                                        isSavingUrl = false
                                    }
                                }
                            },
                            enabled = manualUrl.isNotBlank() && !isSavingUrl
                        ) {
                            if (isSavingUrl) {
                                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(6.dp))
                            }
                            Text("Descargar")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showUrlDialog = false }) { Text("Cancelar") }
                    }
                )
            }

            // ── Contenido principal ──────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // ══════════════════════════════════════════════════════════
                // TARJETA DE IMAGEN  — usa PlantImageCard + PlantImageHelper
                // ══════════════════════════════════════════════════════════
                Card(
                    modifier  = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    // Imagen principal (280dp de alto)
                    PlantImageCard(
                        plant      = p,
                        height     = 280.dp,
                        showReload = true,
                        reloadKey  = loadAttempts,
                        modifier   = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = { showChangeImageDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Text("🖼️ Cambiar imagen", fontSize = 13.sp)
                    }

                    // Barra inferior con accesos rápidos
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Botón: buscar imagen automáticamente
                        OutlinedButton(
                            onClick = {
                                LocalImageCache.deleteLocalImage(context, p.id)
                                loadAttempts++
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text("🔍 Buscar", fontSize = 11.sp)
                        }

                        // Botón: forzar generación con IA
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    isGeneratingAiImg = true
                                    viewModel.forceAiImageGeneration(p.id, context, p)
                                    loadAttempts++
                                    isGeneratingAiImg = false
                                }
                            },
                            enabled = !isGeneratingAiImg,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            if (isGeneratingAiImg) {
                                CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(4.dp))
                                Text("...", fontSize = 11.sp)
                            } else {
                                Text("🤖 Foto IA", fontSize = 11.sp)
                            }
                        }

                        // Botón: pegar URL manual
                        OutlinedButton(
                            onClick = { showUrlDialog = true },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text("🔗 URL", fontSize = 11.sp)
                        }

                        // Botón: abrir Wikipedia (fotos de Commons)
                        OutlinedButton(
                            onClick = {
                                val url = "https://commons.wikimedia.org/w/index.php" +
                                        "?search=${Uri.encode(p.scientificName)}" +
                                        "&title=Special:MediaSearch&type=image"
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                )
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text("🌐 Wiki", fontSize = 11.sp)
                        }
                    }
                }

                // ══════════════════════════════════════════════════════════
                // TARJETA DE UBICACIÓN (si existe)
                // ══════════════════════════════════════════════════════════
                if (p.latitude != null && p.longitude != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors   = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint     = Color(0xFF1565C0),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "📍 Ubicación guardada",
                                    fontWeight = FontWeight.Bold,
                                    color      = Color(0xFF1565C0)
                                )
                                if (!p.locationName.isNullOrBlank()) {
                                    Text(p.locationName, fontSize = 12.sp, color = Color.Gray)
                                }
                                Text(
                                    "Lat: ${p.latitude}, Lon: ${p.longitude}",
                                    fontSize = 10.sp,
                                    color    = Color.Gray
                                )
                                if (!p.notes.isNullOrBlank()) {
                                    Text(
                                        "Nota: ${p.notes}",
                                        fontSize  = 11.sp,
                                        color     = Color.Gray,
                                        fontStyle = FontStyle.Italic
                                    )
                                }
                            }
                            TextButton(onClick = { onNavigateToLocation?.invoke(p.id) }) {
                                Text("Editar", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // ══════════════════════════════════════════════════════════
                // TARJETA DE NOMBRE Y TOXICIDAD
                // ══════════════════════════════════════════════════════════
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors   = CardDefaults.cardColors(
                        containerColor = getToxicityColor(p.toxicityLevel).copy(alpha = 0.1f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(p.commonName, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(
                            p.scientificName,
                            fontSize  = 16.sp,
                            fontStyle = FontStyle.Italic,
                            color     = Color.Gray
                        )
                        if (p.commonNames.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "También conocida como: ${p.commonNames}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Botón: sugerir nombres comunes con IA
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                if (suggestingNames) return@OutlinedButton
                                suggestingNames = true
                                nameSuggestMsg = ""
                                aiScope.launch {
                                    val result = com.toxicplants.database.ui.GeminiNameHelper.suggestCommonNames(
                                        scientificName = p.scientificName,
                                        knownCommonName = p.commonName
                                    )
                                    when (result) {
                                        is com.toxicplants.database.ui.GeminiNameHelper.Result.Success -> {
                                            // Fusiona con los existentes evitando duplicados (ignorando mayúsculas).
                                            val existing = p.commonNames.split(",")
                                                .map { it.trim() }.filter { it.isNotBlank() }
                                            val merged = (existing + result.names)
                                                .map { it.trim() }
                                                .filter { it.isNotBlank() && !it.equals(p.commonName, true) }
                                                .distinctBy { it.lowercase() }
                                            if (merged.isEmpty()) {
                                                nameSuggestMsg = "La IA no encontró nombres comunes adicionales."
                                            } else {
                                                viewModel.insertPlant(p.copy(commonNames = merged.joinToString(", ")))
                                                nameSuggestMsg = "✅ Añadidos ${result.names.size} nombre(s) sugeridos."
                                            }
                                        }
                                        is com.toxicplants.database.ui.GeminiNameHelper.Result.Error -> {
                                            nameSuggestMsg = result.message
                                        }
                                    }
                                    suggestingNames = false
                                }
                            },
                            enabled = !suggestingNames
                        ) {
                            if (suggestingNames) {
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Generando…", fontSize = 13.sp)
                            } else {
                                Text("🤖 Sugerir nombres con IA", fontSize = 13.sp)
                            }
                        }
                        if (nameSuggestMsg.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(nameSuggestMsg, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = getToxicityColor(p.toxicityLevel),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                "Toxicidad: ${p.toxicityLevel}",
                                modifier   = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color      = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // ══════════════════════════════════════════════════════════
                // BOTONES DE WIKIPEDIA
                // ══════════════════════════════════════════════════════════
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Ver en Wikipedia", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val wikiUrl = "https://es.wikipedia.org/wiki/" +
                                    Uri.encode(
                                        p.scientificName.takeIf { it.isNotBlank() } ?: p.commonName
                                    )
                            val commonsUrl = "https://commons.wikimedia.org/w/index.php" +
                                    "?search=${Uri.encode(p.scientificName)}" +
                                    "&title=Special:MediaSearch&type=image"

                            Button(
                                onClick = {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(wikiUrl))
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors   = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1565C0)
                                )
                            ) { Text("Artículo", fontSize = 12.sp) }

                            Button(
                                onClick = {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(commonsUrl))
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors   = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2E7D32)
                                )
                            ) { Text("Fotos", fontSize = 12.sp) }
                        }
                    }
                }

                // ══════════════════════════════════════════════════════════
                // SECCIONES DE INFORMACIÓN
                // ══════════════════════════════════════════════════════════
                DetailSection(title = "Descripción",    content = p.description)
                AiFillButton(
                    label = "🤖 Generar descripción con IA",
                    plant = p,
                    fieldType = com.toxicplants.database.ui.GeminiNameHelper.FieldType.DESCRIPTION,
                    viewModel = viewModel
                )
                DetailSection(title = "Síntomas",       content = p.symptoms)
                AiFillButton(
                    label = "🤖 Generar síntomas de intoxicación con IA",
                    plant = p,
                    fieldType = com.toxicplants.database.ui.GeminiNameHelper.FieldType.SYMPTOMS,
                    viewModel = viewModel
                )
                DetailSection(title = "Primeros Auxilios", content = p.firstAid)
                DetailSection(title = "Partes Tóxicas", content = p.toxicParts)

                // ══════════════════════════════════════════════════════════
                // COMPUESTOS TÓXICOS QUE CONTIENE
                // ══════════════════════════════════════════════════════════
                if (compoundViewModel != null && onCompoundClick != null) {
                    val allCompounds by compoundViewModel.allCompounds.observeAsState(emptyList())
                    val relatedCompounds = remember(allCompounds, p.scientificName) {
                        val sciName = p.scientificName.trim().lowercase()
                        val genus = sciName.split(" ").firstOrNull() ?: ""
                        allCompounds.filter { compound ->
                            compound.sourcePlants.split("|").any { sp ->
                                val spClean = sp.trim().lowercase()
                                spClean == sciName ||
                                        spClean.startsWith(sciName.split(" ").take(2).joinToString(" ")) ||
                                        (spClean == "$genus spp." || spClean == "$genus spp")
                            }
                        }
                    }
                    if (relatedCompounds.isNotEmpty()) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "🧪 Compuestos tóxicos (${relatedCompounds.size})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                relatedCompounds.forEach { compound ->
                                    val chipColor = try {
                                        Color(android.graphics.Color.parseColor(compound.groupColor))
                                    } catch (_: Exception) { Color(0xFF7B1FA2) }

                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                            .clickable { onCompoundClick(compound) },
                                        color = chipColor.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("🧪", fontSize = 16.sp)
                                            Spacer(Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    compound.commonName,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    compound.groupName,
                                                    fontSize = 11.sp,
                                                    color = Color.Gray,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Surface(
                                                color = chipColor.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    "Ver →",
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                    fontSize = 11.sp,
                                                    color = chipColor,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                
                if (p.mythsAndLegends.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "📖 Mitos y Curiosidades",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = p.mythsAndLegends,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = FontStyle.Italic,
                                lineHeight = 22.sp
                            ),
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                DetailSection(title = "Hábitat",        content = p.habitat)
                DetailSection(title = "Distribución",   content = p.geographicDistribution)
                AiFillButton(
                    label = "🤖 Clasificar región / distribución con IA",
                    plant = p,
                    fieldType = com.toxicplants.database.ui.GeminiNameHelper.FieldType.REGION,
                    viewModel = viewModel
                )

                // ══════════════════════════════════════════════════════════
                // CALENDARIO FENOLÓGICO
                // ══════════════════════════════════════════════════════════
                if (p.floweringMonths.isNotBlank() || p.fruitingMonths.isNotBlank() || p.maxToxicityMonths.isNotBlank()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "📅 Calendario fenológico",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Periodos anuales de esta planta",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Barra visual de 12 meses
                            val monthLabels = listOf(
                                "Ene", "Feb", "Mar", "Abr", "May", "Jun",
                                "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"
                            )
                            val floweringMonths = p.floweringMonths.toIntList()
                            val fruitingMonths = p.fruitingMonths.toIntList()
                            val maxToxMonths = p.maxToxicityMonths.toIntList()

                            // Encabezados de mes
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                monthLabels.forEach { label ->
                                    Text(
                                        label,
                                        fontSize = 10.sp,
                                        color = Color.Gray,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))

                            // Fila: Floración
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("🌸", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    (1..12).forEach { month ->
                                        val isActive = month in floweringMonths
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(14.dp)
                                                .padding(horizontal = 1.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(
                                                    if (isActive) Color(0xFFE91E63)
                                                    else Color.Gray.copy(alpha = 0.1f)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isActive) {
                                                Text(
                                                    "${month}",
                                                    fontSize = 8.sp,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))

                            // Fila: Fructificación
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("🍎", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    (1..12).forEach { month ->
                                        val isActive = month in fruitingMonths
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(14.dp)
                                                .padding(horizontal = 1.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(
                                                    if (isActive) Color(0xFFFF9800)
                                                    else Color.Gray.copy(alpha = 0.1f)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isActive) {
                                                Text(
                                                    "${month}",
                                                    fontSize = 8.sp,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))

                            // Fila: Toxicidad máxima
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("☠️", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    (1..12).forEach { month ->
                                        val isActive = month in maxToxMonths
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(14.dp)
                                                .padding(horizontal = 1.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(
                                                    if (isActive) Color(0xFFF44336)
                                                    else Color.Gray.copy(alpha = 0.1f)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isActive) {
                                                Text(
                                                    "${month}",
                                                    fontSize = 8.sp,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            // Leyenda
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                LegendEntry("🌸 Floración", Color(0xFFE91E63))
                                LegendEntry("🍎 Frutos", Color(0xFFFF9800))
                                LegendEntry("☠️ Tóxica máx.", Color(0xFFF44336))
                            }

                            // Texto descriptivo de meses
                            Spacer(modifier = Modifier.height(8.dp))
                            if (floweringMonths.isNotEmpty()) {
                                val monthsStr = floweringMonths.map { monthLabels[it - 1] }.joinToString(", ")
                                Text("🌸 Floración: $monthsStr", fontSize = 13.sp, color = Color(0xFFE91E63))
                            }
                            if (fruitingMonths.isNotEmpty()) {
                                val monthsStr = fruitingMonths.map { monthLabels[it - 1] }.joinToString(", ")
                                Text("🍎 Fructificación: $monthsStr", fontSize = 13.sp, color = Color(0xFFFF9800))
                            }
                            if (maxToxMonths.isNotEmpty()) {
                                val monthsStr = maxToxMonths.map { monthLabels[it - 1] }.joinToString(", ")
                                Text("☠️ Toxicidad máxima: $monthsStr", fontSize = 13.sp, color = Color(0xFFF44336))
                            }
                        }
                    }
                }

                // ══════════════════════════════════════════════════════════
                // INFORMACIÓN ADICIONAL
                // ══════════════════════════════════════════════════════════
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Información adicional", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        InfoRow("Categoría", p.category)
                        InfoRow("Familia",   p.family)
                        InfoRow("Nivel",     p.toxicityLevel)
                    }
                }

            } // fin Column
        } // fin else (plant != null)
    } // fin Scaffold content
}

// ── Composables auxiliares ────────────────────────────────────────────────

@Composable
fun DetailSection(title: String, content: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            // Texto con detección automática de términos botánicos:
            // las palabras del glosario (umbela, palmeada, látex…) se subrayan
            // y al pulsarlas se abre un pop-up con ilustración y definición.
            com.toxicplants.database.ui.components.GlossaryText(
                text = content,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray,      fontSize = 14.sp)
        Text(value, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}

fun getToxicityColor(level: String): Color = when (level) {
    "Mortal"   -> Color(0xFFB71C1C)
    "Muy alto" -> Color(0xFFFF5722)
    "Alto"     -> Color(0xFFE65100)
    "Moderado" -> Color(0xFFF57C00)
    "Bajo"     -> Color(0xFF388E3C)
    else       -> Color.Gray
}

/** Convierte "3,4,5" → listOf(3,4,5) */
private fun String.toIntList(): List<Int> =
    if (isBlank()) emptyList()
    else split(",").mapNotNull { it.trim().toIntOrNull() }

@Composable
private fun LegendEntry(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 11.sp, color = Color.Gray)
    }
}

/**
 * Botón reutilizable que genera el texto de un campo con IA y lo guarda en la planta.
 * Muestra estado de carga y mensaje de resultado.
 */
@Composable
fun AiFillButton(
    label: String,
    plant: PlantEntity,
    fieldType: com.toxicplants.database.ui.GeminiNameHelper.FieldType,
    viewModel: PlantViewModel
) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    Column {
        OutlinedButton(
            onClick = {
                if (loading) return@OutlinedButton
                loading = true
                message = ""
                scope.launch {
                    val result = com.toxicplants.database.ui.GeminiNameHelper.generateField(
                        type = fieldType,
                        scientificName = plant.scientificName,
                        commonName = plant.commonName
                    )
                    when (result) {
                        is com.toxicplants.database.ui.GeminiNameHelper.TextResult.Success -> {
                            val updated = when (fieldType) {
                                com.toxicplants.database.ui.GeminiNameHelper.FieldType.DESCRIPTION ->
                                    plant.copy(description = result.text)
                                com.toxicplants.database.ui.GeminiNameHelper.FieldType.SYMPTOMS ->
                                    plant.copy(symptoms = result.text)
                                com.toxicplants.database.ui.GeminiNameHelper.FieldType.REGION ->
                                    plant.copy(geographicDistribution = result.text)
                            }
                            viewModel.insertPlant(updated)
                            message = "✅ Generado y guardado."
                        }
                        is com.toxicplants.database.ui.GeminiNameHelper.TextResult.Error ->
                            message = result.message
                    }
                    loading = false
                }
            },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (loading) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Generando…", fontSize = 13.sp)
            } else {
                Text(label, fontSize = 13.sp)
            }
        }
        if (message.isNotBlank()) {
            Text(message, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

