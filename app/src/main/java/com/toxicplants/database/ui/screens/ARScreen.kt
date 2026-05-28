package com.toxicplants.database.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.ar.core.ArCoreApk
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.ui.viewmodel.PlantViewModel
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ARScreen(
    viewModel: PlantViewModel,
    onPlantClick: (PlantEntity) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }

    var arAvailable by remember { mutableStateOf<Boolean?>(null) }
    var isDetecting by remember { mutableStateOf(false) }
    var detectedPlant by remember { mutableStateOf<PlantEntity?>(null) }
    var showOverlay by remember { mutableStateOf(false) }
    var detectionMessage by remember { mutableStateOf("Pulsa el botón para detectar") }

    // ImageCapture como estado
    val imageCaptureRef = remember { mutableStateOf<ImageCapture?>(null) }

    val allPlants by viewModel.allPlants.observeAsState(emptyList())

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            try {
                val availability = ArCoreApk.getInstance().checkAvailability(context)
                arAvailable = availability.isSupported
            } catch (e: Exception) {
                arAvailable = false
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            try {
                val availability = ArCoreApk.getInstance().checkAvailability(context)
                arAvailable = availability.isSupported
            } catch (e: Exception) {
                arAvailable = false
            }
        }
    }

    fun captureAndDetect() {
        val imageCapture = imageCaptureRef.value
        if (imageCapture == null) {
            detectionMessage = "❌ Cámara no disponible"
            return
        }
        if (isDetecting) return

        isDetecting = true
        detectionMessage = "📸 Capturando imagen..."

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    val buffer = imageProxy.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                    // Rotar si es necesario
                    if (imageProxy.imageInfo.rotationDegrees != 0) {
                        val matrix = Matrix()
                        matrix.postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    }

                    imageProxy.close()

                    // Detectar con Pl@ntNet
                    detectPlantFromBitmap(bitmap, allPlants) { resultPlant, message ->
                        detectedPlant = resultPlant
                        detectionMessage = message
                        isDetecting = false
                        if (resultPlant != null) {
                            showOverlay = true
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    detectionMessage = "❌ Error al capturar"
                    isDetecting = false
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF1A237E)) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                }
                Spacer(Modifier.width(4.dp))
                Text("🎯 AR Detección", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                when (arAvailable) {
                    true -> Text("✅", fontSize = 18.sp)
                    false -> Text("❌", fontSize = 18.sp)
                    null -> Text("⏳", fontSize = 18.sp)
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (hasCameraPermission) {
                CameraPreviewWithCapture(
                    modifier = Modifier.fillMaxSize(),
                    onImageCaptureReady = { imageCaptureRef.value = it }
                )

                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.size(280.dp, 200.dp).border(width = 3.dp, color = if (isDetecting) Color.Yellow else Color.Cyan, shape = RoundedCornerShape(16.dp)))

                    if (isDetecting) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 280.dp)) {
                            CircularProgressIndicator(color = Color.Yellow, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Analizando...", color = Color.Yellow, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (showOverlay && detectedPlant != null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                        PlantDetectionOverlay(
                            plant = detectedPlant!!,
                            onDismiss = { showOverlay = false; detectionMessage = "Pulsa el botón para detectar" },
                            onViewDetails = { showOverlay = false; onPlantClick(detectedPlant!!) }
                        )
                    }
                }

                Column(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).background(Color.Black.copy(alpha = 0.7f)).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📚 ${allPlants.size} plantas en el catálogo", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(detectionMessage, color = when {
                        detectionMessage.contains("✅") -> Color.Green
                        detectionMessage.contains("❌") -> Color.Red
                        isDetecting -> Color.Yellow
                        else -> Color.White
                    }, fontSize = 14.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = { captureAndDetect() },
                        enabled = !isDetecting,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BCD4)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (isDetecting) "⏳ Analizando..." else "📷 CAPTURAR Y DETECTAR", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(8.dp))
                    Text("📷 Apunta a una planta y presiona el botón", color = Color.Yellow, fontSize = 11.sp, textAlign = TextAlign.Center)
                }
            } else {
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF121212)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Text("📷", fontSize = 64.sp)
                        Spacer(Modifier.height(16.dp))
                        Text("Permiso de cámara necesario", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Para usar AR necesitas permitir el acceso a la cámara", color = Color.Gray, fontSize = 14.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A237E))) {
                            Text("Permitir cámara")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CameraPreviewWithCapture(
    modifier: Modifier = Modifier,
    onImageCaptureReady: (ImageCapture) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }

                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
                    onImageCaptureReady(imageCapture)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = modifier
    )
}

