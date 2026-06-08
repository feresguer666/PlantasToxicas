package com.toxicplants.database.ui.screens

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.ToxicCalendarEvent
import com.toxicplants.database.ui.viewmodel.SeasonalData
import com.toxicplants.database.ui.viewmodel.ToxicCalendarViewModel
import com.toxicplants.database.ui.viewmodel.toIntList
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

// ═══════════════════════════════════════════════════════════════
// PANTALLA PRINCIPAL: Calendario de Tóxicos
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToxicCalendarScreen(
    viewModel: ToxicCalendarViewModel,
    plantViewModel: androidx.lifecycle.ViewModel,
    onBack: () -> Unit,
    onPlantClick: (PlantEntity) -> Unit = {}
) {
    val pvm = plantViewModel as com.toxicplants.database.ui.viewmodel.PlantViewModel
    val allPlants by viewModel.allPlants.collectAsState()
    val seasonalData by viewModel.seasonalPlants.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val selectedDayEvents by viewModel.selectedDayEvents.collectAsState()
    val selectedPhenologyPlant by viewModel.selectedPhenologyPlant.collectAsState()
    val allEvents by viewModel.allEvents.observeAsState(emptyList())

    // Sembrar fenología al abrir la pantalla
    LaunchedEffect(Unit) { viewModel.seedPhenologyIfNeeded() }

    val pagerState = rememberPagerState(initialPage = selectedTab, pageCount = { 3 })
    val tabTitles = listOf("📅 Estacional", "🌿 Fenología", "🔔 Alertas")

    LaunchedEffect(pagerState.currentPage) { viewModel.selectTab(pagerState.currentPage) }
    LaunchedEffect(selectedTab) {
        if (pagerState.currentPage != selectedTab) {
            pagerState.animateScrollToPage(selectedTab)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📅 Calendario de Tóxicos", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1B5E20),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tabs
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = Color(0xFF0D3311),
                contentColor = Color.White,
                edgePadding = 12.dp
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            viewModel.selectTab(index)
                        },
                        text = {
                            Text(title, color = if (pagerState.currentPage == index) Color.White else Color.Gray,
                                fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal)
                        }
                    )
                }
            }

            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                when (page) {
                    0 -> SeasonalTab(
                        viewModel = viewModel,
                        seasonalData = seasonalData,
                        onPlantClick = onPlantClick
                    )
                    1 -> PhenologyTab(
                        viewModel = viewModel,
                        allPlants = allPlants,
                        selectedPlant = selectedPhenologyPlant,
                        onPlantSelect = { viewModel.selectPhenologyPlant(it) },
                        onPlantClick = onPlantClick
                    )
                    2 -> AlertsTab(
                        viewModel = viewModel,
                        allPlants = allPlants,
                        allEvents = allEvents,
                        selectedDate = selectedDate,
                        selectedDayEvents = selectedDayEvents,
                        onDateSelect = { viewModel.selectDate(it) }
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// TAB 1: VISTA ESTACIONAL
// ═══════════════════════════════════════════════════════════════

@Composable
fun SeasonalTab(
    viewModel: ToxicCalendarViewModel,
    seasonalData: SeasonalData,
    onPlantClick: (PlantEntity) -> Unit
) {
    val currentMonth by viewModel.currentMonth.collectAsState()
    val currentYear by viewModel.currentYear.collectAsState()
    val calendarMonth = remember(currentMonth, currentYear) {
        mutableStateOf<com.toxicplants.database.ui.viewmodel.CalendarMonth?>(null)
    }

    LaunchedEffect(currentMonth, currentYear) {
        calendarMonth.value = viewModel.generateCalendarMonth()
    }

    var expandedSection by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── Navegador de mes ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.previousMonth() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Mes anterior", tint = Color.White)
                }
                Text(
                    "${monthName(currentMonth)} $currentYear",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                IconButton(onClick = { viewModel.nextMonth() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Mes siguiente",
                        tint = Color.White, modifier = Modifier.graphicsLayer { scaleX = -1f })
                }
            }
        }

        // ── Indicadores estacionales ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SeasonBadge(Modifier.weight(1f), "🌸 Floración", seasonalData.flowering.size, Color(0xFFE91E63))
                SeasonBadge(Modifier.weight(1f), "🍎 Frutos", seasonalData.fruiting.size, Color(0xFFFF9800))
                SeasonBadge(Modifier.weight(1f), "☠️ Tóxica máx.", seasonalData.maxToxicity.size, Color(0xFFF44336))
            }
        }

        // ── Calendario miniatura ──
        item {
            val cm = calendarMonth.value
            if (cm != null) {
                MiniCalendarGrid(
                    calendarMonth = cm,
                    currentMonth = currentMonth,
                    seasonalData = seasonalData
                )
            }
        }

        // ── Plantas en floración ──
        if (seasonalData.flowering.isNotEmpty()) {
            item {
                ExpandableSection(
                    title = "🌸 En floración (${seasonalData.flowering.size})",
                    color = Color(0xFFE91E63),
                    expanded = expandedSection == "flowering",
                    onToggle = {
                        expandedSection = if (expandedSection == "flowering") null else "flowering"
                    }
                )
            }
            if (expandedSection == "flowering") {
                items(seasonalData.flowering, key = { "fl_${it.id}" }) { plant ->
                    SeasonalPlantCard(plant, indicator = "🌸", onPlantClick = onPlantClick)
                }
            }
        }

        // ── Plantas en fructificación ──
        if (seasonalData.fruiting.isNotEmpty()) {
            item {
                ExpandableSection(
                    title = "🍎 En fructificación (${seasonalData.fruiting.size})",
                    color = Color(0xFFFF9800),
                    expanded = expandedSection == "fruiting",
                    onToggle = {
                        expandedSection = if (expandedSection == "fruiting") null else "fruiting"
                    }
                )
            }
            if (expandedSection == "fruiting") {
                items(seasonalData.fruiting, key = { "fr_${it.id}" }) { plant ->
                    SeasonalPlantCard(plant, indicator = "🍎", onPlantClick = onPlantClick)
                }
            }
        }

        // ── Toxicidad máxima ──
        if (seasonalData.maxToxicity.isNotEmpty()) {
            item {
                ExpandableSection(
                    title = "☠️ Toxicidad máxima (${seasonalData.maxToxicity.size})",
                    color = Color(0xFFF44336),
                    expanded = expandedSection == "maxTox",
                    onToggle = {
                        expandedSection = if (expandedSection == "maxTox") null else "maxTox"
                    }
                )
            }
            if (expandedSection == "maxTox") {
                items(seasonalData.maxToxicity, key = { "tx_${it.id}" }) { plant ->
                    SeasonalPlantCard(plant, indicator = "☠️", onPlantClick = onPlantClick)
                }
            }
        }

        // Mensaje si no hay datos
        if (seasonalData.flowering.isEmpty() && seasonalData.fruiting.isEmpty() && seasonalData.maxToxicity.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🌿", fontSize = 48.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Sin datos estacionales para este mes", color = Color.Gray)
                        Spacer(Modifier.height(4.dp))
                        Text("Los datos de fenología se completarán gradualmente", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniCalendarGrid(
    calendarMonth: com.toxicplants.database.ui.viewmodel.CalendarMonth,
    currentMonth: Int,
    seasonalData: SeasonalData
) {
    val dayNames = listOf("L", "M", "X", "J", "V", "S", "D")
    val floweringDays = seasonalData.flowering.flatMap { it.floweringMonths.toIntList() }.toSet()
    val fruitingDays = seasonalData.fruiting.flatMap { it.fruitingMonths.toIntList() }.toSet()
    val toxDays = seasonalData.maxToxicity.flatMap { it.maxToxicityMonths.toIntList() }.toSet()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1A2E))
            .padding(8.dp)
    ) {
        // Encabezado de días
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            dayNames.forEach { day ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(day, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center)
                }
            }
        }
        Spacer(Modifier.height(4.dp))

        // Semanas
        calendarMonth.weeks.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                week.forEach { day ->
                    val hasFlowering = day.isCurrentMonth && currentMonth in floweringDays
                    val hasFruiting = day.isCurrentMonth && currentMonth in fruitingDays
                    val hasTox = day.isCurrentMonth && currentMonth in toxDays
                    val hasEvents = day.events.isNotEmpty()

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(1.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    day.isToday -> Color(0xFF4CAF50)
                                    !day.isCurrentMonth -> Color.Transparent
                                    day.events.any { it.eventType == "incident" } -> Color(0xFFB71C1C).copy(alpha = 0.3f)
                                    hasEvents -> Color(0xFF1976D2).copy(alpha = 0.3f)
                                    else -> Color.Transparent
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "${day.dayOfMonth}",
                                color = when {
                                    day.isToday -> Color.White
                                    !day.isCurrentMonth -> Color.Gray.copy(alpha = 0.3f)
                                    else -> Color.White
                                },
                                fontSize = 12.sp,
                                fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal
                            )
                            // Indicadores de temporada (puntos)
                            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                                if (hasFlowering) Box(Modifier.size(3.dp).clip(CircleShape).background(Color(0xFFE91E63)))
                                if (hasFruiting) Box(Modifier.size(3.dp).clip(CircleShape).background(Color(0xFFFF9800)))
                                if (hasTox) Box(Modifier.size(3.dp).clip(CircleShape).background(Color(0xFFF44336)))
                            }
                        }
                    }
                }
            }
        }

        // Leyenda
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LegendItem("🌸 Floración", Color(0xFFE91E63))
            LegendItem("🍎 Frutos", Color(0xFFFF9800))
            LegendItem("☠️ Tóxica", Color(0xFFF44336))
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 10.sp, color = Color.Gray)
    }
}

