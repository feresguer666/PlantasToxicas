package com.toxicplants.database.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BerriesScreen(onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme

    // ==================== BAYAS COMESTIBLES ====================
    val edibleBerries = listOf(
        BerryInfo("Arándano", "Vaccinium myrtillus", "Pequeñas bayas azul oscuro, dulces y nutritivas. Crecen en landas ácidas.", "Gayuba (Arctostaphylos uva-ursi) - hojas diferentes", "🫐", "Segura"),
        BerryInfo("Arándano rojo", "Vaccinium vitis-idaea", "Rojas, pequeñas, cranberries europeos. Ácidas pero comestibles.", "Gayuba - sabor harinoso", "🔴", "Segura"),
        BerryInfo("Arándano americano", "Vaccinium corymbosum", "Más grandes que el europeo, azules. Muy cultivados.", "Similar al europeo", "🫐", "Segura"),
        BerryInfo("Fresa silvestre", "Fragaria vesca", "Rojas, pequeñas, muy aromáticas. Dulces y silvestres.", "Fresa de töis (Daphne mezereum) - alargadas, olor fuerte", "🍓", "Segura"),
        BerryInfo("Fresa blanca", "Fragaria viridis", "Blancas o rojizas, más dulces que la silvestre.", "Similar a la silvestre", "🍓", "Segura"),
        BerryInfo("Mora/Zarzamora", "Rubus fruticosus", "Negras, brillantes, carnosas. Muy dulces cuando maduras.", "Yezgo (Sambucus ebulus) - en ramilletes erectos", "🫐", "Segura"),
        BerryInfo("Zarzamora", "Rubus ulmifolius", "Negras, carnosas. Comunes en setos y bordes.", "Yezgo - olor fétido", "🫐", "Segura"),
        BerryInfo("Frambuesa", "Rubus idaeus", "Rojas, huecas por dentro, muy dulces. Fruto del bosque.", "Zaragatona - frutos huecos", "🍓", "Segura"),
        BerryInfo("Grosella roja", "Ribes rubrum", "Rojas o blancas, translúcidas. Ácidas pero comestibles.", "Acebo - hojas espinosas", "🔴", "Segura"),
        BerryInfo("Grosella negra", "Ribes nigrum", "Negras, aromáticas. Ricas en vitamina C.", "Grosella espinosa - tiene espinas", "🫐", "Segura"),
        BerryInfo("Grosella blanca", "Ribes sativum", "Blancas, translúcidas. Más dulces que las rojas.", "Similar a grosella roja", "⚪", "Segura"),
        BerryInfo("Grosella espinosa", "Ribes uvacrispa", "Verdes, rojizas o amarillas. Ácidas, necesitan azúcar.", "Grosella roja - sin espinas", "🟢", "Segura"),
        BerryInfo("Aronia", "Aronia melanocarpa", "Negras, pequeñas, astringentes. Rico en antioxidantes.", "Similar a arándano negro", "🫐", "Segura"),
        BerryInfo("Espino amarillo", "Hippophae rhamnoides", "Naranjas, pequeñas. Ricas en vitamina C. Ácidas.", "Similar a serbal pero naranja", "🟠", "Segura"),
        BerryInfo("Cornejo hembra", "Cornus mas", "Rojas, alargadas. Comestibles, algo ácidas.", "Majuelo - frutos diferentes", "🔴", "Segura"),
        BerryInfo("Serbal", "Sorbus aucuparia", "Rojas, pequeñas, amargas frescas. Comestibles cocinadas.", "Acebo - hojas diferentes", "🔴", "Segura (cocinado)"),
        BerryInfo("Saúco negro", "Sambucus nigra", "Negras, en ramilletes grandes. COMESTIBLES COCIDAS.", "Yezgo - ramilletes erectos, olor", "🫐", "Segura (cocinado)"),
        BerryInfo("Madreselva", "Lonicera periclymenum", "Rojas, pequeñas. Comestibles pero sosas.", "Madreselva japonesa - tóxica", "🔴", "Segura"),
        BerryInfo("Liquen de los renos", "Cladonia rangiferina", "Pequeñas bayas blancas. Comestibles en supervivencia.", "Otros líquenes - difícil distinción", "⚪", "Segura (emergencia)"),
        BerryInfo("Uva de gato", "Sedum acre", "Amarillas, pequeñas. Comestibles, algo amargas.", " otros Sedum - confirmar especie", "🟡", "Segura"),
        BerryInfo("Ráspano/Raspberry de los Alpes", "Rubus chamaemorus", "Rojas, parfum. Raras, de turberas ácidas.", "Ninguna confusión común", "🍓", "Segura"),
        BerryInfo("Mirtilo", "Vaccinium uliginosum", "Azules, más grandes que arándano. Dulces.", "Arándano - muy similares", "🫐", "Segura"),
        BerryInfo("Arándano ojo de conejo", "Vaccinium ovatum", "Negras, pequeñas. De arbustos ornamentales.", "Arándano común", "🫐", "Segura"),
        BerryInfo("Skimmia", "Skimmia japonica", "Rojas, brillantes. Comestibles en pequeñas cantidades.", "Acebo - hojas diferentes", "🔴", "Segura (moderación)"),
        BerryInfo("Baya del saúco", "Sambucus canadensis", "Negras, grandes. Comestibles cocinadas.", "Similar al saúco negro", "🫐", "Segura (cocinado)"),
        BerryInfo("Cornus kousa", "Cornus kousa", "Rojas, carnosas. Comestibles, sabor dulce.", "Cornus mas - más pequeñas", "🔴", "Segura"),
        BerryInfo("Hackberry", "Celtis sinensis", "Negras, pequeñas, dulces. Árbol urbano.", "Ninguna confusión común", "⚫", "Segura"),
        BerryInfo("Jostaberry", "Ribes nidigrolaria", "Negras, grandes, híbridas. Sin espinas, dulces.", "Grosella negra", "🫐", "Segura"),
        BerryInfo("Tayberry", "Rubus loganobaccus", "Rojas, grandes, híbrida. Muy dulce.", "Frambuesa - más grande", "🍓", "Segura"),
        BerryInfo("Wolfberry/Goji", "Lycium barbarum", "Rojas, alargadas. Muy saludables, antioxidantes.", "Lycium - confirmar especie", "🔴", "Segura")
    )

    // ==================== BAYAS TÓXICAS ====================
    val toxicBerries = listOf(
        BerryInfo("Belladona", "Atropa belladonna", "Negras, brillantes, del tamaño de un guisante. MUY TÓXICA. 2-3 bayas pueden ser letales en niños.", "Arándano - sabor amargo, hojas diferentes", "⚫", "MORTAL"),
        BerryInfo("Yezgo", "Sambucus ebulus", "Negras, pequeñas, en ramilletes erectos. Tóxicas. Olor desagradable.", "Mora - ramilletes colgantes", "🫐", "TÓXICA"),
        BerryInfo("Acebo", "Ilex aquifolium", "Rojas, brillantes. Tóxicas en cantidad. Hojas espinosas.", "Grosella - hojas lisas", "🔴", "TÓXICA"),
        BerryInfo("Acebo americano", "Ilex opaca", "Rojas, similares al acebo europeo. Tóxico.", "Grosella", "🔴", "TÓXICA"),
        BerryInfo("Dulcamara", "Solanum dulcamara", "Rojas o negras, alargadas. Tóxicas. Enredadera.", "Grosella - forma diferente", "🔴", "TÓXICA"),
        BerryInfo("Estramonio", "Datura stramonium", "Verdes, luego amarillas. MUY TÓXICA. Espinas en fruto.", "Tomate silvestre - olor fétido", "🟢", "MORTAL"),
        BerryInfo("Fresa de töis", "Daphne mezereum", "Rojas, brillantes, muy olorosas. MUY TÓXICA. Huele muy fuerte.", "Fresa - alargadas, olor", "🍓", "MORTAL"),
        BerryInfo("Torvisco", "Daphne gnidium", "Blancas, pequeñas. MUY TÓXICA. Similar a laurelia.", " 其他 bayas blancas - confundir", "⚪", "MORTAL"),
        BerryInfo("Lauréola", "Daphne laureola", "Negras, pequeñas. MUY TÓXICA. Crece en bosques.", " Otros Daphne", "⚫", "MORTAL"),
        BerryInfo("Tejo", "Taxus baccata", "Rojas con semilla negra dentro. TODA LA PLANTA ES MORTAL. Semilla muy tóxica.", "Ninguna - reconocible por hojas", "🔴", "MORTAL"),
        BerryInfo("Cornezuelo", "Claviceps purpurea", "Negras, duras, en espigas de centeno. MICOtoxinas. Hongo.", "Grano de centeno - más oscuro", "⬛", "MORTAL"),
        BerryInfo("Muérdago", "Viscum album", "Blancas, viscosas. Tóxico. Crece en ramas.", "Acebo - diferente textura", "⚪", "TÓXICA"),
        BerryInfo("Visca", "Viscum album subsp. album", "Blancas, viscosas. Similar al muérdago. Tóxica.", "Similar al muérdago", "⚪", "TÓXICA"),
        BerryInfo("Adelfa", "Nerium oleander", "Blancas, rojas o rosadas. TODA LA PLANTA ES TÓXICA. Hinchazón.", " Oleander - todas partes tóxicas", "⚪", "MORTAL"),
        BerryInfo("Evónimo", "Euonymus europaeus", "Rojas, rosadas, brillantes. Tóxico. Cápsulas rosas.", "Arilo rojo - confusión", "🔴", "TÓXICA"),
        BerryInfo("Aligustre", "Ligustrum vulgare", "Negras, pequeñas. Tóxico. Flores blancas olorosas.", "Saúco - olor diferente", "⚫", "TÓXICA"),
        BerryInfo("Saúco herbáceo", "Sambucus ebulus", "Igual al yezgo. Tóxico. Herbáceo, no arbusto.", "Saúco negro - cultivado", "🫐", "TÓXICA"),
        BerryInfo("Euónimo alado", "Euonymus alatus", "Rojas, en cápsulas aladas. Tóxico.", "Evónimo europeo", "🔴", "TÓXICA"),
        BerryInfo("Kalmia", "Kalmia latifolia", "Rojas, pequeñas. Tóxica. Arbusto ornamental.", "Arándano - hojas diferentes", "🔴", "TÓXICA"),
        BerryInfo("Pieris", "Pieris japonica", "Rojas o blancas. Tóxica. Arbusto ornamental.", "Arándano - hojas diferentes", "🔴", "TÓXICA"),
        BerryInfo("Majuelo/Spindle", "Euonymus europaeus", "Naranjas, brillantes. Tóxico. Cápsulas rosas.", " 其他 con arilo naranja", "🟠", "TÓXICA"),
        BerryInfo("Solano negro", "Solanum nigrum", "Negras, pequeñas. Tóxico. Hierba, no arbusto.", "Arándano - hábito diferente", "⚫", "TÓXICA"),
        BerryInfo("Physalis", "Physalis alkekengi", "Naranjas, dentro de farolillo. Tóxicas crudas.", "Similar a tomate cherry", "🟠", "TÓXICA (crudo)"),
        BerryInfo("Arum", "Arum maculatum", "Rojas, brillantes. Tóxico. Fruto de jaras.", " otros - confundible", "🔴", "TÓXICA"),
        BerryInfo("Cicuta", "Conium maculatum", "Verdes, pequeñas. MUY TÓXICA. Hierba alta.", "其他的浆果 - 很危险", "🟢", "MORTAL"),
        BerryInfo("Hiedra", "Hedera helix", "Negras, pequeñas. Tóxicas. Enredadera.", " otros - 属性", "⚫", "TÓXICA"),
        BerryInfo("Mereia/Smooth_sumac", "Rhus glabra", "Rojas, pubescentes. Similar al zumaque. Moderadamente tóxica.", "Zumaque - 属性", "🔴", "Irritante"),
        BerryInfo("Madreselva japonesa", "Lonicera japonica", "Negras, pequeñas. Tóxica. Enredadera.", "Madreselva europea - 属性", "⚫", "TÓXICA"),
        BerryInfo("Viburno", "Viburnum opulus", "Rojas, pequeñas. Tóxico. Ramilletes planos.", "arándano - 属性", "🔴", "TÓXICA")
    )

    // ==================== CONFUSIONES ====================
    val confusions = listOf(
        ConfusionItem("Mora", "🫐", "✅ COMESTIBLE", "Yezgo", "⚫", "☠️ TÓXICA",
            "Las moras cuelgan hacia abajo en racimos; el yezgo tiene racimos erectos. El yezgo huele mal.",
            "El yezgo causa vómitos, diarrea y puede ser peligroso."),
        ConfusionItem("Fresa silvestre", "🍓", "✅ COMESTIBLE", "Fresa de töis", "🍓", "☠️ MORTAL",
            "La fresa de töis es más alargada, huele muy fuerte (nauseabundo), hojas lanceoladas no dentadas.",
            "La fresa de töis puede ser mortal. Contiene mezereína."),
        ConfusionItem("Arándano", "🫐", "✅ COMESTIBLE", "Belladona", "⚫", "☠️ MORTAL",
            "Los arándanos son mate y saben dulce; la belladona es brillante, grande y sabe amargo.",
            "La belladona puede causar la muerte. Contiene atropina."),
        ConfusionItem("Grosella", "🔴", "✅ COMESTIBLE", "Acebo", "🔴", "☠️ TÓXICA",
            "El acebo tiene hojas espinosas brillantes y crece en arbustos altos; la grosella tiene hojas blandas.",
            "El acebo en cantidad causa problemas gastrointestinales."),
        ConfusionItem("Grosella roja", "🔴", "✅ COMESTIBLE", "Dulcamara", "🔴", "☠️ TÓXICA",
            "La grosella es redonda y translúcida; la dulcamara es alargada y crecen en enredaderas.",
            "La dulcamara contiene alcaloides tóxicos."),
        ConfusionItem("Saúco negro", "🫐", "✅ COCIDO", "Yezgo", "🫐", "☠️ TÓXICA",
            "El saúco negro tiene ramilletes grandes y colgantes; el yezgo tiene ramilletes erectos y huele mal.",
            "Ambos confusos: siempre cocinar el saúco, nunca comer yezgo."),
        ConfusionItem("Arándano rojo", "🔴", "✅ COMESTIBLE", "Gayuba", "🔴", "⚠️ MOLESTO",
            "Muy similares. La gayuba tiene hojas más brillantes y sabor harinoso.",
            "La gayuba causa malestar gastrointestinal leve."),
        ConfusionItem("Frambuesa", "🍓", "✅ COMESTIBLE", "Solano negro", "⚫", "☠️ TÓXICA",
            "La frambuesa es hueca por dentro y crece en zarzas; el solano negro es sólida y crece en hierbas.",
            "El solano negro contiene solanina."),
        ConfusionItem("Serbal", "🔴", "✅ COCIDO", "Acebo", "🔴", "☠️ TÓXICA",
            "El serbal tiene hojas pinnadas; el acebo tiene hojas espinosas brillantes.",
            "El acebo causa problemas digestivos."),
        ConfusionItem("Cornus mas", "🔴", "✅ COMESTIBLE", "Majuelo", "🟠", "⚠️ IRRITANTE",
            "El cornus tiene hueso grande y pulpa amarilla; el majuelo tiene hueso pequeño y pulpa roja.",
            "El majuelo puede causar irritación."),
        ConfusionItem("Arándano americano", "🫐", "✅ COMESTIBLE", "Mirtilo europeo", "🫐", "✅ COMESTIBLE",
            "El americano es más grande y con cicatriz más clara; el europeo tiene cicatriz oscura.",
            "Ambos comestibles - solo identificar para cultivar."),
        ConfusionItem("Liquen reno", "⚪", "✅ EMERGENCIA", "Otros líquenes", "⚪", "☠️ TÓXICOS",
            "El liquen de reno tiene ramitas horizontales; otros líquenes son diferentes.",
            "Muchos líquenes son tóxicos - solo consumir si estás seguro."),
        ConfusionItem("Skimmia", "🔴", "⚠️ MODERACIÓN", "Acebo", "🔴", "☠️ TÓXICA",
            "La skimmia tiene hojas aromáticas; el acebo tiene espinas.",
            "La skimmia contiene compuestos irritantes."),
        ConfusionItem("Espino amarillo", "🟠", "✅ COMESTIBLE", "Serbal", "🔴", "✅ COCIDO",
            "El espino amarillo tiene hojas plateadas; el serbal tiene hojas pinnadas.",
            "Ambos comestibles pero diferentes."),
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
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = colors.onPrimary)
                        }
                        Spacer(Modifier.width(4.dp))
                        Text("🫐 Guía de Bayas", color = colors.onPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    // Buscador
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                        placeholder = { Text("Buscar baya...", fontSize = 14.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.onPrimary,
                            unfocusedBorderColor = colors.onPrimary.copy(alpha = 0.5f),
                            focusedTextColor = colors.onPrimary,
                            unfocusedTextColor = colors.onPrimary,
                            focusedContainerColor = colors.primaryContainer.copy(alpha = 0.3f),
                            unfocusedContainerColor = colors.primaryContainer.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Advertencia
            Surface(modifier = Modifier.fillMaxWidth(), color = colors.error.copy(alpha = 0.15f)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("⚠️", fontSize = 24.sp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("¡ATENCIÓN!", fontWeight = FontWeight.Bold, color = colors.error)
                        Text("Nunca comas bayas si no estás 100% seguro. Cuando dudes, NO COMAS.", fontSize = 12.sp, color = colors.error)
                    }
                }
            }

            // Tabs
            TabRow(selectedTabIndex = selectedTab, containerColor = colors.surface) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("🍓 Comestibles (${filteredEdible.size})", fontSize = 12.sp) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("☠️ Tóxicas (${filteredToxic.size})", fontSize = 12.sp) })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("⚠️ Confusiones", fontSize = 12.sp) })
            }

            // Contenido
            when (selectedTab) {
                0 -> {
                    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(filteredEdible) { berry -> BerryCard(berry = berry, isEdible = true) }
                        if (filteredEdible.isEmpty()) {
                            item { Text("No hay resultados", color = colors.onSurfaceVariant, modifier = Modifier.padding(16.dp)) }
                        }
                    }
                }
                1 -> {
                    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(filteredToxic) { berry -> BerryCard(berry = berry, isEdible = false) }
                        if (filteredToxic.isEmpty()) {
                            item { Text("No hay resultados", color = colors.onSurfaceVariant, modifier = Modifier.padding(16.dp)) }
                        }
                    }
                }
                2 -> {
                    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(confusions) { confusion -> ConfusionCard(confusion = confusion) }
                    }
                }
            }
        }
    }
}

