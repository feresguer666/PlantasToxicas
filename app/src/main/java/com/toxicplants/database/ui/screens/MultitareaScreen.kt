package com.toxicplants.database.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.toxicplants.database.CompoundEntity
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.ui.gbif.GBIFEnrichmentScreen
import com.toxicplants.database.ui.screens.toxicgenera.ToxicGeneraScreen
import com.toxicplants.database.ui.viewmodel.CompoundViewModel
import com.toxicplants.database.ui.viewmodel.LichenViewModel
import com.toxicplants.database.ui.viewmodel.MushroomViewModel
import com.toxicplants.database.ui.viewmodel.PlantViewModel
import com.toxicplants.database.ui.viewmodel.PoisonousFamilyViewModel
import com.toxicplants.database.ui.viewmodel.SightingViewModel
import com.toxicplants.database.ui.viewmodel.ToxicCalendarViewModel

private enum class MultitareaTabType(
    val icon: String,
    val label: String,
    val description: String
) {
    NameSearch("🔤", "Nombres", "Búsqueda por nombres"),
    GlobalSearch("🌐", "Global", "Búsqueda global"),
    GoogleAI("🔎", "Google IA", "Buscador Google con IA"),
    TextScanner("📷", "Escáner texto", "Leer etiquetas con OCR"),
    NewPlant("➕", "Crear ficha", "Crear una ficha nueva de planta"),
    PlantList("🌿", "Fichas", "Lista completa de plantas"),
    Categories("🏷️", "Categorías", "Categorías de plantas"),
    Families("📚", "Familias", "Listado de familias"),
    ToxicSpecies("☠️", "Tóxicas", "Especies tóxicas"),
    ToxicGenera("🧬", "Géneros", "Géneros tóxicos"),
    PoisonousFamilies("☣️", "Fam. venenosas", "Familias venenosas"),
    OrnamentalDanger("🌺", "Ornamentales", "Ornamentales peligrosas"),
    Symptoms("🩺", "Síntomas", "Búsqueda por síntomas"),
    ToxicParts("🍃", "Partes", "Partes tóxicas"),
    Intoxication("🚨", "Intoxicación", "Síntomas, síndromes y ayuda"),
    ToxicSyndromes("🧾", "Síndromes", "Síndromes tóxicos"),
    Emergency("☎️", "Emergencia", "Emergencia toxicológica"),
    PetSafety("🐶", "Mascotas", "Plantas peligrosas para mascotas"),
    ChildSafety("👶", "Niños", "Plantas peligrosas para niños"),
    LivestockSafety("🐄", "Ganado", "Plantas peligrosas para ganado"),
    ColorSearch("🎨", "Colores", "Búsqueda por color"),
    Berries("🍓", "Bayas", "Guía de bayas"),
    Confusable("⚖️", "Confundibles", "Plantas confundibles"),
    Compare("🆚", "Comparar", "Comparador de plantas"),
    Phytochemistry("🔬", "Fitoquímica", "Compuestos tóxicos"),
    Psychotropic("🧠", "Psicotrópicas", "Plantas psicotrópicas"),
    Extraction("⚗️", "Extracción", "Métodos de extracción"),
    Reagents("🧪", "Reactivos", "Reactivos químicos"),
    Mushrooms("🍄", "Setas", "Setas tóxicas"),
    Lichens("🪨", "Líquenes", "Líquenes tóxicos"),
    Camera("📷", "Cámara", "Identificación por cámara"),
    NatureIdentify("🔎", "Foto setas", "Identificar setas y líquenes"),
    AR("📡", "AR", "Identificación AR"),
    Assistant("🤖", "IA", "Asistente IA"),
    RiskCalculator("🧮", "Riesgo", "Calculadora de riesgo"),
    LethalDose("☠️", "LD50", "Calculadora de dosis letal"),
    Glossary("📖", "Glosario", "Glosario botánico"),
    Calendar("📅", "Calendario", "Calendario tóxico"),
    Map("🗺️", "Mapa", "Historial/mapa de avistamientos"),
    Notes("📝", "Notas", "Notas"),
    Myths("📜", "Mitos", "Mitos y leyendas"),
    OnlineDatabases("🌍", "Recursos", "Bases de datos online"),
    GBIF("🌐", "GBIF", "Enriquecimiento GBIF"),
    DownloadImages("⬇️", "Imágenes", "Descargar imágenes"),
    Settings("⚙️", "Ajustes", "Ajustes")
}

