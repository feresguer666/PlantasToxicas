package com.toxicplants.database.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class PhytoTestInfo(
    val id: String,
    val emoji: String,
    val title: String,
    val subtitle: String,
    val targetToxins: String,
    val materials: String,
    val procedure: List<String>,
    val positiveResult: String,
    val negativeResult: String,
    val scientificReason: String,
    val safetyWarning: String,
    val headerColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePhytoTestsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val tests = remember {
        listOf(
            PhytoTestInfo(
                id = "uv_fluorescence",
                emoji = "🔦",
                title = "1. Test de Fluorescencia bajo Luz UV (365-395 nm)",
                subtitle = "Linterna de billetes o resina epoxi en cuarto oscuro",
                targetToxins = "Cumarinas, Furanocumarinas, Alcaloides de Quina/Berberina y Porfirinas",
                materials = "• Linterna ultravioleta UV-A (365 nm o 395 nm, como las de verificar billetes o secar uñas/resina).\n• Servilleta blanca limpia (que no sea fluorescente por blanqueadores ópticos) o plato de cristal.\n• Habitación oscura o monte de noche.\n• Guantes de protección y cuchillo/tijeras.",
                procedure = listOf(
                    "Colócate en una habitación completamente oscura con las luces apagadas.",
                    "Lleva la muestra vegetal (hoja, tallo grueso, corteza de raíz o bayas) sobre el plato de cristal o servilleta.",
                    "Con el cuchillo, realiza un corte fresco en el tallo o machaca ligeramente la muestra para que brote jugo o savia.",
                    "Enciende la linterna UV a unos 10-15 cm de distancia apuntando directamente al corte fresco y a la savia."
                ),
                positiveResult = "• 🔵 AZUL CELESTE BRILLANTE: Alta presencia de Cumarinas y Furanocumarinas (ej. Heracleum / Perejil gigante, Ruda, Higuera, Cítricos silvestres). ALERTA: Indican altísimo peligro de FITOFOTODERMATITIS (quemaduras graves en la piel si te da el sol tras tocar la savia).\n• 🟢 VERDE / AMARILLO LIMÓN RESPLANDECIENTE: Alcaloides de Quina y Berberina (ej. Celidonia mayor / Chelidonium majus, Agracejo). El látex amarillo naranja de celidonia brilla intensamente en verde bajo luz UV.\n• 🔴 ROJO RUBÍ / CARMÍN INTENSO: Porfirinas y Clorofila libre (al triturar hojas verdes con una gota de alcohol o acetona, el líquido emite un color carmín espectacular bajo UV).",
                negativeResult = "• 🟤 OSCURO O SIN BRILLO: Si el corte absorbe la luz y se ve morado oscuro o negro opaco, no emite fluorescencia detectable en ese rango.",
                scientificReason = "Las estructuras químicas con anillos aromáticos conjugados y dobles enlaces alternados (como las cumarinas y alcaloides isoquinolínicos) absorben fotones de radiación UV de onda corta y reemiten energía instantáneamente en forma de luz visible de mayor longitud de onda.",
                safetyWarning = "⚠️ ADVERTENCIA UV: NUNCA mires directamente a la bombilla LED ultravioleta ni apuntes a los ojos de personas o animales. Las furanocumarinas que brillen en azul causan ampollas severas al contacto con la piel y luz solar posterior.",
                headerColor = Color(0xFF4A148C)
            ),
            PhytoTestInfo(
                id = "foam_saponins",
                emoji = "🧼",
                title = "2. Test de Espuma Persistente (Saponinas)",
                subtitle = "Agitación en tubo o frasco de cristal con agua tibia",
                targetToxins = "Saponinas hemolíticas y triterpénicas (Irritantes y tóxicas para sangre/peces)",
                materials = "• Frasco pequeño de cristal con tapa de rosca o tubo de ensayo con tapón.\n• 10 ml de agua tibia (preferiblemente agua destilada o sin gas ni detergentes).\n• Muestra vegetal machacada (hojas de Hiedra / Hedera helix, bayas de Acebo, Jabonera / Saponaria, Solanum nigrum, Phytolacca).",
                procedure = listOf(
                    "Tritura o machaca fina una pequeña cantidad de hojas, bayas o raíz con un mortero o cuchara.",
                    "Introduce la pasta vegetal en el frasco de cristal y añade unos 10 ml de agua tibia.",
                    "Cierra el frasco herméticamente con la tapa.",
                    "Agita el frasco vigorosamente en vertical durante exactamente 30 segundos sin parar.",
                    "Apoya el frasco en una mesa plana, pon un cronómetro y observa la evolución de la espuma durante 10 minutos."
                ),
                positiveResult = "• 🛑 POSITIVO FUERTE (Espuma estable > 10 minutos): Si se forma una capa de espuma densa tipo \"espuma de cerveza o jabón\" de 1 a 3 cm de altura que se mantiene intacta tras 10 minutos de reposo, confirma una altísima concentración de SAPONINAS TÓXICAS / HEMOLÍTICAS.\n• ⚠️ POSITIVO LEVE: Espuma de menos de 1 cm que dura entre 2 y 5 minutos.",
                negativeResult = "• ⚪ NEGATIVO: Si las burbujas grandes desaparecen en menos de 1 minuto dejando la superficie líquida plana, no hay presencia significativa de saponinas.",
                scientificReason = "Las saponinas poseen una estructura anfipática con una aglicona hidrófoba (sapogenina) y cadenas de azúcares hidrófilas. Esto les confiere una potente actividad tensioactiva que reduce la tensión superficial del agua, atrapando aire en una espuma rígida e induciendo la lisis de glóbulos rojos (hemólisis).",
                safetyWarning = "⚠️ ADVERTENCIA: Las saponinas concentradas son tóxicas por ingestión (producen vómitos y diarreas) y letales si entran en el torrente sanguíneo o en acuarios con peces/anfibios (destruyen sus branquias en minutos).",
                headerColor = Color(0xFF006064)
            ),
            PhytoTestInfo(
                id = "acid_base_flavonoids",
                emoji = "🧪",
                title = "3. Test Ácido-Base Casero (Vinagre vs. Bicarbonato)",
                subtitle = "Cambio cromático de Flavonoides y Antocianinas",
                targetToxins = "Flavonoides, Flavonas, Chalconas y Antocianinas fenólicas",
                materials = "• 2 vasos transparentes de cristal pequeños (Vaso A y Vaso B).\n• Vinagre blanco de vino o zumo de limón colado (Ácido casero).\n• Bicarbonato de sodio disuelto en unas gotas de agua o amoniaco casero (Base/Alcalino).\n• Infusión tibia o extracto acuoso de bayas/flores sospechosas (ej. bayas negras de Solanum, flores de Acónito, Belladona, raíces machacadas).",
                procedure = listOf(
                    "Machaca la muestra de bayas, hojas o flores con 2 cucharadas de agua caliente para extraer los pigmentos y filtra el líquido claro.",
                    "Reparte el líquido filtrado a partes iguales entre el Vaso A y el Vaso B.",
                    "En el Vaso A (Medio Ácido): Añade 10 gotas de Vinagre blanco o zumo de limón y remueve suavemente.",
                    "En el Vaso B (Medio Alcalino): Añade media cucharadita de solución de Bicarbonato de sodio o 3 gotas de amoniaco y remueve.",
                    "Compara el cambio de color entre ambos vasos sobre un fondo blanco."
                ),
                positiveResult = "• 🔴 EN VASO A (ÁCIDO): Si la solución vira intensamente hacia ROJO VIVO, ROJO CEREZA o ROSA FUCSIA, confirma el catión flavilio típico de Antocianinas libres.\n• 🟢/🔵 EN VASO B (BASE): Si el color cambia bruscamente hacia VERDE OSCURO, AZUL VERDOSO o MARRÓN PARDO opaco, confirma antocianinas fenólicas en medio alcalino.\n• 🟡 AMARILLO INTENSO EN BASE: Si el Vaso B se vuelve amarillo canario brillante tras el bicarbonato/amoniaco, indica presencia de Flavonas, Flavonoles o Chalconas ocultas.",
                negativeResult = "• ⚪ SIN CAMBIO: Si ambos vasos mantienen exactamente el mismo tono o turbidez que el extracto original, la coloración no se debe a flavonoides ni antocianinas sensibles al pH.",
                scientificReason = "Los compuestos fenólicos y flavonoides poseen equilibrios tautoméricos dependientes de los iones H+ y OH-. Al protonarse en medio ácido o desprotonarse en medio básico, alteran su sistema de dobles enlaces conjugados, cambiando la absorción de luz en el espectro visible.",
                safetyWarning = "⚠️ ADVERTENCIA: Si utilizas amoniaco casero como base en lugar de bicarbonato, hazlo en un área bien ventilada y no mezcles jamás amoniaco con lejía ni ácidos fuertes.",
                headerColor = Color(0xFFC62828)
            ),
            PhytoTestInfo(
                id = "tannin_iron",
                emoji = "🧲",
                title = "4. Test del Tanino con Hierro (Tinta Ferrogálica)",
                subtitle = "Solfato de hierro o chatarra oxidada con vinagre",
                targetToxins = "Taninos condensados e hidrolizables (Polifenoles astringentes irritantes)",
                materials = "• 1 vaso transparente de cristal con 20 ml de infusión concentrada de la planta (hojas de Roble, Castaño, Helecho águila, corteza de sumaque/Rhus o zumaque).\n• Reactivo de Hierro casero: Disuelve una pizca de abono de sulfato de hierro (sulfato ferroso) para plantas en 10 ml de agua, O bien deja un clavo de hierro oxidado en agua con unas gotas de vinagre durante 24 horas para generar acetato/sulfato ferroso.",
                procedure = listOf(
                    "Prepara una infusión concentrada hirviendo la muestra vegetal en un cazo con agua durante 5 minutos y déjala enfriar.",
                    "Filtra el líquido en un vaso transparente para que no queden trozos sólidos.",
                    "Con un cuentagotas o cucharita, deja caer de 5 a 10 gotas del reactivo de hierro sobre el centro del vaso con la infusión vegetal.",
                    "Observa la reacción instantánea al entrar en contacto las gotas de hierro con el extracto."
                ),
                positiveResult = "• 🛑 POSITIVO INMEDIATO (Tinta Negra / Azul Oscura): Si al caer las gotas se forma instantáneamente una nube o precipitado AZUL OSCURO, NEGRO AZULADO o VERDE NEGRUZCO opaco (como tinta china tradicional), confirma altísima concentración de TANINOS HIDROLIZABLES O CONDENSADOS.\n• 🟤 POSITIVO VERDE PARDO: Si vira a verde parduzco oscuro, indica taninos catecónicos o condensados.",
                negativeResult = "• 🟡 NEGATIVO: Si la solución simplemente se diluye ligeramente o adquiere un tono amarillento claro sin precipitado oscuro ni turbidez negra, carece de taninos significativos.",
                scientificReason = "Los grupos hidroxilo fenólicos de los taninos tienen una extraordinaria afinidad quelante por los cationes de hierro trivalente (Fe3+) o bivalente oxidado (Fe2+), formando complejos de coordinación ferrogálicos insolubles que absorben toda la luz visible, generando el color negro intenso de las tintas históricas.",
                safetyWarning = "⚠️ ADVERTENCIA DE TOXICIDAD: Los taninos en altas concentraciones son antinutrientes potentes e irritantes gástricos severos que precipitan proteínas en la mucosa estomacal, produciendo dolor de estómago, estreñimiento severo y daño hepático crónico en el ganado y humanos.",
                headerColor = Color(0xFF33691E)
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("🧪 Pruebas Fitoquímicas Caseras", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Experimentos seguros didácticos y Luz UV", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF006064),
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF071317))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF004D40)),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🔬", fontSize = 32.sp)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("El Laboratorio Botánico en Casa", fontWeight = FontWeight.Black, color = Color.White, fontSize = 18.sp)
                                Text("Aprende a identificar grupos tóxicos con elementos cotidianos", fontSize = 12.sp, color = Color(0xFF80CBC4))
                            }
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 2.dp))
                        Text(
                            "¿No dispones de reactivos de laboratorio como Dragendorff o Mayer? Estas 4 pruebas clásicas te permitirán observar fenómenos físico-químicos espectaculares como la fluorescencia UV, saponificación, quelación férrica e indicadores de pH en plantas silvestres.",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            item {
                Surface(
                    color = Color(0xFFB71C1C).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFE53935)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF8A80), modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("PROTOCOLO DE SEGURIDAD OBLIGATORIO", fontWeight = FontWeight.Bold, color = Color(0xFFFF8A80), fontSize = 12.sp)
                            Text(
                                "• NUNCA pruebes con la lengua ni ingieras ninguna muestra.\n• Usa siempre guantes de protección (nitrilo/látex) y gafas de seguridad al machacar bayas o tallos para evitar salpicaduras oculares.\n• Utiliza recipientes desechables o lávalos muy bien con jabón al terminar.\n• Al terminar las pruebas, lávate bien las manos con agua abundante y jabón.",
                                color = Color.White,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }

            items(tests, key = { it.id }) { test ->
                PhytoTestDetailCard(test)
            }

            item {
                Surface(
                    color = Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Text(
                        "💡 CONSEJO DIDÁCTICO: Estos experimentos son ideales para talleres de botánica, clases de ciencias en institutos o para curiosos de la naturaleza. Puedes documentar tus resultados haciendo fotos con el escáner de la app y adjuntando una nota en tu catálogo.",
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        modifier = Modifier.padding(12.dp),
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PhytoTestDetailCard(info: PhytoTestInfo) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF112226)),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (expanded) info.headerColor else Color.White.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(info.emoji, fontSize = 32.sp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(info.title, fontWeight = FontWeight.Black, color = Color.White, fontSize = 16.sp)
                        Spacer(Modifier.height(2.dp))
                        Text(info.subtitle, fontSize = 12.sp, color = Color(0xFF80CBC4), fontWeight = FontWeight.SemiBold)
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = info.headerColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            Surface(
                color = info.headerColor.copy(alpha = 0.35f),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, info.headerColor)
            ) {
                Row(Modifier.padding(10.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Science, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("COMPUESTOS DETECTADOS:", fontSize = 10.sp, color = Color(0xFFE0F2F1), fontWeight = FontWeight.Bold)
                        Text(info.targetToxins, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

                    Column {
                        Text("🧰 MATERIALES NECESARIOS EN CASA:", fontWeight = FontWeight.Bold, color = Color(0xFFFFB74D), fontSize = 13.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(info.materials, color = Color.White, fontSize = 13.sp, lineHeight = 18.sp)
                    }

                    Column {
                        Text("📋 PROCEDIMIENTO PASO A PASO:", fontWeight = FontWeight.Bold, color = Color(0xFF64B5F6), fontSize = 13.sp)
                        Spacer(Modifier.height(4.dp))
                        info.procedure.forEachIndexed { i, step ->
                            Row(Modifier.padding(vertical = 3.dp)) {
                                Text("${i + 1}.", fontWeight = FontWeight.Bold, color = Color(0xFF64B5F6), fontSize = 13.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(step, color = Color.White, fontSize = 13.sp, lineHeight = 17.sp)
                            }
                        }
                    }

                    Surface(
                        color = Color(0xFF1B5E20).copy(alpha = 0.35f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CAF50)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("✅ INTERPRETACIÓN RESULTADO POSITIVO:", fontWeight = FontWeight.Bold, color = Color(0xFFA5D6A7), fontSize = 12.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(info.positiveResult, color = Color.White, fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    Surface(
                        color = Color(0xFF37474F).copy(alpha = 0.4f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF78909C)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("⚪ RESULTADO NEGATIVO O AUSENCIA:", fontWeight = FontWeight.Bold, color = Color(0xFFB0BEC5), fontSize = 12.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(info.negativeResult, color = Color.White, fontSize = 13.sp, lineHeight = 17.sp)
                        }
                    }

                    Column {
                        Text("🔬 EXPLICACIÓN CIENTÍFICA DEL FENÓMENO:", fontWeight = FontWeight.Bold, color = Color(0xFFBA68C8), fontSize = 13.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(info.scientificReason, color = Color.LightGray, fontSize = 12.sp, lineHeight = 16.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    }

                    Surface(
                        color = Color(0xFFB71C1C).copy(alpha = 0.25f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE53935)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF8A80), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(info.safetyWarning, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, lineHeight = 15.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val shareText = "🧪 PRUEBA FITOQUÍMICA CASERA FLORASAFE\n" +
                                    "${info.title}\n" +
                                    "${info.subtitle}\n\n" +
                                    "🔬 Compuestos: ${info.targetToxins}\n\n" +
                                    "🧰 Materiales:\n${info.materials}\n\n" +
                                    "📋 Resultado Positivo:\n${info.positiveResult}\n\n" +
                                    "🔬 Explicación Científica:\n${info.scientificReason}"
                            val intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(intent, "Compartir prueba fitoquímica"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF80CBC4))
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Compartir guía de experimento", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
