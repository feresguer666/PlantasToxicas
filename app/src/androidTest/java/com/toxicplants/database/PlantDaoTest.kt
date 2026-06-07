package com.toxicplants.database

import androidx.lifecycle.Observer
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Tests de humo para [PlantDao] usando una BD Room en memoria.
 */
@RunWith(AndroidJUnit4::class)
class PlantDaoTest {

    private lateinit var db: PlantDatabase
    private lateinit var dao: PlantDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, PlantDatabase::class.java)
            .allowMainThreadQueries() // OK en tests
            .build()
        dao = db.plantDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun sample(
        id: Int,
        common: String = "Adelfa",
        scientific: String = "Nerium oleander",
        toxicity: String = "Mortal",
        category: String = "Jardín",
    ) = PlantEntity(
        id = id,
        commonName = common,
        scientificName = scientific,
        family = "Apocynaceae",
        toxicityLevel = toxicity,
        toxicParts = "Todas",
        symptoms = "Cardiotoxicidad",
        description = "Arbusto mediterráneo",
        habitat = "Jardines, ramblas",
        geographicDistribution = "Cuenca mediterránea",
        firstAid = "Atención médica urgente",
        imageUrl = "",
        category = category,
    )

    @Test
    fun insertAndQueryById() = runTest {
        dao.insert(sample(1))
        val plant = dao.getPlantById(1)
        assertNotNull(plant)
        assertEquals("Adelfa", plant!!.commonName)
    }

    @Test
    fun toggleFavorite_updatesFlag() = runTest {
        dao.insert(sample(1))
        dao.toggleFavorite(1, true)
        assertEquals(true, dao.getPlantById(1)!!.isFavorite)
        dao.toggleFavorite(1, false)
        assertEquals(false, dao.getPlantById(1)!!.isFavorite)
    }

    @Test
    fun getPlantsByToxicity_filtersCorrectly() = runTest {
        dao.insertAll(
            listOf(
                sample(1, common = "Adelfa", toxicity = "Mortal"),
                sample(2, common = "Aloe", toxicity = "Bajo"),
                sample(3, common = "Cicuta", toxicity = "Mortal"),
            )
        )
        val mortales = dao.getPlantsByToxicitySync("Mortal")
        assertEquals(2, mortales.size)
        assertTrue(mortales.all { it.toxicityLevel == "Mortal" })
    }

    @Test
    fun updateLocation_persistsCoordinates() = runTest {
        dao.insert(sample(1))
        dao.updateLocation(
            plantId = 1,
            lat = 40.4168,
            lng = -3.7038,
            name = "Madrid",
            date = "2026-05-25",
            notes = "Visto en El Retiro"
        )
        val p = dao.getPlantById(1)!!
        assertEquals(40.4168, p.latitude!!, 0.0001)
        assertEquals(-3.7038, p.longitude!!, 0.0001)
        assertEquals("Madrid", p.locationName)
        assertEquals("2026-05-25", p.foundDate)
        assertEquals("Visto en El Retiro", p.notes)
    }

    @Test
    fun searchPlants_matchesCommonNameSubstring() = runTest {
        dao.insertAll(
            listOf(
                sample(1, common = "Adelfa", scientific = "Nerium oleander"),
                sample(2, common = "Aloe vera", scientific = "Aloe barbadensis"),
            )
        )
        val latch = CountDownLatch(1)
        var received: List<PlantEntity> = emptyList()
        val live = dao.searchPlants("%adel%")
        // observeForever necesita main looper en instrumentado
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync {
            live.observeForever(object : Observer<List<PlantEntity>> {
                override fun onChanged(value: List<PlantEntity>) {
                    received = value
                    latch.countDown()
                    live.removeObserver(this)
                }
            })
        }
        latch.await(2, TimeUnit.SECONDS)
        assertEquals(1, received.size)
        assertEquals("Adelfa", received.first().commonName)
    }
}
