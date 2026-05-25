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
    entities = [PlantEntity::class, CompoundEntity::class],
    version = 3,
    exportSchema = true
)
abstract class PlantDatabase : RoomDatabase() {

    abstract fun plantDao(): PlantDao
    abstract fun compoundDao(): CompoundDao

    companion object {

        /**
         * v1 → v2: añade los campos de geolocalización a `plants`.
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

        /**
         * v2 → v3: crea la tabla `compounds` para la pantalla de Fitoquímica.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `compounds` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `commonName` TEXT NOT NULL,
                        `iupacName` TEXT NOT NULL,
                        `groupName` TEXT NOT NULL,
                        `subgroup` TEXT NOT NULL,
                        `molecularFormula` TEXT NOT NULL,
                        `molecularWeight` REAL,
                        `sourcePlants` TEXT NOT NULL,
                        `concentration` TEXT NOT NULL,
                        `mechanism` TEXT NOT NULL,
                        `ld50` TEXT NOT NULL,
                        `toxicDose` TEXT NOT NULL,
                        `clinicalNeuro` TEXT NOT NULL,
                        `clinicalCardio` TEXT NOT NULL,
                        `clinicalDigestive` TEXT NOT NULL,
                        `clinicalRespiratory` TEXT NOT NULL,
                        `clinicalDermal` TEXT NOT NULL,
                        `clinicalOther` TEXT NOT NULL,
                        `onsetTime` TEXT NOT NULL,
                        `duration` TEXT NOT NULL,
                        `treatment` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `groupColor` TEXT NOT NULL,
                        `isFavorite` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_compounds_groupName` ON `compounds` (`groupName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_compounds_commonName` ON `compounds` (`commonName`)")
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Pre-carga plantas + compuestos en la primera ejecución.
                            INSTANCE?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    database.plantDao().insertAll(PlantDataSource.loadAll(appContext))
                                    database.compoundDao().insertAll(CompoundDataSource.loadAll(appContext))
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
