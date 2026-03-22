package com.first_project.chronoai.data

import android.util.Log
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import com.google.api.services.calendar.model.FreeBusyRequest
import com.google.api.services.calendar.model.FreeBusyRequestItem
import com.google.api.services.calendar.model.TimePeriod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone

class CalendarRepository(private val calendarService: Calendar) {

    private val systemTimeZone = TimeZone.getDefault().id

    suspend fun getEventsForDate(date: LocalDate): List<Event> = withContext(Dispatchers.IO) {
        try {
            val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            
            val events = calendarService.events().list("primary")
                .setTimeMin(DateTime(startOfDay))
                .setTimeMax(DateTime(endOfDay))
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute()

            return@withContext events.items ?: emptyList()
        } catch (e: GoogleJsonResponseException) {
            Log.e("CalendarRepository", "API Error (${e.statusCode}): ${e.details?.message}")
            return@withContext emptyList()
        } catch (e: Exception) {
            Log.e("CalendarRepository", "Error fetching events: ${e.message}")
            return@withContext emptyList()
        }
    }

    suspend fun insertEvent(
        title: String, 
        startTimeMillis: Long, 
        endTimeMillis: Long, 
        priority: Int = 3,
        isRecurring: Boolean = false,
        rrule: String? = null
    ): Event? = withContext(Dispatchers.IO) {
        try {
            val event = Event().apply {
                summary = title
                description = "Priority: $priority"
                start = EventDateTime().setDateTime(DateTime(startTimeMillis)).setTimeZone(systemTimeZone)
                end = EventDateTime().setDateTime(DateTime(endTimeMillis)).setTimeZone(systemTimeZone)
                
                if (isRecurring && !rrule.isNullOrEmpty()) {
                    recurrence = listOf(rrule)
                }
            }

            return@withContext calendarService.events().insert("primary", event).execute()
        } catch (e: Exception) {
            Log.e("CalendarRepository", "Error inserting event: ${e.message}")
            // This is likely where the "Connection Error" is coming from
            return@withContext null
        }
    }

    suspend fun getBusySlots(startTimeMillis: Long, endTimeMillis: Long): List<TimePeriod> = withContext(Dispatchers.IO) {
        try {
            val request = FreeBusyRequest().apply {
                timeMin = DateTime(startTimeMillis)
                timeMax = DateTime(endTimeMillis)
                items = listOf(FreeBusyRequestItem().setId("primary"))
            }
            val response = calendarService.freebusy().query(request).execute()
            return@withContext response.calendars["primary"]?.busy ?: emptyList()
        } catch (e: Exception) {
            Log.e("CalendarRepository", "Error fetching freebusy: ${e.message}")
            return@withContext emptyList()
        }
    }

    suspend fun deleteEvent(eventId: String) = withContext(Dispatchers.IO) {
        try {
            calendarService.events().delete("primary", eventId).execute()
        } catch (e: Exception) {
            Log.e("CalendarRepository", "Error deleting event: ${e.message}")
            throw e
        }
    }
}
