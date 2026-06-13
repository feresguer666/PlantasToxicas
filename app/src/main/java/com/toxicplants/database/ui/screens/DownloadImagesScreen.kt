package com.toxicplants.database.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.toxicplants.database.ui.ImageDownloader
import com.toxicplants.database.ui.LocalImageCache
import com.toxicplants.database.ui.viewmodel.PlantViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadImagesScreen(
    viewModel: PlantViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isDownloading by remember { mutableStateOf(false) }
    var isFinished by remember { mutableStateOf(false) }
    var currentPlant by remember { mutableStateOf("") }
    var current by remember { mutableIntStateOf(0) }
    var total by remember { mutableIntStateOf(0) }
    var successCount by remember { mutableIntStateOf(0) }
    var failedCount by remember { mutableIntStateOf(0) }
    var replaceExisting by remember { mutableStateOf(false) }
    var failedListCount by remember { mutableIntStateOf(ImageDownloader.failedPlantCount(context)) }
    var retryPendingCount by remember { mutableIntStateOf(0) }
    var lastRunWasRetry by remember { mutableStateOf(false) }

    fun refreshRetryStats() {
        scope.launch {
            val allPlants = viewModel.getAllPlantsForDownload()
            val failedIds = ImageDownloader.getFailedPlantIds(context)
            failedListCount = failedIds.size
            retryPendingCount = allPlants.count { plant ->
                plant.id in failedIds || !LocalImageCache.hasLocalImage(context, plant.id)
            }
        }
    }

    fun resetProgress() {
        current = 0
        total = 0
        successCount = 0
        failedCount = 0
        currentPlant = ""
    }

    fun startDownload(onlyFailed: Boolean) {
        isDownloading = true
        isFinished = false
        lastRunWasRetry = onlyFailed
        resetProgress()

        scope.launch {
            val allPlants = viewModel.getAllPlantsForDownload()
            val plants = if (onlyFailed) {
                val failedIds = ImageDownloader.getFailedPlantIds(context)
                allPlants.filter { plant ->
                    plant.id in failedIds || !LocalImageCache.hasLocalImage(context, plant.id)
                }
            } else {
                allPlants
            }

            total = plants.size
            if (plants.isEmpty()) {
                currentPlant = if (onlyFailed) "No hay imágenes fallidas pendientes" else "No hay plantas para descargar"
                isDownloading = false
                isFinished = true
                refreshRetryStats()
                return@launch
            }

            val result = ImageDownloader.downloadAll(
                context = context,
                plants = plants,
                overwriteExisting = if (onlyFailed) false else replaceExisting,
                onProgress = { progress ->
                    current = progress.current
                    currentPlant = progress.plantName
                    successCount = progress.success
                    failedCount = progress.failed
                }
            )

            successCount = result.first
            failedCount = result.second
            refreshRetryStats()
            isDownloading = false
            isFinished = true
        }
    }

    LaunchedEffect(Unit) { refreshRetryStats() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Descargar imágenes", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isDownloading) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2E7D32),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                "Descarga de imágenes",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Descarga las fotos de todas las plantas para verlas sin conexión.",
                fontSize = 14.sp,
                color = Color.Gray
            )

            if (retryPendingCount > 0 && !isDownloading && !isFinished) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF3E0)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "⚠️ $retryPendingCount plantas sin imagen / fallidas",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100)
                        )
                        Text(
                            "Incluye fallidas registradas y plantas que aún no tienen foto local. Puedes reintentar solo esas sin recorrer todo el catálogo.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { startDownload(onlyFailed = true) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
                            ) {
                                Text("Reintentar", fontSize = 13.sp)
                            }
                            if (failedListCount > 0) {
                                OutlinedButton(
                                    onClick = {
                                        ImageDownloader.clearFailedPlants(context)
                                        refreshRetryStats()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Limpiar fallidas ($failedListCount)", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (!isDownloading && !isFinished) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Reemplazar imágenes existentes",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                "Útil si tienes fotos antiguas repetidas por nombre común. Si no se encuentra una foto mejor, se conserva la anterior.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = replaceExisting,
                            onCheckedChange = { replaceExisting = it }
                        )
                    }
                }

                Button(
                    onClick = { startDownload(onlyFailed = false) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        if (replaceExisting) "Redescargar imágenes" else "Iniciar descarga",
                        fontSize = 18.sp
                    )
                }
            }

            if (isDownloading) {
                CircularProgressIndicator(color = Color(0xFF2E7D32))

                if (total > 0) {
                    LinearProgressIndicator(
                        progress = { current.toFloat() / total.toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF2E7D32)
                    )

                    Text(
                        "$current / $total",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        currentPlant,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Text("OK: $successCount", color = Color(0xFF388E3C))
                        Text("Fallidas: $failedCount", color = Color(0xFFE65100))
                    }
                } else if (currentPlant.isNotBlank()) {
                    Text(currentPlant, fontSize = 14.sp, color = Color.Gray)
                }
            }

            if (isFinished) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            if (lastRunWasRetry) "Reintento completado" else "Descarga completada",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Descargadas: $successCount", color = Color(0xFF388E3C))
                        Text("No encontradas: $failedCount", color = Color(0xFFE65100))
                        Text("Total procesadas: $total")
                        if (retryPendingCount > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Quedan $retryPendingCount sin imagen / fallidas",
                                color = Color(0xFFE65100),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (retryPendingCount > 0) {
                    Button(
                        onClick = { startDownload(onlyFailed = true) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reintentar sin imagen / fallidas ($retryPendingCount)")
                    }
                }

                OutlinedButton(
                    onClick = {
                        isFinished = false
                        resetProgress()
                        refreshRetryStats()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Nueva descarga")
                }

                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Volver")
                }
            }
        }
    }
}
