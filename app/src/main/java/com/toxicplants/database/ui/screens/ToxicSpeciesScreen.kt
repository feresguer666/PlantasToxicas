package com.toxicplants.database.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.PoisonousFamilyCatalogType
import com.toxicplants.database.PoisonousFamilyGenusEntity
import com.toxicplants.database.ui.gbif.GBIFSpeciesMatch
import com.toxicplants.database.ui.gbif.GBIFViewModel
import com.toxicplants.database.ui.gbif.SearchResultState
import com.toxicplants.database.ui.viewmodel.PlantViewModel
import com.toxicplants.database.ui.viewmodel.PoisonousFamilyViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

private val ToxicSpeciesBg = Brush.verticalGradient(
    listOf(Color(0xFF050B06), Color(0xFF0A1A0C), Color(0xFF102615))
)
private val ToxicSpeciesTopBar = Color(0xFF102A13)

private val OriginBd = Color(0xFF2E7D32)
private val OriginWiki = Color(0xFF1565C0)
private val OriginGbif = Color(0xFFEF6C00)

private data class SpeciesDisplayItem(
    val scientificName: String,
    val source: String,
    val localPlant: PlantEntity?,
)

private val wikiClient = OkHttpClient.Builder()
    .connectTimeout(20, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .build()

private fun localGenusName(scientificName: String): String =
    scientificName
        .trim()
        .split(Regex("\\s+"))
        .firstOrNull()
        ?.trim(' ', ',', '.', ';', ':', '(', ')', '[', ']')
        .orEmpty()

private fun wikipediaSpeciesTitle(scientificName: String): String {
    val cleaned = scientificName.trim().replace(Regex("\\s+"), " ")
    val parts = cleaned.split(" ").filter { it.isNotBlank() }
    if (parts.size >= 2) {
        val genus = parts[0].trim(' ', ',', '.', ';', ':', '(', ')', '[', ']')
        val species = parts[1].trim(' ', ',', '.', ';', ':', '(', ')', '[', ']')
        if (genus.isNotBlank() && species.isNotBlank() &&
            !species.equals("sp.", true) && !species.equals("spp.", true)
        ) return "$genus $species"
    }
    return cleaned
}

private fun normSpeciesName(value: String): String = wikipediaSpeciesTitle(value)
    .trim()
    .lowercase()
    .replace(Regex("\\s+"), " ")

private fun wikiSpeciesCacheFile(context: android.content.Context): File =
    File(context.filesDir, "wiki_species_cache.json")

private fun loadCachedWikiSpecies(
    context: android.content.Context,
    genusName: String
): List<String>? {
    return runCatching {
        val file = wikiSpeciesCacheFile(context)
        if (!file.exists()) return null
        val root = JSONObject(file.readText(Charsets.UTF_8))
        val arr = root.optJSONArray(genusName.lowercase()) ?: return null
        List(arr.length()) { idx -> arr.optString(idx) }.filter { it.isNotBlank() }
    }.getOrNull()
}

private fun saveCachedWikiSpecies(
    context: android.content.Context,
    genusName: String,
    species: List<String>
) {
    runCatching {
        val file = wikiSpeciesCacheFile(context)
        val root = if (file.exists()) JSONObject(file.readText(Charsets.UTF_8)) else JSONObject()
        val arr = JSONArray()
        species.forEach { arr.put(it) }
        root.put(genusName.lowercase(), arr)
        file.writeText(root.toString(), Charsets.UTF_8)
    }
}

private suspend fun fetchWikiSpeciesFixed(
    context: android.content.Context,
    genusName: String
): List<String> =
    withContext(Dispatchers.IO) {
        loadCachedWikiSpecies(context, genusName)?.let { return@withContext it }

        val encoded = URLEncoder.encode(genusName, "UTF-8")
        val candidates = listOf(
            "https://species.wikimedia.org/w/api.php?action=parse&page=$encoded&prop=wikitext&format=json&redirects=1",
            "https://es.wikipedia.org/w/api.php?action=parse&page=$encoded&prop=wikitext&format=json&redirects=1",
            "https://en.wikipedia.org/w/api.php?action=parse&page=$encoded&prop=wikitext&format=json&redirects=1"
        )

        val results = linkedSetOf<String>()
        val regex =
            Regex("\\b${Regex.escape(genusName)}\\s+[a-z][a-z-]+(?:\\s+(?:subsp\\.|var\\.)\\s+[a-z-]+)?\\b")

        for (url in candidates) {
            val text = runCatching {
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", "PlantasToxicas Android; fixed wiki species list")
                    .build()
                wikiClient.newCall(req).execute().use { response ->
                    if (!response.isSuccessful) return@use ""
                    val body = response.body?.string().orEmpty()
                    JSONObject(body)
                        .optJSONObject("parse")
                        ?.optJSONObject("wikitext")
                        ?.optString("*")
                        .orEmpty()
                }
            }.getOrDefault("")

            if (text.isNotBlank()) {
                regex.findAll(text).forEach { m ->
                    val name = wikipediaSpeciesTitle(m.value)
                    val parts = name.split(" ")
                    if (parts.size >= 2 && parts[1].length > 2) results += name
                }
            }
            if (results.size >= 5) break
        }

        val fixed = results
            .distinctBy { normSpeciesName(it) }
            .sortedBy { it.lowercase() }

        saveCachedWikiSpecies(context, genusName, fixed)
        fixed
    }

private suspend fun fetchGbifSpeciesByGenus(genusName: String): List<String> =
    withContext(Dispatchers.IO) {
        runCatching {
            val encoded = URLEncoder.encode(genusName, "UTF-8")
            val request = Request.Builder()
                .url("https://api.gbif.org/v1/species/search?rank=SPECIES&q=$encoded&limit=30")
                .header("User-Agent", "PlantasToxicas Android; gbif genus species")
                .build()

            wikiClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList<String>()
                val body = response.body?.string().orEmpty()
                val json = JSONObject(body)
                val results = json.optJSONArray("results") ?: return@withContext emptyList<String>()
                val names = mutableListOf<String>()

                for (i in 0 until results.length()) {
                    val obj = results.optJSONObject(i) ?: continue
                    val genus = obj.optString("genus")
                    val species = obj.optString("species")
                    val rank = obj.optString("rank")
                    if (
                        rank.equals("SPECIES", true) &&
                        genus.equals(genusName, true) &&
                        species.isNotBlank()
                    ) {
                        names += wikipediaSpeciesTitle(species)
                    }
                }

                names
                    .distinctBy { normSpeciesName(it) }
                    .sortedBy { it.lowercase() }
            }
        }.getOrElse { emptyList() }
    }

private fun sourceColor(source: String): Color = when (source.uppercase()) {
    "BD" -> OriginBd
    "WIKI" -> OriginWiki
    "GBIF" -> OriginGbif
    else -> Color.Gray
}

