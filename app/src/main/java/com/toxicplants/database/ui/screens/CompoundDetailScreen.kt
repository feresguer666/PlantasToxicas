package com.toxicplants.database.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.ui.viewmodel.CompoundViewModel
import com.toxicplants.database.ui.viewmodel.PlantViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompoundDetailScreen(
    compoundId: Int,
    compoundViewModel: CompoundViewModel,
    plantViewModel: PlantViewModel,
    onBack: () -> Unit,
    onPlantClick: (PlantEntity) -> Unit,
) {
    val compound by compoundViewModel.byId(compoundId).observeAsState()
    val allPlants by plantViewModel.allPlants.observeAsState(emptyList())

    if (compound == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    val c = compound!!
    val color = parseColor(c.groupColor)
    val sourcePlantNames = remember(c.sourcePlants) {
        c.sourcePlants.split("|").map { it.trim() }.filter { it.isNotEmpty() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(c.commonName, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1)
                        if (c.molecularFormula.isNotBlank()) {
                            Text(c.molecularFormula, fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { compoundViewModel.toggleFavorite(c) }) {
                        Icon(
                            imageVector = if (c.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorito",
                            tint = if (c.isFavorite) Color(0xFFFFC1C1) else Color.White,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = color,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {

            // Cabecera: nombres
            Card(
                colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(c.commonName, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = color)
                    if (c.iupacName.isNotBlank()) {
                        Text(
                            c.iupacName,
                            fontStyle = FontStyle.Italic,
                            color = Color.DarkGray,
                            fontSize = 14.sp,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Chip(c.groupName, color)
                        if (c.subgroup.isNotBlank()) Chip(c.subgroup, Color.DarkGray)
                    }
                }
            }

            // Datos químicos
            if (c.molecularFormula.isNotBlank() || c.molecularWeight != null) {
                Section(title = "Datos químicos", icon = Icons.Filled.Science, color = color) {
                    if (c.molecularFormula.isNotBlank()) {
                        CompoundInfoRow("Fórmula molecular", c.molecularFormula)
                    }
                    c.molecularWeight?.let {
                        CompoundInfoRow("Peso molecular", "%.2f g/mol".format(it))
                    }
                    if (c.concentration.isNotBlank()) {
                        CompoundInfoRow("Concentración típica", c.concentration)
                    }
                }
            }

            // Plantas que lo contienen
            if (sourcePlantNames.isNotEmpty()) {
                Section(title = "Plantas que lo contienen", icon = Icons.Filled.Spa, color = color) {
                    sourcePlantNames.forEach { name ->
                        SourcePlantItem(
                            name = name,
                            allPlants = allPlants,
                            color = color,
                            onPlantClick = onPlantClick,
                        )
                    }
                }
            }

            // Toxicidad
            Section(title = "Toxicidad", icon = Icons.Filled.Warning, color = Color(0xFFB71C1C)) {
                CompoundInfoRow("Mecanismo", c.mechanism)
                if (c.ld50.isNotBlank()) CompoundInfoRow("LD₅₀", c.ld50)
                if (c.toxicDose.isNotBlank()) CompoundInfoRow("Dosis tóxica humana", c.toxicDose)
            }

            // Cuadro clínico
            val hasClinical = listOf(
                c.clinicalNeuro, c.clinicalCardio, c.clinicalDigestive,
                c.clinicalRespiratory, c.clinicalDermal, c.clinicalOther
            ).any { it.isNotBlank() }
            if (hasClinical) {
                Section(title = "Cuadro clínico", icon = Icons.Filled.MonitorHeart, color = Color(0xFFB71C1C)) {
                    SystemRow("Neurológico", c.clinicalNeuro, Icons.Filled.Psychology)
                    SystemRow("Cardiovascular", c.clinicalCardio, Icons.Filled.MonitorHeart)
                    SystemRow("Digestivo", c.clinicalDigestive, Icons.Filled.Restaurant)
                    SystemRow("Respiratorio", c.clinicalRespiratory, Icons.Filled.Grain)
                    SystemRow("Dérmico / Mucosas", c.clinicalDermal, Icons.Filled.Spa)
                    SystemRow("Otros", c.clinicalOther, Icons.Filled.Warning)
                }
            }

            // Temporalidad
            if (c.onsetTime.isNotBlank() || c.duration.isNotBlank()) {
                Section(title = "Temporalidad", icon = Icons.Filled.Timer, color = Color(0xFF1976D2)) {
                    if (c.onsetTime.isNotBlank()) CompoundInfoRow("Latencia", c.onsetTime)
                    if (c.duration.isNotBlank()) CompoundInfoRow("Duración / pronóstico", c.duration)
                }
            }

            // Tratamiento
            if (c.treatment.isNotBlank()) {
                Section(
                    title = "Tratamiento / Antídoto",
                    icon = Icons.Filled.LocalPharmacy,
                    color = Color(0xFF2E7D32),
                ) {
                    Text(c.treatment, fontSize = 14.sp, lineHeight = 20.sp)
                }
            }

            // Notas
            if (c.notes.isNotBlank()) {
                Section(title = "Notas", icon = Icons.Filled.Science, color = Color(0xFF424242)) {
                    Text(c.notes, fontSize = 14.sp, lineHeight = 20.sp)
                }
            }

            // Disclaimer
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("⚠️", fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Información divulgativa. Ante sospecha de intoxicación contacta con el " +
                            "Instituto Nacional de Toxicología (España): 91 562 04 20.",
                        fontSize = 12.sp,
                        color = Color(0xFF6D4C00),
                    )
                }
            }
        }
    }
}

@Composable
private fun Section(
    title: String,
    icon: ImageVector,
    color: Color,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color)
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
            }
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun CompoundInfoRow(label: String, value: String) {
    if (value.isBlank()) return
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(label, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
        Text(value, fontSize = 14.sp, lineHeight = 19.sp)
    }
}

@Composable
private fun SystemRow(label: String, value: String, icon: ImageVector) {
    if (value.isBlank()) return
    Row(
        modifier = Modifier.padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFFB71C1C), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB71C1C))
            Text(value, fontSize = 14.sp, lineHeight = 19.sp)
        }
    }
}

@Composable
private fun Chip(text: String, color: Color) {
    Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            fontSize = 12.sp,
            color = color,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SourcePlantItem(
    name: String,
    allPlants: List<PlantEntity>,
    color: Color,
    onPlantClick: (PlantEntity) -> Unit,
) {
    // Buscamos coincidencia por scientificName (case-insensitive y permitiendo prefijo)
    val match = remember(allPlants, name) {
        val q = name.trim().lowercase()
        allPlants.firstOrNull { it.scientificName.trim().lowercase() == q }
            ?: allPlants.firstOrNull {
                it.scientificName.trim().lowercase().startsWith(q.split(" ").take(2).joinToString(" "))
            }
    }
    val clickable = match != null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (clickable) Modifier
                    .clickable { onPlantClick(match!!) }
                    .background(color.copy(alpha = 0.07f))
                else Modifier
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Spa,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                name,
                fontStyle = FontStyle.Italic,
                fontSize = 14.sp,
                fontWeight = if (clickable) FontWeight.Bold else FontWeight.Normal,
            )
            if (clickable) {
                Text(
                    "Ver ficha de ${match!!.commonName} →",
                    fontSize = 11.sp,
                    color = color,
                )
            }
        }
    }
}
