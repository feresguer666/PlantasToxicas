package com.toxicplants.database.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.ui.theme.carbonEffectSubtle
import com.toxicplants.database.ui.viewmodel.PlantViewModel

private object GreenScale {
    val bg0    = Color(0xFF060F07)
    val bg1    = Color(0xFF0A1A0C)
    val bg2    = Color(0xFF0D2410)
    val topBar = Color(0xFF0D3311)
}
private val brownStart = Color(0xFF3E1C00)
private val brownEnd   = Color(0xFF8D6E63)
private val redStart   = Color(0xFF5C0000)
private val redEnd     = Color(0xFFC62828)

@Suppress("UNUSED_PARAMETER")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: PlantViewModel,
    onNavigateToList: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToEmergency: () -> Unit,
    onNavigateToMyths: () -> Unit,
    onNavigateToAssistant: () -> Unit = {},
    onNavigateToRiskCalculator: () -> Unit = {},
    onNavigateToLethalDoseCalculator: () -> Unit = {},
    onNavigateToOnlineDatabases: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToDownloadImages: () -> Unit,
    onNavigateToNewPlant: () -> Unit,
    onNavigateToCamera: () -> Unit,
    onNavigateToNatureIdentify: () -> Unit = {},
    onNavigateToPhytochemistry: () -> Unit,
    onNavigateToPsychotropicPlants: () -> Unit = {},
    onNavigateToExtractionMethods: () -> Unit = {},
    onNavigateToChemicalReagents: () -> Unit = {},
    onNavigateToMushrooms: () -> Unit = {},
    onNavigateToLichens: () -> Unit = {},
    onNavigateToSearchBySymptoms: () -> Unit,
    onNavigateToAR: () -> Unit,
    onNavigateToBerries: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToNotes: () -> Unit = {},
    onNavigateToFamilies: () -> Unit = {},
    onNavigateToPetSafety: () -> Unit = {},
    onNavigateToChildSafety: () -> Unit = {},
    onNavigateToLivestockSafety: () -> Unit = {},
    onNavigateToConfusable: () -> Unit = {},
    onNavigateToMap: () -> Unit = {},
    onNavigateToColorSearch: () -> Unit = {},
    onNavigateToGlossary: () -> Unit = {},
    onNavigateToToxicParts: () -> Unit = {},
    onNavigateToIntoxication: () -> Unit = {},
    onNavigateToGlobalSearch: (String) -> Unit = {},
    onNavigateToCalendar: () -> Unit = {},
    onNavigateToGBIF: () -> Unit = {},
    onPlantClick: (PlantEntity) -> Unit,
) {
    val context = LocalContext.current
    val allPlants   by viewModel.allPlants.observeAsState(emptyList())
    val allFamilies by viewModel.allFamilies.observeAsState(emptyList())

    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull() ?: ""
            if (spoken.isNotBlank()) {
                onNavigateToGlobalSearch(spoken)
            }
        }
    }

    val mortalCount     = allPlants.count { it.toxicityLevel == "Mortal" }
    val altoRiesgoCount = allPlants.count { it.toxicityLevel == "Alto" }

    var showSearchDialog   by remember { mutableStateOf(false) }
    var showCompanionSafetyDialog by remember { mutableStateOf(false) }
    var showIdentifyDialog by remember { mutableStateOf(false) }
    var showEmergencyDialog by remember { mutableStateOf(false) }
    var showCalculatorsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Surface(color = GreenScale.topBar) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "☠️ Plantas   Venenosas",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 24.sp,
                        color      = Color.Red,
                        modifier   = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 1.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Filled.Settings, "Ajustes", modifier = Modifier.size(26.dp), tint = Color.Blue)
                        }
                        IconButton(onClick = onNavigateToMap) {
                            Icon(Icons.Filled.Map, "Mapa", modifier = Modifier.size(26.dp), tint = Color.Yellow)
                        }
                        IconButton(onClick = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "🌿 Dime el nombre de la planta…")
                            }
                            try { voiceLauncher.launch(intent) } catch (_: Exception) { }
                        }) {
                            Icon(Icons.Filled.Mic, "Búsqueda por voz", modifier = Modifier.size(28.dp), tint = Color.Red)
                        }
                        IconButton(onClick = onNavigateToNotes) {
                            Icon(Icons.AutoMirrored.Filled.Notes, "Notes", modifier = Modifier.size(26.dp), tint = Color.Yellow)
                        }
                        IconButton(onClick = onNavigateToNewPlant) {
                            Text("+", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.Blue)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(listOf(GreenScale.bg0, GreenScale.bg1, GreenScale.bg2))
                ),
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(80.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.horizontalGradient(listOf(redStart, redEnd)))
                            .carbonFiber()
                            .clickable { showEmergencyDialog = true }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Warning, null, tint = Color.White, modifier = Modifier.size(30.dp))
                            Column {
                                Text("EMERGENCIAS / BD", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 21.sp, maxLines = 1, softWrap = false)
                                Text("Toxicológica y Recursos Externos", color = Color.Gray.copy(alpha = 0.92f), fontSize = 13.sp, maxLines = 1, softWrap = false)
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .width(86.dp)
                            .height(80.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.horizontalGradient(listOf(Color(0xFF6A1B9A), Color(0xFFAB47BC))))
                            .carbonFiber()
                            .clickable { showCalculatorsDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🧮", fontSize = 26.sp)
                            Text("Calculadoras", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(82.dp)
                            .height(80.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF1565C0), Color(0xFF42A5F5))
                                )
                            )
                            .carbonFiber()
                            .clickable { onNavigateToGlossary() },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📖", fontSize = 26.sp)
                            Text(
                                "Glosario",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(80.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.horizontalGradient(listOf(brownStart, brownEnd))
                            )
                            .carbonFiber()
                            .clickable { onNavigateToMyths() }
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.MenuBook,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Column {
                                Text(
                                    "MITOS",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    "Leyendas y Curiosidades",
                                    color = Color.Gray.copy(alpha = 0.92f),
                                    fontSize = 6.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .width(86.dp)
                            .height(80.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF880E4F), Color(0xFFE91E63))
                                )
                            )
                            .carbonFiber()
                            .clickable { onNavigateToCalendar() },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📅", fontSize = 26.sp)
                            Text(
                                "Calendario",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            item {
                StatsRow(
                    totalPlants     = allPlants.size,
                    mortalCount     = mortalCount,
                    altoRiesgoCount = altoRiesgoCount,
                    familiesCount   = allFamilies.size
                )
            }

            item {
                val composition by rememberLottieComposition(LottieCompositionSpec.Asset("molecule.json"))
                val progress by animateLottieCompositionAsState(composition = composition, iterations = LottieConstants.IterateForever)
                LottieAnimation(composition = composition, progress = { progress }, modifier = Modifier.fillMaxWidth().height(130.dp))
            }

            item {
                NavigationGrid(
                    onNavigateToList           = onNavigateToList,
                    onNavigateToCategories     = onNavigateToCategories,
                    onNavigateToPhytochemistry = onNavigateToPhytochemistry,
                    onNavigateToPsychotropicPlants = onNavigateToPsychotropicPlants,
                    onNavigateToExtractionMethods = onNavigateToExtractionMethods,
                    onNavigateToChemicalReagents = onNavigateToChemicalReagents,
                    onNavigateToMushrooms       = onNavigateToMushrooms,
                    onNavigateToLichens         = onNavigateToLichens,
                    onNavigateToBerries        = onNavigateToBerries,
                    onNavigateToSearch         = { showSearchDialog = true },
                    onNavigateToIdentify       = { showIdentifyDialog = true },
                    onNavigateToConfusable     = onNavigateToConfusable,
                    onNavigateToIntoxication   = onNavigateToIntoxication,
                    onNavigateToGlobalSearch   = { onNavigateToGlobalSearch("") },
                    onNavigateToAssistant      = onNavigateToAssistant,
                    onNavigateToGBIF          = onNavigateToGBIF
                )
            }
        }
    }

    if (showSearchDialog) {
        SearchTypeDialog(
            onDismiss            = { showSearchDialog = false },
            onSearchByName       = { showSearchDialog = false; onNavigateToSearch() },
            onSearchBySymptoms   = { showSearchDialog = false; onNavigateToSearchBySymptoms() },
            onSearchByFamily     = { showSearchDialog = false; onNavigateToFamilies() },
            onSearchByPets       = { showSearchDialog = false; onNavigateToPetSafety() },
            onSearchByChildren   = { showSearchDialog = false; onNavigateToChildSafety() },
            onSearchByLivestock  = { showSearchDialog = false; onNavigateToLivestockSafety() },
            onSearchByCompanions = { showSearchDialog = false; showCompanionSafetyDialog = true },
            onSearchByColor      = { showSearchDialog = false; onNavigateToColorSearch() },
            onSearchByConfusable = { showSearchDialog = false; onNavigateToConfusable() },
            onSearchByToxicParts = { showSearchDialog = false; onNavigateToToxicParts() },
            onSearchByBerries    = { showSearchDialog = false; onNavigateToBerries() },
            onIntoxication       = { showSearchDialog = false; onNavigateToIntoxication() },
            onNavigateToGBIF     = { showSearchDialog = false; onNavigateToGBIF() }
        )
    }

    if (showCompanionSafetyDialog) {
        CompanionSafetyDialog(
            onDismiss = { showCompanionSafetyDialog = false },
            onChildren = { showCompanionSafetyDialog = false; onNavigateToChildSafety() },
            onPets = { showCompanionSafetyDialog = false; onNavigateToPetSafety() },
            onLivestock = { showCompanionSafetyDialog = false; onNavigateToLivestockSafety() }
        )
    }

    if (showCalculatorsDialog) {
        CalculatorsTypeDialog(
            onDismiss          = { showCalculatorsDialog = false },
            onRiskCalculator   = { showCalculatorsDialog = false; onNavigateToRiskCalculator() },
            onLD50Calculator   = { showCalculatorsDialog = false; onNavigateToLethalDoseCalculator() }
        )
    }

    if (showEmergencyDialog) {
        AlertDialog(
            onDismissRequest = { showEmergencyDialog = false },
            title = { Text("Emergencias y Recursos", fontWeight = FontWeight.Bold) },
            text = { Text("Selecciona si tienes una emergencia toxicológica o si deseas consultar las bases de datos externas.") },
            confirmButton = {
                Button(
                    onClick = { showEmergencyDialog = false; onNavigateToEmergency() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C))
                ) {
                    Text("🚨 Emergencia", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEmergencyDialog = false; onNavigateToOnlineDatabases() }) {
                    Text("🌐 Bases externas")
                }
            }
        )
    }

    if (showIdentifyDialog) {
        IdentifyTypeDialog(
            onDismiss          = { showIdentifyDialog = false },
            onIdentifyByCamera = { showIdentifyDialog = false; onNavigateToCamera() },
            onIdentifyByNature = { showIdentifyDialog = false; onNavigateToNatureIdentify() },
            onIdentifyByAR     = { showIdentifyDialog = false; onNavigateToAR() },
            onConfusable       = { showIdentifyDialog = false; onNavigateToConfusable() }
        )
    }
}