private data class MultitareaTab(
    val id: Long,
    val type: MultitareaTabType,
    val title: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultitareaScreen(
    plantViewModel: PlantViewModel,
    compoundViewModel: CompoundViewModel,
    poisonousFamilyViewModel: PoisonousFamilyViewModel,
    onBack: () -> Unit,
    onPlantClick: (PlantEntity) -> Unit,
    onCompoundClick: (CompoundEntity) -> Unit,
    onFamilyClick: (String) -> Unit,
    onIntoxicationClick: () -> Unit = {},
    onDichotomousKeysClick: () -> Unit = {}
) {
    val tabs = remember {
        mutableStateListOf(
            MultitareaTab(1L, MultitareaTabType.NameSearch, "Nombres"),
            MultitareaTab(2L, MultitareaTabType.GlobalSearch, "Global"),
            MultitareaTab(3L, MultitareaTabType.ToxicSpecies, "Tóxicas"),
            MultitareaTab(4L, MultitareaTabType.Symptoms, "Síntomas"),
            MultitareaTab(5L, MultitareaTabType.Families, "Familias")
        )
    }
    var activeTabId by remember { mutableStateOf(1L) }
    var splitMode by remember { mutableStateOf(false) }
    var splitRatio by remember { mutableStateOf("50/50") }
    var secondaryTabId by remember { mutableStateOf<Long?>(2L) }
    var nextTabId by remember { mutableStateOf(6L) }
    var showAddDialog by remember { mutableStateOf(false) }
    val saveableStateHolder = rememberSaveableStateHolder()

    fun addTab(type: MultitareaTabType) {
        val sameTypeCount = tabs.count { it.type == type } + 1
        val title = if (sameTypeCount <= 1) type.label else "${type.label} $sameTypeCount"
        val newTab = MultitareaTab(nextTabId, type, title)
        nextTabId += 1L
        tabs.add(newTab)
        activeTabId = newTab.id
    }

    fun closeTab(tabId: Long) {
        if (tabs.size <= 1) {
            onBack()
            return
        }
        val index = tabs.indexOfFirst { it.id == tabId }
        if (index == -1) return
        tabs.removeAt(index)
        if (activeTabId == tabId) {
            activeTabId = tabs.getOrNull(index)?.id ?: tabs.last().id
        }
        if (secondaryTabId == tabId) {
            secondaryTabId = tabs.firstOrNull { it.id != activeTabId }?.id
            if (secondaryTabId == null) splitMode = false
        }
    }

    fun closeActiveOrBack() {
        closeTab(activeTabId)
    }

    val activeTab = tabs.firstOrNull { it.id == activeTabId } ?: tabs.first()
    val topWeight = when (splitRatio) {
        "70/30" -> 0.70f
        "30/70" -> 0.30f
        else -> 0.50f
    }
    val bottomWeight = 1f - topWeight

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text("🧩 Multitarea", fontWeight = FontWeight.Bold)
                            Text(
                                "Pulsa + para abrir cualquier pantalla como pestaña",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.78f)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                splitMode = !splitMode
                                if (splitMode && secondaryTabId == null) {
                                    secondaryTabId = tabs.firstOrNull { it.id != activeTabId }?.id
                                }
                            }
                        ) {
                            Text(if (splitMode) "▣" else "▤", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { showAddDialog = true }) {
                            Icon(Icons.Filled.Add, contentDescription = "Abrir pestaña", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF0D3311),
                        titleContentColor = Color.White
                    )
                )

                MultitareaTabBar(
                    tabs = tabs,
                    activeTabId = activeTabId,
                    onSelect = { activeTabId = it },
                    onClose = { closeTab(it) },
                    onAdd = { showAddDialog = true }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF060F07), Color(0xFF0A1A0C), Color(0xFF0D2410))
                    )
                )
        ) {
            if (!splitMode) {
                RenderMultitareaTab(
                    tab = activeTab,
                    saveableStateHolder = saveableStateHolder,
                    plantViewModel = plantViewModel,
                    compoundViewModel = compoundViewModel,
                    poisonousFamilyViewModel = poisonousFamilyViewModel,
                    onPlantClick = onPlantClick,
                    onCompoundClick = onCompoundClick,
                    onFamilyClick = onFamilyClick,
                    onDichotomousKeysClick = onDichotomousKeysClick,
                    addTab = { addTab(it) },
                    onCloseThis = { closeActiveOrBack() }
                )
            } else {
                val secondaryTab = tabs.firstOrNull { it.id == secondaryTabId && it.id != activeTabId }
                    ?: tabs.firstOrNull { it.id != activeTabId }
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(topWeight)) {
                        RenderMultitareaTab(
                            tab = activeTab,
                            saveableStateHolder = saveableStateHolder,
                            plantViewModel = plantViewModel,
                            compoundViewModel = compoundViewModel,
                            poisonousFamilyViewModel = poisonousFamilyViewModel,
                            onPlantClick = onPlantClick,
                            onCompoundClick = onCompoundClick,
                            onFamilyClick = onFamilyClick,
                            onDichotomousKeysClick = onDichotomousKeysClick,
                            addTab = { addTab(it) },
                            onCloseThis = { closeActiveOrBack() }
                        )
                    }
                    Surface(color = Color(0xFF00ACC1), modifier = Modifier.fillMaxWidth().height(40.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                listOf("50/50", "70/30", "30/70").forEach { ratio ->
                                    Surface(
                                        color = if (splitRatio == ratio) Color.Black else Color.White.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.padding(end = 4.dp).clickable { splitRatio = ratio }
                                    ) {
                                        Text(
                                            ratio,
                                            color = if (splitRatio == ratio) Color.White else Color.Black,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                tabs.filter { it.id != activeTabId }.take(4).forEach { tabOption ->
                                    Text(
                                        text = tabOption.type.icon,
                                        modifier = Modifier.clickable { secondaryTabId = tabOption.id }.padding(horizontal = 5.dp),
                                        fontSize = 17.sp
                                    )
                                }
                            }
                        }
                    }
                    Box(modifier = Modifier.weight(bottomWeight)) {
                        if (secondaryTab != null) {
                            RenderMultitareaTab(
                                tab = secondaryTab,
                                saveableStateHolder = saveableStateHolder,
                                plantViewModel = plantViewModel,
                                compoundViewModel = compoundViewModel,
                                poisonousFamilyViewModel = poisonousFamilyViewModel,
                                onPlantClick = onPlantClick,
                                onCompoundClick = onCompoundClick,
                                onFamilyClick = onFamilyClick,
                                onDichotomousKeysClick = onDichotomousKeysClick,
                                addTab = { addTab(it) },
                                onCloseThis = { closeTab(secondaryTab.id) }
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Abre otra pestaña para usar doble pantalla", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Abrir nueva pestaña") },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 560.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(MultitareaTabType.values().toList()) { type ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    addTab(type)
                                    showAddDialog = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(type.icon, fontSize = 26.sp)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(type.label, fontWeight = FontWeight.Bold)
                                    Text(type.description, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun RenderMultitareaTab(
    tab: MultitareaTab,
    saveableStateHolder: androidx.compose.runtime.saveable.SaveableStateHolder,
    plantViewModel: PlantViewModel,
    compoundViewModel: CompoundViewModel,
    poisonousFamilyViewModel: PoisonousFamilyViewModel,
    onPlantClick: (PlantEntity) -> Unit,
    onCompoundClick: (CompoundEntity) -> Unit,
    onFamilyClick: (String) -> Unit,
    onDichotomousKeysClick: () -> Unit,
    addTab: (MultitareaTabType) -> Unit,
    onCloseThis: () -> Unit
) {
    saveableStateHolder.SaveableStateProvider(tab.id) {

                when (tab.type) {
                    MultitareaTabType.NameSearch -> SearchScreen(
                        viewModel = plantViewModel,
                        onPlantClick = onPlantClick,
                        onBack = { onCloseThis() },
                        onIntoxicationClick = { addTab(MultitareaTabType.Intoxication) },
                        onDichotomousKeysClick = onDichotomousKeysClick
                    )

                    MultitareaTabType.GlobalSearch -> GlobalSearchScreen(
                        plantViewModel = plantViewModel,
                        compoundViewModel = compoundViewModel,
                        initialQuery = "",
                        onPlantClick = onPlantClick,
                        onCompoundClick = onCompoundClick,
                        onBack = { onCloseThis() }
                    )

                    MultitareaTabType.GoogleAI -> GoogleAISearchScreen(
                        onBack = { onCloseThis() }
                    )

                    MultitareaTabType.TextScanner -> TextScannerScreen(
                        viewModel = plantViewModel,
                        onPlantClick = onPlantClick,
                        onBack = { onCloseThis() }
                    )

                    MultitareaTabType.NewPlant -> EditPlantScreen(
                        plantId = null,
                        viewModel = plantViewModel,
                        onBack = { onCloseThis() }
                    )

                    MultitareaTabType.PlantList -> PlantListScreen(
                        viewModel = plantViewModel,
                        onPlantClick = onPlantClick,
                        onBack = { onCloseThis() }
                    )

                    MultitareaTabType.Categories -> CategoriesScreen(
                        viewModel = plantViewModel,
                        onCategoryClick = { /* en multitarea se queda en la lista de categorías */ },
                        onBack = { onCloseThis() }
                    )

                    MultitareaTabType.Families -> FamilyListScreen(
                        viewModel = plantViewModel,
                        onBack = { onCloseThis() },
                        onFamilyClick = onFamilyClick
                    )

                    MultitareaTabType.ToxicSpecies -> ToxicSpeciesScreen(
                        plantViewModel = plantViewModel,
                        poisonousFamilyViewModel = poisonousFamilyViewModel,
                        onBack = { onCloseThis() },
                        onPlantClick = onPlantClick
                    )

                    MultitareaTabType.ToxicGenera -> ToxicGeneraScreen(
                        onBack = { onCloseThis() },
                        onGenusClick = { /* se queda como pantalla interna */ }
                    )

                    MultitareaTabType.PoisonousFamilies -> PoisonousFamiliesScreen(
                        viewModel = poisonousFamilyViewModel,
                        onBack = { onCloseThis() },
                        onFamilyClick = { _, _ -> },
                        onAddGenus = { }
                    )

                    MultitareaTabType.OrnamentalDanger -> OrnamentalDangerPlantsScreen(
                        viewModel = plantViewModel,
                        onPlantClick = onPlantClick,
                        onBack = { onCloseThis() }
                    )

                    MultitareaTabType.Symptoms -> SearchBySymptomsScreen(
                        plantViewModel = plantViewModel,
                        compoundViewModel = compoundViewModel,
                        onPlantClick = onPlantClick,
                        onCompoundClick = onCompoundClick,
                        onBack = { onCloseThis() }
                    )

                    MultitareaTabType.ToxicParts -> ToxicPartsScreen(
                        viewModel = plantViewModel,
                        onPlantClick = onPlantClick,
                        onBack = { onCloseThis() }
                    )

                    MultitareaTabType.Intoxication -> IntoxicationScreen(
                        onSymptomsClick = { addTab(MultitareaTabType.Symptoms) },
                        onSyndromesClick = { addTab(MultitareaTabType.ToxicSyndromes) },
                        onChildrenClick = { addTab(MultitareaTabType.ChildSafety) },
                        onPetsClick = { addTab(MultitareaTabType.PetSafety) },
                        onLivestockClick = { addTab(MultitareaTabType.LivestockSafety) },
                        onBack = { onCloseThis() }
                    )

                    MultitareaTabType.ToxicSyndromes -> ToxicSyndromesScreen(
                        onBack = { onCloseThis() }
                    )

                    MultitareaTabType.Emergency -> EmergencyScreen(
                        viewModel = plantViewModel,
                        onPlantClick = onPlantClick,
                        onBack = { onCloseThis() }
                    )

                    MultitareaTabType.PetSafety -> PetSafetyScreen(
                        viewModel = plantViewModel,
                        onPlantClick = onPlantClick,
                        onBack = { onCloseThis() }
                    )

                    MultitareaTabType.ChildSafety -> ChildSafetyScreen(
                        viewModel = plantViewModel,
                        onPlantClick = onPlantClick,
                        onBack = { onCloseThis() }
                    )

                    MultitareaTabType.LivestockSafety -> LivestockSafetyScreen(
                        viewModel = plantViewModel,
                        onPlantClick = onPlantClick,
                        onBack = { onCloseThis() }
                    )

                    MultitareaTabType.ColorSearch -> ColorSearchScreen(
                        viewModel = plantViewModel,
                        onPlantClick = onPlantClick,
                        onBack = { onCloseThis() }
                    )

                    MultitareaTabType.Berries -> BerriesScreen(
                        onBack = { onCloseThis() }
                    )

                    MultitareaTabType.Confusable -> ConfusablePlantsScreen(
                        viewModel = plantViewModel,
                        onPlantClick = onPlantClick,
                        onBack = { onCloseThis() }
                    )

                    MultitareaTabType.Compare -> PlantCompareScreen(
                        viewModel = plantViewModel,
                        onPlantClick = onPlantClick,
                        onBack = { onCloseThis() }
                    )

                    MultitareaTabType.Phytochemistry -> PhytochemistryScreen(
                        viewModel = compoundViewModel,
                        onCompoundClick = onCompoundClick,
                        onGroupClick = { },
                        onAddCompoundClick = { },
                        onInteractionsClick = { },
                        onBack = { onCloseThis() }
                    )

                    MultitareaTabType.Psychotropic -> PsychotropicPlantsScreen(
                        plantViewModel = plantViewModel,
                        compoundViewModel = compoundViewModel,
                        onPlantClick = onPlantClick,
                        onBack = { onCloseThis() }
                    )

                    MultitareaTabType.Extraction -> ChemicalExtractionMethodsScreen(
                        onBack = { onCloseThis() }
                    )

                    MultitareaTabType.Reagents -> ChemicalReagentsScreen(
                        onBack = { onCloseThis() }
                    )

                    MultitareaTabType.Mushrooms -> {
                        val mushroomViewModel: MushroomViewModel = viewModel()
                        ToxicMushroomsScreen(
                            viewModel = mushroomViewModel,
                            onBack = { onCloseThis() }
                        )
                    }

                    MultitareaTabType.Lichens -> {
                        val lichenViewModel: LichenViewModel = viewModel()
                        ToxicLichensScreen(
                            viewModel = lichenViewModel,
                            onBack = { onCloseThis() }
                        )
                    }

                    MultitareaTabType.Camera -> CameraIdentifyScreen(
                        viewModel = plantViewModel,
                        onPlantClick = onPlantClick,
                        onNavigateToPlantNetResult = { _, _ -> },
                        onBack = { onCloseThis() }
                    )

                    MultitareaTabType.NatureIdentify -> {
                        val mushroomViewModel: MushroomViewModel = viewModel()
                        val lichenViewModel: LichenViewModel = viewModel()
                        PhotoIdentifyFungiLichensScreen(
                            mushroomViewModel = mushroomViewModel,
                            lichenViewModel = lichenViewModel,
                            onBack = { onCloseThis() }
                        )
                    }

                    MultitareaTabType.AR -> ARScreen(
                        viewModel = plantViewModel,
                        onPlantClick = onPlantClick,
                        onBack = { onCloseThis() }
                    )

                    MultitareaTabType.Assistant -> AssistantScreen(
                        onBack = { onCloseThis() },
                        onOpenGlossary = { addTab(MultitareaTabType.Glossary) }
                    )

                    MultitareaTabType.RiskCalculator -> RiskCalculatorScreen(
                        onBack = { onCloseThis() }
                    )

                    MultitareaTabType.LethalDose -> LethalDoseCalculatorScreen(
                        viewModel = plantViewModel,
                        onBack = { onCloseThis() },
                        onPlantClick = onPlantClick
                    )

                    MultitareaTabType.Glossary -> GlossaryScreen(
                        onBack = { onCloseThis() }
                    )

                    MultitareaTabType.Calendar -> {
                        val calendarViewModel: ToxicCalendarViewModel = viewModel()
                        ToxicCalendarScreen(
                            viewModel = calendarViewModel,
                            plantViewModel = plantViewModel,
                            onBack = { onCloseThis() },
                            onPlantClick = onPlantClick
                        )
                    }

                    MultitareaTabType.Map -> {
                        val sightingViewModel: SightingViewModel = viewModel()
                        SightingsHistoryScreen(
                            viewModel = sightingViewModel,
                            plantViewModel = plantViewModel,
                            onPlantClick = onPlantClick,
                            onBack = { onCloseThis() }
                        )
                    }

                    MultitareaTabType.Notes -> NotesScreen(
                        onBack = { onCloseThis() }
                    )

                    MultitareaTabType.Myths -> MythsScreen(
                        viewModel = plantViewModel,
                        onBack = { onCloseThis() },
                        onPlantClick = { id ->
                            val plant = plantViewModel.allPlants.value?.find { it.id == id }
                            if (plant != null) onPlantClick(plant)
                        }
                    )

                    MultitareaTabType.OnlineDatabases -> OnlineDatabasesScreen(
                        onNavigateBack = { onCloseThis() }
                    )

                    MultitareaTabType.GBIF -> GBIFEnrichmentScreen(
                        onNavigateBack = { onCloseThis() }
                    )

                    MultitareaTabType.DownloadImages -> DownloadImagesScreen(
                        viewModel = plantViewModel,
                        onBack = { onCloseThis() }
                    )

                    MultitareaTabType.Settings -> SettingsScreen(
                        onBack = { onCloseThis() },
                        onNavigateToDownloadImages = { addTab(MultitareaTabType.DownloadImages) }
                    )
                }
            
    }
}

@Composable
private fun MultitareaTabBar(
    tabs: List<MultitareaTab>,
    activeTabId: Long,
    onSelect: (Long) -> Unit,
    onClose: (Long) -> Unit,
    onAdd: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF071A09),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val selected = tab.id == activeTabId
                val background = if (selected) Color(0xFF00ACC1) else Color(0xFF1B3A1D)
                val content = if (selected) Color.Black else Color.White

                Surface(
                    shape = RoundedCornerShape(50),
                    color = background,
                    modifier = Modifier
                        .height(42.dp)
                        .clickable { onSelect(tab.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(start = 12.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(tab.type.icon, fontSize = 18.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            tab.title,
                            color = content,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                        IconButton(
                            onClick = { onClose(tab.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Cerrar pestaña",
                                tint = content,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }

            Button(
                onClick = onAdd,
                contentPadding = PaddingValues(horizontal = 12.dp),
                modifier = Modifier.height(42.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Añadir", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Nueva")
            }
        }
    }
}
