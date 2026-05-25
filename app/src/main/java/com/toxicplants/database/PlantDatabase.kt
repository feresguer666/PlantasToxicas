package com.toxicplants.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [PlantEntity::class],
    version = 2,
    exportSchema = true
)
abstract class PlantDatabase : RoomDatabase() {

    abstract fun plantDao(): PlantDao

    companion object {

        /**
         * Migración 1 → 2.
         *
         * Añade los campos de geolocalización introducidos junto con la pantalla
         * [com.toxicplants.database.ui.screens.LocationScreen]:
         *   - latitude         (REAL, nullable)
         *   - longitude        (REAL, nullable)
         *   - locationName     (TEXT, nullable)
         *   - foundDate        (TEXT, nullable)
         *   - notes            (TEXT, nullable)
         *
         * Si un usuario tenía instalada la v1 (sin estos campos) y actualiza, Room
         * sin esta migración crashea con `IllegalStateException: Migration didn't
         * properly handle…`.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE plants ADD COLUMN latitude REAL")
                db.execSQL("ALTER TABLE plants ADD COLUMN longitude REAL")
                db.execSQL("ALTER TABLE plants ADD COLUMN locationName TEXT")
                db.execSQL("ALTER TABLE plants ADD COLUMN foundDate TEXT")
                db.execSQL("ALTER TABLE plants ADD COLUMN notes TEXT")
            }
        }

        @Volatile
        private var INSTANCE: PlantDatabase? = null

        fun getDatabase(context: Context): PlantDatabase {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext
                val instance = Room.databaseBuilder(
                    appContext,
                    PlantDatabase::class.java,
                    "plant_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Pre-carga del catálogo desde assets/plants.json.
                            INSTANCE?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    val plants = PlantDataSource.loadAll(appContext)
                                    database.plantDao().insertAll(plants)
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
