// ui/screens/AssistantScreen.kt
package com.toxicplants.database.ui.screens

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toxicplants.database.BuildConfig
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

// Cambiamos el backend para que use Groq y sus modelos libres de 429
private const val GROQ_URL = "https://api.groq.com/openai/v1/chat/completions"
private const val MODEL_TEXT = "llama-3.1-8b-instant"            // Modelo de texto rápido
private const val MODEL_VISION = "meta-llama/llama-4-scout-17b-16e-instruct"  // Modelo Llama 4 Vision activo

private val ASSISTANT_API_KEY: String = BuildConfig.GROQ_API_KEY

/** Un mensaje del chat. */
private data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val image: Bitmap? = null
)

private val SYSTEM_PROMPT = """  
Eres "FloraSafe", un asistente experto en plantas tóxicas, setas (micología), líquenes  
y toxicología, integrado en una app de seguridad. Respondes en español, de forma clara,  
útil y con prudencia médica.  
  
Puedes ayudar con:  
- Identificar plantas, árboles, setas y líquenes a partir de descripciones (color de flor/fruto,  
  forma de hoja, porte del árbol/arbusto, hábitat, época) o de una FOTO si el usuario la adjunta.  
- Relacionar SÍNTOMAS con posibles causantes (plantas/setas tóxicas) de forma orientativa.  
- Explicar nivel de toxicidad, partes tóxicas, principios activos y primeros auxilios.  
- Diferenciar especies comestibles de sus confusiones tóxicas.  
  
Reglas IMPORTANTES:  
- Si hay sospecha de intoxicación con síntomas graves (dificultad para respirar, convulsiones,  
  pérdida de consciencia...), indica SIEMPRE llamar al 112 o a Toxicología (91 562 04 20) cuanto antes.  
- Nunca recomiendes consumir una seta/planta identificada solo por foto o descripción.  
- Sé honesto con la incertidumbre: ofrece varias hipótesis y los rasgos para distinguirlas.  
- Respuestas concretas y bien estructuradas (usa listas y negritas con asteriscos si ayuda).  
- Si te falta información para identificar, haz 1-3 preguntas clave (color, tamaño, hábitat, olor...).  
- No des información para fabricar venenos o causar daño.  
""".trimIndent()

private val QUICK_PROMPTS = listOf(
    "🌸 Identificar por color de flor y fruto",
    "🌳 Identificar un árbol por su forma",
    "🍄 ¿Qué seta puede ser?",
    "🩺 Tengo síntomas, ¿qué pudo causarlos?",
    "🐶 ¿Es tóxica para mi mascota?",
    "🌿 Diferencia entre comestible y tóxica"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(onBack: () -> Unit, onOpenGlossary: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var messages by remember {
        mutableStateOf(
            listOf(
                ChatMessage(
                    "¡Hola! 👋 Soy **FloraSafe**, tu asistente de plantas tóxicas, setas y líquenes.\n\n" +
                            "Puedo ayudarte a:\n" +
                            "• 🌸 Identificar por color de flor/fruto, forma de hoja o árbol\n" +
                            "• 🍄 Sugerir qué seta o liquen puede ser\n" +
                            "• 🩺 Relacionar síntomas con un posible causante\n" +
                            "• 📷 Analizar una foto (pulsa el icono de imagen)\n\n" +
                            "¿En qué te ayudo?",
                    isUser = false
                )
            )
        )
    }
    var input by remember { mutableStateOf("") }
    var attached by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.openInputStream(it)?.use { s ->
                    attached = BitmapFactory.decodeStream(s)
                }
            }
        }
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        if (perms.values.any { it }) picker.launch("image/*")
        else Toast.makeText(context, "Se necesita permiso para elegir imagen", Toast.LENGTH_SHORT).show()
    }
    fun pickImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) picker.launch("image/*")
        else permLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
    }

    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank() && attached == null) return
        val userMsg = ChatMessage(trimmed.ifBlank { "(imagen adjunta)" }, isUser = true, image = attached)
        val history = messages + userMsg
        messages = history
        val imageToSend = attached
        input = ""
        attached = null
        isLoading = true
        scope.launch {
            val reply = askAssistant(history, imageToSend)
            messages = messages + ChatMessage(reply, isUser = false)
            isLoading = false
        }
    }

    // Auto-scroll al último mensaje
    LaunchedEffect(messages.size, isLoading) {
        val target = messages.size // + posible indicador de carga
        if (target > 0) listState.animateScrollToItem(target)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("🤖 Asistente FloraSafe", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("IA · orientativo, no médico", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenGlossary) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = "Glosario botánico"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            InputBar(
                input = input,
                onInputChange = { input = it },
                attached = attached,
                onClearAttachment = { attached = null },
                onPickImage = { pickImage() },
                onSend = { send(input) },
                enabled = !isLoading
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages.size) { idx -> MessageBubble(messages[idx]) }
                if (isLoading) {
                    item { TypingIndicator() }
                }
                // Sugerencias rápidas solo al inicio (1 mensaje del bot, sin escribir aún)
                if (messages.size == 1 && !isLoading) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                            Text("Sugerencias:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            QUICK_PROMPTS.forEach { q ->
                                SuggestionChip(text = q, onClick = { send(q) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(msg: ChatMessage) {
    // MODO OSCURO DE LUXE COMPLETO: Usamos colores semánticos del tema de Compose en lugar de hardcodeados
    val bubbleColor = if (msg.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (msg.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp, topEnd = 16.dp,
                        bottomStart = if (msg.isUser) 16.dp else 4.dp,
                        bottomEnd = if (msg.isUser) 4.dp else 16.dp
                    )
                )
                .background(bubbleColor)
                .padding(12.dp)
        ) {
            msg.image?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Imagen adjunta",
                    modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(8.dp))
            }
            // Render simple de **negrita** -> texto normal limpiando asteriscos.
            Text(formatMarkdownLite(msg.text), color = textColor, fontSize = 14.sp)
        }
    }
}

@Composable
private fun TypingIndicator() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Pensando…", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun SuggestionChip(text: String, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(text, color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InputBar(
    input: String,
    onInputChange: (String) -> Unit,
    attached: Bitmap?,
    onClearAttachment: () -> Unit,
    onPickImage: () -> Unit,
    onSend: () -> Unit,
    enabled: Boolean
) {
    Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.navigationBarsPadding().padding(8.dp)) {
            // Vista previa de la imagen adjunta
            attached?.let { bmp ->
                Row(Modifier.padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Adjunto",
                            modifier = Modifier.size(54.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = onClearAttachment,
                            modifier = Modifier.size(22.dp).align(Alignment.TopEnd)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Quitar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("Imagen lista para analizar", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPickImage) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Adjuntar foto", tint = MaterialTheme.colorScheme.primary)
                }
                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Escribe o describe lo que ves…") },
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions.Default,
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(Modifier.width(4.dp))
                FilledIconButton(
                    onClick = onSend,
                    enabled = enabled && (input.isNotBlank() || attached != null),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar")
                }
            }
        }
    }
}

