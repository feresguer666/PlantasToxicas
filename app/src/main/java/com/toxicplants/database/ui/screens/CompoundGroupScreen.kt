package com.toxicplants.database.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Science
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
import com.toxicplants.database.CompoundEntity
import com.toxicplants.database.ui.viewmodel.CompoundViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompoundGroupScreen(
    viewModel: CompoundViewModel,
    groupName: String,
    onCompoundClick: (CompoundEntity) -> Unit,
    onBack: () -> Unit,
    onEditCompound: (CompoundEntity) -> Unit = {}
) {
    val compounds by viewModel.byGroup(groupName).observeAsState(emptyList())
    var compoundToDelete by remember { mutableStateOf<CompoundEntity?>(null) }
    
    val groupColorStr = compounds.firstOrNull()?.groupColor ?: "#7B1FA2"
    val mainColor = parseColor(groupColorStr)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(groupName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = mainColor,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (compounds.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Filled.Science,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color.Gray
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("No hay sustancias en este grupo", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(compounds, key = { it.id }) { c ->
                        CompoundCard(
                            compound = c,
                            onClick = { onCompoundClick(c) },
                            mainColor = mainColor,
                            onEdit = { onEditCompound(c) },
                            onDelete = { compoundToDelete = c }
                        )
                    }
                }
            }
        }
    }

    compoundToDelete?.let { c ->
        AlertDialog(
            onDismissRequest = { compoundToDelete = null },
            title = { Text("Eliminar componente") },
            text = { Text("¿Estás seguro de que quieres eliminar '${c.commonName}'?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCompound(c)
                    compoundToDelete = null
                }) {
                    Text("Eliminar", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { compoundToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun CompoundCard(
    compound: CompoundEntity,
    onClick: () -> Unit,
    mainColor: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(mainColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Science, contentDescription = null, tint = mainColor)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = compound.commonName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = compound.molecularFormula,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    fontStyle = FontStyle.Italic,
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color.Gray)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red.copy(alpha=0.7f))
            }
        }
    }
}