@Composable
private fun RowScope.SeasonBadge(modifier: Modifier = Modifier, label: String, count: Int, color: Color) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$count", fontWeight = FontWeight.Bold, color = color, fontSize = 18.sp)
            Text(label, color = color, fontSize = 9.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ExpandableSection(
    title: String,
    color: Color,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f))
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = color, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Icon(
            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = color
        )
    }
}

@Composable
private fun SeasonalPlantCard(
    plant: PlantEntity,
    indicator: String,
    onPlantClick: (PlantEntity) -> Unit
) {
    val toxicityColor = when (plant.toxicityLevel) {
        "Mortal" -> Color(0xFFB71C1C)
        "Muy alto" -> Color(0xFFFF5722)
        "Alto" -> Color(0xFFE65100)
        "Moderado" -> Color(0xFFF57C00)
        "Bajo" -> Color(0xFF388E3C)
        else -> Color.Gray
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlantClick(plant) },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(indicator, fontSize = 24.sp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(plant.commonName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(plant.scientificName, color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(color = toxicityColor.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                        Text(plant.toxicityLevel, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp, color = toxicityColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.Gray, modifier = Modifier.graphicsLayer { scaleX = -1f })
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// TAB 2: FENOLOGÍA
// ═══════════════════════════════════════════════════════════════

@Composable
fun PhenologyTab(
    viewModel: ToxicCalendarViewModel,
    allPlants: List<PlantEntity>,
    selectedPlant: PlantEntity?,
    onPlantSelect: (PlantEntity?) -> Unit,
    onPlantClick: (PlantEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredPlants = remember(allPlants, searchQuery) {
        if (searchQuery.isBlank()) allPlants.filter { it.floweringMonths.isNotBlank() || it.fruitingMonths.isNotBlank() || it.maxToxicityMonths.isNotBlank() }
        else allPlants.filter {
            (it.floweringMonths.isNotBlank() || it.fruitingMonths.isNotBlank() || it.maxToxicityMonths.isNotBlank()) &&
                    (it.commonName.contains(searchQuery, true) || it.scientificName.contains(searchQuery, true))
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Selector de planta
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Buscar planta con datos fenológicos...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        // Gráfico fenológico de la planta seleccionada
        if (selectedPlant != null) {
            item {
                PhenologyTimeline(plant = selectedPlant)
            }
        }

        // Lista de plantas con datos fenológicos
        items(filteredPlants.take(50), key = { "ph_${it.id}" }) { plant ->
            val isSelected = selectedPlant?.id == plant.id
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlantSelect(if (isSelected) null else plant) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color(0xFF1B5E20).copy(alpha = 0.3f) else Color(0xFF1A1A2E)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(plant.commonName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(plant.scientificName, color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = 12.sp)
                    }
                    // Mini indicadores de meses
                    MonthDots(plant)
                }
                if (isSelected) {
                    Spacer(Modifier.height(4.dp))
                    PhenologyMiniBar(plant, Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                }
            }
        }

        if (filteredPlants.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🌿", fontSize = 40.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("No hay datos fenológicos disponibles", color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
private fun PhenologyTimeline(plant: PlantEntity) {
    val flowering = plant.floweringMonths.toIntList()
    val fruiting = plant.fruitingMonths.toIntList()
    val maxTox = plant.maxToxicityMonths.toIntList()
    val monthLabels = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(plant.commonName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(plant.scientificName, color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            Spacer(Modifier.height(16.dp))

            // Timeline de 12 meses
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                (1..12).forEach { month ->
                    val isFlowering = month in flowering
                    val isFruiting = month in fruiting
                    val isMaxTox = month in maxTox
                    val bgColor = when {
                        isMaxTox -> Color(0xFFF44336).copy(alpha = 0.6f)
                        isFlowering -> Color(0xFFE91E63).copy(alpha = 0.5f)
                        isFruiting -> Color(0xFFFF9800).copy(alpha = 0.5f)
                        else -> Color.Gray.copy(alpha = 0.1f)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(bgColor),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isMaxTox) Text("☠️", fontSize = 12.sp)
                            else if (isFlowering) Text("🌸", fontSize = 12.sp)
                            else if (isFruiting) Text("🍎", fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(monthLabels[month - 1], fontSize = 9.sp, color = Color.Gray)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            // Leyenda
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem("🌸 Floración", Color(0xFFE91E63))
                LegendItem("🍎 Frutos", Color(0xFFFF9800))
                LegendItem("☠️ Tóxica máx.", Color(0xFFF44336))
            }
        }
    }
}

@Composable
private fun MonthDots(plant: PlantEntity) {
    val flowering = plant.floweringMonths.toIntList()
    val fruiting = plant.fruitingMonths.toIntList()
    val maxTox = plant.maxToxicityMonths.toIntList()

    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        (1..12).forEach { month ->
            val color = when {
                month in maxTox -> Color(0xFFF44336)
                month in flowering -> Color(0xFFE91E63)
                month in fruiting -> Color(0xFFFF9800)
                else -> Color.Gray.copy(alpha = 0.2f)
            }
            Box(Modifier.size(4.dp).clip(CircleShape).background(color))
        }
    }
}

@Composable
private fun PhenologyMiniBar(plant: PlantEntity, modifier: Modifier = Modifier) {
    val flowering = plant.floweringMonths.toIntList()
    val fruiting = plant.fruitingMonths.toIntList()
    val maxTox = plant.maxToxicityMonths.toIntList()

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        (1..12).forEach { month ->
            val color = when {
                month in maxTox -> Color(0xFFF44336)
                month in flowering -> Color(0xFFE91E63)
                month in fruiting -> Color(0xFFFF9800)
                else -> Color.Gray.copy(alpha = 0.15f)
            }
            Box(
                Modifier
                    .size(width = 22.dp, height = 6.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// TAB 3: ALERTAS Y EVENTOS
// ═══════════════════════════════════════════════════════════════

@Composable
fun AlertsTab(
    viewModel: ToxicCalendarViewModel,
    allPlants: List<PlantEntity>,
    allEvents: List<ToxicCalendarEvent>,
    selectedDate: String?,
    selectedDayEvents: List<ToxicCalendarEvent>,
    onDateSelect: (String) -> Unit
) {
    val currentMonth by viewModel.currentMonth.collectAsState()
    val currentYear by viewModel.currentYear.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<ToxicCalendarEvent?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<ToxicCalendarEvent?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ── Navegador de mes ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.previousMonth() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Mes anterior", tint = Color.White)
                }
                Text(
                    "${monthName(currentMonth)} $currentYear",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Row {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, "Añadir evento", tint = Color(0xFF4CAF50))
                    }
                    IconButton(onClick = { viewModel.nextMonth() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Mes siguiente",
                            tint = Color.White, modifier = Modifier.graphicsLayer { scaleX = -1f })
                    }
                }
            }
        }

        // ── Calendario interactivo ──
        item {
            AlertsCalendarGrid(
                viewModel = viewModel,
                allEvents = allEvents,
                selectedDate = selectedDate,
                onDateSelect = onDateSelect
            )
        }

        // ── Eventos del día seleccionado ──
        if (selectedDate != null) {
            item {
                val formatted = try {
                    val ld = LocalDate.parse(selectedDate)
                    ld.format(DateTimeFormatter.ofPattern("d 'de' MMMM, yyyy", Locale("es")))
                } catch (_: Exception) { selectedDate }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "📅 $formatted",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.AddCircle, "Añadir", tint = Color(0xFF4CAF50))
                    }
                }
            }

            if (selectedDayEvents.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Sin eventos para este día", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            } else {
                items(selectedDayEvents) { event ->
                    EventCard(
                        event = event,
                        onEdit = { showEditDialog = event },
                        onDelete = { showDeleteConfirm = event }
                    )
                }
            }
        }

        // ── Últimos eventos ──
        if (selectedDate == null && allEvents.isNotEmpty()) {
            item {
                Text("📋 Últimos eventos", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            items(allEvents.take(10)) { event ->
                EventCard(
                    event = event,
                    onEdit = { showEditDialog = event },
                    onDelete = { showDeleteConfirm = event }
                )
            }
        }
    }

    // Diálogo: Añadir evento
    if (showAddDialog) {
        AddOrEditEventDialog(
            initialDate = selectedDate ?: String.format("%04d-%02d-%02d", currentYear, currentMonth, java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH)),
            allPlants = allPlants,
            onDismiss = { showAddDialog = false },
            onSave = { event ->
                viewModel.addEvent(event)
                showAddDialog = false
            }
        )
    }

    // Diálogo: Editar evento
    showEditDialog?.let { event ->
        AddOrEditEventDialog(
            existingEvent = event,
            initialDate = event.date,
            allPlants = allPlants,
            onDismiss = { showEditDialog = null },
            onSave = { updated ->
                viewModel.updateEvent(updated)
                showEditDialog = null
            }
        )
    }

    // Confirmación: Eliminar evento
    showDeleteConfirm?.let { event ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("🗑️ ¿Eliminar evento?") },
            text = { Text("¿Seguro que quieres eliminar \"${event.title}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteEvent(event)
                    showDeleteConfirm = null
                }) { Text("Eliminar", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun AlertsCalendarGrid(
    viewModel: ToxicCalendarViewModel,
    allEvents: List<ToxicCalendarEvent>,
    selectedDate: String?,
    onDateSelect: (String) -> Unit
) {
    val currentMonth by viewModel.currentMonth.collectAsState()
    val currentYear by viewModel.currentYear.collectAsState()
    var calendarMonth by remember { mutableStateOf<com.toxicplants.database.ui.viewmodel.CalendarMonth?>(null) }

    LaunchedEffect(currentMonth, currentYear, allEvents) {
        calendarMonth = viewModel.generateCalendarMonth()
    }

    val dayNames = listOf("L", "M", "X", "J", "V", "S", "D")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1A2E))
            .padding(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            dayNames.forEach { day ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(day, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center)
                }
            }
        }
        Spacer(Modifier.height(4.dp))

        calendarMonth?.weeks?.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                week.forEach { day ->
                    val isSelected = day.date == selectedDate
                    val hasIncident = day.events.any { it.eventType == "incident" }
                    val hasAlert = day.events.any { it.eventType == "alert" }
                    val hasAny = day.events.isNotEmpty()

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isSelected && day.isCurrentMonth -> Color(0xFF4CAF50)
                                    hasIncident -> Color(0xFFB71C1C).copy(alpha = 0.4f)
                                    hasAlert -> Color(0xFFFFA000).copy(alpha = 0.3f)
                                    hasAny -> Color(0xFF1976D2).copy(alpha = 0.3f)
                                    day.isToday -> Color.White.copy(alpha = 0.1f)
                                    !day.isCurrentMonth -> Color.Transparent
                                    else -> Color.Transparent
                                }
                            )
                            .clickable(day.isCurrentMonth) { onDateSelect(day.date) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${day.dayOfMonth}",
                            color = when {
                                isSelected && day.isCurrentMonth -> Color.White
                                !day.isCurrentMonth -> Color.Gray.copy(alpha = 0.3f)
                                day.isToday -> Color(0xFF4CAF50)
                                hasIncident -> Color(0xFFEF5350)
                                hasAny -> Color.White
                                else -> Color.White.copy(alpha = 0.7f)
                            },
                            fontSize = 13.sp,
                            fontWeight = if (day.isToday || isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LegendItem("🔴 Intoxicación", Color(0xFFB71C1C))
            LegendItem("🟡 Alerta", Color(0xFFFFA000))
            LegendItem("🔵 Nota", Color(0xFF1976D2))
        }
    }
}

@Composable
private fun EventCard(
    event: ToxicCalendarEvent,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val typeColor = when (event.eventType) {
        "incident" -> Color(0xFFB71C1C)
        "alert" -> Color(0xFFFFA000)
        else -> Color(0xFF1976D2)
    }
    val typeIcon = when (event.eventType) {
        "incident" -> "🚨"
        "alert" -> "⚠️"
        else -> "📝"
    }
    val typeLabel = when (event.eventType) {
        "incident" -> "Intoxicación"
        "alert" -> "Alerta"
        else -> "Nota"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(typeIcon, fontSize = 24.sp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(event.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (event.description.isNotBlank()) {
                    Text(event.description, color = Color.Gray, fontSize = 12.sp, maxLines = 2)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(color = typeColor.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                        Text(typeLabel, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp, color = typeColor, fontWeight = FontWeight.Bold)
                    }
                    if (event.plantName != null) {
                        Surface(color = Color(0xFF1B5E20).copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                            Text("🌿 ${event.plantName}", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp, color = Color(0xFF4CAF50))
                        }
                    }
                }
            }
            Column {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, "Editar", tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, "Eliminar", tint = Color.Red, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddOrEditEventDialog(
    existingEvent: ToxicCalendarEvent? = null,
    initialDate: String,
    allPlants: List<PlantEntity>,
    onDismiss: () -> Unit,
    onSave: (ToxicCalendarEvent) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(existingEvent?.title ?: "") }
    var description by remember { mutableStateOf(existingEvent?.description ?: "") }
    var date by remember { mutableStateOf(existingEvent?.date ?: initialDate) }
    var eventType by remember { mutableStateOf(existingEvent?.eventType ?: "note") }
    var selectedPlantId by remember { mutableStateOf(existingEvent?.plantId) }
    var selectedPlantName by remember { mutableStateOf(existingEvent?.plantName) }
    var showPlantPicker by remember { mutableStateOf(false) }

    if (showPlantPicker) {
        var plantSearch by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPlantPicker = false },
            title = { Text("🌿 Vincular planta") },
            text = {
                Column {
                    OutlinedTextField(
                        value = plantSearch,
                        onValueChange = { plantSearch = it },
                        label = { Text("Buscar planta...") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        val filtered = if (plantSearch.isBlank()) allPlants else
                            allPlants.filter { it.commonName.contains(plantSearch, true) || it.scientificName.contains(plantSearch, true) }
                        items(filtered.take(20)) { plant ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedPlantId = plant.id
                                        selectedPlantName = plant.commonName
                                        showPlantPicker = false
                                    }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(plant.commonName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(plant.scientificName, fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                    // Opción para desvincular
                    if (selectedPlantId != null) {
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = {
                            selectedPlantId = null
                            selectedPlantName = null
                            showPlantPicker = false
                        }) { Text("❌ Quitar planta vinculada", color = Color.Red) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPlantPicker = false }) { Text("Cerrar") }
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingEvent != null) "✏️ Editar evento" else "➕ Nuevo evento", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
                // Selector de fecha
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📅 Fecha:", color = Color.White)
                    TextButton(onClick = {
                        val parts = date.split("-")
                        val y = parts.getOrNull(0)?.toIntOrNull() ?: 2024
                        val m = (parts.getOrNull(1)?.toIntOrNull() ?: 1) - 1
                        val d = parts.getOrNull(2)?.toIntOrNull() ?: 1
                        DatePickerDialog(context, { _: DatePicker, year: Int, month: Int, day: Int ->
                            date = String.format("%04d-%02d-%02d", year, month + 1, day)
                        }, y, m, d).show()
                    }) {
                        Text(try {
                            LocalDate.parse(date).format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale("es")))
                        } catch (_: Exception) { date })
                    }
                }
                // Tipo de evento
                Text("Tipo:", color = Color.White, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EventTypeChip("🚨 Intoxicación", "incident", eventType == "incident", Color(0xFFB71C1C)) { eventType = "incident" }
                    EventTypeChip("⚠️ Alerta", "alert", eventType == "alert", Color(0xFFFFA000)) { eventType = "alert" }
                    EventTypeChip("📝 Nota", "note", eventType == "note", Color(0xFF1976D2)) { eventType = "note" }
                }
                // Vincular planta
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1A1A2E))
                        .clickable { showPlantPicker = true }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (selectedPlantName != null) "🌿 $selectedPlantName" else "🌿 Vincular planta (opcional)",
                        color = if (selectedPlantName != null) Color(0xFF4CAF50) else Color.Gray,
                        fontSize = 14.sp
                    )
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.Gray,
                        modifier = Modifier.graphicsLayer { scaleX = -1f })
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(ToxicCalendarEvent(
                            id = existingEvent?.id ?: 0,
                            title = title,
                            description = description,
                            date = date,
                            plantId = selectedPlantId,
                            plantName = selectedPlantName,
                            eventType = eventType,
                            createdAt = existingEvent?.createdAt ?: System.currentTimeMillis()
                        ))
                    }
                },
                enabled = title.isNotBlank()
            ) { Text("Guardar", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun EventTypeChip(label: String, type: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        color = if (selected) color.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = if (selected) color else Color.Gray,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// UTILIDADES
// ═══════════════════════════════════════════════════════════════

private fun monthName(month: Int): String = listOf(
    "", "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
)[month]