data class BerryInfo(val name: String, val scientificName: String, val description: String, val lookAlike: String, val imageEmoji: String, val toxicity: String)

data class ConfusionItem(
    val name1: String, val emoji1: String, val label1: String,
    val name2: String, val emoji2: String, val label2: String,
    val tip: String, val danger: String
)

@Composable
fun BerryCard(berry: BerryInfo, isEdible: Boolean) {
    val colors = MaterialTheme.colorScheme
    val toxicityColor = if (isEdible) Color(0xFF4CAF50) else colors.error
    val toxicityBg = if (isEdible) Color(0xFF4CAF50).copy(alpha = 0.15f) else Color(0xFFB71C1C).copy(alpha = 0.15f)

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = colors.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(45.dp).background(toxicityBg, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Text(berry.imageEmoji, fontSize = 24.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(berry.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(berry.scientificName, fontStyle = FontStyle.Italic, fontSize = 10.sp, color = colors.onSurfaceVariant)
                Text(berry.description, fontSize = 11.sp, color = colors.onSurface, maxLines = 2)
                if (berry.lookAlike.isNotBlank()) {
                    Text("Confundible con: ${berry.lookAlike}", fontSize = 10.sp, color = Color(0xFFFF9800), maxLines = 1)
                }
            }
            Surface(color = toxicityBg, shape = RoundedCornerShape(4.dp)) {
                Text(berry.toxicity, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = toxicityColor)
            }
        }
    }
}

@Composable
fun ConfusionCard(confusion: ConfusionItem) {
    val colors = MaterialTheme.colorScheme

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = colors.surface), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(confusion.emoji1, fontSize = 32.sp)
                    Text(confusion.name1, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF4CAF50))
                    Surface(color = Color(0xFF4CAF50).copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                        Text(confusion.label1, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
                Text("vs", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.onSurfaceVariant)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(confusion.emoji2, fontSize = 32.sp)
                    Text(confusion.name2, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = colors.error)
                    Surface(color = colors.error.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                        Text(confusion.label2, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colors.error, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Surface(color = Color(0xFFFFF3E0), shape = RoundedCornerShape(6.dp)) {
                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.Top) {
                    Text("💡", fontSize = 14.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(confusion.tip, fontSize = 11.sp, color = Color(0xFF5D4037))
                }
            }
            Spacer(Modifier.height(6.dp))
            Surface(color = colors.error.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                Row(modifier = Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("⚠️", fontSize = 12.sp)
                    Spacer(Modifier.width(4.dp))
                    Text(confusion.danger, fontSize = 10.sp, color = colors.error, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}