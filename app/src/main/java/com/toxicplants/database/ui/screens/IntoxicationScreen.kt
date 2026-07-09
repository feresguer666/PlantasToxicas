package com.toxicplants.database.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun IntoxicationScreen(
    onSymptomsClick: () -> Unit,
    onSyndromesClick: () -> Unit,
    onChildrenClick: () -> Unit = {},
    onPetsClick: () -> Unit = {},
    onLivestockClick: () -> Unit = {},
    onBack: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var showCompanionDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Surface(modifier = Modifier.fillMaxWidth(), color = colors.error) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = colors.onError
                    )
                }
                Text(
                    text = "☠️ Intoxicación",
                    color = colors.onError,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(containerColor = colors.errorContainer.copy(alpha = 0.55f)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Guía rápida", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.onErrorContainer)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Busca por síntomas o síndromes toxicológicos.",
                        color = colors.onErrorContainer,
                        fontSize = 11.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            IntoxicationActionCard(
                icon = "🔬",
                title = "Síntomas",
                subtitle = "plantas compuestos",
                gradient = Brush.horizontalGradient(listOf(Color(0xFF2E7D32), Color(0xFF66BB6A))),
                onClick = onSymptomsClick
            )

            IntoxicationActionCard(
                icon = "📚",
                title = "Síndromes toxicológicos",
                subtitle = "",
                gradient = Brush.horizontalGradient(listOf(Color(0xFFB71C1C), Color(0xFFFF7043))),
                onClick = onSyndromesClick
            )

            IntoxicationActionCard(
                icon = "👶🐾",
                title = "Niños y mascotas",
                subtitle = "",
                gradient = Brush.horizontalGradient(listOf(Color(0xFF6A1B9A), Color(0xFF8E24AA))),
                onClick = { showCompanionDialog = true }
            )

            Spacer(Modifier.weight(1f))

            Text(
                text = "Consejo: conserva restos de la planta/seta/líquen, foto del ejemplar y hora aproximada de exposición.",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = colors.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
    }

    if (showCompanionDialog) {
        AlertDialog(
            onDismissRequest = { showCompanionDialog = false },
            title = { Text("👶🐾 Niños y mascotas", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CompanionIntoxicationOption(
                        icon = "👶",
                        title = "Niños",
                        subtitle = "Riesgos domésticos y plantas peligrosas para menores",
                        gradient = Brush.horizontalGradient(listOf(Color(0xFFE65100), Color(0xFFF57C00))),
                        onClick = { showCompanionDialog = false; onChildrenClick() }
                    )
                    CompanionIntoxicationOption(
                        icon = "🐾",
                        title = "Mascotas",
                        subtitle = "Perros, gatos y animales de compañía",
                        gradient = Brush.horizontalGradient(listOf(Color(0xFF4A148C), Color(0xFF6A1B9A))),
                        onClick = { showCompanionDialog = false; onPetsClick() }
                    )
                    CompanionIntoxicationOption(
                        icon = "🐄",
                        title = "Ganado",
                        subtitle = "Caballos, vacas, ovejas, cabras y otros animales",
                        gradient = Brush.horizontalGradient(listOf(Color(0xFF4E342E), Color(0xFF6D4C41))),
                        onClick = { showCompanionDialog = false; onLivestockClick() }
                    )
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showCompanionDialog = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun CompanionIntoxicationOption(
    icon: String,
    title: String,
    subtitle: String,
    gradient: Brush,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(gradient)
            .clickable { onClick() },
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(subtitle, color = Color.White.copy(alpha = 0.84f), fontSize = 11.sp, lineHeight = 14.sp)
            }
            Text("›", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun IntoxicationActionCard(
    icon: String,
    title: String,
    subtitle: String,
    gradient: Brush,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(118.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(gradient)
            .clickable { onClick() },
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 38.sp)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, color = Color.White.copy(alpha = 0.88f), fontSize = 12.sp, lineHeight = 16.sp)
            }
            Text("›", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        }
    }
}
