package com.toxicplants.database.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────
// Modelos
// ─────────────────────────────────────────────
data class BerryInfo(
    val name: String,
    val scientificName: String,
    val description: String,
    val lookAlike: String,
    val imageEmoji: String,
    val toxicity: String,
    val wikipediaUrl: String = ""
)

data class ConfusionItem(
    val name1: String, val emoji1: String, val label1: String,
    val name2: String, val emoji2: String, val label2: String,
    val tip: String,   val danger: String
)

// ─────────────────────────────────────────────
// Pantalla
// ─────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BerriesScreen(onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme

    val edibleBerries = listOf(
        BerryInfo("Arándano",           "Vaccinium myrtillus",    "Pequeñas bayas azul oscuro, dulces y nutritivas. Crecen en landas ácidas.",          "Gayuba (Arctostaphylos uva-ursi) - hojas diferentes",       "🫐", "Segura",            "https://es.wikipedia.org/wiki/Vaccinium_myrtillus"),
        BerryInfo("Arándano rojo",      "Vaccinium vitis-idaea",  "Rojas, pequeñas, cranberries europeos. Ácidas pero comestibles.",                     "Gayuba - sabor harinoso",                                   "🔴", "Segura",            "https://es.wikipedia.org/wiki/Vaccinium_vitis-idaea"),
        BerryInfo("Arándano americano", "Vaccinium corymbosum",   "Más grandes que el europeo, azules. Muy cultivados.",                                 "Similar al europeo",                                        "🫐", "Segura",            "https://es.wikipedia.org/wiki/Vaccinium_corymbosum"),
        BerryInfo("Fresa silvestre",    "Fragaria vesca",         "Rojas, pequeñas, muy aromáticas. Dulces y silvestres.",                               "Fresa de töis (Daphne mezereum) - alargadas, olor fuerte",  "🍓", "Segura",            "https://es.wikipedia.org/wiki/Fragaria_vesca"),
        BerryInfo("Fresa blanca",       "Fragaria viridis",       "Blancas o rojizas, más dulces que la silvestre.",                                     "Similar a la silvestre",                                    "🍓", "Segura",            "https://es.wikipedia.org/wiki/Fragaria_viridis"),
        BerryInfo("Mora/Zarzamora",     "Rubus fruticosus",       "Negras, brillantes, carnosas. Muy dulces cuando maduras.",                            "Yezgo (Sambucus ebulus) - en ramilletes erectos",           "🫐", "Segura",            "https://es.wikipedia.org/wiki/Rubus_fruticosus"),
        BerryInfo("Zarzamora",          "Rubus ulmifolius",       "Negras, carnosas. Comunes en setos y bordes.",                                        "Yezgo - olor fétido",                                       "🫐", "Segura",            "https://es.wikipedia.org/wiki/Rubus_ulmifolius"),
        BerryInfo("Frambuesa",          "Rubus idaeus",           "Rojas, huecas por dentro, muy dulces. Fruto del bosque.",                             "Zaragatona - frutos huecos",                                "🍓", "Segura",            "https://es.wikipedia.org/wiki/Rubus_idaeus"),
        BerryInfo("Grosella roja",      "Ribes rubrum",           "Rojas o blancas, translúcidas. Ácidas pero comestibles.",                             "Acebo - hojas espinosas",                                   "🔴", "Segura",            "https://es.wikipedia.org/wiki/Ribes_rubrum"),
        BerryInfo("Grosella negra",     "Ribes nigrum",           "Negras, aromáticas. Ricas en vitamina C.",                                            "Grosella espinosa - tiene espinas",                         "🫐", "Segura",            "https://es.wikipedia.org/wiki/Ribes_nigrum"),
        BerryInfo("Grosella blanca",    "Ribes sativum",          "Blancas, translúcidas. Más dulces que las rojas.",                                    "Similar a grosella roja",                                   "⚪", "Segura",            "https://es.wikipedia.org/wiki/Ribes_sativum"),
        BerryInfo("Grosella espinosa",  "Ribes uva-crispa",       "Verdes, rojizas o amarillas. Ácidas, necesitan azúcar.",                              "Grosella roja - sin espinas",                               "🟢", "Segura",            "https://es.wikipedia.org/wiki/Ribes_uva-crispa"),
        BerryInfo("Aronia",             "Aronia melanocarpa",     "Negras, pequeñas, astringentes. Rico en antioxidantes.",                              "Similar a arándano negro",                                  "🫐", "Segura",            "https://es.wikipedia.org/wiki/Aronia_melanocarpa"),
        BerryInfo("Espino amarillo",    "Hippophae rhamnoides",   "Naranjas, pequeñas. Ricas en vitamina C. Ácidas.",                                    "Similar a serbal pero naranja",                             "🟠", "Segura",            "https://es.wikipedia.org/wiki/Hippophae_rhamnoides"),
        BerryInfo("Cornejo hembra",     "Cornus mas",             "Rojas, alargadas. Comestibles, algo ácidas.",                                         "Majuelo - frutos diferentes",                               "🔴", "Segura",            "https://es.wikipedia.org/wiki/Cornus_mas"),
        BerryInfo("Serbal",             "Sorbus aucuparia",       "Rojas, pequeñas, amargas frescas. Comestibles cocinadas.",                            "Acebo - hojas diferentes",                                  "🔴", "Segura (cocinado)", "https://es.wikipedia.org/wiki/Sorbus_aucuparia"),
        BerryInfo("Saúco negro",        "Sambucus nigra",         "Negras, en ramilletes grandes. COMESTIBLES COCIDAS.",                                 "Yezgo - ramilletes erectos, olor",                          "🫐", "Segura (cocinado)", "https://es.wikipedia.org/wiki/Sambucus_nigra"),
        BerryInfo("Madreselva",         "Lonicera periclymenum",  "Rojas, pequeñas. Comestibles pero sosas.",                                            "Madreselva japonesa - tóxica",                              "🔴", "Segura",            "https://es.wikipedia.org/wiki/Lonicera_periclymenum"),
        BerryInfo("Wolfberry/Goji",     "Lycium barbarum",        "Rojas, alargadas. Muy saludables, antioxidantes.",                                    "Lycium - confirmar especie",                                "🔴", "Segura",            "https://es.wikipedia.org/wiki/Lycium_barbarum"),
        BerryInfo("Mirtilo",            "Vaccinium uliginosum",   "Azules, más grandes que arándano. Dulces.",                                           "Arándano - muy similares",                                  "🫐", "Segura",            "https://es.wikipedia.org/wiki/Vaccinium_uliginosum"),
        BerryInfo("Cornus kousa",       "Cornus kousa",           "Rojas, carnosas. Comestibles, sabor dulce.",                                          "Cornus mas - más pequeñas",                                 "🔴", "Segura",            "https://es.wikipedia.org/wiki/Cornus_kousa"),
        BerryInfo("Jostaberry",         "Ribes × nidigrolaria",   "Negras, grandes, híbridas. Sin espinas, dulces.",                                     "Grosella negra",                                            "🫐", "Segura",            "https://es.wikipedia.org/wiki/Jostaberry"),
        BerryInfo("Tayberry",           "Rubus loganobaccus",     "Rojas, grandes, híbrida. Muy dulce.",                                                 "Frambuesa - más grande",                                    "🍓", "Segura",            "https://es.wikipedia.org/wiki/Tayberry")
    )

    val toxicBerries = listOf(
        BerryInfo("Belladona",           "Atropa belladonna",    "Negras, brillantes. MUY TÓXICA. 2-3 bayas pueden ser letales en niños.",              "Arándano - sabor amargo, hojas diferentes",  "⚫", "MORTAL",         "https://es.wikipedia.org/wiki/Atropa_belladonna"),
        BerryInfo("Yezgo",               "Sambucus ebulus",      "Negras, en ramilletes erectos. Tóxicas. Olor desagradable.",                          "Mora - ramilletes colgantes",                "🫐", "TÓXICA",         "https://es.wikipedia.org/wiki/Sambucus_ebulus"),
        BerryInfo("Acebo",               "Ilex aquifolium",      "Rojas, brillantes. Tóxicas en cantidad. Hojas espinosas.",                            "Grosella - hojas lisas",                     "🔴", "TÓXICA",         "https://es.wikipedia.org/wiki/Ilex_aquifolium"),
        BerryInfo("Dulcamara",           "Solanum dulcamara",    "Rojas o negras, alargadas. Tóxicas. Enredadera.",                                     "Grosella - forma diferente",                 "🔴", "TÓXICA",         "https://es.wikipedia.org/wiki/Solanum_dulcamara"),
        BerryInfo("Estramonio",          "Datura stramonium",    "Verdes, luego amarillas. MUY TÓXICA. Espinas en fruto.",                              "Tomate silvestre - olor fétido",             "🟢", "MORTAL",         "https://es.wikipedia.org/wiki/Datura_stramonium"),
        BerryInfo("Fresa de töis",       "Daphne mezereum",      "Rojas, brillantes, muy olorosas. MUY TÓXICA.",                                        "Fresa - alargadas, olor",                    "🍓", "MORTAL",         "https://es.wikipedia.org/wiki/Daphne_mezereum"),
        BerryInfo("Torvisco",            "Daphne gnidium",       "Blancas, pequeñas. MUY TÓXICA.",                                                      "Otras bayas blancas",                        "⚪", "MORTAL",         "https://es.wikipedia.org/wiki/Daphne_gnidium"),
        BerryInfo("Lauréola",            "Daphne laureola",      "Negras, pequeñas. MUY TÓXICA. Crece en bosques.",                                     "Otros Daphne",                               "⚫", "MORTAL",         "https://es.wikipedia.org/wiki/Daphne_laureola"),
        BerryInfo("Tejo",                "Taxus baccata",        "Rojas con semilla negra. TODA LA PLANTA ES MORTAL.",                                  "Ninguna - reconocible por hojas",            "🔴", "MORTAL",         "https://es.wikipedia.org/wiki/Taxus_baccata"),
        BerryInfo("Muérdago",            "Viscum album",         "Blancas, viscosas. Tóxico. Crece en ramas.",                                          "Acebo - diferente textura",                  "⚪", "TÓXICA",         "https://es.wikipedia.org/wiki/Viscum_album"),
        BerryInfo("Adelfa",              "Nerium oleander",      "TODA LA PLANTA ES TÓXICA. Flores rosas.",                                             "Oleander - todas partes tóxicas",            "⚪", "MORTAL",         "https://es.wikipedia.org/wiki/Nerium_oleander"),
        BerryInfo("Evónimo",             "Euonymus europaeus",   "Rojas, rosadas, brillantes. Tóxico. Cápsulas rosas.",                                 "Arilo rojo - confusión",                     "🔴", "TÓXICA",         "https://es.wikipedia.org/wiki/Euonymus_europaeus"),
        BerryInfo("Aligustre",           "Ligustrum vulgare",    "Negras, pequeñas. Tóxico. Flores blancas olorosas.",                                  "Saúco - olor diferente",                     "⚫", "TÓXICA",         "https://es.wikipedia.org/wiki/Ligustrum_vulgare"),
        BerryInfo("Solano negro",        "Solanum nigrum",       "Negras, pequeñas. Tóxico. Hierba, no arbusto.",                                       "Arándano - hábito diferente",                "⚫", "TÓXICA",         "https://es.wikipedia.org/wiki/Solanum_nigrum"),
        BerryInfo("Physalis",            "Physalis alkekengi",   "Naranjas, dentro de farolillo. Tóxicas crudas.",                                      "Similar a tomate cherry",                    "🟠", "TÓXICA (crudo)", "https://es.wikipedia.org/wiki/Physalis_alkekengi"),
        BerryInfo("Arum",                "Arum maculatum",       "Rojas, brillantes. Tóxico. Fruto de jaras.",                                          "Otros - confundible",                        "🔴", "TÓXICA",         "https://es.wikipedia.org/wiki/Arum_maculatum"),
        BerryInfo("Cicuta",              "Conium maculatum",     "Verdes, pequeñas. MUY TÓXICA. Hierba alta.",                                          "Otras umbelíferas - peligroso",              "🟢", "MORTAL",         "https://es.wikipedia.org/wiki/Conium_maculatum"),
        BerryInfo("Hiedra",              "Hedera helix",         "Negras, pequeñas. Tóxicas. Enredadera.",                                              "Otras enredaderas",                          "⚫", "TÓXICA",         "https://es.wikipedia.org/wiki/Hedera_helix"),
        BerryInfo("Madreselva japonesa", "Lonicera japonica",    "Negras, pequeñas. Tóxica. Enredadera.",                                               "Madreselva europea - comestible",            "⚫", "TÓXICA",         "https://es.wikipedia.org/wiki/Lonicera_japonica"),
        BerryInfo("Viburno",             "Viburnum opulus",      "Rojas, pequeñas. Tóxico. Ramilletes planos.",                                         "Arándano - ramilletes diferentes",           "🔴", "TÓXICA",         "https://es.wikipedia.org/wiki/Viburnum_opulus"),
        BerryInfo("Kalmia",              "Kalmia latifolia",     "Rojas, pequeñas. Tóxica. Arbusto ornamental.",                                        "Arándano - hojas diferentes",                "🔴", "TÓXICA",         "https://es.wikipedia.org/wiki/Kalmia_latifolia"),
        BerryInfo("Pieris",              "Pieris japonica",      "Rojas o blancas. Tóxica. Arbusto ornamental.",                                        "Arándano - hojas diferentes",                "🔴", "TÓXICA",         "https://es.wikipedia.org/wiki/Pieris_japonica")
    )

    val confusions = listOf(
        ConfusionItem("Mora", "🫐", "✅ COMESTIBLE", "Yezgo", "⚫", "☠️ TÓXICA",
            "Las moras cuelgan hacia abajo; el yezgo tiene racimos erectos y huele mal.",
            "El yezgo causa vómitos, diarrea y puede ser peligroso."),
        ConfusionItem("Fresa silvestre", "🍓", "✅ COMESTIBLE", "Fresa de töis", "🍓", "☠️ MORTAL",
            "La fresa de töis es más alargada, huele muy fuerte (nauseabundo), hojas lanceoladas.",
            "La fresa de töis puede ser mortal. Contiene mezereína."),
        ConfusionItem("Arándano", "🫐", "✅ COMESTIBLE", "Belladona", "⚫", "☠️ MORTAL",
            "Los arándanos son mate y saben dulce; la belladona es brillante y sabe amargo.",
            "La belladona puede causar la muerte. Contiene atropina."),
        ConfusionItem("Grosella", "🔴", "✅ COMESTIBLE", "Acebo", "🔴", "☠️ TÓXICA",
            "El acebo tiene hojas espinosas; la grosella tiene hojas blandas.",
            "El acebo en cantidad causa problemas gastrointestinales."),
        ConfusionItem("Grosella roja", "🔴", "✅ COMESTIBLE", "Dulcamara", "🔴", "☠️ TÓXICA",
            "La grosella es redonda y translúcida; la dulcamara es alargada y en enredaderas.",
            "La dulcamara contiene alcaloides tóxicos."),
        ConfusionItem("Saúco negro", "🫐", "✅ COCIDO", "Yezgo", "🫐", "☠️ TÓXICA",
            "El saúco negro tiene ramilletes colgantes; el yezgo tiene ramilletes erectos y huele mal.",
            "Siempre cocinar el saúco, nunca comer yezgo."),
        ConfusionItem("Arándano rojo", "🔴", "✅ COMESTIBLE", "Gayuba", "🔴", "⚠️ MOLESTO",
            "Muy similares. La gayuba tiene hojas más brillantes y sabor harinoso.",
            "La gayuba causa malestar gastrointestinal leve."),
        ConfusionItem("Frambuesa", "🍓", "✅ COMESTIBLE", "Solano negro", "⚫", "☠️ TÓXICA",
            "La frambuesa es hueca y crece en zarzas; el solano negro es sólido y en hierbas.",
            "El solano negro contiene solanina."),
        ConfusionItem("Serbal", "🔴", "✅ COCIDO", "Acebo", "🔴", "☠️ TÓXICA",
            "El serbal tiene hojas pinnadas; el acebo tiene hojas espinosas brillantes.",
            "El acebo causa problemas digestivos."),
        ConfusionItem("Physalis", "🟠", "⚠️ COCIDO", "Tomate cherry", "🍅", "✅ COMESTIBLE",
            "El physalis tiene farolillo naranja; el tomate cherry no lo tiene.",
            "El physalis crudo es tóxico; cocido puede ser comestible.")
    )

    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredEdible = edibleBerries.filter {
        searchQuery.isBlank() ||
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.scientificName.contains(searchQuery, ignoreCase = true)
    }
    val filteredToxic = toxicBerries.filter {
        searchQuery.isBlank() ||
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.scientificName.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            Surface(modifier = Modifier.fillMaxWidth(), color = colors.primary) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = colors.onPrimary)
                        }
                        Spacer(Modifier.width(4.dp))
                        Text("🫐 Guía de Bayas", color = colors.onPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedTextField(
                        value         = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier      = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                        placeholder   = { Text("Buscar baya...", fontSize = 14.sp) },
                        singleLine    = true,
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor      = colors.onPrimary,
                            unfocusedBorderColor    = colors.onPrimary.copy(alpha = 0.5f),
                            focusedTextColor        = colors.onPrimary,
                            unfocusedTextColor      = colors.onPrimary,
                            focusedContainerColor   = colors.primaryContainer.copy(alpha = 0.3f),
                            unfocusedContainerColor = colors.primaryContainer.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            Surface(modifier = Modifier.fillMaxWidth(), color = colors.error.copy(alpha = 0.15f)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("⚠️", fontSize = 24.sp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("¡ATENCIÓN!", fontWeight = FontWeight.Bold, color = colors.error)
                        Text("Nunca comas bayas si no estás 100% seguro. Cuando dudes, NO COMAS.",
                            fontSize = 12.sp, color = colors.error)
                    }
                }
            }

            // ← PrimaryTabRow reemplaza TabRow deprecated
            PrimaryTabRow(selectedTabIndex = selectedTab, containerColor = colors.surface) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                    text = { Text("🍓 Comestibles (${filteredEdible.size})", fontSize = 12.sp) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                    text = { Text("☠️ Tóxicas (${filteredToxic.size})", fontSize = 12.sp) })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 },
                    text = { Text("⚠️ Confusiones", fontSize = 12.sp) })
            }

            when (selectedTab) {
                0 -> LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(filteredEdible) { berry -> BerryCard(berry = berry, isEdible = true) }
                    if (filteredEdible.isEmpty()) item {
                        Text("No hay resultados", color = colors.onSurfaceVariant, modifier = Modifier.padding(16.dp))
                    }
                }
                1 -> LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(filteredToxic) { berry -> BerryCard(berry = berry, isEdible = false) }
                    if (filteredToxic.isEmpty()) item {
                        Text("No hay resultados", color = colors.onSurfaceVariant, modifier = Modifier.padding(16.dp))
                    }
                }
                2 -> LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(confusions) { confusion -> ConfusionCard(confusion = confusion) }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// Tarjeta baya + botón Wikipedia
