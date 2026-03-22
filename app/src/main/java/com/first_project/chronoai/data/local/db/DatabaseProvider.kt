package com.first_project.chronoai.data.local.db

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    @Volatile
    private var instance: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "chronos_db_v3"
            )
            .fallbackToDestructiveMigration()
            .build().also { instance = it }
        }
    }
}
