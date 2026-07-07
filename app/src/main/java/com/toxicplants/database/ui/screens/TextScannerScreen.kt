package com.toxicplants.database.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.ui.viewmodel.PlantViewModel
import java.text.Normalizer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextScannerScreen(
    viewModel: PlantViewModel,
    onPlantClick: (PlantEntity) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val allPlants by viewModel.allPlants.observeAsState(emptyList())

    var recognizedText by remember { mutableStateOf("") }
    var manualQuery by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf("") }
    var cameraBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedImageText by remember { mutableStateOf("") }

    fun processImage(image: InputImage) {
        isProcessing = true
        errorText = ""
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            .process(image)
            .addOnSuccessListener { result ->
                recognizedText = result.text.trim()
                manualQuery = bestLineForSearch(recognizedText)
                if (recognizedText.isBlank()) {
                    errorText = "No he podido leer texto. Prueba con una foto más cercana y bien iluminada."
                }
                isProcessing = false
            }
            .addOnFailureListener { e ->
                errorText = "Error leyendo texto: ${e.message ?: "desconocido"}"
                isProcessing = false
            }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            cameraBitmap = null
            selectedImageText = "Imagen seleccionada"
            try {
                processImage(InputImage.fromFilePath(context, uri))
            } catch (e: Exception) {
                errorText = "No se pudo abrir la imagen: ${e.message ?: "error"}"
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        if (bitmap != null) {
            cameraBitmap = bitmap
            selectedImageText = "Foto tomada con cámara"
            processImage(InputImage.fromBitmap(bitmap, 0))
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) cameraLauncher.launch(null)
        else errorText = "Permiso de cámara denegado. Puedes seleccionar una imagen de la galería."
    }

    fun openCamera() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (granted) cameraLauncher.launch(null)
        else permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val searchText = manualQuery.ifBlank { recognizedText }
    val matches = remember(allPlants, searchText) {
        findPlantMatches(allPlants, searchText)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("📷 Escáner de texto", fontWeight = FontWeight.Bold)
                        Text("Lee etiquetas, libros o carteles y busca la planta", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1B5E20),
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Brush.verticalGradient(listOf(Color(0xFF061207), Color(0xFF0D2410), Color(0xFF123B18))))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF17351B)), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { openCamera() }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Filled.CameraAlt, contentDescription = null)
                                Spacer(Modifier.size(6.dp))
                                Text("Cámara")
                            }
                            OutlinedButton(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                                Spacer(Modifier.size(6.dp))
                                Text("Galería")
                            }
                        }

                        if (selectedImageText.isNotBlank()) {
                            Text(selectedImageText, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                        }

                        cameraBitmap?.let { bitmap ->
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Foto escaneada",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                            )
                        }

                        if (isProcessing) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                                Spacer(Modifier.size(10.dp))
                                Text("Leyendo texto...", color = Color.White)
                            }
                        }

                        if (errorText.isNotBlank()) {
                            Text(errorText, color = Color(0xFFFFAB91), fontSize = 13.sp)
                        }
                    }
                }
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF102A43)), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Texto leído / búsqueda", color = Color.White, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = manualQuery,
                            onValueChange = { manualQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            label = { Text("Corrige aquí el nombre si hace falta") },
                            placeholder = { Text("Ej: Nerium oleander") },
                            trailingIcon = { Icon(Icons.Filled.Search, contentDescription = null) }
                        )

                        if (recognizedText.isNotBlank()) {
                            Text("OCR completo:", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(recognizedText, color = Color.White.copy(alpha = 0.72f), fontSize = 12.sp, maxLines = 6, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            item {
                Text("Coincidencias en la base de datos: ${matches.size}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            if (matches.isEmpty()) {
                item {
                    Text(
                        "Cuando escanees una etiqueta o escribas un nombre, aquí aparecerán las plantas coincidentes.",
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 14.sp
                    )
                }
            } else {
                items(matches) { plant ->
                    TextScannerPlantCard(plant = plant, onClick = { onPlantClick(plant) })
                }
            }
        }
    }
}

@Composable
private fun TextScannerPlantCard(
    plant: PlantEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A5F)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(plant.commonName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text(plant.scientificName, color = Color.White.copy(alpha = 0.82f), fontStyle = FontStyle.Italic)
            Text("Familia: ${plant.family} · Toxicidad: ${plant.toxicityLevel}", color = Color.White.copy(alpha = 0.72f), fontSize = 12.sp)
        }
    }
}

private fun bestLineForSearch(text: String): String {
    return text
        .lines()
        .map { it.trim() }
        .filter { it.length >= 3 }
        .maxByOrNull { line ->
            var score = line.length
            if (line.any { it.isLowerCase() } && line.any { it.isUpperCase() }) score += 12
            if (line.split(" ").size in 2..3) score += 10
            if (line.contains(Regex("\\d"))) score -= 20
            score
        }
        ?: text.take(80)
}

private fun findPlantMatches(plants: List<PlantEntity>, rawQuery: String): List<PlantEntity> {
    val query = normalizeText(rawQuery)
    if (query.length < 2) return emptyList()

    val words = query.split(Regex("\\s+"))
        .filter { it.length >= 3 }
        .distinct()
        .take(12)

    return plants.mapNotNull { plant ->
        val haystack = normalizeText(
            listOf(
                plant.commonName,
                plant.commonNames,
                plant.scientificName,
                plant.family
            ).joinToString(" ")
        )

        val directScore = when {
            haystack.contains(query) -> 100
            query.contains(normalizeText(plant.scientificName)) -> 95
            query.contains(normalizeText(plant.commonName)) -> 90
            else -> 0
        }

        val wordScore = words.count { haystack.contains(it) } * 12
        val score = directScore + wordScore
        if (score > 0) plant to score else null
    }
        .sortedByDescending { it.second }
        .take(30)
        .map { it.first }
}

private fun normalizeText(value: String): String {
    val noAccents = Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
    return noAccents.replace(Regex("[^a-z0-9ñ ]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}
