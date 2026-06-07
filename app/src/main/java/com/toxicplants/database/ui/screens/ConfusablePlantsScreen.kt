package com.toxicplants.database.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.PlantExtraDataSource
import com.toxicplants.database.ui.theme.carbonEffectSubtle
import com.toxicplants.database.ui.viewmodel.PlantViewModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ConfusablePlantsScreen(
    viewModel: PlantViewModel,
    onPlantClick: (PlantEntity) -> Unit,
    onBack: () -> Unit,
    onAddPlant: (String) -> Unit = {},
    onEditPlant: (Int) -> Unit = {}
) {
    val context   = LocalContext.current
    val allPlants by viewModel.allPlants.observeAsState(emptyList())
    val extraMap  = remember { PlantExtraDataSource.loadAll(context) }
    var query by remember { mutableStateOf("") }

    var comparingPlant by remember { mutableStateOf<PlantEntity?>(null) }
    var confusableTargetName by remember { mutableStateOf<String?>(null) }

    val confusablePlants = remember(allPlants, extraMap) {
        allPlants.filter { plant ->
            val extra = extraMap[plant.scientificName]
            extra != null && extra.confusableWith.isNotEmpty()
        }.sortedByDescending {
            when (it.toxicityLevel) {
                "Mortal" -> 5; "Muy alto" -> 4; "Alto" -> 3; "Moderado" -> 2; "Bajo" -> 1; else -> 0
            }
        }
    }

    val filtered = remember(confusablePlants, query) {
        if (query.isBlank()) confusablePlants
        else confusablePlants.filter { plant ->
            val extra = extraMap[plant.scientificName]
            plant.commonName.contains(query, ignoreCase = true) ||
                    plant.scientificName.contains(query, ignoreCase = true) ||
                    extra?.confusableWith?.any { it.contains(query, ignoreCase = true) } == true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("📸 Comparador Visual", fontWeight = FontWeight.Bold, color = Color.White)
                        Text("${filtered.size} riesgos detectados",
                            fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFBF360C))
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().carbonEffectSubtle().padding(paddingValues)) {
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    placeholder = { Text("Buscar planta tóxica o segura…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFBF360C))
                )

                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Sin resultados", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filtered) { plant ->
                            val extra = extraMap[plant.scientificName] ?: return@items
                            ConfusablePlantCard(
                                plant = plant,
                                extra = extra,
                                onPlantClick = onPlantClick,
                                onCompare = { target ->
                                    comparingPlant = plant
                                    confusableTargetName = target
                                }
                            )
                        }
                    }
                }
            }

            comparingPlant?.let { toxic ->
                VisualComparatorDialog(
                    toxicPlant = toxic,
                    targetName = confusableTargetName ?: "",
                    allPlants = allPlants,
                    onDismiss = {
                        comparingPlant = null
                        confusableTargetName = null
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConfusablePlantCard(
    plant: PlantEntity,
    extra: com.toxicplants.database.PlantExtraInfo,
    onPlantClick: (PlantEntity) -> Unit,
    onCompare: (String) -> Unit
) {
    val toxColor = when (plant.toxicityLevel) {
        "Mortal"   -> Color(0xFFB71C1C)
        "Muy alto" -> Color(0xFFFF5722)
        "Alto"     -> Color(0xFFE65100)
        "Moderado" -> Color(0xFFF57C00)
        "Bajo"     -> Color(0xFF388E3C)
        else       -> Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = toxColor.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp), modifier = Modifier.size(50.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(when (plant.toxicityLevel) { "Mortal" -> "💀"; "Muy alto" -> "☠️"; "Alto" -> "⚠️"; "Moderado" -> "⚡"; else -> "🟢" }, fontSize = 24.sp)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(plant.commonName, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    Text(plant.scientificName, fontSize = 12.sp, fontStyle = FontStyle.Italic, color = Color.Gray)
                }
                IconButton(onClick = { onPlantClick(plant) }) { Icon(Icons.Default.Info, null, tint = toxColor) }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(thickness = 0.5.dp, color = Color.Gray.copy(alpha = 0.3f))
            Spacer(Modifier.height(12.dp))

            Text("Peligro de confusión con:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFD84315))
            Spacer(Modifier.height(8.dp))

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                extra.confusableWith.forEach { name ->
                    Button(
                        onClick = { onCompare(name) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f), contentColor = Color(0xFF2E7D32)),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Compare, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (extra.confusionReason.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Surface(color = Color.Black.copy(alpha = 0.03f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("💡 ${extra.confusionReason}", modifier = Modifier.padding(12.dp), fontSize = 13.sp, color = Color.DarkGray, fontStyle = FontStyle.Italic)
                }
            }
        }
    }
}

@Composable
fun VisualComparatorDialog(toxicPlant: PlantEntity, targetName: String, allPlants: List<PlantEntity>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val targetPlant = remember(targetName, allPlants) {
        allPlants.find { it.commonName.contains(targetName, ignoreCase = true) || it.scientificName.contains(targetName, ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize().padding(16.dp), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Compare, null, tint = Color(0xFFBF360C))
                    Spacer(Modifier.width(12.dp))
                    Text("Comparador Visual", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Clear, null) }
                }
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Surface(color = Color(0xFFB71C1C), shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp), modifier = Modifier.fillMaxWidth()) {
                            Text("TÓXICA 💀", color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.padding(6.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Box(modifier = Modifier.weight(1f).fillMaxWidth().border(2.dp, Color(0xFFB71C1C))) {
                            if (toxicPlant.imageUrl.isNotBlank()) AsyncImage(model = toxicPlant.imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            else Box(Modifier.fillMaxSize().background(Color.LightGray), contentAlignment = Alignment.Center) { Text("Sin imagen", color = Color.Gray) }
                        }
                        Surface(color = Color.Black.copy(alpha = 0.05f), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(8.dp)) {
                                Text(toxicPlant.commonName, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                                Text(toxicPlant.scientificName, fontStyle = FontStyle.Italic, fontSize = 10.sp, color = Color.Gray, maxLines = 1)
                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Surface(color = Color(0xFF2E7D32), shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp), modifier = Modifier.fillMaxWidth()) {
                            Text("SEGURA ✅", color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.padding(6.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Box(modifier = Modifier.weight(1f).fillMaxWidth().border(2.dp, Color(0xFF2E7D32))) {
                            if (targetPlant?.imageUrl?.isNotBlank() == true) AsyncImage(model = targetPlant.imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            else Box(Modifier.fillMaxSize().background(Color.LightGray).padding(12.dp), contentAlignment = Alignment.Center) { Text("Sin imagen", textAlign = TextAlign.Center, fontSize = 10.sp, color = Color.Gray) }
                        }
                        Surface(color = Color.Black.copy(alpha = 0.05f), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(8.dp)) {
                                Text(targetName, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                                Text(targetPlant?.scientificName ?: "Especie segura", fontStyle = FontStyle.Italic, fontSize = 10.sp, color = Color.Gray, maxLines = 1)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = { val query = Uri.encode("${toxicPlant.scientificName} vs ${targetPlant?.scientificName ?: targetName} differences"); context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$query&tbm=isch"))) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBF360C))) {
                    Icon(Icons.Default.Search, null); Spacer(Modifier.width(8.dp)); Text("BUSCAR DIFERENCIAS CLAVE")
                }
                Spacer(Modifier.height(8.dp))
                Text("ADVERTENCIA: La identificación visual puede fallar. Ante la duda, NUNCA consumas ni toques la planta.", fontSize = 11.sp, color = Color.Red, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
            }
        }
    }
}
