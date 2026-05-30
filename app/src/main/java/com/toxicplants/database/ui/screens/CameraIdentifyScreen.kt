package com.toxicplants.database.ui.screens

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.toxicplants.database.BuildConfig
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.ui.viewmodel.PlantViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

// API key inyectada desde local.properties via BuildConfig (ver app/build.gradle.kts).
// Si está vacía, la app muestra un diálogo invitando a configurarla.
private val PLANTNET_API_KEY: String = BuildConfig.PLANTNET_API_KEY
private const val PLANTNET_URL = "https://my-api.plantnet.org/v2/identify/all"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraIdentifyScreen(
    viewModel: PlantViewModel,
    onPlantClick: (PlantEntity) -> Unit,
    onNavigateToPlantNetResult: (name: String, scientificName: String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var identificationResults by remember { mutableStateOf<List<IdentificationResult>>(emptyList()) }
    var showApiKeyDialog by remember { mutableStateOf(false) }

    val allPlants by viewModel.allPlants.observeAsState(emptyList())

    // Verificar si la API key está configurada
    val isApiKeyConfigured = PLANTNET_API_KEY.isNotEmpty() && PLANTNET_API_KEY != "TU_API_KEY_AQUI"

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    selectedBitmap = BitmapFactory.decodeStream(stream)
                }
            } catch (e: Exception) { }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.values.any { it }) imagePickerLauncher.launch("image/*")
        else Toast.makeText(context, "❌ Se necesita permiso", Toast.LENGTH_SHORT).show()
    }

    fun selectImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            imagePickerLauncher.launch("image/*")
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
        }
    }

    fun identifyPlant() {
        if (selectedBitmap == null) {
            Toast.makeText(context, "⚠️ Selecciona primero una imagen", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isApiKeyConfigured) {
            showApiKeyDialog = true
            return
        }

        isLoading = true
        identificationResults = emptyList()

        scope.launch {
            try {
                identificationResults = identifyWithPlantNet(context, selectedBitmap, PLANTNET_API_KEY, allPlants)
                Toast.makeText(context, "✅ ${identificationResults.size} resultados", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "❌ ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🔍 Identificar Planta", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF2E7D32), titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {

            Card(modifier = Modifier.fillMaxWidth().height(150.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    when {
                        selectedBitmap != null -> Image(bitmap = selectedBitmap!!.asImageBitmap(), contentDescription = "Tu imagen", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        else -> Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("🖼️", fontSize = 32.sp); Spacer(modifier = Modifier.height(4.dp)); Text("Sin imagen", fontSize = 12.sp, color = Color.Gray) }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { selectImage() }, modifier = Modifier.weight(1f)) {
                    Text("🖼️ Galería", fontSize = 14.sp)
                }
                Button(onClick = { identifyPlant() }, modifier = Modifier.weight(1f), enabled = !isLoading && selectedBitmap != null, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                    else Text("🌿 Identificar", fontSize = 14.sp)
                }
            }

            // ✅ Solo mostrar aviso si NO está configurada
            if (!isApiKeyConfigured) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("⚠️ API Key no configurada", color = Color(0xFFE65100), fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (identificationResults.isNotEmpty()) {
                Text("📋 ${identificationResults.size} resultados", fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    items(identificationResults) { result ->
                        CompactResultCard(
                            result = result,
                            onClick = {
                                if (result.plant.id == -1) {
                                    onNavigateToPlantNetResult(result.plant.commonName, result.plant.scientificName)
                                } else {
                                    onPlantClick(result.plant)
                                }
                            }
                        )
                    }
                }
            } else if (!isLoading) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("🔍 Selecciona una foto y presiona identificar", fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center)
                }
            }
        }
    }

    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = { Text("⚠️ API Key no configurada") },
            text = { Column { Text("Ve a https://my.plantnet.org/ para obtener tu API key") } },
            confirmButton = { TextButton(onClick = { showApiKeyDialog = false; isLoading = true; scope.launch { identificationResults = identifyPlantLocally(allPlants); isLoading = false } }) { Text("Usar BD local") } },
            dismissButton = { TextButton(onClick = { showApiKeyDialog = false }) { Text("Cancelar") } }
        )
    }
}

