package com.toxicplants.database.ui

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.ui.gbif.GBIFEnrichmentScreen
import com.toxicplants.database.ui.screens.*
import com.toxicplants.database.ui.screens.toxicgenera.GenusDetailScreen
import com.toxicplants.database.ui.screens.toxicgenera.ToxicGeneraScreen
import com.toxicplants.database.ui.theme.ThemeManager
import com.toxicplants.database.ui.theme.ToxicPlantsTheme
import com.toxicplants.database.ui.theme.carbonEffectSubtle
import com.toxicplants.database.ui.viewmodel.CompoundViewModel
import com.toxicplants.database.ui.viewmodel.LichenViewModel
import com.toxicplants.database.ui.viewmodel.MushroomViewModel
import com.toxicplants.database.ui.viewmodel.PlantViewModel
import com.toxicplants.database.ui.viewmodel.PoisonousFamilyViewModel
import com.toxicplants.database.ui.viewmodel.SightingViewModel
import com.toxicplants.database.ui.viewmodel.ToxicCalendarViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.init(this)
        setContent {
            val themeMode by ThemeManager.themeMode.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> systemDark
            }
            ToxicPlantsTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .carbonEffectSubtle(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainApp()
                }
            }
        }
    }
}

@Composable
fun MainApp() {
    val navController = rememberNavController()
    val viewModel: PlantViewModel = viewModel()
    val compoundViewModel: CompoundViewModel = viewModel()

    val poisonousFamilyViewModel: PoisonousFamilyViewModel = viewModel()
    NavHost(navController = navController, startDestination = "home") {

        // ── HOME ─────────────────────────────────────────────────────
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToList = { navController.navigate("plant_list") },
                onNavigateToCategories = { navController.navigate("categories") },
                onNavigateToOrnamentalDanger = { navController.navigate("ornamental_danger") },
                onNavigateToEmergency = { navController.navigate("emergency") },
                onNavigateToMyths = { navController.navigate("myths") },
                onNavigateToAssistant = { navController.navigate("assistant") },
                onNavigateToRiskCalculator = { navController.navigate("risk_calculator") },
                onNavigateToLethalDoseCalculator = { navController.navigate("lethal_dose_calculator") },
                onNavigateToOnlineDatabases = { navController.navigate("online_databases") },
                onNavigateToSearch = { navController.navigate("search") },
                onNavigateToSearchBySymptoms = { navController.navigate("search_symptoms") },
                onNavigateToDownloadImages = { navController.navigate("download_images") },
                onNavigateToNewPlant = { navController.navigate("new_plant") },
                onNavigateToCamera = { navController.navigate("camera_identify") },
                onNavigateToTextScanner = { navController.navigate("text_scanner") },
                onNavigateToNatureIdentify = { navController.navigate("nature_photo_identify") },
                onNavigateToPhytochemistry = { navController.navigate("phytochemistry") },
                onNavigateToPsychotropicPlants = { navController.navigate("psychotropic_plants") },
                onNavigateToExtractionMethods = { navController.navigate("chemical_extraction_methods") },
                onNavigateToChemicalReagents = { navController.navigate("chemical_reagents") },
                onNavigateToHomePhytoTests = { navController.navigate("home_phyto_tests") },
                onNavigateToMushrooms = { navController.navigate("toxic_mushrooms") },
                onNavigateToLichens = { navController.navigate("toxic_lichens") },
                onNavigateToPoisonousFamilies = { navController.navigate("poisonous_families") },
                onNavigateToToxicSpecies = { navController.navigate("toxic_species") },
                onNavigateToToxicGenera = { navController.navigate("toxic_genera") },
                onNavigateToAR = { navController.navigate("ar") },
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToFieldMode = { navController.navigate("field_mode") },
                onNavigateToBerries = { navController.navigate("berries") },
                onNavigateToNotes = { navController.navigate("notes") },
                onNavigateToFamilies = { navController.navigate("family_list") },
                onNavigateToPetSafety = { navController.navigate("pet_safety") },
                onNavigateToChildSafety = { navController.navigate("child_safety") },
                onNavigateToLivestockSafety = { navController.navigate("livestock_safety") },
                onNavigateToConfusable = { navController.navigate("confusable_plants") },
                onNavigateToPlantCompare = { navController.navigate("plant_compare") },
                onNavigateToMap = { navController.navigate("sightings_history") },
                onNavigateToColorSearch = { navController.navigate("color_search") },
                onNavigateToGlossary = { navController.navigate("glossary") },
                onNavigateToToxicParts = { navController.navigate("toxic_parts") },
                onNavigateToIntoxication = { navController.navigate("intoxication") },
                onNavigateToGlobalSearch = { query ->
                    if (query.isBlank()) navController.navigate("global_search")
                    else navController.navigate("global_search/${Uri.encode(query)}")
                },
                onNavigateToMultitarea = { navController.navigate("multitarea") },
                onNavigateToCalendar = { navController.navigate("toxic_calendar") },
                onNavigateToGBIF = { navController.navigate("gbif_enrichment") },
                onPlantClick = { plant ->
                    viewModel.selectPlant(plant)
                    navController.navigate("plant_detail/${plant.id}")
                }
            )
        }

        // ── MODO CAMPO ──────────────────────────────────────────────
        composable("field_mode") {
            FieldModeScreen(
                onBack = { navController.popBackStack() },
                onIdentifyPlants = { navController.navigate("camera_identify") },
                onIdentifyFungiLichens = { navController.navigate("nature_photo_identify") },
                onTextScanner = { navController.navigate("text_scanner") },
                onColorSearch = { navController.navigate("color_search") },
                onSymptomsSearch = { navController.navigate("search_symptoms") },
                onMap = { navController.navigate("sightings_history") },
                onNotes = { navController.navigate("notes") },
                onEmergency = { navController.navigate("emergency") },
                onGlobalSearch = { navController.navigate("global_search") },
                onEmergencyMap = { navController.navigate("emergency_map") }
            )
        }

        // ── GBIF ENRICHMENT ─────────────────────────────────────────
        composable("gbif_enrichment") {
            GBIFEnrichmentScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }





        // ── MULTITAREA: pestañas internas ───────────────────────────
        composable("multitarea") {
            MultitareaScreen(
                plantViewModel = viewModel,
                compoundViewModel = compoundViewModel,
                poisonousFamilyViewModel = poisonousFamilyViewModel,
                onBack = { navController.popBackStack() },
                onPlantClick = { plant ->
                    viewModel.selectPlant(plant)
                    navController.navigate("plant_detail/${plant.id}")
                },
                onCompoundClick = { compound ->
                    navController.navigate("compound_detail/${compound.id}")
                },
                onFamilyClick = { family ->
                    navController.navigate("plants_by_family/${Uri.encode(family)}")
                },
                onIntoxicationClick = { navController.navigate("intoxication") },
                onDichotomousKeysClick = { navController.navigate("dichotomous_keys") }
            )
        }

        // ── ESPECIES TÓXICAS ─────────────────────────────────────────
        composable("toxic_species") {
            ToxicSpeciesScreen(
                plantViewModel = viewModel,
                poisonousFamilyViewModel = poisonousFamilyViewModel,
                onBack = { navController.popBackStack() },
                onPlantClick = { plant ->
                    viewModel.selectPlant(plant)
                    navController.navigate("plant_detail/${plant.id}")
                }
            )
        }

        // ── FAMILIAS VENENOSAS ───────────────────────────────────────
        composable("poisonous_families") {
            PoisonousFamiliesScreen(
                viewModel = poisonousFamilyViewModel,
                onBack = { navController.popBackStack() },
                onFamilyClick = { catalogType, familyName -> navController.navigate("poisonous_family/${Uri.encode(catalogType)}/${Uri.encode(familyName)}") },
                onAddGenus = { catalogType -> navController.navigate("poisonous_genus/${Uri.encode(catalogType)}/0/__new__") }
            )
        }

        composable(
            "poisonous_family/{catalogType}/{familyName}",
            arguments = listOf(
                navArgument("catalogType") { type = NavType.StringType },
                navArgument("familyName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val catalogType = Uri.decode(backStackEntry.arguments?.getString("catalogType") ?: "all")
            val familyName = Uri.decode(backStackEntry.arguments?.getString("familyName") ?: "")
            PoisonousFamilyGeneraScreen(
                familyName = familyName,
                catalogType = catalogType,
                viewModel = poisonousFamilyViewModel,
                plantViewModel = viewModel,
                onBack = { navController.popBackStack() },
                onGenusClick = { genusId -> navController.navigate("poisonous_genus/${Uri.encode(catalogType)}/$genusId/${Uri.encode(familyName)}") },
                onAddGenus = { type, fam -> navController.navigate("poisonous_genus/${Uri.encode(type)}/0/${Uri.encode(fam)}") },
                onPlantClick = { plant ->
                    viewModel.selectPlant(plant)
                    navController.navigate("plant_detail/${plant.id}")
                },
                onCreatePlantFromGenus = { genus ->
                    navController.navigate("new_plant_from_genus/${genus.id}")
                }
            )
        }

        composable(
            "poisonous_genus/{catalogType}/{genusId}/{familyName}",
            arguments = listOf(
                navArgument("catalogType") { type = NavType.StringType },
                navArgument("genusId") { type = NavType.IntType },
                navArgument("familyName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val catalogType = Uri.decode(backStackEntry.arguments?.getString("catalogType") ?: "all")
            val genusId = backStackEntry.arguments?.getInt("genusId") ?: 0
            val familyArg = backStackEntry.arguments?.getString("familyName") ?: "__new__"
            val initialFamilyName = if (familyArg == "__new__") "" else Uri.decode(familyArg)
            PoisonousGenusEditScreen(
                genusId = genusId,
                initialFamilyName = initialFamilyName,
                catalogType = catalogType,
                viewModel = poisonousFamilyViewModel,
                onBack = { navController.popBackStack() },
                onSaved = { savedCatalogType, savedFamilyName ->
                    navController.navigate("poisonous_family/${Uri.encode(savedCatalogType)}/${Uri.encode(savedFamilyName)}") {
                        popUpTo("poisonous_families") { inclusive = false }
                    }
                }
            )
        }

        // ── LISTA DE PLANTAS ─────────────────────────────────────────
        composable("plant_list") {
            PlantListScreen(
                viewModel = viewModel,
                onPlantClick = { plant ->
                    viewModel.selectPlant(plant)
                    navController.navigate("plant_detail/${plant.id}")
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── PLANTAS ORNAMENTALES PELIGROSAS ────────────────────
        composable("ornamental_danger") {
            OrnamentalDangerPlantsScreen(
                viewModel = viewModel,
                onPlantClick = { plant ->
                    viewModel.selectPlant(plant)
                    navController.navigate("plant_detail/${plant.id}")
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── CATEGORÍAS ───────────────────────────────────────────────
        composable("categories") {
            CategoriesScreen(
                viewModel = viewModel,
                onCategoryClick = { categoryName ->
                    navController.navigate("category/${Uri.encode(categoryName)}")
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── EMERGENCIA ───────────────────────────────────────────────
        composable("myths") {
            MythsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onPlantClick = { id ->
                    val plant = viewModel.allPlants.value?.find { it.id == id }
                    if (plant != null) {
                        viewModel.selectPlant(plant)
                        navController.navigate("plant_detail/${plant.id}")
                    }
                }
            )
        }

        composable("emergency") {
            EmergencyScreen(
                viewModel = viewModel,
                onPlantClick = { plant ->
                    viewModel.selectPlant(plant)
                    navController.navigate("plant_detail/${plant.id}")
                },
                onBack = { navController.popBackStack() },
                onNavigateToEmergencyMap = { navController.navigate("emergency_map") }
            )
        }

        composable("emergency_map") {
            EmergencyMapScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // ── ASISTENTE VIRTUAL (IA) ───────────────────────────────────
        composable("assistant") {
            AssistantScreen(
                onBack = { navController.popBackStack() },
                onOpenGlossary = { navController.navigate("glossary") }
            )
        }

        // ── GLOSARIO BOTÁNICO ILUSTRADO ──────────────────────────────
        composable("glossary") {
            GlossaryScreen(onBack = { navController.popBackStack() })
        }

        // ── CALCULADORA DE RIESGO (IA) ───────────────────────────────
        composable("risk_calculator") {
            RiskCalculatorScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // ── CALCULADORA DE DOSIS LETAL (LD50) ────────────────────────
        composable("lethal_dose_calculator") {
            LethalDoseCalculatorScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onPlantClick = { plant ->
                    viewModel.selectPlant(plant)
                    navController.navigate("plant_detail/${plant.id}")
                }
            )
        }

        // ── BÚSQUEDA ─────────────────────────────────────────────────
        composable("search") {
            SearchScreen(
                viewModel = viewModel,
                onPlantClick = { plant ->
                    viewModel.selectPlant(plant)
                    navController.navigate("plant_detail/${plant.id}")
                },
                onBack = { navController.popBackStack() },
                onIntoxicationClick = { navController.navigate("intoxication") },
                onDichotomousKeysClick = { navController.navigate("dichotomous_keys") }
            )
        }

        // ── CLAVES DICOTÓMICAS: ÍNDICE ───────────────────────────────
        composable("dichotomous_keys") {
            val keyViewModel: com.toxicplants.database.ui.viewmodel.DichotomousKeyViewModel =
                viewModel()
            DichotomousKeysIndexScreen(
                viewModel = keyViewModel,
                plantViewModel = viewModel,
                onBack = { navController.popBackStack() },
                onKeyClick = { keyId -> navController.navigate("dichotomous_key/${Uri.encode(keyId)}") }
            )
        }

        // ── CLAVES DICOTÓMICAS: RECORRIDO ────────────────────────────
        composable(
            "dichotomous_key/{keyId}",
            arguments = listOf(navArgument("keyId") { type = NavType.StringType })
        ) { backStackEntry ->
            val keyId = backStackEntry.arguments?.getString("keyId") ?: "general"
            val keyViewModel: com.toxicplants.database.ui.viewmodel.DichotomousKeyViewModel =
                viewModel()
            DichotomousKeyScreen(
                keyId = keyId,
                keyViewModel = keyViewModel,
                plantViewModel = viewModel,
                onBack = { navController.popBackStack() },
                onPlantClick = { plant ->
                    viewModel.selectPlant(plant)
                    navController.navigate("plant_detail/${plant.id}")
                }
            )
        }

        // ── BÚSQUEDA POR SÍNTOMAS ────────────────────────────────────
        composable("search_symptoms") {
            SearchBySymptomsScreen(
                plantViewModel = viewModel,
                compoundViewModel = compoundViewModel,
                onPlantClick = { plant ->
                    viewModel.selectPlant(plant)
                    navController.navigate("plant_detail/${plant.id}")
                },
                onCompoundClick = { compound ->
                    navController.navigate("compound_detail/${compound.id}")
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── INTOXICACIÓN: SÍNTOMAS + SÍNDROMES ───────────────────────
        composable("intoxication") {
            IntoxicationScreen(
                onSymptomsClick = { navController.navigate("search_symptoms") },
                onSyndromesClick = { navController.navigate("toxic_syndromes") },
                onChildrenClick = { navController.navigate("child_safety") },
                onPetsClick = { navController.navigate("pet_safety") },
                onLivestockClick = { navController.navigate("livestock_safety") },
                onBack = { navController.popBackStack() }
            )
        }

        composable("toxic_syndromes") {
            ToxicSyndromesScreen(onBack = { navController.popBackStack() })
        }

        // ── AR ───────────────────────────────────────────────────────
        composable("ar") {
            ARScreen(
                viewModel = viewModel,
                onPlantClick = { plant ->
                    viewModel.selectPlant(plant)
                    navController.navigate("plant_detail/${plant.id}")
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── BAYAS ────────────────────────────────────────────────────
        composable("berries") {
            BerriesScreen(
                onBack = { navController.popBackStack() },
                onAddPlant = { color ->
                    navController.navigate(
                        "add_plant_extra/fruitColor?color=${
                            Uri.encode(
                                color
                            )
                        }"
                    )
                }
            )
        }

        // ── NOTAS ────────────────────────────────────────────────────
        composable("notes") {
            NotesScreen(onBack = { navController.popBackStack() })
        }

        // ── CÁMARA / IDENTIFICAR ─────────────────────────────────────
        composable("camera_identify") {
            CameraIdentifyScreen(
                viewModel = viewModel,
                onPlantClick = { plant ->
                    viewModel.selectPlant(plant)
                    navController.navigate("plant_detail/${plant.id}")
                },
                onNavigateToPlantNetResult = { name, scientificName ->
                    navController.navigate(
                        "plantnet_result/${Uri.encode(name)}/${
                            Uri.encode(
                                scientificName
                            )
                        }"
                    )
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── ESCÁNER DE TEXTO OCR ─────────────────────────────────────
        composable("text_scanner") {
            TextScannerScreen(
                viewModel = viewModel,
                onPlantClick = { plant ->
                    viewModel.selectPlant(plant)
                    navController.navigate("plant_detail/${plant.id}")
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── IDENTIFICAR SETAS / LÍQUENES ─────────────────────────────
        composable("nature_photo_identify") {
            val mushroomViewModel: MushroomViewModel = viewModel()
            val lichenViewModel: LichenViewModel = viewModel()
            PhotoIdentifyFungiLichensScreen(
                mushroomViewModel = mushroomViewModel,
                lichenViewModel = lichenViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // ── DETALLE DE PLANTA por ID — estable al volver de navegador/Wikipedia ─────
        composable(
            route = "plant_detail/{plantId}",
            arguments = listOf(navArgument("plantId") { type = NavType.IntType })
        ) { backStackEntry ->
            val routePlantId = backStackEntry.arguments?.getInt("plantId") ?: 0
            PlantDetailScreen(
                plantId = routePlantId,
                viewModel = viewModel,
                compoundViewModel = compoundViewModel,
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate("edit_plant/$id") },
                onNavigateToLocation = { id -> navController.navigate("location/$id") },
                onCompoundClick = { compound -> navController.navigate("compound_detail/${compound.id}") }
            )
        }

        // ── DETALLE DE PLANTA ────────────────────────────────────────
        composable("plant_detail") {
            val selectedPlant by viewModel.selectedPlantData.collectAsState()
            val plantToShow = selectedPlant

            if (plantToShow != null) {
                PlantDetailScreen(
                    plantId = plantToShow.id,
                    viewModel = viewModel,
                    compoundViewModel = compoundViewModel,
                    onBack = { navController.popBackStack() },
                    onEdit = { plantId -> navController.navigate("edit_plant/$plantId") },
                    onNavigateToLocation = { plantId -> navController.navigate("location/$plantId") },
                    onCompoundClick = { compound -> navController.navigate("compound_detail/${compound.id}") },
                    onNavigateToPlant = { id ->
                        navController.popBackStack()
                        navController.navigate("plant_detail/$id") {
                            launchSingleTop = true
                        }
                    }
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("❌", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No hay plantas disponibles", color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { navController.popBackStack() }) { Text("← Volver") }
                    }
                }
            }
        }

        // ── UBICACIÓN ────────────────────────────────────────────────
        composable("location/{plantId}") { backStackEntry ->
            val plantId = backStackEntry.arguments?.getString("plantId")?.toIntOrNull() ?: 0
            val allPlants by viewModel.allPlants.observeAsState(emptyList())
            val plant = allPlants.find { it.id == plantId }

            if (plant != null) {
                LocationScreen(
                    plantId = plantId,
                    plantName = plant.commonName,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("❌", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Planta no encontrada", color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { navController.popBackStack() }) { Text("← Volver") }
                    }
                }
            }
        }

        // ── EDITAR PLANTA ────────────────────────────────────────────
        composable("edit_plant/{plantId}") { backStackEntry ->
            val plantId = backStackEntry.arguments?.getString("plantId")?.toIntOrNull()
            EditPlantScreen(
                plantId = plantId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onSaveAndNext = { nextPlantId ->
                    navController.navigate("edit_plant/$nextPlantId") {
                        popUpTo("edit_plant/$plantId") { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onSaved = { savedPlantId ->
                    val removedOldDetail =
                        navController.popBackStack("plant_detail/{plantId}", inclusive = true)
                    if (!removedOldDetail) {
                        navController.popBackStack()
                    }
                    navController.navigate("plant_detail/$savedPlantId") {
                        launchSingleTop = true
                    }
                }
            )
        }

        // ── LISTA POR CATEGORÍA ──────────────────────────────────────
        composable("category/{category}") { backStackEntry ->
            val categoryName = backStackEntry.arguments?.getString("category") ?: ""
            CategoryListScreen(
                viewModel = viewModel,
                categoryName = categoryName,
                onPlantClick = { plant ->
                    viewModel.selectPlant(plant)
                    navController.navigate("plant_detail/${plant.id}")
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── RESULTADO PLANTNET ───────────────────────────────────────
        composable("plantnet_result/{name}/{scientificName}") { backStackEntry ->
            val name = backStackEntry.arguments?.getString("name") ?: ""
            val scientificName = backStackEntry.arguments?.getString("scientificName") ?: ""
            PlantNetResultScreen(
                name = name,
                scientificName = scientificName,
                onBack = { navController.popBackStack() }
            )
        }

        // ── BASES DE DATOS ONLINE ────────────────────────────────────
        composable("online_databases") {
            OnlineDatabasesScreen(onNavigateBack = { navController.popBackStack() })
        }

        // ── DESCARGAR IMÁGENES ───────────────────────────────────────
        composable("download_images") {
            DownloadImagesScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }

        // ── AJUSTES ──────────────────────────────────────────────────
        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToDownloadImages = { navController.navigate("download_images") },
                onNavigateToReviewCenter = { navController.navigate("review_center") }
            )
        }

        // ── CENTRO DE REVISIÓN ───────────────────────────────────
        composable("review_center") {
            ReviewCenterScreen(
                viewModel = viewModel,
                onIncompletePlants = { navController.navigate("incomplete_plants") },
                onPlantsWithNotes = { navController.navigate("plants_with_notes") },
                onPlantsWithMarkers = { navController.navigate("plants_with_markers") },
                onRecentPlants = { navController.navigate("recent_plants") },
                onDeletedPlants = { navController.navigate("deleted_plants") },
                onDuplicatePlants = { navController.navigate("duplicate_plants") },
                onScientificNameReview = { navController.navigate("scientific_name_review") },
                onSuspiciousText = { navController.navigate("suspicious_text_plants") },
                onDuplicateText = { navController.navigate("duplicate_text_plants") },
                onLocalChanges = { navController.navigate("local_changes") },
                onBack = { navController.popBackStack() }
            )
        }

        // ── CAMBIOS LOCALES ─────────────────────────────────────
        composable("local_changes") {
            LocalChangesScreen(
                plantViewModel = viewModel,
                compoundViewModel = compoundViewModel,
                onPlantClick = { plant ->
                    viewModel.selectPlant(plant)
                    navController.navigate("plant_detail/${plant.id}")
                },
                onCompoundClick = { compound -> navController.navigate("compound_detail/${compound.id}") },
                onBack = { navController.popBackStack() }
            )
        }

        // ── TEXTO DUPLICADO ───────────────────────────────────
        composable("duplicate_text_plants") {
            DuplicateTextPlantsScreen(
                viewModel = viewModel,
                onPlantClick = { plant ->
                    viewModel.selectPlant(plant)
                    navController.navigate("plant_detail/${plant.id}")
                },
                onEditPlant = { plantId -> navController.navigate("edit_plant/$plantId") },
                onBack = { navController.popBackStack() }
            )
        }

        // ── TEXTO SOSPECHOSO / INGLÉS ────────────────────────
        composable("suspicious_text_plants") {
            SuspiciousTextPlantsScreen(
                viewModel = viewModel,
                onPlantClick = { plant ->
                    viewModel.selectPlant(plant)
                    navController.navigate("plant_detail/${plant.id}")
                },
                onEditPlant = { plantId -> navController.navigate("edit_plant/$plantId") },
                onBack = { navController.popBackStack() }
            )
        }

        // ── REVISIÓN DE NOMBRES CIENTÍFICOS ───────────────────
        composable("scientific_name_review") {
            ScientificNameReviewScreen(
                viewModel = viewModel,
                onPlantClick = { plant ->
                    viewModel.selectPlant(plant)
                    navController.navigate("plant_detail/${plant.id}")
                },
                onEditPlant = { plantId -> navController.navigate("edit_plant/$plantId") },
                onBack = { navController.popBackStack() }
            )
        }

        // ── POSIBLES DUPLICADOS ────────────────────────────────
        composable("duplicate_plants") {
            DuplicatePlantsScreen(
                viewModel = viewModel,
                onPlantClick = { plant ->
                    viewModel.selectPlant(plant)
                    navController.navigate("plant_detail/${plant.id}")
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── PAPELERA DE PLANTAS ─────────────────────────────────
        composable("deleted_plants") {
            DeletedPlantsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // ── PLANTAS INCOMPLETAS / CALIDAD DE CATÁLOGO ─────────────
        composable("incomplete_plants") {
            IncompletePlantsScreen(
                viewModel = viewModel,
                onPlantClick = { plant ->
                    viewModel.selectPlant(plant)
                    navController.navigate("plant_detail/${plant.id}")
                },
                onEditPlant = { plantId -> navController.navigate("edit_plant/$plantId") },
                onBack = { navController.popBackStack() }
            )
        }

        // ── PLANTAS CON MARCADORES ────────────────────────────────
        composable("plants_with_markers") {
            PlantsWithMarkersScreen(
                viewModel = viewModel,
                onPlantClick = { plant ->
                    viewModel.selectPlant(plant)
                    navController.navigate("plant_detail/${plant.id}")
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── PLANTAS VISTAS RECIENTEMENTE ───────────────────────
        composable("recent_plants") {
            RecentPlantsScreen(
                viewModel = viewModel,
                onPlantClick = { plant ->
                    viewModel.selectPlant(plant)
                    navController.navigate("plant_detail/${plant.id}")
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── COMPARADOR DE PLANTAS ────────────────────────────────
        composable("plant_compare") {
            PlantCompareScreen(
                viewModel = viewModel,
                onPlantClick = { plant ->
                    viewModel.selectPlant(plant)
                    navController.navigate("plant_detail/${plant.id}")
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── PLANTAS CON NOTAS ─────────────────────────────────────
        composable("plants_with_notes") {
            PlantsWithNotesScreen(
                viewModel = viewModel,
                onPlantClick = { plant ->
                    viewModel.selectPlant(plant)
                    navController.navigate("plant_detail/${plant.id}")
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── BUSCADOR GLOBAL UNIFICADO ─────────────────────────────
        composable("global_search/{initialQuery}") { backStackEntry ->
            val initialQuery = backStackEntry.arguments?.getString("initialQuery") ?: ""
            GlobalSearchScreen(
                plantViewModel = viewModel,
                compoundViewModel = compoundViewModel,
                initialQuery = initialQuery,
                onPlantClick = { plant ->
                    viewModel.selectPlant(plant)
                    navController.navigate("plant_detail/${plant.id}")
                },
                onCompoundClick = { compound ->
                    navController.navigate("compound_detail/${compound.id}")
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable("global_search") {
            GlobalSearchScreen(
                plantViewModel = viewModel,
                compoundViewModel = compoundViewModel,
                initialQuery = "",
                onPlantClick = { plant ->
                    viewModel.selectPlant(plant)
                    navController.navigate("plant_detail/${plant.id}")
                },
                onCompoundClick = { compound ->
                    navController.navigate("compound_detail/${compound.id}")
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── PLANTAS PSICOTRÓPICAS ───────────────────────────────────
        composable("psychotropic_plants") {
            PsychotropicPlantsScreen(
                plantViewModel = viewModel,
                compoundViewModel = compoundViewModel,
                onPlantClick = { plant ->
                    viewModel.selectPlant(plant)
                    navController.navigate("plant_detail/${plant.id}")
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── QUÍMICA / FITOQUÍMICA ────────────────────────────────────
        composable("chemical_extraction_methods") {
            ChemicalExtractionMethodsScreen(onBack = { navController.popBackStack() })
        }

        composable("chemical_reagents") {
            ChemicalReagentsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToHomePhytoTests = { navController.navigate("home_phyto_tests") }
            )
        }

        composable("home_phyto_tests") {
            HomePhytoTestsScreen(onBack = { navController.popBackStack() })
        }

        composable("phytochemistry") {
            PhytochemistryScreen(
                viewModel = compoundViewModel,
                onGroupClick = { group -> navController.navigate("compound_group/${Uri.encode(group)}") },
                onAddCompoundClick = { navController.navigate("edit_compound/0") },
                onInteractionsClick = { navController.navigate("compound_interactions") },
                onCompoundClick = { c -> navController.navigate("compound_detail/${c.id}") },
                onBack = { navController.popBackStack() }
            )
        }

        composable("compound_interactions") {
            CompoundInteractionsScreen(
                viewModel = compoundViewModel,
                onCompoundClick = { c -> navController.navigate("compound_detail/${c.id}") },
                onBack = { navController.popBackStack() }
            )
        }

        composable("toxic_mushrooms") {
            val mushroomViewModel: MushroomViewModel = viewModel()
            ToxicMushroomsScreen(
                viewModel = mushroomViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("toxic_lichens") {
            val lichenViewModel: LichenViewModel = viewModel()
            ToxicLichensScreen(
                viewModel = lichenViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // ── GÉNEROS TÓXICOS ───────────────────────────────────────
        composable("toxic_genera") {
            ToxicGeneraScreen(
                onBack = { navController.popBackStack() },
                onGenusClick = { genus -> navController.navigate("genus_detail/${Uri.encode(genus)}") }
            )
        }

        composable(
            "genus_detail/{genus}",
            arguments = listOf(navArgument("genus") { type = NavType.StringType })
        ) { backStackEntry ->
            val genus = backStackEntry.arguments?.getString("genus") ?: ""
            GenusDetailScreen(
                genusName = genus,
                plantViewModel = viewModel,
                onBack = { navController.popBackStack() },
                onPlantClick = { plant ->
                    viewModel.selectPlant(plant)
                    navController.navigate("plant_detail/${plant.id}")
                }
            )
        }

        composable("compound_group/{group}") { backStackEntry ->
            val group = backStackEntry.arguments?.getString("group") ?: ""
            CompoundGroupScreen(
                viewModel = compoundViewModel,
                groupName = group,
                onCompoundClick = { c -> navController.navigate("compound_detail/${c.id}") },
                onBack = { navController.popBackStack() },
                onEditCompound = { c -> navController.navigate("edit_compound/${c.id}") }
            )
        }

        composable("compound_detail/{id}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id")?.toIntOrNull() ?: 0
            CompoundDetailScreen(
                compoundId = id,
                compoundViewModel = compoundViewModel,
                plantViewModel = viewModel,
                onBack = { navController.popBackStack() },
                onPlantClick = { plant ->
                    viewModel.selectPlant(plant)
                    navController.navigate("plant_detail/${plant.id}")
                }
            )
        }

        composable("edit_compound/{compoundId}") { backStackEntry ->
            val compoundId = backStackEntry.arguments?.getString("compoundId")?.toIntOrNull()
            EditCompoundScreen(
                compoundId = compoundId,
                viewModel = compoundViewModel,
                onBack = { navController.popBackStack() }
            )
        }


        composable(
            "new_plant_from_genus/{genusId}",
            arguments = listOf(navArgument("genusId") { type = NavType.IntType })
        ) { backStackEntry ->
            val genusId = backStackEntry.arguments?.getInt("genusId") ?: 0
            val genera by poisonousFamilyViewModel.allGenera.observeAsState(emptyList())
            val genus = genera.firstOrNull { it.id == genusId }
            val prefillPlant = genus?.let { g ->
                PlantEntity(
                    id = 0,
                    commonName = g.genusName,
                    commonNames = "",
                    scientificName = "${g.genusName} sp.",
                    family = g.familyName,
                    toxicityLevel = if (g.catalogType == "all") "Alto" else "Moderado",
                    toxicParts = g.toxicParts,
                    symptoms = g.symptoms,
                    description = listOf(
                        "Ficha creada desde Familias venenosas.",
                        "Género: ${g.genusName}",
                        "Familia: ${g.familyName}",
                        "Toxinas/principios: ${g.toxins}",
                        "Notas: ${g.notes}"
                    ).joinToString("\n\n"),
                    habitat = "",
                    geographicDistribution = "",
                    firstAid = "",
                    imageUrl = "",
                    isFavorite = false,
                    category = "Familias venenosas",
                    notes = g.notes
                )
            }
            EditPlantScreen(
                plantId = null,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                prefillPlant = prefillPlant
            )
        }

        // ── NUEVA PLANTA ─────────────────────────────────────────────
        composable("new_plant") {
            EditPlantScreen(
                plantId = null,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // ════════════════════════════════════════════════════════════
        // NUEVAS RUTAS — Familias botánicas
        // ════════════════════════════════════════════════════════════

        composable("family_list") {
            FamilyListScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onFamilyClick = { family ->
                    navController.navigate("plants_by_family/${Uri.encode(family)}")
                }
            )
        }

        composable(
            route = "plants_by_family/{family}",
            arguments = listOf(navArgument("family") { type = NavType.StringType })
        ) { backStackEntry ->
            val family = backStackEntry.arguments?.getString("family") ?: ""
            PlantsByFamilyScreen(
                family = family,
                viewModel = viewModel,
                onPlantClick = { plant ->
                    viewModel.selectPlant(plant)
                    navController.navigate("plant_detail/${plant.id}")
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── HISTORIAL DE AVISTAMIENTOS ───────────────────────────────
        composable("sightings_history") {
            val sightingViewModel: SightingViewModel = viewModel()
            SightingsHistoryScreen(
                viewModel = sightingViewModel,
                plantViewModel = viewModel,
                onPlantClick = { plant ->
                    viewModel.selectPlant(plant)
                    navController.navigate("plant_detail/${plant.id}")
                },
                onEditPlantLocation = { plantId -> navController.navigate("location/$plantId") },
                onBack = { navController.popBackStack() }
            )
        }

        // ── MASCOTAS ─────────────────────────────────────────────────
        composable("pet_safety") {
            PetSafetyScreen(
                viewModel = viewModel,
                onPlantClick = { plant -> viewModel.selectPlant(plant); navController.navigate("plant_detail/${plant.id}") },
                onBack = { navController.popBackStack() },
                onAddPlant = { navController.navigate("add_plant_extra/dogs") },
                onEditPlant = { id -> navController.navigate("edit_plant/$id") }
            )
        }

        // ── INFANTIL ─────────────────────────────────────────────────
        composable("child_safety") {
            ChildSafetyScreen(
                viewModel = viewModel,
                onPlantClick = { plant -> viewModel.selectPlant(plant); navController.navigate("plant_detail/${plant.id}") },
                onBack = { navController.popBackStack() },
                onAddPlant = { navController.navigate("add_plant_extra/children") },
                onEditPlant = { id -> navController.navigate("edit_plant/$id") }
            )
        }

        // ── GANADO ───────────────────────────────────────────────────
        composable("livestock_safety") {
            LivestockSafetyScreen(
                viewModel = viewModel,
                onPlantClick = { plant -> viewModel.selectPlant(plant); navController.navigate("plant_detail/${plant.id}") },
                onBack = { navController.popBackStack() },
                onAddPlant = { navController.navigate("add_plant_extra/horses") },
                onEditPlant = { id -> navController.navigate("edit_plant/$id") }
            )
        }

        // ── PLANTAS CONFUNDIBLES ─────────────────────────────────────
        composable("confusable_plants") {
            ConfusablePlantsScreen(
                viewModel = viewModel,
                onPlantClick = { plant -> viewModel.selectPlant(plant); navController.navigate("plant_detail/${plant.id}") },
                onBack = { navController.popBackStack() },
                onAddPlant = { navController.navigate("add_plant_extra/dogs") },
                onEditPlant = { id -> navController.navigate("edit_plant/$id") }
            )
        }

        // ── MAPA DE AVISTAMIENTOS ────────────────────────────────────
        composable("sightings_map") {
            SightingsMapScreen(
                viewModel = viewModel,
                onPlantClick = { plant -> viewModel.selectPlant(plant); navController.navigate("plant_detail/${plant.id}") },
                onBack = { navController.popBackStack() }
            )
        }

        composable("color_search") {
            ColorSearchScreen(
                viewModel = viewModel,
                onPlantClick = { plant -> viewModel.selectPlant(plant); navController.navigate("plant_detail/${plant.id}") },
                onBack = { navController.popBackStack() },
                onAddPlant = { mode: String, color: String ->
                    navController.navigate(
                        "add_plant_extra/${
                            Uri.encode(
                                mode
                            )
                        }?color=${Uri.encode(color)}"
                    )
                },
                onEditPlant = { id -> navController.navigate("edit_plant/$id") }
            )
        }

        composable("toxic_parts") {
            ToxicPartsScreen(
                viewModel = viewModel,
                onPlantClick = { plant -> viewModel.selectPlant(plant); navController.navigate("plant_detail/${plant.id}") },
                onBack = { navController.popBackStack() }
            )
        }

        // ── CALENDARIO DE TÓXICOS ────────────────────────────────────
        composable("toxic_calendar") {
            val calendarViewModel: ToxicCalendarViewModel = viewModel()
            ToxicCalendarScreen(
                viewModel = calendarViewModel,
                plantViewModel = viewModel,
                onBack = { navController.popBackStack() },
                onPlantClick = { plant ->
                    viewModel.selectPlant(plant)
                    navController.navigate("plant_detail/${plant.id}")
                }
            )
        }

        // ── AÑADIR PLANTA A EXTRA ────────────────────────────────────
        composable(
            route = "add_plant_extra/{mode}?color={color}",
            arguments = listOf(
                navArgument("mode") { type = NavType.StringType },
                navArgument("color") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode") ?: "dogs"
            val color = backStackEntry.arguments?.getString("color") ?: ""
            AddPlantToExtraScreen(
                viewModel = viewModel,
                mode = mode,
                colorValue = color,
                onBack = { navController.popBackStack() }
            )
        }

    } // fin NavHost
}

// ── CategoryListScreen ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListScreen(
    viewModel: PlantViewModel,
    categoryName: String,
    onPlantClick: (PlantEntity) -> Unit,
    onBack: () -> Unit
) {
    val allPlants by viewModel.allPlants.observeAsState(emptyList())
    val filteredPlants = allPlants.filter { it.category == categoryName }
    var plantToDelete by remember { mutableStateOf<PlantEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🗂️ $categoryName", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1976D2),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {
            if (filteredPlants.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🌿", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No hay plantas en esta categoría", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredPlants) { plant ->
                        CategoryPlantCard(
                            plant = plant,
                            onClick = {
                                viewModel.setDetailNavigationPlants(filteredPlants); onPlantClick(
                                plant
                            )
                            },
                            onDeleteClick = { plantToDelete = plant }
                        )
                    }
                }
            }
        }
    }

    plantToDelete?.let { plant ->
        AlertDialog(
            onDismissRequest = { plantToDelete = null },
            title = { Text("¿Eliminar planta?") },
            text = { Text("¿Eliminar ${plant.commonName}?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePlant(plant)
                    plantToDelete = null
                }) { Text("Eliminar", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { plantToDelete = null }) { Text("Cancelar") }
            }
        )
    }
}

// ── CategoryPlantCard ─────────────────────────────────────────────────────

@Composable
fun CategoryPlantCard(
    plant: PlantEntity,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val toxicityColor = when (plant.toxicityLevel) {
        "Mortal" -> Color(0xFFB71C1C)
        "Muy alto" -> Color(0xFFFF5722)
        "Alto" -> Color(0xFFE65100)
        "Moderado" -> Color(0xFFF57C00)
        "Bajo" -> Color(0xFF388E3C)
        else -> Color.Gray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            PlantThumbnail(
                plant = plant,
                toxicityColor = toxicityColor,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    plant.commonName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    plant.scientificName,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontStyle = FontStyle.Italic,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    color = toxicityColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        plant.toxicityLevel,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 11.sp,
                        color = toxicityColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (plant.latitude != null && plant.longitude != null) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = "Tiene ubicación",
                    tint = Color(0xFF1565C0),
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red)
            }
        }
    }
}

