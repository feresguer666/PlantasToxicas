package com.toxicplants.database.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.ui.viewmodel.PlantViewModel
import com.toxicplants.database.ui.viewmodel.CompoundViewModel
import com.toxicplants.database.ui.screens.*

sealed class Screen(val route: String) {
    object Home             : Screen("home")
    object PlantList        : Screen("plant_list")
    object PlantDetail      : Screen("plant_detail/{plantId}") { fun createRoute(plantId: Int) = "plant_detail/$plantId" }
    object Categories       : Screen("categories")
    object Emergency        : Screen("emergency")
    object OnlineDatabases  : Screen("online_databases")
    object Search           : Screen("search")
    object SearchBySymptoms : Screen("search_symptoms")
    object DownloadImages   : Screen("download_images")
    object NewPlant         : Screen("new_plant")
    object EditPlant        : Screen("edit_plant/{plantId}") { fun createRoute(plantId: Int) = "edit_plant/$plantId" }
    object CameraIdentify   : Screen("camera_identify")
    object PlantNetResult   : Screen("plantnet_result/{name}/{scientificName}") { fun createRoute(name: String, scientificName: String) = "plantnet_result/$name/$scientificName" }
    object AR               : Screen("ar")
    object BerriesGuide     : Screen("berries_guide")
    object Notes            : Screen("notes")
    object Phytochemistry   : Screen("phytochemistry")
    object Settings         : Screen("settings")
    object CompoundGroup    : Screen("compound_group/{group}") { fun createRoute(group: String) = "compound_group/$group" }
    object CompoundDetail   : Screen("compound_detail/{id}") { fun createRoute(id: Int) = "compound_detail/$id" }
    object EditCompound     : Screen("edit_compound/{compoundId}") { fun createRoute(compoundId: Int) = "edit_compound/$compoundId" }
    object Location         : Screen("location/{plantId}") { fun createRoute(plantId: Int) = "location/$plantId" }
    object CategoryList     : Screen("category/{category}") { fun createRoute(category: String) = "category/$category" }
}

/**
 * ⚠️ NOTA: Esta función NO se usa actualmente.
 * La navegación activa está en `MainActivity.kt` → `MainApp()`.
 * Si decides usar esta función, reemplaza la navegación de MainActivity.kt por esta.
 */
