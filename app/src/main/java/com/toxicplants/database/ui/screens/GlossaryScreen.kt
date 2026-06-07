package com.toxicplants.database.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items as listItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toxicplants.database.GlossaryCategory
import com.toxicplants.database.GlossaryDataSource
import com.toxicplants.database.GlossaryTerm
import com.toxicplants.database.data.repository.GlossaryPhotoRepository
import com.toxicplants.database.ui.components.AssetImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.graphics.BitmapFactory
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlossaryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val data = remember { GlossaryDataSource.load(context) }
    val colors = MaterialTheme.colorScheme

    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var openedTerm by remember { mutableStateOf<GlossaryTerm?>(null) }

    val filtered = remember(query, selectedCategory, data) {
        var list = data.terms
        if (!selectedCategory.isNullOrBlank()) {
            list = list.filter { it.category == selectedCategory }
        }
        if (query.isNotBlank()) {
            val nq = GlossaryDataSource.normalize(query)
            list = list.filter { t ->
                val candidates = (listOf(t.term) + t.synonyms + listOf(t.definition))
                candidates.any { GlossaryDataSource.normalize(it).contains(nq) }
            }
        }
        list
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Glosario botánico") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.primary,
                    titleContentColor = colors.onPrimary,
                    navigationIconContentColor = colors.onPrimary
                )
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Buscar término (umbela, palmeada, látex…)") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp)
            )

            CategoryChips(
                categories = data.categories,
                selected = selectedCategory,
                onSelect = { selectedCategory = if (selectedCategory == it) null else it }
            )

            Text(
                "${filtered.size} término(s)",
                fontSize = 12.sp,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                gridItems(filtered, key = { it.id }) { term ->
                    TermCard(term = term, onClick = { openedTerm = term })
                }
            }
        }
    }

    openedTerm?.let { term ->
        GlossaryTermDialog(term = term, onDismiss = { openedTerm = null })
    }
}

@Composable
private fun CategoryChips(
    categories: List<GlossaryCategory>,
    selected: String?,
    onSelect: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        listItems(categories) { c ->
            FilterChip(
                selected = selected == c.id,
                onClick = { onSelect(c.id) },
                label = { Text("${c.icon} ${c.label}", fontSize = 12.sp) }
            )
        }
    }
}

@Composable
private fun TermCard(term: GlossaryTerm, onClick: () -> Unit) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme

    // Mostrar una miniatura de la primera foto real (si existe).
    var thumbFile by remember(term.id) { mutableStateOf<File?>(null) }
    LaunchedEffect(term.id) {
        val photos = withContext(Dispatchers.IO) {
            GlossaryPhotoRepository.listPhotos(context, term.id)
        }
        thumbFile = photos.firstOrNull()?.file
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.primary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                val f = thumbFile
                if (f != null && f.exists()) {
                    val bmp = remember(f.absolutePath, f.lastModified()) {
                        BitmapFactory.decodeFile(f.absolutePath)
                    }
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = term.term,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else Text("📷", fontSize = 28.sp)
                } else {
                    // No hay foto aún: el usuario podrá añadir al abrir el detalle.
                    Text("📷", fontSize = 28.sp, color = colors.primary.copy(alpha = 0.5f))
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                term.term.replaceFirstChar { it.titlecase() },
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                color = colors.onSurface
            )
        }
    }
}

