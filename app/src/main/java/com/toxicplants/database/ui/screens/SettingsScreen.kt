package com.toxicplants.database.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.ui.LocalImageCache
import kotlinx.coroutines.launch
import com.toxicplants.database.ui.viewmodel.BackupStatus
import com.toxicplants.database.ui.viewmodel.BackupViewModel
import com.toxicplants.database.ui.viewmodel.PlantViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToDownloadImages: () -> Unit = {},
    onNavigateToIncompletePlants: () -> Unit = {},
    onNavigateToPlantsWithNotes: () -> Unit = {},
    onNavigateToPlantsWithMarkers: () -> Unit = {}
) {
    val context = LocalContext.current
    val backupViewModel: BackupViewModel = viewModel()
    val plantViewModel: PlantViewModel = viewModel()

    val themeMode by com.toxicplants.database.ui.theme.ThemeManager.themeMode.collectAsState()
    val darkModeEnabled = themeMode == "dark" || (themeMode == "system" && androidx.compose.foundation.isSystemInDarkTheme())
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Estadísticas de imágenes descargadas (se recalculan al abrir la pantalla)
    var imageCount by remember { mutableIntStateOf(LocalImageCache.imageCount(context)) }
    var imageSizeMb by remember {
        mutableFloatStateOf(LocalImageCache.totalSizeBytes(context) / 1024f / 1024f)
    }

    // Función para refrescar las estadísticas de imágenes
    fun refreshImageStats() {
        imageCount  = LocalImageCache.imageCount(context)
        imageSizeMb = LocalImageCache.totalSizeBytes(context) / 1024f / 1024f
    }

    // Estado del backup
    val backupStatus by backupViewModel.backupStatus.observeAsState(BackupStatus.Idle)
    val backupProgress by backupViewModel.progress.observeAsState(null)

    // Estado del tipo de backup que el usuario eligió antes de abrir el SAF
    var pendingBackupType by remember {
        mutableStateOf(com.toxicplants.database.ui.viewmodel.BackupViewModel.BackupType.FULL)
    }
    var photoPreset by remember {
        mutableStateOf(com.toxicplants.database.data.repository.PhotoCompressor.Preset.LOW)
    }
    var showPresetDialog by remember { mutableStateOf(false) }
    var showRecompressDialog by remember { mutableStateOf(false) }
    var showIncrementalPreview by remember {
        mutableStateOf<com.toxicplants.database.data.repository.BackupRepository.IncrementalPreview?>(null)
    }
    var localPhotosStats by remember { mutableStateOf<Pair<Int, Long>?>(null) }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    var showDeletedPlantsDialog by remember { mutableStateOf(false) }
    var deletedSeedPlants by remember { mutableStateOf<List<PlantEntity>>(emptyList()) }
    var loadingDeletedPlants by remember { mutableStateOf(false) }

    fun loadDeletedPlants(openDialog: Boolean = true) {
        coroutineScope.launch {
            loadingDeletedPlants = true
            deletedSeedPlants = plantViewModel.getDeletedSeedPlants()
            loadingDeletedPlants = false
            if (openDialog) showDeletedPlantsDialog = true
        }
    }

    // Cargar stats de fotos locales al entrar
    LaunchedEffect(Unit) {
        localPhotosStats = backupViewModel.getLocalPhotosStats()
    }

    // Launcher para exportar (MIME "*/*" para que Android respete la extensión .json.gz)
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        if (uri != null) {
            backupViewModel.exportDatabase(uri, pendingBackupType, photoPreset)
        } else {
            backupViewModel.resetStatus()
        }
    }

    // Launcher para importar (acepta cualquier tipo: .json y .json.gz)
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { backupViewModel.importDatabase(it) } ?: backupViewModel.resetStatus()
    }

    // Mostrar resultado de backup
    LaunchedEffect(backupStatus) {
        when (val status = backupStatus) {
            is BackupStatus.Success -> {
                Toast.makeText(context, "✅ ${status.message}", Toast.LENGTH_LONG).show()
                backupViewModel.resetStatus()
            }
            is BackupStatus.Error -> {
                Toast.makeText(context, "❌ ${status.message}", Toast.LENGTH_LONG).show()
                backupViewModel.resetStatus()
            }
            else -> {}
        }
    }

    // ── Diálogo de confirmación para borrar imágenes ─────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("¿Borrar imágenes?") },
            text  = {
                Text(
                    "Se borrarán $imageCount fotos descargadas " +
                            "(${String.format("%.1f", imageSizeMb)} MB).\n\n" +
                            "La app las volverá a buscar automáticamente cuando " +
                            "entres al detalle de cada planta."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        LocalImageCache.deleteAll(context)
                        refreshImageStats()
                        showDeleteDialog = false
                        Toast.makeText(
                            context,
                            "🗑️ Imágenes eliminadas",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD32F2F)
                    )
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Borrar todo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // ── Diálogo de progreso de copia/restauración ─────────────────────────
    if (backupStatus is BackupStatus.Loading) {
        AlertDialog(
            onDismissRequest = { /* no se puede cancelar a mitad */ },
            title = { Text("Procesando copia…") },
            text = {
                Column {
                    val p = backupProgress
                    Text(p?.phase ?: "Iniciando…")
                    Spacer(Modifier.height(12.dp))
                    if (p != null && p.total > 0) {
                        LinearProgressIndicator(
                            progress = { p.percent / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(6.dp))
                        Text("${p.percent}%  (${p.current}/${p.total})", fontSize = 12.sp)
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "No cierres la app hasta que termine. Las copias grandes pueden tardar varios minutos.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            },
            confirmButton = {}
        )
    }

    // ── Diálogo: elegir calidad de recompresión ────────────────────────────
    if (showPresetDialog) {
        val presets = com.toxicplants.database.data.repository.PhotoCompressor.Preset.values()
        AlertDialog(
            onDismissRequest = { showPresetDialog = false },
            title = { Text("Calidad de las fotos") },
            text = {
                Column {
                    Text(
                        "Elige cuánto comprimir las fotos al exportar. Las fotos del móvil " +
                                "no se tocan (a menos que uses 'Comprimir fotos del móvil').",
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    presets.forEach { p ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    photoPreset = p
                                    showPresetDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = photoPreset == p,
                                onClick = {
                                    photoPreset = p
                                    showPresetDialog = false
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(p.label, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPresetDialog = false }) { Text("Cerrar") }
            }
        )
    }

    // ── Diálogo: confirmar comprimir fotos del móvil (DESTRUCTIVO) ────────
    if (showRecompressDialog) {
        val presets = com.toxicplants.database.data.repository.PhotoCompressor.Preset.values()
            .filter { it != com.toxicplants.database.data.repository.PhotoCompressor.Preset.ORIGINAL }
        var chosen by remember {
            mutableStateOf(com.toxicplants.database.data.repository.PhotoCompressor.Preset.LOW)
        }
        AlertDialog(
            onDismissRequest = { showRecompressDialog = false },
            title = { Text("⚠️ Comprimir fotos del móvil") },
            text = {
                Column {
                    val (n, bytes) = localPhotosStats ?: (0 to 0L)
                    val mb = bytes / 1024f / 1024f
                    Text(
                        "Tienes $n fotos (${"%.1f".format(mb)} MB).",
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Esta operación SUSTITUYE las fotos originales por una versión más " +
                                "pequeña. Es IRREVERSIBLE. Para conservar los originales, " +
                                "haz primero una copia completa.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Calidad de salida:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    presets.forEach { p ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { chosen = p }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = chosen == p, onClick = { chosen = p })
                            Spacer(Modifier.width(8.dp))
                            Text(p.label, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showRecompressDialog = false
                    backupViewModel.recompressLocalPhotos(chosen)
                    // refrescar stats al terminar
                    coroutineScope.launch {
                        kotlinx.coroutines.delay(500)
                        localPhotosStats = backupViewModel.getLocalPhotosStats()
                        refreshImageStats()
                    }
                }) { Text("COMPRIMIR", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showRecompressDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // ── Diálogo: preview de backup incremental ─────────────────────────────
    showIncrementalPreview?.let { preview ->
        AlertDialog(
            onDismissRequest = { showIncrementalPreview = null },
            title = { Text("Copia incremental") },
            text = {
                Column {
                    if (!preview.hasPreviousBackup) {
                        Text(
                            "⚠️ No hay copia completa registrada en el móvil. " +
                                    "La incremental solo guarda datos; haz una copia completa si quieres respaldar fotos.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp
                        )
                    } else {
                        Text(
                            "Última copia: " + backupViewModel.formatLastBackupAt(preview.previousBackupAt),
                            fontSize = 13.sp
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Resumen de lo que se incluirá:", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text("• Todos los textos: ${preview.totalPlantsCount} plantas",
                        fontSize = 13.sp)
                    Text(
                        "• Fotos: no se incluyen en la incremental (${preview.totalPhotosCount} fotos quedan cubiertas por copias completas)",
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Esta copia será mucho más pequeña: guarda datos editables y deja las fotos para la copia completa.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showIncrementalPreview = null
                    pendingBackupType =
                        com.toxicplants.database.ui.viewmodel.BackupViewModel.BackupType.INCREMENTAL
                    exportLauncher.launch(
                        backupViewModel.getSuggestedFileName(
                            compressed = true,
                            type = pendingBackupType
                        )
                    )
                }) { Text("Continuar") }
            },
            dismissButton = {
                TextButton(onClick = { showIncrementalPreview = null }) { Text("Cancelar") }
            }
        )
    }

    // ── Diálogo: papelera de plantas borradas ──────────────────────────────
    if (showDeletedPlantsDialog) {
        AlertDialog(
            onDismissRequest = { showDeletedPlantsDialog = false },
            title = { Text("🗑️ Papelera de plantas") },
            text = {
                Column {
                    Text(
                        "Fichas del catálogo base que has eliminado manualmente. Puedes restaurarlas si las borraste por error.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    if (loadingDeletedPlants) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Cargando…", fontSize = 13.sp)
                        }
                    } else if (deletedSeedPlants.isEmpty()) {
                        Text("No hay plantas en la papelera.", fontSize = 14.sp)
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 360.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(deletedSeedPlants, key = { it.id }) { plant ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(plant.commonName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(
                                                "#${plant.id} · ${plant.scientificName}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        TextButton(onClick = {
                                            plantViewModel.restoreDeletedPlant(plant.id)
                                            deletedSeedPlants = deletedSeedPlants.filter { it.id != plant.id }
                                            Toast.makeText(context, "Restaurada: ${plant.commonName}", Toast.LENGTH_SHORT).show()
                                        }) {
                                            Text("Restaurar")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = deletedSeedPlants.isNotEmpty() && !loadingDeletedPlants,
                    onClick = {
                        plantViewModel.restoreAllDeletedPlants()
                        Toast.makeText(context, "Plantas restauradas", Toast.LENGTH_SHORT).show()
                        deletedSeedPlants = emptyList()
                        showDeletedPlantsDialog = false
                    }
                ) { Text("Restaurar todas") }
            },
            dismissButton = {
                TextButton(onClick = { showDeletedPlantsDialog = false }) { Text("Cerrar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("⚙️ Ajustes", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ══════════════════════════════════════════════════════════
            // SECCIÓN: APARIENCIA
            // ══════════════════════════════════════════════════════════
            item {
                SettingsSectionTitle("APARIENCIA")
            }

            item {
                SettingsCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (darkModeEnabled) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint     = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Modo Oscuro", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                if (darkModeEnabled) "Activado" else "Desactivado",
                                fontSize = 12.sp,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked         = darkModeEnabled,
                            onCheckedChange = {
                                com.toxicplants.database.ui.theme.ThemeManager.setMode(
                                    context,
                                    if (it) "dark" else "light"
                                )
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            // ══════════════════════════════════════════════════════════
            // SECCIÓN: IMÁGENES
            // ══════════════════════════════════════════════════════════
            item {
                Spacer(Modifier.height(4.dp))
                SettingsSectionTitle("IMÁGENES")
            }

            // Card: Descargar imágenes (masivo)
            item {
                SettingsCard(modifier = Modifier.clickable { onNavigateToDownloadImages() }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            tint     = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Descargar imágenes",
                                fontWeight = FontWeight.Bold,
                                fontSize   = 16.sp
                            )
                            Text(
                                "Guardar fotos de todas las plantas en el dispositivo",
                                fontSize = 12.sp,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Card: Gestión de imágenes descargadas ──────────────────
            item {
                SettingsCard {
                    Column(modifier = Modifier.fillMaxWidth()) {

                        // Cabecera
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = null,
                                tint     = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Imágenes descargadas",
                                    fontWeight = FontWeight.Bold,
                                    fontSize   = 16.sp
                                )
                                Text(
                                    if (imageCount == 0) "No hay imágenes guardadas"
                                    else "$imageCount fotos · ${String.format("%.1f", imageSizeMb)} MB",
                                    fontSize = 12.sp,
                                    color    = if (imageCount == 0)
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    else
                                        Color(0xFF2E7D32)
                                )
                            }
                        }

                        // Barra de progreso visual (porcentaje del límite de 300 MB)
                        if (imageCount > 0) {
                            Spacer(Modifier.height(12.dp))
                            val progress = (imageSizeMb / 1536f).coerceIn(0f, 1f)
                            val barColor = when {
                                progress > 0.80f -> Color(0xFFD32F2F)
                                progress > 0.50f -> Color(0xFFF57C00)
                                else             -> Color(0xFF2E7D32)
                            }
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color            = barColor,
                                trackColor       = barColor.copy(alpha = 0.15f),
                            )
                            Text(
                                "${String.format("%.1f", imageSizeMb)} MB de 1536 MB",
                                fontSize = 10.sp,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(12.dp))

                        // Botón borrar
                        OutlinedButton(
                            onClick  = { showDeleteDialog = true },
                            enabled  = imageCount > 0,
                            modifier = Modifier.fillMaxWidth(),
                            colors   = ButtonDefaults.outlinedButtonColors(
                                contentColor         = Color(0xFFD32F2F),
                                disabledContentColor = Color.Gray
                            )
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (imageCount == 0) "No hay imágenes que borrar"
                                else "Borrar imágenes descargadas",
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // ══════════════════════════════════════════════════════════
            // SECCIÓN: DATOS
            // ══════════════════════════════════════════════════════════
            item {
                Spacer(Modifier.height(4.dp))
                SettingsSectionTitle("DATOS")
            }

            // Card: Calidad de fotos (selector compartido por copia completa e incremental)
            item {
                SettingsCard(modifier = Modifier.clickable { showPresetDialog = true }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = null,
                            tint = Color(0xFF6A1B9A),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Calidad de fotos en backups",
                                fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                photoPreset.label,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Card: Copia COMPLETA
            item {
                SettingsCard(modifier = Modifier.clickable {
                    pendingBackupType = com.toxicplants.database.ui.viewmodel.BackupViewModel.BackupType.FULL
                    exportLauncher.launch(
                        backupViewModel.getSuggestedFileName(
                            compressed = true,
                            type = pendingBackupType
                        )
                    )
                }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint     = Color(0xFF2E7D32),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("📦 Copia COMPLETA",
                                fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                "Todos los datos + todas las fotos. Comprimido GZIP.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Card: Copia INCREMENTAL
            item {
                SettingsCard(modifier = Modifier.clickable {
                    coroutineScope.launch {
                        showIncrementalPreview = backupViewModel.getIncrementalPreview()
                    }
                }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = Color(0xFFEF6C00),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("⚡ Copia INCREMENTAL",
                                fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                "Solo datos/textos editables, sin fotos. Mucho más pequeña.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Card: COMPRIMIR fotos del móvil (destructivo)
            item {
                SettingsCard(modifier = Modifier.clickable { showRecompressDialog = true }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = null,
                            tint = Color(0xFFC62828),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("🗜️ Comprimir fotos del móvil",
                                fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            val (n, bytes) = localPhotosStats ?: (0 to 0L)
                            val mb = bytes / 1024f / 1024f
                            Text(
                                if (n > 0) "$n fotos · ${"%.1f".format(mb)} MB · libera espacio (IRREVERSIBLE)"
                                else "Sin fotos para comprimir",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Card: Importar
            item {
                SettingsCard(modifier = Modifier.clickable {
                    importLauncher.launch(arrayOf("application/json", "*/*"))
                }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint     = Color(0xFF1565C0),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Importar datos",
                                fontWeight = FontWeight.Bold,
                                fontSize   = 16.sp
                            )
                            Text(
                                "Restaurar desde archivo de respaldo",
                                fontSize = 12.sp,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Card: Calidad del catálogo / plantas incompletas ─────
            item {
                SettingsCard(modifier = Modifier.clickable { onNavigateToIncompletePlants() }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFF57C00),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "🧹 Plantas incompletas",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                "Revisar fichas sin imagen, familia, síntomas o datos clave",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Card: Plantas con notas ───────────────────────────
            item {
                SettingsCard(modifier = Modifier.clickable { onNavigateToPlantsWithNotes() }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Notes,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "📝 Plantas con notas",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                "Ver fichas con notas personales",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Card: Plantas con marcadores ────────────────────
            item {
                SettingsCard(modifier = Modifier.clickable { onNavigateToPlantsWithMarkers() }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Label,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "🏷️ Plantas con marcadores",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                "Ver fichas marcadas como Revisar, Pendiente foto, etc.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Card: Papelera de plantas borradas ──────────────────
            item {
                SettingsCard(modifier = Modifier.clickable { loadDeletedPlants(openDialog = true) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "🗑️ Papelera de plantas",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                "Restaurar fichas eliminadas manualmente",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Indicador de progreso de backup
            if (backupStatus is BackupStatus.Loading) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors   = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                        shape    = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color    = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(16.dp))
                            Text("Procesando...", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // ══════════════════════════════════════════════════════════
            // SECCIÓN: ACERCA DE
            // ══════════════════════════════════════════════════════════
            item {
                Spacer(Modifier.height(4.dp))
                SettingsSectionTitle("ACERCA DE")
            }

            item {
                SettingsCard {
                    Column(
                        modifier            = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🌿", fontSize = 48.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Plantas Tóxicas",
                            fontWeight = FontWeight.Bold,
                            fontSize   = 18.sp
                        )
                        Text(
                            "Versión 1.0.0",
                            fontSize = 12.sp,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Catálogo de plantas tóxicas",
                            fontSize = 12.sp,
                            color    = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Imágenes: iNaturalist · Wikimedia · GBIF · EOL",
                            fontSize = 10.sp,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Espacio final
            item { Spacer(Modifier.height(16.dp)) }

        } // fin LazyColumn
    } // fin Scaffold
}

// ── Composables auxiliares ────────────────────────────────────────────────

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text     = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color    = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier  = modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape     = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}
