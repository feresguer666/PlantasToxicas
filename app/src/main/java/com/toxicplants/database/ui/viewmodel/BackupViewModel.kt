package com.toxicplants.database.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.toxicplants.database.PlantDatabase
import com.toxicplants.database.data.repository.BackupManifest
import com.toxicplants.database.data.repository.BackupRepository
import com.toxicplants.database.data.repository.PhotoCompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupViewModel(application: Application) : AndroidViewModel(application) {
    private val db = PlantDatabase.getDatabase(application)
    private val backupRepository = BackupRepository(application, db)

    private val _backupStatus = MutableLiveData<BackupStatus>(BackupStatus.Idle)
    val backupStatus: LiveData<BackupStatus> = _backupStatus

    private val _progress = MutableLiveData<BackupProgress?>(null)
    val progress: LiveData<BackupProgress?> = _progress

    // ── Configuración persistente en memoria (la UI puede modificar) ────
    var photoPreset: PhotoCompressor.Preset = PhotoCompressor.Preset.LOW
    var lastBackupType: BackupType = BackupType.FULL

    enum class BackupType { FULL, INCREMENTAL }

    // ── Export ──────────────────────────────────────────────────────────

    fun exportDatabase(
        uri: Uri,
        type: BackupType = BackupType.FULL,
        preset: PhotoCompressor.Preset = photoPreset
    ) {
        lastBackupType = type
        photoPreset = preset
        _backupStatus.value = BackupStatus.Loading
        _progress.value = BackupProgress("Iniciando…", 0, 1)
        viewModelScope.launch {
            val result = backupRepository.exportDatabaseToUri(
                uri = uri,
                progress = { phase, cur, total ->
                    _progress.postValue(BackupProgress(phase, cur, total))
                },
                recompression = preset,
                incremental = (type == BackupType.INCREMENTAL)
            )
            _progress.value = null
            _backupStatus.value = result.fold(
                onSuccess = {
                    val label = if (type == BackupType.INCREMENTAL) "incremental" else "completa"
                    BackupStatus.Success("Copia $label guardada correctamente")
                },
                onFailure = { e ->
                    BackupStatus.Error("Error al guardar: ${e.message ?: e::class.java.simpleName}")
                }
            )
        }
    }

    fun importDatabase(uri: Uri) {
        _backupStatus.value = BackupStatus.Loading
        _progress.value = BackupProgress("Iniciando…", 0, 1)
        viewModelScope.launch {
            val result = backupRepository.importDatabaseFromUri(uri) { phase, cur, total ->
                _progress.postValue(BackupProgress(phase, cur, total))
            }
            _progress.value = null
            _backupStatus.value = result.fold(
                onSuccess = { BackupStatus.Success("Datos restaurados correctamente") },
                onFailure = { e -> BackupStatus.Error("Error al restaurar: ${e.message ?: e::class.java.simpleName}") }
            )
        }
    }

    // ── Recompresión IN-PLACE de fotos del móvil ────────────────────────

    private val _recompressionResult = MutableLiveData<BackupRepository.RecompressionResult?>(null)
    val recompressionResult: LiveData<BackupRepository.RecompressionResult?> = _recompressionResult

    fun recompressLocalPhotos(preset: PhotoCompressor.Preset) {
        photoPreset = preset
        _backupStatus.value = BackupStatus.Loading
        _progress.value = BackupProgress("Comprimiendo…", 0, 1)
        viewModelScope.launch {
            val result = backupRepository.recompressLocalPhotos(preset) { phase, cur, total ->
                _progress.postValue(BackupProgress(phase, cur, total))
            }
            _progress.value = null
            _backupStatus.value = result.fold(
                onSuccess = { r ->
                    _recompressionResult.value = r
                    val mb = r.savedBytes / 1024f / 1024f
                    BackupStatus.Success(
                        "Comprimidas ${r.processed}/${r.totalFiles} fotos · " +
                                "liberados ${"%.1f".format(mb)} MB (${r.savedPercent}%)"
                    )
                },
                onFailure = { e -> BackupStatus.Error("Error al comprimir: ${e.message}") }
            )
        }
    }

    fun clearRecompressionResult() { _recompressionResult.value = null }

    // ── Info para la UI ─────────────────────────────────────────────────

    suspend fun getIncrementalPreview(): BackupRepository.IncrementalPreview =
        withContext(Dispatchers.IO) { backupRepository.incrementalPreview() }

    suspend fun getLocalPhotosStats(): Pair<Int, Long> = backupRepository.localPhotosStats()

    fun formatLastBackupAt(timestamp: Long): String {
        if (timestamp <= 0L) return "Nunca"
        return SimpleDateFormat("d/MM/yyyy HH:mm", Locale.getDefault())
            .format(Date(timestamp))
    }

    fun clearManifest() {
        BackupManifest.clear(getApplication())
    }

    // ── Nombre sugerido ─────────────────────────────────────────────────

    fun getSuggestedFileName(
        compressed: Boolean = true,
        type: BackupType = lastBackupType
    ): String {
        val ext = if (compressed) "json.gz" else "json"
        val tag = if (type == BackupType.INCREMENTAL) "Incremental" else "Completo"
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())
        return "PlantasToxicas_Backup_${tag}_${dateFormat.format(Date())}.$ext"
    }

    fun resetStatus() {
        _backupStatus.value = BackupStatus.Idle
    }
}

data class BackupProgress(
    val phase: String,
    val current: Int,
    val total: Int
) {
    val percent: Int get() = if (total <= 0) 0 else ((current.toLong() * 100) / total).toInt().coerceIn(0, 100)
}

sealed class BackupStatus {
    object Idle : BackupStatus()
    object Loading : BackupStatus()
    data class Success(val message: String) : BackupStatus()
    data class Error(val message: String) : BackupStatus()
}
