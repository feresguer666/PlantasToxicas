package com.toxicplants.database.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.toxicplants.database.PlantDatabase
import com.toxicplants.database.data.repository.BackupRepository
import kotlinx.coroutines.launch

class BackupViewModel(application: Application) : AndroidViewModel(application) {
    private val db = PlantDatabase.getDatabase(application)
    private val backupRepository = BackupRepository(application, db)

    private val _backupStatus = MutableLiveData<BackupStatus>(BackupStatus.Idle)
    val backupStatus: LiveData<BackupStatus> = _backupStatus

    fun exportDatabase(uri: Uri) {
        _backupStatus.value = BackupStatus.Loading
        viewModelScope.launch {
            val success = backupRepository.exportDatabaseToUri(uri)
            _backupStatus.value = if (success) BackupStatus.Success("Copia de seguridad guardada correctamente") else BackupStatus.Error("Error al guardar copia de seguridad")
        }
    }

    fun importDatabase(uri: Uri) {
        _backupStatus.value = BackupStatus.Loading
        viewModelScope.launch {
            val success = backupRepository.importDatabaseFromUri(uri)
            _backupStatus.value = if (success) BackupStatus.Success("Datos restaurados correctamente") else BackupStatus.Error("Error al restaurar los datos")
        }
    }

    fun getSuggestedFileName(): String = backupRepository.getSuggestedFileName()
    
    fun resetStatus() {
        _backupStatus.value = BackupStatus.Idle
    }
}

sealed class BackupStatus {
    object Idle : BackupStatus()
    object Loading : BackupStatus()
    data class Success(val message: String) : BackupStatus()
    data class Error(val message: String) : BackupStatus()
}
