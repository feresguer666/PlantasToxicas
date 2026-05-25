package com.toxicplants.database.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
) {
    val compounds by viewModel.byGroup(groupName).observeAsState(emptyList())
    val groupColor = remember(compounds) {
        parseColor(compounds.firstOrNull()?.groupColor ?: "#512DA8")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(groupName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "${compounds.size} sustancia${if (compounds.size == 1) "" else "s"}",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f),
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = groupColor,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(compounds, key = { it.id }) { c ->
                CompoundRow(c, onClick = { onCompoundClick(c) })
            }
        }
    }
}
