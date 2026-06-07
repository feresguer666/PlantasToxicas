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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toxicplants.database.BuildConfig
import com.toxicplants.database.LichenEntity
import com.toxicplants.database.MushroomEntity
import com.toxicplants.database.ui.viewmodel.LichenViewModel
import com.toxicplants.database.ui.viewmodel.MushroomViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

private enum class NatureIdentifyMode(val label: String, val icon: String) {
    Mushrooms("Setas", "🍄"),
    Lichens("Líquenes", "🪨")
}

private data class NatureIdentificationResult(
    val type: String,
    val commonName: String,
    val scientificName: String,
    val confidence: Float,
    val source: String,
    val imageUrl: String,
    val toxicityLevel: String,
    val localMatch: Boolean,
    val warning: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoIdentifyFungiLichensScreen(
    mushroomViewModel: MushroomViewModel,
    lichenViewModel: LichenViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mushrooms by mushroomViewModel.allMushrooms.observeAsState(emptyList())
    val lichens by lichenViewModel.allLichens.observeAsState(emptyList())

    var mode by remember { mutableStateOf(NatureIdentifyMode.Mushrooms) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<NatureIdentificationResult>>(emptyList()) }
    // Mensaje de error visible en pantalla (diagnóstico sin necesidad de cable/Logcat).
    var errorMsg by remember { mutableStateOf("") }

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    selectedBitmap = BitmapFactory.decodeStream(stream)
                }
                results = emptyList()
                errorMsg = ""
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.values.any { it }) imagePickerLauncher.launch("image/*")
        else Toast.makeText(context, "Se necesita permiso para elegir imagen", Toast.LENGTH_SHORT).show()
    }

    fun selectImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            imagePickerLauncher.launch("image/*")
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
        }
    }

    fun identify() {
        val bitmap = selectedBitmap
        if (bitmap == null) {
            Toast.makeText(context, "Selecciona primero una imagen", Toast.LENGTH_SHORT).show()
            return
        }
        isLoading = true
        results = emptyList()
        errorMsg = ""
        scope.launch {
            val outcome = identifyFungiOrLichen(
                bitmap = bitmap,
                mode = mode,
                mushrooms = mushrooms,
                lichens = lichens
            )
            results = outcome.results
            isLoading = false
            errorMsg = outcome.message
            Toast.makeText(context, if (outcome.results.isNotEmpty()) "Identificación completada" else "No se obtuvieron resultados de IA", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("📷 Identificar setas y líquenes", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Orientativo con IA + catálogo local", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f))
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LazyRowLikeModeSelector(mode = mode, onModeChange = { mode = it; results = emptyList(); errorMsg = "" })

            Card(
                modifier = Modifier.fillMaxWidth().height(180.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    val bitmap = selectedBitmap
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Imagen seleccionada",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(mode.icon, fontSize = 42.sp)
                            Text("Selecciona una foto", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { selectImage() }, modifier = Modifier.weight(1f)) {
                    Text("🖼️ Galería")
                }
                Button(
                    onClick = { identify() },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (isLoading) CircularProgressIndicator(Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                    else Text("${mode.icon} Identificar")
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "La identificación por foto de setas y líquenes es orientativa y puede fallar. Nunca consumas una seta/liquen basándote solo en una app.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        lineHeight = 17.sp
                    )
                }
            }

            // Diagnóstico visual en pantalla para ver los errores o fallos en tiempo real
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (errorMsg.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Aviso / Estado de la IA:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(errorMsg, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 13.sp)
                        }
                    }
                }

                when {
                    isLoading -> {
                        Text("Analizando con IA de Groq (Llama 4 Scout Vision)…", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), modifier = Modifier.padding(top = 24.dp))
                    }
                    results.isNotEmpty() -> {
                        Text("Resultados (${results.size})", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.fillMaxWidth())
                        results.forEach { result -> NatureResultCard(result) }
                    }
                    else -> {
                        Text(
                            "Elige modo, selecciona foto y pulsa identificar",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
                        )
                    }
                }
            }
        }
    }

}

