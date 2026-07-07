package com.toxicplants.database.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class ToxicSyndromeInfo(
    val name: String,
    val icon: String,
    val source: String,
    val latency: String,
    val keySymptoms: String,
    val causes: String,
    val mechanism: String,
    val firstAid: String,
    val severity: String,
    val color: Color,
)

private val toxicSyndromes = listOf(
    ToxicSyndromeInfo(
        name = "Síndrome Faloidiano / Amatoxínico",
        icon = "🍄",
        source = "Setas",
        latency = "6-24 h; a veces hasta 36 h",
        keySymptoms = "Diarrea acuosa intensa, vómitos, dolor abdominal, falsa mejoría, hepatitis tóxica, coagulopatía y posible fallo hepático.",
        causes = "Amanita phalloides, A. verna, A. virosa, Lepiota pequeñas, Galerina marginata y otras especies con amatoxinas.",
        mechanism = "Las amatoxinas inhiben la ARN polimerasa II; dañan sobre todo hígado e intestino.",
        firstAid = "Urgencia vital aunque la persona esté sin síntomas. Conservar restos, no esperar a la latencia y contactar con emergencias/toxicología.",
        severity = "Mortal",
        color = Color(0xFFC62828)
    ),
    ToxicSyndromeInfo(
        name = "Síndrome Atropínico / Anticolinérgico",
        icon = "🌿",
        source = "Plantas",
        latency = "30 min-4 h",
        keySymptoms = "Boca seca, piel caliente y seca, pupilas dilatadas, visión borrosa, taquicardia, retención urinaria, fiebre, agitación, delirios y alucinaciones.",
        causes = "Datura stramonium, Brugmansia, Atropa belladonna, Hyoscyamus, Mandragora y otras solanáceas con alcaloides tropánicos.",
        mechanism = "Bloqueo muscarínico por atropina, escopolamina e hiosciamina.",
        firstAid = "No inducir vómito. Mantener vigilancia, evitar sobrecalentamiento y pedir ayuda médica urgente si hay confusión, fiebre, taquicardia o ingestión infantil.",
        severity = "Alta",
        color = Color(0xFF6A1B9A)
    ),
    ToxicSyndromeInfo(
        name = "Síndrome Muscarínico / Colinérgico",
        icon = "💧",
        source = "Setas y plantas",
        latency = "15 min-2 h",
        keySymptoms = "Sudoración, salivación, lagrimeo, miosis, vómitos, diarrea, cólicos, broncorrea, bradicardia e hipotensión.",
        causes = "Setas Inocybe, Inosperma, Clitocybe dealbata/rivulosa; plantas o extractos con acción colinérgica también pueden simularlo.",
        mechanism = "Exceso de estimulación muscarínica por muscarina u otros agonistas colinérgicos.",
        firstAid = "Consultar urgentemente si hay dificultad respiratoria, pulso lento, debilidad marcada o niños afectados. El antídoto médico clásico es atropina bajo control sanitario.",
        severity = "Alta",
        color = Color(0xFF0277BD)
    ),
    ToxicSyndromeInfo(
        name = "Síndrome Orellánico / Nefrotóxico tardío",
        icon = "🫘",
        source = "Setas",
        latency = "2-17 días",
        keySymptoms = "Sed intensa, boca seca, náuseas, dolor lumbar, cefalea, poca orina y fallo renal tardío.",
        causes = "Cortinarius orellanus, C. rubellus, C. splendens, C. orellanoides y afines.",
        mechanism = "Orellanina y nefrotoxinas relacionadas lesionan túbulos renales.",
        firstAid = "Toda sospecha exige valoración médica aunque hayan pasado días desde la comida. Conservar datos y restos de setas.",
        severity = "Mortal",
        color = Color(0xFF8D6E63)
    ),
    ToxicSyndromeInfo(
        name = "Síndrome Giromitrínico",
        icon = "🧠",
        source = "Setas",
        latency = "6-12 h; variable",
        keySymptoms = "Vómitos, diarrea, cefalea, mareo; en casos graves convulsiones, hemólisis, hepatitis tóxica, coma y fallo renal.",
        causes = "Gyromitra esculenta, G. infula, G. gigas, Helvella y falsas colmenillas mal identificadas.",
        mechanism = "Giromitrina metabolizada a monometilhidrazina, con toxicidad hepática y neurológica.",
        firstAid = "No confiar en hervidos caseros. Contactar con toxicología; las convulsiones o somnolencia requieren urgencias.",
        severity = "Alta",
        color = Color(0xFFEF6C00)
    ),
    ToxicSyndromeInfo(
        name = "Síndrome Iboténico-Muscimol / Panterínico",
        icon = "🍄",
        source = "Setas",
        latency = "30 min-3 h",
        keySymptoms = "Somnolencia o agitación, ataxia, confusión, delirios, alucinaciones, náuseas y vómitos.",
        causes = "Amanita muscaria, A. pantherina, A. regalis, A. gemmata y afines.",
        mechanism = "Ácido iboténico y muscimol alteran neurotransmisión glutamatérgica/GABAérgica.",
        firstAid = "Acompañar en lugar seguro, evitar alcohol y consultar si hay coma, convulsiones, agitación intensa o exposición infantil.",
        severity = "Moderada-Alta",
        color = Color(0xFFD84315)
    ),
    ToxicSyndromeInfo(
        name = "Síndrome Coprínico / Reacción con alcohol",
        icon = "🍷",
        source = "Setas",
        latency = "30 min-2 h tras alcohol; puede ocurrir hasta 72 h después de la seta",
        keySymptoms = "Rubor facial, palpitaciones, taquicardia, hipotensión, náuseas, vómitos, sudoración y cefalea.",
        causes = "Coprinopsis atramentaria, C. romagnesiana y algunas especies con efecto antabús-like.",
        mechanism = "Inhibición del metabolismo del alcohol con acumulación de acetaldehído.",
        firstAid = "Evitar alcohol varios días. Urgencias si hay dolor torácico, síncope, hipotensión o antecedentes cardiacos.",
        severity = "Moderada",
        color = Color(0xFF5D4037)
    ),
    ToxicSyndromeInfo(
        name = "Síndrome Gastrointestinal por setas irritantes",
        icon = "🤢",
        source = "Setas",
        latency = "30 min-6 h",
        keySymptoms = "Náuseas, vómitos, diarrea, dolor abdominal, cólicos y deshidratación.",
        causes = "Omphalotus, Entoloma sinuatum, Chlorophyllum molybdites, Agaricus xanthodermus, Russula/Lactarius acres, boletes tóxicos y otras.",
        mechanism = "Irritantes gastrointestinales diversos; algunos no están completamente caracterizados.",
        firstAid = "Hidratación si está consciente; consultar si síntomas intensos, sangre, fiebre, niños, mayores o dudas con setas mortales de latencia larga.",
        severity = "Variable",
        color = Color(0xFFAD1457)
    ),
    ToxicSyndromeInfo(
        name = "Síndrome Digitálico / Glucósidos cardiacos",
        icon = "❤️",
        source = "Plantas",
        latency = "1-6 h; variable",
        keySymptoms = "Náuseas, vómitos, dolor abdominal, visión amarilla o borrosa, bradicardia, arritmias, confusión y shock.",
        causes = "Digitalis, Nerium oleander, Thevetia, Convallaria, Adonis y otras con cardenólidos/bufadienólidos.",
        mechanism = "Inhibición de Na+/K+-ATPasa con alteraciones de conducción cardiaca y potasio.",
        firstAid = "Urgencia médica por riesgo de arritmias. Llevar muestra de la planta; no administrar remedios caseros.",
        severity = "Mortal",
        color = Color(0xFFC2185B)
    ),
    ToxicSyndromeInfo(
        name = "Síndrome Aconitínico / Neurocardiotóxico",
        icon = "⚡",
        source = "Plantas",
        latency = "Minutos-2 h",
        keySymptoms = "Hormigueo en boca y extremidades, náuseas, debilidad, hipotensión, arritmias graves, parálisis y dificultad respiratoria.",
        causes = "Aconitum napellus y otras especies de acónito; también algunas Delphinium en ganado.",
        mechanism = "Alcaloides diterpénicos que mantienen abiertos canales de sodio en nervios y corazón.",
        firstAid = "Emergencia vital. Monitorización cardiaca urgente si hay contacto/ingesta sospechosa.",
        severity = "Mortal",
        color = Color(0xFF283593)
    ),
    ToxicSyndromeInfo(
        name = "Síndrome Cianogénico",
        icon = "🌱",
        source = "Plantas",
        latency = "Minutos-pocas horas",
        keySymptoms = "Ansiedad, cefalea, mareo, vómitos, respiración rápida, convulsiones, colapso y olor almendrado no siempre detectable.",
        causes = "Semillas de Prunus, yuca amarga mal procesada, sorgo, laurel cerezo y plantas con glucósidos cianogénicos.",
        mechanism = "Liberación de cianuro, bloqueo de respiración celular y falta de utilización de oxígeno.",
        firstAid = "Urgencia si hay ingestión significativa o síntomas neurológicos/respiratorios. No retrasar la atención.",
        severity = "Mortal",
        color = Color(0xFF00838F)
    ),
    ToxicSyndromeInfo(
        name = "Síndrome por Oxalato cálcico irritante",
        icon = "🔥",
        source = "Plantas",
        latency = "Inmediata-minutos",
        keySymptoms = "Ardor oral, dolor, salivación, hinchazón de labios/lengua, vómitos; rara vez edema de vía aérea.",
        causes = "Dieffenbachia, Philodendron, Monstera, Zantedeschia, Arum, Colocasia y otras aráceas.",
        mechanism = "Rafidios de oxalato cálcico que lesionan mecánicamente mucosas y liberan irritantes.",
        firstAid = "Enjuagar boca, retirar restos y consultar si hay dificultad para tragar/respirar, edema importante o exposición infantil.",
        severity = "Baja-Moderada",
        color = Color(0xFF558B2F)
    ),
    ToxicSyndromeInfo(
        name = "Síndrome Ricínico / Toxalbúminas",
        icon = "☠️",
        source = "Plantas",
        latency = "Horas-3 días",
        keySymptoms = "Vómitos, diarrea intensa a veces sanguinolenta, dolor abdominal, deshidratación, hipotensión, fallo renal/hepático y shock.",
        causes = "Ricinus communis, Abrus precatorius y semillas con toxalbúminas.",
        mechanism = "Ricina/abrina inhiben síntesis proteica celular.",
        firstAid = "Urgencia por ingestión de semillas masticadas. Conservar semillas/restos para identificación.",
        severity = "Mortal",
        color = Color(0xFFB71C1C)
    ),
    ToxicSyndromeInfo(
        name = "Síndrome Fototóxico / Furanocumarinas",
        icon = "☀️",
        source = "Plantas",
        latency = "Horas-48 h tras luz solar",
        keySymptoms = "Enrojecimiento, quemadura, ampollas, dolor e hiperpigmentación en zonas expuestas al sol.",
        causes = "Heracleum mantegazzianum, Ruta graveolens, higuera, apio silvestre, cítricos y umbelíferas ricas en furanocumarinas.",
        mechanism = "Furanocumarinas activadas por UVA dañan piel.",
        firstAid = "Lavar piel, evitar sol 48-72 h y consultar si hay ampollas extensas, afectación ocular o niños.",
        severity = "Moderada",
        color = Color(0xFFF9A825)
    ),
    ToxicSyndromeInfo(
        name = "Síndrome Hepatotóxico por alcaloides pirrolizidínicos",
        icon = "🧬",
        source = "Plantas",
        latency = "Días-semanas; crónico por uso repetido",
        keySymptoms = "Dolor abdominal, cansancio, ictericia, ascitis, aumento de enzimas hepáticas y enfermedad venooclusiva hepática.",
        causes = "Senecio, Crotalaria, Heliotropium, Echium, Borago y preparados herbales contaminados.",
        mechanism = "Metabolitos reactivos lesionan endotelio sinusoidal hepático.",
        firstAid = "Suspender exposición y consultar; especial cuidado en embarazo, lactancia, niños y enfermedad hepática.",
        severity = "Alta",
        color = Color(0xFF6D4C41)
    ),
    ToxicSyndromeInfo(
        name = "Síndrome Paxillus / Hemólisis inmunológica",
        icon = "🩸",
        source = "Setas",
        latency = "Minutos-horas; a menudo tras consumos repetidos",
        keySymptoms = "Vómitos, diarrea, debilidad, ictericia, orina oscura, anemia hemolítica, shock y fallo renal.",
        causes = "Paxillus involutus, P. rubicundulus y afines.",
        mechanism = "Reacción inmunológica con destrucción de glóbulos rojos.",
        firstAid = "Urgencia si se ha consumido, aunque antes se tolerara. Riesgo por sensibilización.",
        severity = "Mortal",
        color = Color(0xFF880E4F)
    ),
    ToxicSyndromeInfo(
        name = "Ergotismo",
        icon = "🌾",
        source = "Hongos en plantas",
        latency = "Horas-días",
        keySymptoms = "Vasoconstricción, dolor, frialdad, parestesias, convulsiones, alucinaciones, contracciones uterinas y gangrena en casos graves.",
        causes = "Claviceps purpurea en cereales contaminados.",
        mechanism = "Alcaloides del cornezuelo con acción serotoninérgica, dopaminérgica y vasoconstrictora.",
        firstAid = "No consumir grano contaminado. Atención médica si hay síntomas neurológicos, vasculares o embarazo.",
        severity = "Alta-Mortal",
        color = Color(0xFF4E342E)
    ),
    ToxicSyndromeInfo(
        name = "Síndrome por líquenes con ácido vulpínico/usnínico",
        icon = "🪨",
        source = "Líquenes",
        latency = "Horas-días; variable y poco documentado",
        keySymptoms = "Náuseas, vómitos, dolor abdominal, diarrea; algunas exposiciones se asocian a mareo, irritación o daño hepático.",
        causes = "Letharia vulpina, Vulpicida, algunos Usnea y preparados concentrados de líquenes.",
        mechanism = "Ácidos liquénicos irritantes, desacoplantes o hepatotóxicos según especie y dosis.",
        firstAid = "Evitar consumo. Consultar si hay ingesta de niños/mascotas, preparados concentrados o síntomas digestivos/hepáticos.",
        severity = "Variable",
        color = Color(0xFF607D8B)
    ),
    ToxicSyndromeInfo(
        name = "Dermatitis irritativa o alérgica por plantas/líquenes",
        icon = "🖐️",
        source = "Plantas y líquenes",
        latency = "Minutos-días",
        keySymptoms = "Picor, enrojecimiento, urticaria, eccema, ampollas, dolor o quemazón local.",
        causes = "Euphorbia, Toxicodendron, Urtica, Primula, hiedra, látex de plantas y contacto con algunos líquenes sensibilizantes.",
        mechanism = "Irritación química, látex cáustico, pelos urticantes o reacción alérgica de contacto.",
        firstAid = "Lavar con agua y jabón, evitar frotar ojos, retirar ropa contaminada y consultar si hay afectación ocular, ampollas extensas o dificultad respiratoria.",
        severity = "Baja-Moderada",
        color = Color(0xFF2E7D32)
    )
)