fun Modifier.carbonFiber(): Modifier {
    val cLight = Color.White.copy(alpha = 0.12f)
    val cDark  = Color.Black.copy(alpha = 0.40f)
    return this
        .drawBehind {
            val s = 12.dp.toPx()
            var x = 0f
            while (x < size.width + size.height) {
                drawLine(cLight, Offset(x, 0f), Offset(x - size.height, size.height), 2.5f)
                drawLine(cDark, Offset(x + s / 2, 0f), Offset(x + s / 2 - size.height, size.height), 2.5f)
                x += s
            }
        }
        .drawBehind {
            drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.20f), Color.Transparent, Color.Black.copy(alpha = 0.35f))))
        }
}

@Composable
fun BannerCard(
    modifier: Modifier = Modifier,
    gradient: Brush,
    height: Dp,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    val cLight = Color.White.copy(alpha = 0.12f)
    val cDark  = Color.Black.copy(alpha = 0.40f)

    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(14.dp))
            .background(gradient)
            .drawBehind {
                val s = 12.dp.toPx()
                var x = 0f
                while (x < size.width + size.height) {
                    drawLine(cLight, Offset(x, 0f), Offset(x - size.height, size.height), 2.5f)
                    drawLine(cDark,  Offset(x + s / 2, 0f), Offset(x + s / 2 - size.height, size.height), 2.5f)
                    x += s
                }
            }
            .drawBehind {
                drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.20f), Color.Transparent, Color.Black.copy(alpha = 0.35f))))
            }
            .clickable { onClick() }
    ) {
        Row(
            modifier          = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            content           = content
        )
    }
}