private fun detectPlantFromBitmap(bitmap: Bitmap, allPlants: List<PlantEntity>, callback: (PlantEntity?, String) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val apiKey = "2b10xAb3tJgmvhuWpj3DVvcFO"  // ⚠️ Cambia esto por tu API key de Pl@ntNet

            val url = URL("https://my-api.plantnet.org/v2/identify/all?api-key=$apiKey")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true

            val boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW"
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

            val os = connection.outputStream
            os.write("--$boundary\r\n".toByteArray())
            os.write("Content-Disposition: form-data; name=\"images\"; filename=\"plant.jpg\"\r\n".toByteArray())
            os.write("Content-Type: image/jpeg\r\n\r\n".toByteArray())

            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
            os.write(baos.toByteArray())
            os.write("\r\n".toByteArray())
            os.write("--$boundary--\r\n".toByteArray())
            os.flush()
            os.close()

            val response = connection.responseCode

            if (response == HttpURLConnection.HTTP_OK) {
                val reader = java.io.BufferedReader(java.io.InputStreamReader(connection.inputStream))
                val responseText = reader.readText()
                reader.close()

                val json = org.json.JSONObject(responseText)
                val results = json.getJSONArray("results")

                if (results.length() > 0) {
                    val topResult = results.getJSONObject(0)
                    val species = topResult.getJSONObject("species")
                    val scientificName = species.getString("scientificName")
                    val commonNames = species.getJSONArray("commonNames")

                    val commonName = if (commonNames.length() > 0) commonNames.getString(0) else scientificName.split(" ").first()

                    withContext(Dispatchers.Main) {
                        val matchedPlant = allPlants.find {
                            it.scientificName.contains(scientificName.split(" ").take(2).joinToString(" "), ignoreCase = true) ||
                                    it.commonName.contains(commonName, ignoreCase = true)
                        }

                        if (matchedPlant != null) {
                            callback(matchedPlant, "✅ Detectado: ${matchedPlant.commonName}")
                        } else {
                            val tempPlant = PlantEntity(
                                id = -1,
                                commonName = commonName,
                                scientificName = scientificName,
                                family = "",
                                toxicityLevel = "Desconocido",
                                toxicParts = "Por identificar",
                                symptoms = "Consultar fuentes oficiales",
                                description = "Detectado mediante Pl@ntNet",
                                habitat = "",
                                geographicDistribution = "",
                                firstAid = "",
                                imageUrl = "",
                                isFavorite = false,
                                category = "Detectado",
                                latitude = null,
                                longitude = null,
                                locationName = null,
                                foundDate = null,
                                notes = null
                            )
                            callback(tempPlant, "✅ Detectado: $commonName (sin datos de toxicidad)")
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        callback(null, "❌ No se pudo identificar la planta")
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    callback(null, "❌ Error en la API de Pl@ntNet (código: $response)")
                }
            }

            connection.disconnect()

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                callback(null, "❌ Error: ${e.message}")
            }
        }
    }
}

@Composable
fun PlantDetectionOverlay(plant: PlantEntity, onDismiss: () -> Unit, onViewDetails: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val toxicityColor = when (plant.toxicityLevel) {
        "Mortal" -> colors.error
        "Alto" -> Color(0xFFE65100)
        "Muy alto" -> Color(0xFFFF5722)
        "Moderado" -> Color(0xFFF57C00)
        "Bajo" -> colors.primary
        else -> colors.onSurfaceVariant
    }

    val toxicityEmoji = when (plant.toxicityLevel) { "Mortal" -> "💀"; "Muy alto" -> "☠️"; "Alto" -> "⚠️"; "Moderado" -> "⚡"; "Bajo" -> "🟢"; else -> "❓" }

    Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 12.dp), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(toxicityEmoji, fontSize = 40.sp)
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(plant.commonName, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(plant.scientificName, fontSize = 14.sp, color = Color.Gray, fontStyle = FontStyle.Italic)
                }
                Surface(color = toxicityColor, shape = RoundedCornerShape(10.dp)) {
                    Text(plant.toxicityLevel, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(16.dp))
            if (plant.toxicParts.isNotBlank() && plant.toxicParts != "Por identificar") {
                Row(verticalAlignment = Alignment.Top) {
                    Text("⚠️", fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Column { Text("Partes tóxicas:", fontSize = 11.sp, color = Color.Gray); Text(plant.toxicParts, fontSize = 13.sp, color = toxicityColor, fontWeight = FontWeight.Medium) }
                }
            }
            if (plant.symptoms.isNotBlank() && plant.symptoms != "Consultar fuentes oficiales") {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Text("🔴", fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Column { Text("Síntomas:", fontSize = 11.sp, color = Color.Gray); Text(plant.symptoms, fontSize = 13.sp, color = Color.DarkGray, maxLines = 3) }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cerrar") }
                Button(onClick = onViewDetails, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = toxicityColor)) { Text("Ver detalles") }
            }
        }
    }
}