/** Diálogo con carrusel de fotos + definición + botones para añadir/borrar. */
@Composable
private fun GlossaryTermDialog(term: GlossaryTerm, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()

    var photos by remember(term.id) {
        mutableStateOf<List<GlossaryPhotoRepository.GlossaryPhoto>>(emptyList())
    }
    var downloading by remember(term.id) { mutableStateOf(false) }
    var refreshKey by remember(term.id) { mutableStateOf(0) }

    // Cargar fotos existentes y, si aún no hay seed, descargar de Wikimedia.
    LaunchedEffect(term.id, refreshKey) {
        photos = withContext(Dispatchers.IO) {
            GlossaryPhotoRepository.listPhotos(context, term.id)
        }
        if (photos.none { it.isSeed } && !term.wikimediaSearch.isNullOrBlank()) {
            downloading = true
            withContext(Dispatchers.IO) {
                GlossaryPhotoRepository.ensureSeedPhotos(
                    context, term.id, term.wikimediaSearch
                )
            }
            photos = withContext(Dispatchers.IO) {
                GlossaryPhotoRepository.listPhotos(context, term.id)
            }
            downloading = false
        }
    }

    val pickPhoto = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val r = GlossaryPhotoRepository.addUserPhotoFromUri(context, term.id, uri)
                if (r.isSuccess) refreshKey++
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "📖 ${term.term.replaceFirstChar { it.titlecase() }}",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                // ── Carrusel de fotos ─────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(
                            colors.surfaceVariant.copy(alpha = 0.4f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        downloading && photos.isEmpty() -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(8.dp))
                                Text("Buscando fotos…", fontSize = 12.sp)
                            }
                        }
                        photos.isEmpty() && !term.image.isNullOrBlank() -> {
                            // Aún sin fotos reales: mostramos el esquema dibujado solo.
                            PhotoCarousel(
                                photos = emptyList(),
                                schemaAssetPath = term.image,
                                onDelete = {}
                            )
                        }
                        photos.isEmpty() -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📷", fontSize = 48.sp)
                                Text(
                                    "Sin fotos aún",
                                    fontSize = 13.sp,
                                    color = colors.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Pulsa ➕ para añadir una desde tu galería",
                                    fontSize = 11.sp,
                                    color = colors.onSurfaceVariant
                                )
                            }
                        }
                        else -> {
                            PhotoCarousel(
                                photos = photos,
                                schemaAssetPath = term.image,
                                onDelete = { p ->
                                    scope.launch(Dispatchers.IO) {
                                        GlossaryPhotoRepository.deletePhoto(context, term.id, p)
                                        refreshKey++
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // ── Botones de acción ─────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { pickPhoto.launch("image/*") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Añadir foto", fontSize = 12.sp)
                    }
                    if (!term.wikimediaSearch.isNullOrBlank() && photos.none { it.isSeed }) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    downloading = true
                                    withContext(Dispatchers.IO) {
                                        // Permitir reintentar borrando el marker
                                        File(
                                            GlossaryPhotoRepository.termDir(context, term.id),
                                            ".seed_attempted"
                                        ).delete()
                                        GlossaryPhotoRepository.ensureSeedPhotos(
                                            context, term.id, term.wikimediaSearch
                                        )
                                    }
                                    refreshKey++
                                    downloading = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !downloading
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Buscar online", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ── Definición ───────────────────────────────────────
                Text(term.definition, fontSize = 14.sp, color = colors.onSurface)

                if (term.synonyms.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "También: " + term.synonyms.joinToString(", "),
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

@Composable
private fun PhotoCarousel(
    photos: List<GlossaryPhotoRepository.GlossaryPhoto>,
    schemaAssetPath: String?,
    onDelete: (GlossaryPhotoRepository.GlossaryPhoto) -> Unit
) {
    val pagerState = rememberLazyListState()
    LazyRow(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
    ) {
        // 1º: esquema dibujado (si existe)
        if (!schemaAssetPath.isNullOrBlank()) {
            item { SchemaCard(assetPath = schemaAssetPath) }
        }
        // 2º: fotos reales
        listItems(photos) { p ->
            PhotoCard(photo = p, onDelete = { onDelete(p) })
        }
    }
}

/** Carta con la ilustración esquemática dibujada (PNG en assets/key_images/). */
@Composable
private fun SchemaCard(assetPath: String) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(220.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFE8F5E9))  // verde clarito de fondo, para que se vea el contorno
    ) {
        AssetImage(
            assetPath = assetPath,
            contentDescription = "Esquema",
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentScale = ContentScale.Fit
        )
        // Etiqueta naranja "Esquema"
        Surface(
            color = Color(0xCCEF6C00),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp)
        ) {
            Text(
                "Esquema",
                color = Color.White,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun PhotoCard(
    photo: GlossaryPhotoRepository.GlossaryPhoto,
    onDelete: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val bmp = remember(photo.file.absolutePath, photo.file.lastModified()) {
        runCatching { BitmapFactory.decodeFile(photo.file.absolutePath) }.getOrNull()
    }
    var showActions by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(220.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black)
            .clickable { showActions = !showActions }
    ) {
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        // Etiqueta de tipo de foto
        Surface(
            color = if (photo.isSeed) Color(0xCC2E7D32) else Color(0xCC1565C0),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp)
        ) {
            Text(
                if (photo.isSeed) "Commons" else "Tuya",
                color = Color.White,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
        // Botón borrar (sale al tocar)
        if (showActions) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(Color(0x88000000), RoundedCornerShape(20.dp))
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Borrar", tint = Color.White)
            }
        }
    }
}
