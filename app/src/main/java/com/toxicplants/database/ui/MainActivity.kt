package com.toxicplants.database.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.ui.screens.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.toxicplants.database.ui.theme.ToxicPlantsTheme
import com.toxicplants.database.ui.viewmodel.CompoundViewModel
import com.toxicplants.database.ui.viewmodel.PlantViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val systemDark = isSystemInDarkTheme()
            ToxicPlantsTheme(darkTheme = systemDark) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
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

    NavHost(navController = navController, startDestination = "home") {

        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToList = { navController.navigate("plant_list") },
                onNavigateToCategories = { navController.navigate("categories") },
                onNavigateToEmergency = { navController.navigate("emergency") },
                onNavigateToOnlineDatabases = { navController.navigate("online_databases") },
                onNavigateToSearch = { navController.navigate("search") },
                onNavigateToSearchBySymptoms = { navController.navigate("search_symptoms") },
                onNavigateToDownloadImages = { navController.navigate("download_images") },
                onNavigateToNewPlant = { navController.navigate("new_plant") },
                onNavigateToCamera = { navController.navigate("camera_identify") },
                onNavigateToPhytochemistry = { navController.navigate("phytochemistry") },
                onNavigateToAR = { navController.navigate("ar") },
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToBerries = { navController.navigate("berries") },
                onPlantClick = { plant ->
                    viewModel.selectPlant(plant)
                    navController.navigate("plant_detail")
                }
            )
        }

        composable("plant_list") {
            PlantListScreen(
                viewModel = viewModel,
                onPlantClick = { plant ->
                    viewModel.selectPlant(plant)
                    navController.navigate("plant_detail")
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable("categories") {
            CategoriesScreen(
                viewModel = viewModel,
                onCategoryClick = { categoryName -> navController.navigate("category/$categoryName") },
                onBack = { navController.popBackStack() }
            )
        }

        composable("emergency") {
            EmergencyScreen(
                viewModel = viewModel,
                onPlantClick = { plant ->
                    viewModel.selectPlant(plant)
                    navController.navigate("plant_detail")
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable("search") {
            SearchScreen(
                viewModel = viewModel,
                onPlantClick = { plant ->
                    viewModel.selectPlant(plant)
                    navController.navigate("plant_detail")
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable("search_symptoms") {
            SearchBySymptomsScreen(
                viewModel = viewModel,
                onPlantClick = { plant ->
                    viewModel.selectPlant(plant)
                    navController.navigate("plant_detail")
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable("ar") {
            ARScreen(
                viewModel = viewModel,
                onPlantClick = { plant ->
                    viewModel.selectPlant(plant)
                    navController.navigate("plant_detail")
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable("berries") {
            BerriesScreen(onBack = { navController.popBackStack() })
        }

        composable("camera_identify") {
            CameraIdentifyScreen(
                viewModel = viewModel,
                onPlantClick = { plant ->
                    viewModel.selectPlant(plant)
                    navController.navigate("plant_detail")
                },
                onNavigateToPlantNetResult = { name, scientificName ->
                    navController.navigate("plantnet_result/$name/$scientificName")
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable("plant_detail") {
            val selectedPlant by viewModel.selectedPlantData.collectAsState()
            val allPlants by viewModel.allPlants.observeAsState(emptyList())
            val plantToShow = selectedPlant ?: allPlants.firstOrNull()

            if (plantToShow != null) {
                PlantDetailScreen(
                    plantId = plantToShow.id,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onEdit = { plantId -> navController.navigate("edit_plant/$plantId") },
                    onNavigateToLocation = { plantId -> navController.navigate("location/$plantId") }
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("❌", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No hay plantas disponibles", color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { navController.popBackStack() }) {
                            Text("← Volver")
                        }
                    }
                }
            }
        }

        composable("location/{plantId}") { backStackEntry ->
            val plantIdString = backStackEntry.arguments?.getString("plantId") ?: "0"
            val plantId = plantIdString.toIntOrNull() ?: 0
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
                        Button(onClick = { navController.popBackStack() }) {
                            Text("← Volver")
                        }
                    }
                }
            }
        }

        composable("edit_plant/{plantId}") { backStackEntry ->
            val plantIdString = backStackEntry.arguments?.getString("plantId") ?: "0"
            val plantId = plantIdString.toIntOrNull()
            EditPlantScreen(
                plantId = plantId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("category/{category}") { backStackEntry ->
            val categoryName = backStackEntry.arguments?.getString("category") ?: ""
            CategoryListScreen(
                viewModel = viewModel,
                categoryName = categoryName,
                onPlantClick = { plant ->
                    viewModel.selectPlant(plant)
                    navController.navigate("plant_detail")
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable("plantnet_result/{name}/{scientificName}") { backStackEntry ->
            val name = backStackEntry.arguments?.getString("name") ?: ""
            val scientificName = backStackEntry.arguments?.getString("scientificName") ?: ""
            PlantNetResultScreen(name = name, scientificName = scientificName, onBack = { navController.popBackStack() })
        }

        composable("online_databases") {
            OnlineDatabasesScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable("download_images") {
            DownloadImagesScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }

        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToDownloadImages = { navController.navigate("download_images") }
            )
        }

        composable("phytochemistry") {
            PhytochemistryScreen(
                viewModel = compoundViewModel,
                onGroupClick = { group -> navController.navigate("compound_group/$group") },
                onAddCompoundClick = { navController.navigate("edit_compound/0") },
                onCompoundClick = { c -> navController.navigate("compound_detail/${c.id}") },
                onBack = { navController.popBackStack() }
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
                    navController.navigate("plant_detail")
                },
            )
        }

        composable("edit_compound/{compoundId}") { backStackEntry ->
            val compoundIdString = backStackEntry.arguments?.getString("compoundId") ?: "0"
            val compoundId = compoundIdString.toIntOrNull()
            EditCompoundScreen(
                compoundId = compoundId,
                viewModel = compoundViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("new_plant") {
            EditPlantScreen(
                plantId = null,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

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
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1976D2), titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (filteredPlants.isEmpty()) {
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🌿", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No hay plantas en esta categoría", color = Color.Gray)
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredPlants) { plant ->
                        CategoryPlantCard(plant = plant, onClick = { onPlantClick(plant) }, onDeleteClick = { plantToDelete = plant })
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
            confirmButton = { TextButton(onClick = { viewModel.deletePlant(plant); plantToDelete = null }) { Text("Eliminar", color = Color.Red) } },
            dismissButton = { TextButton(onClick = { plantToDelete = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
fun CategoryPlantCard(plant: PlantEntity, onClick: () -> Unit, onDeleteClick: () -> Unit) {
    val toxicityColor = when (plant.toxicityLevel) {
        "Mortal" -> Color(0xFFB71C1C); "Alto" -> Color(0xFFE65100); "Moderado" -> Color(0xFFF57C00); "Bajo" -> Color(0xFF388E3C)
        else -> Color.Gray
    }

    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(50.dp).background(toxicityColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Text(when (plant.toxicityLevel) { "Mortal" -> "☠️"; "Alto" -> "⚠️"; "Moderado" -> "⚡"; else -> "🌿" }, fontSize = 24.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(plant.commonName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(plant.scientificName, style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontStyle = FontStyle.Italic, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Surface(color = toxicityColor.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                    Text(plant.toxicityLevel, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 11.sp, color = toxicityColor, fontWeight = FontWeight.Bold)
                }
            }
            if (plant.latitude != null && plant.longitude != null) {
                Icon(Icons.Default.LocationOn, contentDescription = "Tiene ubicación", tint = Color(0xFF1565C0), modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red)
            }
        }
    }
}