private fun buildPlantFromExternalSpecies(
    genus: PoisonousFamilyGenusEntity,
    scientificName: String,
    sourceLabel: String
): PlantEntity {
    val cleanName = wikipediaSpeciesTitle(scientificName)
    return PlantEntity(
        id = 0,
        commonName = cleanName,
        commonNames = "",
        scientificName = cleanName,
        family = genus.familyName,
        toxicityLevel = if (genus.catalogType == PoisonousFamilyCatalogType.ALL) "Alto" else "Moderado",
        toxicParts = genus.toxicParts,
        symptoms = genus.symptoms,
        description = listOf(
            "Ficha creada desde pestaña $sourceLabel del género.",
            "Género: ${genus.genusName}",
            "Familia: ${genus.familyName}",
            "Toxinas/principios: ${genus.toxins}",
            "Notas: ${genus.notes}"
        ).joinToString("\n\n"),
        habitat = "",
        geographicDistribution = "",
        firstAid = "",
        imageUrl = "",
        isFavorite = false,
        category = "Especies tóxicas",
        latitude = null,
        longitude = null,
        locationName = null,
        foundDate = null,
        notes = genus.notes,
        floweringMonths = "",
        fruitingMonths = "",
        maxToxicityMonths = "",
        mythsAndLegends = ""
    )
}

private fun speciesSelectionKey(item: SpeciesDisplayItem): String =
    "${item.source}|${normSpeciesName(item.scientificName)}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToxicSpeciesScreen(
    plantViewModel: PlantViewModel,
    poisonousFamilyViewModel: PoisonousFamilyViewModel,
    onBack: () -> Unit,
    onPlantClick: (PlantEntity) -> Unit,
) {
    val allPlants by plantViewModel.allPlants.observeAsState(emptyList())
    val allGenera by poisonousFamilyViewModel.allGenera.observeAsState(emptyList())

    var selectedFamily by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedGenusId by rememberSaveable { mutableStateOf<Int?>(null) }
    var editMode by rememberSaveable { mutableStateOf(false) }

    val selectedGenus = remember(selectedGenusId, allGenera) {
        selectedGenusId?.let { id: Int ->
            allGenera.firstOrNull { genus: PoisonousFamilyGenusEntity -> genus.id == id }
        }
    }

    val familyListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val generaListState = rememberSaveable(
        selectedFamily ?: "__none__",
        saver = LazyListState.Saver
    ) { LazyListState() }

    val families = remember(allGenera) {
        allGenera
            .groupBy { genus: PoisonousFamilyGenusEntity -> genus.familyName.trim() }
            .filterKeys { familyName: String -> familyName.isNotBlank() }
            .mapValues { entry ->
                val items: List<PoisonousFamilyGenusEntity> = entry.value
                val generaCount = items
                    .map { genus: PoisonousFamilyGenusEntity -> genus.genusName.trim().lowercase() }
                    .filter { genusName: String -> genusName.isNotBlank() }
                    .distinct()
                    .size
                val speciesCount = items.sumOf { genus: PoisonousFamilyGenusEntity ->
                    genus.genusSpeciesCount.coerceAtLeast(0)
                }
                generaCount to speciesCount
            }
            .toSortedMap(String.CASE_INSENSITIVE_ORDER)
    }

    val title = when {
        selectedGenus != null -> selectedGenus.genusName
        selectedFamily != null -> selectedFamily!!
        else -> "🧪 Especies tóxicas"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        title,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        when {
                            selectedGenusId != null -> selectedGenusId = null
                            selectedFamily != null -> selectedFamily = null
                            else -> onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { editMode = !editMode }) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = if (editMode) "Salir edición" else "Editar",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ToxicSpeciesTopBar,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(ToxicSpeciesBg)
        ) {
            when {
                selectedGenus != null -> ToxicSpeciesByGenus(
                    plantViewModel = plantViewModel,
                    poisonousFamilyViewModel = poisonousFamilyViewModel,
                    genus = selectedGenus,
                    allPlants = allPlants,
                    onPlantClick = onPlantClick,
                    editMode = editMode,
                    onGenusDeleted = {
                        selectedGenusId = null
                    }
                )

                selectedFamily != null -> ToxicGeneraByFamily(
                    familyName = selectedFamily!!,
                    listState = generaListState,
                    allGenera = allGenera,
                    allPlants = allPlants,
                    poisonousFamilyViewModel = poisonousFamilyViewModel,
                    editMode = editMode,
                    onGenusClick = { genus: PoisonousFamilyGenusEntity ->
                        selectedGenusId = genus.id
                    },
                    onPlantClick = onPlantClick
                )

                else -> ToxicSpeciesFamilies(
                    families = families,
                    allGenera = allGenera,
                    allPlants = allPlants,
                    listState = familyListState,
                    onFamilyClick = { familyName: String ->
                        selectedFamily = familyName
                        selectedGenusId = null
                    },
                    onGenusClick = { genus: PoisonousFamilyGenusEntity ->
                        selectedGenusId = genus.id
                    },
                    onPlantClick = onPlantClick
                )
            }
        }
    }
}

