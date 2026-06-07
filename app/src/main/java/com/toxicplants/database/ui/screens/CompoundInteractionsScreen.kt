package com.toxicplants.database.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toxicplants.database.CompoundEntity
import com.toxicplants.database.ui.viewmodel.CompoundViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class CompoundInteraction(
    val first: CompoundEntity,
    val second: CompoundEntity,
    val type: String,
    val severity: String,
    val score: Int,
    val sharedSystems: List<String>,
    val sharedPlants: List<String>,
    val explanation: String,
    val recommendation: String,
)

private data class CompoundInteractionProfile(
    val compound: CompoundEntity,
    val systems: Set<String>,
    val plantsByKey: Map<String, String>,
    val groupKey: String,
    val toxicityScore: Int,
    val hasCardiacSignal: Boolean,
    val hasCholinergicSignal: Boolean,
    val hasAnticholinergicSignal: Boolean,
    val hasCyanogenicSignal: Boolean,
    val hasRespiratorySignal: Boolean,
    val hasHepaticSignal: Boolean,
)

private var cachedInteractionsKey: String? = null
private var cachedInteractions: List<CompoundInteraction> = emptyList()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompoundInteractionsScreen(
    viewModel: CompoundViewModel,
    onCompoundClick: (CompoundEntity) -> Unit,
    onBack: () -> Unit,
) {
    val allCompounds by viewModel.allCompounds.observeAsState(emptyList())
    val cacheKey = remember(allCompounds) { allCompounds.interactionCacheKey() }
    var interactions by remember(cacheKey) {
        mutableStateOf(if (cachedInteractionsKey == cacheKey) cachedInteractions else null)
    }

    LaunchedEffect(cacheKey) {
        interactions = when {
            allCompounds.size < 2 -> emptyList()
            cachedInteractionsKey == cacheKey -> cachedInteractions
            else -> withContext(Dispatchers.Default) { buildCompoundInteractions(allCompounds) }
                .also { computed ->
                    cachedInteractionsKey = cacheKey
                    cachedInteractions = computed
                }
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var selectedSeverity by remember { mutableStateOf("Todas") }

    val accent = Color(0xFF6A1B9A)
    val severities = listOf("Todas", "Crítica", "Alta", "Moderada", "Teórica")
    val visibleInteractions = interactions.orEmpty()
    val isAnalyzing = allCompounds.isNotEmpty() && interactions == null

    val filteredInteractions = remember(visibleInteractions, query, selectedSeverity) {
        visibleInteractions.filter { interaction ->
            val matchesSeverity = selectedSeverity == "Todas" || interaction.severity == selectedSeverity
            val q = query.trim()
            val matchesQuery = q.isBlank() ||
                    interaction.first.commonName.contains(q, ignoreCase = true) ||
                    interaction.second.commonName.contains(q, ignoreCase = true) ||
                    interaction.first.groupName.contains(q, ignoreCase = true) ||
                    interaction.second.groupName.contains(q, ignoreCase = true) ||
                    interaction.sharedPlants.any { it.contains(q, ignoreCase = true) } ||
                    interaction.sharedSystems.any { it.contains(q, ignoreCase = true) }
            matchesSeverity && matchesQuery
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Interacciones", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(
                            if (isAnalyzing) "Analizando compuestos…" else "${visibleInteractions.size} pares relevantes detectados",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.82f),
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = accent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Pares") },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Comparador") },
                )
            }

            if (allCompounds.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (isAnalyzing) {
                LoadingInteractionsMessage(compoundsCount = allCompounds.size)
            } else if (selectedTab == 0) {
                InteractionsListTab(
                    query = query,
                    onQueryChange = { query = it },
                    severities = severities,
                    selectedSeverity = selectedSeverity,
                    onSeverityChange = { selectedSeverity = it },
                    interactions = filteredInteractions,
                    totalInteractions = visibleInteractions.size,
                    onCompoundClick = onCompoundClick,
                )
            } else {
                InteractionComparatorTab(
                    compounds = allCompounds,
                    onCompoundClick = onCompoundClick,
                )
            }
        }
    }
}

