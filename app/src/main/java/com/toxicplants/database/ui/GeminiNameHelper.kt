package com.toxicplants.database.ui

import android.graphics.Bitmap
import android.util.Base64
import com.toxicplants.database.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import android.util.Log

/**
 * Utilidad híbrida para sugerir nombres comunes, evaluar riesgos y realizar análisis de visión (Fotos)
 * usando Groq (Llama 3 / Llama 3.2 Vision) como alternativa ultra-rápida, gratuita y libre de errores 429.
 *
 * MANTIENE la firma de tus clases y añade funciones genéricas de chat y visión para que adaptes tus
 * otras pantallas locales (Asistente FloraSafe, Identificador de Setas por Foto, etc.) de forma sencillísima.
 */
object GeminiNameHelper {

    private const val TAG = "GeminiNameHelper"
    // Endpoint de Groq (OpenAI-compatible)
    private const val URL = "https://api.groq.com/openai/v1/chat/completions"

    // Modelos estables y activos en la capa gratuita de Groq (2026)
    private const val MODEL_FAST = "llama-3.1-8b-instant"            // Ultra-rápido para nombres y textos breves
    private const val MODEL_COMPLEX = "llama-3.3-70b-versatile"      // ¡CORREGIDO! Nombre correcto para evitar HTTP 400 en calculadora de riesgos
    private const val MODEL_VISION = "meta-llama/llama-4-scout-17b-16e-instruct"  // Modelo Llama 4 Vision activo en 2026

    // Usamos la API key de Groq que se inyecta desde local.properties
    private val API_KEY: String = BuildConfig.GROQ_API_KEY

    /* Resultado de la sugerencia. */
    sealed class Result {
        data class Success(val names: List<String>) : Result()
        data class Error(val message: String) : Result()
    }

    /* Resultado de la generación de un texto (descripción, síntomas, región...). */
    sealed class TextResult {
        data class Success(val text: String) : TextResult()
        data class Error(val message: String) : TextResult()
    }

    /* Tipo de campo de texto a generar. */
    enum class FieldType { DESCRIPTION, SYMPTOMS, REGION, HABITAT, TOXIC_PARTS, FIRST_AID }