@Composable
private fun ToxicSpeciesFamilies(
    families: Map<String, Pair<Int, Int>>,
    allGenera: List<PoisonousFamilyGenusEntity>,
    allPlants: List<PlantEntity>,
    listState: LazyListState,
    onFamilyClick: (String) -> Unit,
    onGenusClick: (PoisonousFamilyGenusEntity) -> Unit,
    onPlantClick: (PlantEntity) -> Unit,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val matchingFamilies = remember(searchQuery, families) {
        if (searchQuery.isBlank()) emptyList()
        else families.entries.filter { it.key.contains(searchQuery, ignoreCase = true) }
    }
    val matchingGenera = remember(searchQuery, allGenera) {
        if (searchQuery.isBlank()) emptyList()
        else allGenera.filter {
            it.genusName.contains(searchQuery, ignoreCase = true) ||
            it.familyName.contains(searchQuery, ignoreCase = true) ||
            it.toxins.contains(searchQuery, ignoreCase = true)
        }.distinctBy { it.genusName.lowercase() }.sortedBy { it.genusName.lowercase() }
    }
    val matchingPlants = remember(searchQuery, allPlants) {
        if (searchQuery.isBlank()) emptyList()
        else allPlants.filter {
            it.scientificName.contains(searchQuery, ignoreCase = true) ||
            it.commonName.contains(searchQuery, ignoreCase = true) || it.commonNames.contains(searchQuery, ignoreCase = true) ||
            it.family.contains(searchQuery, ignoreCase = true)
        }.sortedBy { it.scientificName.lowercase() }
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111A12)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Familias con especies tóxicas",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Explora familias, géneros y especies marcadas con BD, Wiki y GBIF.",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar familia, género o especie...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = Color.White)
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFA5D6A7),
                    unfocusedBorderColor = Color(0xFF2E7D32)
                ),
                shape = RoundedCornerShape(14.dp)
            )
        }

        if (searchQuery.isBlank()) {
            items(families.entries.toList(), key = { entry -> "fam_all_" + entry.key }) { entry ->
                val (generaCount, speciesCount) = entry.value
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onFamilyClick(entry.key) },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF162019)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🧬", fontSize = 26.sp)
                        Spacer(Modifier.size(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                entry.key,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                "$generaCount géneros · $speciesCount especies/registros",
                                color = Color(0xFFA5D6A7),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        } else {
            if (matchingFamilies.isEmpty() && matchingGenera.isEmpty() && matchingPlants.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF162019)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "No se encontraron resultados para: $searchQuery",
                            color = Color.LightGray,
                            modifier = Modifier.padding(16.dp),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            if (matchingFamilies.isNotEmpty()) {
                item {
                    Text(
                        text = "Familias (${matchingFamilies.size})",
                        color = Color(0xFFA5D6A7),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }
                items(matchingFamilies, key = { entry -> "fam_s_" + entry.key }) { entry ->
                    val (generaCount, speciesCount) = entry.value
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onFamilyClick(entry.key) },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF162019)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🧬", fontSize = 26.sp)
                            Spacer(Modifier.size(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    entry.key,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                Text(
                                    "$generaCount géneros · $speciesCount especies/registros",
                                    color = Color(0xFFA5D6A7),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            if (matchingGenera.isNotEmpty()) {
                item {
                    Text(
                        text = "Géneros (${matchingGenera.size})",
                        color = Color(0xFFA5D6A7),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                }
                items(matchingGenera, key = { genus -> "gen_s_" + genus.id + "_" + genus.genusName }) { genus ->
                    val localCount = allPlants.count { plant ->
                        localGenusName(plant.scientificName).equals(genus.genusName, ignoreCase = true)
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onGenusClick(genus) },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF162019)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🌿", fontSize = 26.sp)
                            Spacer(Modifier.size(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    genus.genusName,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                Text(
                                    "Familia: ${genus.familyName} · ${genus.genusSpeciesCount.coerceAtLeast(0)} esp. · $localCount en BD",
                                    color = Color(0xFFA5D6A7),
                                    fontSize = 13.sp
                                )
                                if (genus.toxins.isNotBlank()) {
                                    Text(
                                        genus.toxins,
                                        color = Color.LightGray,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (matchingPlants.isNotEmpty()) {
                item {
                    Text(
                        text = "Especies en tu BD (${matchingPlants.size})",
                        color = Color(0xFFA5D6A7),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                }
                items(matchingPlants, key = { plant -> "plt_s_" + plant.id }) { plant ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlantClick(plant) },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF162019)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🌱", fontSize = 26.sp)
                            Spacer(Modifier.size(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    plant.scientificName,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                val commons = listOf(plant.commonName, plant.commonNames).filter { it.isNotBlank() }.joinToString(", ")
                                if (commons.isNotBlank()) {
                                    Text(
                                        commons,
                                        color = Color(0xFFA5D6A7),
                                        fontSize = 13.sp
                                    )
                                }
                                Text(
                                    "Familia: ${plant.family} · Género: ${localGenusName(plant.scientificName)}",
                                    color = Color.LightGray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToxicGeneraByFamily(
    familyName: String,
    listState: LazyListState,
    allGenera: List<PoisonousFamilyGenusEntity>,
    allPlants: List<PlantEntity>,
    poisonousFamilyViewModel: PoisonousFamilyViewModel,
    editMode: Boolean,
    onGenusClick: (PoisonousFamilyGenusEntity) -> Unit,
    onPlantClick: (PlantEntity) -> Unit,
) {
    val context = LocalContext.current

    var genusSearchQuery by rememberSaveable { mutableStateOf("") }

    var genusToDelete by remember { mutableStateOf<PoisonousFamilyGenusEntity?>(null) }
    var genusToEdit by remember { mutableStateOf<PoisonousFamilyGenusEntity?>(null) }

    val genera = remember(familyName, allGenera) {
        allGenera
            .filter { genus: PoisonousFamilyGenusEntity ->
                genus.familyName.equals(familyName, ignoreCase = true)
            }
            .distinctBy { genus: PoisonousFamilyGenusEntity ->
                genus.genusName.lowercase()
            }
            .sortedBy { genus: PoisonousFamilyGenusEntity ->
                genus.genusName.lowercase()
            }
    }

    val displayedGenera = remember(genusSearchQuery, genera) {
        if (genusSearchQuery.isBlank()) genera
        else genera.filter {
            it.genusName.contains(genusSearchQuery, ignoreCase = true) ||
            it.toxins.contains(genusSearchQuery, ignoreCase = true)
        }
    }

    if (genusToDelete != null) {
        ConfirmDeleteDialog(
            title = "Eliminar género",
            text = "¿Seguro que quieres eliminar el género ${genusToDelete?.genusName} de la familia ${genusToDelete?.familyName}?",
            onConfirm = {
                genusToDelete?.let { genus ->
                    poisonousFamilyViewModel.deleteGenus(genus)
                    Toast.makeText(
                        context,
                        "Género eliminado: ${genus.genusName}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                genusToDelete = null
            },
            onDismiss = { genusToDelete = null }
        )
    }

    genusToEdit?.let { genus ->
        EditGenusDialog(
            genus = genus,
            onDismiss = { genusToEdit = null },
            onSave = { updated: PoisonousFamilyGenusEntity ->
                poisonousFamilyViewModel.saveGenus(updated)
                Toast.makeText(
                    context,
                    "Género guardado: ${updated.genusName}",
                    Toast.LENGTH_SHORT
                ).show()
                genusToEdit = null
            }
        )
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "$familyName · ${genera.size} géneros",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                if (editMode) {
                    Text(
                        "Modo edición activo: puedes editar o borrar géneros.",
                        color = Color(0xFFFFCC80),
                        fontSize = 12.sp
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = genusSearchQuery,
                onValueChange = { genusSearchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar género o especie en $familyName...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White) },
                trailingIcon = {
                    if (genusSearchQuery.isNotBlank()) {
                        IconButton(onClick = { genusSearchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = Color.White)
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFA5D6A7),
                    unfocusedBorderColor = Color(0xFF2E7D32)
                ),
                shape = RoundedCornerShape(14.dp)
            )
        }

        items(displayedGenera, key = { genus -> genus.id.toString() + "_" + genus.genusName }) { genus ->
            val localCount = allPlants.count { plant: PlantEntity ->
                localGenusName(plant.scientificName).equals(genus.genusName, ignoreCase = true)
            }

            var reviewStatus by remember(genus.id, genus.familyName, genus.genusName) {
                mutableStateOf(
                    ToxicSpeciesReviewStore.getGenusStatus(
                        context,
                        genus.familyName,
                        genus.genusName
                    )
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF162019)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                genus.genusName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                "${genus.genusSpeciesCount.coerceAtLeast(0)} especies posibles · $localCount en tu BD",
                                color = Color(0xFFA5D6A7),
                                fontSize = 13.sp
                            )
                        }

                        ReviewStatusChip(
                            currentStatus = reviewStatus,
                            onStatusChange = { newStatus: String ->
                                reviewStatus = newStatus
                                ToxicSpeciesReviewStore.setGenusStatus(
                                    context,
                                    genus.familyName,
                                    genus.genusName,
                                    newStatus
                                )
                            }
                        )

                        if (editMode) {
                            IconButton(onClick = { genusToEdit = genus }) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Editar género",
                                    tint = Color(0xFF80CBC4)
                                )
                            }
                            IconButton(onClick = { genusToDelete = genus }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Eliminar género",
                                    tint = Color(0xFFFF8A80)
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = false,
                            onClick = {},
                            label = { Text("BD $localCount", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = false,
                            onClick = {},
                            label = { Text(reviewStatusLabel(reviewStatus), fontSize = 11.sp) }
                        )
                    }

                    if (genus.toxins.isNotBlank()) {
                        Text(
                            genus.toxins,
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = { onGenusClick(genus) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Abrir género")
                    }
                }
            }
        }

        if (genusSearchQuery.isNotBlank()) {
            val matchingFamilyPlants = allPlants.filter { plant ->
                plant.family.equals(familyName, ignoreCase = true) &&
                (plant.scientificName.contains(genusSearchQuery, ignoreCase = true) ||
                 (plant.commonName.contains(genusSearchQuery, ignoreCase = true) || plant.commonNames.contains(genusSearchQuery, ignoreCase = true)))
            }.sortedBy { it.scientificName.lowercase() }

            if (matchingFamilyPlants.isNotEmpty()) {
                item {
                    Text(
                        text = "Especies de $familyName en BD (${matchingFamilyPlants.size})",
                        color = Color(0xFFA5D6A7),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                }
                items(matchingFamilyPlants, key = { plant -> "fam_plt_" + plant.id }) { plant ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlantClick(plant) },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF162019)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🌱", fontSize = 26.sp)
                            Spacer(Modifier.size(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    plant.scientificName,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                val commons = listOf(plant.commonName, plant.commonNames).filter { it.isNotBlank() }.joinToString(", ")
                                if (commons.isNotBlank()) {
                                    Text(
                                        commons,
                                        color = Color(0xFFA5D6A7),
                                        fontSize = 13.sp
                                    )
                                }
                                Text(
                                    "Familia: ${plant.family} · Género: ${localGenusName(plant.scientificName)}",
                                    color = Color.LightGray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToxicSpeciesByGenus(
    plantViewModel: PlantViewModel,
    poisonousFamilyViewModel: PoisonousFamilyViewModel,
    genus: PoisonousFamilyGenusEntity,
    allPlants: List<PlantEntity>,
    onPlantClick: (PlantEntity) -> Unit,
    editMode: Boolean,
    onGenusDeleted: () -> Unit,
) {
    val context = LocalContext.current

    var genusToDelete by remember(genus.id) { mutableStateOf<PoisonousFamilyGenusEntity?>(null) }
    var genusToEdit by remember(genus.id) { mutableStateOf<PoisonousFamilyGenusEntity?>(null) }
    var plantToDelete by remember { mutableStateOf<PlantEntity?>(null) }
    var plantToEdit by remember { mutableStateOf<PlantEntity?>(null) }

    var selectionMode by remember(genus.id) { mutableStateOf(false) }
    var selectedKeys by remember(genus.id) { mutableStateOf(setOf<String>()) }

    val localPlants = remember(allPlants, genus.genusName) {
        allPlants
            .filter { plant: PlantEntity ->
                localGenusName(plant.scientificName).equals(genus.genusName, ignoreCase = true)
            }
            .sortedBy { plant: PlantEntity ->
                plant.scientificName.lowercase()
            }
    }

    var genusReviewStatus by remember(genus.id, genus.familyName, genus.genusName) {
        mutableStateOf(
            ToxicSpeciesReviewStore.getGenusStatus(
                context,
                genus.familyName,
                genus.genusName
            )
        )
    }

    var wikiSpecies by remember(genus.genusName) { mutableStateOf<List<String>>(emptyList()) }
    var gbifSpecies by remember(genus.genusName) { mutableStateOf<List<String>>(emptyList()) }

    var isLoadingWiki by remember(genus.genusName) { mutableStateOf(false) }
    var isLoadingGbif by remember(genus.genusName) { mutableStateOf(false) }

    var errorWiki by remember(genus.genusName) { mutableStateOf<String?>(null) }
    var errorGbif by remember(genus.genusName) { mutableStateOf<String?>(null) }

    var selectedTab by remember(genus.genusName) { mutableIntStateOf(0) }
    var wikiReloadKey by remember(genus.genusName) { mutableIntStateOf(0) }
    var gbifReloadKey by remember(genus.genusName) { mutableIntStateOf(0) }
    var query by remember(genus.genusName) { mutableStateOf("") }

    LaunchedEffect(genus.genusName, wikiReloadKey) {
        isLoadingWiki = true
        errorWiki = null
        val fixed = fetchWikiSpeciesFixed(context, genus.genusName)
        wikiSpecies = fixed
        if (fixed.isEmpty()) {
            errorWiki = "Wiki/Wikispecies no devolvió especies para ${genus.genusName}"
        }
        isLoadingWiki = false
    }

    LaunchedEffect(genus.genusName, gbifReloadKey) {
        isLoadingGbif = true
        errorGbif = null
        val found = fetchGbifSpeciesByGenus(genus.genusName)
        gbifSpecies = found
        if (found.isEmpty()) {
            errorGbif = "GBIF no devolvió especies para ${genus.genusName}"
        }
        isLoadingGbif = false
    }

    val localKeys = remember(localPlants) {
        localPlants.map { plant: PlantEntity -> normSpeciesName(plant.scientificName) }.toSet()
    }

    val bdItems = remember(localPlants) {
        localPlants.map { plant: PlantEntity ->
            SpeciesDisplayItem(plant.scientificName, "BD", plant)
        }
    }

    val wikiMissingItems = remember(wikiSpecies, localKeys) {
        wikiSpecies
            .filter { speciesName: String -> normSpeciesName(speciesName) !in localKeys }
            .map { speciesName: String -> SpeciesDisplayItem(speciesName, "WIKI", null) }
            .sortedBy { it.scientificName.lowercase() }
    }

    val gbifMissingItems = remember(gbifSpecies, localKeys) {
        gbifSpecies
            .filter { speciesName: String -> normSpeciesName(speciesName) !in localKeys }
            .map { speciesName: String -> SpeciesDisplayItem(speciesName, "GBIF", null) }
            .sortedBy { it.scientificName.lowercase() }
    }

    val visibleItems = remember(selectedTab, bdItems, wikiMissingItems, gbifMissingItems, query) {
        val base = when (selectedTab) {
            0 -> bdItems
            1 -> wikiMissingItems
            else -> gbifMissingItems
        }

        if (query.isBlank()) {
            base
        } else {
            base.filter { item ->
                item.scientificName.contains(query, ignoreCase = true) ||
                item.localPlant?.commonName?.contains(query, ignoreCase = true) == true ||
                item.localPlant?.commonNames?.contains(query, ignoreCase = true) == true ||
                item.localPlant?.symptoms?.contains(query, ignoreCase = true) == true ||
                item.localPlant?.toxicParts?.contains(query, ignoreCase = true) == true
            }
        }
    }

    val canBulkAdd = selectedTab == 1 || selectedTab == 2
    val bulkAddLabel = when (selectedTab) {
        1 -> "Añadir visibles a BD desde Wiki"
        2 -> "Añadir visibles a BD desde GBIF"
        else -> ""
    }

    val selectedVisibleItems = visibleItems.filter { item ->
        speciesSelectionKey(item) in selectedKeys
    }

    if (genusToDelete != null) {
        ConfirmDeleteDialog(
            title = "Eliminar género completo",
            text = "¿Seguro que quieres eliminar el género ${genus.genusName} de la lista tóxica?",
            onConfirm = {
                poisonousFamilyViewModel.deleteGenus(genus)
                Toast.makeText(
                    context,
                    "Género eliminado: ${genus.genusName}",
                    Toast.LENGTH_SHORT
                ).show()
                genusToDelete = null
                onGenusDeleted()
            },
            onDismiss = { genusToDelete = null }
        )
    }

    genusToEdit?.let { currentGenus ->
        EditGenusDialog(
            genus = currentGenus,
            onDismiss = { genusToEdit = null },
            onSave = { updated: PoisonousFamilyGenusEntity ->
                poisonousFamilyViewModel.saveGenus(updated)
                Toast.makeText(
                    context,
                    "Género actualizado: ${updated.genusName}",
                    Toast.LENGTH_SHORT
                ).show()
                genusToEdit = null
            }
        )
    }

    if (plantToDelete != null) {
        ConfirmDeleteDialog(
            title = "Eliminar ficha BD",
            text = "¿Seguro que quieres eliminar la ficha ${plantToDelete?.scientificName} de la BD?",
            onConfirm = {
                plantToDelete?.let { plant ->
                    plantViewModel.deletePlant(plant)
                    Toast.makeText(
                        context,
                        "Ficha eliminada: ${plant.scientificName}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                plantToDelete = null
            },
            onDismiss = { plantToDelete = null }
        )
    }

    plantToEdit?.let { currentPlant ->
        EditPlantDialog(
            plant = currentPlant,
            onDismiss = { plantToEdit = null },
            onSave = { updated: PlantEntity ->
                plantViewModel.insertPlant(updated)
                Toast.makeText(
                    context,
                    "Ficha actualizada: ${updated.scientificName}",
                    Toast.LENGTH_SHORT
                ).show()
                plantToEdit = null
            }
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111A12)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${genus.familyName} > ${genus.genusName}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                "Explora especies locales y externas de este género",
                                color = Color.LightGray,
                                fontSize = 13.sp
                            )
                        }
                        ReviewStatusChip(
                            currentStatus = genusReviewStatus,
                            onStatusChange = { newStatus: String ->
                                genusReviewStatus = newStatus
                                ToxicSpeciesReviewStore.setGenusStatus(
                                    context,
                                    genus.familyName,
                                    genus.genusName,
                                    newStatus
                                )
                            }
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OriginChip("BD ${bdItems.size}", OriginBd)
                        OriginChip("WIKI ${wikiMissingItems.size}", OriginWiki)
                        OriginChip("GBIF ${gbifMissingItems.size}", OriginGbif)
                    }

                    if (genus.toxins.isNotBlank()) {
                        Text("Toxinas: ${genus.toxins}", color = Color.LightGray, fontSize = 12.sp)
                    }

                    if (editMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { genusToEdit = genus },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = Color(0xFF80CBC4)
                                )
                                Spacer(Modifier.size(8.dp))
                                Text("Editar género", color = Color(0xFF80CBC4))
                            }

                            OutlinedButton(
                                onClick = { genusToDelete = genus },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = Color(0xFFFF8A80)
                                )
                                Spacer(Modifier.size(8.dp))
                                Text("Eliminar género", color = Color(0xFFFF8A80))
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Explorar género", color = Color.White, fontWeight = FontWeight.Bold)
                    if (editMode) {
                        Text(
                            "Modo edición activo",
                            color = Color(0xFFFFCC80),
                            fontSize = 12.sp
                        )
                    }
                }

                if (selectedTab == 1 || selectedTab == 2) {
                    IconButton(onClick = {
                        if (selectedTab == 1) wikiReloadKey++
                        if (selectedTab == 2) gbifReloadKey++
                    }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Recargar",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        item {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        selectionMode = false
                        selectedKeys = emptySet()
                    },
                    text = { Text("BD (${bdItems.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        selectionMode = false
                        selectedKeys = emptySet()
                    },
                    text = { Text("Wiki (${wikiMissingItems.size})") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                        selectionMode = false
                        selectedKeys = emptySet()
                    },
                    text = { Text("GBIF (${gbifMissingItems.size})") }
                )
            }
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar especie en esta pestaña…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White) },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = Color.White)
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFA5D6A7),
                    unfocusedBorderColor = Color(0xFF2E7D32)
                ),
                shape = RoundedCornerShape(14.dp)
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        selectionMode = true
                        selectedKeys = visibleItems.map { speciesSelectionKey(it) }.toSet()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Seleccionar visibles")
                }

                OutlinedButton(
                    onClick = {
                        selectionMode = false
                        selectedKeys = emptySet()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Limpiar selección")
                }
            }
        }

        if (selectionMode) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1D2A1F)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Seleccionadas: ${selectedVisibleItems.size}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        if (selectedTab == 0) {
                            Button(
                                onClick = {
                                    val plantsToDelete =
                                        selectedVisibleItems.mapNotNull { it.localPlant }
                                    plantsToDelete.forEach { plant ->
                                        plantViewModel.deletePlant(plant)
                                    }
                                    Toast.makeText(
                                        context,
                                        if (plantsToDelete.isEmpty()) {
                                            "No hay fichas BD seleccionadas."
                                        } else {
                                            "Eliminadas ${plantsToDelete.size} fichas de BD."
                                        },
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    selectedKeys = emptySet()
                                    selectionMode = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(
                                        0xFFC62828
                                    )
                                )
                            ) {
                                Text("Borrar seleccionadas de BD")
                            }
                        } else {
                            Button(
                                onClick = {
                                    val sourceLabel = if (selectedTab == 1) "Wiki" else "GBIF"
                                    val currentLocalKeys = allPlants
                                        .map { plant -> normSpeciesName(plant.scientificName) }
                                        .toMutableSet()

                                    var insertedCount = 0

                                    selectedVisibleItems.forEach { item ->
                                        val normalized = normSpeciesName(item.scientificName)
                                        if (normalized !in currentLocalKeys) {
                                            val plant = buildPlantFromExternalSpecies(
                                                genus = genus,
                                                scientificName = item.scientificName,
                                                sourceLabel = sourceLabel
                                            )
                                            plantViewModel.insertPlant(plant)
                                            ToxicSpeciesReviewStore.setSpeciesStatus(
                                                context,
                                                genus.familyName,
                                                genus.genusName,
                                                wikipediaSpeciesTitle(item.scientificName),
                                                ToxicSpeciesReviewStore.ANADIDA
                                            )
                                            currentLocalKeys += normalized
                                            insertedCount++
                                        }
                                    }

                                    Toast.makeText(
                                        context,
                                        when {
                                            selectedVisibleItems.isEmpty() ->
                                                "No hay especies seleccionadas."

                                            insertedCount == 0 ->
                                                "No se añadió ninguna. Ya estaban en BD."

                                            else ->
                                                "Añadidas $insertedCount seleccionadas desde $sourceLabel."
                                        },
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    selectedKeys = emptySet()
                                    selectionMode = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(
                                        0xFF6A1B9A
                                    )
                                )
                            ) {
                                Text(
                                    if (selectedTab == 1) {
                                        "Añadir seleccionadas a BD desde Wiki"
                                    } else {
                                        "Añadir seleccionadas a BD desde GBIF"
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (canBulkAdd) {
            item {
                Button(
                    onClick = {
                        val sourceLabel = if (selectedTab == 1) "Wiki" else "GBIF"
                        val currentLocalKeys = allPlants
                            .map { plant -> normSpeciesName(plant.scientificName) }
                            .toMutableSet()

                        var insertedCount = 0

                        visibleItems.forEach { item ->
                            val normalized = normSpeciesName(item.scientificName)
                            if (normalized !in currentLocalKeys) {
                                val plant = buildPlantFromExternalSpecies(
                                    genus = genus,
                                    scientificName = item.scientificName,
                                    sourceLabel = sourceLabel
                                )
                                plantViewModel.insertPlant(plant)
                                ToxicSpeciesReviewStore.setSpeciesStatus(
                                    context,
                                    genus.familyName,
                                    genus.genusName,
                                    wikipediaSpeciesTitle(item.scientificName),
                                    ToxicSpeciesReviewStore.ANADIDA
                                )
                                currentLocalKeys += normalized
                                insertedCount++
                            }
                        }

                        val message = when {
                            visibleItems.isEmpty() ->
                                "No hay especies visibles para añadir."

                            insertedCount == 0 ->
                                "No se añadió ninguna. Ya estaban en BD."

                            else ->
                                "Añadidas $insertedCount especies a BD desde $sourceLabel."
                        }

                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF7B1FA2)
                    )
                ) {
                    Text(bulkAddLabel)
                }
            }
        }

        when (selectedTab) {
            0 -> {
                if (visibleItems.isEmpty()) {
                    item {
                        EmptyMessageCard("No hay especies en BD con ese filtro.")
                    }
                }

                itemsIndexed(
                    visibleItems,
                    key = { index: Int, item: SpeciesDisplayItem ->
                        "bd_${item.localPlant?.id ?: index}_${item.scientificName}"
                    }
                ) { _, item ->
                    SpeciesCard(
                        genus = genus,
                        item = item,
                        onPlantClick = onPlantClick,
                        onAddToDb = {},
                        editMode = editMode,
                        selectionMode = selectionMode,
                        isSelected = speciesSelectionKey(item) in selectedKeys,
                        onToggleSelection = {
                            val key = speciesSelectionKey(item)
                            selectedKeys = if (key in selectedKeys) {
                                selectedKeys - key
                            } else {
                                selectedKeys + key
                            }
                        },
                        onDeletePlant = {
                            item.localPlant?.let { plant ->
                                plantToDelete = plant
                            }
                        },
                        onEditPlant = {
                            item.localPlant?.let { plant ->
                                plantToEdit = plant
                            }
                        }
                    )
                }
            }

            1 -> {
                if (isLoadingWiki) {
                    item {
                        CenterLoadingCard("Cargando especies desde Wiki/Wikispecies…")
                    }
                }

                if (errorWiki != null) {
                    item {
                        ErrorMessageCard(errorWiki ?: "Error Wiki")
                    }
                }

                if (!isLoadingWiki && visibleItems.isEmpty()) {
                    item {
                        EmptyMessageCard(
                            if (query.isBlank())
                                "No hay especies nuevas en Wiki fuera de tu BD."
                            else
                                "No hay especies Wiki con ese filtro."
                        )
                    }
                }

                itemsIndexed(
                    visibleItems,
                    key = { index: Int, item: SpeciesDisplayItem -> "wiki_${index}_${item.scientificName}" }
                ) { _, item ->
                    SpeciesCard(
                        genus = genus,
                        item = item,
                        onPlantClick = onPlantClick,
                        onAddToDb = {
                            val cleanName = wikipediaSpeciesTitle(item.scientificName)
                            val plant = buildPlantFromExternalSpecies(
                                genus = genus,
                                scientificName = cleanName,
                                sourceLabel = "Wiki"
                            )
                            plantViewModel.insertPlant(plant)
                            ToxicSpeciesReviewStore.setSpeciesStatus(
                                context,
                                genus.familyName,
                                genus.genusName,
                                cleanName,
                                ToxicSpeciesReviewStore.ANADIDA
                            )
                            Toast.makeText(
                                context,
                                "Añadida desde Wiki: $cleanName",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        editMode = editMode,
                        selectionMode = selectionMode,
                        isSelected = speciesSelectionKey(item) in selectedKeys,
                        onToggleSelection = {
                            val key = speciesSelectionKey(item)
                            selectedKeys = if (key in selectedKeys) {
                                selectedKeys - key
                            } else {
                                selectedKeys + key
                            }
                        },
                        onDeletePlant = {},
                        onEditPlant = {}
                    )
                }
            }

            2 -> {
                if (isLoadingGbif) {
                    item {
                        CenterLoadingCard("Cargando especies desde GBIF…")
                    }
                }

                if (errorGbif != null) {
                    item {
                        ErrorMessageCard(errorGbif ?: "Error GBIF")
                    }
                }

                if (!isLoadingGbif && visibleItems.isEmpty()) {
                    item {
                        EmptyMessageCard(
                            if (query.isBlank())
                                "No hay especies nuevas en GBIF fuera de tu BD."
                            else
                                "No hay especies GBIF con ese filtro."
                        )
                    }
                }

                itemsIndexed(
                    visibleItems,
                    key = { index: Int, item: SpeciesDisplayItem -> "gbif_${index}_${item.scientificName}" }
                ) { _, item ->
                    SpeciesCard(
                        genus = genus,
                        item = item,
                        onPlantClick = onPlantClick,
                        onAddToDb = {
                            val cleanName = wikipediaSpeciesTitle(item.scientificName)
                            val plant = buildPlantFromExternalSpecies(
                                genus = genus,
                                scientificName = cleanName,
                                sourceLabel = "GBIF"
                            )
                            plantViewModel.insertPlant(plant)
                            ToxicSpeciesReviewStore.setSpeciesStatus(
                                context,
                                genus.familyName,
                                genus.genusName,
                                cleanName,
                                ToxicSpeciesReviewStore.ANADIDA
                            )
                            Toast.makeText(
                                context,
                                "Añadida desde GBIF: $cleanName",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        editMode = editMode,
                        selectionMode = selectionMode,
                        isSelected = speciesSelectionKey(item) in selectedKeys,
                        onToggleSelection = {
                            val key = speciesSelectionKey(item)
                            selectedKeys = if (key in selectedKeys) {
                                selectedKeys - key
                            } else {
                                selectedKeys + key
                            }
                        },
                        onDeletePlant = {},
                        onEditPlant = {}
                    )
                }
            }
        }
    }
}

@Composable
private fun OriginChip(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.18f),
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun CenterLoadingCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF162019)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text(message, color = Color.White, fontSize = 13.sp)
        }
    }
}

@Composable
private fun EmptyMessageCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF162019)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            color = Color.White,
            fontSize = 13.sp,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun ErrorMessageCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF3B1515)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            color = Color(0xFFFFCDD2),
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun ConfirmDeleteDialog(
    title: String,
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title, fontWeight = FontWeight.Bold)
        },
        text = {
            Text(text)
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Eliminar", color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun EditGenusDialog(
    genus: PoisonousFamilyGenusEntity,
    onDismiss: () -> Unit,
    onSave: (PoisonousFamilyGenusEntity) -> Unit,
) {
    var familyName by remember(genus.id) { mutableStateOf(genus.familyName) }
    var genusName by remember(genus.id) { mutableStateOf(genus.genusName) }
    var speciesCountText by remember(genus.id) { mutableStateOf(genus.genusSpeciesCount.toString()) }
    var toxins by remember(genus.id) { mutableStateOf(genus.toxins) }
    var symptoms by remember(genus.id) { mutableStateOf(genus.symptoms) }
    var toxicParts by remember(genus.id) { mutableStateOf(genus.toxicParts) }
    var notes by remember(genus.id) { mutableStateOf(genus.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar género", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    OutlinedTextField(
                        value = familyName,
                        onValueChange = { familyName = it },
                        label = { Text("Familia") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = genusName,
                        onValueChange = { genusName = it },
                        label = { Text("Género") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = speciesCountText,
                        onValueChange = { speciesCountText = it },
                        label = { Text("Nº especies") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                item {
                    OutlinedTextField(
                        value = toxins,
                        onValueChange = { toxins = it },
                        label = { Text("Toxinas") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
                item {
                    OutlinedTextField(
                        value = symptoms,
                        onValueChange = { symptoms = it },
                        label = { Text("Síntomas") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
                item {
                    OutlinedTextField(
                        value = toxicParts,
                        onValueChange = { toxicParts = it },
                        label = { Text("Partes tóxicas") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notas") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        genus.copy(
                            familyName = familyName.trim(),
                            genusName = genusName.trim(),
                            genusSpeciesCount = speciesCountText.toIntOrNull()
                                ?: genus.genusSpeciesCount,
                            toxins = toxins.trim(),
                            symptoms = symptoms.trim(),
                            toxicParts = toxicParts.trim(),
                            notes = notes.trim(),
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                },
                enabled = familyName.isNotBlank() && genusName.isNotBlank()
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun EditPlantDialog(
    plant: PlantEntity,
    onDismiss: () -> Unit,
    onSave: (PlantEntity) -> Unit,
) {
    var commonName by remember(plant.id) { mutableStateOf(plant.commonName) }
    var commonNames by remember(plant.id) { mutableStateOf(plant.commonNames) }
    var scientificName by remember(plant.id) { mutableStateOf(plant.scientificName) }
    var family by remember(plant.id) { mutableStateOf(plant.family) }
    var toxicityLevel by remember(plant.id) { mutableStateOf(plant.toxicityLevel) }
    var toxicParts by remember(plant.id) { mutableStateOf(plant.toxicParts) }
    var symptoms by remember(plant.id) { mutableStateOf(plant.symptoms) }
    var description by remember(plant.id) { mutableStateOf(plant.description) }
    var habitat by remember(plant.id) { mutableStateOf(plant.habitat) }
    var distribution by remember(plant.id) { mutableStateOf(plant.geographicDistribution) }
    var firstAid by remember(plant.id) { mutableStateOf(plant.firstAid) }
    var category by remember(plant.id) { mutableStateOf(plant.category) }
    var notes by remember(plant.id) { mutableStateOf(plant.notes.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar ficha BD", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    OutlinedTextField(
                        value = commonName,
                        onValueChange = { commonName = it },
                        label = { Text("Nombre común") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = commonNames,
                        onValueChange = { commonNames = it },
                        label = { Text("Nombres comunes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = scientificName,
                        onValueChange = { scientificName = it },
                        label = { Text("Nombre científico") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = family,
                        onValueChange = { family = it },
                        label = { Text("Familia") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = toxicityLevel,
                        onValueChange = { toxicityLevel = it },
                        label = { Text("Toxicidad") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = toxicParts,
                        onValueChange = { toxicParts = it },
                        label = { Text("Partes tóxicas") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
                item {
                    OutlinedTextField(
                        value = symptoms,
                        onValueChange = { symptoms = it },
                        label = { Text("Síntomas") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descripción") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
                item {
                    OutlinedTextField(
                        value = habitat,
                        onValueChange = { habitat = it },
                        label = { Text("Hábitat") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
                item {
                    OutlinedTextField(
                        value = distribution,
                        onValueChange = { distribution = it },
                        label = { Text("Distribución") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
                item {
                    OutlinedTextField(
                        value = firstAid,
                        onValueChange = { firstAid = it },
                        label = { Text("Primeros auxilios") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notas") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
                item {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Categoría") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        plant.copy(
                            commonName = commonName.trim(),
                            commonNames = commonNames.trim(),
                            scientificName = scientificName.trim(),
                            family = family.trim(),
                            toxicityLevel = toxicityLevel.trim(),
                            toxicParts = toxicParts.trim(),
                            symptoms = symptoms.trim(),
                            description = description.trim(),
                            habitat = habitat.trim(),
                            geographicDistribution = distribution.trim(),
                            firstAid = firstAid.trim(),
                            category = category.trim(),
                            notes = notes.trim().ifBlank { null }
                        )
                    )
                },
                enabled = commonName.isNotBlank() && scientificName.isNotBlank()
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun SpeciesCard(
    genus: PoisonousFamilyGenusEntity,
    item: SpeciesDisplayItem,
    onPlantClick: (PlantEntity) -> Unit,
    onAddToDb: () -> Unit,
    editMode: Boolean,
    selectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onDeletePlant: () -> Unit,
    onEditPlant: () -> Unit,
) {
    val context = LocalContext.current
    var selectedTab by remember(item.scientificName) { mutableIntStateOf(0) }
    var gbifReloadKey by remember(item.scientificName) { mutableIntStateOf(0) }
    var speciesReviewStatus by remember(genus.familyName, genus.genusName, item.scientificName) {
        mutableStateOf(
            ToxicSpeciesReviewStore.getSpeciesStatus(
                context,
                genus.familyName,
                genus.genusName,
                wikipediaSpeciesTitle(item.scientificName)
            )
        )
    }

    val chipColor = sourceColor(item.source)

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF223A27) else Color(0xFF162019)
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (selectionMode) onToggleSelection()
                },
                onLongClick = {
                    onToggleSelection()
                }
            )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (item.localPlant != null) "🌿" else "🌐", fontSize = 22.sp)
                Spacer(Modifier.size(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        wikipediaSpeciesTitle(item.scientificName),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OriginChip(
                            text = if (item.localPlant != null) "BD" else item.source.uppercase(),
                            color = chipColor
                        )
                        if (isSelected) {
                            OriginChip(
                                text = "Seleccionada",
                                color = Color(0xFF81C784)
                            )
                        }
                    }
                }

                if (editMode && item.localPlant != null) {
                    IconButton(onClick = onEditPlant) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Editar ficha",
                            tint = Color(0xFF80CBC4)
                        )
                    }
                    IconButton(onClick = onDeletePlant) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar ficha",
                            tint = Color(0xFFFF8A80)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Género: ${genus.genusName}", color = Color(0xFFA5D6A7), fontSize = 12.sp)
                ReviewStatusChip(
                    currentStatus = speciesReviewStatus,
                    onStatusChange = { newStatus: String ->
                        speciesReviewStatus = newStatus
                        ToxicSpeciesReviewStore.setSpeciesStatus(
                            context,
                            genus.familyName,
                            genus.genusName,
                            wikipediaSpeciesTitle(item.scientificName),
                            newStatus
                        )
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Estado: ${reviewStatusLabel(speciesReviewStatus)}",
                    color = reviewStatusColor(speciesReviewStatus),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                if (selectedTab == 2) {
                    IconButton(onClick = { gbifReloadKey++ }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Recargar",
                            tint = Color.White
                        )
                    }
                }
            }

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("BD") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Wiki") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("GBIF") }
                )
            }

            when (selectedTab) {
                0 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (item.localPlant != null) {
                            Button(
                                onClick = { onPlantClick(item.localPlant) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Ficha BD")
                            }

                            if (editMode) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = onEditPlant,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = null,
                                            tint = Color(0xFF80CBC4)
                                        )
                                        Spacer(Modifier.size(8.dp))
                                        Text("Editar", color = Color(0xFF80CBC4))
                                    }

                                    OutlinedButton(
                                        onClick = onDeletePlant,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = Color(0xFFFF8A80)
                                        )
                                        Spacer(Modifier.size(8.dp))
                                        Text("Eliminar", color = Color(0xFFFF8A80))
                                    }
                                }
                            }
                        } else {
                            Button(
                                onClick = {
                                    onAddToDb()
                                    speciesReviewStatus = ToxicSpeciesReviewStore.ANADIDA
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF6A1B9A)
                                )
                            ) {
                                Text("Añadir a BD")
                            }
                        }

                        Text(
                            text = "Consulta Wiki y GBIF en sus pestañas",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                    }
                }

                1 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = wikipediaSpeciesTitle(item.scientificName),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Género: ${genus.genusName}",
                            color = Color(0xFFA5D6A7),
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Familia: ${genus.familyName}",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Fuente: ${item.source.uppercase()}",
                            color = chipColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        if (genus.toxins.isNotBlank()) {
                            Text(
                                text = "Toxinas: ${genus.toxins}",
                                color = Color.LightGray,
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = "Resumen Wikipedia no disponible localmente para esta especie.",
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }

                2 -> {
                    SpeciesGbifPane(
                        scientificName = item.scientificName,
                        reloadKey = gbifReloadKey
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeciesGbifPane(
    scientificName: String,
    reloadKey: Int,
    gbifViewModel: GBIFViewModel = viewModel()
) {
    val searchState by gbifViewModel.searchResult.collectAsState()
    val vernacularNames by gbifViewModel.vernacularNames.collectAsState()
    val occurrences by gbifViewModel.occurrences.collectAsState()

    LaunchedEffect(scientificName, reloadKey) {
        gbifViewModel.resetSearch()
        gbifViewModel.searchSpecies(scientificName)
    }

    when (val state = searchState) {
        is SearchResultState.Idle,
        is SearchResultState.Loading -> Box(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }

        is SearchResultState.Success -> {
            LaunchedEffect(state.match.usageKey, reloadKey) {
                state.match.usageKey?.let { usageKey: Long ->
                    gbifViewModel.loadVernacularNames(usageKey)
                    gbifViewModel.loadOccurrences(usageKey)
                }
            }
            SpeciesGbifContent(
                state.match,
                vernacularNames.mapNotNull { it.name },
                occurrences.size
            )
        }

        is SearchResultState.NotFound -> {
            ErrorMessageCard("No encontrado en GBIF: ${state.query}")
        }

        is SearchResultState.Error -> {
            ErrorMessageCard(state.message)
        }
    }
}

@Composable
private fun SpeciesGbifContent(
    match: GBIFSpeciesMatch,
    vernacular: List<String>,
    occurrenceCount: Int
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111A12)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                match.scientificName ?: "Sin nombre",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text("Familia: ${match.family ?: "—"}", color = Color.LightGray, fontSize = 12.sp)
            Text("Género: ${match.genus ?: "—"}", color = Color.LightGray, fontSize = 12.sp)
            Text("Reino: ${match.kingdom ?: "—"}", color = Color.LightGray, fontSize = 12.sp)
            Text("Estado: ${match.status ?: "—"}", color = Color.LightGray, fontSize = 12.sp)
            match.confidence?.let {
                Text(
                    "Confianza: $it%",
                    color = Color(0xFFA5D6A7),
                    fontSize = 12.sp
                )
            }
            if (vernacular.isNotEmpty()) {
                Text(
                    "Nombres comunes: ${vernacular.distinct().take(5).joinToString()}",
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
            Text(
                "Registros con coordenadas: $occurrenceCount",
                color = Color.White,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun ReviewStatusChip(
    currentStatus: String,
    onStatusChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val chipColor = reviewStatusColor(currentStatus)

    Box {
        Surface(
            color = chipColor.copy(alpha = 0.18f),
            shape = RoundedCornerShape(999.dp),
            modifier = Modifier.clickable { expanded = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(chipColor, CircleShape)
                )
                Text(
                    text = reviewStatusLabel(currentStatus),
                    color = chipColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ToxicSpeciesReviewStore.allStatuses.forEach { status ->
                DropdownMenuItem(
                    text = { Text(reviewStatusLabel(status)) },
                    onClick = {
                        expanded = false
                        onStatusChange(status)
                    }
                )
            }
        }
    }
}
