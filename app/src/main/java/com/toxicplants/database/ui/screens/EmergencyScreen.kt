// ui/screens/EmergencyScreen.kt
package com.toxicplants.database.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.ui.viewmodel.PlantViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyScreen(
    viewModel: PlantViewModel,
    onPlantClick: (PlantEntity) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val mortalPlants by viewModel.mortalPlantsData.collectAsState()
    var plantToDelete by remember { mutableStateOf<PlantEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "🚨 Emergencias",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFB71C1C),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFEBEE)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "⚡ Protocolo de Emergencia",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFFB71C1C)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        listOf(
                            "1. Mantenga la calma",
                            "2. NO induzca el vómito sin indicación médica",
                            "3. Llame al 112 o al Centro de Toxicología",
                            "4. Tenga a mano: nombre de la planta, cantidad ingerida y tiempo",
                            "5. Si tiene la planta, llévela al hospital para identificación",
                            "6. Monitorice respiración y pulso"
                        ).forEach { step ->
                            Text(
                                step,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 2.dp),
                                color = Color.Black
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    "📞 Centros de Toxicología",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            val emergencyNumbers = listOf(
                Triple("🚨 Emergencias Generales", "112", "Toda España"),
                Triple("☠️ Toxicología España", "91 562 04 20", "Instituto Nacional de Toxicología"),
                Triple("🌎 Poison Control USA", "1-800-222-1222", "American Association"),
                Triple("🏥 Toxicología México", "800 112 3200", "IMSS"),
                Triple("🇨🇱 Chile", "2 2635 3800", "Hospital Clínico UC"),
                Triple("🇦🇷 Argentina", "0800 333 0160", "CIAT"),
                Triple("🇨🇴 Colombia", "01 8000 916 012", "Liga Contra el Cáncer")
            )

            items(emergencyNumbers) { (name, number, institution) ->
                EmergencyCallCard(
                    name = name,
                    number = number,
                    institution = institution,
                    onCall = {
                        val intent = Intent(
                            Intent.ACTION_DIAL,
                            Uri.parse("tel:$number")
                        )
                        context.startActivity(intent)
                    }
                )
            }

            item {
                Text(
                    "☠️ Plantas Más Peligrosas",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(mortalPlants) { plant ->
                PlantCard(
                    plant = plant,
                    onClick = { onPlantClick(plant) },
                    onDeleteClick = { plantToDelete = plant }
                )
            }
        }
    }

    // Diálogo de confirmación para eliminar
    plantToDelete?.let { plant ->
        AlertDialog(
            onDismissRequest = { plantToDelete = null },
            title = { Text("¿Eliminar planta?") },
            text = { Text("¿Estás seguro de eliminar ${plant.commonName}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePlant(plant)
                        plantToDelete = null
                    }
                ) {
                    Text("Eliminar", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { plantToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun EmergencyCallCard(
    name: String,
    number: String,
    institution: String,
    onCall: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    name,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFB71C1C)
                )
                Text(
                    number,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A237E)
                )
                Text(
                    institution,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Button(
                onClick = onCall,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFB71C1C)
                )
            ) {
                Icon(Icons.Default.Phone, contentDescription = "Llamar")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Llamar")
            }
        }
    }
}