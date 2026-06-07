package com.toxicplants.database.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
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
import com.toxicplants.database.ui.GeminiNameHelper
import kotlinx.coroutines.launch

private val RiskPurple = Color(0xFF4A148C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiskCalculatorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var species by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("No sé") }
    var amount by remember { mutableStateOf("") }
    var route by remember { mutableStateOf("Ingerida") }
    var age by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var timeElapsed by remember { mutableStateOf("") }
    var symptoms by remember { mutableStateOf("") }

    var loading by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<GeminiNameHelper.RiskResult?>(null) }
    var errorMsg by remember { mutableStateOf("") }

    fun dial(n: String) = context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$n")))

    fun evaluate() {
        if (species.isBlank()) {
            errorMsg = "Indica al menos la especie o sustancia."
            return
        }
        loading = true
        errorMsg = ""
        result = null
        scope.launch {
            val outcome = GeminiNameHelper.assessRisk(
                GeminiNameHelper.RiskInput(
                    species = species,
                    kind = kind,
                    amount = amount,
                    route = route,
                    ageYears = age,
                    weightKg = weight,
                    timeElapsed = timeElapsed,
                    symptoms = symptoms
                )
            )
            when (outcome) {
                is GeminiNameHelper.RiskOutcome.Success -> result = outcome.result
                is GeminiNameHelper.RiskOutcome.Error -> errorMsg = outcome.message
            }
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("⚠️ Calculadora de riesgo", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Orientativa con IA · no sustituye al médico", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = RiskPurple,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Aviso de seguridad
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(
                    "Esta herramienta es ORIENTATIVA. Ante una intoxicación real, llama YA al 112 o a Toxicología (91 562 04 20). No esperes a los síntomas.",
                    color = Color(0xFF212121),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(14.dp)
                )
            }

            // Botón 112 siempre accesible
            Button(
                onClick = { dial("112") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C))
            ) {
                Icon(Icons.Default.Call, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Llamar al 112", fontWeight = FontWeight.Bold)
            }

            OutlinedTextField(
                value = species, onValueChange = { species = it },
                label = { Text("Especie o sustancia *") },
                placeholder = { Text("Ej: Amanita phalloides, adelfa…") },
                modifier = Modifier.fillMaxWidth()
            )

            DropdownField(
                label = "Tipo",
                options = listOf("No sé", "Planta", "Seta", "Liquen", "Baya/fruto"),
                selected = kind, onSelected = { kind = it }
            )

            OutlinedTextField(
                value = amount, onValueChange = { amount = it },
                label = { Text("Cantidad / exposición") },
                placeholder = { Text("Ej: 2 hojas, un bocado, una seta…") },
                modifier = Modifier.fillMaxWidth()
            )

            DropdownField(
                label = "Vía de exposición",
                options = listOf("Ingerida", "Contacto con piel", "Contacto con ojos", "Inhalada"),
                selected = route, onSelected = { route = it }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = age, onValueChange = { age = it.filter { c -> c.isDigit() } },
                    label = { Text("Edad (años)") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = weight, onValueChange = { weight = it.filter { c -> c.isDigit() } },
                    label = { Text("Peso (kg)") },
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = timeElapsed, onValueChange = { timeElapsed = it },
                label = { Text("Tiempo desde la exposición") },
                placeholder = { Text("Ej: hace 30 min, 2 horas…") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = symptoms, onValueChange = { symptoms = it },
                label = { Text("Síntomas actuales") },
                placeholder = { Text("Ej: náuseas, mareo, ninguno…") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { evaluate() },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = RiskPurple)
            ) {
                if (loading) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Evaluando…")
                } else {
                    Text("Evaluar riesgo", fontWeight = FontWeight.Bold)
                }
            }

            if (errorMsg.isNotBlank()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(errorMsg, color = Color(0xFFB71C1C), modifier = Modifier.padding(12.dp))
                }
            }

            result?.let { r -> RiskResultCard(r, onCall112 = { dial("112") }, onCallTox = { dial("915620420") }) }

            Text(
                "La identificación y la evaluación por IA pueden equivocarse. Ante la mínima duda, contacta con los servicios de emergencia.",
                fontSize = 11.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun RiskResultCard(
    r: GeminiNameHelper.RiskResult,
    onCall112: () -> Unit,
    onCallTox: () -> Unit
) {
    val color = when (r.level) {
        "MORTAL" -> Color(0xFFB71C1C)
        "ALTO" -> Color(0xFFE65100)
        "MODERADO" -> Color(0xFFF9A825)
        "BAJO" -> Color(0xFF388E3C)
        else -> Color.Gray
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = color, shape = RoundedCornerShape(8.dp)) {
                    Text(
                        "Riesgo: ${r.level}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            if (r.summary.isNotBlank()) {
                Text(r.summary, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            }
            if (r.advice.isNotBlank()) {
                Text("Qué hacer:", fontWeight = FontWeight.Bold, color = color, fontSize = 13.sp)
                Text(r.advice, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 19.sp)
            }
            if (r.callEmergency) {
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onCall112,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C))
                ) {
                    Icon(Icons.Default.Call, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("LLAMAR AL 112 AHORA", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = onCallTox, modifier = Modifier.fillMaxWidth()) {
                    Text("Llamar a Toxicología (91 562 04 20)")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onSelected(option); expanded = false }
                )
            }
        }
    }
}
