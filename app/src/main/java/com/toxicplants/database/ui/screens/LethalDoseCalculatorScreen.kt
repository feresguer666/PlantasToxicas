package com.toxicplants.database.ui.screens

import android.content.Context
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.compose.runtime.livedata.observeAsState
import com.toxicplants.database.ui.viewmodel.PlantViewModel
import com.toxicplants.database.PlantEntity
import org.json.JSONArray
import org.json.JSONObject

data class ToxinPreset(
    val plantName: String,
    val part: String,
    val toxin: String,
    val ld50: Double,          // mg/kg
    val amountPerUnit: Double, // mg per unit
    val unitName: String,
    val category: String = "Planta",
    /**
     * null = preajuste interno.
     * "override:<clave>" = edición local de un preajuste interno.
     * "custom:<timestamp>" = preajuste creado manualmente por el usuario.
     */
    val userId: String? = null
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

private const val LD50_PREFS = "ld50_presets_prefs"
private const val LD50_USER_PRESETS_KEY = "user_presets_json"

private fun ToxinPreset.defaultKey(): String =
    listOf(plantName, part, toxin)
        .joinToString("|") { it.trim().lowercase() }

private fun ToxinPreset.identityKey(): String = userId ?: "builtin:${defaultKey()}"

private fun ToxinPreset.isUserCreated(): Boolean = userId?.startsWith("custom:") == true
private fun ToxinPreset.isUserOverride(): Boolean = userId?.startsWith("override:") == true

private fun mergeToxinPresets(
    builtIns: List<ToxinPreset>,
    userPresets: List<ToxinPreset>
): List<ToxinPreset> {
    val overrides = userPresets
        .filter { it.isUserOverride() }
        .associateBy { it.userId!!.removePrefix("override:") }
    val custom = userPresets
        .filter { it.isUserCreated() }
        .sortedBy { it.plantName.lowercase() }

    return builtIns.map { preset -> overrides[preset.defaultKey()] ?: preset } + custom
}

private fun loadUserToxinPresets(context: Context): List<ToxinPreset> {
    val raw = context.getSharedPreferences(LD50_PREFS, Context.MODE_PRIVATE)
        .getString(LD50_USER_PRESETS_KEY, "[]") ?: "[]"
    return try {
        val arr = JSONArray(raw)
        buildList {
            for (i in 0 until arr.length()) {
                arr.optJSONObject(i)?.toToxinPresetOrNull()?.let(::add)
            }
        }
    } catch (_: Exception) {
        emptyList()
    }
}

private fun saveUserToxinPresets(context: Context, presets: List<ToxinPreset>) {
    val arr = JSONArray()
    presets.forEach { arr.put(it.toJson()) }
    context.getSharedPreferences(LD50_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(LD50_USER_PRESETS_KEY, arr.toString())
        .apply()
}

private fun ToxinPreset.toJson(): JSONObject = JSONObject().apply {
    put("plantName", plantName)
    put("part", part)
    put("toxin", toxin)
    put("ld50", ld50)
    put("amountPerUnit", amountPerUnit)
    put("unitName", unitName)
    put("category", category)
    put("userId", userId ?: "")
}

private fun JSONObject.toToxinPresetOrNull(): ToxinPreset? {
    val name = optString("plantName").trim()
    val part = optString("part").trim()
    val toxin = optString("toxin").trim()
    val unit = optString("unitName").trim()
    val id = optString("userId").trim().ifBlank { null }
    val ld50 = optDouble("ld50", Double.NaN)
    val amount = optDouble("amountPerUnit", Double.NaN)

    if (name.isBlank() || part.isBlank() || toxin.isBlank() || unit.isBlank()) return null
    if (ld50.isNaN() || ld50 <= 0.0 || amount.isNaN() || amount <= 0.0) return null

    return ToxinPreset(
        plantName = name,
        part = part,
        toxin = toxin,
        ld50 = ld50,
        amountPerUnit = amount,
        unitName = unit,
        category = optString("category", "Planta").ifBlank { "Planta" },
        userId = id
    )
}

private fun upsertUserPreset(
    context: Context,
    current: List<ToxinPreset>,
    preset: ToxinPreset
): List<ToxinPreset> {
    val updated = current.filterNot { it.identityKey() == preset.identityKey() } + preset
    saveUserToxinPresets(context, updated)
    return updated
}

private fun removeUserPreset(
    context: Context,
    current: List<ToxinPreset>,
    preset: ToxinPreset
): List<ToxinPreset> {
    val updated = current.filterNot { it.identityKey() == preset.identityKey() }
    saveUserToxinPresets(context, updated)
    return updated
}

private fun editableCopyOf(initial: ToxinPreset?): ToxinPreset =
    initial ?: ToxinPreset(
        plantName = "",
        part = "",
        toxin = "",
        ld50 = 1.0,
        amountPerUnit = 1.0,
        unitName = "gramos",
        category = "Planta"
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LethalDoseCalculatorScreen(
    viewModel: PlantViewModel,
    onBack: () -> Unit,
    onPlantClick: (PlantEntity) -> Unit
) {
    val context = LocalContext.current
    val allPlants by viewModel.allPlants.observeAsState(emptyList())
    var userPresets by remember { mutableStateOf(loadUserToxinPresets(context)) }
    val allPresets = remember(userPresets) { mergeToxinPresets(PRESETS, userPresets) }

    var weight by remember { mutableStateOf("") }
    var isManual by remember { mutableStateOf(false) }

    var manualLd50 by remember { mutableStateOf("") }
    var selectedPreset by remember { mutableStateOf<ToxinPreset?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var showPresetEditor by remember { mutableStateOf(false) }
    var editingPreset by remember { mutableStateOf<ToxinPreset?>(null) }

    var resultDoseMg by remember { mutableStateOf<Double?>(null) }
    var resultUnits by remember { mutableStateOf<Double?>(null) }
    var resultPreset by remember { mutableStateOf<ToxinPreset?>(null) }
    var calculatedWeight by remember { mutableStateOf<Double?>(null) }

    LaunchedEffect(allPresets) {
        if (selectedPreset == null || allPresets.none { it.identityKey() == selectedPreset!!.identityKey() }) {
            selectedPreset = allPresets.firstOrNull()
            resultDoseMg = null
        }
    }

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

    if (showPresetEditor) {
        ToxinPresetEditorDialog(
            initial = editingPreset,
            onDismiss = {
                showPresetEditor = false
                editingPreset = null
            },
            onSave = { edited ->
                val base = editingPreset
                val userId = when {
                    base?.userId != null -> base.userId
                    base != null -> "override:${base.defaultKey()}"
                    else -> "custom:${System.currentTimeMillis()}"
                }
                val saved = edited.copy(userId = userId)
                userPresets = upsertUserPreset(context, userPresets, saved)
                selectedPreset = saved
                resultDoseMg = null
                showPresetEditor = false
                editingPreset = null
                Toast.makeText(context, "Preajuste guardado", Toast.LENGTH_SHORT).show()
            }
        )
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

                val filteredPresets = remember(selectedFilter, allPresets) {
                    if (selectedFilter == "Todas") allPresets else allPresets.filter { it.category == selectedFilter }
                }

                LaunchedEffect(selectedFilter, filteredPresets) {
                    if (selectedPreset == null || filteredPresets.none { it.identityKey() == selectedPreset!!.identityKey() }) {
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            editingPreset = null
                            showPresetEditor = true
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("➕ Añadir", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            editingPreset = selectedPreset
                            showPresetEditor = true
                        },
                        enabled = selectedPreset != null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("✏️ Editar", fontSize = 12.sp)
                    }
                    selectedPreset?.takeIf { it.userId != null }?.let { localPreset ->
                        OutlinedButton(
                            onClick = {
                                userPresets = removeUserPreset(context, userPresets, localPreset)
                                selectedPreset = null
                                resultDoseMg = null
                                val msg = if (localPreset.isUserOverride()) "Preajuste restaurado" else "Preajuste eliminado"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(if (localPreset.isUserOverride()) "↩️ Original" else "🗑️ Borrar", fontSize = 12.sp)
                        }
                    }
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
                                text = {
                                    Text(
                                        (if (preset.isUserCreated()) "★ " else if (preset.isUserOverride()) "✏️ " else "") +
                                                "${preset.plantName} - ${preset.part}"
                                    )
                                },
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
                            "${if (p.isUserCreated()) "★ Preajuste manual\n" else if (p.isUserOverride()) "✏️ Preajuste editado\n" else ""}" +
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
@Composable
private fun ToxinPresetEditorDialog(
    initial: ToxinPreset?,
    onDismiss: () -> Unit,
    onSave: (ToxinPreset) -> Unit
) {
    val base = remember(initial) { editableCopyOf(initial) }

    var plantName by remember(initial) { mutableStateOf(base.plantName) }
    var part by remember(initial) { mutableStateOf(base.part) }
    var toxin by remember(initial) { mutableStateOf(base.toxin) }
    var ld50 by remember(initial) { mutableStateOf(base.ld50.toString()) }
    var amountPerUnit by remember(initial) { mutableStateOf(base.amountPerUnit.toString()) }
    var unitName by remember(initial) { mutableStateOf(base.unitName) }
    var category by remember(initial) { mutableStateOf(base.category.ifBlank { "Planta" }) }

    val ld50Value = ld50.replace(",", ".").toDoubleOrNull()
    val amountValue = amountPerUnit.replace(",", ".").toDoubleOrNull()
    val isValid = plantName.isNotBlank() &&
            part.isNotBlank() &&
            toxin.isNotBlank() &&
            unitName.isNotBlank() &&
            ld50Value != null && ld50Value > 0.0 &&
            amountValue != null && amountValue > 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initial == null) "Añadir planta a LD50" else "Editar preajuste LD50")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Estos datos se guardan solo en tu dispositivo. Úsalos como referencia educativa.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = plantName,
                    onValueChange = { plantName = it },
                    label = { Text("Nombre visible") },
                    placeholder = { Text("Ej. Belladona (Atropa belladonna)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = part,
                    onValueChange = { part = it },
                    label = { Text("Parte usada") },
                    placeholder = { Text("Hojas, semillas, bayas...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = toxin,
                    onValueChange = { toxin = it },
                    label = { Text("Toxina principal") },
                    placeholder = { Text("Atropina, ricina, taxina...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ld50,
                        onValueChange = { ld50 = it },
                        label = { Text("LD50 mg/kg") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError = ld50.isNotBlank() && (ld50Value == null || ld50Value <= 0.0),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = amountPerUnit,
                        onValueChange = { amountPerUnit = it },
                        label = { Text("mg/unidad") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError = amountPerUnit.isNotBlank() && (amountValue == null || amountValue <= 0.0),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = unitName,
                    onValueChange = { unitName = it },
                    label = { Text("Nombre de unidad") },
                    placeholder = { Text("gramos de hoja, semillas, bayas...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Categoría", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Planta" to "🌿", "Baya" to "🍒", "Seta" to "🍄").forEach { (cat, icon) ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text("$icon $cat", fontSize = 12.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        ToxinPreset(
                            plantName = plantName.trim(),
                            part = part.trim(),
                            toxin = toxin.trim(),
                            ld50 = ld50Value!!,
                            amountPerUnit = amountValue!!,
                            unitName = unitName.trim(),
                            category = category,
                            userId = initial?.userId
                        )
                    )
                },
                enabled = isValid
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
