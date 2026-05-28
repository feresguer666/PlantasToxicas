package com.toxicplants.database.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri   // ← KTX extension

// ─────────────────────────────────────────────
// Modelo
// ─────────────────────────────────────────────
data class PoisonDatabase(
    val name: String,
    val description: String,
    val url: String,
    val emoji: String
)

// ─────────────────────────────────────────────
// Bases de datos científicas / oficiales
// ─────────────────────────────────────────────
val onlineDatabases = listOf(
    PoisonDatabase("Poison Control (EE.UU.)",        "Base de datos nacional sobre tóxicos. Llamada gratuita 24h.",              "https://www.poison.org/",                                                                                  "🇺🇸"),
    PoisonDatabase("FDA Poisonous Plant Database",   "Banco de datos de plantas venenosas de la FDA.",                           "https://www.accessdata.fda.gov/scripts/plantox/",                                                          "🌿"),
    PoisonDatabase("ASPCA Toxic & Non-Toxic Plants", "Lista de plantas tóxicas y no tóxicas para animales domésticos.",          "https://www.aspca.org/pet-care/animal-poison-control/toxic-and-non-toxic-plants",                          "🐕"),
    PoisonDatabase("Toxicología CLM (España)",       "Centro de Información Toxicológica de Castilla-La Mancha.",                "http://www.toxicologia.org/",                                                                              "🇪🇸"),
    PoisonDatabase("PlantNet",                       "Identificación de plantas con contribución científica global.",             "https://plantnet.org/",                                                                                    "🔬"),
    PoisonDatabase("Wikipedia – Lista plantas venenosas", "Artículo colaborativo con extensa lista mundial de plantas venenosas.", "https://en.wikipedia.org/wiki/List_of_poisonous_plants",                                                 "📚")
)

// ─────────────────────────────────────────────
// Recursos adicionales
// ─────────────────────────────────────────────
val extraResources = listOf(
    PoisonDatabase("Wikipedia – Plantas psicoactivas",          "Lista completa de plantas con propiedades psicoactivas documentadas.",                 "https://en.wikipedia.org/wiki/List_of_psychoactive_plants",                                                       "🧠"),
    PoisonDatabase("DMT-Nexus",                                 "Comunidad y base de conocimiento sobre plantas y sustancias psicoactivas.",            "https://www.dmt-nexus.me/",                                                                                       "🌀"),
    PoisonDatabase("Energy Control – Sustancias",               "Guía de harm reduction sobre sustancias principales. Proyecto de la ABD.",            "https://energycontrol.org/tiposustancia/sustancias-principales/",                                                 "💊"),
    PoisonDatabase("Jardines sin Fronteras – Plantas venenosas","Extracto de «The Poison Garden»: plantas venenosas con fichas detalladas.",           "https://jardinessinfronteras.com/2017/07/11/plantas-venenosas-extraido-de-the-poison-garden/",                    "☠️"),
    PoisonDatabase("Asturnatura – Plantas tóxicas y mágicas",   "Guía ilustrada de plantas tóxicas y con propiedades mágico-medicinales.",             "https://www.asturnatura.com/naturaleza/guias/plantas-toxicas-magicas/",                                           "🔮"),
    PoisonDatabase("Open Sanctuary – Global Toxic Plant DB",    "Base de datos global de plantas tóxicas para animales de santuario.",                 "https://opensanctuary.org/the-open-sanctuary-projects-global-toxic-plant-database/",                              "🐄"),
    PoisonDatabase("InfoJardín – Plantas tóxicas y venenosas",  "Fichas de plantas tóxicas y venenosas con fotos y síntomas.",                        "https://fichas.infojardin.com/listas-plantas/plantas-toxicas-venenosas.htm",                                      "🌱"),
    PoisonDatabase("Wikipedia – Categoría plantas venenosas ES","Categoría completa en Wikipedia en español con todas las especies documentadas.",      "https://es.wikipedia.org/wiki/Categor%C3%ADa:Plantas_venenosas",                                                  "📖")
)

// ─────────────────────────────────────────────
// Pantalla
// ─────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineDatabasesScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Recursos online", fontWeight = FontWeight.Bold)
                        Text(
                            "Bases de datos y referencias toxicológicas",
                            fontSize = 12.sp,
                            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier        = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding  = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Banner emergencia
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "⚠️ En caso de intoxicación",
                            fontWeight = FontWeight.Bold,
                            fontSize   = 18.sp,
                            color      = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Llama inmediatamente al Servicio de Información Toxicológica de España:\n☎ 91 562 04 20",
                            fontSize = 14.sp,
                            color    = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Sección 1
            item { SectionHeader("🔬", "Bases de datos científicas", MaterialTheme.colorScheme.primary) }

            items(onlineDatabases) { db ->
                DatabaseCard(database = db) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, db.url.toUri()))  // ← KTX
                }
            }

            // Sección 2
            item {
                Spacer(Modifier.height(4.dp))
                SectionHeader("🌐", "Recursos adicionales", Color(0xFF2E7D32))
            }

            items(extraResources) { db ->
                DatabaseCard(database = db) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, db.url.toUri()))  // ← KTX
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Toca cualquier recurso para abrirlo en el navegador",
                    fontSize = 11.sp,
                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// Cabecera de sección
// ─────────────────────────────────────────────
@Composable
fun SectionHeader(emoji: String, title: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier          = Modifier.padding(top = 4.dp, bottom = 2.dp)
    ) {
        Text(emoji, fontSize = 18.sp)
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
    }
    HorizontalDivider(color = color.copy(alpha = 0.3f), thickness = 1.dp)
}

// ─────────────────────────────────────────────
// Tarjeta de recurso
// ─────────────────────────────────────────────
@Composable
fun DatabaseCard(database: PoisonDatabase, onClick: () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier          = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(database.emoji, fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(database.name,        fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(database.description, fontSize   = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Text(database.url,         fontSize   = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.width(8.dp))
            Text("↗", fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
        }
    }
}
