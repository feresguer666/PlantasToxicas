package com.toxicplants.database.ui.screens.toxicgenera

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.ui.viewmodel.PlantViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

data class GbifSpecies(val scientificName: String, val canonical: String, val key: Long? = null)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenusDetailScreen(
    genusName: String,
    plantViewModel: PlantViewModel,
    onBack: () -> Unit,
    onPlantClick: (PlantEntity) -> Unit
) {
    val context = LocalContext.current
    val allPlants by plantViewModel.allPlants.observeAsState(emptyList())

    fun extractGenus(sci: String) = sci.trim().split(Regex("[\\s_]+")).firstOrNull() ?: ""
    fun canonicalName(sci: String) =
        sci.trim().split(Regex("\\s+")).take(2).joinToString(" ").lowercase()

    // Buscar info del género en el store editable, no solo en el catálogo base
    val genusInfo = remember(genusName) {
        val store = ToxicGeneraUserStore(context)
        store.getMerged().find { it.genus.equals(genusName, true) }
            ?: ToxicGenus(genusName, "", "", 0, "", null)
    }

    val localSpecies = remember(allPlants, genusName) {
        allPlants.filter { extractGenus(it.scientificName).equals(genusName, ignoreCase = true) }
            .sortedBy { it.scientificName }
    }

    var gbifSpecies by remember { mutableStateOf<List<GbifSpecies>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var fromCache by remember { mutableStateOf(false) }
    var reloadTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(genusName, reloadTrigger) {
        loading = true; error = null; fromCache = false
        val forceRefresh = reloadTrigger > 0

        // 1. Cache
        if (!forceRefresh) {
            GbifCache.get(context, genusName)?.let {
                gbifSpecies = it
                loading = false
                fromCache = true
                if (it.isNotEmpty()) return@LaunchedEffect
            }
        }
        // 2. Red
        var result: List<GbifSpecies> = emptyList()
        var lastErr: String? = null
        try {
            val urls = mutableListOf<String>()
            genusInfo.gbifGenusKey?.let { key ->
                urls += "https://api.gbif.org/v1/species/search?highertaxon_key=$key&rank=SPECIES&status=ACCEPTED&limit=300"
                urls += "https://api.gbif.org/v1/species/search?highertaxon_key=$key&rank=SPECIES&limit=300"
            }
            val enc = URLEncoder.encode(genusName, "UTF-8")
            urls += "https://api.gbif.org/v1/species/search?rank=SPECIES&genus=$enc&status=ACCEPTED&kingdom=Plantae&limit=300"
            urls += "https://api.gbif.org/v1/species/search?rank=SPECIES&genus=$enc&kingdom=Plantae&limit=300"
            urls += "https://api.gbif.org/v1/species/search?rank=SPECIES&q=$enc&limit=300"

            loop@ for (url in urls) {
                try {
                    val jsonText = withContext(Dispatchers.IO) {
                        val conn = URL(url).openConnection()
                        conn.setRequestProperty("User-Agent", "ToxicPlants-Android/1.0")
                        conn.connectTimeout = 8000; conn.readTimeout = 8000
                        conn.getInputStream().bufferedReader().readText()
                    }
                    val arr = JSONObject(jsonText).getJSONArray("results")
                    val list = mutableListOf<GbifSpecies>()
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val kingdom = o.optString("kingdom", "Plantae")
                        val rank = o.optString("rank", "SPECIES")
                        val sci = o.optString("scientificName", "")
                        val canon = o.optString("canonicalName", sci)
                        if (kingdom.equals("Plantae", true) && rank.equals("SPECIES", true) &&
                            extractGenus(canon).equals(genusName, ignoreCase = true)
                        ) {
                            list.add(
                                GbifSpecies(
                                    sci,
                                    canon.ifBlank { sci },
                                    o.optLong("key").takeIf { o.has("key") })
                            )
                        }
                    }
                    if (list.isNotEmpty()) {
                        result = list.distinctBy { canonicalName(it.canonical) }
                            .sortedBy { it.canonical.lowercase() }
                        break@loop
                    }
                } catch (e: Exception) {
                    lastErr = e.message; continue
                }
            }
            if (result.isNotEmpty()) {
                gbifSpecies = result
                GbifCache.put(context, genusName, result)
                fromCache = false
            } else {
                // fallback a cache aunque esté caducado
                GbifCache.get(context, genusName)?.let {
                    gbifSpecies = it; fromCache = true
                    error = "Sin conexión – mostrando cache"
                } ?: run {
                    error = lastErr ?: "GBIF: 0 especies para $genusName"
                    gbifSpecies = emptyList()
                }
            }
        } catch (e: Exception) {
            error = e.message
            gbifSpecies = GbifCache.get(context, genusName) ?: emptyList()
            fromCache = gbifSpecies.isNotEmpty()
        }
        loading = false
    }

    val merged = remember(gbifSpecies, localSpecies) {
        val localMap = localSpecies.associateBy { canonicalName(it.scientificName) }
        val gbifMap = gbifSpecies.associateBy { canonicalName(it.canonical) }
        val allKeys = (gbifMap.keys + localMap.keys).sorted()
        allKeys.map { key ->
            val gbif = gbifMap[key]
            val local = localMap[key]
            Triple(gbif?.scientificName ?: local!!.scientificName, local, gbif != null)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$genusName spp.", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            null
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFB71C1C),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { reloadTrigger++ }) {
                        Icon(
                            Icons.Default.Refresh,
                            "Actualizar GBIF",
                            tint = Color.White
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            genusInfo.commonNameEs.ifBlank { genusName },
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        if (genusInfo.family.isNotBlank()) Text(
                            genusInfo.family,
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                        if (genusInfo.toxicityNote.isNotBlank()) Text(
                            genusInfo.toxicityNote,
                            fontSize = 13.sp,
                            color = Color(0xFFD32F2F)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "GBIF: ${gbifSpecies.size}${if (fromCache) " (cache)" else ""}  •  Tu BD: ${localSpecies.size}  •  Catálogo: ${genusInfo.speciesCount}",
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (loading) {
                    LinearProgressIndicator(Modifier.fillMaxWidth()); Text(
                        "Buscando en GBIF…",
                        fontSize = 11.sp,
                        color = Color.Gray
                    ); Spacer(Modifier.height(8.dp))
                }
                if (error != null) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                "GBIF",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100),
                                fontSize = 13.sp
                            )
                            Text(error!!, fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            items(merged) { item ->
                val sciName = item.first
                val localPlant = item.second
                val inDb = localPlant != null
                ListItem(
                    modifier = Modifier.clickable(enabled = inDb) { localPlant?.let(onPlantClick) },
                    headlineContent = {
                        Text(
                            sciName,
                            fontStyle = FontStyle.Italic,
                            fontWeight = if (inDb) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    supportingContent = {
                        if (inDb) Text(
                            localPlant!!.commonName,
                            fontSize = 12.sp
                        )
                    },
                    trailingContent = {
                        if (inDb) AssistChip(
                            onClick = { onPlantClick(localPlant!!) },
                            label = { Text("Ver ficha") }) else Text("—", color = Color.Gray)
                    }
                )
                HorizontalDivider()
            }
            if (!loading && merged.isEmpty()) {
                item { Text("Sin resultados para este género.", color = Color.Gray) }
            }
        }
    }
}