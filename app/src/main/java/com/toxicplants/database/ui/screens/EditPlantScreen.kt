package com.toxicplants.database.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.ui.viewmodel.PlantViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPlantScreen(
    plantId: Int?,
    viewModel: PlantViewModel,
    onBack: () -> Unit,
    onSaveAndNext: ((Int) -> Unit)? = null,
    onSaved: ((Int) -> Unit)? = null
) {
    val isNew = plantId == null || plantId == 0
    val existingPlant = if (!isNew) {
        viewModel.getPlantById(plantId!!).observeAsState().value
    } else null
    val saveScope = rememberCoroutineScope()
    val navigationPlants by viewModel.detailNavigationPlantsData.collectAsState()
    val currentIndexInContext = remember(navigationPlants, existingPlant?.id, plantId) {
        val currentId = existingPlant?.id ?: plantId ?: 0
        navigationPlants.indexOfFirst { it.id == currentId }
    }
    val nextPlantInContext = remember(navigationPlants, currentIndexInContext) {
        if (currentIndexInContext >= 0 && currentIndexInContext < navigationPlants.lastIndex) {
            navigationPlants[currentIndexInContext + 1]
        } else null
    }
    val canSaveAndNext = !isNew && onSaveAndNext != null && nextPlantInContext != null

    var commonName by remember(existingPlant) { mutableStateOf(existingPlant?.commonName ?: "") }
    var commonNames by remember(existingPlant) { mutableStateOf(existingPlant?.commonNames ?: "") }
    var scientificName by remember(existingPlant) { mutableStateOf(existingPlant?.scientificName ?: "") }
    var family by remember(existingPlant) { mutableStateOf(existingPlant?.family ?: "") }
    var toxicityLevel by remember(existingPlant) { mutableStateOf(existingPlant?.toxicityLevel ?: "Moderado") }
    var toxicParts by remember(existingPlant) { mutableStateOf(existingPlant?.toxicParts ?: "") }
    var symptoms by remember(existingPlant) { mutableStateOf(existingPlant?.symptoms ?: "") }
    var description by remember(existingPlant) { mutableStateOf(existingPlant?.description ?: "") }
    var habitat by remember(existingPlant) { mutableStateOf(existingPlant?.habitat ?: "") }
    var geographicDistribution by remember(existingPlant) { mutableStateOf(existingPlant?.geographicDistribution ?: "") }
    var firstAid by remember(existingPlant) { mutableStateOf(existingPlant?.firstAid ?: "") }
    var imageUrl by remember(existingPlant) { mutableStateOf(existingPlant?.imageUrl ?: "") }
    var category by remember(existingPlant) { mutableStateOf(existingPlant?.category ?: "") }
    var floweringMonths by remember(existingPlant) { mutableStateOf(existingPlant?.floweringMonths ?: "") }
    var fruitingMonths by remember(existingPlant) { mutableStateOf(existingPlant?.fruitingMonths ?: "") }
    var maxToxicityMonths by remember(existingPlant) { mutableStateOf(existingPlant?.maxToxicityMonths ?: "") }

    fun buildPlantForSave(): PlantEntity = PlantEntity(
        id = existingPlant?.id ?: 0,
        commonName = commonName,
        commonNames = commonNames,
        scientificName = scientificName,
        family = family,
        toxicityLevel = toxicityLevel,
        toxicParts = toxicParts,
        symptoms = symptoms,
        description = description,
        habitat = habitat,
        geographicDistribution = geographicDistribution,
        firstAid = firstAid,
        imageUrl = imageUrl,
        isFavorite = existingPlant?.isFavorite ?: false,
        category = category,
        latitude = existingPlant?.latitude,
        longitude = existingPlant?.longitude,
        locationName = existingPlant?.locationName,
        foundDate = existingPlant?.foundDate,
        notes = existingPlant?.notes,
        floweringMonths = floweringMonths,
        fruitingMonths = fruitingMonths,
        maxToxicityMonths = maxToxicityMonths,
        mythsAndLegends = existingPlant?.mythsAndLegends ?: ""
    )


    var expanded by remember { mutableStateOf(false) }
    val toxicityOptions = listOf("Mortal", "Alto", "Moderado", "Bajo")

    // Colores personalizados para los campos de texto (Modo Oscuro)
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = Color(0xFF4CAF50),
        unfocusedBorderColor = Color.Gray,
        focusedLabelColor = Color(0xFF81C784),
        unfocusedLabelColor = Color.LightGray,
        cursorColor = Color.White
    )

    Scaffold(
        topBar = {

            TopAppBar(
                title = {
                    Text(
                        if (isNew) "Nueva planta" else "Editar planta",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2E7D32),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                actions = {
                    IconButton(
                        onClick = {
                            if (commonName.isNotBlank()) {
                                val plant = buildPlantForSave()
                                viewModel.insertPlant(plant)
                                if (onSaved != null && plant.id != 0) {
                                    onSaved(plant.id)
                                } else {
                                    onBack()
                                }
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Guardar",
                            tint = Color.White
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                if (isNew) "Crear nueva planta" else "Editar datos",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = commonName,
                onValueChange = { commonName = it },
                label = { Text("Nombre comun *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = textFieldColors
            )

            OutlinedTextField(
                value = commonNames,
                onValueChange = { commonNames = it },
                label = { Text("Otros nombres comunes (separados por comas)") },
                placeholder = { Text("Ej: belladona, tabaco bordo, hierba mora mayor") },
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors
            )

            OutlinedTextField(
                value = scientificName,
                onValueChange = { scientificName = it },
                label = { Text("Nombre cientifico") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = textFieldColors
            )

            OutlinedTextField(
                value = family,
                onValueChange = { family = it },
                label = { Text("Familia") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = textFieldColors
            )

            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Categoria") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = textFieldColors
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = toxicityLevel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Nivel de toxicidad") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = textFieldColors
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    toxicityOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option, color = Color.White) },
                            onClick = {
                                toxicityLevel = option
                                expanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = toxicParts,
                onValueChange = { toxicParts = it },
                label = { Text("Partes toxicas") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                colors = textFieldColors
            )

            OutlinedTextField(
                value = symptoms,
                onValueChange = { symptoms = it },
                label = { Text("Sintomas") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                colors = textFieldColors
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descripcion") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                colors = textFieldColors
            )

            OutlinedTextField(
                value = habitat,
                onValueChange = { habitat = it },
                label = { Text("Habitat") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                colors = textFieldColors
            )

            OutlinedTextField(
                value = geographicDistribution,
                onValueChange = { geographicDistribution = it },
                label = { Text("Distribucion geografica") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                colors = textFieldColors
            )

            OutlinedTextField(
                value = firstAid,
                onValueChange = { firstAid = it },
                label = { Text("Primeros auxilios") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                colors = textFieldColors
            )

            OutlinedTextField(
                value = imageUrl,
                onValueChange = { imageUrl = it },
                label = { Text("URL de imagen") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = textFieldColors
            )

            // ══════════════════════════════════════════════════════════
            // CAMPOS DE FENOLOGÍA
            // ══════════════════════════════════════════════════════════
            Text(
                "📅 Fenología (meses del año)",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50)
            )
            Text(
                "Indica los meses con números separados por comas. Ejemplo: 3,4,5 (marzo, abril, mayo)",
                fontSize = 12.sp,
                color = Color.Gray
            )

            OutlinedTextField(
                value = floweringMonths,
                onValueChange = { floweringMonths = it },
                label = { Text("🌸 Meses de floración") },
                placeholder = { Text("Ej: 5,6,7") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = textFieldColors
            )

            OutlinedTextField(
                value = fruitingMonths,
                onValueChange = { fruitingMonths = it },
                label = { Text("🍎 Meses de fructificación") },
                placeholder = { Text("Ej: 9,10") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = textFieldColors
            )

            OutlinedTextField(
                value = maxToxicityMonths,
                onValueChange = { maxToxicityMonths = it },
                label = { Text("☠️ Meses de toxicidad máxima") },
                placeholder = { Text("Ej: 6,7,8,9") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = textFieldColors
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (commonName.isNotBlank()) {
                        val plant = buildPlantForSave()
                        viewModel.insertPlant(plant)
                        if (onSaved != null && plant.id != 0) {
                            onSaved(plant.id)
                        } else {
                            onBack()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2E7D32)
                )
            ) {
                Text(
                    if (isNew) "Crear planta" else "Guardar cambios",
                    fontSize = 18.sp
                )
            }

            if (canSaveAndNext) {
                OutlinedButton(
                    onClick = {
                        val nextId = nextPlantInContext?.id ?: return@OutlinedButton
                        if (commonName.isNotBlank()) {
                            val plant = buildPlantForSave()
                            saveScope.launch {
                                viewModel.insertPlantSync(plant)
                                onSaveAndNext?.invoke(nextId)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("Guardar y siguiente", fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}