@Composable
fun ToxicSyndromesScreen(onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var query by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf("Todos") }
    val sources = listOf("Todos", "Plantas", "Setas", "Líquenes")

    val filtered by remember(query, selectedSource) {
        derivedStateOf {
            val q = query.trim()
            toxicSyndromes.filter { syndrome ->
                val matchesSource = selectedSource == "Todos" || syndrome.source.contains(
                    selectedSource,
                    ignoreCase = true
                )
                val haystack =
                    "${syndrome.name} ${syndrome.source} ${syndrome.keySymptoms} ${syndrome.causes} ${syndrome.mechanism} ${syndrome.severity}"
                val matchesQuery = q.isBlank() || haystack.contains(q, ignoreCase = true)
                matchesSource && matchesQuery
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Surface(modifier = Modifier.fillMaxWidth(), color = colors.error) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = colors.onError
                    )
                }
                Text(
                    text = "📚 Síndromes toxicológicos",
                    color = colors.onError,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar síndrome") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.error,
                    cursorColor = colors.error
                ),
                shape = RoundedCornerShape(14.dp)
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                sources.forEach { source ->
                    AssistChip(
                        onClick = { selectedSource = source },
                        label = {
                            Text(
                                source,
                                fontWeight = if (source == selectedSource) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = { Text(text = sourceIcon(source)) }
                    )
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colors.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Text(
                "${filtered.size} síndromes encontrados",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = colors.onSurfaceVariant
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "",
                    color = colors.error,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
            items(filtered) { syndrome ->
                SyndromeCard(syndrome)
            }
        }
    }
}

@Composable
private fun SyndromeCard(syndrome: ToxicSyndromeInfo) {
    val colors = MaterialTheme.colorScheme
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = syndrome.color.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(syndrome.icon, fontSize = 28.sp, modifier = Modifier.padding(10.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        syndrome.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = colors.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MiniTag(syndrome.source, syndrome.color)
                        MiniTag(syndrome.severity, severityColor(syndrome.severity))
                    }
                }
            }

            HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.55f))

            InfoLine("⏱️", "Latencia", syndrome.latency)
            InfoLine("🩺", "Síntomas clave", syndrome.keySymptoms)
            InfoLine("🧪", "Causas típicas", syndrome.causes)
            InfoLine("⚙️", "Mecanismo", syndrome.mechanism)
            InfoLine("🆘", "Qué hacer", syndrome.firstAid, emphasize = true)
        }
    }
}