@Composable
fun GradientNavButton(
    modifier: Modifier,
    icon: String,
    text: String,
    gradient: Brush,
    height: Dp = 100.dp,
    onClick: () -> Unit
) {
    val cLight = Color.White.copy(alpha = 0.12f)
    val cDark  = Color.Black.copy(alpha = 0.40f)

    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(14.dp))
            .background(gradient)
            .drawBehind {
                val s = 12.dp.toPx()
                var x = 0f
                while (x < size.width + size.height) {
                    drawLine(cLight, Offset(x, 0f), Offset(x - size.height, size.height), 2.5f)
                    drawLine(cDark,  Offset(x + s / 2, 0f), Offset(x + s / 2 - size.height, size.height), 2.5f)
                    x += s
                }
            }
            .drawBehind {
                drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent, Color.Black.copy(alpha = 0.40f))))
            }
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier            = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(icon, fontSize = 30.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                text       = text,
                color = Color.White,
                fontSize   = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines   = 2,
                textAlign  = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun NavigationGrid(
    onNavigateToList: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToPhytochemistry: () -> Unit,
    onNavigateToPsychotropicPlants: () -> Unit = {},
    onNavigateToExtractionMethods: () -> Unit = {},
    onNavigateToChemicalReagents: () -> Unit = {},
    onNavigateToMushrooms: () -> Unit = {},
    onNavigateToLichens: () -> Unit = {},
    onNavigateToBerries: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToIdentify: () -> Unit,
    onNavigateToConfusable: () -> Unit = {},
    onNavigateToIntoxication: () -> Unit = {},
    onNavigateToGlobalSearch: () -> Unit = {},
    onNavigateToAssistant: () -> Unit = {},
    onNavigateToGBIF: () -> Unit = {}
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier.weight(1f).height(60.dp).clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF0D47A1), Color(0xFF1976D2), Color(0xFF0D47A1))))
                    .carbonFiber().clickable { onNavigateToGlobalSearch() },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔍", fontSize = 18.sp)
                    Spacer(Modifier.width(4.dp))
                    Text("Búsqueda Global", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
            Box(
                modifier = Modifier.height(60.dp).width(72.dp).clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF42A5F5), Color(0xFF64B5F6), Color(0xFF42A5F5))))
                    .clickable { onNavigateToAssistant() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🤖", fontSize = 24.sp)
                    Text("IA", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
            }
        }

        var showFitoDialog by remember { mutableStateOf(false) }
        var showBusquedaDialog by remember { mutableStateOf(false) }
        var showBotanicaDialog by remember { mutableStateOf(false) }
        var showQuimicaDialog by remember { mutableStateOf(false) }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GradientNavButton(modifier = Modifier.weight(1f), icon = "☠️☠️☠️", text = "Fito-\ntoxicología", gradient = Brush.linearGradient(listOf(Color(0xFF0A2E0E), Color(0xFF1B5E20), Color(0xFF0A2E0E))), height = 130.dp, onClick = { showFitoDialog = true })
            GradientNavButton(modifier = Modifier.weight(1f), icon = "🔍🔍🔍", text = "Búsqueda \nReconocer", gradient = Brush.linearGradient(listOf(Color(0xFF1B5E20), Color(0xFF43A047), Color(0xFF1B5E20))), height = 130.dp, onClick = { showBusquedaDialog = true })
        }

        if (showFitoDialog) {
            AlertDialog(
                onDismissRequest = { showFitoDialog = false },
                title = { Text("☠️ Fitotoxicología", fontWeight = FontWeight.Bold) },
                text  = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        DialogOptionCard(gradient = Brush.horizontalGradient(listOf(Color(0xFF1B5E20), Color(0xFF43A047))), icon = "🌿", title = "Botánica", subtitle = "Líquenes, setas, plantas y categorías", onClick = { showFitoDialog = false; showBotanicaDialog = true })
                        DialogOptionCard(gradient = Brush.horizontalGradient(listOf(Color(0xFF263238), Color(0xFF7B1FA2))), icon = "🧠", title = "Plantas psicotrópicas", subtitle = "Alucinógenos, IMAO, depresores, estimulantes y tropánicos", onClick = { showFitoDialog = false; onNavigateToPsychotropicPlants() })
                        DialogOptionCard(gradient = Brush.horizontalGradient(listOf(Color(0xFF2D1B69), Color(0xFF6A1B9A))), icon = "🔬", title = "Química", subtitle = "Fitoquímica: compuestos tóxicos y alcaloides", onClick = { showFitoDialog = false; showQuimicaDialog = true })
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = { showFitoDialog = false }) { Text("Cancelar") } }
            )
        }

        if (showBotanicaDialog) {
            AlertDialog(
                onDismissRequest = { showBotanicaDialog = false },
                title = { Text("🌿 Botánica", fontWeight = FontWeight.Bold) },
                text  = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        DialogOptionCard(gradient = Brush.horizontalGradient(listOf(Color(0xFF37474F), Color(0xFF607D8B))), icon = "🪨", title = "Líquenes tóxicos", subtitle = "Compuestos liquénicos y efectos", onClick  = { showBotanicaDialog = false; onNavigateToLichens() })
                        DialogOptionCard(gradient = Brush.horizontalGradient(listOf(Color(0xFF4E342E), Color(0xFF795548))), icon = "🍄", title = "Setas tóxicas", subtitle = "Catálogo micológico y síndromes", onClick  = { showBotanicaDialog = false; onNavigateToMushrooms() })
                        DialogOptionCard(gradient = Brush.horizontalGradient(listOf(Color(0xFF1B3A1E), Color(0xFF2E5232))), icon = "🌿", title = "Plantas", subtitle = "Catálogo completo", onClick  = { showBotanicaDialog = false; onNavigateToList() })
                        DialogOptionCard(gradient = Brush.horizontalGradient(listOf(Color(0xFF1E4423), Color(0xFF2A5C30))), icon = "🗂️", title = "Categorías", subtitle = "Silvestre, jardín, interior…", onClick  = { showBotanicaDialog = false; onNavigateToCategories() })
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = { showBotanicaDialog = false }) { Text("Cancelar") } }
            )
        }

        if (showQuimicaDialog) {
            AlertDialog(
                onDismissRequest = { showQuimicaDialog = false },
                title = { Text("🔬 Química", fontWeight = FontWeight.Bold) },
                text  = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        DialogOptionCard(gradient = Brush.horizontalGradient(listOf(Color(0xFF2D1B69), Color(0xFF6A1B9A))), icon = "🔬", title = "Fitoquímica", subtitle = "Compuestos tóxicos y alcaloides", onClick  = { showQuimicaDialog = false; onNavigateToPhytochemistry() })
                        DialogOptionCard(gradient = Brush.horizontalGradient(listOf(Color(0xFF4A148C), Color(0xFF7B1FA2))), icon = "⚗️", title = "Métodos de extracción", subtitle = "Alcaloides, glucósidos, saponinas…", onClick  = { showQuimicaDialog = false; onNavigateToExtractionMethods() })
                        DialogOptionCard(gradient = Brush.horizontalGradient(listOf(Color(0xFF880E4F), Color(0xFFC2185B))), icon = "🧫", title = "Reactivos", subtitle = "Mayer, Dragendorff, Marquis, Froehde…", onClick  = { showQuimicaDialog = false; onNavigateToChemicalReagents() })
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = { showQuimicaDialog = false }) { Text("Cancelar") } }
            )
        }

        if (showBusquedaDialog) {
            AlertDialog(
                onDismissRequest = { showBusquedaDialog = false },
                title = { Text("🔍 Búsqueda y Reconocimiento", fontWeight = FontWeight.Bold) },
                text  = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        DialogOptionCard(gradient = Brush.horizontalGradient(listOf(Color(0xFF4CAF50), Color(0xFF60C264))), icon = "🔍", title = "Buscar", subtitle = "Por nombre, síntomas, color, mascotas…", onClick  = { showBusquedaDialog = false; onNavigateToSearch() })
                        DialogOptionCard(gradient = Brush.horizontalGradient(listOf(Color(0xFFB71C1C), Color(0xFFE65100))), icon = "☠️", title = "Intoxicación", subtitle = "Síntomas, síndromes y primeros pasos", onClick  = { showBusquedaDialog = false; onNavigateToIntoxication() })
                        DialogOptionCard(gradient = Brush.horizontalGradient(listOf(Color(0xFF1B5E20), Color(0xFF388E3C))), icon = "📷", title = "Identificar por foto", subtitle = "Cámara o galería con Pl@ntNet", onClick  = { showBusquedaDialog = false; onNavigateToIdentify() })
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = { showBusquedaDialog = false }) { Text("Cancelar") } }
            )
        }
    }
}

