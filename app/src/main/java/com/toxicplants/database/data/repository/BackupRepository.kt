package com.toxicplants.database.data.repository

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.toxicplants.database.CompoundEntity
import com.toxicplants.database.PlantDatabase
import com.toxicplants.database.PlantEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupRepository(private val context: Context, private val db: PlantDatabase) {

    private val gson = Gson()

    data class BackupData(
        val plants: List<PlantEntity>,
        val compounds: List<CompoundEntity>
    )

    suspend fun exportDatabaseToUri(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val plants = db.plantDao().getAllPlantsSync()
            val compounds = db.compoundDao().getAllSync()
            val backupData = BackupData(plants, compounds)

            val jsonString = gson.toJson(backupData)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(jsonString)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun importDatabaseFromUri(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            var jsonString = ""
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    jsonString = reader.readText()
                }
            }

            if (jsonString.isBlank()) return@withContext false

            val type = object : TypeToken<BackupData>() {}.type
            val backupData: BackupData = gson.fromJson(jsonString, type)

            if (backupData.plants.isNotEmpty()) {
                db.plantDao().insertAll(backupData.plants)
            }
            if (backupData.compounds.isNotEmpty()) {
                db.compoundDao().insertAll(backupData.compounds)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getSuggestedFileName(): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())
        return "PlantasToxicas_Backup_${dateFormat.format(Date())}.json"
    }
}
