package com.toxicplants.database.ui.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toxicplants.database.PlantExtraDataSource
import com.toxicplants.database.ui.theme.carbonEffectSubtle
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.ui.viewmodel.PlantViewModel

/**
 * Pantalla para buscar una planta de la BD y marcarla en plant_extra.json.
 *
 * @param mode  qué campo se va a marcar:
 *   "dogs", "cats", "horses", "cattle", "children", "fruitColor", "flowerColor"
 * @param colorValue  solo para fruitColor/flowerColor — el color a asignar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlantToExtraScreen(
    viewModel: PlantViewModel,
    mode: String,
    colorValue: String = "",
    onBack: () -> Unit
) {
    val context   = LocalContext.current
    val allPlants by viewModel.allPlants.observeAsState(emptyList())
    val extraMap  = remember { PlantExtraDataSource.loadAll(context).toMutableMap() }

    var query        by remember { mutableStateOf("") }
    var savedPlant   by remember { mutableStateOf<String?>(null) }   // nombre de la última guardada

    val title = when (mode) {
        "dogs"        -> "🐕 Añadir planta — Perros"
        "cats"        -> "🐈 Añadir planta — Gatos"
        "horses"      -> "🐴 Añadir planta — Caballos"
        "cattle"      -> "🐄 Añadir planta — Ganado"
        "children"    -> "👶 Añadir planta — Niños"
        "fruitColor"  -> "🍒 Añadir planta — Color fruto: $colorValue"
        "flowerColor" -> "🌸 Añadir planta — Color flor: $colorValue"
        else          -> "Añadir planta"
    }

    val headerColor = when (mode) {
        "dogs", "cats"       -> Color(0xFF4A148C)
        "horses", "cattle"   -> Color(0xFF4E342E)
        "children"           -> Color(0xFFE65100)
        "fruitColor"         -> Color(0xFF880E4F)
        "flowerColor"        -> Color(0xFF880E4F)
        else                 -> Color(0xFF2E7D32)
    }

    // Filtra por query
    val filtered = remember(allPlants, query) {
        if (query.isBlank()) allPlants
        else allPlants.filter {
            it.commonName.contains(query, ignoreCase = true) ||
                    it.scientificName.contains(query, ignoreCase = true)
        }
    }

    // ¿Ya está marcada esta planta en este modo?
    fun isMarked(plant: PlantEntity): Boolean {
        val extra = extraMap[plant.scientificName] ?: return false
        return when (mode) {
            "dogs"        -> extra.toxicDogs
            "cats"        -> extra.toxicCats
            "horses"      -> extra.toxicHorses
            "cattle"      -> extra.toxicCattle
            "children"    -> extra.toxicChildren
            "fruitColor"  -> extra.fruitColor == colorValue
            "flowerColor" -> extra.flowerColor == colorValue
            else          -> false
        }
    }

    // Marca o desmarca la planta
    fun toggle(plant: PlantEntity) {
        val marked = isMarked(plant)
        when (mode) {
            "dogs"        -> PlantExtraDataSource.setToxicDogs(context, plant.scientificName, !marked)
            "cats"        -> PlantExtraDataSource.setToxicCats(context, plant.scientificName, !marked)
            "horses"      -> PlantExtraDataSource.setToxicHorses(context, plant.scientificName, !marked)
            "cattle"      -> PlantExtraDataSource.setToxicCattle(context, plant.scientificName, !marked)
            "children"    -> PlantExtraDataSource.setToxicChildren(context, plant.scientificName, !marked)
            "fruitColor"  -> PlantExtraDataSource.setFruitColor(context, plant.scientificName, if (!marked) colorValue else "")
            "flowerColor" -> PlantExtraDataSource.setFlowerColor(context, plant.scientificName, if (!marked) colorValue else "")
        }
        // Refrescar caché
        PlantExtraDataSource.clearCache()
        savedPlant = if (!marked) plant.commonName else null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = headerColor)
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().carbonEffectSubtle().padding(paddingValues)) {

            // Buscador
            OutlinedTextField(
                value         = query,
                onValueChange = { query = it },
                modifier      = Modifier.fillMaxWidth().padding(12.dp),
                placeholder   = { Text("Buscar planta…") },
                leadingIcon   = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon  = {
                    if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                    }
                },
                singleLine = true,
                shape      = RoundedCornerShape(12.dp),
                colors     = OutlinedTextFieldDefaults.colors(focusedBorderColor = headerColor)
            )

            // Mensaje de confirmación
            savedPlant?.let { name ->
                Surface(
                    color    = Color(0xFF4CAF50).copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    shape    = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "✅ $name añadida correctamente",
                        modifier   = Modifier.padding(10.dp),
                        color      = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Medium,
                        fontSize   = 13.sp
                    )
                }
            }

            // Contador
            Text(
                "${filtered.size} plantas",
                fontSize = 12.sp,
                color    = Color.Gray,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
            )

            // Lista
            LazyColumn(
                contentPadding      = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filtered) { plant ->
                    val marked   = isMarked(plant)
                    val toxColor = when (plant.toxicityLevel) {
                        "Mortal"   -> Color(0xFFB71C1C)
                        "Muy alto" -> Color(0xFFFF5722)
                        "Alto"     -> Color(0xFFE65100)
                        "Moderado" -> Color(0xFFF57C00)
                        "Bajo"     -> Color(0xFF388E3C)
                        else       -> Color.Gray
                    }

                    Card(
                        modifier  = Modifier.fillMaxWidth().clickable { toggle(plant) },
                        elevation = CardDefaults.cardElevation(2.dp),
                        colors    = if (marked)
                            CardDefaults.cardColors(containerColor = headerColor.copy(alpha = 0.08f))
                        else
                            CardDefaults.cardColors()
                    ) {
                        Row(
                            modifier          = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Icono check o toxicidad
                            if (marked) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Marcada",
                                    tint     = headerColor,
                                    modifier = Modifier.size(28.dp)
                                )
                            } else {
                                Text(
                                    when (plant.toxicityLevel) {
                                        "Mortal"   -> "☠️"
                                        "Muy alto" -> "💀"
                                        "Alto"     -> "⚠️"
                                        "Moderado" -> "⚡"
                                        "Bajo"     -> "🟢"
                                        else       -> "🌿"
                                    },
                                    fontSize = 22.sp,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    plant.commonName,
                                    fontWeight = FontWeight.Bold,
                                    maxLines   = 1,
                                    overflow   = TextOverflow.Ellipsis,
                                    color      = if (marked) headerColor else Color.Unspecified
                                )
                                Text(
                                    plant.scientificName,
                                    fontSize  = 12.sp,
                                    fontStyle = FontStyle.Italic,
                                    color     = Color.Gray,
                                    maxLines  = 1
                                )
                            }

                            // Badge toxicidad
                            Surface(
                                color = toxColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    plant.toxicityLevel,
                                    modifier   = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize   = 10.sp,
                                    color      = toxColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}
