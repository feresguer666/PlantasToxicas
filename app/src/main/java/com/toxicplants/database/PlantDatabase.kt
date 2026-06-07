package com.toxicplants.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [PlantEntity::class, CompoundEntity::class, MushroomEntity::class],
    version = 7,
    exportSchema = true
)
abstract class PlantDatabase : RoomDatabase() {

    abstract fun plantDao(): PlantDao
    abstract fun compoundDao(): CompoundDao
    abstract fun mushroomDao(): MushroomDao

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
         * v3 → v4: añade columna `pubchemCid` a la tabla `compounds`.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE compounds ADD COLUMN pubchemCid INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * v4 → v5: crea la tabla `mushrooms` para la sección Setas tóxicas.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `mushrooms` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `commonName` TEXT NOT NULL,
                        `scientificName` TEXT NOT NULL,
                        `family` TEXT NOT NULL,
                        `toxicityLevel` TEXT NOT NULL,
                        `syndrome` TEXT NOT NULL,
                        `toxicCompounds` TEXT NOT NULL,
                        `onsetTime` TEXT NOT NULL,
                        `symptoms` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `habitat` TEXT NOT NULL,
                        `season` TEXT NOT NULL,
                        `geographicDistribution` TEXT NOT NULL,
                        `edibleConfusions` TEXT NOT NULL,
                        `firstAid` TEXT NOT NULL,
                        `treatment` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `imageUrl` TEXT NOT NULL,
                        `isDeadly` INTEGER NOT NULL,
                        `isFavorite` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_mushrooms_scientificName` ON `mushrooms` (`scientificName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_mushrooms_toxicityLevel` ON `mushrooms` (`toxicityLevel`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_mushrooms_syndrome` ON `mushrooms` (`syndrome`)")
            }
        }

        /**
         * v5 → v6: migración defensiva para instalaciones que llegaron a v5
         * durante el desarrollo de la sección de setas. Asegura que la tabla
         * `mushrooms` exista antes de validar el esquema final.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `mushrooms` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `commonName` TEXT NOT NULL,
                        `scientificName` TEXT NOT NULL,
                        `family` TEXT NOT NULL,
                        `toxicityLevel` TEXT NOT NULL,
                        `syndrome` TEXT NOT NULL,
                        `toxicCompounds` TEXT NOT NULL,
                        `onsetTime` TEXT NOT NULL,
                        `symptoms` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `habitat` TEXT NOT NULL,
                        `season` TEXT NOT NULL,
                        `geographicDistribution` TEXT NOT NULL,
                        `edibleConfusions` TEXT NOT NULL,
                        `firstAid` TEXT NOT NULL,
                        `treatment` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `imageUrl` TEXT NOT NULL,
                        `isDeadly` INTEGER NOT NULL,
                        `isFavorite` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_mushrooms_scientificName` ON `mushrooms` (`scientificName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_mushrooms_toxicityLevel` ON `mushrooms` (`toxicityLevel`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_mushrooms_syndrome` ON `mushrooms` (`syndrome`)")
            }
        }

        /**
         * v6 → v7: añade la columna `commonNames` a `plants`
         * (nombres comunes/populares adicionales, separados por comas).
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE plants ADD COLUMN commonNames TEXT NOT NULL DEFAULT ''")
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
