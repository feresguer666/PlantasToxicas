package com.toxicplants.database.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class GoogleAIFilter(
    val icon: String,
    val title: String,
    val prompt: String
)

private val googleAIFilters = listOf(
    GoogleAIFilter(
        icon = "🩺",
        title = "Síntomas tóxicos",
        prompt = "síntomas tóxicos intoxicación en humanos animales primeros auxilios"
    ),
    GoogleAIFilter(
        icon = "📖",
        title = "Descripción",
        prompt = "descripción botánica identificación hojas flores frutos"
    ),
    GoogleAIFilter(
        icon = "🍃",
        title = "Partes tóxicas",
        prompt = "partes tóxicas hojas semillas frutos raíz savia toxicidad"
    ),
    GoogleAIFilter(
        icon = "🔬",
        title = "Componentes tóxicos",
        prompt = "componentes tóxicos alcaloides glucósidos toxinas principios activos"
    ),
    GoogleAIFilter(
        icon = "🌍",
        title = "Hábitat",
        prompt = "hábitat distribución clima dónde crece"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleAISearchScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }

    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    fun buildGoogleUrl(text: String): String {
        return "https://www.google.com/search?hl=es&gl=ES&q=${Uri.encode(text)}"
    }

    fun searchWithExtra(extra: String = "") {
        val species = query.trim()
        if (species.isBlank()) {
            errorText = "Escribe primero una especie o nombre de planta."
            return
        }
        errorText = ""
        val finalQuery = if (extra.isBlank()) {
            "$species planta tóxica información"
        } else {
            "$species $extra"
        }
        openUrl(buildGoogleUrl(finalQuery))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("🔎 Google IA", fontWeight = FontWeight.Bold)
                        Text(
                            "Consulta por especie y filtra la información",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { searchWithExtra() }) {
                        Icon(Icons.Filled.OpenInBrowser, contentDescription = "Buscar en Google", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D47A1),
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF06121F), Color(0xFF0D1B2A), Color(0xFF102A43))
                    )
                )
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF172A3A)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = {
                            query = it
                            if (it.isNotBlank()) errorText = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        minLines = 2,
                        label = { Text("Especie o nombre de planta") },
                        placeholder = { Text("Ej: Nerium oleander, Ricinus communis, adelfa...") },
                        trailingIcon = {
                            IconButton(onClick = { searchWithExtra() }) {
                                Icon(Icons.Filled.Search, contentDescription = "Buscar")
                            }
                        }
                    )

                    if (errorText.isNotBlank()) {
                        Text(errorText, color = Color(0xFFFFAB91), fontSize = 13.sp)
                    }

                    Button(
                        onClick = { searchWithExtra() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🔎 Buscar especie")
                    }
                }
            }

            Text(
                "Filtros de respuesta",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Text(
                "Escribe una especie arriba y elige qué información quieres consultar.",
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 13.sp
            )

            googleAIFilters.forEach { filter ->
                GoogleAIFilterCard(
                    icon = filter.icon,
                    title = filter.title,
                    prompt = filter.prompt,
                    onClick = { searchWithExtra(filter.prompt) }
                )
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun GoogleAIFilterCard(
    icon: String,
    title: String,
    prompt: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A5F)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(icon, fontSize = 26.sp)
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold)
                Text(prompt, color = Color.White.copy(alpha = 0.72f), fontSize = 12.sp)
            }
        }
    }
}
