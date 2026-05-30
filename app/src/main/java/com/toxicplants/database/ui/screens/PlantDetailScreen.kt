package com.toxicplants.database.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.CachePolicy
import com.toxicplants.database.R
import com.toxicplants.database.ui.LocalImageCache
import com.toxicplants.database.ui.WikiImageFetcher
import com.toxicplants.database.ui.viewmodel.PlantViewModel
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantDetailScreen(
    plantId: Int,
    viewModel: PlantViewModel,
    onBack: () -> Unit,
    onEdit: ((Int) -> Unit)? = null,
    onNavigateToLocation: ((Int) -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val allPlants by viewModel.allPlants.observeAsState(initial = emptyList())
    val plant = remember(allPlants, plantId) { allPlants.firstOrNull { it.id == plantId } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(plant?.commonName ?: "Detalle", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2E7D32),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                actions = {
                    plant?.let { p ->
                        // ✅ BOTÓN DE UBICACIÓN
                        IconButton(onClick = { onNavigateToLocation?.invoke(p.id) }) {
                            Icon(
                                if (p.latitude != null && p.longitude != null) Icons.Default.LocationOn else Icons.Default.LocationOn,
                                contentDescription = "Ubicación",
                                tint = if (p.latitude != null && p.longitude != null) Color.Yellow else Color.White
                            )
                        }
                        IconButton(onClick = { viewModel.toggleFavorite(p.id, p.isFavorite) }) {
                            Icon(if (p.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = "Favorito", tint = Color.White)
                        }
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
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("❌", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Planta no encontrada", color = Color.Gray)
                }
            }
        } else {
            val p = plant
            val scope = rememberCoroutineScope()

            val hasLocal = LocalImageCache.hasLocalImage(context, p.id)
            val localPath = LocalImageCache.getLocalImagePath(context, p.id)

            var imageSource by remember(p.id) { mutableStateOf(if (hasLocal) "file://$localPath" else "") }
            var isLoadingImage by remember(p.id) { mutableStateOf(false) }
            var imageError by remember(p.id) { mutableStateOf(false) }
            var showUrlDialog by remember { mutableStateOf(false) }
            var manualUrl by remember { mutableStateOf("") }
            var currentSource by remember(p.id) { mutableStateOf("") }

            val customLoader = remember {
                ImageLoader.Builder(context)
                    .okHttpClient {
                        OkHttpClient.Builder()
                            .addInterceptor { chain ->
                                val req = chain.request().newBuilder()
                                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                                    .header("Accept", "image/webp,image/apng,image/*,*/*;q=0.8")
                                    .build()
                                chain.proceed(req)
                            }
                            .followRedirects(true)
                            .build()
                    }
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .crossfade(true)
                    .build()
            }

            fun searchAllImages() {
                if (isLoadingImage) return
                isLoadingImage = true
                imageError = false

                scope.launch {
                    var found = false

                    if (hasLocal) {
                        imageSource = "file://$localPath"
                        currentSource = "Local"
                        found = true
                    }

                    if (!found) {
                        val wikiUrl = WikiImageFetcher.getImageUrl(p.scientificName)
                        if (wikiUrl.isNotBlank()) {
                            val saved = LocalImageCache.downloadAndSave(context, p.id, wikiUrl)
                            if (saved) {
                                imageSource = "file://$localPath"
                                currentSource = "Wikipedia"
                                found = true
                            }
                        }
                    }

                    if (!found && p.commonName != p.scientificName) {
                        val commonUrl = WikiImageFetcher.getImageUrl(p.commonName)
                        if (commonUrl.isNotBlank()) {
                            val saved = LocalImageCache.downloadAndSave(context, p.id, commonUrl)
                            if (saved) {
                                imageSource = "file://$localPath"
                                currentSource = "Wikipedia (común)"
                                found = true
                            }
                        }
                    }

                    if (!found) imageError = true
                    isLoadingImage = false
                }
            }

            LaunchedEffect(p.id) {
                if (!hasLocal && imageSource.isEmpty()) {
                    searchAllImages()
                }
            }

            if (showUrlDialog) {
                AlertDialog(
                    onDismissRequest = { showUrlDialog = false },
                    title = { Text("Pegar URL de imagen") },
                    text = {
                        Column {
                            Text("1. Abre Wikipedia o Google\n2. Busca la planta\n3. Copia URL de imagen\n4. Pégala aquí", fontSize = 12.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = manualUrl, onValueChange = { manualUrl = it }, label = { Text("URL de la imagen") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            Spacer(modifier = Modifier.height(4.dp))
                            TextButton(onClick = {
                                val clip = clipboardManager.getText()
                                if (clip != null) manualUrl = clip.text
                            }) { Text("Pegar del portapapeles") }
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (manualUrl.isNotBlank()) {
                                isLoadingImage = true
                                showUrlDialog = false
                                scope.launch {
                                    val saved = LocalImageCache.downloadAndSave(context, p.id, manualUrl)
                                    if (saved) { imageSource = "file://$localPath"; imageError = false; currentSource = "Manual" }
                                    isLoadingImage = false
                                    manualUrl = ""
                                }
                            }
                        }, enabled = manualUrl.isNotBlank()) { Text("Descargar") }
                    },
                    dismissButton = { TextButton(onClick = { showUrlDialog = false }) { Text("Cancelar") } }
                )
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ✅ TARJETA DE IMAGEN
                Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
                    Box(modifier = Modifier.fillMaxWidth().height(280.dp), contentAlignment = Alignment.Center) {
                        if (imageSource.isNotEmpty() && !imageError) {
                            AsyncImage(model = if (imageSource.startsWith("file://")) File(localPath) else imageSource, imageLoader = customLoader, contentDescription = p.commonName, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Image(painter = painterResource(id = R.drawable.placeholder_plant), contentDescription = "Imagen no disponible", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        }

                        Column(modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            if (isLoadingImage) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Buscando imagen...", color = Color.White, fontSize = 12.sp)
                                }
                            } else {
                                Text(text = when { imageError -> "❌ Sin imagen"; hasLocal -> "✅ Imagen guardada"; imageSource.isNotEmpty() -> "✅ $currentSource"; else -> "⚠️ Toca buscar" }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Button(onClick = { searchAllImages() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Buscar", fontSize = 10.sp)
                                    }
                                    Button(onClick = { showUrlDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6F00)), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) { Text("Pegar URL", fontSize = 10.sp) }
                                    // ✅ Después - agrega ) después de Uri.parse(...)):
                                    Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://commons.wikimedia.org/w/index.php?search=${Uri.encode(p.scientificName)}&title=Special:MediaSearch&type=image"))) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) { Text("Wiki", fontSize = 10.sp) }
                                }
                            }
                        }
                    }
                }

                // ✅ TARJETA CON UBICACIÓN (SI EXISTE)
                if (p.latitude != null && p.longitude != null) {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF1565C0), modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("📍 Ubicación guardada", fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                                if (!p.locationName.isNullOrBlank()) {
                                    Text(p.locationName, fontSize = 12.sp, color = Color.Gray)
                                }
                                Text("Lat: ${p.latitude}, Lon: ${p.longitude}", fontSize = 10.sp, color = Color.Gray)
                                if (!p.notes.isNullOrBlank()) {
                                    Text("Nota: ${p.notes}", fontSize = 11.sp, color = Color.Gray, fontStyle = FontStyle.Italic)
                                }
                            }
                            TextButton(onClick = { onNavigateToLocation?.invoke(p.id) }) {
                                Text("Editar", fontSize = 12.sp)
                            }
                        }
                    }
                }

                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = getToxicityColor(p.toxicityLevel).copy(alpha = 0.1f))) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(p.commonName, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(p.scientificName, fontSize = 16.sp, fontStyle = FontStyle.Italic, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(color = getToxicityColor(p.toxicityLevel), shape = MaterialTheme.shapes.small) {
                            Text("Toxicidad: ${p.toxicityLevel}", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // ✅ DESPUÉS:
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Ver en Wikipedia", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val wikiUrl = "https://es.wikipedia.org/wiki/${Uri.encode(p.scientificName.takeIf { it.isNotBlank() } ?: p.commonName)}"
                            val commonsUrl = "https://commons.wikimedia.org/w/index.php?search=${Uri.encode(p.scientificName)}&title=Special:MediaSearch&type=image"
                            Button(
                                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(wikiUrl))) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                            ) { Text("Artículo", fontSize = 12.sp) }
                            Button(
                                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(commonsUrl))) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                            ) { Text("Fotos", fontSize = 12.sp) }
                        }
                    }
                }
                DetailSection(title = "Descripción", content = p.description)
                DetailSection(title = "Síntomas", content = p.symptoms)
                DetailSection(title = "Primeros Auxilios", content = p.firstAid)
                DetailSection(title = "Partes Tóxicas", content = p.toxicParts)
                DetailSection(title = "Hábitat", content = p.habitat)
                DetailSection(title = "Distribución", content = p.geographicDistribution)

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Información adicional", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        InfoRow("Categoría", p.category)
                        InfoRow("Familia", p.family)
                        InfoRow("Nivel", p.toxicityLevel)
                    }
                }
            }
        }
    }
}

@Composable
fun DetailSection(title: String, content: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(content, fontSize = 14.sp)
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray, fontSize = 14.sp)
        Text(value, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}

fun getToxicityColor(level: String): Color {
    return when (level) {
        "Mortal" -> Color(0xFFB71C1C)
        "Muy alto" -> Color(0xFFFF5722)
        "Alto" -> Color(0xFFE65100)
        "Moderado" -> Color(0xFFF57C00)
        "Bajo" -> Color(0xFF388E3C)
        else -> Color.Gray
    }
}