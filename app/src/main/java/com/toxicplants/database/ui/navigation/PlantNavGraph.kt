package com.toxicplants.database.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.ui.viewmodel.PlantViewModel
import com.toxicplants.database.ui.screens.HomeScreen
import com.toxicplants.database.ui.screens.PlantListScreen
import com.toxicplants.database.ui.screens.PlantDetailScreen
import com.toxicplants.database.ui.screens.CategoriesScreen
import com.toxicplants.database.ui.screens.EmergencyScreen
import com.toxicplants.database.ui.screens.SearchScreen
import com.toxicplants.database.ui.screens.DownloadImagesScreen
import com.toxicplants.database.ui.screens.EditPlantScreen
import com.toxicplants.database.ui.screens.OnlineDatabasesScreen
import com.toxicplants.database.ui.screens.CameraIdentifyScreen
import com.toxicplants.database.ui.screens.PlantNetResultScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object PlantList : Screen("plant_list")
    object PlantDetail : Screen("plant_detail/{plantId}") {
        fun createRoute(plantId: Int) = "plant_detail/$plantId"
    }
    object Categories : Screen("categories")
    object Emergency : Screen("emergency")
    object OnlineDatabases : Screen("online_databases")
    object Search : Screen("search")
    object DownloadImages : Screen("download_images")
    object NewPlant : Screen("new_plant")
    object EditPlant : Screen("edit_plant/{plantId}") {
        fun createRoute(plantId: Int) = "edit_plant/$plantId"
    }
    object CameraIdentify : Screen("camera_identify")
    object PlantNetResult : Screen("plantnet_result/{name}/{scientificName}") {
        fun createRoute(name: String, scientificName: String) = "plantnet_result/$name/$scientificName"
    }
    object SearchBySymptoms : Screen("search_symptoms")
}

@Composable
fun PlantNavGraph(viewModel: PlantViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToList = { navController.navigate(Screen.PlantList.route) },
                onNavigateToCategories = { navController.navigate(Screen.Categories.route) },
                onNavigateToEmergency = { navController.navigate(Screen.Emergency.route) },
                onNavigateToOnlineDatabases = { navController.navigate(Screen.OnlineDatabases.route) },
                onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                onNavigateToDownloadImages = { navController.navigate(Screen.DownloadImages.route) },
                onNavigateToNewPlant = { navController.navigate(Screen.NewPlant.route) },
                onNavigateToCamera = { navController.navigate(Screen.CameraIdentify.route) },
                onNavigateToPhytochemistry = { /* TODO */ },
                onNavigateToSettings = { /* TODO */ },
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
                onEdit = { id -> navController.navigate(Screen.EditPlant.createRoute(id)) }
            )
        }

        composable(Screen.Categories.route) {
            CategoriesScreen(
                viewModel = viewModel,
                onCategoryClick = { category: String ->
                    viewModel.setCategory(category)
                    navController.navigate(Screen.PlantList.route)
                },
                onBack = { navController.popBackStack() }
            )
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
    }
}