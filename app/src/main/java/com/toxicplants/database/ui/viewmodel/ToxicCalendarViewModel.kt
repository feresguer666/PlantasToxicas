package com.toxicplants.database.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.toxicplants.database.PlantDatabase
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.ToxicCalendarDao
import com.toxicplants.database.ToxicCalendarEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class CalendarDay(
    val date: String,          // yyyy-MM-dd
    val dayOfMonth: Int,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val events: List<ToxicCalendarEvent> = emptyList(),
    /** Plantas que están en floración este mes (para la vista estacional). */
    val floweringPlants: List<PlantEntity> = emptyList(),
    /** Plantas que están en fructificación este mes. */
    val fruitingPlants: List<PlantEntity> = emptyList(),
    /** Plantas con toxicidad máxima este mes. */
    val maxToxPlants: List<PlantEntity> = emptyList()
)

data class CalendarMonth(
    val year: Int,
    val month: Int,            // 1-12
    val displayName: String,
    val weeks: List<List<CalendarDay>>
)

class ToxicCalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val calendarDao: ToxicCalendarDao =
        PlantDatabase.getDatabase(application).toxicCalendarDao()
    private val plantDao =
        PlantDatabase.getDatabase(application).plantDao()

    val allEvents: LiveData<List<ToxicCalendarEvent>> = calendarDao.getAllEvents()

    private val _currentMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1)
    val currentMonth: StateFlow<Int> = _currentMonth

    private val _currentYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val currentYear: StateFlow<Int> = _currentYear

    private val _allPlants = MutableStateFlow<List<PlantEntity>>(emptyList())
    val allPlants: StateFlow<List<PlantEntity>> = _allPlants

    private val _selectedDayEvents = MutableStateFlow<List<ToxicCalendarEvent>>(emptyList())
    val selectedDayEvents: StateFlow<List<ToxicCalendarEvent>> = _selectedDayEvents

    private val _selectedDate = MutableStateFlow<String?>(null)
    val selectedDate: StateFlow<String?> = _selectedDate

    /** Plantas relevantes para el mes actual (vista estacional). */
    private val _seasonalPlants = MutableStateFlow<SeasonalData>(SeasonalData())
    val seasonalPlants: StateFlow<SeasonalData> = _seasonalPlants

    /** Tab activo: 0 = Estacional, 1 = Fenología, 2 = Alertas */
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab

    /** Planta seleccionada para ver fenología. */
    private val _selectedPhenologyPlant = MutableStateFlow<PlantEntity?>(null)
    val selectedPhenologyPlant: StateFlow<PlantEntity?> = _selectedPhenologyPlant

    init {
        viewModelScope.launch {
            _allPlants.value = plantDao.getAllPlantsSync()
            refreshSeasonalData()
        }
    }

    fun selectTab(tab: Int) { _selectedTab.value = tab }
    fun selectPhenologyPlant(plant: PlantEntity?) { _selectedPhenologyPlant.value = plant }

    fun previousMonth() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, _currentYear.value)
            set(Calendar.MONTH, _currentMonth.value - 1)
            add(Calendar.MONTH, -1)
        }
        _currentMonth.value = cal.get(Calendar.MONTH) + 1
        _currentYear.value = cal.get(Calendar.YEAR)
        refreshSeasonalData()
    }

    fun nextMonth() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, _currentYear.value)
            set(Calendar.MONTH, _currentMonth.value - 1)
            add(Calendar.MONTH, 1)
        }
        _currentMonth.value = cal.get(Calendar.MONTH) + 1
        _currentYear.value = cal.get(Calendar.YEAR)
        refreshSeasonalData()
    }

    fun selectDate(date: String) {
        _selectedDate.value = date
        viewModelScope.launch {
            _selectedDayEvents.value = calendarDao.getEventsByDate(date)
        }
    }

    private fun refreshSeasonalData() {
        val month = _currentMonth.value
        val plants = _allPlants.value
        _seasonalPlants.value = SeasonalData(
            flowering = plants.filter { month in it.floweringMonths.toIntList() },
            fruiting = plants.filter { month in it.fruitingMonths.toIntList() },
            maxToxicity = plants.filter { month in it.maxToxicityMonths.toIntList() }
        )
    }

    /** Genera la cuadrícula de días del mes actual con eventos. */
    suspend fun generateCalendarMonth(): CalendarMonth {
        val year = _currentYear.value
        val month = _currentMonth.value
        val monthNames = listOf(
            "", "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        )
        val displayName = "${monthNames[month]} $year"

        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val firstDayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7  // Lun=0 … Dom=6
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Cargar eventos del mes
        val monthStr = String.format("%04d-%02d", year, month)
        val events = calendarDao.getEventsByMonth("$monthStr%")
        val eventsByDate = events.groupBy { it.date }

        val days = mutableListOf<CalendarDay>()

        // Días del mes anterior
        val prevCal = (cal.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        val prevDays = prevCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (i in firstDayOfWeek - 1 downTo 0) {
            val d = prevDays - i
            val pm = if (month == 1) 12 else month - 1
            val py = if (month == 1) year - 1 else year
            days.add(CalendarDay(
                date = String.format("%04d-%02d-%02d", py, pm, d),
                dayOfMonth = d,
                isCurrentMonth = false,
                isToday = false
            ))
        }

        // Días del mes actual
        for (d in 1..daysInMonth) {
            val dateStr = String.format("%04d-%02d-%02d", year, month, d)
            days.add(CalendarDay(
                date = dateStr,
                dayOfMonth = d,
                isCurrentMonth = true,
                isToday = dateStr == today,
                events = eventsByDate[dateStr] ?: emptyList()
            ))
        }

        // Días del mes siguiente para completar la última semana
        val remaining = 42 - days.size // 6 semanas × 7 días
        for (d in 1..remaining) {
            val nm = if (month == 12) 1 else month + 1
            val ny = if (month == 12) year + 1 else year
            days.add(CalendarDay(
                date = String.format("%04d-%02d-%02d", ny, nm, d),
                dayOfMonth = d,
                isCurrentMonth = false,
                isToday = false
            ))
        }

        val weeks = days.chunked(7)
        return CalendarMonth(year = year, month = month, displayName = displayName, weeks = weeks)
    }

    // ── CRUD de eventos ──

    fun addEvent(event: ToxicCalendarEvent) {
        viewModelScope.launch { calendarDao.insert(event) }
    }

    fun updateEvent(event: ToxicCalendarEvent) {
        viewModelScope.launch { calendarDao.update(event) }
    }

    fun deleteEvent(event: ToxicCalendarEvent) {
        viewModelScope.launch { calendarDao.delete(event) }
    }

    fun deleteEventById(id: Int) {
        viewModelScope.launch { calendarDao.deleteById(id) }
    }

    // ── Sembrar datos fenológicos si están vacíos ──

    fun seedPhenologyIfNeeded() {
        viewModelScope.launch {
            // Recargar plantas desde la BD por si acaso
            _allPlants.value = plantDao.getAllPlantsSync()
            val plants = _allPlants.value

            // Solo sembrar si hay plantas sin datos fenológicos
            val needsSeed = plants.any { it.floweringMonths.isBlank() && it.toxicityLevel in listOf("Mortal", "Muy alto", "Alto") }
            if (!needsSeed) return@launch

            val phenologyMap = getPhenologySeedData()
            var updated = 0

            for (plant in plants) {
                if (plant.floweringMonths.isNotBlank()) continue

                // Normalizar: solo género + especie (quitar autor botánico)
                val norm = normalizeScientificName(plant.scientificName)

                // Buscar en el map (con y sin autor)
                val entry = phenologyMap[norm]
                    ?: phenologyMap[plant.scientificName.lowercase().trim()]

                if (entry != null) {
                    plantDao.update(plant.copy(
                        floweringMonths = entry.flowering,
                        fruitingMonths = entry.fruiting,
                        maxToxicityMonths = entry.maxToxicity
                    ))
                    updated++
                }
            }

            if (updated > 0) {
                _allPlants.value = plantDao.getAllPlantsSync()
                refreshSeasonalData()
            }
        }
    }

    /** Normaliza un nombre científico tomando solo género + especie (2 primeras palabras). */
    private fun normalizeScientificName(name: String): String {
        val parts = name.trim().lowercase().split(Regex("\\s+"))
        return if (parts.size >= 2) "${parts[0]} ${parts[1]}" else name.trim().lowercase()
    }

    companion object {
        data class PhenologyEntry(val flowering: String, val fruiting: String, val maxToxicity: String)

        fun getPhenologySeedData(): Map<String, PhenologyEntry> = mapOf(
            // ── Mortales ──
            "aconitum napellus"               to PhenologyEntry("6,7,8",   "9,10",    "6,7,8,9"),
            "aconitum vulparia"               to PhenologyEntry("6,7,8",   "9,10",    "6,7,8,9"),
            "cicuta virosa"                   to PhenologyEntry("6,7,8",   "8,9,10",  "6,7,8,9"),
            "conium maculatum"                to PhenologyEntry("5,6,7",   "7,8,9",   "5,6,7,8"),
            "veratrum album"                  to PhenologyEntry("6,7,8",   "8,9",     "5,6,7,8"),
            "veratrum nigrum"                 to PhenologyEntry("6,7",     "8,9",     "5,6,7,8"),
            "taxus baccata"                   to PhenologyEntry("3,4",     "9,10",    "9,10,11"),
            "nerium oleander"                 to PhenologyEntry("6,7,8,9", "10,11",   "1,2,3,4,5,6,7,8,9,10,11,12"),
            "digitalis purpurea"              to PhenologyEntry("6,7",     "7,8,9",   "6,7,8"),
            "atropa belladonna"               to PhenologyEntry("6,7,8",   "9,10",    "8,9,10"),
            "datura stramonium"               to PhenologyEntry("7,8,9",   "10,11",   "7,8,9,10"),
            "colchicum autumnale"             to PhenologyEntry("8,9,10",  "5,6",     "8,9,10"),
            "ricinus communis"                to PhenologyEntry("7,8,9",   "9,10,11", "9,10,11"),

            // ── Muy alto / Alto ──
            "convallaria majalis"             to PhenologyEntry("4,5",     "8,9",     "4,5,6"),
            "hedera helix"                    to PhenologyEntry("9,10,11", "3,4,5",   "1,2,3,4,5,6,7,8,9,10,11,12"),
            "ligustrum vulgare"               to PhenologyEntry("6,7",     "9,10",    "9,10"),
            "euphorbia pulcherrima"           to PhenologyEntry("12,1,2",  "3,4",     "12,1,2,3"),
            "rhododendron ponticum"           to PhenologyEntry("4,5,6",   "9,10",    "1,2,3,4,5,6,7,8,9,10,11,12"),
            "prunus laurocerasus"             to PhenologyEntry("4,5",     "8,9,10",  "8,9,10"),
            "laburnum anagyroides"            to PhenologyEntry("5,6",     "9,10",    "9,10"),
            "euonymus europaeus"              to PhenologyEntry("5,6",     "9,10,11", "9,10,11"),
            "actaea spicata"                  to PhenologyEntry("5,6,7",   "8,9",     "8,9"),
            "buxus sempervirens"              to PhenologyEntry("3,4,5",   "6,7",     "1,2,3,4,5,6,7,8,9,10,11,12"),
            "ilex aquifolium"                 to PhenologyEntry("5,6",     "10,11,12", "10,11,12"),
            "sambucus nigra"                  to PhenologyEntry("6,7",     "8,9",     "8,9"),
            "caltha palustris"                to PhenologyEntry("3,4,5",   "7,8",     "3,4,5,6"),
            "ranunculus acris"                to PhenologyEntry("5,6,7",   "7,8",     "5,6,7"),
            "anemone nemorosa"                to PhenologyEntry("3,4,5",   "5,6",     "3,4,5"),
            "euphorbia helioscopia"           to PhenologyEntry("4,5,6",   "7,8",     "4,5,6,7"),
            "mercurialis annua"               to PhenologyEntry("6,7,8",   "9,10",    "6,7,8,9"),
            "clematis vitalba"                to PhenologyEntry("7,8,9",   "10,11",   "7,8,9,10"),
            "ranunculus ficaria"              to PhenologyEntry("2,3,4",   "4,5",     "2,3,4,5"),
            "chelidonium majus"               to PhenologyEntry("5,6,7",   "7,8",     "5,6,7,8"),
            "vincetoxicum hirundinaria"       to PhenologyEntry("6,7",     "8,9",     "6,7,8"),
            "arum maculatum"                  to PhenologyEntry("4,5",     "8,9",     "4,5,6,7,8"),
            "phitolacca americana"            to PhenologyEntry("7,8,9",   "9,10,11", "9,10,11"),
            "cotoneaster spp."                to PhenologyEntry("5,6",     "9,10,11", "9,10,11"),
            "forsythia × intermedia"          to PhenologyEntry("3,4",     "9,10",    "3,4,5"),
            "robina pseudoacacia"             to PhenologyEntry("5,6",     "10,11",   "10,11"),
            "hydrangea macrophylla"           to PhenologyEntry("6,7,8",   "10,11",   "6,7,8"),
            "ficus carica"                    to PhenologyEntry("",         "8,9",     "6,7,8,9"),
            "narcissus pseudonarcissus"       to PhenologyEntry("2,3,4",   "5,6",     "2,3,4,5"),
            "tulipa spp."                     to PhenologyEntry("4,5",     "6,7",     "4,5"),
        )
    }
}

data class SeasonalData(
    val flowering: List<PlantEntity> = emptyList(),
    val fruiting: List<PlantEntity> = emptyList(),
    val maxToxicity: List<PlantEntity> = emptyList()
)

/** Convierte "3,4,5" → listOf(3,4,5) */
fun String.toIntList(): List<Int> =
    if (isBlank()) emptyList()
    else split(",").mapNotNull { it.trim().toIntOrNull() }
