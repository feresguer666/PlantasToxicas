package com.toxicplants.database.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
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
import com.google.ar.core.Session
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.ui.viewmodel.PlantViewModel

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

    val allPlants by viewModel.allPlants.observeAsState(emptyList())

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            // Verificar disponibilidad de ARCore
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

    fun simulateDetection() {
        isDetecting = true
        Handler(Looper.getMainLooper()).postDelayed({
            isDetecting = false
            if (allPlants.isNotEmpty()) {
                detectedPlant = allPlants.random()
                showOverlay = true
            }
        }, 2000)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF1A237E)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                }
                Spacer(Modifier.width(4.dp))
                Text("🎯 AR Detección", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))

                // Indicador de AR
                when (arAvailable) {
                    true -> Text("✅", fontSize = 18.sp)
                    false -> Text("❌", fontSize = 18.sp)
                    null -> Text("⏳", fontSize = 18.sp)
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (hasCameraPermission) {
                CameraPreview(modifier = Modifier.fillMaxSize())

                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(280.dp, 200.dp)
                            .border(width = 3.dp, color = if (isDetecting) Color.Yellow else Color.Cyan, shape = RoundedCornerShape(16.dp))
                    )
                    if (isDetecting) {
                        Text("🔍 Escaneando...", color = Color.Yellow, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 280.dp))
                    }
                }

                if (showOverlay && detectedPlant != null) {
                    PlantDetectionOverlay(
                        plant = detectedPlant!!,
                        onDismiss = { showOverlay = false },
                        onViewDetails = { showOverlay = false; onPlantClick(detectedPlant!!) }
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).background(Color.Black.copy(alpha = 0.6f)).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔍", fontSize = 24.sp)
                            Text("Detección", color = Color.White, fontSize = 12.sp)
                            Text(
                                when (arAvailable) {
                                    true -> "ARCore listo"
                                    false -> "No disponible"
                                    else -> "Verificando..."
                                },
                                color = when (arAvailable) {
                                    true -> Color.Green
                                    false -> Color.Red
                                    else -> Color.Yellow
                                },
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = { simulateDetection() },
                        enabled = !isDetecting,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BCD4)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isDetecting) "🔄 Detectando..." else "🔍 DETECTAR PLANTA", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(8.dp))

                    Text("⚠️ Usa el botón para detectar plantas", color = Color.Yellow, fontSize = 11.sp)
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
fun CameraPreview(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = modifier
    )
}

@Composable
fun PlantDetectionOverlay(plant: PlantEntity, onDismiss: () -> Unit, onViewDetails: () -> Unit) {
    val toxicityColor = when (plant.toxicityLevel) {
        "Mortal" -> Color(0xFFB71C1C)
        "Alto" -> Color(0xFFE65100)
        "Muy alto" -> Color(0xFFFF5722)
        "Moderado" -> Color(0xFFF57C00)
        "Bajo" -> Color(0xFF388E3C)
        else -> Color.Gray
    }

    val toxicityEmoji = when (plant.toxicityLevel) {
        "Mortal" -> "💀"
        "Muy alto" -> "☠️"
        "Alto" -> "⚠️"
        "Moderado" -> "⚡"
        "Bajo" -> "🟢"
        else -> "ℹ️"
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(toxicityEmoji, fontSize = 32.sp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(plant.commonName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(plant.scientificName, fontSize = 12.sp, color = Color.Gray, fontStyle = FontStyle.Italic)
                }
                Surface(color = toxicityColor, shape = RoundedCornerShape(8.dp)) {
                    Text(plant.toxicityLevel, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(12.dp))

            if (plant.toxicParts.isNotBlank()) {
                Text("⚠️ Partes tóxicas: ${plant.toxicParts}", fontSize = 13.sp, color = toxicityColor)
            }
            if (plant.symptoms.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text("🔴 Síntomas: ${plant.symptoms}", fontSize = 12.sp, color = Color(0xFF666666), maxLines = 2)
            }

            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Cerrar")
                }
                Button(onClick = onViewDetails, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = toxicityColor)) {
                    Text("Ver detalles")
                }
            }
        }
    }
}