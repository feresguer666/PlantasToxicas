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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPlantScreen(
    plantId: Int?,
    viewModel: PlantViewModel,
    onBack: () -> Unit
) {
    val isNew = plantId == null || plantId == 0
    val existingPlant = if (!isNew) {
        viewModel.getPlantById(plantId!!).observeAsState().value
    } else null

    var commonName by remember(existingPlant) { mutableStateOf(existingPlant?.commonName ?: "") }
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

    var expanded by remember { mutableStateOf(false) }
    val toxicityOptions = listOf("Mortal", "Alto", "Moderado", "Bajo")

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
                                val plant = PlantEntity(
                                    id = existingPlant?.id ?: 0,
                                    commonName = commonName,
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
                                    category = category
                                )
                                viewModel.insertPlant(plant)
                                onBack()
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
                singleLine = true
            )

            OutlinedTextField(
                value = scientificName,
                onValueChange = { scientificName = it },
                label = { Text("Nombre cientifico") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = family,
                onValueChange = { family = it },
                label = { Text("Familia") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Categoria") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
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
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    toxicityOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
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
                minLines = 2
            )

            OutlinedTextField(
                value = symptoms,
                onValueChange = { symptoms = it },
                label = { Text("Sintomas") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descripcion") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            OutlinedTextField(
                value = habitat,
                onValueChange = { habitat = it },
                label = { Text("Habitat") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            OutlinedTextField(
                value = geographicDistribution,
                onValueChange = { geographicDistribution = it },
                label = { Text("Distribucion geografica") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            OutlinedTextField(
                value = firstAid,
                onValueChange = { firstAid = it },
                label = { Text("Primeros auxilios") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            OutlinedTextField(
                value = imageUrl,
                onValueChange = { imageUrl = it },
                label = { Text("URL de imagen") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (commonName.isNotBlank()) {
                        val plant = PlantEntity(
                            id = existingPlant?.id ?: 0,
                            commonName = commonName,
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
                            category = category
                        )
                        viewModel.insertPlant(plant)
                        onBack()
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

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
