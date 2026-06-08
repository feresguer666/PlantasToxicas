package com.toxicplants.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ToxicCalendarDao {

    @Query("SELECT * FROM calendar_events ORDER BY date DESC")
    fun getAllEvents(): LiveData<List<ToxicCalendarEvent>>

    @Query("SELECT * FROM calendar_events ORDER BY date DESC")
    suspend fun getAllEventsSync(): List<ToxicCalendarEvent>

    @Query("SELECT * FROM calendar_events WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getEventsInRange(startDate: String, endDate: String): LiveData<List<ToxicCalendarEvent>>

    @Query("SELECT * FROM calendar_events WHERE date LIKE :monthPattern ORDER BY date ASC")
    suspend fun getEventsByMonth(monthPattern: String): List<ToxicCalendarEvent>

    @Query("SELECT * FROM calendar_events WHERE date = :date ORDER BY createdAt DESC")
    suspend fun getEventsByDate(date: String): List<ToxicCalendarEvent>

    @Query("SELECT * FROM calendar_events WHERE plantId = :plantId ORDER BY date DESC")
    fun getEventsByPlant(plantId: Int): LiveData<List<ToxicCalendarEvent>>

    @Query("SELECT * FROM calendar_events WHERE id = :id")
    suspend fun getEventById(id: Int): ToxicCalendarEvent?

    @Query("SELECT COUNT(*) FROM calendar_events")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: ToxicCalendarEvent): Long

    @Update
    suspend fun update(event: ToxicCalendarEvent)

    @Delete
    suspend fun delete(event: ToxicCalendarEvent)

    @Query("DELETE FROM calendar_events WHERE id = :id")
    suspend fun deleteById(id: Int)
}
