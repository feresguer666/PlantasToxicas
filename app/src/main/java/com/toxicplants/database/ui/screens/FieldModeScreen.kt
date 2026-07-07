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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldModeScreen(
    onBack: () -> Unit,
    onIdentifyPlants: () -> Unit,
    onIdentifyFungiLichens: () -> Unit,
    onTextScanner: () -> Unit,
    onColorSearch: () -> Unit,
    onSymptomsSearch: () -> Unit,
    onMap: () -> Unit,
    onNotes: () -> Unit,
    onEmergency: () -> Unit,
    onGlobalSearch: () -> Unit,
    onEmergencyMap: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("🌲 Modo campo", fontWeight = FontWeight.Bold)
                        Text("Accesos rápidos para usar fuera", fontSize = 12.sp, color = Color.White.copy(alpha = 0.82f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1B5E20),
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Brush.verticalGradient(listOf(Color(0xFF061207), Color(0xFF0D2410), Color(0xFF123B18))))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                FieldModeBigButton(
                    icon = "🆘",
                    title = "SOS URGENCIA (Asistente 112)",
                    gradient = Brush.horizontalGradient(listOf(Color(0xFFB71C1C), Color(0xFF7F0000))),
                    onClick = onEmergency
                )
            }
            item {
                FieldModeBigButton(
                    icon = "📷",
                    title = "Identificar planta",
                    gradient = Brush.horizontalGradient(listOf(Color(0xFF1B5E20), Color(0xFF43A047))),
                    onClick = onIdentifyPlants
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FieldModeSmallButton(
                        modifier = Modifier.weight(1f),
                        icon = "🍄",
                        title = "Setas / líquenes",
                        gradient = Brush.horizontalGradient(listOf(Color(0xFF4E342E), Color(0xFF795548))),
                        onClick = onIdentifyFungiLichens
                    )
                    FieldModeSmallButton(
                        modifier = Modifier.weight(1f),
                        icon = "🔤",
                        title = "Escáner texto",
                        gradient = Brush.horizontalGradient(listOf(Color(0xFF0D47A1), Color(0xFF1976D2))),
                        onClick = onTextScanner
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FieldModeSmallButton(
                        modifier = Modifier.weight(1f),
                        icon = "🎨",
                        title = "Color",
                        gradient = Brush.horizontalGradient(listOf(Color(0xFF6A1B9A), Color(0xFFAB47BC))),
                        onClick = onColorSearch
                    )
                    FieldModeSmallButton(
                        modifier = Modifier.weight(1f),
                        icon = "🩺",
                        title = "Síntomas",
                        gradient = Brush.horizontalGradient(listOf(Color(0xFFC62828), Color(0xFFE65100))),
                        onClick = onSymptomsSearch
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FieldModeSmallButton(
                        modifier = Modifier.weight(1f),
                        icon = "🗺️",
                        title = "Avistamientos",
                        gradient = Brush.horizontalGradient(listOf(Color(0xFF33691E), Color(0xFF558B2F))),
                        onClick = onMap
                    )
                    FieldModeSmallButton(
                        modifier = Modifier.weight(1f),
                        icon = "📝",
                        title = "Notas",
                        gradient = Brush.horizontalGradient(listOf(Color(0xFF455A64), Color(0xFF78909C))),
                        onClick = onNotes
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FieldModeSmallButton(
                        modifier = Modifier.weight(1f),
                        icon = "🚨",
                        title = "Emergencia",
                        gradient = Brush.horizontalGradient(listOf(Color(0xFFB71C1C), Color(0xFFD32F2F))),
                        onClick = onEmergency
                    )
                    FieldModeSmallButton(
                        modifier = Modifier.weight(1f),
                        icon = "🏥",
                        title = "Radar Urgencias",
                        gradient = Brush.horizontalGradient(listOf(Color(0xFF006064), Color(0xFF00838F))),
                        onClick = onEmergencyMap
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FieldModeSmallButton(
                        modifier = Modifier.weight(1f),
                        icon = "🔎",
                        title = "Búsqueda global",
                        gradient = Brush.horizontalGradient(listOf(Color(0xFF0D47A1), Color(0xFF00ACC1))),
                        onClick = onGlobalSearch
                    )
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun FieldModeBigButton(
    icon: String,
    title: String,
    gradient: Brush,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() },
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 38.sp)
                Spacer(Modifier.width(14.dp))
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            }
        }
    }
}

@Composable
private fun FieldModeSmallButton(
    modifier: Modifier = Modifier,
    icon: String,
    title: String,
    gradient: Brush,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(88.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(icon, fontSize = 28.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
