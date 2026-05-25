package com.toxicplants.database.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Modelo de datos para cada base de datos online
data class PoisonDatabase(
    val name: String,
    val description: String,
    val url: String,
    val emoji: String
)

// Lista de bases de datos recomendadas
val onlineDatabases = listOf(
    PoisonDatabase(
        name = "Poison Control (EE.UU.)",
        description = "Base de datos nacional sobre toxicos. Llamada gratuita 24h.",
        url = "https://www.poison.org/",
        emoji = "🇺🇸"
    ),
    PoisonDatabase(
        name = "FDA Poisonous Plant Database",
        description = "Banco de datos de plantas venenosas de la FDA.",
        url = "https://www.accessdata.fda.gov/scripts/plantox/",
        emoji = "🌿"
    ),
    PoisonDatabase(
        name = "ASPCA Toxic & Non-Toxic Plants",
        description = "Lista de plantas toxicas y no toxicas para animales.",
        url = "https://www.aspca.org/pet-care/animal-poison-control/toxic-and-non-toxic-plants",
        emoji = "🐕"
    ),
    PoisonDatabase(
        name = "Toxicologia CLM (Espana)",
        description = "Centro de Informacion Toxicologica de Castilla-La Mancha.",
        url = "http://www.toxicologia.org/",
        emoji = "🇪🇸"
    ),
    PoisonDatabase(
        name = "PlantNet",
        description = "Identificacion de plantas con contribucion cientifica global.",
        url = "https://plantnet.org/",
        emoji = "🔬"
    ),
    PoisonDatabase(
        name = "Wikipedia - Lista plantas venenosas",
        description = "Articulo colaborativo con extensa lista mundial.",
        url = "https://en.wikipedia.org/wiki/List_of_poisonous_plants",
        emoji = "📚"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineDatabasesScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Bases de Datos Online",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Recursos toxicologicos oficiales",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
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
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "⚠️ En caso de intoxicacion",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Llama inmediatamente al Servicio de Informacion Toxicologica de Espana:\n☎ 91 562 04 20",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Bases de datos cientificas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            items(onlineDatabases) { db ->
                DatabaseCard(
                    database = db,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(db.url))
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun DatabaseCard(
    database: PoisonDatabase,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(database.emoji, fontSize = 28.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    database.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    database.description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    database.url,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = "↗",
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
