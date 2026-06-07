package com.toxicplants.database.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class ExtractionMethod(
    val title: String,
    val group: String,
    val filterGroup: String,    // grupo simplificado para filtrar
    val principle: String,
    val solvents: String,
    val equipment: String,      // NUEVO: equipo necesario
    val conditions: String,     // NUEVO: tiempo / temperatura
    val steps: List<String>,
    val pros: String,           // NUEVO: ventajas
    val cons: String,           // NUEVO: limitaciones
    val notes: String,
    val color: Color
)

private val EXTRACTION_GROUPS = listOf(
    "Todos", "Alcaloides", "Glucósidos", "Saponinas", "Flavonoides",
    "Fenoles/Taninos", "Volátiles", "Lípidos", "Generales", "Modernas"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChemicalExtractionMethodsScreen(onBack: () -> Unit) {
    val allMethods = remember { extractionMethods() }
    var query by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf("Todos") }

    val filtered = remember(query, selectedGroup) {
        allMethods.filter { m ->
            (selectedGroup == "Todos" || m.filterGroup == selectedGroup) &&
                    (query.isBlank() ||
                            m.title.contains(query, true) ||
                            m.group.contains(query, true) ||
                            m.principle.contains(query, true))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("⚗️ Métodos de extracción (${allMethods.size})", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Alcaloides · Glucósidos · Saponinas · Flavonoides", fontSize = 12.sp, color = Color.White.copy(alpha = 0.82f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF4A148C),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Buscar método o compuesto…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EXTRACTION_GROUPS.forEach { g ->
                    FilterChip(
                        selected = g == selectedGroup,
                        onClick = { selectedGroup = g },
                        label = { Text(g, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF4A148C),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { SafetyCard() }
                if (filtered.isEmpty()) {
                    item {
                        Text(
                            "Sin métodos para «$query» en $selectedGroup.",
                            color = Color.Gray,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                items(filtered) { method -> ExtractionMethodCard(method) }
            }
        }
    }
}

@Composable
private fun SafetyCard() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
        Column(Modifier.padding(14.dp)) {
            Text("⚠️ Uso educativo y de laboratorio", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
            Text(
                "Estos esquemas son orientativos para fitoquímica. La extracción real requiere campana, EPI, gestión de residuos y supervisión profesional. No usar para consumo ni automedicación.",
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
private fun ExtractionMethodCard(method: ExtractionMethod) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row {
                Text("🧪", fontSize = 24.sp)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(method.title, fontWeight = FontWeight.Bold, color = method.color, fontSize = 16.sp)
                    Text(method.group, color = Color.Gray, fontSize = 12.sp)
                }
                Text(if (expanded) "▲" else "▼", color = method.color, fontSize = 14.sp)
            }
            InfoLine("Principio", method.principle, method.color)
            InfoLine("Disolventes habituales", method.solvents, method.color)

            Text(
                if (expanded) "Ver menos ▲" else "Ver esquema completo (equipo, pasos, ventajas) ▼",
                color = method.color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
            )

            if (expanded) {
                InfoLine("Equipo necesario", method.equipment, method.color)
                InfoLine("Condiciones (tiempo / temperatura)", method.conditions, method.color)
                Text("Esquema", fontWeight = FontWeight.Bold, color = method.color, fontSize = 13.sp)
                method.steps.forEachIndexed { index, step ->
                    Text("${index + 1}. $step", fontSize = 13.sp, lineHeight = 18.sp)
                }
                InfoLine("Ventajas", method.pros, Color(0xFF2E7D32))
                InfoLine("Limitaciones", method.cons, Color(0xFFC62828))
                InfoLine("Notas", method.notes, Color(0xFF455A64))
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String, color: Color) {
    if (value.isBlank()) return
    Column {
        Text(label, fontWeight = FontWeight.Bold, color = color, fontSize = 12.sp)
        Text(value, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

private fun extractionMethods(): List<ExtractionMethod> = listOf(
    ExtractionMethod(
        title = "Extracción ácido-base de alcaloides",
        group = "Alcaloides tropánicos, isoquinolínicos, indólicos…",
        filterGroup = "Alcaloides",
        principle = "Los alcaloides se protonan en medio ácido y pasan a fase acuosa; al basificar se liberan como bases y se extraen con disolvente orgánico.",
        solvents = "Etanol/metanol acidificado, agua ácida; fase orgánica como diclorometano, cloroformo o acetato de etilo.",
        equipment = "Embudo de decantación, matraces, papel pH, evaporador rotatorio o baño de evaporación.",
        conditions = "Temperatura ambiente o suave; varias particiones hasta agotar la fase.",
        steps = listOf(
            "Desengrasar el material seco si contiene ceras o aceites.",
            "Extraer con medio hidroalcohólico acidificado.",
            "Filtrar y concentrar suavemente.",
            "Basificar de forma controlada y particionar con fase orgánica.",
            "Secar y concentrar la fracción alcaloídica para análisis."
        ),
        pros = "Selectivo para alcaloides; separa bien de compuestos neutros/ácidos.",
        cons = "Disolventes clorados peligrosos; pH mal controlado degrada alcaloides sensibles.",
        notes = "Método clásico de cribado. Evitar calentar alcaloides termolábiles.",
        color = Color(0xFF6A1B9A)
    ),
    ExtractionMethod(
        title = "Maceración / percolación hidroalcohólica",
        group = "Glucósidos, flavonoides, taninos, fenoles y alcaloides polares",
        filterGroup = "Generales",
        principle = "Difusión de metabolitos desde la matriz vegetal hacia un disolvente polar o hidroalcohólico.",
        solvents = "Etanol 50–80 %, metanol acuoso, agua, mezclas acidificadas suaves.",
        equipment = "Recipiente con cierre, percolador, embudo Büchner, papel de filtro.",
        conditions = "Maceración: horas a días a temperatura ambiente, con agitación ocasional.",
        steps = listOf(
            "Pulverizar material seco y homogéneo.",
            "Cubrir con disolvente y mantener en contacto con agitación ocasional.",
            "Filtrar o percolar lentamente.",
            "Repetir extracción para agotar la matriz si procede.",
            "Concentrar extracto y almacenar protegido de luz/oxidación."
        ),
        pros = "Sencillo, barato, sin calor: respeta compuestos termolábiles.",
        cons = "Lento y de bajo rendimiento; gran consumo de disolvente.",
        notes = "La proporción agua/alcohol cambia mucho el perfil extraído.",
        color = Color(0xFF2E7D32)
    ),
    ExtractionMethod(
        title = "Soxhlet (extracción continua caliente)",
        group = "Alcaloides, lactonas, terpenos, pigmentos, polaridad media",
        filterGroup = "Generales",
        principle = "El disolvente caliente se recicla continuamente a través de la muestra, aumentando el rendimiento.",
        solvents = "Etanol, metanol, acetato de etilo, hexano según polaridad.",
        equipment = "Equipo Soxhlet (cuerpo, cartucho/dedal, balón, refrigerante), manta calefactora.",
        conditions = "Varias horas (4–24 h); a la temperatura de ebullición del disolvente.",
        steps = listOf(
            "Secar y pulverizar el material; cargar el dedal.",
            "Elegir disolvente compatible con el metabolito y su estabilidad térmica.",
            "Extraer de forma continua hasta agotamiento visual/analítico.",
            "Evaporar disolvente y fraccionar si es necesario."
        ),
        pros = "Alto rendimiento con poco disolvente; muy reproducible.",
        cons = "Calor prolongado: degrada termolábiles, hidroliza glucósidos.",
        notes = "No recomendado para compuestos termolábiles o muestras con enzimas activas.",
        color = Color(0xFFE65100)
    ),
    ExtractionMethod(
        title = "Glucósidos cardiotónicos",
        group = "Cardenólidos y bufadienólidos: Digitalis, Nerium, Convallaria…",
        filterGroup = "Glucósidos",
        principle = "Glucósidos cardiacos relativamente polares; se extraen con alcohol acuoso y se purifican retirando clorofilas/lípidos.",
        solvents = "Etanol o metanol acuoso; partición con acetato de etilo/butanol.",
        equipment = "Embudo de decantación, baño maría, evaporador, placa de TLC.",
        conditions = "Inactivación enzimática previa; extracción templada.",
        steps = listOf(
            "Inactivar enzimas vegetales con secado o alcohol caliente suave.",
            "Extraer con alcohol acuoso.",
            "Eliminar grasas y pigmentos con partición no polar.",
            "Concentrar y usar Keller-Kiliani, Kedde o TLC/HPLC."
        ),
        pros = "Permite concentrar y detectar cardenólidos para cribado cualitativo.",
        cons = "Compuestos MUY tóxicos; la hidrólisis enzimática altera el perfil.",
        notes = "Manipular cantidades pequeñas; nunca interpretar como aptitud medicinal.",
        color = Color(0xFFB71C1C)
    ),
    ExtractionMethod(
        title = "Glucósidos cianogénicos",
        group = "Prunus, Sambucus, Manihot, Linum…",
        filterGroup = "Glucósidos",
        principle = "Se preserva o libera HCN de forma controlada para análisis; la hidrólisis enzimática puede alterar el resultado.",
        solvents = "Agua fría, tampón o alcohol acuoso; trampas alcalinas para HCN.",
        equipment = "Sistema cerrado con trampa alcalina, campana, kit colorimétrico de cianuro.",
        conditions = "En frío para evitar hidrólisis; o incubación controlada según método.",
        steps = listOf(
            "Mantener la muestra fría para evitar hidrólisis prematura.",
            "Extraer o incubar según el método analítico elegido.",
            "Capturar o derivatizar el cianuro liberado para cuantificación.",
            "Confirmar con métodos colorimétricos o instrumentales."
        ),
        pros = "Permite cuantificar el potencial cianogénico de la muestra.",
        cons = "Riesgo de HCN; requiere ventilación y protocolo estricto.",
        notes = "Solo laboratorio ventilado. No acidificar fuera de protocolo seguro.",
        color = Color(0xFF00695C)
    ),
    ExtractionMethod(
        title = "Extracción de saponinas",
        group = "Saponinas triterpénicas y esteroideas",
        filterGroup = "Saponinas",
        principle = "Son anfipáticas: se extraen con alcohol acuoso y se enriquecen por partición con n-butanol o precipitación.",
        solvents = "Etanol/metanol acuoso, agua caliente, n-butanol, acetona/éter para precipitar.",
        equipment = "Embudo de decantación, baño maría, centrífuga, tubos para prueba de espuma.",
        conditions = "Extracción templada; partición a temperatura ambiente.",
        steps = listOf(
            "Desengrasar con disolvente no polar si la muestra es rica en lípidos.",
            "Extraer con alcohol acuoso.",
            "Particionar con n-butanol para enriquecer saponinas.",
            "Concentrar y evaluar espuma, hemólisis o TLC específica."
        ),
        pros = "Enriquece saponinas separándolas de azúcares y sales.",
        cons = "Emulsiones difíciles de romper; taninos pueden coextraerse.",
        notes = "La prueba de espuma es orientativa; detergentes naturales interfieren.",
        color = Color(0xFF1976D2)
    ),
    ExtractionMethod(
        title = "Taninos y polifenoles",
        group = "Taninos hidrolizables, condensados, ácidos fenólicos",
        filterGroup = "Fenoles/Taninos",
        principle = "Compuestos polares y oxidables; se extraen con solventes acuosos/alcohólicos evitando oxidación.",
        solvents = "Agua, acetona/agua, metanol/agua, etanol/agua; acidificación suave.",
        equipment = "Centrífuga, filtros, recipientes opacos, espectrofotómetro (Folin).",
        conditions = "Frío o templado; trabajar rápido para evitar oxidación.",
        steps = listOf(
            "Moler muestra y proteger de luz/aire.",
            "Extraer con solvente polar frío o templado.",
            "Centrifugar/filtrar rápidamente.",
            "Analizar con FeCl₃, gelatina-sal, Folin-Ciocalteu, TLC o HPLC."
        ),
        pros = "Compatible con cuantificación de fenoles totales (Folin).",
        cons = "Oxidación rápida; el calor condensa/degrada polifenoles.",
        notes = "Evitar calor excesivo. Acetona/agua mejora taninos condensados.",
        color = Color(0xFF5D4037)
    ),
    ExtractionMethod(
        title = "Extracción de flavonoides",
        group = "Flavonas, flavonoles, antocianinas e isoflavonas",
        filterGroup = "Flavonoides",
        principle = "La polaridad depende de aglicona/glicósido; los glicósidos son más polares y las agliconas más orgánicas.",
        solvents = "Etanol/metanol acuoso; antocianinas con medio ácido suave; acetato de etilo para agliconas.",
        equipment = "Embudo de decantación, evaporador, TLC con reveladores (NP/PEG, AlCl₃).",
        conditions = "Templado y al abrigo de la luz; ajuste de pH según objetivo.",
        steps = listOf(
            "Extraer con alcohol acuoso protegido de la luz.",
            "Ajustar pH según el flavonoide objetivo.",
            "Fraccionar con acetato de etilo o butanol si procede.",
            "Confirmar con Shinoda, AlCl₃, NP/PEG en TLC o HPLC."
        ),
        pros = "Versátil para gran variedad de flavonoides.",
        cons = "Antocianinas muy sensibles a pH, luz y temperatura.",
        notes = "El pH ácido estabiliza antocianinas (color rojo).",
        color = Color(0xFFAD1457)
    ),
    ExtractionMethod(
        title = "Hidrodestilación / arrastre de vapor",
        group = "Aceites esenciales y monoterpenos tóxicos",
        filterGroup = "Volátiles",
        principle = "Los volátiles se arrastran con vapor y se separan del hidrolato por diferencia de densidad/solubilidad.",
        solvents = "Agua/vapor; extracción posterior con disolvente orgánico para trazas.",
        equipment = "Equipo Clevenger o destilador de arrastre de vapor, refrigerante.",
        conditions = "1–4 h a ebullición; recoger aceite del hidrolato.",
        steps = listOf(
            "Trocear material fresco o seco según el aceite objetivo.",
            "Destilar con agua o vapor.",
            "Separar el aceite esencial del hidrolato.",
            "Secar y analizar por GC-MS si se requiere identificación."
        ),
        pros = "Aceite esencial puro, libre de disolvente.",
        cons = "Solo para volátiles; el calor puede alterar componentes sensibles.",
        notes = "No sirve para alcaloides o glucósidos no volátiles.",
        color = Color(0xFF00838F)
    ),
    // ───────── NUEVOS MÉTODOS ─────────
    ExtractionMethod(
        title = "Extracción asistida por ultrasonidos (UAE)",
        group = "Polifenoles, flavonoides, alcaloides, general",
        filterGroup = "Modernas",
        principle = "La cavitación ultrasónica rompe las paredes celulares y acelera la difusión del soluto al disolvente.",
        solvents = "Etanol/metanol acuoso, agua; el mismo que la maceración pero más rápido.",
        equipment = "Baño o sonda de ultrasonidos, vasos, control de temperatura.",
        conditions = "Minutos (10–60 min); controlar temperatura para no degradar.",
        steps = listOf(
            "Mezclar muestra molida con disolvente.",
            "Sonicar a la frecuencia/tiempo elegidos, refrigerando si es necesario.",
            "Filtrar o centrifugar.",
            "Concentrar y analizar."
        ),
        pros = "Rápido, bajo consumo de disolvente, sin calor intenso.",
        cons = "El sobrecalentamiento o exceso de sonicación puede degradar compuestos.",
        notes = "Muy usado en fitoquímica moderna por su eficiencia.",
        color = Color(0xFF0277BD)
    ),
    ExtractionMethod(
        title = "Extracción asistida por microondas (MAE)",
        group = "Flavonoides, alcaloides, aceites, general",
        filterGroup = "Modernas",
        principle = "Las microondas calientan el disolvente y el agua intracelular, generando presión que libera el soluto.",
        solvents = "Disolventes que absorben microondas (etanol, metanol, agua).",
        equipment = "Extractor de microondas de laboratorio (vasos cerrados o abiertos).",
        conditions = "Muy rápido (minutos); potencia y temperatura controladas.",
        steps = listOf(
            "Cargar muestra y disolvente en el vaso del microondas.",
            "Programar potencia, temperatura y tiempo.",
            "Enfriar, filtrar y concentrar.",
            "Analizar el extracto."
        ),
        pros = "Muy rápido y eficiente; alto rendimiento con poco disolvente.",
        cons = "Riesgo de sobrepresión; no apto para muy termolábiles.",
        notes = "Requiere equipo específico de microondas para laboratorio.",
        color = Color(0xFF512DA8)
    ),
    ExtractionMethod(
        title = "Fluidos supercríticos (SFE, CO₂)",
        group = "Aceites, terpenos, compuestos lipófilos",
        filterGroup = "Modernas",
        principle = "El CO₂ supercrítico actúa como disolvente ajustable con la presión/temperatura; se evapora dejando extracto limpio.",
        solvents = "CO₂ supercrítico, opcionalmente con cosolvente (etanol).",
        equipment = "Equipo de SFE (bomba de alta presión, celda, separador).",
        conditions = "Alta presión (>73 bar) y ~31–60 °C.",
        steps = listOf(
            "Cargar la muestra seca en la celda de extracción.",
            "Presurizar con CO₂ y ajustar densidad con presión/temperatura.",
            "Recoger el extracto al despresurizar.",
            "Analizar (sin residuo de disolvente)."
        ),
        pros = "Extracto sin disolvente residual; selectivo y limpio.",
        cons = "Equipo caro; poco eficaz para compuestos muy polares sin cosolvente.",
        notes = "Ideal para aceites esenciales y lipófilos de alta pureza.",
        color = Color(0xFF00897B)
    ),
    ExtractionMethod(
        title = "Partición líquido-líquido (fraccionamiento)",
        group = "Fraccionamiento por polaridad (todos los grupos)",
        filterGroup = "Generales",
        principle = "El extracto crudo se reparte entre disolventes inmiscibles de polaridad creciente, separando los metabolitos por afinidad.",
        solvents = "Hexano → diclorometano/cloroformo → acetato de etilo → n-butanol → agua.",
        equipment = "Embudo de decantación, evaporador rotatorio.",
        conditions = "Temperatura ambiente; varias particiones sucesivas.",
        steps = listOf(
            "Disolver el extracto crudo en agua/alcohol acuoso.",
            "Particionar sucesivamente con disolventes de polaridad creciente.",
            "Concentrar cada fracción por separado.",
            "Analizar cada fracción (TLC, HPLC)."
        ),
        pros = "Separa familias de compuestos por polaridad; paso clave de purificación.",
        cons = "Emulsiones; uso de disolventes clorados; laborioso.",
        notes = "Suele ir después de una extracción inicial (maceración/Soxhlet).",
        color = Color(0xFF455A64)
    ),
    ExtractionMethod(
        title = "Extracción en fase sólida (SPE)",
        group = "Limpieza y concentración de extractos",
        filterGroup = "Modernas",
        principle = "El analito se retiene en un cartucho sólido y luego se eluye selectivamente, eliminando interferencias.",
        solvents = "Acondicionamiento y elución con disolventes de polaridad ajustada.",
        equipment = "Cartuchos SPE (C18, sílice, intercambio iónico), manifold de vacío.",
        conditions = "Rápido; a temperatura ambiente.",
        steps = listOf(
            "Acondicionar el cartucho.",
            "Cargar la muestra para que el analito se retenga.",
            "Lavar para eliminar interferencias.",
            "Eluir el analito purificado y concentrado."
        ),
        pros = "Excelente limpieza y preconcentración antes del análisis instrumental.",
        cons = "Coste de cartuchos; capacidad limitada de muestra.",
        notes = "Muy usada como paso previo a HPLC/GC-MS.",
        color = Color(0xFF3949AB)
    ),
    ExtractionMethod(
        title = "Extracción de lípidos (Folch / Bligh-Dyer)",
        group = "Aceites fijos, ácidos grasos, lípidos tóxicos",
        filterGroup = "Lípidos",
        principle = "Mezcla cloroformo-metanol-agua que separa los lípidos en la fase clorofórmica.",
        solvents = "Cloroformo:metanol (2:1 Folch) o (1:2 Bligh-Dyer) + agua.",
        equipment = "Embudo de decantación, centrífuga, evaporador.",
        conditions = "Temperatura ambiente; agitación y separación de fases.",
        steps = listOf(
            "Homogeneizar la muestra con cloroformo-metanol.",
            "Añadir agua/sal para inducir separación de fases.",
            "Recoger la fase clorofórmica (lípidos).",
            "Evaporar y pesar/analizar."
        ),
        pros = "Estándar para extraer y cuantificar lípidos totales.",
        cons = "Disolventes clorados tóxicos; requiere campana.",
        notes = "Bligh-Dyer es una variante con menos disolvente para muestras acuosas.",
        color = Color(0xFFF9A825)
    ),
    ExtractionMethod(
        title = "Infusión / decocción acuosa",
        group = "Compuestos hidrosolubles (cribado simple)",
        filterGroup = "Generales",
        principle = "Extracción con agua caliente: infusión (verter agua caliente) o decocción (hervir el material).",
        solvents = "Agua (caliente o en ebullición).",
        equipment = "Recipiente, fuente de calor, colador/filtro.",
        conditions = "Infusión: 5–15 min con agua casi hirviendo. Decocción: 15–30 min hirviendo.",
        steps = listOf(
            "Trocear el material vegetal.",
            "Infusión: verter agua caliente y reposar. Decocción: hervir el tiempo indicado.",
            "Filtrar.",
            "Usar el extracto acuoso para pruebas cualitativas."
        ),
        pros = "Sencillísimo, sin disolventes orgánicos; refleja preparaciones tradicionales.",
        cons = "Solo compuestos hidrosolubles; el calor degrada termolábiles.",
        notes = "Útil para taninos, mucílagos y algunos glucósidos polares.",
        color = Color(0xFF6D4C41)
    )
)
