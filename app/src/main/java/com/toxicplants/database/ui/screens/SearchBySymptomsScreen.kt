package com.toxicplants.database.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.CompoundEntity
import com.toxicplants.database.ui.viewmodel.PlantViewModel
import com.toxicplants.database.ui.viewmodel.CompoundViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.toxicplants.database.ui.search.buildSearchQuery
import com.toxicplants.database.ui.search.fuzzyTextScore
import com.toxicplants.database.ui.search.multiTermFieldScore
import com.toxicplants.database.ui.search.splitSymptomTerms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

// ── Lista de síntomas comunes para autocompletado ─────────────────────────
private val SINTOMAS_COMUNES: List<String> = listOf(
    "vómitos", "náuseas", "diarrea", "diarrea con sangre",
    "convulsiones", "parálisis", "dificultad respiratoria",
    "fallo renal", "fallo hepático", "fallo cardíaco",
    "arritmia", "taquicardia", "hipotensión", "hipertensión",
    "dermatitis", "irritación cutánea", "quemaduras",
    "irritación ocular", "ceguera temporal",
    "alucinaciones", "confusión mental", "delirio",
    "somnolencia", "coma", "pérdida de conciencia",
    "salivación excesiva", "babeo", "espasmos musculares",
    "dolor abdominal", "inflamación", "edema",
    "pupilas dilatadas", "pupilas contraídas",
    "picor", "urticaria", "eritema", "ampolla",
    "fotosensibilización", "hemorragia",
    "muerte", "paro cardíaco", "paro respiratorio",
    "dolor de cabeza", "mareo", "debilidad",
    "entumecimiento", "hormigueo", "sed excesiva",
    "pérdida de apetito", "pérdida de peso",
    "ictericia", "orina oscura", "sangre en orina",
    "síndrome serotoninérgico", "depresión respiratoria",
    "shock anafiláctico", "reacción alérgica"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBySymptomsScreen(
    plantViewModel: PlantViewModel,
    compoundViewModel: CompoundViewModel,
    onPlantClick: (PlantEntity) -> Unit,
    onCompoundClick: (CompoundEntity) -> Unit,
    onBack: () -> Unit
) {
    val allPlants by plantViewModel.allPlants.observeAsState(emptyList())
    val allCompounds by compoundViewModel.allCompounds.observeAsState(emptyList())
    val colors    = MaterialTheme.colorScheme

    // ── Estado ────────────────────────────────────────────────────────────
    var symptomsQuery    by remember { mutableStateOf("") }
    var selectedToxicity by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var toxicityExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var filtersExpanded  by remember { mutableStateOf(true) }
    var showSuggestions  by remember { mutableStateOf(false) }

    // ── Datos para filtros ────────────────────────────────────────────────
    val toxicityLevels = listOf("Mortal", "Muy alto", "Alto", "Moderado", "Bajo")
    val categories by remember(allPlants) {
        derivedStateOf<List<String>> {
            allPlants.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()
        }
    }

    // ── Sugerencias de autocompletado ─────────────────────────────────────
    val allSuggestions by remember(allPlants, allCompounds) {
        derivedStateOf<List<String>> {
            val fromPlants = allPlants
                .flatMap { plant ->
                    plant.symptoms
                        .split(Regex("[,;.]"))
                        .map { it.trim().lowercase() }
                        .filter { it.length in 4..40 }
                }
            val fromCompounds = allCompounds
                .flatMap { compound ->
                    listOf(compound.mechanism, compound.clinicalNeuro, compound.clinicalCardio,
                        compound.clinicalDigestive, compound.clinicalRespiratory,
                        compound.clinicalDermal, compound.clinicalOther)
                        .filter { it.isNotBlank() }
                        .flatMap { it.split(Regex("[,;.]")) }
                        .map { it.trim().lowercase() }
                        .filter { it.length in 4..40 }
                }
            (SINTOMAS_COMUNES + fromPlants + fromCompounds).distinct().sorted()
        }
    }

    val suggestions = remember(symptomsQuery, allSuggestions) {
        if (symptomsQuery.length < 2) emptyList<String>()
        else allSuggestions
            .filter { it.contains(symptomsQuery.trim(), ignoreCase = true) && it != symptomsQuery.trim() }
            .take(6)
    }

    // ── Filtrado de resultados (búsqueda multi-síntoma tolerante) ────────
    // Se calcula en segundo plano y con debounce para evitar ANR.
    val symptomTerms = remember(symptomsQuery) { splitSymptomTerms(symptomsQuery) }
    val fallbackQuery = remember(symptomsQuery) { buildSearchQuery(symptomsQuery) }
    var filteredPlants by remember { mutableStateOf<List<PlantEntity>>(emptyList()) }
    var filteredCompounds by remember { mutableStateOf<List<CompoundEntity>>(emptyList()) }
    var isSearchingSymptoms by remember { mutableStateOf(false) }

    val activeFilters = listOfNotNull(selectedToxicity, selectedCategory).size

    LaunchedEffect(symptomTerms, fallbackQuery, selectedToxicity, selectedCategory, allPlants, allCompounds) {
        // Con la pantalla vacía no renderizamos 10.000 plantas de golpe.
        if (fallbackQuery.isBlank && activeFilters == 0) {
            filteredPlants = emptyList()
            filteredCompounds = emptyList()
            isSearchingSymptoms = false
            return@LaunchedEffect
        }

        isSearchingSymptoms = true
        delay(250)
        val plantsSnapshot = allPlants
        val compoundsSnapshot = allCompounds
        val toxicity = selectedToxicity
        val category = selectedCategory
        val terms = symptomTerms
        val query = fallbackQuery

        val result = withContext(Dispatchers.Default) {
            val plants = plantsSnapshot
                .mapNotNull { plant ->
                    currentCoroutineContext().ensureActive()
                    val matchesToxicity = toxicity == null ||
                            plant.toxicityLevel.equals(toxicity, ignoreCase = true)
                    val matchesCategory = category == null ||
                            plant.category.equals(category, ignoreCase = true)
                    if (!matchesToxicity || !matchesCategory) return@mapNotNull null

                    if (query.isBlank) {
                        return@mapNotNull 1 to plant
                    }

                    val symptomScore = multiTermFieldScore(
                        terms = terms,
                        weightedFields = listOf(
                            plant.symptoms to 8,
                            plant.toxicParts to 5,
                            plant.firstAid to 4,
                            plant.description to 3,
                            plant.commonName to 3,
                            plant.commonNames to 3,
                            plant.scientificName to 3,
                            plant.family to 2
                        )
                    )

                    val minimumMatches = if (symptomScore.totalTerms <= 1) 1 else (symptomScore.totalTerms + 1) / 2
                    if (symptomScore.matchedTerms >= minimumMatches) {
                        symptomScore.score to plant
                    } else {
                        null
                    }
                }
                .sortedByDescending { it.first }
                .take(300)
                .map { it.second }

            val compounds = if (query.isBlank) emptyList<CompoundEntity>()
            else compoundsSnapshot
                .mapNotNull { compound ->
                    currentCoroutineContext().ensureActive()
                    val symptomScore = multiTermFieldScore(
                        terms = terms,
                        weightedFields = listOf(
                            compound.clinicalNeuro to 6,
                            compound.clinicalCardio to 6,
                            compound.clinicalDigestive to 6,
                            compound.clinicalRespiratory to 6,
                            compound.clinicalDermal to 6,
                            compound.clinicalOther to 5,
                            compound.mechanism to 4,
                            compound.commonName to 4,
                            compound.groupName to 3,
                            compound.subgroup to 3,
                            compound.sourcePlants to 2
                        )
                    )
                    val nameScore = fuzzyTextScore(compound.commonName, query) * 4 +
                            fuzzyTextScore(compound.groupName, query) * 3
                    val score = symptomScore.score + nameScore
                    if (score > 0 && symptomScore.matchedTerms > 0) score to compound else null
                }
                .sortedByDescending { it.first }
                .take(120)
                .map { it.second }

            SymptomSearchResultSet(plants, compounds)
        }

        filteredPlants = result.plants
        filteredCompounds = result.compounds
        isSearchingSymptoms = false
    }

    // ── UI ────────────────────────────────────────────────────────────────
    Column(modifier = Modifier.fillMaxSize()) {

        // TopBar
        Surface(modifier = Modifier.fillMaxWidth(), color = colors.tertiary) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = colors.onTertiary
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    "Buscar por síntomas",
                    color      = colors.onTertiary,
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.weight(1f)
                )
                if (activeFilters > 0) {
                    Badge(containerColor = MaterialTheme.colorScheme.surface) {
                        Text("$activeFilters", color = colors.tertiary, fontSize = 11.sp)
                    }
                    Spacer(Modifier.width(4.dp))
                }
                TextButton(onClick = { filtersExpanded = !filtersExpanded }) {
                    Text(
                        if (filtersExpanded) "Ocultar" else "Filtros",
                        color    = colors.onTertiary,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Panel de filtros + buscador
        AnimatedVisibility(visible = filtersExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.primaryContainer.copy(alpha = 0.25f))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                // ── Campo de búsqueda con autocompletado ──────────────────
                Box {
                    OutlinedTextField(
                        value         = symptomsQuery,
                        onValueChange = { q ->
                            symptomsQuery   = q
                            showSuggestions = q.length >= 2
                        },
                        modifier    = Modifier.fillMaxWidth(),
                        placeholder = { Text("Escribe un síntoma, planta o parte tóxica…") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (symptomsQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    symptomsQuery   = ""
                                    showSuggestions = false
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                                }
                            }
                        },
                        singleLine = true,
                        colors     = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = colors.tertiary,
                            cursorColor          = colors.tertiary
                        )
                    )
                }

                Text(
                    "Puedes combinar síntomas: vómitos + taquicardia + pupilas dilatadas",
                    fontSize = 11.sp,
                    color = colors.onSurfaceVariant
                )

                // ── Sugerencias de autocompletado ─────────────────────────
                AnimatedVisibility(
                    visible = showSuggestions && suggestions.isNotEmpty(),
                    enter   = fadeIn(),
                    exit    = fadeOut()
                ) {
                    Card(
                        modifier  = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(8.dp),
                        shape     = RoundedCornerShape(12.dp)
                    ) {
                        Column {
                            suggestions.forEachIndexed { index, suggestion ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            symptomsQuery   = suggestion
                                            showSuggestions = false
                                        }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🔍", fontSize = 14.sp)
                                    Spacer(Modifier.width(10.dp))
                                    // Resalta la parte que coincide
                                    val idx = suggestion.indexOf(
                                        symptomsQuery.trim(), ignoreCase = true
                                    )
                                    if (idx >= 0) {
                                        Text(
                                            suggestion.substring(0, idx),
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            suggestion.substring(idx, idx + symptomsQuery.trim().length),
                                            fontSize   = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color      = colors.tertiary
                                        )
                                        Text(
                                            suggestion.substring(idx + symptomsQuery.trim().length),
                                            fontSize = 14.sp
                                        )
                                    } else {
                                        Text(suggestion, fontSize = 14.sp)
                                    }
                                }
                                if (index < suggestions.lastIndex) {
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                                }
                            }
                        }
                    }
                }

                // ── Chips de síntomas comunes rápidos ─────────────────────
                if (symptomsQuery.isBlank()) {
                    Text(
                        "Síntomas frecuentes:",
                        fontSize   = 11.sp,
                        color      = colors.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    val quickSymptoms = listOf(
                        "vómitos", "convulsiones", "alucinaciones",
                        "dermatitis", "irritación", "parálisis", "muerte"
                    )
                    Row(
                        modifier             = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickSymptoms.take(4).forEach { s ->
                            FilterChip(
                                selected = false,
                                onClick  = {
                                    symptomsQuery   = s
                                    showSuggestions = false
                                },
                                label    = { Text(s, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(
                        modifier             = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickSymptoms.drop(4).forEach { s ->
                            FilterChip(
                                selected = false,
                                onClick  = {
                                    symptomsQuery   = s
                                    showSuggestions = false
                                },
                                label    = { Text(s, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Spacer para alinear si son impares
                        if (quickSymptoms.drop(4).size < 4) {
                            repeat(4 - quickSymptoms.drop(4).size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }

                // ── Filtros Nivel | Categoría ──────────────────────────────
                Row(
                    modifier             = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Toxicidad
                    ExposedDropdownMenuBox(
                        expanded        = toxicityExpanded,
                        onExpandedChange = { toxicityExpanded = !toxicityExpanded },
                        modifier        = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value         = selectedToxicity ?: "",
                            onValueChange = {},
                            readOnly      = true,
                            modifier      = Modifier.menuAnchor().fillMaxWidth(),
                            textStyle     = LocalTextStyle.current.copy(fontSize = 13.sp),
                            trailingIcon  = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = toxicityExpanded)
                            },
                            colors      = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.tertiary
                            ),
                            placeholder = { Text("Nivel", fontSize = 13.sp) }
                        )
                        ExposedDropdownMenu(
                            expanded        = toxicityExpanded,
                            onDismissRequest = { toxicityExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text    = { Text("Todos", fontSize = 13.sp) },
                                onClick = { selectedToxicity = null; toxicityExpanded = false }
                            )
                            toxicityLevels.forEach { level ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(getToxicityEmoji(level), fontSize = 14.sp)
                                            Spacer(Modifier.width(6.dp))
                                            Text(level, fontSize = 13.sp)
                                        }
                                    },
                                    onClick = { selectedToxicity = level; toxicityExpanded = false }
                                )
                            }
                        }
                    }

                    // Categoría
                    ExposedDropdownMenuBox(
                        expanded        = categoryExpanded,
                        onExpandedChange = { categoryExpanded = !categoryExpanded },
                        modifier        = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value         = selectedCategory ?: "",
                            onValueChange = {},
                            readOnly      = true,
                            modifier      = Modifier.menuAnchor().fillMaxWidth(),
                            textStyle     = LocalTextStyle.current.copy(fontSize = 13.sp),
                            trailingIcon  = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                            },
                            colors      = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.tertiary
                            ),
                            placeholder = { Text("Tipo", fontSize = 13.sp) }
                        )
                        ExposedDropdownMenu(
                            expanded        = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text    = { Text("Todos", fontSize = 13.sp) },
                                onClick = { selectedCategory = null; categoryExpanded = false }
                            )
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text    = { Text(category, fontSize = 13.sp) },
                                    onClick = { selectedCategory = category; categoryExpanded = false }
                                )
                            }
                        }
                    }
                }

                // Botón limpiar filtros
                if (symptomsQuery.isNotEmpty() || activeFilters > 0) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = {
                            symptomsQuery    = ""
                            selectedToxicity = null
                            selectedCategory = null
                            showSuggestions  = false
                        }) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Limpiar todo", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Contador de resultados
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color    = colors.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Text(
                if (isSearchingSymptoms) "🔎 Buscando coincidencias…"
                else "📋 ${filteredPlants.size} plantas ${if (filteredCompounds.isNotEmpty()) "y ${filteredCompounds.size} componentes" else ""}",
                modifier   = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                fontWeight = FontWeight.Medium,
                fontSize   = 14.sp,
                color      = colors.onBackground
            )
        }

        // Lista de resultados
        when {
            isSearchingSymptoms && filteredPlants.isEmpty() && filteredCompounds.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = colors.tertiary)
                        Spacer(Modifier.height(8.dp))
                        Text("Buscando…", color = colors.onSurfaceVariant)
                    }
                }
            }
            filteredPlants.isNotEmpty() || filteredCompounds.isNotEmpty() -> {
                LazyColumn(
                    modifier        = Modifier.fillMaxSize(),
                    contentPadding  = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (filteredPlants.isNotEmpty()) {
                        item {
                            Text(
                                "🌿 Plantas relacionadas",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(filteredPlants) { plant ->
                            CompactPlantCard(
                                plant   = plant,
                                query   = symptomsQuery,
                                onClick = { onPlantClick(plant) }
                            )
                        }
                    }

                    if (filteredCompounds.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "🧪 Componentes responsables",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(filteredCompounds) { compound ->
                            CompactCompoundCard(
                                compound = compound,
                                query    = symptomsQuery,
                                onClick   = { onCompoundClick(compound) }
                            )
                        }
                    }
                }
            }
            symptomsQuery.isNotEmpty() || activeFilters > 0 -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔍", fontSize = 50.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Sin resultados", fontWeight = FontWeight.Bold)
                        Text("Prueba con otros términos", color = colors.onSurfaceVariant)
                    }
                }
            }
            else -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("💊", fontSize = 48.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Escribe un síntoma o pulsa\nuno de los accesos rápidos",
                            color     = colors.onSurfaceVariant,
                            fontSize  = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

private data class SymptomSearchResultSet(
    val plants: List<PlantEntity>,
    val compounds: List<CompoundEntity>
)

// ── Tarjeta de planta compacta con resaltado del término buscado ──────────
@Composable
fun CompactPlantCard(
    plant: PlantEntity,
    onClick: () -> Unit,
    query: String = "",
    flowerColor: String? = null,
    fruitColor: String? = null
) {
    val colors = MaterialTheme.colorScheme
    val toxicityColor = when (plant.toxicityLevel) {
        "Mortal"   -> colors.error
        "Alto"     -> Color(0xFFE65100)
        "Muy alto" -> Color(0xFFFF5722)
        "Moderado" -> Color(0xFFF57C00)
        "Bajo"     -> colors.primary
        else       -> colors.onSurfaceVariant
    }

    Card(
        modifier  = Modifier.fillMaxWidth().clickable { onClick() },
        colors    = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape     = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                when (plant.toxicityLevel) {
                    "Mortal"   -> "💀"
                    "Muy alto" -> "☠️"
                    "Alto"     -> "⚠️"
                    "Moderado" -> "⚡"
                    "Bajo"     -> "🟢"
                    else       -> "ℹ️"
                },
                fontSize = 18.sp,
                modifier = Modifier.width(28.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        plant.commonName,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 14.sp,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                        modifier   = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(6.dp))
                    Surface(color = toxicityColor.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                        Text(
                            plant.toxicityLevel,
                            modifier   = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            fontSize   = 9.sp,
                            color      = toxicityColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    plant.scientificName,
                    color     = colors.onSurfaceVariant,
                    fontStyle = FontStyle.Italic,
                    maxLines  = 1,
                    overflow  = TextOverflow.Ellipsis,
                    fontSize  = 11.sp
                )
                if (plant.symptoms.isNotBlank()) {
                    val q = query.trim()
                    if (q.isNotBlank()) {
                        val symptomText = plant.symptoms
                        val idx = symptomText.indexOf(q, ignoreCase = true)
                        if (idx >= 0) {
                            val start  = maxOf(0, idx - 15)
                            val end    = minOf(symptomText.length, idx + q.length + 40)
                            val prefix = if (start > 0) "…" else ""
                            val suffix = if (end < symptomText.length) "…" else ""
                            val snippet = prefix + symptomText.substring(start, end) + suffix
                            Row {
                                val snipIdx = snippet.indexOf(q, ignoreCase = true)
                                if (snipIdx >= 0) {
                                    Text(snippet.substring(0, snipIdx), fontSize = 11.sp, color = Color(0xFF888888), maxLines = 1)
                                    Text(snippet.substring(snipIdx, snipIdx + q.length), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.tertiary, maxLines = 1)
                                    Text(snippet.substring(snipIdx + q.length), fontSize = 11.sp, color = Color(0xFF888888), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                } else {
                                    Text(snippet, fontSize = 11.sp, color = Color(0xFF888888), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        } else {
                            Text(plant.symptoms, color = Color(0xFF888888), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp)
                        }
                    } else {
                        Text(plant.symptoms, color = Color(0xFF888888), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp)
                    }
                }
                if (!flowerColor.isNullOrBlank() || !fruitColor.isNullOrBlank()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 2.dp)) {
                        if (!flowerColor.isNullOrBlank()) {
                            Surface(color = Color(0xFFF8BBD0).copy(alpha = 0.5f), shape = RoundedCornerShape(4.dp)) {
                                Text("🌸 $flowerColor", modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp), fontSize = 9.sp, color = Color(0xFF880E4F))
                            }
                        }
                        if (!fruitColor.isNullOrBlank()) {
                            Surface(color = Color(0xFFC8E6C9).copy(alpha = 0.5f), shape = RoundedCornerShape(4.dp)) {
                                Text("🍒 $fruitColor", modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp), fontSize = 9.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompactCompoundCard(
    compound: CompoundEntity,
    onClick: () -> Unit,
    query: String = ""
) {
    val colors = MaterialTheme.colorScheme
    val mainColor = Color(android.graphics.Color.parseColor(compound.groupColor))

    Card(
        modifier  = Modifier.fillMaxWidth().clickable { onClick() },
        colors    = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape     = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🧪", fontSize = 18.sp, modifier = Modifier.width(28.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        compound.commonName,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 14.sp,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                        modifier   = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(6.dp))
                    Surface(color = mainColor.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                        Text(
                            compound.subgroup,
                            modifier   = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            fontSize   = 9.sp,
                            color      = mainColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    compound.groupName,
                    color    = colors.onSurfaceVariant,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val fullText = "${compound.mechanism} ${compound.clinicalNeuro} ${compound.clinicalCardio} ${compound.clinicalDigestive}".trim()
                val q = query.trim()
                if (q.isNotBlank()) {
                    val idx = fullText.indexOf(q, ignoreCase = true)
                    if (idx >= 0) {
                        val start  = maxOf(0, idx - 15)
                        val end    = minOf(fullText.length, idx + q.length + 40)
                        val prefix = if (start > 0) "…" else ""
                        val suffix = if (end < fullText.length) "…" else ""
                        val snippet = prefix + fullText.substring(start, end) + suffix
                        Row {
                            val snipIdx = snippet.indexOf(q, ignoreCase = true)
                            if (snipIdx >= 0) {
                                Text(snippet.substring(0, snipIdx), fontSize = 11.sp, color = Color(0xFF888888), maxLines = 1)
                                Text(snippet.substring(snipIdx, snipIdx + q.length), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.tertiary, maxLines = 1)
                                Text(snippet.substring(snipIdx + q.length), fontSize = 11.sp, color = Color(0xFF888888), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            } else {
                                Text(snippet, fontSize = 11.sp, color = Color(0xFF888888), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    } else {
                        Text(compound.mechanism, color = Color(0xFF888888), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp)
                    }
                } else {
                    Text(compound.mechanism, color = Color(0xFF888888), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp)
                }
            }
        }
    }
}

private fun getToxicityEmoji(level: String): String = when (level.lowercase()) {
    "mortal"   -> "💀"
    "muy alto" -> "☠️"
    "alto"     -> "⚠️"
    "moderado" -> "⚡"
    "bajo"     -> "🟢"
    else       -> "❓"
}