@Composable
fun PlantNavGraph(
    viewModel: PlantViewModel,
    compoundViewModel: CompoundViewModel
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Home.route) {

        composable(Screen.Home.route) {
            HomeScreen(
                viewModel                    = viewModel,
                onNavigateToList             = { navController.navigate(Screen.PlantList.route) },
                onNavigateToCategories       = { navController.navigate(Screen.Categories.route) },
                onNavigateToEmergency        = { navController.navigate(Screen.Emergency.route) },
                onNavigateToOnlineDatabases  = { navController.navigate(Screen.OnlineDatabases.route) },
                onNavigateToSearch           = { navController.navigate(Screen.Search.route) },
                onNavigateToSearchBySymptoms = { navController.navigate(Screen.SearchBySymptoms.route) },
                onNavigateToDownloadImages   = { navController.navigate(Screen.DownloadImages.route) },
                onNavigateToNewPlant         = { navController.navigate(Screen.NewPlant.route) },
                onNavigateToCamera           = { navController.navigate(Screen.CameraIdentify.route) },
                onNavigateToPhytochemistry   = { navController.navigate(Screen.Phytochemistry.route) },
                onNavigateToSettings         = { navController.navigate(Screen.Settings.route) },
                onNavigateToAR               = { navController.navigate(Screen.AR.route) },
                onNavigateToBerries          = { navController.navigate(Screen.BerriesGuide.route) },
                onNavigateToNotes            = { navController.navigate(Screen.Notes.route) },
                onPlantClick = { plant: PlantEntity ->
                    viewModel.selectPlant(plant)
                    navController.navigate(Screen.PlantDetail.createRoute(plant.id))
                }
            )
        }

        composable(Screen.PlantList.route) {
            PlantListScreen(
                viewModel = viewModel,
                onPlantClick = { plant: PlantEntity ->
                    viewModel.selectPlant(plant)
                    navController.navigate(Screen.PlantDetail.createRoute(plant.id))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.PlantDetail.route,
            arguments = listOf(navArgument("plantId") { type = NavType.IntType })
        ) { backStackEntry ->
            val plantId = backStackEntry.arguments?.getInt("plantId") ?: return@composable
            PlantDetailScreen(
                plantId = plantId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(Screen.EditPlant.createRoute(id)) },
                onNavigateToLocation = { id -> navController.navigate(Screen.Location.createRoute(id)) }
            )
        }

        composable(Screen.Categories.route) {
            CategoriesScreen(
                viewModel = viewModel,
                onCategoryClick = { category: String ->
                    navController.navigate(Screen.CategoryList.createRoute(category))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.CategoryList.route,
            arguments = listOf(navArgument("category") { type = NavType.StringType })
        ) { backStackEntry ->
            val categoryName = backStackEntry.arguments?.getString("category") ?: ""
            // Usar la CategoryListScreen de MainActivity o navegar con filtro
            viewModel.setCategory(categoryName)
            navController.navigate(Screen.PlantList.route)
        }

        composable(Screen.Emergency.route) {
            EmergencyScreen(
                viewModel = viewModel,
                onPlantClick = { plant: PlantEntity ->
                    viewModel.selectPlant(plant)
                    navController.navigate(Screen.PlantDetail.createRoute(plant.id))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.OnlineDatabases.route) {
            OnlineDatabasesScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Search.route) {
            SearchScreen(
                viewModel = viewModel,
                onPlantClick = { plant: PlantEntity ->
                    viewModel.selectPlant(plant)
                    navController.navigate(Screen.PlantDetail.createRoute(plant.id))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.SearchBySymptoms.route) {
            SearchBySymptomsScreen(
                viewModel = viewModel,
                onPlantClick = { plant: PlantEntity ->
                    viewModel.selectPlant(plant)
                    navController.navigate(Screen.PlantDetail.createRoute(plant.id))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.DownloadImages.route) {
            DownloadImagesScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }

        composable(Screen.NewPlant.route) {
            EditPlantScreen(plantId = null, viewModel = viewModel, onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.EditPlant.route,
            arguments = listOf(navArgument("plantId") { type = NavType.IntType })
        ) { backStackEntry ->
            val plantId = backStackEntry.arguments?.getInt("plantId") ?: return@composable
            EditPlantScreen(plantId = plantId, viewModel = viewModel, onBack = { navController.popBackStack() })
        }

        composable(Screen.CameraIdentify.route) {
            CameraIdentifyScreen(
                viewModel = viewModel,
                onPlantClick = { plant: PlantEntity ->
                    viewModel.selectPlant(plant)
                    navController.navigate(Screen.PlantDetail.createRoute(plant.id))
                },
                onNavigateToPlantNetResult = { name, scientificName ->
                    navController.navigate(Screen.PlantNetResult.createRoute(name, scientificName))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.PlantNetResult.route,
            arguments = listOf(
                navArgument("name") { type = NavType.StringType },
                navArgument("scientificName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val name = backStackEntry.arguments?.getString("name") ?: ""
            val scientificName = backStackEntry.arguments?.getString("scientificName") ?: ""
            PlantNetResultScreen(name = name, scientificName = scientificName, onBack = { navController.popBackStack() })
        }

        composable(Screen.AR.route) {
            ARScreen(
                viewModel = viewModel,
                onPlantClick = { plant: PlantEntity ->
                    viewModel.selectPlant(plant)
                    navController.navigate(Screen.PlantDetail.createRoute(plant.id))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.BerriesGuide.route) {
            BerriesGuideScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }

        composable(Screen.Notes.route) {
            NotesScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToDownloadImages = { navController.navigate(Screen.DownloadImages.route) }
            )
        }

        // ✅ FITOQUÍMICA (antes faltaba)
        composable(Screen.Phytochemistry.route) {
            PhytochemistryScreen(
                viewModel = compoundViewModel,
                onGroupClick = { group -> navController.navigate(Screen.CompoundGroup.createRoute(group)) },
                onAddCompoundClick = { navController.navigate(Screen.EditCompound.createRoute(0)) },
                onCompoundClick = { c -> navController.navigate(Screen.CompoundDetail.createRoute(c.id)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.CompoundGroup.route,
            arguments = listOf(navArgument("group") { type = NavType.StringType })
        ) { backStackEntry ->
            val group = backStackEntry.arguments?.getString("group") ?: ""
            CompoundGroupScreen(
                viewModel = compoundViewModel,
                groupName = group,
                onCompoundClick = { c -> navController.navigate(Screen.CompoundDetail.createRoute(c.id)) },
                onBack = { navController.popBackStack() },
                onEditCompound = { c -> navController.navigate(Screen.EditCompound.createRoute(c.id)) }
            )
        }

        composable(
            route = Screen.CompoundDetail.route,
            arguments = listOf(navArgument("id") { type = NavType.IntType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id") ?: 0
            CompoundDetailScreen(
                compoundId = id,
                compoundViewModel = compoundViewModel,
                plantViewModel = viewModel,
                onBack = { navController.popBackStack() },
                onPlantClick = { plant ->
                    viewModel.selectPlant(plant)
                    navController.navigate(Screen.PlantDetail.createRoute(plant.id))
                }
            )
        }

        composable(
            route = Screen.EditCompound.route,
            arguments = listOf(navArgument("compoundId") { type = NavType.IntType })
        ) { backStackEntry ->
            val compoundId = backStackEntry.arguments?.getInt("compoundId") ?: 0
            EditCompoundScreen(
                compoundId = compoundId,
                viewModel = compoundViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // ✅ UBICACIÓN (antes faltaba)
        composable(
            route = Screen.Location.route,
            arguments = listOf(navArgument("plantId") { type = NavType.IntType })
        ) { backStackEntry ->
            val plantId = backStackEntry.arguments?.getInt("plantId") ?: 0
            val allPlants = viewModel.allPlants.value ?: emptyList()
            val plant = allPlants.find { it.id == plantId }
            if (plant != null) {
                LocationScreen(
                    plantId = plantId,
                    plantName = plant.commonName,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
