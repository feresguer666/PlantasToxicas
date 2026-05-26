package com.toxicplants.database.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toxicplants.database.ui.viewmodel.BackupStatus
import com.toxicplants.database.ui.viewmodel.BackupViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: BackupViewModel = viewModel()
) {
    val backupStatus by viewModel.backupStatus.observeAsState(BackupStatus.Idle)
    val snackbarHostState = remember { SnackbarHostState() }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportDatabase(it) }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importDatabase(it) }
    }

    LaunchedEffect(backupStatus) {
        when (backupStatus) {
            is BackupStatus.Success -> {
                snackbarHostState.showSnackbar((backupStatus as BackupStatus.Success).message)
                viewModel.resetStatus()
            }
            is BackupStatus.Error -> {
                snackbarHostState.showSnackbar((backupStatus as BackupStatus.Error).message)
                viewModel.resetStatus()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes y Backup") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2E7D32),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Copia de Seguridad (Backup)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            
            Text("Guarda una copia de todas tus plantas, fotos locales y componentes fitoquímicos en tu móvil para no perderlos si desinstalas la app.", color = Color.Gray)

            Button(
                onClick = { createDocumentLauncher.launch(viewModel.getSuggestedFileName()) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Exportar Copia de Seguridad")
            }

            Button(
                onClick = { openDocumentLauncher.launch(arrayOf("application/json", "*/*")) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
            ) {
                Icon(Icons.Default.Upload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Restaurar Copia de Seguridad")
            }

            if (backupStatus is BackupStatus.Loading) {
                Spacer(Modifier.height(16.dp))
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}