// ─────────────────────────────────────────────
@Composable
fun BerryCard(berry: BerryInfo, isEdible: Boolean) {
    val colors        = MaterialTheme.colorScheme
    val context       = LocalContext.current
    val toxicityColor = if (isEdible) Color(0xFF4CAF50) else colors.error
    val toxicityBg    = if (isEdible) Color(0xFF4CAF50).copy(alpha = 0.15f) else Color(0xFFB71C1C).copy(alpha = 0.15f)

    Card(
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(45.dp).background(toxicityBg, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center) {
                    Text(berry.imageEmoji, fontSize = 24.sp)
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(berry.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(berry.scientificName, fontStyle = FontStyle.Italic, fontSize = 10.sp, color = colors.onSurfaceVariant)
                    Text(berry.description, fontSize = 11.sp, color = colors.onSurface, maxLines = 2)
                    if (berry.lookAlike.isNotBlank()) {
                        Text("Confundible con: ${berry.lookAlike}", fontSize = 10.sp,
                            color = Color(0xFFFF9800), maxLines = 1)
                    }
                }
                Spacer(Modifier.width(6.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(color = toxicityBg, shape = RoundedCornerShape(4.dp)) {
                        Text(berry.toxicity,
                            modifier   = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize   = 9.sp, fontWeight = FontWeight.Bold,
                            color      = toxicityColor, textAlign = TextAlign.Center)
                    }
                    if (berry.wikipediaUrl.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Surface(
                            color    = Color(0xFF1565C0).copy(alpha = 0.15f),
                            shape    = RoundedCornerShape(6.dp),
                            modifier = Modifier.clickable {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(berry.wikipediaUrl)))
                            }
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.OpenInBrowser, "Wiki",
                                    tint = Color(0xFF1E88E5), modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(3.dp))
                                Text("Wiki", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E88E5))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// Tarjeta confusión
// ─────────────────────────────────────────────
@Composable
fun ConfusionCard(confusion: ConfusionItem) {
    val colors = MaterialTheme.colorScheme
    Card(modifier = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(confusion.emoji1, fontSize = 32.sp)
                    Text(confusion.name1, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF4CAF50))
                    Surface(color = Color(0xFF4CAF50).copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                        Text(confusion.label1, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
                Text("vs", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.onSurfaceVariant)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(confusion.emoji2, fontSize = 32.sp)
                    Text(confusion.name2, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = colors.error)
                    Surface(color = colors.error.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                        Text(confusion.label2, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            color = colors.error, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Surface(color = Color(0xFFFFF3E0), shape = RoundedCornerShape(6.dp)) {
                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.Top) {
                    Text("💡", fontSize = 14.sp); Spacer(Modifier.width(6.dp))
                    Text(confusion.tip, fontSize = 11.sp, color = Color(0xFF5D4037))
                }
            }
            Spacer(Modifier.height(6.dp))
            Surface(color = colors.error.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                Row(modifier = Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("⚠️", fontSize = 12.sp); Spacer(Modifier.width(4.dp))
                    Text(confusion.danger, fontSize = 10.sp, color = colors.error, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
