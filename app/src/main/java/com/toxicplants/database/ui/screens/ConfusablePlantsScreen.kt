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
import java.text.Normalizer
import java.util.Locale

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

    // Fotos fijas para el comparador visual. No se consulta ninguna base de datos ni pantalla.
    val toxicImageUrl = remember(toxicPlant.scientificName, toxicPlant.commonName, toxicPlant.imageUrl) {
        fixedComparatorPhoto(toxicPlant.scientificName)
            ?: fixedComparatorPhoto(toxicPlant.commonName)
            ?: toxicPlant.imageUrl.split("|").map { it.trim() }.firstOrNull { it.isUsableComparatorImageUrl() }
    }
    val targetImageUrl = remember(targetName, targetPlant?.scientificName, targetPlant?.imageUrl) {
        fixedComparatorPhoto(targetName)
            ?: targetPlant?.scientificName?.let { fixedComparatorPhoto(it) }
            ?: targetPlant?.imageUrl?.split("|")?.map { it.trim() }?.firstOrNull { it.isUsableComparatorImageUrl() }
    }
    var expandedPhoto by remember { mutableStateOf<Pair<String, String>?>(null) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize().padding(8.dp), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Compare, null, tint = Color(0xFFBF360C))
                    Spacer(Modifier.width(12.dp))
                    Text("Comparador Visual", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Clear, null) }
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Surface(color = Color(0xFFB71C1C), shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp), modifier = Modifier.fillMaxWidth()) {
                            Text("TÓXICA 💀", color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.padding(6.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Box(modifier = Modifier.weight(1f).fillMaxWidth().border(2.dp, Color(0xFFB71C1C))) {
                            ComparatorFixedImage(
                                imageUrl = toxicImageUrl,
                                fallbackText = toxicPlant.commonName.ifBlank { toxicPlant.scientificName },
                                onImageClick = {
                                    toxicImageUrl?.let { expandedPhoto = it to toxicPlant.commonName.ifBlank { toxicPlant.scientificName } }
                                }
                            )
                        }
                        Surface(color = Color.Black.copy(alpha = 0.05f), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(8.dp)) {
                                Text(toxicPlant.commonName, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(toxicPlant.scientificName, fontStyle = FontStyle.Italic, fontSize = 10.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Surface(color = Color(0xFF2E7D32), shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp), modifier = Modifier.fillMaxWidth()) {
                            Text("CONFUNDIBLE ✅", color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.padding(6.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Box(modifier = Modifier.weight(1f).fillMaxWidth().border(2.dp, Color(0xFF2E7D32))) {
                            ComparatorFixedImage(
                                imageUrl = targetImageUrl,
                                fallbackText = targetName.ifBlank { "Confundible" },
                                onImageClick = {
                                    targetImageUrl?.let { expandedPhoto = it to targetName.ifBlank { "Confundible" } }
                                }
                            )
                        }
                        Surface(color = Color.Black.copy(alpha = 0.05f), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(8.dp)) {
                                Text(targetName, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(targetPlant?.scientificName ?: "Foto fija de comparación", fontStyle = FontStyle.Italic, fontSize = 10.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { val query = Uri.encode("${toxicPlant.scientificName} vs ${targetPlant?.scientificName ?: targetName} differences"); context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$query&tbm=isch"))) }, modifier = Modifier.fillMaxWidth().height(44.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBF360C))) {
                    Icon(Icons.Default.Search, null); Spacer(Modifier.width(8.dp)); Text("BUSCAR DIFERENCIAS CLAVE")
                }
                Spacer(Modifier.height(8.dp))
                Text("ADVERTENCIA: La identificación visual puede fallar. Ante la duda, NUNCA consumas ni toques la planta.", fontSize = 11.sp, color = Color.Red, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
            }
        }
    }

    expandedPhoto?.let { photo ->
        FullScreenComparatorImage(
            imageUrl = photo.first,
            title = photo.second,
            onDismiss = { expandedPhoto = null }
        )
    }
}

@Composable
private fun FullScreenComparatorImage(
    imageUrl: String,
    title: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            val context = LocalContext.current
            AsyncImage(
                model = remember(imageUrl, context) { com.toxicplants.database.ui.PlantImageHelper.getModelForUrl(context, imageUrl) },
                contentDescription = title,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .clickable { onDismiss() },
                contentScale = ContentScale.Fit
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(50))
            ) {
                Icon(Icons.Default.Clear, contentDescription = "Cerrar", tint = Color.White)
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(14.dp),
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = title,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ComparatorFixedImage(
    imageUrl: String?,
    fallbackText: String,
    onImageClick: (() -> Unit)? = null
) {
    var failed by remember(imageUrl) { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F8E9))
            .clickable(enabled = !imageUrl.isNullOrBlank() && !failed && onImageClick != null) {
                onImageClick?.invoke()
            },
        contentAlignment = Alignment.Center
    ) {
        if (!imageUrl.isNullOrBlank() && !failed) {
            val context = LocalContext.current
            AsyncImage(
                model = remember(imageUrl, context) { com.toxicplants.database.ui.PlantImageHelper.getModelForUrl(context, imageUrl) },
                contentDescription = fallbackText,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onError = { failed = true }
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(6.dp),
                color = Color.Black.copy(alpha = 0.55f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "🔍 Tocar para ampliar",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("🖼️", fontSize = 34.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    fallbackText,
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun fixedComparatorPhoto(name: String): String? =
    fixedComparatorPhotos[name.comparatorKey()]

private fun String.isUsableComparatorImageUrl(): Boolean {
    val value = trim()
    if (value.isBlank()) return false
    if (value.startsWith("file:///android_asset/generated_images/", ignoreCase = true)) return false
    if (value.equals("https://wikimedia.org", ignoreCase = true)) return false
    if (value.equals("http://wikimedia.org", ignoreCase = true)) return false
    return true
}

private fun String.comparatorKey(): String {
    val withoutAccents = Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
    return withoutAccents
        .lowercase(Locale.getDefault())
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
}

private val fixedComparatorPhotos = mapOf(
    // ── Plantas tóxicas del comparador ───────────────────────────────
    "abrus precatorius" to "https://upload.wikimedia.org/wikipedia/commons/2/21/Abrus_precatorius_pods.jpg",
    "aconitum napellus" to "https://upload.wikimedia.org/wikipedia/commons/thumb/6/66/Illustration_Aconitum_napellus0_clean.jpg/250px-Illustration_Aconitum_napellus0_clean.jpg",
    "atropa belladonna" to "https://upload.wikimedia.org/wikipedia/commons/7/79/Atropa_bella-donna1.jpg",
    "conium maculatum" to "https://upload.wikimedia.org/wikipedia/commons/b/b2/Conium.jpg",
    "digitalis purpurea" to "https://es.wikipedia.org/wiki/Digitalis_purpurea#/media/Archivo:Digitalis_purpurea_Sturm10033.jpg",
    "datura stramonium" to "https://upload.wikimedia.org/wikipedia/commons/6/63/Datura_stramonium_002.JPG",
    "hyoscyamus niger" to "https://upload.wikimedia.org/wikipedia/commons/thumb/4/4f/Hyoscyamus_niger_-_K%C3%B6hler%E2%80%93s_Medizinal-Pflanzen-073.jpg/330px-Hyoscyamus_niger_-_K%C3%B6hler%E2%80%93s_Medizinal-Pflanzen-073.jpg",
    "taxus baccata" to "https://upload.wikimedia.org/wikipedia/commons/2/24/Taxus_baccata_MHNT.jpg",
    "solanum nigrum" to "https://upload.wikimedia.org/wikipedia/commons/2/2c/Solanum_nigrum_fruits.JPG",
    "solanum dulcamara" to "https://upload.wikimedia.org/wikipedia/commons/7/74/Solanum_dulcamara_003.JPG",
    "nerium oleander" to "https://upload.wikimedia.org/wikipedia/commons/thumb/0/00/Nerium_oleander_Ouarzazate_wild1.jpg/500px-Nerium_oleander_Ouarzazate_wild1.jpg",
    "ricinus communis" to "https://upload.wikimedia.org/wikipedia/commons/3/3f/Ricinus_communis_001.JPG",
    "prunus laurocerasus" to "https://upload.wikimedia.org/wikipedia/commons/5/53/Prunus_laurocerasus_flowers.jpg",
    "colchicum autumnale" to "https://es.wikipedia.org/wiki/Colchicum_autumnale#/media/Archivo:Colchicum_autumnale_-_K%C3%B6hler%E2%80%93s_Medizinal-Pflanzen-044.jpg",
    "allium ursinum" to "https://upload.wikimedia.org/wikipedia/commons/8/8e/Allium_ursinum_-_Flickr_001.jpg",
    "convallaria majalis" to "https://upload.wikimedia.org/wikipedia/commons/1/1d/Convallaria_majalis_0002.JPG",
    "veratrum album" to "https://es.wikipedia.org/wiki/Veratrum_album#/media/Archivo:Illustration_Veratrum_album0.jpg",
    "arum maculatum" to "https://upload.wikimedia.org/wikipedia/commons/c/c1/Arum_maculatum_fruits.jpg",
    "sambucus nigra" to "https://upload.wikimedia.org/wikipedia/commons/3/3a/Sambucus_nigra_-_Flickr_001.jpg",
    "ligustrum vulgare" to "https://upload.wikimedia.org/wikipedia/commons/d/d8/Ligustrum_vulgare_fruit.jpg",
    "laburnum anagyroides" to "https://upload.wikimedia.org/wikipedia/commons/8/89/Laburnum_anagyroides.jpg",
    "rhododendron ferrugineum" to "https://upload.wikimedia.org/wikipedia/commons/8/8b/Rhododendron_ferrugineum_Ankogel_20190730.jpg",
    "oenanthe crocata" to "https://upload.wikimedia.org/wikipedia/commons/d/d1/Oenanthe_crocata_-_Flickr_001.jpg",
    "daphne mezereum" to "https://upload.wikimedia.org/wikipedia/commons/6/6c/Daphne_mezereum_-_Flickr_001.jpg",
    "nicotiana tabacum" to "https://upload.wikimedia.org/wikipedia/commons/a/a1/Nicotiana_tabacum_Blütenstand.jpg",
    "papaver somniferum" to "https://upload.wikimedia.org/wikipedia/commons/thumb/4/47/Papaver_somniferum_-_K%C3%B6hler%E2%80%93s_Medizinal-Pflanzen-102.jpg/330px-Papaver_somniferum_-_K%C3%B6hler%E2%80%93s_Medizinal-Pflanzen-102.jpg",
    "peganum harmala" to "https://upload.wikimedia.org/wikipedia/commons/8/8c/Peganum_harmala.jpg",
    "euphorbia lathyris" to "https://upload.wikimedia.org/wikipedia/commons/4/49/Kruisbladige_wolfsmelk_%28Euphorbia_lathyris%29_%28d.j.b.%29.jpg",
    "cicuta virosa" to "https://ast.wikipedia.org/wiki/Cicuta_virosa#/media/Ficheru:Cicuta_virosa_-_K%C3%B6hler%E2%80%93s_Medizinal-Pflanzen-038_cropped.jpg",
    "arundo donax" to "https://upload.wikimedia.org/wikipedia/commons/0/08/Arundo_donax_2.jpg",
    "phragmites australis" to "https://upload.wikimedia.org/wikipedia/commons/5/58/Phragmites_australis.jpg",
    "passiflora caerulea" to "https://upload.wikimedia.org/wikipedia/commons/4/49/Passiflora_caerulea.jpg",
    "anadenanthera colubrina" to "https://upload.wikimedia.org/wikipedia/commons/5/5c/Anadenanthera_colubrina.jpg",
    "mucuna pruriens" to "https://upload.wikimedia.org/wikipedia/commons/2/28/Mucuna_pruriens.jpg",
    "hypericum perforatum" to "https://upload.wikimedia.org/wikipedia/commons/5/56/Hypericum_perforatum_i04.jpg",
    "delphininium elatum" to "https://upload.wikimedia.org/wikipedia/commons/7/75/Delphinium_elatum-20200616-RM-080831.jpg",
    "delphinium elatum" to "https://upload.wikimedia.org/wikipedia/commons/7/75/Delphinium_elatum-20200616-RM-080831.jpg",
    "ranunculus acris" to "https://upload.wikimedia.org/wikipedia/commons/6/6f/Ranunculus_acris_close_up.jpg",
    "bryonia dioica" to "https://upload.wikimedia.org/wikipedia/commons/5/5b/Bryonia_dioica_berries_-_Flickr_001.jpg",

    // ── Especies/confundibles de la parte derecha ─────────────────────
    "judia comun" to "https://upload.wikimedia.org/wikipedia/commons/c/ca/Snijboon_peulen_Phaseolus_vulgaris.jpg",
    "judia" to "https://upload.wikimedia.org/wikipedia/commons/c/ca/Snijboon_peulen_Phaseolus_vulgaris.jpg",
    "habichuela" to "https://upload.wikimedia.org/wikipedia/commons/c/ca/Snijboon_peulen_Phaseolus_vulgaris.jpg",
    "garbanzo" to "https://upload.wikimedia.org/wikipedia/commons/thumb/a/aa/Cicer_arietinum_003.JPG/250px-Cicer_arietinum_003.JPG",
    "perejil" to "https://upload.wikimedia.org/wikipedia/commons/2/2d/Petroselinum_crispum_-_K%C3%B6hler%E2%80%93s_Medizinal-Pflanzen-103.jpg",
    "rabano rusticano" to "https://upload.wikimedia.org/wikipedia/commons/b/b6/Armoracia_rusticana.jpg",
    "genciana azul" to "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a6/GentianaAcaulisRannoch.jpg/250px-GentianaAcaulisRannoch.jpg",
    "genciana amarilla" to "https://upload.wikimedia.org/wikipedia/commons/9/99/Gentiana_lutea_090705.jpg",
    "cereza" to "https://upload.wikimedia.org/wikipedia/commons/4/49/Prunus_avium_fruit.jpg",
    "mora" to "https://species.wikimedia.org/wiki/Rubus_fruticosus#/media/File:Rubus_plicatus_kz1.jpg",
    "arandano" to "https://upload.wikimedia.org/wikipedia/commons/5/5c/203_Vaccinum_myrtillus_L.jpg",
    "arandano rojo" to "https://upload.wikimedia.org/wikipedia/commons/5/5c/203_Vaccinum_myrtillus_L.jpg",
    "zanahoria silvestre" to "https://es.wikipedia.org/wiki/Daucus_carota#/media/Archivo:Daucus_Carota.jpg",
    "zanahoria" to "https://es.wikipedia.org/wiki/Daucus_carota#/media/Archivo:Daucus_Carota.jpg",
    "anis" to "https://es.wikipedia.org/wiki/Pimpinella_anisum#/media/Archivo:Koehler1887-PimpinellaAnisum.jpg",
    "hinojo" to "https://es.wikipedia.org/wiki/Foeniculum_vulgare#/media/Archivo:Foeniculum_vulgare_-_K%C3%B6hler%E2%80%93s_Medizinal-Pflanzen-148.jpg",
    "consuelda" to "https://upload.wikimedia.org/wikipedia/commons/0/06/Symphytum_officinale_-_K%C3%B6hler%E2%80%93s_Medizinal-Pflanzen-268.jpg",
    "gordolobo" to "https://es.wikipedia.org/wiki/Verbascum_phlomoides#/media/Archivo:Verbascum_phlomoides_-_K%C3%B6hler%E2%80%93s_Medizinal-Pflanzen-144.jpg",
    "verbascum" to "https://es.wikipedia.org/wiki/Verbascum_phlomoides#/media/Archivo:Verbascum_phlomoides_-_K%C3%B6hler%E2%80%93s_Medizinal-Pflanzen-144.jpg",
    "pimiento" to "https://upload.wikimedia.org/wikipedia/commons/d/d2/Capsicum_annuum_-_K%C3%B6hler%E2%80%93s_Medizinal-Pflanzen-027.jpg",
    "tomate silvestre" to "https://upload.wikimedia.org/wikipedia/commons/8/89/Tomato_je.jpg",
    "tomate" to "https://upload.wikimedia.org/wikipedia/commons/8/89/Tomato_je.jpg",
    "tomate cherry" to "https://upload.wikimedia.org/wikipedia/commons/8/88/Bright_red_tomato_and_cross_section02.jpg",
    "okra" to "https://upload.wikimedia.org/wikipedia/commons/thumb/9/95/Hong_Kong_Okra_Aug_25_2012.JPG/1024px-Hong_Kong_Okra_Aug_25_2012.JPG",
    "remolacha silvestre" to "https://upload.wikimedia.org/wikipedia/commons/f/ff/Beta_vulgaris_-_K%C3%B6hler%E2%80%93s_Medizinal-Pflanzen-167.jpg",
    "veronica" to "https://upload.wikimedia.org/wikipedia/commons/4/41/Veronica_officinalis_1543.JPG",
    "enebro" to "https://upload.wikimedia.org/wikipedia/commons/2/26/Juniperus_communis_berries.jpg",
    "picea" to "https://upload.wikimedia.org/wikipedia/commons/4/42/Picea_abies.jpg",
    "rosa" to "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c6/Rosa_%27Ambiente%27%2C_Bad_W%C3%B6rishofen%2C_Alemania%2C_2019-06-20%2C_DD_25.jpg/1024px-Rosa_%27Ambiente%27%2C_Bad_W%C3%B6rishofen%2C_Alemania%2C_2019-06-20%2C_DD_25.jpg",
    "rosa silvestre" to "https://upload.wikimedia.org/wikipedia/commons/3/32/Divlja_ruza_cvijet_270508.jpg",
    "physalis" to "https://upload.wikimedia.org/wikipedia/commons/8/88/Physalis_peruviana_fruit.jpg",
    "laurel" to "https://upload.wikimedia.org/wikipedia/commons/a/a2/Starr-071024-0195-Laurus_nobilis-leaves-Enchanting_Floral_Gardens_of_Kula-Maui_%2824867859296%29.jpg",
    "eucalipto" to "https://upload.wikimedia.org/wikipedia/commons/thumb/6/6b/Starr_051123-5467_Eucalyptus_globulus.jpg/250px-Starr_051123-5467_Eucalyptus_globulus.jpg",
    "alcaparra" to "https://upload.wikimedia.org/wikipedia/commons/2/26/Capparis_spinosa_1.jpg",
    "higuera" to "https://upload.wikimedia.org/wikipedia/commons/2/2e/Ficus_carica_L%2C_1771.jpg",
    "cerezo silvestre" to "https://upload.wikimedia.org/wikipedia/commons/4/49/Prunus_avium_fruit.jpg",
    "ajo silvestre" to "https://upload.wikimedia.org/wikipedia/commons/4/45/Photo_of_Allium_Ursinum%2C_wild_garlic%2C_north-west_Hampshire%2C_UK%2C_May_2014.jpg",
    "cebolla silvestre" to "https://upload.wikimedia.org/wikipedia/commons/d/da/Alliumvineale1web.jpg",
    "polygonatum" to "https://upload.wikimedia.org/wikipedia/commons/4/48/Polygonatum_verticillatum01.jpg",
    "arnica" to "https://es.wikipedia.org/wiki/Arnica#/media/Archivo:Arnica_montana_-_K%C3%B6hler%E2%80%93s_Medizinal-Pflanzen-015.jpg",
    "castana de indias" to "https://upload.wikimedia.org/wikipedia/commons/4/43/Aesculus_hippocastanum_-_Flickr_001.jpg",
    "sauco" to "https://upload.wikimedia.org/wikipedia/commons/3/3a/Sambucus_nigra_-_Flickr_001.jpg",
    "acacia" to "https://upload.wikimedia.org/wikipedia/commons/thumb/0/0a/Acacia_dealbata_2601.jpg/500px-Acacia_dealbata_2601.jpg",
    "mimosa" to "https://upload.wikimedia.org/wikipedia/commons/thumb/0/0a/Acacia_dealbata_2601.jpg/500px-Acacia_dealbata_2601.jpg",
    "brezo" to "https://upload.wikimedia.org/wikipedia/commons/7/7d/Calluna_vulgaris_-_Flickr_001.jpg",
    "apio silvestre" to "https://es.wikipedia.org/wiki/Apium_graveolens#/media/Archivo:Illustration_Apium_graveolens0.jpg",
    "madreselva" to "https://upload.wikimedia.org/wikipedia/commons/8/81/Lonicera_periclymenum_-_Flickr_001.jpg",
    "patata" to "https://upload.wikimedia.org/wikipedia/commons/a/ab/Patates.jpg",
    "amapola silvestre" to "https://upload.wikimedia.org/wikipedia/commons/thumb/9/93/Papaver_rhoeas_-_K%C3%B6hler%E2%80%93s_Medizinal-Pflanzen-101.jpg/250px-Papaver_rhoeas_-_K%C3%B6hler%E2%80%93s_Medizinal-Pflanzen-101.jpg",
    "papaver rhoeas" to "https://upload.wikimedia.org/wikipedia/commons/thumb/9/93/Papaver_rhoeas_-_K%C3%B6hler%E2%80%93s_Medizinal-Pflanzen-101.jpg/250px-Papaver_rhoeas_-_K%C3%B6hler%E2%80%93s_Medizinal-Pflanzen-101.jpg",
    "ruda" to "https://upload.wikimedia.org/wikipedia/commons/2/29/Ruta_graveolens_001.JPG",
    "artemisa" to "https://upload.wikimedia.org/wikipedia/commons/6/6f/ArtemisiaVulgaris.jpg",
    "bambu" to "https://upload.wikimedia.org/wikipedia/commons/8/8e/Phyllostachys_aurea_-_Flickr_001.jpg",
    "phragmites" to "https://upload.wikimedia.org/wikipedia/commons/5/58/Phragmites_australis.jpg",
    "maracuya" to "https://upload.wikimedia.org/wikipedia/commons/f/fb/Passiflora_edulis_flower.jpg",
    "granadilla" to "https://upload.wikimedia.org/wikipedia/commons/8/88/Passiflora_ligularis_%2814642851748%29.jpg",
    "algarrobo" to "https://upload.wikimedia.org/wikipedia/commons/thumb/0/0a/Illustration_Ceratonia_siliqua0.jpg/250px-Illustration_Ceratonia_siliqua0.jpg",
    "hypericum calycinum" to "https://upload.wikimedia.org/wikipedia/commons/6/6f/Hypericum_calycinum1.jpg",
    "tutsan" to "https://upload.wikimedia.org/wikipedia/commons/8/85/Hypericum_androsaemum_Dziurawiec_barwierski_01.jpg",
    "consolida" to "https://upload.wikimedia.org/wikipedia/commons/5/54/Consolida_ajacis_-_Flickr_001.jpg",
    "ficaria verna" to "https://upload.wikimedia.org/wikipedia/commons/2/21/Ficaria_verna1.jpg",
    "trollius europaeus" to "http://calphotos.berkeley.edu/imgs/512x768/0000_0000/0105/0844.jpeg",
    "vid silvestre" to "https://upload.wikimedia.org/wikipedia/commons/d/dd/Cabernet_Sauvignon_Gaillac.jpg"
)
