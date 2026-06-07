// ui/screens/EmergencyScreen.kt
package com.toxicplants.database.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.ui.viewmodel.PlantViewModel

private val EmergencyRed = Color(0xFFB71C1C)
private val EmergencyRedDark = Color(0xFF7F0000)
private val CardPink = Color(0xFFFFEBEE)

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

    fun dial(number: String) {
        val clean = number.replace(" ", "")
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean")))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🚨 Emergencia toxicológica", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EmergencyRed,
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ───────── BOTÓN SOS 112 (lo primero, enorme) ─────────
            item { SosButton(onClick = { dial("112") }) }

            // ───────── Llamada directa a Toxicología ─────────
            item {
                ToxicologyButton(onClick = { dial("915620420") })
            }

            // ───────── Pasos inmediatos (siempre visibles) ─────────
            item { ImmediateStepsCard() }

            // ───────── Señales de alarma (llamar YA al 112) ─────────
            item { WarningSignsCard() }

            // ───────── Protocolos por tipo (desplegables) ─────────
            item {
                Text(
                    "📋 Protocolo según el tipo",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            item {
                ProtocolCard(
                    emoji = "🌿",
                    title = "Plantas tóxicas",
                    steps = listOf(
                        "Retira restos de planta de la boca.",
                        "Enjuaga la boca con agua (no tragar).",
                        "Si hay contacto con la piel/ojos: lava con agua abundante 15 min.",
                        "Guarda una muestra de la planta o una foto.",
                        "NO induzcas el vómito salvo orden médica.",
                        "Llama al 112 o a Toxicología con los datos a mano."
                    )
                )
            }
            item {
                ProtocolCard(
                    emoji = "🍄",
                    title = "Setas / hongos",
                    steps = listOf(
                        "Las intoxicaciones graves pueden tardar 6–24 h en dar síntomas: NO esperes.",
                        "Conserva restos de la seta cruda y cocinada, y del vómito si lo hubo.",
                        "Anota a qué hora se comió y cuándo empezaron los síntomas.",
                        "Si varias personas comieron, avisa a todas aunque estén bien.",
                        "NO induzcas el vómito por tu cuenta.",
                        "Acude a urgencias / llama al 112 SIEMPRE ante la duda."
                    )
                )
            }
            item {
                ProtocolCard(
                    emoji = "🪨",
                    title = "Líquenes",
                    steps = listOf(
                        "Retira restos de la boca y enjuaga con agua.",
                        "En caso de contacto cutáneo, lava la zona con agua y jabón.",
                        "Guarda una muestra para identificación.",
                        "Vigila reacciones alérgicas o digestivas.",
                        "Ante síntomas, llama al 112 o a Toxicología."
                    )
                )
            }

            // ───────── Qué datos dar por teléfono ─────────
            item { PhoneDataCard() }

            // ───────── Más teléfonos ─────────
            item {
                Text(
                    "📞 Teléfonos útiles",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            val numbers = listOf(
                Triple("🚨 Emergencias (UE)", "112", "Bomberos · Sanitario · Policía"),
                Triple("☠️ Inf. Toxicológica 24h", "91 562 04 20", "Inst. Nac. de Toxicología (público)"),
                Triple("🩺 Toxicología (médicos)", "91 411 26 76", "Solo personal sanitario"),
                Triple("👮 Guardia Civil", "062", "Emergencias rurales/montaña"),
                Triple("🚓 Policía Nacional", "091", "")
            )
            items(numbers) { (name, number, institution) ->
                EmergencyCallCard(name, number, institution) { dial(number) }
            }

            // ───────── Plantas más peligrosas ─────────
            if (mortalPlants.isNotEmpty()) {
                item {
                    Text(
                        "☠️ Plantas más peligrosas",
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

            // Aviso legal
            item { DisclaimerText() }
        }
    }

    plantToDelete?.let { plant ->
        AlertDialog(
            onDismissRequest = { plantToDelete = null },
            title = { Text("¿Eliminar planta?") },
            text = { Text("¿Estás seguro de eliminar ${plant.commonName}?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePlant(plant)
                    plantToDelete = null
                }) { Text("Eliminar", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { plantToDelete = null }) { Text("Cancelar") }
            }
        )
    }
}

// ════════════════════════ COMPONENTES ════════════════════════

@Composable
private fun SosButton(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = EmergencyRed),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 22.dp, horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                Modifier.size(56.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Call, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text("LLAMAR AL 112", color = Color.White, fontWeight = FontWeight.Black, fontSize = 26.sp)
                Text("Emergencias · pulsa para marcar", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun ToxicologyButton(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A237E))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Información Toxicológica 24h", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("91 562 04 20 · gratuito, todo el año", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
            }
            Icon(Icons.Default.Call, contentDescription = "Llamar", tint = Color.White)
        }
    }
}

@Composable
private fun ImmediateStepsCard() {
    Card(colors = CardDefaults.cardColors(containerColor = Color.Gray), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("⚡ Qué hacer AHORA", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = EmergencyRed)
            Spacer(Modifier.height(8.dp))
            listOf(
                "1. Mantén la calma y aleja a la persona del tóxico.",
                "2. NO induzcas el vómito sin indicación médica.",
                "3. NO des leche, sal, ni remedios caseros.",
                "4. Si está inconsciente, ponla de lado (posición lateral de seguridad).",
                "5. Llama al 112 o a Toxicología cuanto antes.",
                "6. Conserva la planta/seta o una foto para identificarla."
            ).forEach { Text(it, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(vertical = 2.dp)) }
        }
    }
}

@Composable
private fun WarningSignsCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE65100))
                Spacer(Modifier.width(8.dp))
                Text("Señales de alarma → 112 YA", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFE65100))
            }
            Spacer(Modifier.height(8.dp))
            listOf(
                "Dificultad para respirar o tragar",
                "Pérdida de consciencia o convulsiones",
                "Vómitos o diarrea intensos / con sangre",
                "Confusión, alucinaciones o agitación",
                "Latido irregular, dolor de pecho",
                "Hinchazón de labios, lengua o garganta"
            ).forEach {
                Text("• $it", fontSize = 14.sp, color = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.padding(vertical = 1.dp))
            }
        }
    }
}

