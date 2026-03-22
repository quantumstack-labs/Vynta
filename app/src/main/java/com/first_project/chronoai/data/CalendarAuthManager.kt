package com.first_project.chronoai.data

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.calendar.CalendarScopes
import kotlinx.coroutines.tasks.await

class CalendarAuthManager(private val context: Context) {

    fun getGoogleSignInOptions(): GoogleSignInOptions {
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(
                Scope(CalendarScopes.CALENDAR_EVENTS),
                Scope(CalendarScopes.CALENDAR_READONLY)
            )
            .build()
    }

    suspend fun signOut() {
        val googleSignInClient = GoogleSignIn.getClient(context, getGoogleSignInOptions())
        try {
            googleSignInClient.signOut().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
