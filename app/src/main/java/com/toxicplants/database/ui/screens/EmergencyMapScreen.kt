package com.toxicplants.database.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Share
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

data class AntidoteInfo(
    val id: String,
    val title: String,
    val plants: String,
    val toxins: String,
    val antidoteName: String,
    val symptoms: String,
    val humanTreatment: String,
    val vetTreatment: String,
    val contraindications: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyMapScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current

    fun launchGeoSearch(query: String) {
        try {
            val uri = Uri.parse("geo:0,0?q=" + Uri.encode(query))
            val intent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(query))
                context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
            } catch (ex: Exception) {
                Toast.makeText(context, "No se encontró aplicación de mapas", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val antidotes = remember {
        listOf(
            AntidoteInfo(
                id = "tropanicos",
                title = "1. Alcaloides Tropánicos / Síndrome Anticolinérgico",
                plants = "Atropa belladonna (Belladona), Datura stramonium (Estramonio), Hyoscyamus niger (Beleño), Mandragora",
                toxins = "Atropina, Escopolamina, Hiosciamina",
                antidoteName = "FISOSTIGMINA (Salicilato de fisostigmina)",
                symptoms = "Midriasis severa (pupilas muy dilatadas), taquicardia, piel roja y seca, hipertermia, alucinaciones violentas, delirio, agitación motriz, retención urinaria (\"rojo como un tomate, seco como un hueso, loco como una cabra\").",
                humanTreatment = "• Antídoto específico: FISOSTIGMINA 0.5–2 mg IV lenta (en adultos; 0.02 mg/kg en niños). Revierte rápidamente el delirio y la agitación severa al inhibir la acetilcolinesterasa.\n• Soporte: Carbón activado (si < 2 horas desde ingesta), Benzodiazepinas (Diazepam/Lorazepam) para convulsiones o agitación extrema.\n• Ambiente: Cuadro oscuro, tranquilo, control estrecho de temperatura y ECG.",
                vetTreatment = "• Perros/Gatos: Fisostigmina 1-2 mg IV lenta (en perros grandes) o 0.02-0.05 mg/kg IV.\n• Sedación con Diazepam 0.5 mg/kg IV si hay excitación severa. Fluidoterapia de mantenimiento y vaciado vesical.",
                contraindications = "⚠️ NUNCA utilizar fenotiazinas (ej. Clorpromazina) ni haloperidol para sedar, ya que poseen actividad anticolinérgica propia y empeorarán drásticamente el delirio y la toxicidad."
            ),
            AntidoteInfo(
                id = "digitalicos",
                title = "2. Glucósidos Cardiotóxicos / Cardenólidos",
                plants = "Nerium oleander (Adelfa), Digitalis purpurea (Dedalera), Convallaria majalis (Lirio de los valles), Thevetia",
                toxins = "Oleandrina, Digitoxina, Gitoxina (Bloqueo de la bomba Na+/K+ ATPasa cardiaca)",
                antidoteName = "ANTICUERPOS ANTIDIGITAL (Fragmentos Fab anti-digoxina / DigiFab)",
                symptoms = "Bradicardia severa, bloqueo aurículo-ventricular (AV), hiperpotasemia fatal (potasio > 5.5 mEq/L), arritmias ventriculares polimórficas, náuseas y vómitos incoercibles, visión amarilla/verde (xantopsia).",
                humanTreatment = "• Antídoto específico: FRAGMENTOS Fab ANTI-DIGOXINA (Digibind / DigiFab). Neutralizan oleandrina y digitoxina en sangre. Indicado si K+ > 5.0 mEq/L, arritmia ventricular o bloqueo AV sintomático.\n• Soporte: Atropina 0.5-1 mg IV para bradicardia sintomática. Marcapasos transitorio si hay bloqueo refractario.\n• Control electrolítico: Tratar hiperpotasemia con insulina/glucosa o resinas de intercambio.",
                vetTreatment = "• Perros/Gatos: Fragmentos Fab antidigoxina (si disponible en hospital veterinario). Atropina 0.02-0.04 mg/kg IV para bradicardia severa.\n• Fluidoterapia con Suero Fisiológico 0.9%. Prohibido Ringer Lactato.",
                contraindications = "⚠️ PROHIBIDO ADMINISTRAR CALCIO IV (Gluconato o Cloruro cálcico): El calcio intravenoso en presencia de glucósidos cardiacos provoca una parada cardiaca instantánea e irreversible en sístole (\"corazón de piedra\")."
            ),
            AntidoteInfo(
                id = "cianogenicos",
                title = "3. Glucósidos Cianogénicos / Cianuro de Hidrógeno",
                plants = "Prunus laurocerasus (Laurel cerezo), Semillas de Prunus (Almendras amargas, melocotón, albaricoque), Manihot (Yuca cruda)",
                toxins = "Amigdalina, Prunasina (liberan HCN que bloquea la citocromo oxidasa mitocondrial)",
                antidoteName = "HIDROXOCOBALAMINA (Cyanokit 5g IV) o KIT DE CIANURO",
                symptoms = "Aliento característico a almendras amargas, hiperventilación inicial seguida de disnea y apnea, cefalea intensa, convulsiones, coloración rojo cereza en piel y mucosas (sangre venosa oxigenada por falta de extracción celular), colapso cardiovascular.",
                humanTreatment = "• Antídoto de 1ª elección: HIDROXOCOBALAMINA (Cyanokit) 5 g IV en infusión de 15 min (niños: 70 mg/kg). Se une al cianuro formando cianocobalamina (vitamina B12 no tóxica que se excreta por orina).\n• Antídoto alternativo: Nitrito de sodio IV + Tiosulfato sódico IV (induce metahemoglobinemia que atrapa cianuro).\n• Soporte: Oxigenoterapia al 100% con mascarilla con reservorio o intubación endotraqueal precoz.",
                vetTreatment = "• Perros/Gatos: Hidroxocobalamina 70 mg/kg IV lenta o Tiosulfato sódico al 20% (500 mg/kg IV en 10 min).\n• Oxigenoterapia intensiva 100% en jaula o mascarilla.",
                contraindications = "⚠️ No demorar la administración del antídoto esperando confirmación de laboratorio; el cianuro produce anoxia celular rápida y daño cerebral irreversible en pocos minutos."
            ),
            AntidoteInfo(
                id = "aconitina",
                title = "4. Toxinas Aconitinas / Cardiotoxinas Neurotóxicas",
                plants = "Aconitum napellus (Acónito común, matalobos), Veratrum album (Ballestera), Delphinium",
                toxins = "Aconitina, Mesaconitina (Apertura persistente de canales de sodio voltaje-dependientes)",
                antidoteName = "SOPORTE ANTIARRÍTMICO (Amiodarona) + ECMO",
                symptoms = "Parestesias (hormigueo intenso y quemazón) en boca, lengua y cara que desciende hacia las extremidades, debilidad muscular intensa, hipotensión refractaria, arritmias ventriculares graves (Torsades de pointes, taquicardia ventricular), parada cardiaca.",
                humanTreatment = "• No existe antídoto químico específico.\n• Antiarrítmicos: AMIODARONA IV (o Flecainida / Lidocaína). Evitar fármacos que depriman la conducción si hay bradicardia previa.\n• Soporte vital avanzado: Ante parada cardiaca o arritmia refractaria, iniciar soporte vital extracorpóreo (ECMO / Bypass cardiopulmonar) hasta el aclaramiento renal de la toxina (24-48h).\n• Lavado gástrico enérgico y carbón activado en dosis repetidas.",
                vetTreatment = "• Perros: Lidocaína 2 mg/kg en bolo IV seguido de infusión continua (25-80 mcg/kg/min) para taquicardia ventricular.\n• Lavado gástrico bajo anestesia y carbón activado con sorbitol.",
                contraindications = "⚠️ Evitar el uso de cardioversores o marcapasos sin control antiarrítmico previo, ya que el acónito sensibiliza extremadamente el miocardio a la fibrilación ventricular."
            ),
            AntidoteInfo(
                id = "ricinas",
                title = "5. Toxinas Ricinas e Inhibidores Ribosómicos",
                plants = "Ricinus communis (Ricino / Higuera del infierno), Abrus precatorius (Regaliz americano / Ojo de cangrejo), Robinia",
                toxins = "Ricina, Abrina (Inhibición irreversible de la síntesis proteica en ribosomas 60S)",
                antidoteName = "SOPORTE INTENSIVO UCI (Fluidoterapia agresiva + Lavado precoz)",
                symptoms = "Período de latencia de 6 a 12 horas, seguido de gastroenteritis hemorrágica violenta, vómitos incoercibles, diarrea sanguinolenta grave, deshidratación masiva, shock hipovolémico, necrosis hepática y renal, fallo multiorgánico.",
                humanTreatment = "• No existe antídoto comercial específico (investigación con antitoxinas monoclonales).\n• Descontaminación precoz: Lavado gástrico enérgico y carbón activado si la ingesta es reciente (< 1-2 horas). La semilla entera masticada libera toda la toxina.\n• Soporte UCI: Fluidoterapia agresiva con cristaloides y coloides para mantener presión arterial y diuresis, transfusiones de concentrados de hematíes y plasma si hay hemorragia severa, alcalinización urinaria y soporte vasopresor.",
                vetTreatment = "• Perros: Fluidoterapia IV masiva e intensiva (Ringer Lactato + hidroxietilalmidón) para prevenir shock hipovolémico.\n• Protectores gástricos (Sucralfato, Omeprazol IV), antieméticos de acción central (Maropitant) y analgesia opioide pura (Metadona/Fentanilo).",
                contraindications = "⚠️ NUNCA subestimar una ingesta de semillas de ricino o abrus aunque el paciente esté asintomático en las primeras horas; la latencia clínica es típica antes del colapso de órganos."
            ),
            AntidoteInfo(
                id = "oxalatos",
                title = "6. Oxalatos de Calcio Corrosivos e Insolubles",
                plants = "Dieffenbachia (Lotería), Monstera deliciosa (Costilla de Adán), Philodendron, Zantedeschia (Cala), Arum maculatum",
                toxins = "Cristales de oxalato cálcico en rafidios (agujas microscópicas disparadas por células idioblastos) + enzimas proteolíticas",
                antidoteName = "CORTICOIDES IV + CONTROL DE VÍA AÉREA (Intubación precoz)",
                symptoms = "Dolor urente e insoportable inmediato en boca, lengua y faringe, edema severo (hinchazón masiva) de glotis, labios y lengua, sialorrea masiva (babear extremo), afonía, imposibilidad para tragar (disfagia), peligro inminente de asfixia por obstrucción de la vía aérea.",
                humanTreatment = "• Tratamiento de urgencia: CORTICOSTEROIDES IV (Metilprednisolona 1-2 mg/kg o Dexametasona) + Antihistamínicos IV (Dexclorfeniramina) para frenar y reducir el edema angioneurótico y de glotis.\n• Vía aérea: Si existe estridor, disnea o hinchazón faríngea progresiva, realizar INTUBACIÓN ENDOTRAQUEAL PRECOZ o traqueotomía antes de que el edema impida el paso del tubo.\n• Sintomático: Enjuagues fríos, hielo triturado, anestésicos tópicos orales (Lidocaína viscosa).",
                vetTreatment = "• Perros/Gatos: Lavado suave de la boca con agua fría o leche tibia. Administración inmediata de Dexametasona 0.2-0.5 mg/kg IV/SC y Difenhidramina 1-2 mg/kg IM.\n• Oxigenoterapia y preparación para intubación o traqueotomía de urgencia si el perro/gato ronca o presenta dificultad respiratoria.",
                contraindications = "⚠️ NUNCA INDUCIR EL VÓMITO NI REALIZAR LAVADO GÁSTRICO: El paso de nuevo por el esófago y la garganta empeorará el edema y precipitará la obstrucción total de la respiración."
            ),
            AntidoteInfo(
                id = "amatoxinas",
                title = "7. Toxinas Hepáticas Tardías / Amatoxinas",
                plants = "Amanita phalloides (Oronja verde), Amanita virosa, Galerina marginata, Lepiota brunneoincarnata",
                toxins = "Alfa-amanitina, beta-amanitina (Inhibición de la ARN polimerasa II celular, necrosis hepática masiva)",
                antidoteName = "SILIBININA IV (Legalon SIL) + N-ACETILCISTEÍNA",
                symptoms = "1ª Fase latencia (6-24 h): Asintomático. 2ª Fase coleriforme (24-48 h): Vómitos intensos, diarrea coleriforme profusa, deshidratación y cólicos. 3ª Fase de falsa mejoría (día 2-3). 4ª Fase hepatorrenal (día 3-5): Ictericia, coagulopatía grave, encefalopatía hepática, coma y muerte.",
                humanTreatment = "• Antídoto específico de elección: SILIBININA IV (Legalon SIL) 20-50 mg/kg/día en infusión continua durante 3 a 4 días. Bloquea el transporte de la amatoxina al interior del hepatocito.\n• Antídoto alternativo/coadyuvante: N-ACETILCISTEÍNA (NAC) a dosis de protocolo paracetamol (150 mg/kg en 1h, luego 50 mg/kg en 4h y 100 mg/kg en 16h).\n• Eliminación: Carbón activado seriado cada 4 horas (para interrumir el ciclo enterohepático de la toxina) + Penicilina G sódica a altas dosis (1-1.5 millones UI/kg/día).\n• Trasplante: Evaluación urgente de criterios de trasplante hepático (Criterios de Kings College o Clichy).",
                vetTreatment = "• Perros: Silibinina IV o extracto de Cardo Mariano / SAMe a altas dosis + N-Acetilcisteína 140 mg/kg IV diluida.\n• Fluidoterapia intensiva, plasma fresco congelado para la coagulopatía, lactulosa y neomicina para la encefalopatía hepática.",
                contraindications = "⚠️ NO DAR ALTA MÉDICA DURANTE LA FASE DE FALSA MEJORÍA (día 2): La desaparición de los vómitos coincide con el inicio de la destrucción masiva del parénquima hepático."
            ),
            AntidoteInfo(
                id = "cicuta",
                title = "8. Toxinas Coniínas y Alcaloides Piridínicos",
                plants = "Conium maculatum (Cicuta mayor), Nicotiana glauca (Gandul / Tabaco moro), Lobelia",
                toxins = "Coniína, gamma-coniceína (Acción tipo nicotina y curare sobre receptores nicotínicos musculares)",
                antidoteName = "SOPORTE VENTILATORIO MECÁNICO (Intubación precoz)",
                symptoms = "Sensación de ardor en garganta, debilidad en extremidades inferiores que asciende progresivamente (parálisis motora ascendente), ataxia, ptosis palpebral, mente completamente lúcida, parálisis de los músculos respiratorios y del diafragma, muerte por asfixia consciente.",
                humanTreatment = "• No existe antídoto químico específico.\n• Tratamiento vital: VENTILACIÓN MECÁNICA ASISTIDA (Intubación endotraqueal precoz) en cuanto se observe debilidad en extremidades o disminución de la capacidad vital respiratoria. La toxina se elimina por vía renal en 24-48 horas; si se mantiene la respiración artificial, la recuperación muscular es completa sin secuelas.\n• Descontaminación: Lavado gástrico y carbón activado si la ingesta es muy reciente (< 1 hora).",
                vetTreatment = "• Perros/Gatos/Caballos: Soporte ventilatorio con oxígeno 100% o intubación y ventilación de presión positiva en clínica veterinaria.\n• Control de convulsiones iniciales con Diazepam IV y fluidoterapia para acelerar la excreción urinaria.",
                contraindications = "⚠️ No administrar estimulantes centrales ni respiratorios (ej. analépticos); aumentan el consumo de oxígeno del miocardio y precipitan arritmias o convulsiones fatales."
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("🏥 Radar de Urgencias y Antídotos", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Navegación 24h y Guía Hospitalaria", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D47A1),
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF0A192F))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A8A)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📍", fontSize = 28.sp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Localizador GPS de Urgencias 24h", fontWeight = FontWeight.Black, color = Color.White, fontSize = 17.sp)
                                Text("1 clic abre tu mapa con la ruta más rápida al centro", fontSize = 12.sp, color = Color(0xFF90CAF9))
                            }
                        }
                    }
                }
            }

            // ── BOTONES DE BUSQUEDA EN MAPA ──
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GeoNavCard(
                        modifier = Modifier.weight(1f),
                        icon = "🏥",
                        title = "Hospital Urgencias 24h",
                        subtitle = "Hospitales médicos",
                        gradient = Brush.horizontalGradient(listOf(Color(0xFFB71C1C), Color(0xFFD32F2F))),
                        onClick = { launchGeoSearch("hospital urgencias 24 horas") }
                    )
                    GeoNavCard(
                        modifier = Modifier.weight(1f),
                        icon = "🐶",
                        title = "Veterinario Urgencia 24h",
                        subtitle = "Clínicas animales",
                        gradient = Brush.horizontalGradient(listOf(Color(0xFF4A148C), Color(0xFF7B1FA2))),
                        onClick = { launchGeoSearch("clinica veterinaria urgencias 24 horas") }
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GeoNavCard(
                        modifier = Modifier.weight(1f),
                        icon = "🩺",
                        title = "Ambulatorio / Salud",
                        subtitle = "Centros médicos",
                        gradient = Brush.horizontalGradient(listOf(Color(0xFF1B5E20), Color(0xFF2E7D32))),
                        onClick = { launchGeoSearch("centro de salud urgencias") }
                    )
                    GeoNavCard(
                        modifier = Modifier.weight(1f),
                        icon = "💊",
                        title = "Farmacia de Guardia",
                        subtitle = "Abiertas 24 horas",
                        gradient = Brush.horizontalGradient(listOf(Color(0xFFE65100), Color(0xFFF57C00))),
                        onClick = { launchGeoSearch("farmacia de guardia 24 horas") }
                    )
                }
            }

            item {
                HorizontalDivider(color = Color.White.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("💉", fontSize = 26.sp)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Guía Clínica de Antídotos y Tratamientos", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                        Text("Muestra este protocolo al médico o veterinario en urgencias", fontSize = 12.sp, color = Color(0xFFA5D6A7))
                    }
                }
            }

            items(antidotes) { antidote ->
                AntidoteDetailCard(antidote)
            }

            item {
                Surface(
                    color = Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Text(
                        "⚠️ AVISO MÉDICO: Los tratamientos y antídoto aquí descritos son protocolos hospitalarios oficiales de toxicología clínica. Su administración debe realizarse estrictamente bajo supervisión médica o veterinaria intensiva con monitorización continua.",
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GeoNavCard(
    modifier: Modifier = Modifier,
    icon: String,
    title: String,
    subtitle: String,
    gradient: Brush,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(96.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = Color.Transparent,
        shadowElevation = 6.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 32.sp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 16.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(subtitle, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🧭 Navegar GPS", color = Color.Yellow, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun AntidoteDetailCard(info: AntidoteInfo) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF132338)),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (expanded) Color(0xFF64B5F6) else Color.White.copy(alpha = 0.1f))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(info.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                    Spacer(Modifier.height(3.dp))
                    Text("🌿 Especies: " + info.plants, fontSize = 12.sp, color = Color(0xFF90CAF9), fontWeight = FontWeight.Medium)
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Color(0xFF64B5F6),
                    modifier = Modifier.size(24.dp)
                )
            }

            Surface(
                color = Color(0xFF1B5E20).copy(alpha = 0.4f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CAF50))
            ) {
                Row(Modifier.padding(8.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("💉", fontSize = 18.sp)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("ANTÍDOTO / TRATAMIENTO DE ELECCIÓN:", fontSize = 10.sp, color = Color(0xFFA5D6A7), fontWeight = FontWeight.Bold)
                        Text(info.antidoteName, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Black)
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

                    Column {
                        Text("🔬 TOXINAS Y MECANISMO:", fontWeight = FontWeight.Bold, color = Color(0xFFFFB74D), fontSize = 12.sp)
                        Text(info.toxins, color = Color.White, fontSize = 13.sp)
                    }

                    Column {
                        Text("⚠️ SÍNTOMAS CLÍNICOS CRÍTICOS:", fontWeight = FontWeight.Bold, color = Color(0xFFFF8A80), fontSize = 12.sp)
                        Text(info.symptoms, color = Color.LightGray, fontSize = 13.sp, lineHeight = 17.sp)
                    }

                    Column {
                        Text("🏥 PROTOCOLO HOSPITALARIO HUMANO:", fontWeight = FontWeight.Bold, color = Color(0xFF64B5F6), fontSize = 12.sp)
                        Text(info.humanTreatment, color = Color.White, fontSize = 13.sp, lineHeight = 18.sp)
                    }

                    Column {
                        Text("🐶 PROTOCOLO VETERINARIO DE URGENCIAS:", fontWeight = FontWeight.Bold, color = Color(0xFFBA68C8), fontSize = 12.sp)
                        Text(info.vetTreatment, color = Color.White, fontSize = 13.sp, lineHeight = 18.sp)
                    }

                    Surface(
                        color = Color(0xFFB71C1C).copy(alpha = 0.25f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE53935)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(10.dp)) {
                            Text("🚫 CONTRAINDICACIONES Y ADVERTENCIAS CRÍTICAS:", fontWeight = FontWeight.Bold, color = Color(0xFFFF8A80), fontSize = 11.sp)
                            Text(info.contraindications, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val shareText = "🏥 GUÍA DE ANTÍDOTOS FLORASAFE\n" + info.title + "\n\n🌿 Plantas: " + info.plants + "\n🔬 Toxinas: " + info.toxins + "\n💉 ANTÍDOTO: " + info.antidoteName + "\n\n🏥 Protocolo Humano:\n" + info.humanTreatment + "\n\n🐶 Protocolo Veterinario:\n" + info.vetTreatment + "\n\n🚫 Contraindicaciones:\n" + info.contraindications
                            val intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(intent, "Compartir protocolo médico"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF90CAF9))
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Compartir protocolo con sanitario / veterinario", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