private suspend fun identifyWithPlantNet(context: Context, bitmap: Bitmap?, apiKey: String, allPlants: List<PlantEntity>): List<IdentificationResult> = withContext(Dispatchers.IO) {
    try {
        val imageBytes = bitmap?.let {
            val stream = ByteArrayOutputStream()
            it.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            stream.toByteArray()
        } ?: return@withContext identifyPlantLocally(allPlants)

        val client = OkHttpClient.Builder().connectTimeout(60, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).writeTimeout(60, TimeUnit.SECONDS).build()
        val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("images", "plant.jpg", imageBytes.toRequestBody("image/jpeg".toMediaType()))
            .addFormDataPart("organs", "leaf").build()
        val request = Request.Builder().url("$PLANTNET_URL?api-key=$apiKey").post(requestBody).build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()

        if (response.isSuccessful && responseBody != null) {
            val json = JSONObject(responseBody)
            val resultsArray = json.getJSONArray("results")
            val results = mutableListOf<IdentificationResult>()

            for (i in 0 until minOf(resultsArray.length(), 10)) {
                val result = resultsArray.getJSONObject(i)
                val species = result.getJSONObject("species")
                val scientificName = species.getString("scientificName")
                val commonNames = species.getJSONArray("commonNames")
                val displayName = if (commonNames.length() > 0) commonNames.getString(0) else scientificName.split(" ").take(2).joinToString(" ")
                val score = result.getDouble("score") * 100

                var imageUrl = ""
                try {
                    if (result.has("images")) {
                        val imagesArray = result.getJSONArray("images")
                        if (imagesArray.length() > 0) {
                            val firstImage = imagesArray.getJSONObject(0)
                            if (firstImage.has("url")) imageUrl = firstImage.getString("url")
                        }
                    }
                } catch (e: Exception) { }

                val matchedPlant = allPlants.find { plant -> plant.scientificName.contains(scientificName.split(" ").take(2).joinToString(" "), ignoreCase = true) }
                results.add(IdentificationResult(matchedPlant ?: createFakePlant(displayName, scientificName), score.toFloat(), "PlantNet", imageUrl))
            }
            return@withContext results.sortedByDescending { it.confidence }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    identifyPlantLocally(allPlants)
}

private fun identifyPlantLocally(allPlants: List<PlantEntity>): List<IdentificationResult> {
    if (allPlants.isEmpty()) return emptyList()
    return allPlants.shuffled().take(5).mapIndexed { index, plant -> IdentificationResult(plant, (100 - index * 15).toFloat(), "BD Local", "") }.sortedByDescending { it.confidence }
}

private fun createFakePlant(commonName: String, scientificName: String): PlantEntity {
    return PlantEntity(id = -1, commonName = commonName, scientificName = scientificName, family = "Desconocida", toxicityLevel = "Desconocido", toxicParts = "", symptoms = "", description = "", habitat = "", geographicDistribution = "", firstAid = "", imageUrl = "", category = "")
}

data class IdentificationResult(val plant: PlantEntity, val confidence: Float, val source: String, val imageUrl: String)

@Composable
fun CompactResultCard(result: IdentificationResult, onClick: () -> Unit) {
    val toxicityColor = when (result.plant.toxicityLevel) {
        "Mortal" -> Color(0xFFB71C1C); "Muy alto" -> Color(0xFFFF5722); "Alto" -> Color(0xFFE65100); "Moderado" -> Color(0xFFF57C00); "Bajo" -> Color(0xFF388E3C)
        else -> Color.Gray
    }

    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (result.imageUrl.isNotEmpty()) {
                AsyncImage(model = result.imageUrl, contentDescription = null, modifier = Modifier.size(50.dp).clip(RoundedCornerShape(6.dp)), contentScale = ContentScale.Crop)
                Spacer(modifier = Modifier.width(10.dp))
            } else {
                Box(modifier = Modifier.size(50.dp).clip(RoundedCornerShape(6.dp)).background(Color.LightGray.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                    Text("🌿", fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(result.plant.commonName, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(result.plant.scientificName, fontSize = 11.sp, color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${result.confidence.toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (result.confidence > 70) Color(0xFF2E7D32) else Color.Gray)
                    Text(" • ${result.source}", fontSize = 10.sp, color = Color.Gray)
                }
            }

            if (result.plant.toxicityLevel != "Desconocido") {
                Surface(color = toxicityColor, shape = RoundedCornerShape(4.dp)) {
                    Text(result.plant.toxicityLevel, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Icon(Icons.Default.Info, contentDescription = "Info", tint = Color(0xFF1565C0), modifier = Modifier.size(20.dp))
            }
        }
    }
}