    /**
     * Función genérica para que puedas usarla en el ASISTENTE FLORASAFE
     * Reemplaza tu llamada directa a Gemini por esta función y resolverás el error 429.
     */
    suspend fun chatCompletion(
        prompt: String,
        systemInstruction: String = "Eres FloraSafe, un asistente experto en seguridad botánica y toxicología. Responde de manera concisa y clara en español."
    ): String = withContext(Dispatchers.IO) {
        if (API_KEY.isBlank() || API_KEY == "TU_API_KEY_AQUI") {
            return@withContext "❌ Falta la API key de Groq (GROQ_API_KEY en local.properties)."
        }

        val body = JSONObject().apply {
            put("model", MODEL_FAST)
            put("temperature", 0.5)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemInstruction)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url(URL)
            .header("Authorization", "Bearer $API_KEY")
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext "❌ Error de servidor (${response.code})"
                }
                val responseJson = JSONObject(raw)
                responseJson.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim()
            }
        } catch (e: Exception) {
            "❌ Error de conexión: ${e.localizedMessage}"
        }
    }

    /**
     * Función con VISIÓN (Analizar fotos) para tu IDENTIFICADOR DE SETAS / HONGOS / PLANTAS
     * Envía la imagen codificada en Base64 al modelo Llama 3.2 Vision de Groq.
     */
    suspend fun analyzeImageWithVision(
        bitmap: Bitmap,
        prompt: String = "Identifica qué especie de hongo, seta o planta aparece en esta imagen. Describe si es comestible o tóxica, su nivel de riesgo y características clave. Responde en español de forma estructurada."
    ): String = withContext(Dispatchers.IO) {
        if (API_KEY.isBlank() || API_KEY == "TU_API_KEY_AQUI") {
            return@withContext "❌ Falta la API key de Groq (GROQ_API_KEY en local.properties)."
        }

        try {
            // 1. Codificar bitmap a Base64
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            val base64Image = Base64.encodeToString(byteArray, Base64.NO_WRAP)
            val dataUrl = "data:image/jpeg;base64,$base64Image"

            // 2. Crear estructura de mensajes multimodal para Groq Vision (OpenAI Vision compatible)
            val contentArray = JSONArray().apply {
                // Parte 1: El prompt de texto
                put(JSONObject().apply {
                    put("type", "text")
                    put("text", prompt)
                })
                // Parte 2: La imagen en base64
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

            val body = JSONObject().apply {
                put("model", MODEL_VISION)
                put("messages", messagesArray)
                put("max_tokens", 800)
                put("temperature", 0.2)
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(45, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .writeTimeout(45, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url(URL)
                .header("Authorization", "Bearer $API_KEY")
                .header("Content-Type", "application/json")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext "❌ Error en análisis de imagen (HTTP ${response.code})"
                }
                val responseJson = JSONObject(raw)
                responseJson.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en analyzeImageWithVision", e)
            "❌ No se pudo analizar la imagen: ${e.localizedMessage}"
        }
    }

    /**
     * Genera el texto de un campo concreto (descripción, síntomas de intoxicación o
     * distribución geográfica/región) para una especie usando la IA de Groq (Llama 3).
     */
    suspend fun generateField(
        type: FieldType,
        scientificName: String,
        commonName: String = ""
    ): TextResult = withContext(Dispatchers.IO) {
        if (API_KEY.isBlank() || API_KEY == "TU_API_KEY_AQUI") {
            return@withContext TextResult.Error("Falta la API key de Groq (GROQ_API_KEY en local.properties).")
        }
        if (scientificName.isBlank() && commonName.isBlank()) {
            return@withContext TextResult.Error("No hay nombre de especie para consultar.")
        }

        val species = "Especie: \"${scientificName.ifBlank { "(desconocida)" }}\" " +
                "(nombre común: \"${commonName.ifBlank { "(ninguno)" }}\")."
        val toxinHint = toxinHintForSpecies(scientificName, commonName)

        val instruction = when (type) {
            FieldType.DESCRIPTION -> """
                $species
                Escribe una DESCRIPCIÓN BOTÁNICA breve y clara en español (3-5 frases):
                porte (árbol/arbusto/hierba), hojas, flores, frutos, olor/látex si aplica y rasgos distintivos.
                Evita frases genéricas. Si no conoces la especie con fiabilidad, responde exactamente: "DESCONOCIDO".
                Texto plano, sin markdown y sin títulos.
            """.trimIndent()

            FieldType.SYMPTOMS -> """
                $species

                Redacta SOLO el campo "síntomas de intoxicación" para esta planta.

                Información orientativa local sobre toxina/grupo, si existe:
                ${toxinHint.ifBlank { "No hay toxina predefinida en la tabla local. Si no conoces con certeza la toxina real, NO menciones compuestos concretos." }}

                Reglas estrictas:
                - NO inventes compuestos.
                - NO digas "varios compuestos", "incluyendo alcaloides tropánicos y cianogénicos" ni listas genéricas de toxinas.
                - Nombra una toxina o grupo químico SOLO si es característico y conocido para esa especie/género.
                - Si hay una toxina conocida en la tabla local, úsala como prioridad.
                - Si no sabes la toxina exacta, describe solo el cuadro clínico sin nombrar compuestos.
                - NO des primeros auxilios ni recomendaciones médicas.
                - NO digas "buscar atención médica", "acudir a urgencias", "consultar a un profesional".
                - NO añadas coletillas como "es importante destacar que..." o "la toxicidad depende de...".
                - NO uses markdown ni títulos.

                Contenido esperado:
                - 2 a 4 frases clínicas en español.
                - Menciona mecanismo solo si es conocido.
                - Describe síntomas por sistemas: digestivos, neurológicos, cardíacos, respiratorios, dérmicos, oculares, hepáticos o renales según corresponda.
                - Incluye signos diferenciales solo si son propios de esa toxina/planta.

                Ejemplos de precisión:
                - Abrus precatorius: abrina/abrin, toxalbúmina proteica inhibidora de ribosomas; gastroenteritis intensa, vómitos, diarrea, deshidratación, fallo multiorgánico retardado.
                - Ricinus communis: ricina, toxalbúmina inhibidora de ribosomas.
                - Digitalis/Nerium/Thevetia: glucósidos cardíacos, arritmias, bradicardia/bloqueo AV.
                - Atropa/Datura/Hyoscyamus/Brugmansia: alcaloides tropánicos, síndrome anticolinérgico.
                - Prunus/Sorghum/mandioca amarga: glucósidos cianogénicos/cianuro.

                Si no conoces datos fiables, responde exactamente: "DESCONOCIDO".
            """.trimIndent()
            FieldType.REGION -> """
                $species
                Indica la DISTRIBUCIÓN GEOGRÁFICA / regiones donde se encuentra esta especie, en español,
                en una frase corta (ej: "Europa central y meridional; naturalizada en América del Norte").
                Si no la conoces, responde exactamente: "DESCONOCIDO". Texto plano, sin markdown.
            """.trimIndent()

            FieldType.HABITAT -> """
                $species
                Describe el HÁBITAT típico de esta especie en español: tipo de suelo, humedad, altitud aproximada,
                exposición solar, ambientes naturales o cultivados, bordes de camino/jardines/bosques/pastizales si aplica.
                Sé concreto y evita frases genéricas. Si no conoces datos fiables, responde exactamente: "DESCONOCIDO".
                Devuelve 1-3 frases, texto plano, sin markdown.
            """.trimIndent()

            FieldType.TOXIC_PARTS -> """
                $species
                Indica qué PARTES DE LA PLANTA son tóxicas o irritantes y, si se conoce, cuáles concentran más compuestos activos:
                hojas, semillas, bayas/frutos, raíces, bulbos, corteza, látex, savia, flores, toda la planta, etc.
                Si hay variación por madurez/estación/secado, menciónala. Si no conoces datos fiables, responde exactamente: "DESCONOCIDO".
                Devuelve una frase o lista breve en español, texto plano, sin markdown.
            """.trimIndent()

            FieldType.FIRST_AID -> """
                $species
                Redacta PRIMEROS AUXILIOS orientativos para exposición o ingestión accidental de esta planta.

                Reglas:
                - Prioriza seguridad: retirar restos de la boca/piel, lavar con agua, no provocar vómito salvo indicación profesional.
                - Recomienda llamar a Toxicología/112/urgencias si hay síntomas, ingestión relevante, niños, mascotas, o toxicidad alta.
                - Menciona conservar muestra/foto de la planta.
                - No des dosis de medicamentos ni tratamientos hospitalarios específicos.
                - Si no conoces datos fiables, da consejos generales prudentes y no inventes antídotos.

                Devuelve 3-5 frases cortas en español, texto plano, sin markdown.
            """.trimIndent()
        }

        // Construcción del JSON adaptado para Groq Chat Completions
        val body = JSONObject().apply {
            put("model", MODEL_FAST)
            put("temperature", 0.3)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", instruction)
                })
            })
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url(URL)
            .header("Authorization", "Bearer $API_KEY")
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        runCatching {
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@use TextResult.Error(httpErrorMessage(response.code, raw))
                }

                val responseJson = JSONObject(raw)
                val choices = responseJson.optJSONArray("choices")
                if (choices == null || choices.length() == 0) {
                    return@use TextResult.Error("La IA de Groq devolvió un formato vacío.")
                }

                val txt = choices.getJSONObject(0)
                    .getJSONObject("message")
                    .optString("content", "")
                    .trim()

                val finalTxt = if (type == FieldType.SYMPTOMS) cleanGeneratedSymptoms(txt) else txt

                when {
                    finalTxt.isBlank() -> TextResult.Error("La IA no devolvió respuesta útil.")
                    finalTxt.equals("DESCONOCIDO", true) -> TextResult.Error("La IA no conoce datos fiables de esta especie.")
                    else -> TextResult.Success(finalTxt)
                }
            }
        }.getOrElse { e ->
            TextResult.Error("No se pudo conectar con la IA (${e.message ?: "error"}).")
        }
    }


    private fun toxinHintForSpecies(scientificName: String, commonName: String): String {
        val n = (scientificName + " " + commonName).lowercase()

        return when {
            "abrus precatorius" in n || "abrus" in n || "regaliz americano" in n ->
                "Toxina característica: abrina/abrin, una toxalbúmina proteica inhibidora de ribosomas."

            "ricinus communis" in n || "ricino" in n ->
                "Toxina característica: ricina, toxalbúmina proteica inhibidora de ribosomas."

            "nerium oleander" in n || "adelfa" in n || "oleander" in n ->
                "Toxinas características: glucósidos cardíacos como oleandrina/neriina."

            "thevetia" in n ->
                "Toxinas características: glucósidos cardíacos como tevetina/peruvósido."

            "digitalis" in n || "dedalera" in n ->
                "Toxinas características: glucósidos cardíacos como digitoxina/digoxina."

            "atropa" in n || "belladonna" in n || "belladona" in n ->
                "Toxinas características: alcaloides tropánicos como atropina, hiosciamina y escopolamina."

            "datura" in n || "brugmansia" in n || "hyoscyamus" in n || "estramonio" in n ->
                "Toxinas características: alcaloides tropánicos; cuadro anticolinérgico."

            "taxus" in n || "tejo" in n ->
                "Toxinas características: taxinas; cardiotoxicidad con arritmias y colapso."

            "aconitum" in n || "acónito" in n ->
                "Toxina característica: aconitina y alcaloides diterpénicos; neuro/cardiotoxicidad."

            "conium maculatum" in n || "cicuta" in n ->
                "Toxinas características: alcaloides piperidínicos como coniína; bloqueo neuromuscular."

            "colchicum" in n || "colchico" in n ->
                "Toxina característica: colchicina; toxicidad digestiva, hematológica y multiorgánica."

            "dieffenbachia" in n || "zantedeschia" in n || "arum " in n || "philodendron" in n ->
                "Principio irritante característico: cristales de oxalato cálcico; irritación oral y edema local."

            "euphorbia" in n ->
                "Principios irritantes característicos: látex con ésteres diterpénicos; dermatitis e irritación ocular."

            "prunus" in n || "amygdalus" in n || "sorghum" in n || "yuca amarga" in n || "mandioca" in n ->
                "Toxinas posibles: glucósidos cianogénicos con liberación de cianuro, según especie y parte."

            "laburnum" in n || "cytisus" in n ->
                "Toxina característica: citisina y alcaloides quinolizidínicos."

            "veratrum" in n ->
                "Toxinas características: alcaloides esteroidales/veratrínicos; hipotensión, bradicardia y síntomas neurológicos."

            "phytolacca" in n ->
                "Principios tóxicos: saponinas y lectinas; irritación gastrointestinal marcada."

            else -> ""
        }
    }

    private fun cleanGeneratedSymptoms(text: String): String {
        if (text.isBlank()) return text

        val bannedFragments = listOf(
            "buscar atención médica",
            "busque atención médica",
            "acudir a urgencias",
            "acuda a urgencias",
            "consultar a un profesional",
            "consulte a un profesional",
            "es importante destacar",
            "es importante señalar",
            "la toxicidad depende",
            "puede estar relacionada con varios compuestos",
            "incluyendo alcaloides tropánicos y cianogénicos",
            "varios compuestos, incluyendo"
        )

        val sentences = text
            .replace("\n", " ")
            .split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val cleaned = sentences.filterNot { sentence ->
            val lower = sentence.lowercase()
            bannedFragments.any { it in lower }
        }.joinToString(" ").trim()

        return cleaned.ifBlank { text.trim() }
    }


    private fun httpErrorMessage(code: Int, body: String): String = when (code) {
        401 -> "API key de Groq no válida o no configurada."
        429 -> "IA ocupada (Límite 429). Por favor, reintenta en un momento."
        503 -> "Servidores de Groq saturados (503). Reinténtalo."
        else -> "Error de la IA de Groq (HTTP $code)."
    }

    /**
     * Pide a la IA una lista de nombres comunes (en español) de la especie indicada.
     */
    suspend fun suggestCommonNames(
        scientificName: String,
        knownCommonName: String = ""
    ): Result = withContext(Dispatchers.IO) {
        if (API_KEY.isBlank() || API_KEY == "TU_API_KEY_AQUI") {
            return@withContext Result.Error("Falta la API key de Groq (GROQ_API_KEY en local.properties).")
        }
        if (scientificName.isBlank() && knownCommonName.isBlank()) {
            return@withContext Result.Error("No hay nombre de especie para consultar.")
        }

        val prompt = """  
            Eres un botánico experto. Para la especie indicada, dame sus NOMBRES COMUNES en español  
            (populares, vernáculos y regionales de España y Latinoamérica).  
  
            Especie científica: "$scientificName"  
            Nombre común ya conocido: "$knownCommonName"  
  
            Devuelve EXCLUSIVAMENTE un JSON válido, sin markdown ni texto extra, con esta forma:  
            { "commonNames": ["nombre1", "nombre2", "..."] }  
  
            Reglas:  
            - Entre 0 y 8 nombres, sin duplicados.  
            - NO incluyas el nombre común ya conocido ni el nombre científico.  
            - Solo nombres reales y verificables; si no conoces ninguno extra, devuelve {"commonNames": []}.  
            - Nombres en minúscula salvo nombres propios.  
        """.trimIndent()

        val body = JSONObject().apply {
            put("model", MODEL_FAST)
            put("temperature", 0.3)
            put("response_format", JSONObject().put("type", "json_object"))
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url(URL)
            .header("Authorization", "Bearer $API_KEY")
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        runCatching {
            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@use Result.Error(httpErrorMessage(response.code, text))
                }

                val responseJson = JSONObject(text)
                val choices = responseJson.optJSONArray("choices")
                if (choices == null || choices.length() == 0) {
                    return@use Result.Error("La IA devolvió un formato vacío.")
                }

                val generated = choices.getJSONObject(0)
                    .getJSONObject("message")
                    .optString("content", "")
                    .trim()
                    .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

                if (generated.isBlank()) return@use Result.Error("La IA no devolvió respuesta.")

                val arr = JSONObject(generated).optJSONArray("commonNames")
                    ?: return@use Result.Error("Respuesta sin formato de nombres comunes.")

                val names = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    val n = arr.optString(i, "").trim()
                    if (n.isNotBlank()) names += n
                }
                Result.Success(names.distinct())
            }
        }.getOrElse { e ->
            Result.Error("No se pudo conectar con la IA (${e.message ?: "error"}).")
        }
    }

    /* Datos de entrada para la evaluación de riesgo. */
    data class RiskInput(
        val species: String,
        val kind: String,          // Planta / Seta / Liquen / No sé
        val amount: String,        // cantidad ingerida/contacto
        val route: String,         // Ingerida / Contacto piel / Ojos / Inhalada
        val ageYears: String,
        val weightKg: String,
        val timeElapsed: String,   // tiempo desde la exposición
        val symptoms: String
    )

    /* Resultado de la evaluación de riesgo. */
    data class RiskResult(
        val level: String,         // p.ej. "ALTO", "MODERADO", "BAJO", "DESCONOCIDO"
        val callEmergency: Boolean,
        val summary: String,
        val advice: String
    )

    sealed class RiskOutcome {
        data class Success(val result: RiskResult) : RiskOutcome()
        data class Error(val message: String) : RiskOutcome()
    }

    /**
     * Evalúa de forma ORIENTATIVA el riesgo de una posible intoxicación con IA (Llama 3 70B).
     */
    suspend fun assessRisk(input: RiskInput): RiskOutcome = withContext(Dispatchers.IO) {
        if (API_KEY.isBlank() || API_KEY == "TU_API_KEY_AQUI") {
            return@withContext RiskOutcome.Error("Falta la API key de Groq (GROQ_API_KEY en local.properties).")
        }

        val prompt = """  
            Eres un asistente toxicológico PRUDENTE. Evalúa de forma ORIENTATIVA el riesgo de una posible  
            intoxicación, SIN sustituir la atención médica. Ante cualquier duda, recomienda llamar al 112  
            o al Instituto Nacional de Toxicología (91 562 04 20).  
  
            Datos del caso:  
            - Especie/sustancia: "${input.species}" (tipo: ${input.kind})  
            - Cantidad/exposición: "${input.amount}"  
            - Vía: ${input.route}  
            - Edad: ${input.ageYears} años · Peso: ${input.weightKg} kg  
            - Tiempo desde la exposición: ${input.timeElapsed}  
            - Síntomas actuales: "${input.symptoms.ifBlank { "ninguno indicado" }}"  
  
            Devuelve EXCLUSIVAMENTE un JSON válido, sin markdown, con esta forma:  
            {  
              "level": "MORTAL|ALTO|MODERADO|BAJO|DESCONOCIDO",  
              "callEmergency": true|false,  
              "summary": "1-2 frases sobre la gravedad estimada en español",  
              "advice": "qué hacer ahora, en frases cortas, en español"  
            }  
  
            Reglas:  
            - Sé conservador: si hay incertidumbre o síntomas de alarma, level alto y callEmergency=true.  
            - Si la especie es muy tóxica/mortal (p.ej. Amanita phalloides, Digitalis, Nerium), callEmergency=true.  
            - En niños pequeños o ancianos, sube la precaución.  
            - NUNCA digas que es seguro consumir nada. Nunca des dosis de tratamiento.  
            - Si no conoces la especie, level "DESCONOCIDO" y recomienda contactar Toxicología.  
        """.trimIndent()

        val body = JSONObject().apply {
            put("model", MODEL_COMPLEX)
            put("temperature", 0.2)
            put("response_format", JSONObject().put("type", "json_object"))
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url(URL)
            .header("Authorization", "Bearer $API_KEY")
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        runCatching {
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@use RiskOutcome.Error(httpErrorMessage(response.code, raw))
                }

                val responseJson = JSONObject(raw)
                val choices = responseJson.optJSONArray("choices")
                if (choices == null || choices.length() == 0) {
                    return@use RiskOutcome.Error("La IA devolvió un formato vacío.")
                }

                val generated = choices.getJSONObject(0)
                    .getJSONObject("message")
                    .optString("content", "")
                    .trim()
                    .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

                if (generated.isBlank()) return@use RiskOutcome.Error("La IA no devolvió respuesta.")

                val obj = JSONObject(generated)
                RiskOutcome.Success(
                    RiskResult(
                        level = obj.optString("level", "DESCONOCIDO").uppercase(),
                        callEmergency = obj.optBoolean("callEmergency", true),
                        summary = obj.optString("summary", ""),
                        advice = obj.optString("advice", "")
                    )
                )
            }
        }.getOrElse { e ->
            RiskOutcome.Error("No se pudo conectar con la IA (${e.message ?: "error"}).")
        }
    }
}