@Composable
fun StatsRow(totalPlants: Int, mortalCount: Int, altoRiesgoCount: Int, familiesCount: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        StatCard("🌿", totalPlants.toString(),     "Plantas",     Color(0xFF4CAF50))
        StatCard("☠️", mortalCount.toString(),     "Mortales",    Color(0xFFEF5350))
        StatCard("⚠️", altoRiesgoCount.toString(), "Alto riesgo", Color(0xFFFFA726))
        StatCard("📋", familiesCount.toString(),   "Familias",    Color(0xFF81C784))
    }
}

@Composable
fun StatCard(emoji: String, value: String, label: String, color: Color) {
    Box(modifier = Modifier.size(width = 80.dp, height = 80.dp).clip(RoundedCornerShape(16.dp)).background(color.copy(alpha = 0.13f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 20.sp)
            Text(value, fontWeight = FontWeight.Bold, color = color, fontSize = 17.sp)
            Text(label, fontSize = 10.sp, color = Color.LightGray)
        }
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun SearchTypeDialog(
    onDismiss: () -> Unit,
    onSearchByName: () -> Unit,
    onSearchBySymptoms: () -> Unit,
    onSearchByFamily: () -> Unit,
    onSearchByPets: () -> Unit,
    onSearchByChildren: () -> Unit,
    onSearchByLivestock: () -> Unit,
    onSearchByCompanions: () -> Unit = {},
    onSearchByColor: () -> Unit = {},
    onSearchByConfusable: () -> Unit = {},
    onSearchByToxicParts: () -> Unit = {},
    onSearchByBerries: () -> Unit = {},
    onIntoxication: () -> Unit = {},
    onNavigateToGBIF: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🔍 ¿Cómo quieres buscar?", fontWeight = FontWeight.Bold) },
        text  = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SearchOptionSquare(modifier = Modifier.weight(1f), gradient = Brush.verticalGradient(listOf(Color(0xFF1565C0), Color(0xFF1976D2))), icon = "📚", title = "Familias", onClick = onSearchByFamily)
                    SearchOptionSquare(modifier = Modifier.weight(1f), gradient = Brush.verticalGradient(listOf(Color(0xFF1B5E20), Color(0xFF2E7D32))), icon = "🔤", title = "Nombres", onClick = onSearchByName)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SearchOptionSquare(modifier = Modifier.weight(1f), gradient = Brush.verticalGradient(listOf(Color(0xFF880E4F), Color(0xFFAD1457))), icon = "🌸", title = "Colores", onClick = onSearchByColor)
                    SearchOptionSquare(modifier = Modifier.weight(1f), gradient = Brush.verticalGradient(listOf(Color(0xFFBF360C), Color(0xFFD84315))), icon = "⚠️", title = "Confundibles", onClick = onSearchByConfusable)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SearchOptionSquare(modifier = Modifier.weight(1f), gradient = Brush.verticalGradient(listOf(Color(0xFF338034), Color(0xFF4A9E4C))), icon = "🫐", title = "Bayas", onClick = onSearchByBerries)
                    SearchOptionSquare(modifier = Modifier.weight(1f), gradient = Brush.verticalGradient(listOf(Color(0xFF4A148C), Color(0xFF6A1B9A))), icon = "☠️", title = "Parte tóxica", onClick = onSearchByToxicParts)
                }

                // ── NUEVA FILA: GBIF ───────────────────────────────────
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SearchOptionSquare(
                        modifier = Modifier.weight(1f),
                        gradient = Brush.verticalGradient(listOf(Color(0xFF0D47A1), Color(0xFF1565C0))),
                        icon = "🌍",
                        title = "GBIF",
                        onClick = {
                            onDismiss()
                            onNavigateToGBIF()
                        }
                    )
                    SearchOptionSquare(
                        modifier = Modifier.weight(1f),
                        gradient = Brush.verticalGradient(listOf(Color(0xFF00695C), Color(0xFF00897B))),
                        icon = "📱",
                        title = "Local",
                        onClick = {
                            onDismiss()
                            onSearchByName()
                        }
                    )
                }

            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun SearchOptionSquare(modifier: Modifier = Modifier, gradient: Brush, icon: String, title: String, onClick: () -> Unit) {
    Box(modifier = modifier.height(82.dp).clip(RoundedCornerShape(14.dp)).background(gradient).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 26.sp)
            Spacer(Modifier.height(4.dp))
            Text(text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}

@Composable
private fun SearchWideOptionCompact(
    gradient: Brush,
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(gradient)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 22.sp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                Text(subtitle, color = Color.White.copy(alpha = 0.82f), fontSize = 10.sp, maxLines = 1)
            }
            Text("›", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CompanionSafetyDialog(
    onDismiss: () -> Unit,
    onChildren: () -> Unit,
    onPets: () -> Unit,
    onLivestock: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("👶🐾🐄 Niños y compañía", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DialogOptionCard(
                    gradient = Brush.horizontalGradient(listOf(Color(0xFFE65100), Color(0xFFF57C00))),
                    icon = "👶",
                    title = "Niños",
                    subtitle = "Riesgos domésticos y plantas peligrosas para menores",
                    onClick = onChildren
                )
                DialogOptionCard(
                    gradient = Brush.horizontalGradient(listOf(Color(0xFF4A148C), Color(0xFF6A1B9A))),
                    icon = "🐾",
                    title = "Mascotas",
                    subtitle = "Perros, gatos y animales de compañía",
                    onClick = onPets
                )
                DialogOptionCard(
                    gradient = Brush.horizontalGradient(listOf(Color(0xFF4E342E), Color(0xFF6D4C41))),
                    icon = "🐄",
                    title = "Ganado",
                    subtitle = "Caballos, vacas, ovejas, cabras y otros animales",
                    onClick = onLivestock
                )
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun CalculatorsTypeDialog(
    onDismiss: () -> Unit,
    onRiskCalculator: () -> Unit,
    onLD50Calculator: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🧮 Calculadoras", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DialogOptionCard(
                    gradient = Brush.horizontalGradient(listOf(Color(0xFF6A1B9A), Color(0xFFAB47BC))),
                    icon = "⚠️",
                    title = "Riesgos",
                    subtitle = "Calculadora de evaluación de riesgo orientativa",
                    onClick = onRiskCalculator
                )
                DialogOptionCard(
                    gradient = Brush.horizontalGradient(listOf(Color(0xFFC2185B), Color(0xFFE91E63))),
                    icon = "⚖️",
                    title = "Dosis letal (LD50)",
                    subtitle = "Cálculo matemático de dosis letal por toxinas",
                    onClick = onLD50Calculator
                )
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun IdentifyTypeDialog(onDismiss: () -> Unit, onIdentifyByCamera: () -> Unit, onIdentifyByNature: () -> Unit = {}, onIdentifyByAR: () -> Unit, onConfusable: () -> Unit = {}) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("📷 ¿Cómo quieres identificar?", fontWeight = FontWeight.Bold) },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DialogOptionCard(gradient = Brush.horizontalGradient(listOf(Color(0xFF1B5E20), Color(0xFF388E3C))), icon = "📷", title = "Identificar plantas", subtitle = "Cámara o galería con Pl@ntNet", onClick  = onIdentifyByCamera)
                DialogOptionCard(gradient = Brush.horizontalGradient(listOf(Color(0xFF4E342E), Color(0xFF795548))), icon = "🍄", title = "Identificar setas y líquenes", subtitle = "Foto orientativa con iNaturalist + catálogo local", onClick  = onIdentifyByNature)
                DialogOptionCard(gradient = Brush.horizontalGradient(listOf(Color(0xFF33691E), Color(0xFF558B2F))), icon = "🎯", title = "AR Detección", subtitle = "Realidad aumentada en tiempo real", onClick  = onIdentifyByAR)
            }
        },
        confirmButton  = {},
        dismissButton  = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun DialogOptionCard(gradient: Brush, icon: String, title: String, subtitle: String, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(gradient).clickable { onClick() }) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 26.sp)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(subtitle, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            }
        }
    }
}