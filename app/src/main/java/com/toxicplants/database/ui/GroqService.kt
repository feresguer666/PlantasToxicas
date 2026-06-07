package com.toxicplants.database.ui

import android.content.Context
import android.util.Log
import com.toxicplants.database.PlantEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Servicio gratuito para interactuar con la API de Groq utilizando Llama 3.
 * Proporciona respuestas ultrarrápidas y mayor margen de rate limits que Gemini Free.
 */
object GroqService {
    private const val TAG = "GroqService"
    private const val GROQ_URL = "https://api.groq.com/openai/v1/chat/completions"

    // Modelos recomendados y completamente gratuitos en Groq
    const val MODEL_LLAMA_8B = "llama-3.1-8b-instant"       // Ultra rápido, ideal para resúmenes e interacciones rápidas
    const val MODEL_LLAMA_70B = "llama-3.3-70b-specdec"     // Mayor capacidad de razonamiento para preguntas complejas

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Realiza una consulta de chat a la API de Groq con reintentos automáticos (Exponential Backoff)
     * en caso de recibir un error 429 (Rate Limit).
     */
    suspend fun chatCompletion(
        prompt: String,
        systemInstruction: String = "Eres un asistente botánico experto en plantas tóxicas. Responde en español.",
        model: String = MODEL_LLAMA_8B,
        apiKey: String,
        maxRetries: Int = 3
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "tu_api_key_aqui") {
            return@withContext "❌ API Key de Groq no configurada. Por favor, añádela en los ajustes de tu aplicación."
        }

        var currentDelay = 1500L // Retraso inicial para reintento (1.5 segundos)
        val mediaType = "application/json; charset=utf-8".toMediaType()

        for (attempt in 1..maxRetries) {
            try {
                // Construir cuerpo JSON compatible con OpenAI / Groq
                val jsonBody = JSONObject().apply {
                    put("model", model)
                    put("temperature", 0.2) // Baja temperatura para respuestas consistentes y precisas

                    val messages = JSONArray().apply {
                        // Mensaje de sistema (personalidad e instrucciones)
                        put(JSONObject().apply {
                            put("role", "system")
                            put("content", systemInstruction)
                        })
                        // Mensaje de usuario
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", prompt)
                        })
                    }
                    put("messages", messages)
                }

                val requestBody = jsonBody.toString().toRequestBody(mediaType)
                val request = Request.Builder()
                    .url(GROQ_URL)
                    .header("Authorization", "Bearer $apiKey")
                    .header("Content-Type", "application/json")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: ""

                    if (response.code == 429) {
                        Log.w(TAG, "Rate limit (429) alcanzado en intento $attempt. Esperando ${currentDelay}ms...")
                        if (attempt == maxRetries) {
                            return@withContext "⚠️ Servidor de IA saturado (429). Por favor, espera un momento y vuelve a intentarlo."
                        }
                        delay(currentDelay)
                        currentDelay *= 2 // Retroceso exponencial
                        return@use // Continúa al siguiente ciclo para reintentar
                    }

                    if (!response.isSuccessful) {
                        return@withContext "❌ Error de servidor (${response.code}): ${response.message}"
                    }

                    val jsonResponse = JSONObject(responseBody)
                    val choices = jsonResponse.getJSONArray("choices")
                    if (choices.length() > 0) {
                        return@withContext choices.getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error de red en intento $attempt", e)
                if (attempt == maxRetries) {
                    return@withContext "❌ Error de red: No se pudo conectar con el servicio de IA. Revisa tu conexión a internet."
                }
                delay(currentDelay)
                currentDelay *= 2
            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado al consultar Groq", e)
                return@withContext "❌ Error inesperado: ${e.localizedMessage}"
            }
        }
        "No se pudo obtener respuesta de la IA."
    }

    /**
     * Helper para realizar una consulta de seguridad o primeros auxilios sobre una planta específica.
     */
    suspend fun getFirstAidAdvice(plant: PlantEntity, apiKey: String): String {
        val prompt = """
            Tengo una sospecha de exposición a la planta '${plant.commonName}' (nombre científico: '${plant.scientificName}').
            La base de datos indica:
            - Nivel de toxicidad: ${plant.toxicityLevel}
            - Partes tóxicas: ${plant.toxicParts}
            - Síntomas reportados: ${plant.symptoms}
            
            Por favor, bríndame de forma resumida e inmediata:
            1. Una evaluación rápida de peligrosidad.
            2. Qué primeros auxilios aplicar de inmediato (antes de ir al médico o veterinario).
            3. Qué información crítica debo dar a emergencias médicas o al centro toxicológico.
            
            Sé extremadamente directo, usa viñetas claras y resalta las alertas en negrita.
        """.trimIndent()

        val systemInstruction = """
            Eres un médico toxicólogo clínico y veterinario de emergencias. Tu prioridad es salvar vidas y evitar el pánico. 
            Proporciona consejos de primeros auxilios inmediatos y realistas. Agrega SIEMPRE un aviso claro de que el usuario debe 
            llamar al Instituto Nacional de Toxicología (91 562 04 20 en España) o a urgencias de inmediato.
        """.trimIndent()

        return chatCompletion(prompt, systemInstruction, MODEL_LLAMA_70B, apiKey)
    }
}
