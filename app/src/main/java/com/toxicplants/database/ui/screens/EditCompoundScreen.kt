package com.toxicplants.database.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toxicplants.database.CompoundEntity
import com.toxicplants.database.ui.viewmodel.CompoundViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCompoundScreen(
    compoundId: Int?,
    viewModel: CompoundViewModel,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var compound by remember { mutableStateOf<CompoundEntity?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Campos del formulario (adaptados a CompoundEntity)
    var commonName by remember { mutableStateOf("") }
    var iupacName by remember { mutableStateOf("") }
    var groupName by remember { mutableStateOf("") }
    var subgroup by remember { mutableStateOf("") }
    var molecularFormula by remember { mutableStateOf("") }
    var molecularWeight by remember { mutableStateOf("") }
    var sourcePlants by remember { mutableStateOf("") }
    var concentration by remember { mutableStateOf("") }
    var mechanism by remember { mutableStateOf("") }
    var ld50 by remember { mutableStateOf("") }
    var toxicDose by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    LaunchedEffect(compoundId) {
        if (compoundId != null && compoundId != 0) {
            val currentList = viewModel.allCompounds.value ?: emptyList()
            compound = currentList.find { it.id == compoundId }
            compound?.let {
                commonName = it.commonName
                iupacName = it.iupacName
                groupName = it.groupName
                subgroup = it.subgroup
                molecularFormula = it.molecularFormula
                molecularWeight = it.molecularWeight?.toString() ?: ""
                sourcePlants = it.sourcePlants
                concentration = it.concentration
                mechanism = it.mechanism
                ld50 = it.ld50
                toxicDose = it.toxicDose
                notes = it.notes
            }
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (compoundId == null || compoundId == 0) "Nuevo Componente" else "Editar Componente") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF512DA8),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(value = commonName, onValueChange = { commonName = it }, label = { Text("Nombre Común") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = iupacName, onValueChange = { iupacName = it }, label = { Text("Nombre IUPAC") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = groupName, onValueChange = { groupName = it }, label = { Text("Grupo Fitoquímico") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = subgroup, onValueChange = { subgroup = it }, label = { Text("Subgrupo") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = molecularFormula, onValueChange = { molecularFormula = it }, label = { Text("Fórmula Molecular") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = molecularWeight, onValueChange = { molecularWeight = it }, label = { Text("Peso Molecular (g/mol)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = sourcePlants, onValueChange = { sourcePlants = it }, label = { Text("Plantas que lo contienen") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(value = concentration, onValueChange = { concentration = it }, label = { Text("Concentración") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = mechanism, onValueChange = { mechanism = it }, label = { Text("Mecanismo de Toxicidad") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(value = ld50, onValueChange = { ld50 = it }, label = { Text("LD50") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = toxicDose, onValueChange = { toxicDose = it }, label = { Text("Dosis Tóxica") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notas Adicionales") }, modifier = Modifier.fillMaxWidth(), minLines = 3)

                Button(
                    onClick = {
                        val newCompound = CompoundEntity(
                            id = compound?.id ?: 0,
                            commonName = commonName,
                            iupacName = iupacName,
                            groupName = groupName,
                            subgroup = subgroup,
                            molecularFormula = molecularFormula,
                            molecularWeight = molecularWeight.toDoubleOrNull(),
                            sourcePlants = sourcePlants,
                            concentration = concentration,
                            mechanism = mechanism,
                            ld50 = ld50,
                            toxicDose = toxicDose,
                            notes = notes,
                            groupColor = compound?.groupColor ?: "#7B1FA2",
                            isFavorite = compound?.isFavorite ?: false,
                            clinicalNeuro = compound?.clinicalNeuro ?: "",
                            clinicalCardio = compound?.clinicalCardio ?: "",
                            clinicalDigestive = compound?.clinicalDigestive ?: "",
                            clinicalRespiratory = compound?.clinicalRespiratory ?: "",
                            clinicalDermal = compound?.clinicalDermal ?: "",
                            clinicalOther = compound?.clinicalOther ?: "",
                            onsetTime = compound?.onsetTime ?: "",
                            duration = compound?.duration ?: "",
                            treatment = compound?.treatment ?: ""
                        )
                        if (compound == null) {
                            viewModel.addCompound(newCompound)
                        } else {
                            viewModel.updateCompound(newCompound)
                        }
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) {
                    Text("Guardar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
