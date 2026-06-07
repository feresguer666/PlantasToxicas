package com.toxicplants.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Verifica que la [PlantDatabase.MIGRATION_1_2] migre correctamente datos creados
 * en la versión 1 del esquema sin perder información.
 *
 * Requiere que el esquema v1 esté volcado en
 * `app/schemas/com.toxicplants.database.PlantDatabase/1.json`
 * (lo genera KSP gracias al argumento `room.schemaLocation` de `app/build.gradle.kts`).
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val dbName = "migration-test-db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        PlantDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate1To2_keepsExistingRow_andAddsNullableLocationColumns() {
        // 1) Creamos la BD en versión 1 con una fila típica.
        helper.createDatabase(dbName, 1).apply {
            execSQL(
                """
                INSERT INTO plants
                  (id, commonName, scientificName, family, toxicityLevel, toxicParts,
                   symptoms, description, habitat, geographicDistribution, firstAid,
                   imageUrl, isFavorite, category)
                VALUES
                  (1, 'Dieffenbachia', 'Dieffenbachia seguine', 'Araceae', 'Alta',
                   'Todas', 'Irritación', 'Planta de interior', 'Interior', 'América',
                   'Lavar la boca', 'https://example.com/img.jpg', 0, 'Doméstica')
                """.trimIndent()
            )
            close()
        }

        // 2) Migramos a la v2.
        helper.runMigrationsAndValidate(
            dbName,
            2,
            true,
            PlantDatabase.MIGRATION_1_2
        )

        // 3) Abrimos la BD ya migrada con Room y comprobamos que:
        //    - la fila original sobrevive
        //    - las nuevas columnas existen y son null
        val db = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            PlantDatabase::class.java,
            dbName
        )
            .addMigrations(PlantDatabase.MIGRATION_1_2)
            .build()

        db.openHelper.writableDatabase.query(
            "SELECT id, commonName, latitude, longitude, locationName, foundDate, notes " +
                "FROM plants WHERE id = 1"
        ).use { cursor ->
            assertNotNull(cursor)
            assertEquals(true, cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
            assertEquals("Dieffenbachia", cursor.getString(1))
            assertEquals(true, cursor.isNull(2)) // latitude
            assertEquals(true, cursor.isNull(3)) // longitude
            assertEquals(true, cursor.isNull(4)) // locationName
            assertEquals(true, cursor.isNull(5)) // foundDate
            assertEquals(true, cursor.isNull(6)) // notes
        }

        db.close()
    }
}
