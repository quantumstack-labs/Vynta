package com.first_project.chronoai.data

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.calendar.Calendar
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory

class CalendarApiService(private val credential: GoogleAccountCredential) {

    fun getCalendarService(): Calendar {
        return Calendar.Builder(
            NetHttpTransport(),
            GsonFactory(),
            credential
        ).setApplicationName("Vynta").build()
    }
}