/** Limpia marcadores markdown básicos para mostrar texto legible. */
private fun formatMarkdownLite(text: String): String {
    return text
        .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
        .replace(Regex("(?m)^\\s*[*-]\\s+"), "• ")
        .replace("```", "")
        .trim()
}

// ════════════════════ LLAMADA A GROQ (EVITA EL ERROR 429) ════════════════════

private suspend fun askAssistant(history: List<ChatMessage>, image: Bitmap?): String =
    withContext(Dispatchers.IO) {
        if (ASSISTANT_API_KEY.isBlank() || ASSISTANT_API_KEY == "TU_API_KEY_AQUI") {
            return@withContext "⚠️ Falta la API key de Groq. Configúrala en local.properties (GROQ_API_KEY) para usar el asistente."
        }
        runCatching { callGroq(history, image) }.getOrElse { e ->
            "No pude conectar con la IA de Groq (${e.message ?: "error"}). Revisa tu conexión e inténtalo de nuevo."
        }
    }

private fun callGroq(history: List<ChatMessage>, image: Bitmap?): String {
    val messagesJson = JSONArray()

    // 1) System Prompt para moldear la personalidad de FloraSafe
    messagesJson.put(JSONObject().apply {
        put("role", "system")
        put("content", SYSTEM_PROMPT)
    })

    // 2) Historial de conversación (Formato estándar OpenAI Chat compatible con Groq)
    val recent = history.takeLast(12)
    recent.forEachIndexed { index, msg ->
        val role = if (msg.isUser) "user" else "assistant"
        val messageObj = JSONObject().apply {
            put("role", role)

            // Si el mensaje es de usuario, es el último del chat y tiene imagen, usamos formato multimodal (Vision)
            if (msg.isUser && index == recent.lastIndex && image != null) {
                val contentArray = JSONArray().apply {
                    // Texto descriptivo del usuario
                    put(JSONObject().apply {
                        put("type", "text")
                        put("text", msg.text)
                    })

                    // Imagen en Base64 para el modelo de Visión de Groq
                    val stream = ByteArrayOutputStream()
                    image.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                    val b64 = android.util.Base64.encodeToString(stream.toByteArray(), android.util.Base64.NO_WRAP)
                    val dataUrl = "data:image/jpeg;base64,$b64"

                    put(JSONObject().apply {
                        put("type", "image_url")
                        put("image_url", JSONObject().apply {
                            put("url", dataUrl)
                        })
                    })
                }
                put("content", contentArray)
            } else {
                // Mensaje regular solo con texto
                put("content", msg.text)
            }
        }
        messagesJson.put(messageObj)
    }

    // Seleccionamos dinámicamente el modelo: visión para imágenes, texto hiper-rápido si solo chatea por texto.
    val modelName = if (image != null) "meta-llama/llama-4-scout-17b-16e-instruct" else MODEL_TEXT

    val body = JSONObject().apply {
        put("model", modelName)
        put("messages", messagesJson)
        put("temperature", 0.4)
    }

    val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val request = Request.Builder()
        .url(GROQ_URL)
        .header("Authorization", "Bearer $ASSISTANT_API_KEY")
        .post(body.toString().toRequestBody("application/json".toMediaType()))
        .build()

    client.newCall(request).execute().use { response ->
        val text = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
            return when (response.code) {
                401 -> "⚠️ API key de Groq no válida o no configurada."
                429 -> "La IA está ocupada (Límite 429). Espera un momento y reinténtalo."
                503 -> "La IA de Groq está saturada (503). Reinténtalo en unos segundos."
                else -> "Error de la IA de Groq (HTTP ${response.code})."
            }
        }

        val root = JSONObject(text)
        val choices = root.optJSONArray("choices")
        if (choices == null || choices.length() == 0) {
            return "No se recibió respuesta válida del asistente."
        }

        return choices.getJSONObject(0)
            .getJSONObject("message")
            .optString("content", "")
            .trim()
    }
}