@Composable
private fun MiniTag(text: String, color: Color) {
    Surface(color = color.copy(alpha = 0.14f), shape = RoundedCornerShape(50)) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun InfoLine(icon: String, label: String, value: String, emphasize: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(icon, fontSize = 15.sp, modifier = Modifier.width(24.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = colors.onSurface)
            Text(
                value,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = if (emphasize) colors.error else colors.onSurfaceVariant,
                fontStyle = if (emphasize) FontStyle.Normal else FontStyle.Italic
            )
        }
    }
}

private fun sourceIcon(source: String): String = when {
    source.contains("Plantas", ignoreCase = true) && source.contains(
        "Líquenes",
        ignoreCase = true
    ) -> "🌿"

    source.contains("Setas", ignoreCase = true) && source.contains(
        "Plantas",
        ignoreCase = true
    ) -> "☠️"

    source.contains("Plantas", ignoreCase = true) -> "🌿"
    source.contains("Setas", ignoreCase = true) -> "🍄"
    source.contains("Líquenes", ignoreCase = true) -> "🪨"
    else -> "☠️"
}

private fun severityColor(severity: String): Color = when {
    severity.contains("Mortal", ignoreCase = true) -> Color(0xFFC62828)
    severity.contains("Alta", ignoreCase = true) -> Color(0xFFE65100)
    severity.contains("Moderada", ignoreCase = true) -> Color(0xFFF57C00)
    else -> Color(0xFF607D8B)
}
