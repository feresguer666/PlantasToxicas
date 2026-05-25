package com.toxicplants.database.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Descargar imagenes", fontWeight = FontWeight.Bold) },
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
                "Descarga de imagenes",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Descarga las fotos de todas las plantas para verlas sin conexion.",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (!isDownloading && !isFinished) {
                Button(
                    onClick = {
                        isDownloading = true
                        scope.launch {
                            val plants = viewModel.getAllPlantsForDownload()
                            total = plants.size

                            val result = ImageDownloader.downloadAll(
                                context = context,
                                plants = plants,
                                onProgress = { progress ->
                                    current = progress.current
                                    currentPlant = progress.plantName
                                    successCount = progress.success
                                    failedCount = progress.failed
                                }
                            )

                            successCount = result.first
                            failedCount = result.second
                            isDownloading = false
                            isFinished = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("Iniciar descarga", fontSize = 18.sp)
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
                }
            }

            if (isFinished) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8F5E9)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Descarga completada",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Descargadas: $successCount", color = Color(0xFF388E3C))
                        Text("No encontradas: $failedCount", color = Color(0xFFE65100))
                        Text("Total: $total")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32)
                    )
                ) {
                    Text("Volver")
                }
            }
        }
    }
}
