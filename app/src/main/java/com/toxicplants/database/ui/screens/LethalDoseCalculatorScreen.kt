package com.toxicplants.database.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.runtime.livedata.observeAsState
import com.toxicplants.database.ui.viewmodel.PlantViewModel
import com.toxicplants.database.PlantEntity
import androidx.compose.material.icons.filled.Info

data class ToxinPreset(
    val plantName: String,
    val part: String,
    val toxin: String,
    val ld50: Double,          // mg/kg
    val amountPerUnit: Double, // mg per unit
    val unitName: String,
    val category: String = "Planta"
)

val PRESETS = listOf(
    // 🌿 Plantas (General)
    ToxinPreset("Acónito (Aconitum napellus)", "Raíz", "Aconitina", 0.03, 3.0, "gramos de raíz", "Planta"),
    ToxinPreset("Adelfa (Nerium oleander)", "Hojas", "Oleandrina", 0.3, 15.0, "gramos de hoja", "Planta"),
    ToxinPreset("Adonis de primavera (Adonis vernalis)", "Partes aéreas", "Adonidina", 1.0, 5.0, "gramos", "Planta"),
    ToxinPreset("Adormidera (Papaver somniferum)", "Látex/Cápsula", "Morfina", 2.0, 10.0, "cápsulas", "Planta"),
    ToxinPreset("Ajenjo (Artemisia absinthium)", "Hojas", "Tuyona", 45.0, 2.0, "gramos de hoja", "Planta"),
    ToxinPreset("Árbol de los suicidas (Cerbera odollam)", "Semillas", "Cerberina", 0.1, 5.0, "semillas masticadas", "Planta"),
    ToxinPreset("Ballestera (Veratrum album)", "Raíz", "Veratridina", 0.5, 1.0, "gramos de raíz", "Planta"),
    ToxinPreset("Beleño negro (Hyoscyamus niger)", "Hojas", "Hiosciamina", 2.0, 1.0, "gramos de hoja", "Planta"),
    ToxinPreset("Boj (Buxus sempervirens)", "Hojas", "Buxina", 10.0, 5.0, "gramos de hoja", "Planta"),
    ToxinPreset("Celidonia (Chelidonium majus)", "Savia", "Celidonina", 35.0, 5.0, "ml de savia", "Planta"),
    ToxinPreset("Cicuta mayor (Conium maculatum)", "Hojas/Frutos", "Cicutina", 5.0, 5.0, "gramos", "Planta"),
    ToxinPreset("Cicuta virosa (Cicuta virosa)", "Raíz", "Cicutoxina", 0.2, 2.0, "gramos de raíz", "Planta"),
    ToxinPreset("Cizaña (Lolium temulentum)", "Semillas", "Temulina", 20.0, 5.0, "gramos de semilla", "Planta"),
    ToxinPreset("Cólquico (Colchicum autumnale)", "Bulbo/Semillas", "Colchicina", 0.8, 4.0, "gramos", "Planta"),
    ToxinPreset("Dedalera (Digitalis purpurea)", "Hojas", "Digitalina", 0.5, 2.0, "gramos de hoja", "Planta"),
    ToxinPreset("Difenbaquia (Dieffenbachia spp.)", "Hojas", "Cristales de oxalato", 50.0, 15.0, "gramos de hoja", "Planta"),
    ToxinPreset("Eléboro negro (Helleborus niger)", "Raíces", "Heleborina", 0.5, 2.0, "gramos de raíz", "Planta"),
    ToxinPreset("Estramonio (Datura stramonium)", "Semillas", "Atropina/Escop.", 0.3, 0.1, "semillas", "Planta"),
    ToxinPreset("Glicinia (Wisteria sinensis)", "Semillas", "Citisina", 1.0, 2.0, "semillas", "Planta"),
    ToxinPreset("Haba de San Ignacio (Strychnos ignatii)", "Semillas", "Estricnina", 1.5, 20.0, "semillas masticadas", "Planta"),
    ToxinPreset("Hiedra venenosa (Toxicodendron radicans)", "Hojas", "Urushiol", 5.0, 1.0, "gramos (ingeridos)", "Planta"),
    ToxinPreset("Khat (Catha edulis)", "Hojas", "Catinona", 35.0, 5.0, "gramos", "Planta"),
    ToxinPreset("Lirio de los valles (Convallaria majalis)", "Hojas", "Convalatoxina", 1.0, 0.3, "gramos de hoja", "Planta"),
    ToxinPreset("Lluvia de oro (Laburnum anagyroides)", "Semillas", "Citisina", 1.0, 1.0, "semillas", "Planta"),
    ToxinPreset("Mandrágora (Mandragora officinarum)", "Raíz", "Atropina/Escop.", 0.5, 3.0, "gramos de raíz", "Planta"),
    ToxinPreset("Nabo del diablo (Oenanthe crocata)", "Raíz", "Oenantotoxina", 1.0, 10.0, "gramos de raíz", "Planta"),
    ToxinPreset("Nuez vómica (Strychnos nux-vomica)", "Semillas", "Estricnina", 1.5, 25.0, "semillas masticadas", "Planta"),
    ToxinPreset("Pulsatilla (Pulsatilla vulgaris)", "Planta fresca", "Ranunculina", 2.0, 5.0, "gramos", "Planta"),
    ToxinPreset("Regaliz americano (Abrus precatorius)", "Semillas", "Abrina", 0.002, 0.1, "semillas masticadas", "Planta"),
    ToxinPreset("Retama negra (Cytisus scoparius)", "Ramas", "Esparteína", 15.0, 2.0, "gramos", "Planta"),
    ToxinPreset("Ricino (Ricinus communis)", "Semillas", "Ricina", 1.0, 1.5, "semillas masticadas", "Planta"),
    ToxinPreset("Rododendro (Rhododendron spp.)", "Hojas", "Grayanotoxina", 1.5, 1.0, "gramos de hoja", "Planta"),
    ToxinPreset("Sabina (Juniperus sabina)", "Brotes", "Sabinol", 5.0, 1.0, "gramos", "Planta"),
    ToxinPreset("Tabaco (Nicotiana tabacum)", "Hojas", "Nicotina", 0.8, 20.0, "gramos de hoja seca", "Planta"),
    ToxinPreset("Tejo (Taxus baccata)", "Hojas", "Taxina", 5.0, 5.0, "gramos de hoja", "Planta"),
    ToxinPreset("Trompeta de ángel (Brugmansia spp.)", "Flores/Hojas", "Escopolamina", 2.0, 0.5, "gramos", "Planta"),

    // 🍒 Bayas y frutos tóxicos
    ToxinPreset("Acebo (Ilex aquifolium)", "Bayas", "Ilicina", 20.0, 5.0, "bayas", "Baya"),
    ToxinPreset("Actea (Actaea spicata)", "Bayas", "Protoanemonina", 5.0, 2.0, "bayas", "Baya"),
    ToxinPreset("Aligustre (Ligustrum vulgare)", "Bayas", "Ligustrina", 30.0, 5.0, "bayas", "Baya"),
    ToxinPreset("Alquequenje (Physalis alkekengi)", "Bayas inmaduras", "Fisalinas", 25.0, 2.0, "bayas inmaduras", "Baya"),
    ToxinPreset("Aro (Arum maculatum)", "Bayas", "Oxalato de calcio", 50.0, 10.0, "bayas", "Baya"),
    ToxinPreset("Belladona (Atropa belladonna)", "Bayas", "Atropina", 0.3, 2.0, "bayas", "Baya"),
    ToxinPreset("Bonetero (Euonymus europaeus)", "Bayas", "Evobiosina", 15.0, 3.0, "bayas", "Baya"),
    ToxinPreset("Cerezo de Jerusalén (Solanum pseudocapsicum)", "Bayas", "Solanocapsina", 10.0, 2.0, "bayas", "Baya"),
    ToxinPreset("Dulcamara (Solanum dulcamara)", "Bayas inmaduras", "Solanina", 3.0, 0.5, "bayas inmaduras", "Baya"),
    ToxinPreset("Espino cerval (Rhamnus cathartica)", "Bayas", "Antraquinonas", 50.0, 2.0, "bayas", "Baya"),
    ToxinPreset("Hiedra (Hedera helix)", "Bayas", "Hederina", 10.0, 2.0, "bayas", "Baya"),
    ToxinPreset("Hierba carmín (Phytolacca americana)", "Bayas", "Fitolaccatoxina", 15.0, 5.0, "bayas crudas", "Baya"),
    ToxinPreset("Hierba mora (Solanum nigrum)", "Bayas inmaduras", "Solanina", 3.0, 0.5, "bayas", "Baya"),
    ToxinPreset("Lantana (Lantana camara)", "Bayas inmaduras", "Lantadeno A", 5.0, 2.0, "bayas inmaduras", "Baya"),
    ToxinPreset("Madreselva (Lonicera xylosteum)", "Bayas", "Xilosteína", 25.0, 4.0, "bayas", "Baya"),
    ToxinPreset("Mezereón (Daphne mezereum)", "Bayas", "Dafnetoxina", 0.5, 5.0, "bayas", "Baya"),
    ToxinPreset("Muérdago (Viscum album)", "Bayas", "Viscotoxina", 2.5, 0.5, "bayas", "Baya"),
    ToxinPreset("Muguete / Lirio del valle (Convallaria majalis)", "Bayas", "Convalatoxina", 1.0, 0.3, "bayas", "Baya"),
    ToxinPreset("Nuez de lavado (Sapindus mukorossi)", "Frutos", "Saponinas", 20.0, 5.0, "frutos", "Baya"),
    ToxinPreset("Rusco (Ruscus aculeatus)", "Bayas", "Ruscogenina", 20.0, 1.0, "bayas", "Baya"),
    ToxinPreset("Saúco (Sambucus nigra)", "Bayas crudas", "Sambunigrina", 25.0, 2.0, "bayas crudas", "Baya"),
    ToxinPreset("Sinfocarpo (Symphoricarpos albus)", "Bayas", "Saponinas", 30.0, 5.0, "bayas", "Baya"),
    ToxinPreset("Tejo (Taxus baccata)", "Semilla de la baya", "Taxina", 5.0, 5.0, "semillas (arilo rojo es comestible)", "Baya"),
    ToxinPreset("Uva de zorra (Paris quadrifolia)", "Bayas", "Paridina", 5.0, 3.0, "bayas", "Baya"),
    ToxinPreset("Viburno (Viburnum opulus)", "Bayas crudas", "Viburnina", 30.0, 1.0, "bayas", "Baya"),

    // 🍄 Setas
    ToxinPreset("Amanita maloliente (Amanita virosa)", "Seta", "Amatoxina", 0.1, 0.25, "gramos de seta fresca", "Seta"),
    ToxinPreset("Amanita matamoscas (Amanita muscaria)", "Seta", "Ácido iboténico", 10.0, 5.0, "gramos de seta fresca", "Seta"),
    ToxinPreset("Amanita pantera (Amanita pantherina)", "Seta", "Ácido iboténico", 15.0, 5.0, "gramos de seta fresca", "Seta"),
    ToxinPreset("Amanita primaveral (Amanita verna)", "Seta", "Amatoxina", 0.1, 0.25, "gramos de seta fresca", "Seta"),
    ToxinPreset("Boletus satanas (Rubroboletus satanas)", "Seta cruda", "Bolesatina", 10.0, 5.0, "gramos crudos", "Seta"),
    ToxinPreset("Clitocibe blanco (Clitocybe dealbata)", "Seta", "Muscarina", 2.0, 3.0, "gramos de seta fresca", "Seta"),
    ToxinPreset("Cortinario de la montaña (Cortinarius rubellus)", "Seta", "Orellanina", 2.0, 3.0, "gramos de seta fresca", "Seta"),
    ToxinPreset("Cortinario espléndido (Cortinarius splendens)", "Seta", "Orellanina", 2.0, 3.0, "gramos de seta fresca", "Seta"),
    ToxinPreset("Entoloma lívido (Inosperma erubescens)", "Seta", "Muscarina", 2.0, 4.0, "gramos de seta fresca", "Seta"),
    ToxinPreset("Falsa colmenilla (Gyromitra esculenta)", "Seta cruda", "Giromitrina", 20.0, 1.0, "gramos de seta cruda", "Seta"),
    ToxinPreset("Galerina marginata (Galerina)", "Seta", "Amatoxina", 0.1, 0.1, "gramos de seta fresca", "Seta"),
    ToxinPreset("Lepiota de carne parda (Lepiota brunneoincarnata)", "Seta", "Amatoxina", 0.1, 0.2, "gramos de seta fresca", "Seta"),
    ToxinPreset("Oronja verde / Cicuta verde (Amanita phalloides)", "Seta", "Amatoxina", 0.1, 0.25, "gramos de seta fresca", "Seta"),
    ToxinPreset("Paxilo enrollado (Paxillus involutus)", "Seta", "Paxilina", 10.0, 5.0, "gramos de seta fresca", "Seta"),
    ToxinPreset("Seta de los caballeros (Tricholoma equestre)", "Seta", "Toxinas musculares", 15.0, 5.0, "gramos de seta fresca", "Seta"),
    ToxinPreset("Seta de los sudores (Inocybe erubescens)", "Seta", "Muscarina", 2.0, 5.0, "gramos de seta fresca", "Seta"),
    ToxinPreset("Seta del olivo (Omphalotus olearius)", "Seta", "Iludina", 5.0, 2.0, "gramos de seta fresca", "Seta")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LethalDoseCalculatorScreen(
    viewModel: PlantViewModel,
    onBack: () -> Unit,
    onPlantClick: (PlantEntity) -> Unit
) {
    val allPlants by viewModel.allPlants.observeAsState(emptyList())
    var weight by remember { mutableStateOf("") }
    var isManual by remember { mutableStateOf(false) }

    var manualLd50 by remember { mutableStateOf("") }
    var selectedPreset by remember { mutableStateOf<ToxinPreset?>(PRESETS.first()) }
    var expanded by remember { mutableStateOf(false) }

    var resultDoseMg by remember { mutableStateOf<Double?>(null) }
    var resultUnits by remember { mutableStateOf<Double?>(null) }
    var resultPreset by remember { mutableStateOf<ToxinPreset?>(null) }
    var calculatedWeight by remember { mutableStateOf<Double?>(null) }

    fun calculate() {
        val w = weight.replace(",", ".").toDoubleOrNull()
        if (w == null) {
            resultDoseMg = null
            return
        }
        calculatedWeight = w
        if (isManual) {
            val l = manualLd50.replace(",", ".").toDoubleOrNull()
            if (l != null) {
                resultDoseMg = w * l
                resultUnits = null
                resultPreset = null
            }
        } else {
            selectedPreset?.let { p ->
                resultDoseMg = w * p.ld50
                resultUnits = (w * p.ld50) / p.amountPerUnit
                resultPreset = p
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calculadora de Dosis Letal", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFC2185B),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(
                    "AVISO: Los valores (LD50) y las concentraciones son promedios educativos. La toxicidad real varía enormemente por la planta, época del año y metabolismo. Ante ingestión, contacta urgencias inmediatamente.",
                    color = Color.Black,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(14.dp)
                )
            }

            TabRow(
                selectedTabIndex = if (isManual) 1 else 0,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = !isManual,
                    onClick = { isManual = false; resultDoseMg = null },
                    text = { Text("Preajustes Botánicos", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = isManual,
                    onClick = { isManual = true; resultDoseMg = null },
                    text = { Text("Cálculo Manual", fontWeight = FontWeight.Bold) }
                )
            }

            OutlinedTextField(
                value = weight,
                onValueChange = { weight = it },
                label = { Text("Peso corporal del afectado (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            if (!isManual) {
                var selectedFilter by remember { mutableStateOf("Todas") }

                val filteredPresets = remember(selectedFilter) {
                    if (selectedFilter == "Todas") PRESETS else PRESETS.filter { it.category == selectedFilter }
                }

                LaunchedEffect(selectedFilter) {
                    if (selectedPreset !in filteredPresets) {
                        selectedPreset = filteredPresets.firstOrNull()
                        resultDoseMg = null
                    }
                }

                // Filtro visual de Planta vs Seta vs Baya
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                ) {
                    SegmentedButton(
                        selected = selectedFilter == "Todas",
                        onClick = { selectedFilter = "Todas" },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 4)
                    ) { Text("Todas", fontSize = 11.sp) }
                    SegmentedButton(
                        selected = selectedFilter == "Planta",
                        onClick = { selectedFilter = "Planta" },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 4)
                    ) { Text("🌿 Planta", fontSize = 11.sp) }
                    SegmentedButton(
                        selected = selectedFilter == "Seta",
                        onClick = { selectedFilter = "Seta" },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 4)
                    ) { Text("🍄 Seta", fontSize = 11.sp) }
                    SegmentedButton(
                        selected = selectedFilter == "Baya",
                        onClick = { selectedFilter = "Baya" },
                        shape = SegmentedButtonDefaults.itemShape(index = 3, count = 4)
                    ) { Text("🍒 Baya", fontSize = 11.sp) }
                }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedPreset?.plantName ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Especie") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        filteredPresets.forEach { preset ->
                            DropdownMenuItem(
                                text = { Text("${preset.plantName} - ${preset.part}") },
                                onClick = {
                                    selectedPreset = preset
                                    expanded = false
                                    resultDoseMg = null
                                }
                            )
                        }
                    }
                }
                selectedPreset?.let { p ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Toxina principal: ${p.toxin}\nLD50 estimado: ${p.ld50} mg/kg\nConcentración aprox: ${p.amountPerUnit} mg por ${p.unitName.split(" ").first()}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )

                        val sciName = p.plantName.substringAfter("(").substringBefore(")")
                        val linkedPlant = allPlants.find { it.scientificName.equals(sciName, ignoreCase = true) }
                        if (linkedPlant != null) {
                            FilledTonalButton(
                                onClick = { onPlantClick(linkedPlant) },
                                modifier = Modifier.padding(start = 8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Ver ficha", fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = manualLd50,
                    onValueChange = { manualLd50 = it },
                    label = { Text("LD50 de la toxina (mg/kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Button(
                onClick = { calculate() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC2185B))
            ) {
                Text("Calcular Dosis", fontWeight = FontWeight.Bold)
            }

            resultDoseMg?.let { doseMg ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Resultados para ${calculatedWeight} kg", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                        Column {
                            Text("Dosis tóxica de la sustancia pura:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "${String.format("%.2f", doseMg)} mg",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFB71C1C)
                            )
                            if (doseMg > 1000) {
                                Text("(${String.format("%.2f", doseMg / 1000)} gramos)", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        if (resultUnits != null && resultPreset != null) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                            Column {
                                Text("Cantidad de materia vegetal:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val units = resultUnits!!
                                val formattedUnits = if (units > 10) units.roundToInt().toString() else String.format("%.1f", units)

                                Text(
                                    "~ $formattedUnits ${resultPreset!!.unitName}",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFE65100)
                                )
                                Text("Puede ser mortal si se ingiere", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}