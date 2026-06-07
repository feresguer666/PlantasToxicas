package com.toxicplants.database.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class ReagentInfo(
    val name: String,
    val category: String,
    val group: String,          // grupo para filtrar
    val detects: String,
    val composition: String,
    val preparation: String,    // NUEVO: preparación paso a paso
    val reactionTime: String,   // NUEVO: tiempo de reacción
    val positive: String,
    val storage: String,        // NUEVO: almacenamiento / caducidad
    val cautions: String,
    val color: Color
)

// Grupos para los filtros
private val REAGENT_GROUPS = listOf(
    "Todos", "Alcaloides", "Color", "Glucósidos", "Flavonoides",
    "Fenoles/Taninos", "Saponinas", "Azúcares", "Aminoácidos/Proteínas", "Terpenos/Esteroles"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChemicalReagentsScreen(onBack: () -> Unit) {
    val allReagents = remember { phytochemicalReagents() }
    var query by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf("Todos") }

    val filtered = remember(query, selectedGroup) {
        allReagents.filter { r ->
            (selectedGroup == "Todos" || r.group == selectedGroup) &&
                    (query.isBlank() ||
                            r.name.contains(query, true) ||
                            r.detects.contains(query, true) ||
                            r.category.contains(query, true))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("🧫 Reactivos fitoquímicos (${allReagents.size})", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Precipitación · Coloración · Cribado cualitativo", fontSize = 12.sp, color = Color.White.copy(alpha = 0.82f))
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

            // Buscador
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Buscar reactivo o qué detecta…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            // Filtros por grupo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                REAGENT_GROUPS.forEach { g ->
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
                item { SafetyReagentCard() }
                if (filtered.isEmpty()) {
                    item {
                        Text(
                            "Sin reactivos para «$query» en $selectedGroup.",
                            color = Color.Gray,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                items(filtered) { reagent -> ReagentCard(reagent) }
            }
        }
    }
}

@Composable
private fun SafetyReagentCard() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Column(Modifier.padding(14.dp)) {
            Text("⚠️ Seguridad", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
            Text(
                "Muchos reactivos usan ácidos concentrados, sales de mercurio, bismuto, yodo o metales pesados. Solo laboratorio equipado, campana, EPI y residuos gestionados. Los resultados son orientativos y requieren confirmación instrumental.",
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun ReagentCard(reagent: ReagentInfo) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row {
                Text(if (reagent.category.contains("recipitación")) "⚪" else "🎨", fontSize = 22.sp)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(reagent.name, fontWeight = FontWeight.Bold, color = reagent.color, fontSize = 16.sp)
                    Text(reagent.category, color = Color.Gray, fontSize = 12.sp)
                }
                Text(if (expanded) "▲" else "▼", color = reagent.color, fontSize = 14.sp)
            }
            ReagentLine("Detecta", reagent.detects, reagent.color)
            ReagentLine("Resultado positivo típico", reagent.positive, reagent.color)

            // Botón ver más / menos
            Text(
                if (expanded) "Ver menos ▲" else "Ver más (preparación, tiempo, almacenamiento) ▼",
                color = reagent.color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
                    .clickableNoRipple { expanded = !expanded }
            )

            if (expanded) {
                ReagentLine("Composición / fórmula", reagent.composition, reagent.color)
                ReagentLine("Preparación", reagent.preparation, reagent.color)
                ReagentLine("Tiempo de reacción", reagent.reactionTime, reagent.color)
                ReagentLine("Almacenamiento / caducidad", reagent.storage, reagent.color)
                ReagentLine("Precauciones e interferencias", reagent.cautions, Color(0xFF455A64))
            }
        }
    }
}

// Click simple para el texto "ver más"
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.clickable { onClick() }

@Composable
private fun ReagentLine(label: String, value: String, color: Color) {
    Column {
        Text(label, fontWeight = FontWeight.Bold, color = color, fontSize = 12.sp)
        Text(value, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

private fun phytochemicalReagents(): List<ReagentInfo> = listOf(
    ReagentInfo(
        name = "Mayer",
        category = "Precipitación de alcaloides",
        group = "Alcaloides",
        detects = "Alcaloides en extractos acidificados.",
        composition = "Tetraiodomercurato(II) potásico: HgCl₂ y KI en agua; especie activa K₂[HgI₄].",
        preparation = "Disolver 1,36 g de HgCl₂ en 60 mL de agua. Aparte, 5 g de KI en 10 mL. Mezclar y enrasar a 100 mL.",
        reactionTime = "Inmediato: precipitado a los pocos segundos.",
        positive = "Precipitado crema, blanco amarillento o turbidez.",
        storage = "Frasco ámbar, temperatura ambiente. Estable meses. Residuo con mercurio: gestión especial.",
        cautions = "Contiene mercurio. Falsos positivos con aminas, proteínas u otros compuestos nitrogenados.",
        color = Color(0xFF6A1B9A)
    ),
    ReagentInfo(
        name = "Dragendorff",
        category = "Precipitación de alcaloides",
        group = "Alcaloides",
        detects = "Alcaloides, bases orgánicas y aminas terciarias.",
        composition = "Complejo yodobismutato: subnitrato de bismuto + KI en medio ácido.",
        preparation = "Sol. A: 0,85 g subnitrato de Bi en 10 mL ác. acético + 40 mL agua. Sol. B: 8 g KI en 20 mL agua. Mezclar A+B; usar diluido.",
        reactionTime = "Inmediato.",
        positive = "Precipitado naranja, rojo ladrillo o marrón anaranjado.",
        storage = "Frasco ámbar. La solución madre dura meses; la diluida, prepárala el día de uso.",
        cautions = "Muy sensible pero poco específico. Reacciona con aminas no alcaloídicas y compuestos cuaternarios.",
        color = Color(0xFFD84315)
    ),
    ReagentInfo(
        name = "Wagner",
        category = "Precipitación de alcaloides",
        group = "Alcaloides",
        detects = "Alcaloides en medio ácido.",
        composition = "Yodo-yoduro potásico: I₂ + KI en agua.",
        preparation = "2 g de KI + 1,27 g de I₂ en 5 mL de agua; enrasar a 100 mL.",
        reactionTime = "Inmediato.",
        positive = "Precipitado marrón rojizo.",
        storage = "Frasco ámbar, lejos de luz. El yodo se sublima: cierre hermético.",
        cautions = "El yodo reacciona con fenoles, almidón y compuestos reductores.",
        color = Color(0xFF795548)
    ),
    ReagentInfo(
        name = "Hager",
        category = "Precipitación de alcaloides",
        group = "Alcaloides",
        detects = "Alcaloides y bases orgánicas.",
        composition = "Solución saturada de ácido pícrico.",
        preparation = "Saturar agua destilada con ácido pícrico (≈1,2 g/100 mL a 20 °C).",
        reactionTime = "Inmediato.",
        positive = "Precipitado amarillo.",
        storage = "Mantener SIEMPRE húmedo. El ácido pícrico seco es explosivo.",
        cautions = "Ácido pícrico: peligroso si se cristaliza/seca. Manejo profesional obligatorio.",
        color = Color(0xFFF9A825)
    ),
    ReagentInfo(
        name = "Ácido tánico",
        category = "Precipitación de alcaloides",
        group = "Alcaloides",
        detects = "Alcaloides por formación de tanatos insolubles.",
        composition = "Solución acuosa de ácido tánico (~10 %).",
        preparation = "Disolver 10 g de ácido tánico en 100 mL de agua destilada.",
        reactionTime = "Inmediato a 1 min.",
        positive = "Precipitado blanquecino o amarillento.",
        storage = "Refrigerar; se oxida y oscurece con el tiempo. Preparar en poca cantidad.",
        cautions = "Precipita proteínas y otros compuestos polares; baja especificidad.",
        color = Color(0xFF5D4037)
    ),
    ReagentInfo(
        name = "Silicotúngstico / Fosfotúngstico",
        category = "Precipitación de alcaloides",
        group = "Alcaloides",
        detects = "Alcaloides, bases orgánicas y algunas aminas.",
        composition = "Ácido silicotúngstico o fosfotúngstico en medio ácido.",
        preparation = "Solución al 5–10 % en agua, acidulada con HCl diluido.",
        reactionTime = "Inmediato.",
        positive = "Precipitado blanco o amarillento.",
        storage = "Frasco cerrado, ambiente. Estable.",
        cautions = "Útil como confirmación cruzada, no concluyente por sí solo.",
        color = Color(0xFF455A64)
    ),
    ReagentInfo(
        name = "Marquis",
        category = "Reactivo de color",
        group = "Color",
        detects = "Fenoles, alcaloides aromáticos y estructuras oxidables.",
        composition = "Formaldehído en ácido sulfúrico concentrado.",
        preparation = "1 mL de formaldehído (37 %) en 10 mL de H₂SO₄ concentrado (añadir formol sobre el ácido, en frío).",
        reactionTime = "Segundos; observar la evolución de color.",
        positive = "Violeta, púrpura, marrón, naranja o verde según estructura.",
        storage = "Inestable: preparar en pequeñas cantidades y desechar tras semanas. Frasco ámbar.",
        cautions = "Ácido sulfúrico y formaldehído: muy corrosivo/tóxico. Prueba inespecífica.",
        color = Color(0xFF8E24AA)
    ),
    ReagentInfo(
        name = "Froehde",
        category = "Reactivo de color",
        group = "Color",
        detects = "Alcaloides y compuestos fenólicos oxidables.",
        composition = "Molibdato sódico/ácido molíbdico en ácido sulfúrico concentrado.",
        preparation = "0,5 g de molibdato sódico en 100 mL de H₂SO₄ concentrado.",
        reactionTime = "Segundos a 1 min.",
        positive = "Azules, verdes, violetas o marrones según compuesto.",
        storage = "Frasco ámbar; degrada con el tiempo. Preparar poca cantidad.",
        cautions = "Comparar con controles. Reactivo muy corrosivo.",
        color = Color(0xFF1565C0)
    ),
    ReagentInfo(
        name = "Mecke",
        category = "Reactivo de color",
        group = "Color",
        detects = "Alcaloides y compuestos aromáticos; cribado comparativo.",
        composition = "Ácido selenioso en ácido sulfúrico concentrado.",
        preparation = "0,25 g de ácido selenioso en 25 mL de H₂SO₄ concentrado.",
        reactionTime = "Segundos.",
        positive = "Verdes, azules o negros en ciertos compuestos.",
        storage = "Frasco ámbar, etiquetado de tóxico (Se). Estable semanas.",
        cautions = "Selenio y ácido concentrado: toxicidad elevada. Solo microensayos.",
        color = Color(0xFF00838F)
    ),
    ReagentInfo(
        name = "Mandelin",
        category = "Reactivo de color",
        group = "Color",
        detects = "Alcaloides y otros compuestos oxidables.",
        composition = "Metavanadato amónico en ácido sulfúrico concentrado.",
        preparation = "0,5 g de metavanadato amónico en 100 mL de H₂SO₄ concentrado.",
        reactionTime = "Segundos.",
        positive = "Verde, azul, marrón, naranja o negro según analito.",
        storage = "Frasco ámbar; reemplazar si cambia de color base.",
        cautions = "Corrosivo y oxidante. Interpretación visual subjetiva.",
        color = Color(0xFF2E7D32)
    ),
    ReagentInfo(
        name = "Ehrlich / Van Urk",
        category = "Reactivo de color",
        group = "Color",
        detects = "Indoles: alcaloides indólicos, triptaminas, ergolinas.",
        composition = "p-dimetilaminobenzaldehído (p-DMAB) en medio ácido.",
        preparation = "1 g de p-DMAB en 50 mL de etanol + 50 mL de HCl concentrado (Van Urk usa H₂SO₄).",
        reactionTime = "1–5 min; el color se intensifica.",
        positive = "Rosa, rojo, violeta o púrpura para núcleos indólicos.",
        storage = "Frasco ámbar, refrigerado. Estable algunas semanas.",
        cautions = "No todos los indoles responden igual; interferencias con aminas aromáticas.",
        color = Color(0xFFC2185B)
    ),
    ReagentInfo(
        name = "Liebermann-Burchard",
        category = "Reactivo de color",
        group = "Terpenos/Esteroles",
        detects = "Esteroles, triterpenos y saponinas esteroideas.",
        composition = "Anhídrido acético + ácido sulfúrico concentrado.",
        preparation = "Disolver muestra en cloroformo; añadir anhídrido acético y, por la pared, gotas de H₂SO₄ concentrado.",
        reactionTime = "Observar 5–15 min; el color evoluciona.",
        positive = "Verde, azul verdoso o violeta para esteroles/triterpenos.",
        storage = "Preparar en el momento; la mezcla no se conserva.",
        cautions = "Fuertemente corrosivo; la humedad altera la reacción.",
        color = Color(0xFF00695C)
    ),
    ReagentInfo(
        name = "Salkowski",
        category = "Reactivo de color",
        group = "Terpenos/Esteroles",
        detects = "Esteroides y triterpenos insaturados.",
        composition = "Ácido sulfúrico concentrado sobre disolución clorofórmica.",
        preparation = "Disolver la muestra en cloroformo y añadir cuidadosamente H₂SO₄ concentrado formando dos capas.",
        reactionTime = "Inmediato a pocos minutos.",
        positive = "Capa de cloroformo roja/amarilla; interfase amarillo-verdosa fluorescente.",
        storage = "Usar ácido fresco; no se prepara mezcla almacenable.",
        cautions = "Ácido sulfúrico concentrado. Diferenciar de Liebermann-Burchard por la fluorescencia.",
        color = Color(0xFF00897B)
    ),
    ReagentInfo(
        name = "Keller-Kiliani",
        category = "Color para glucósidos cardiacos",
        group = "Glucósidos",
        detects = "Desoxiazúcares de cardenólidos en glucósidos cardiotónicos.",
        composition = "Ácido acético glacial con trazas de FeCl₃ y capa de H₂SO₄ concentrado.",
        preparation = "Disolver muestra en ác. acético glacial + 1 gota de FeCl₃ al 5 %; añadir por la pared H₂SO₄ concentrado.",
        reactionTime = "1–3 min; observar el anillo en la interfase.",
        positive = "Anillo pardo/rojizo en interfase y azul verdoso en la fase acética.",
        storage = "Preparar en el momento.",
        cautions = "Indicativa, no cuantitativa. Riesgo por ácido sulfúrico y acético glacial.",
        color = Color(0xFFB71C1C)
    ),
    ReagentInfo(
        name = "Kedde / Baljet",
        category = "Color para cardenólidos",
        group = "Glucósidos",
        detects = "Anillo lactónico insaturado de cardenólidos.",
        composition = "Kedde: ác. 3,5-dinitrobenzoico en medio alcalino. Baljet: picrato alcalino.",
        preparation = "Kedde: 2 % de ác. 3,5-dinitrobenzoico en etanol + KOH al 5,7 % (1:1). Aplicar sobre la muestra.",
        reactionTime = "Inmediato; el color decae en minutos.",
        positive = "Violeta/púrpura (Kedde) o naranja/rojo (Baljet).",
        storage = "Mezclar las dos soluciones justo antes de usar.",
        cautions = "Requieren controles y confirmación cromatográfica.",
        color = Color(0xFFD32F2F)
    ),
    ReagentInfo(
        name = "Bornträger",
        category = "Color para antraquinonas",
        group = "Glucósidos",
        detects = "Antraquinonas libres o liberadas de glucósidos antraquinónicos.",
        composition = "Extracción orgánica y alcalinización con amoníaco o base diluida.",
        preparation = "Extraer con tolueno/éter; agitar la fase orgánica con NH₃ diluido o NaOH.",
        reactionTime = "Inmediato al alcalinizar.",
        positive = "Color rosa, rojo o violeta en la fase alcalina.",
        storage = "Reactivos por separado; estables.",
        cautions = "La variante modificada requiere hidrólisis previa para O/C-glucósidos.",
        color = Color(0xFFAD1457)
    ),
    ReagentInfo(
        name = "Shinoda",
        category = "Color para flavonoides",
        group = "Flavonoides",
        detects = "Flavonas y flavonoles con sistemas conjugados reducibles.",
        composition = "Magnesio metálico + HCl en extracto alcohólico.",
        preparation = "Al extracto alcohólico, añadir virutas de Mg y luego gotas de HCl concentrado.",
        reactionTime = "1–3 min; observar el burbujeo y el color.",
        positive = "Rojo, rosa, naranja o magenta.",
        storage = "Usar Mg y HCl frescos; no se almacena mezcla.",
        cautions = "No todos los flavonoides responden; antocianinas/pigmentos interfieren.",
        color = Color(0xFFE91E63)
    ),
    ReagentInfo(
        name = "Cloruro férrico",
        category = "Color para fenoles/taninos",
        group = "Fenoles/Taninos",
        detects = "Fenoles, taninos hidrolizables y algunos polifenoles.",
        composition = "FeCl₃ acuoso o alcohólico diluido (1–5 %).",
        preparation = "Disolver 1–5 g de FeCl₃ en 100 mL de agua o etanol.",
        reactionTime = "Inmediato.",
        positive = "Azul, verde, negro o violeta según tipo de fenol/tanino.",
        storage = "Frasco ámbar; el FeCl₃ es higroscópico, cerrar bien.",
        cautions = "Muy inespecífico; útil para cribado inicial.",
        color = Color(0xFF37474F)
    ),
    ReagentInfo(
        name = "Vainillina-HCl",
        category = "Color para taninos condensados",
        group = "Fenoles/Taninos",
        detects = "Taninos condensados (proantocianidinas) y catequinas.",
        composition = "Vainillina en etanol/metanol con HCl concentrado.",
        preparation = "1 g de vainillina en 100 mL de etanol; mezclar 1:1 con HCl concentrado al usar.",
        reactionTime = "2–5 min.",
        positive = "Color rojo intenso.",
        storage = "Solución de vainillina refrigerada; mezclar con HCl al momento.",
        cautions = "Sensible a temperatura; estandarizar condiciones para comparar.",
        color = Color(0xFFEF6C00)
    ),
    ReagentInfo(
        name = "Prueba de espuma",
        category = "Ensayo físico para saponinas",
        group = "Saponinas",
        detects = "Saponinas por actividad tensioactiva.",
        composition = "Extracto acuoso agitado en condiciones controladas.",
        preparation = "Agitar vigorosamente 2 mL de extracto acuoso en tubo durante 15 s.",
        reactionTime = "Observar la espuma a los 10–15 min.",
        positive = "Espuma persistente (>1 cm) durante varios minutos.",
        storage = "No aplica (ensayo directo).",
        cautions = "Detergentes naturales, proteínas o contaminación dan falsos positivos.",
        color = Color(0xFF1976D2)
    ),
    ReagentInfo(
        name = "Molisch",
        category = "Color para azúcares",
        group = "Azúcares",
        detects = "Carbohidratos en general (mono, di y polisacáridos).",
        composition = "α-naftol en etanol + ácido sulfúrico concentrado.",
        preparation = "Sol. de α-naftol al 5 % en etanol; añadir 2 gotas a la muestra y, por la pared, H₂SO₄ concentrado.",
        reactionTime = "Inmediato; anillo en la interfase.",
        positive = "Anillo violeta/púrpura en la interfase.",
        storage = "α-naftol refrigerado y al abrigo de la luz (se oscurece).",
        cautions = "Prueba general de azúcares; H₂SO₄ concentrado peligroso.",
        color = Color(0xFF6D4C41)
    ),
    ReagentInfo(
        name = "Fehling",
        category = "Color para azúcares reductores",
        group = "Azúcares",
        detects = "Azúcares reductores (glucosa, fructosa, maltosa…).",
        composition = "Fehling A (CuSO₄) + Fehling B (tartrato sódico-potásico + NaOH).",
        preparation = "Mezclar volúmenes iguales de A y B justo antes de usar; calentar con la muestra al baño maría.",
        reactionTime = "Calentar 1–3 min.",
        positive = "Precipitado rojo ladrillo (Cu₂O).",
        storage = "A y B por separado, estables. Mezcla: usar de inmediato.",
        cautions = "No detecta azúcares no reductores (sacarosa). Requiere calentamiento.",
        color = Color(0xFFBF360C)
    ),
    ReagentInfo(
        name = "Benedict",
        category = "Color para azúcares reductores",
        group = "Azúcares",
        detects = "Azúcares reductores (alternativa más estable a Fehling).",
        composition = "Citrato sódico + carbonato sódico + sulfato de cobre.",
        preparation = "Reactivo único listo para usar; añadir muestra y calentar al baño maría 3–5 min.",
        reactionTime = "3–5 min al calor.",
        positive = "Verde → amarillo → naranja → rojo ladrillo según cantidad.",
        storage = "Reactivo único, estable a temperatura ambiente durante meses.",
        cautions = "Semicuantitativo por el color. No detecta azúcares no reductores.",
        color = Color(0xFFE65100)
    ),
    ReagentInfo(
        name = "Ninhidrina",
        category = "Color para aminoácidos",
        group = "Aminoácidos/Proteínas",
        detects = "Aminoácidos libres y aminas primarias.",
        composition = "Ninhidrina en etanol/butanol (0,2–2 %).",
        preparation = "Solución de ninhidrina al 0,2 % en etanol; rociar/añadir y calentar suavemente (~100 °C).",
        reactionTime = "Aparece al calentar (1–5 min).",
        positive = "Color violeta-azulado (púrpura de Ruhemann); prolina da amarillo.",
        storage = "Frasco ámbar, refrigerado; se degrada con luz/aire.",
        cautions = "Sensible; evitar contaminación con la piel (da falsos positivos por aminoácidos).",
        color = Color(0xFF512DA8)
    ),
    ReagentInfo(
        name = "Biuret",
        category = "Color para proteínas",
        group = "Aminoácidos/Proteínas",
        detects = "Enlaces peptídicos (proteínas y péptidos).",
        composition = "Sulfato de cobre en medio alcalino (NaOH/KOH).",
        preparation = "A la muestra añadir NaOH al 10 % y unas gotas de CuSO₄ al 1 %.",
        reactionTime = "Inmediato a 5 min.",
        positive = "Color violeta/púrpura (no para aminoácidos libres).",
        storage = "Reactivos por separado; estables.",
        cautions = "Requiere al menos dos enlaces peptídicos; aminoácidos sueltos no reaccionan.",
        color = Color(0xFF283593)
    )
)