@Composable
private fun LoadingInteractionsMessage(compoundsCount: Int) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CircularProgressIndicator(color = Color(0xFF6A1B9A))
                Text("Analizando interacciones", fontWeight = FontWeight.Bold, color = Color(0xFF6A1B9A))
                Text(
                    "Procesando $compoundsCount compuestos en segundo plano…",
                    fontSize = 12.sp,
                    color = Color.Gray,
                )
            }
        }
    }
}

@Composable
private fun InteractionsListTab(
    query: String,
    onQueryChange: (String) -> Unit,
    severities: List<String>,
    selectedSeverity: String,
    onSeverityChange: (String) -> Unit,
    interactions: List<CompoundInteraction>,
    totalInteractions: Int,
    onCompoundClick: (CompoundEntity) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            placeholder = { Text("Filtrar por compuesto, planta o sistema…") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(severities) { severity ->
                        FilterChip(
                            selected = selectedSeverity == severity,
                            onClick = { onSeverityChange(severity) },
                            label = { Text(severity, fontSize = 11.sp) },
                        )
                    }
                }
            }

            item {
                DisclaimerCard()
            }

            item {
                Text(
                    "${interactions.size} de $totalInteractions interacciones",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            if (interactions.isEmpty()) {
                item {
                    EmptyInteractionsMessage()
                }
            } else {
                items(interactions, key = { "${it.first.id}-${it.second.id}" }) { interaction ->
                    InteractionCard(
                        interaction = interaction,
                        onCompoundClick = onCompoundClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun InteractionComparatorTab(
    compounds: List<CompoundEntity>,
    onCompoundClick: (CompoundEntity) -> Unit,
) {
    var firstQuery by remember { mutableStateOf("") }
    var secondQuery by remember { mutableStateOf("") }
    var first by remember { mutableStateOf<CompoundEntity?>(null) }
    var second by remember { mutableStateOf<CompoundEntity?>(null) }

    val firstSuggestions = remember(compounds, firstQuery, first) {
        compoundSuggestions(compounds, firstQuery, first, second)
    }
    val secondSuggestions = remember(compounds, secondQuery, first, second) {
        compoundSuggestions(compounds, secondQuery, second, first)
    }
    val interaction = remember(first, second) {
        if (first != null && second != null && first!!.id != second!!.id) {
            analyzePair(first!!, second!!)
        } else null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DisclaimerCard()

        CompoundPicker(
            title = "Compuesto A",
            query = firstQuery,
            onQueryChange = {
                firstQuery = it
                first = null
            },
            selected = first,
            suggestions = firstSuggestions,
            onSelect = {
                first = it
                firstQuery = it.commonName
            },
        )

        CompoundPicker(
            title = "Compuesto B",
            query = secondQuery,
            onQueryChange = {
                secondQuery = it
                second = null
            },
            selected = second,
            suggestions = secondSuggestions,
            onSelect = {
                second = it
                secondQuery = it.commonName
            },
        )

        if (interaction != null) {
            Text("Resultado", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            InteractionCard(
                interaction = interaction,
                onCompoundClick = onCompoundClick,
            )
        } else {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))) {
                Column(Modifier.padding(16.dp)) {
                    Text("Selecciona dos compuestos", fontWeight = FontWeight.Bold, color = Color(0xFF6A1B9A))
                    Text(
                        "El comparador estima si comparten sistemas afectados, plantas fuente o mecanismos compatibles con una interacción aditiva o sinérgica.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun CompoundPicker(
    title: String,
    query: String,
    onQueryChange: (String) -> Unit,
    selected: CompoundEntity?,
    suggestions: List<CompoundEntity>,
    onSelect: (CompoundEntity) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, fontWeight = FontWeight.Bold, color = Color(0xFF6A1B9A))
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Nombre del compuesto…") },
            leadingIcon = { Icon(Icons.Filled.Science, contentDescription = null) },
            singleLine = true,
        )
        if (selected != null) {
            SelectedCompoundChip(selected)
        } else if (query.isNotBlank()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                suggestions.take(6).forEach { compound ->
                    CompoundSuggestionRow(compound = compound, onSelect = { onSelect(compound) })
                }
                if (suggestions.isEmpty()) {
                    Text("Sin coincidencias", color = Color.Gray, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun SelectedCompoundChip(compound: CompoundEntity) {
    val color = parseColor(compound.groupColor)
    Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(10.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(compound.commonName, fontWeight = FontWeight.Bold, color = color, fontSize = 13.sp)
            Spacer(Modifier.width(6.dp))
            Text(compound.groupName, color = Color.Gray, fontSize = 11.sp, maxLines = 1)
        }
    }
}

@Composable
private fun CompoundSuggestionRow(compound: CompoundEntity, onSelect: () -> Unit) {
    val color = parseColor(compound.groupColor)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Science, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(compound.commonName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(compound.groupName, color = Color.Gray, fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun InteractionCard(
    interaction: CompoundInteraction,
    onCompoundClick: (CompoundEntity) -> Unit,
) {
    val severityColor = severityColor(interaction.severity)
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(severityColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(severityEmoji(interaction.severity), fontSize = 22.sp)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(interaction.type, fontWeight = FontWeight.Bold, color = severityColor)
                    Text(
                        "Puntuación: ${interaction.score}",
                        fontSize = 11.sp,
                        color = Color.Gray,
                    )
                }
                SeverityChip(interaction.severity)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompoundMiniCard(
                    compound = interaction.first,
                    modifier = Modifier.weight(1f),
                    onClick = { onCompoundClick(interaction.first) },
                )
                CompoundMiniCard(
                    compound = interaction.second,
                    modifier = Modifier.weight(1f),
                    onClick = { onCompoundClick(interaction.second) },
                )
            }

            if (interaction.sharedSystems.isNotEmpty()) {
                InfoBlock(
                    label = "Sistemas compartidos",
                    value = interaction.sharedSystems.joinToString(", "),
                    color = severityColor,
                )
            }

            if (interaction.sharedPlants.isNotEmpty()) {
                InfoBlock(
                    label = "Coexposición probable en plantas",
                    value = interaction.sharedPlants.take(5).joinToString(", ") +
                            if (interaction.sharedPlants.size > 5) "…" else "",
                    color = Color(0xFF2E7D32),
                )
            }

            Text(interaction.explanation, fontSize = 13.sp, lineHeight = 18.sp)

            Surface(color = severityColor.copy(alpha = 0.10f), shape = RoundedCornerShape(10.dp)) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = severityColor, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(interaction.recommendation, fontSize = 12.sp, lineHeight = 17.sp, color = severityColor)
                }
            }
        }
    }
}

@Composable
private fun CompoundMiniCard(
    compound: CompoundEntity,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val color = parseColor(compound.groupColor)
    Surface(
        modifier = modifier.clickable { onClick() },
        color = color.copy(alpha = 0.10f),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(Modifier.padding(10.dp)) {
            Text(
                compound.commonName,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 13.sp,
            )
            Text(
                compound.groupName,
                color = Color.Gray,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (compound.molecularFormula.isNotBlank()) {
                Text(compound.molecularFormula, fontStyle = FontStyle.Italic, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun InfoBlock(label: String, value: String, color: Color) {
    Column {
        Text(label, fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold)
        Text(value, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun SeverityChip(severity: String) {
    val color = severityColor(severity)
    AssistChip(
        onClick = {},
        label = { Text(severity, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
        leadingIcon = { Text(severityEmoji(severity), fontSize = 14.sp) },
    )
}

@Composable
private fun DisclaimerCard() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Text("⚠️", fontSize = 18.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                "Estimación educativa basada en mecanismos, sistemas clínicos y plantas compartidas. No sustituye a toxicología clínica ni confirma interacciones reales en humanos.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                lineHeight = 17.sp,
            )
        }
    }
}

@Composable
private fun EmptyInteractionsMessage() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("🔎", fontSize = 34.sp)
            Text("No hay interacciones con esos filtros", fontWeight = FontWeight.Bold)
            Text("Prueba con otra severidad o borra la búsqueda.", color = Color.Gray, fontSize = 13.sp)
        }
    }
}

private fun List<CompoundEntity>.interactionCacheKey(): String = joinToString(separator = "|") { compound ->
    listOf(
        compound.id,
        compound.commonName,
        compound.groupName,
        compound.subgroup,
        compound.sourcePlants,
        compound.mechanism,
        compound.ld50,
        compound.toxicDose,
        compound.clinicalNeuro,
        compound.clinicalCardio,
        compound.clinicalDigestive,
        compound.clinicalRespiratory,
        compound.clinicalDermal,
        compound.clinicalOther,
    ).joinToString(separator = "¦").hashCode().toString()
}

private fun buildCompoundInteractions(compounds: List<CompoundEntity>): List<CompoundInteraction> {
    if (compounds.size < 2) return emptyList()

    // Precalcular una sola vez evita repetir Regex, split de plantas y análisis de texto
    // para cada par. Con 246 compuestos se pasa de miles de cálculos en UI a un cálculo
    // lineal + comparaciones simples en background.
    val profiles = compounds.map { it.toInteractionProfile() }
    val result = mutableListOf<CompoundInteraction>()

    for (i in profiles.indices) {
        for (j in i + 1 until profiles.size) {
            val interaction = analyzePair(profiles[i], profiles[j])
            if (interaction.score >= 4 && interaction.severity != "Teórica") {
                result += interaction
            }
        }
    }
    return result
        .sortedWith(
            compareByDescending<CompoundInteraction> { severityRank(it.severity) }
                .thenByDescending { it.score }
                .thenBy { it.first.commonName }
        )
        .take(250)
}

private fun analyzePair(first: CompoundEntity, second: CompoundEntity): CompoundInteraction =
    analyzePair(first.toInteractionProfile(), second.toInteractionProfile())

private fun analyzePair(first: CompoundInteractionProfile, second: CompoundInteractionProfile): CompoundInteraction {
    val firstCompound = first.compound
    val secondCompound = second.compound
    val sharedSystems = first.systems.intersect(second.systems).toList()
    val sharedPlants = first.plantsByKey.keys.intersect(second.plantsByKey.keys)
        .map { key -> first.plantsByKey[key] ?: second.plantsByKey[key] ?: key }
        .sorted()
    val sameGroup = first.groupKey == second.groupKey && first.groupKey.isNotBlank()
    val mechanismSignal = mechanismSignal(first, second)
    val toxicityScore = first.toxicityScore + second.toxicityScore

    var relationScore = 0
    relationScore += sharedSystems.size * 3
    relationScore += sharedPlants.size.coerceAtMost(4) * 2
    if (sameGroup) relationScore += 2
    if (mechanismSignal.isNotBlank()) relationScore += 3

    val hasInteractionEvidence = relationScore > 0
    val score = relationScore + if (hasInteractionEvidence) toxicityScore else 0

    val severity = when {
        !hasInteractionEvidence -> "Teórica"
        sharedSystems.any { it == "Cardiovascular" || it == "Respiratorio" } && toxicityScore >= 4 -> "Crítica"
        sharedSystems.size >= 2 && toxicityScore >= 4 -> "Crítica"
        score >= 11 -> "Alta"
        score >= 6 -> "Moderada"
        else -> "Teórica"
    }

    val type = when {
        sharedSystems.contains("Cardiovascular") -> "Sinergia cardiotóxica potencial"
        sharedSystems.contains("Respiratorio") -> "Depresión/irritación respiratoria aditiva"
        sharedSystems.contains("Neurológico") -> "Neurotoxicidad aditiva potencial"
        sharedSystems.contains("Digestivo") -> "Irritación digestiva acumulativa"
        sharedSystems.contains("Dérmico / Mucosas") -> "Irritación mucocutánea aditiva"
        mechanismSignal.isNotBlank() -> mechanismSignal
        sameGroup -> "Interacción farmacodinámica de grupo"
        sharedPlants.isNotEmpty() -> "Coexposición probable"
        else -> "Interacción teórica"
    }

    val explanation = buildExplanation(firstCompound, secondCompound, sharedSystems, sharedPlants, sameGroup, mechanismSignal)
    val recommendation = recommendationFor(severity)

    return CompoundInteraction(
        first = firstCompound,
        second = secondCompound,
        type = type,
        severity = severity,
        score = score,
        sharedSystems = sharedSystems,
        sharedPlants = sharedPlants,
        explanation = explanation,
        recommendation = recommendation,
    )
}

private fun CompoundEntity.toInteractionProfile(): CompoundInteractionProfile {
    val signalText = "$groupName $subgroup $mechanism".lowercase()
    return CompoundInteractionProfile(
        compound = this,
        systems = clinicalSystems(),
        plantsByKey = sourcePlants.toPlantMap(),
        groupKey = groupName.trim().lowercase(),
        toxicityScore = toxicityScore(),
        hasCardiacSignal = signalText.hasCardiacSignal(),
        hasCholinergicSignal = signalText.hasCholinergicSignal(),
        hasAnticholinergicSignal = signalText.hasAnticholinergicSignal(),
        hasCyanogenicSignal = signalText.hasCyanogenicSignal(),
        hasRespiratorySignal = signalText.hasRespiratorySignal(),
        hasHepaticSignal = signalText.hasHepaticSignal(),
    )
}

private fun CompoundEntity.clinicalSystems(): Set<String> = buildSet {
    if (clinicalNeuro.isNotBlank()) add("Neurológico")
    if (clinicalCardio.isNotBlank()) add("Cardiovascular")
    if (clinicalDigestive.isNotBlank()) add("Digestivo")
    if (clinicalRespiratory.isNotBlank()) add("Respiratorio")
    if (clinicalDermal.isNotBlank()) add("Dérmico / Mucosas")
    if (clinicalOther.isNotBlank()) add("Otros")
}

private fun CompoundEntity.toxicityScore(): Int {
    val text = listOf(ld50, toxicDose, mechanism, clinicalNeuro, clinicalCardio, clinicalRespiratory, clinicalOther)
        .joinToString(" ")
        .lowercase()
    return when {
        text.contains("microg") || text.contains("µg") || Regex("""<\s*1\s*mg""").containsMatchIn(text) -> 3
        text.contains("letal") || text.contains("mortal") || text.contains("muerte") || text.contains("paro") || text.contains("coma") -> 3
        Regex("""\b[1-9]\d?\s*mg/kg""").containsMatchIn(text) || text.contains("arritmia") || text.contains("convuls") -> 2
        text.contains("tóxic") || text.contains("toxic") || text.contains("irrit") -> 1
        else -> 0
    }
}

private fun intersectPlants(a: String, b: String): List<String> {
    val first = a.toPlantMap()
    val second = b.toPlantMap()
    return first.keys.intersect(second.keys)
        .map { key -> first[key] ?: second[key] ?: key }
        .sorted()
}

private fun String.toPlantMap(): Map<String, String> = split("|")
    .map { it.trim() }
    .filter { it.isNotBlank() }
    .associateBy { it.lowercase().replace(Regex("\\s+"), " ") }

private fun mechanismSignal(first: CompoundInteractionProfile, second: CompoundInteractionProfile): String = when {
    first.hasCardiacSignal && second.hasCardiacSignal -> "Mecanismos cardiotóxicos convergentes"
    first.hasCholinergicSignal && second.hasCholinergicSignal -> "Síndrome colinérgico aditivo"
    first.hasAnticholinergicSignal && second.hasAnticholinergicSignal -> "Síndrome anticolinérgico aditivo"
    (first.hasCyanogenicSignal && second.hasRespiratorySignal) ||
            (second.hasCyanogenicSignal && first.hasRespiratorySignal) -> "Hipoxia/toxicidad respiratoria combinada"
    first.hasHepaticSignal && second.hasHepaticSignal -> "Hepatotoxicidad acumulativa potencial"
    else -> ""
}

private fun String.hasCardiacSignal(): Boolean = contains("card") || contains("digital") || contains("na+/k+") || contains("canal de sodio") || contains("arrit")
private fun String.hasCholinergicSignal(): Boolean = contains("colin") && !contains("anti")
private fun String.hasAnticholinergicSignal(): Boolean = contains("anticolin") || contains("muscar")
private fun String.hasCyanogenicSignal(): Boolean = contains("cian") || contains("citocromo oxidasa")
private fun String.hasRespiratorySignal(): Boolean = contains("respirat") || contains("hipoxia") || contains("asfix")
private fun String.hasHepaticSignal(): Boolean = contains("hepato") || contains("hígado") || contains("higado")

private fun buildExplanation(
    first: CompoundEntity,
    second: CompoundEntity,
    sharedSystems: List<String>,
    sharedPlants: List<String>,
    sameGroup: Boolean,
    mechanismSignal: String,
): String {
    val parts = mutableListOf<String>()
    if (sharedSystems.isNotEmpty()) {
        parts += "Ambos compuestos registran manifestaciones en ${sharedSystems.joinToString(", ").lowercase()}, por lo que una exposición conjunta podría intensificar el cuadro clínico."
    }
    if (sharedPlants.isNotEmpty()) {
        parts += "Aparecen juntos en ${sharedPlants.take(3).joinToString(", ")}${if (sharedPlants.size > 3) "…" else ""}, indicando posible coexposición por una misma planta."
    }
    if (sameGroup) {
        parts += "Pertenecen al mismo grupo fitoquímico (${first.groupName}), lo que sugiere mecanismos o dianas toxicológicas relacionadas."
    }
    if (mechanismSignal.isNotBlank()) {
        parts += "Se detecta señal mecanística: ${mechanismSignal.lowercase()}."
    }
    if (parts.isEmpty()) {
        parts += "No se detectan coincidencias fuertes en los campos disponibles; la interacción se considera teórica y debe revisarse con fuentes toxicológicas específicas."
    }
    parts += "Revisar individualmente ${first.commonName} y ${second.commonName} antes de interpretar el riesgo."
    return parts.joinToString(" ")
}

private fun recommendationFor(severity: String): String = when (severity) {
    "Crítica" -> "Riesgo alto de potenciación. Ante exposición real con síntomas cardiovasculares, respiratorios, neurológicos intensos o ingesta accidental: contactar de inmediato con urgencias/toxicología."
    "Alta" -> "Evitar interpretar cada compuesto por separado: la coexposición puede aumentar gravedad o duración. Recomendada valoración toxicológica si hay síntomas."
    "Moderada" -> "Vigilar signos compartidos y consultar fuentes clínicas. Puede ser relevante en niños, mascotas, ancianos o exposiciones repetidas."
    else -> "Interacción inferida con datos limitados. Úsala como pista educativa, no como confirmación clínica."
}

private fun compoundSuggestions(
    compounds: List<CompoundEntity>,
    query: String,
    selected: CompoundEntity?,
    other: CompoundEntity?,
): List<CompoundEntity> {
    if (selected != null) return emptyList()
    val q = query.trim()
    val base = compounds.asSequence().filter { other == null || it.id != other.id }
    if (q.isBlank()) return base.take(8).toList()
    return base
        .filter {
            it.commonName.contains(q, ignoreCase = true) ||
                    it.groupName.contains(q, ignoreCase = true) ||
                    it.iupacName.contains(q, ignoreCase = true) ||
                    it.sourcePlants.contains(q, ignoreCase = true)
        }
        .take(8)
        .toList()
}

private fun severityRank(severity: String): Int = when (severity) {
    "Crítica" -> 4
    "Alta" -> 3
    "Moderada" -> 2
    else -> 1
}

private fun severityColor(severity: String): Color = when (severity) {
    "Crítica" -> Color(0xFFB71C1C)
    "Alta" -> Color(0xFFE65100)
    "Moderada" -> Color(0xFFF9A825)
    else -> Color(0xFF546E7A)
}

private fun severityEmoji(severity: String): String = when (severity) {
    "Crítica" -> "☠️"
    "Alta" -> "⚠️"
    "Moderada" -> "🔶"
    else -> "ℹ️"
}