@Composable
private fun ProtocolCard(emoji: String, title: String, steps: List<String>) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, fontSize = 26.sp)
                Spacer(Modifier.width(10.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Contraer" else "Expandir",
                    tint = EmergencyRed
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(top = 10.dp)) {
                    steps.forEachIndexed { i, step ->
                        Row(Modifier.padding(vertical = 3.dp)) {
                            Text("${i + 1}.", fontWeight = FontWeight.Bold, color = EmergencyRed, fontSize = 14.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(step, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhoneDataCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("🗒️ Datos que te pedirán por teléfono", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
            Spacer(Modifier.height(8.dp))
            listOf(
                "Qué se ha tomado/tocado (nombre o foto)",
                "Cantidad aproximada",
                "Hora de la exposición",
                "Edad y peso de la persona",
                "Síntomas actuales",
                "Enfermedades o medicación habitual"
            ).forEach {
                Text("• $it", fontSize = 14.sp, color = Color(0xFF2E7D32), modifier = Modifier.padding(vertical = 1.dp))
            }
        }
    }
}

@Composable
private fun DisclaimerText() {
    Text(
        "Esta información es orientativa y NO sustituye la atención médica profesional. " +
                "Ante cualquier sospecha de intoxicación, contacta con los servicios de emergencia.",
        fontSize = 11.sp,
        color = Color.Gray,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
    )
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
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardPink)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Bold, color = EmergencyRed)
                Text(number, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A237E))
                if (institution.isNotBlank()) {
                    Text(institution, fontSize = 12.sp, color = Color.Gray)
                }
            }
            Button(
                onClick = onCall,
                colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed)
            ) {
                Icon(Icons.Default.Phone, contentDescription = "Llamar")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Llamar")
            }
        }
    }
}
