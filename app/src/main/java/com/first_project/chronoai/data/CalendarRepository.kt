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

    suspend fun getUpcomingEvents(): List<Event> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            // Fetch events for the next 7 days
            val nextWeek = now + (7 * 24 * 60 * 60 * 1000L)
            
            val events = calendarService.events().list("primary")
                .setTimeMin(DateTime(now))
                .setTimeMax(DateTime(nextWeek))
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute()

            return@withContext events.items ?: emptyList()
        } catch (e: Exception) {
            Log.e("CalendarRepository", "Error fetching upcoming events: ${e.message}")
            return@withContext emptyList()
        }
    }

    suspend fun insertEvent(
        title: String, 
        startTimeMillis: Long, 
        endTimeMillis: Long, 
        priority: Int = 3,
        energyLevel: String? = null,
        isRecurring: Boolean = false,
        rrule: String? = null
    ): Event? = withContext(Dispatchers.IO) {
        try {
            val event = Event().apply {
                summary = title
                var desc = "Priority: $priority"
                if (energyLevel != null) {
                    desc += "\nEnergy: $energyLevel"
                }
                description = desc
                start = EventDateTime().setDateTime(DateTime(startTimeMillis)).setTimeZone(systemTimeZone)
                end = EventDateTime().setDateTime(DateTime(endTimeMillis)).setTimeZone(systemTimeZone)
                
                if (isRecurring && !rrule.isNullOrEmpty()) {
                    val rule = if (rrule.startsWith("RRULE:", ignoreCase = true)) rrule else "RRULE:$rrule"
                    recurrence = listOf(rule)
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

    suspend fun updateEvent(
        eventId: String,
        title: String,
        startTimeMillis: Long,
        endTimeMillis: Long,
        priority: Int = 3,
        energyLevel: String? = null,
        isRecurring: Boolean = false,
        rrule: String? = null
    ): Event? = withContext(Dispatchers.IO) {
        try {
            val event = calendarService.events().get("primary", eventId).execute()
            event.summary = title
            var desc = "Priority: $priority"
            if (energyLevel != null) {
                desc += "\nEnergy: $energyLevel"
            }
            event.description = desc
            event.start = EventDateTime().setDateTime(DateTime(startTimeMillis)).setTimeZone(systemTimeZone)
            event.end = EventDateTime().setDateTime(DateTime(endTimeMillis)).setTimeZone(systemTimeZone)
            
            if (isRecurring && !rrule.isNullOrEmpty()) {
                val rule = if (rrule.startsWith("RRULE:", ignoreCase = true)) rrule else "RRULE:$rrule"
                event.recurrence = listOf(rule)
            } else {
                event.recurrence = null
            }
            
            return@withContext calendarService.events().update("primary", eventId, event).execute()
        } catch (e: Exception) {
            Log.e("CalendarRepository", "Error updating event: ${e.message}")
            return@withContext null
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