@Composable
private fun LazyRowLikeModeSelector(
    mode: NatureIdentifyMode,
    onModeChange: (NatureIdentifyMode) -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NatureIdentifyMode.entries.forEach { option ->
            FilterChip(
                selected = mode == option,
                onClick = { onModeChange(option) },
                label = { Text("${option.icon} ${option.label}") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun NatureResultCard(result: NatureIdentificationResult) {
    val color = when (result.toxicityLevel) {
        "Mortal" -> Color(0xFFB71C1C)
        "Muy alta", "Muy alto" -> Color(0xFFD84315)
        "Alta", "Alto" -> Color(0xFFE65100)
        "Moderada", "Moderado" -> Color(0xFFF9A825)
        "Baja", "Bajo" -> Color(0xFF388E3C)
        else -> Color.Gray
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            // Icono local (sin red) para evitar cualquier fallo de carga de imagen.
            Box(
                Modifier.size(58.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(if (result.type == "Seta") "🍄" else "🪨", fontSize = 28.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    result.commonName.ifBlank { result.scientificName },
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    result.scientificName,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontStyle = FontStyle.Italic,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${result.confidence.toInt()}%",
                        fontSize = 12.sp,
                        color = if (result.confidence >= 70f) Color(0xFF2E7D32) else Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    Text(" · ${result.source}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
                if (result.warning.isNotBlank()) {
                    Text(result.warning, fontSize = 11.sp, color = Color(0xFFE65100), maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
            Surface(color = color.copy(alpha = 0.14f), shape = RoundedCornerShape(8.dp)) {
                Text(
                    if (result.localMatch) result.toxicityLevel.ifBlank { "Local" } else (result.toxicityLevel.ifBlank { "?" }),
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    color = color,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Cambiamos el backend para que use el nuevo modelo Llama 4 Vision de Groq (evita la depreciación)
private val GROQ_API_KEY: String = BuildConfig.GROQ_API_KEY
private const val GROQ_MODEL_VISION = "meta-llama/llama-4-scout-17b-16e-instruct"  // Modelo Llama 4 activo con visión en 2026
private const val GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions"

/** Resultado del proceso de identificación junto con un mensaje de estado para la UI. */
private data class NatureIdentifyOutcome(
    val results: List<NatureIdentificationResult>,
    val message: String
)

private suspend fun identifyFungiOrLichen(
    bitmap: Bitmap,
    mode: NatureIdentifyMode,
    mushrooms: List<MushroomEntity>,
    lichens: List<LichenEntity>
): NatureIdentifyOutcome = withContext(Dispatchers.IO) {
    val apiKeyConfigured = GROQ_API_KEY.isNotBlank() && GROQ_API_KEY != "TU_API_KEY_AQUI"
    if (!apiKeyConfigured) {
        return@withContext NatureIdentifyOutcome(
            results = emptyList(),
            message = "⚠️ API key de Groq no configurada. Configúrala en local.properties como GROQ_API_KEY."
        )
    }

    val apiResult = runCatching { identifyWithGroqVision(bitmap, mode, mushrooms, lichens) }
    apiResult.fold(
        onSuccess = { outcome ->
            outcome
        },
        onFailure = { e ->
            NatureIdentifyOutcome(
                results = emptyList(),
                message = "Error en Groq Vision: ${e.message ?: e.javaClass.simpleName}."
            )
        }
    )
}

/**
 * Identifica setas o líquenes enviando la foto a Groq Vision (Llama 4 Vision)
 * y cruzando los resultados con el catálogo local de tu Room Database.
 */
private fun identifyWithGroqVision(
    bitmap: Bitmap,
    mode: NatureIdentifyMode,
    mushrooms: List<MushroomEntity>,
    lichens: List<LichenEntity>
): NatureIdentifyOutcome {
    // 1) Comprimir y codificar la imagen en base64.
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream) // Reducimos a 70 para agilizar la red en el móvil
    val base64Image = android.util.Base64.encodeToString(stream.toByteArray(), android.util.Base64.NO_WRAP)
    val dataUrl = "data:image/jpeg;base64,$base64Image"

    // 2) Prompt experto robusto en español que FUERZA al menos 1 resultado
    val kindEs = if (mode == NatureIdentifyMode.Mushrooms) "seta u hongo" else "liquen"
    val prompt = """  
        Eres un micólogo y liquenólogo experto. Analiza la foto de forma precisa e identifica qué $kindEs aparece.
        Debuelve EXCLUSIVAMENTE un objeto JSON válido (sin explicaciones extra, sin markdown, sin ```), con esta forma exacta:  
        {  
          "candidates": [  
            {  
              "scientificName": "Género especie",  
              "commonName": "nombre común en español",  
              "confidence": 1-100,  
              "toxicity": "Mortal|Muy alta|Alta|Moderada|Baja|Comestible|Desconocida",  
              "notes": "rasgo clave"  
            }  
          ]  
        }  
        Reglas obligatorias:  
        - "candidates" NO puede ser un array vacío. Debes proponer SIEMPRE al menos 1 candidato que represente tu mejor estimación botánica basada en la forma, color y rasgos visuales, incluso si tienes dudas.
        - "scientificName": Siempre el binomio completo "Género especie" (ej. "Amanita muscaria"), sin autor ni abreviaturas.
        - "commonName": nombre común en español; si no tiene, pon el nombre científico.  
        - "confidence" es la probabilidad estimada de coincidencia (del 1 al 100).  
        - "toxicity": una de las opciones indicadas.
    """.trimIndent()

    // 3) Estructura multimodal de Groq/OpenAI compatible
    val contentArray = JSONArray().apply {
        put(JSONObject().apply {
            put("type", "text")
            put("text", prompt)
        })
        put(JSONObject().apply {
            put("type", "image_url")
            put("image_url", JSONObject().apply {
                put("url", dataUrl)
            })
        })
    }

    val messagesArray = JSONArray().apply {
        put(JSONObject().apply {
            put("role", "user")
            put("content", contentArray)
        })
    }

    // Para Llama 4 Vision, no incluimos response_format con json_object ya que causa problemas en endpoints mutlimodales.
    val requestJson = JSONObject().apply {
        put("model", GROQ_MODEL_VISION)
        put("messages", messagesArray)
        put("temperature", 0.2)
    }

    val client = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())

    val request = Request.Builder()
        .url(GROQ_API_URL)
        .header("Authorization", "Bearer $GROQ_API_KEY")
        .post(requestBody)
        .build()

    val text: String
    client.newCall(request).execute().use { response ->
        text = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
            val reason = when (response.code) {
                401 -> "API key de Groq inválida o sin permisos (401). Verifica tu local.properties."
                429 -> "Límite 429 de peticiones en Groq. Espera un momento."
                503 -> "Servidor de Groq saturado (503). Reintenta en unos segundos."
                else -> "Error de Groq Vision (HTTP ${response.code}). Detalle: $text"
            }
            return NatureIdentifyOutcome(emptyList(), reason)
        }
    }

    if (text.isBlank()) return NatureIdentifyOutcome(emptyList(), "Respuesta vacía de Groq.")

    // 4) Extraer el texto generado por Groq
    val root = JSONObject(text)
    val choices = root.optJSONArray("choices") ?: return NatureIdentifyOutcome(emptyList(), "Formato de respuesta de Groq no reconocido. Respuesta: $text")
    val generated = choices.getJSONObject(0)
        .getJSONObject("message")
        .optString("content", "")
        .trim()

    if (generated.isBlank()) return NatureIdentifyOutcome(emptyList(), "La IA de Groq devolvió un bloque de texto vacío.")

    // Rebanamos el contenido entre la primera llave '{' y la última '}'
    // Esto garantiza que se extraiga el JSON puro, incluso si el modelo Vision responde con texto conversacional.
    var cleaned = generated
    if (cleaned.contains("{") && cleaned.contains("}")) {
        val start = cleaned.indexOf("{")
        val end = cleaned.lastIndexOf("}") + 1
        cleaned = cleaned.substring(start, end)
    }

    val parsed = JSONObject(cleaned)
    val arr = parsed.optJSONArray("candidates") ?: return NatureIdentifyOutcome(emptyList(), "El JSON no contiene la lista 'candidates'. Respuesta IA: $cleaned")

    val out = mutableListOf<NatureIdentificationResult>()
    for (i in 0 until minOf(arr.length(), 5)) {
        val item = arr.optJSONObject(i) ?: continue

        // Robustez de parseo para LLMs (acepta camelCase y snake_case)
        val scientific = (item.optString("scientificName", "").ifBlank { item.optString("scientific_name", "") }).trim()
        if (scientific.isBlank()) continue

        val commonAi = (item.optString("commonName", "").ifBlank { item.optString("common_name", "") }).trim()
        val confidence = item.optDouble("confidence", 0.0).toFloat().coerceIn(0f, 100f)
        val toxicityAi = (item.optString("toxicity", "").ifBlank { item.optString("toxicity_level", "Desconocida") }).trim().ifBlank { "Desconocida" }
        val notes = (item.optString("notes", "").ifBlank { item.optString("description", "") }).trim()

        val result = when (mode) {
            NatureIdentifyMode.Mushrooms -> {
                val match = mushrooms.findBestMushroom(scientific)
                NatureIdentificationResult(
                    type = "Seta",
                    commonName = match?.commonName ?: commonAi.ifBlank { scientific },
                    scientificName = match?.scientificName ?: scientific,
                    confidence = confidence,
                    source = "IA (Groq)",
                    imageUrl = match?.imageUrl.orEmpty(),
                    toxicityLevel = match?.toxicityLevel ?: toxicityAi,
                    localMatch = match != null,
                    warning = match?.let { "Coincide con catálogo local: ${it.syndrome}" }
                        ?: (notes.ifBlank { "Identificación de IA orientativa." } + " NUNCA consumas por foto.")
                )
            }
            NatureIdentifyMode.Lichens -> {
                val match = lichens.findBestLichen(scientific)
                NatureIdentificationResult(
                    type = "Liquen",
                    commonName = match?.commonName ?: commonAi.ifBlank { scientific },
                    scientificName = match?.scientificName ?: scientific,
                    confidence = confidence,
                    source = "IA (Groq)",
                    imageUrl = match?.imageUrl.orEmpty(),
                    toxicityLevel = match?.toxicityLevel ?: toxicityAi,
                    localMatch = match != null,
                    warning = match?.let { "Coincide con catálogo local: ${it.syndrome}" }
                        ?: notes.ifBlank { "Identificación de IA orientativa." }
                )
            }
        }
        out += result
    }

    val sorted = out.sortedByDescending { it.confidence }
    val msg = if (sorted.isEmpty()) "No se encontraron candidatos válidos en la respuesta de la IA."
    else "✅ Se identificó(aron) ${sorted.size} candidato(s)."
    return NatureIdentifyOutcome(sorted, msg)
}

private fun localFallback(mode: NatureIdentifyMode, mushrooms: List<MushroomEntity>, lichens: List<LichenEntity>): List<NatureIdentificationResult> {
    return when (mode) {
        NatureIdentifyMode.Mushrooms -> mushrooms.take(8).mapIndexed { idx, m ->
            NatureIdentificationResult("Seta", m.commonName, m.scientificName, (55 - idx * 3).toFloat(), "Catálogo local", m.imageUrl, m.toxicityLevel, true, "Sugerencia local orientativa (no es identificación de IA real).")
        }
        NatureIdentifyMode.Lichens -> lichens.take(8).mapIndexed { idx, l ->
            NatureIdentificationResult("Liquen", l.commonName, l.scientificName, (55 - idx * 3).toFloat(), "Catálogo local", l.imageUrl, l.toxicityLevel, true, "Sugerencia local orientativa (no es identificación de IA real).")
        }
    }
}

/** Normaliza un nombre científico: minúsculas, sin acentos, sin autor, solo "género especie". */
private fun normalizeBinomial(name: String): String {
    val noAccents = java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFD)
        .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    return noAccents
        .lowercase()
        .replace("[^a-z ]".toRegex(), " ")   // quita puntos, paréntesis, autores con símbolos
        .trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)                              // género + especie
        .joinToString(" ")
}

private fun List<MushroomEntity>.findBestMushroom(scientific: String): MushroomEntity? {
    val q = normalizeBinomial(scientific)
    if (q.isBlank()) return null
    val genus = q.substringBefore(" ")
    // 1) Binomio exacto (género + especie). Es lo único fiable.
    firstOrNull { normalizeBinomial(it.scientificName) == q }?.let { return it }
    // 2) Coincidencia parcial robusta (por si el catálogo tiene subespecie/variedad).
    firstOrNull { normalizeBinomial(it.scientificName).startsWith(q) || q.startsWith(normalizeBinomial(it.scientificName)) }?.let { return it }
    // 3) Como ÚLTIMO recurso, mismo género PERO solo si en el catálogo hay una única especie de ese género
    //    (evita devolver una especie equivocada del mismo género).
    val sameGenus = filter { normalizeBinomial(it.scientificName).substringBefore(" ") == genus }
    return if (sameGenus.size == 1) sameGenus.first() else null
}

private fun List<LichenEntity>.findBestLichen(scientific: String): LichenEntity? {
    val q = normalizeBinomial(scientific)
    if (q.isBlank()) return null
    val genus = q.substringBefore(" ")
    firstOrNull { normalizeBinomial(it.scientificName) == q }?.let { return it }
    firstOrNull { normalizeBinomial(it.scientificName).startsWith(q) || q.startsWith(normalizeBinomial(it.scientificName)) }?.let { return it }
    val sameGenus = filter { normalizeBinomial(it.scientificName).substringBefore(" ") == genus }
    return if (sameGenus.size == 1) sameGenus.first() else null
}
