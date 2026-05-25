package com.toxicplants.database.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.os.Handler        // ✅ AGREGAR
import android.os.Looper         // ✅ AGREGAR
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import com.toxicplants.database.ui.viewmodel.PlantViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationScreen(
    plantId: Int,
    plantName: String,
    viewModel: PlantViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var locationName by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isGettingLocation by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            getCurrentLocation(context) { lat, lng ->
                latitude = lat
                longitude = lng
                isGettingLocation = false
                if (lat != null && lng != null) {
                    statusMessage = "✅ Ubicación obtenida"
                    getAddressFromLocation(context, lat, lng) { address ->
                        locationName = address
                    }
                } else {
                    statusMessage = "❌ No se pudo obtener ubicación"
                }
            }
        } else {
            statusMessage = "❌ Permiso de ubicación denegado"
            isGettingLocation = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📍 Añadir Ubicación", fontWeight = FontWeight.Bold) },
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Info de la planta
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🌿 Planta:", fontWeight = FontWeight.Bold)
                    Text(plantName, fontSize = 18.sp, color = Color(0xFF2E7D32))
                }
            }

            // Estado de ubicación
            if (latitude != null && longitude != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("📍 Ubicación GPS", fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Latitud: $latitude", fontSize = 14.sp)
                        Text("Longitud: $longitude", fontSize = 14.sp)
                        if (locationName.isNotBlank()) {
                            Text("Dirección: $locationName", fontSize = 14.sp, color = Color.Gray)
                        }
                    }
                }
            }

            // Botón GPS
            Button(
                onClick = {
                    isGettingLocation = true
                    statusMessage = "🔄 Obteniendo ubicación GPS..."
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                enabled = !isGettingLocation
            ) {
                if (isGettingLocation) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Obteniendo...")
                } else {
                    Icon(Icons.Default.LocationOn, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("📍 Usar GPS actual")
                }
            }

            // Campo nombre del lugar
            OutlinedTextField(
                value = locationName,
                onValueChange = { locationName = it },
                label = { Text("Nombre del lugar") },
                placeholder = { Text("Ej: Jardín de mi casa, Parque local...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Campo notas
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notas (opcional)") },
                placeholder = { Text("Ej: Encontrada bajo un árbol grande...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            // Mensaje de estado
            if (statusMessage.isNotBlank()) {
                Text(statusMessage, color = Color.Gray, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón guardar
            Button(
                onClick = {
                    viewModel.updatePlantLocation(
                        plantId = plantId,
                        latitude = latitude,
                        longitude = longitude,
                        locationName = locationName.ifBlank { null },
                        notes = notes.ifBlank { null }
                    )
                    statusMessage = "✅ Ubicación guardada"
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                enabled = latitude != null && longitude != null
            ) {
                Text("💾 Guardar Ubicación", fontSize = 16.sp)
            }

            // Tip
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("💡 Tip:", fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                    Text("También puedes escribir la ubicación manualmente en el campo de arriba.", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun getCurrentLocation(context: Context, onResult: (Double?, Double?) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
        .setWaitForAccurateLocation(false)
        .setMinUpdateIntervalMillis(5000)
        .setMaxUpdates(1)
        .build()

    val locationCallback = object : com.google.android.gms.location.LocationCallback() {
        override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
            val location = locationResult.lastLocation
            if (location != null) {
                onResult(location.latitude, location.longitude)
            } else {
                onResult(null, null)
            }
            fusedLocationClient.removeLocationUpdates(this)
        }
    }

    try {
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    } catch (e: Exception) {
        onResult(null, null)
    }

    // Timeout 15 segundos
    android.os.Handler(Looper.getMainLooper()).postDelayed({
        onResult(null, null)
    }, 15000)
}

@Suppress("DEPRECATION")
private fun getAddressFromLocation(context: Context, latitude: Double, longitude: Double, onResult: (String) -> Unit) {
    try {
        val geocoder = Geocoder(context, Locale.getDefault())
        val addresses = geocoder.getFromLocation(latitude, longitude, 1)

        if (addresses != null && addresses.isNotEmpty()) {
            val address = addresses[0]
            val parts = mutableListOf<String>()

            if (address.thoroughfare != null) parts.add(address.thoroughfare)
            if (address.locality != null) parts.add(address.locality)
            if (address.adminArea != null) parts.add(address.adminArea)
            if (address.countryName != null) parts.add(address.countryName)

            onResult(parts.joinToString(", "))
        } else {
            onResult("Ubicación unknown")
        }
    } catch (e: Exception) {
        onResult("No se pudo obtener dirección")
    }
}