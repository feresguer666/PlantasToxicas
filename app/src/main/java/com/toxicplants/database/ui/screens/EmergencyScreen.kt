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
import androidx.compose.material.icons.filled.Navigation
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
    onBack: () -> Unit,
    onNavigateToEmergencyMap: () -> Unit = {}
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

            // ───────── RADAR DE URGENCIAS Y ANTÍDOTOS (Hospitales / Veterinarios 24h) ─────────
            item {
                EmergencyMapNavButton(onClick = onNavigateToEmergencyMap)
            }

            // ───────── Llamada directa a Toxicología ─────────
            item {
                ToxicologyButton(onClick = { dial("915620420") })
            }

            // ───────── ASISTENTE SOS INTERACTIVO (Algoritmo de Primeros Auxilios) ─────────
            item { SosAssistantCard() }

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

@Composable
private fun SosAssistantCard() {
    var step1 by remember { mutableStateOf<String?>(null) }
    var step2 by remember { mutableStateOf<String?>(null) }
    var step3 by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B263B)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🩺", fontSize = 28.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Asistente SOS de Emergencia", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFFFF8A80))
                    Text("Algoritmo interactivo de primeros auxilios", fontSize = 12.sp, color = Color.LightGray)
                }
                if (step1 != null) {
                    TextButton(onClick = { step1 = null; step2 = null; step3 = null }) {
                        Text("🔄 Reiniciar", color = Color(0xFF80CBC4), fontSize = 12.sp)
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

            Text("1. ¿Qué tipo de exposición o incidente ha ocurrido?", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SosOptionChip(
                    label = "👄 Ingestión / Tragar",
                    selected = step1 == "ingestion",
                    onClick = { step1 = "ingestion"; if (step2 == null) step2 = "conscious" },
                    modifier = Modifier.weight(1f)
                )
                SosOptionChip(
                    label = "👁️ Piel / Ojos",
                    selected = step1 == "skin",
                    onClick = { step1 = "skin"; step2 = "conscious" },
                    modifier = Modifier.weight(1f)
                )
                SosOptionChip(
                    label = "🍄 Seta / Hongo",
                    selected = step1 == "mushroom",
                    onClick = { step1 = "mushroom"; if (step2 == null) step2 = "conscious" },
                    modifier = Modifier.weight(1f)
                )
            }

            if (step1 != null) {
                if (step1 != "skin") {
                    Text("2. ¿La persona está consciente y respirando bien?", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SosOptionChip(
                            label = "✅ Sí, consciente",
                            selected = step2 == "conscious",
                            onClick = { step2 = "conscious" },
                            modifier = Modifier.weight(1f)
                        )
                        SosOptionChip(
                            label = "❌ Inconsciente / Ahogo",
                            selected = step2 == "unconscious",
                            onClick = { step2 = "unconscious" },
                            modifier = Modifier.weight(1f),
                            color = Color(0xFFC62828)
                        )
                    }
                }

                Text("3. ¿Cuánto tiempo hace que sucedió?", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SosOptionChip(
                        label = "⏱️ < 1 hora",
                        selected = step3 == "less1h",
                        onClick = { step3 = "less1h" },
                        modifier = Modifier.weight(1f)
                    )
                    SosOptionChip(
                        label = "⏱️ > 1 hora",
                        selected = step3 == "more1h",
                        onClick = { step3 = "more1h" },
                        modifier = Modifier.weight(1f)
                    )
                    SosOptionChip(
                        label = "❓ No se sabe",
                        selected = step3 == "unknown",
                        onClick = { step3 = "unknown" },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(4.dp))
                SosAlgorithmResult(step1 = step1!!, step2 = step2 ?: "conscious", step3 = step3 ?: "unknown")
            } else {
                Surface(color = Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "👆 Selecciona una opción arriba para generar el protocolo de actuación inmediata paso a paso.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SosOptionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF1976D2)
) {
    val bgColor = if (selected) color else Color.White.copy(alpha = 0.08f)
    val textColor = if (selected) Color.White else Color.LightGray
    val borderCol = if (selected) Color.White else Color.Transparent

    Surface(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderCol)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
            Text(label, color = textColor, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun SosAlgorithmResult(step1: String, step2: String, step3: String) {
    val title: String
    val color: Color
    val instructions: List<String>
    val warnings: List<String>

    if (step2 == "unconscious") {
        title = "🚨 URGENCIA VITAL EXTREMA (112 YA)"
        color = Color(0xFFD32F2F)
        instructions = listOf(
            "PULSA EL BOTÓN ROJO SUPERIOR Y LLAMA AL 112 AHORA MISMO.",
            "Coloca a la persona de lado en Posición Lateral de Seguridad (para evitar que se ahogue si vomita).",
            "Afloja ropa ajustada alrededor del cuello o pecho.",
            "Si no respira o no tiene pulso, inicia maniobras de RCP de inmediato (30 compresiones torácicas / 2 ventilaciones)."
        )
        warnings = listOf(
            "NO INTENTES DAR DE BEBER AGUA NI NINGÚN LÍQUIDO.",
            "NO PROVOQUES EL VÓMITO BAJO NINGÚN CONCEPTO.",
            "NO METAS NADA EN LA BOCA SI TIENE CONVULSIONES."
        )
    } else when (step1) {
        "ingestion" -> {
            title = "🌿 PROTOCOLO POR INGESTIÓN DE PLANTA"
            color = Color(0xFFE65100)
            instructions = listOf(
                "Retira con los dedos limpios o un paño cualquier resto de planta o fruto de la boca.",
                "Enjuaga la boca con agua abundante varias veces y haz que ESCUPA todo el agua (que no la trague).",
                "Guarda un trozo de la planta, hoja, fruto o hazle varias fotos claras con el móvil para que el médico o toxicólogo la identifique.",
                "Llama de inmediato al 91 562 04 20 (Información Toxicológica 24h) o al 112 indicando nombre de planta o descripción y hora exacta."
            )
            warnings = listOf(
                "NO INDUZCAS EL VÓMITO: si la planta contiene oxalatos corrosivos (ej. Dieffenbachia, Adelfa o Arum), vomitar quemará el esófago por segunda vez.",
                "NO DES LECHE, ACEITE NI SAL: la leche facilita la absorción intestinal de toxinas liposolubles.",
                "NO ESPERES a ver si aparecen síntomas graves; consulta siempre con Toxicología."
            )
        }
        "skin" -> {
            title = "👁️ PROTOCOLO POR CONTACTO PIEL / OJOS"
            color = Color(0xFF0097A7)
            instructions = listOf(
                "EN OJOS: Lava con un chorro continuo de agua templada o suero fisiológico durante mínimo 15-20 minutos manteniendo los párpados abiertos.",
                "EN PIEL: Quita la ropa manchada o expuesta. Lava la piel con abundante agua y jabón neutro.",
                "Lava suavemente sin frotar con fuerza (frotar puede extender resinas o jugos tóxicos como el látex de Euphorbia o hiedras).",
                "EVITA LA LUZ SOLAR: muchas plantas causan fitofotodermatitis graves (ampollas severas) al reaccionar el jugo con el sol (ej. Higuera del diablo, Heracleum o Ruda)."
            )
            warnings = listOf(
                "NO FROTES NI RASQUES LOS OJOS NI LA PIEL.",
                "NO APLIQUES POMADAS, ALCOHOL NI REMEDIOS CASEROS SIN ORDEN MÉDICA.",
                "Si hay alteración de la visión, ampollas extensas o ardor insoportable, acude de inmediato a urgencias."
            )
        }
        "mushroom" -> {
            title = "🍄 PROTOCOLO DE URGENCIA POR SETAS"
            color = Color(0xFF8E24AA)
            instructions = listOf(
                "PELIGRO LATENTE: Las setas más letales (Amanita phalloides, Lepiota) tardan entre 6 y 24 horas en dar síntomas graves. NO ESPERES a ver cómo evoluciona.",
                "CONSERVA MUESTRAS: Guarda en el frigorífico (envueltas en papel o paño, NUNCA en plástico) restos de la seta cruda, cocinada o del vómito/diarrea. Son esenciales para el hospital.",
                "AVISA A TODOS: Si varias personas comieron del mismo plato, DEBEN IR A URGENCIAS TODAS, incluso aquellas que se sientan perfectamente bien.",
                "Si hay vómitos o diarrea intensos, mantén la hidratación con suero de reanimación oral a pequeños sorbos."
            )
            warnings = listOf(
                "NO TOMES ANTIDIARRÉICOS NI MEDICAMENTOS PARA CORTAR EL VÓMITO SIN RECETA MÉDICA (retrasan la eliminación de la toxina).",
                "NO TE FÍES SI LOS SÍNTOMAS PARECEN DESAPARECER TRAS UNAS HORAS (fase de falsa mejoría previa al fallo hepático).",
                "Llama al 112 o acude directamente a urgencias hospitalarias con las muestras."
            )
        }
        else -> {
            title = "📋 PROTOCOLO GENERAL"
            color = Color.Gray
            instructions = emptyList()
            warnings = emptyList()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f)),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, color)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.Black, color = Color.White, fontSize = 15.sp)

            Text("⚡ QUÉ HACER INMEDIATAMENTE:", fontWeight = FontWeight.Bold, color = Color(0xFFA5D6A7), fontSize = 13.sp)
            instructions.forEachIndexed { idx, ins ->
                Row(Modifier.padding(vertical = 1.dp)) {
                    Text("${idx + 1}.", fontWeight = FontWeight.Bold, color = Color(0xFFA5D6A7), fontSize = 13.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(ins, color = Color.White, fontSize = 13.sp)
                }
            }

            if (warnings.isNotEmpty()) {
                HorizontalDivider(color = Color.White.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 4.dp))
                Text("🚫 QUÉ NO HACER JAMÁS:", fontWeight = FontWeight.Bold, color = Color(0xFFFF8A80), fontSize = 13.sp)
                warnings.forEach { warn ->
                    Row(Modifier.padding(vertical = 1.dp)) {
                        Text("•", fontWeight = FontWeight.Bold, color = Color(0xFFFF8A80), fontSize = 13.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(warn, color = Color(0xFFFFEBEE), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (step3 == "more1h") {
                Surface(color = Color(0xFFD32F2F).copy(alpha = 0.3f), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Text(
                        "⏳ AVISO DE TIEMPO (> 1 hora): Como ha pasado más de 1 hora, la absorción del tóxico ya puede estar en marcha. Es vital contactar urgentemente con un médico o toxicología sin demora.",
                        color = Color(0xFFFFCDD2),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmergencyMapNavButton(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF006064)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🏥", fontSize = 34.sp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Radar Urgencias 24h y Guía Antídotos", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text("Navega con 1 clic al Hospital o Veterinario más cercano y consulta dosis clínicas", color = Color(0xFFE0F7FA), fontSize = 12.sp, lineHeight = 15.sp)
            }
            Icon(Icons.Default.Navigation, contentDescription = "Abrir", tint = Color.Yellow, modifier = Modifier.size(24.dp))
        }
    }
}
