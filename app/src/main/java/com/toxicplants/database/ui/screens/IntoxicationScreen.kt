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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    onBack: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme

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
                    Text("Guía rápida educativa", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.onErrorContainer)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Busca por síntomas o consulta síndromes toxicológicos de plantas, setas y líquenes. " +
                                "Si hay ingestión real o síntomas graves, llama a emergencias/toxicología: esta sección no sustituye atención médica.",
                        color = colors.onErrorContainer,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            IntoxicationActionCard(
                icon = "🔬",
                title = "Síntomas",
                subtitle = "Buscar plantas y compuestos por vómitos, diarrea, alucinaciones, convulsiones, irritación…",
                gradient = Brush.horizontalGradient(listOf(Color(0xFF2E7D32), Color(0xFF66BB6A))),
                onClick = onSymptomsClick
            )

            IntoxicationActionCard(
                icon = "📚",
                title = "Síndromes toxicológicos",
                subtitle = "Atropínico, Faloidiano, Muscarínico, Orellánico, Giromitrínico, Digitálico, Cianogénico y más.",
                gradient = Brush.horizontalGradient(listOf(Color(0xFFB71C1C), Color(0xFFFF7043))),
                onClick = onSyndromesClick
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
