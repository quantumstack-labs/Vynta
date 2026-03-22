package com.first_project.chronoai.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.first_project.chronoai.data.local.dao.TaskDao
import com.first_project.chronoai.data.local.dao.HabitDao
import com.first_project.chronoai.data.local.entity.TaskEntity
import com.first_project.chronoai.data.local.entity.HabitEntity
import com.first_project.chronoai.data.local.db.converters.Converters

@Database(entities = [TaskEntity::class, HabitEntity::class], version = 16, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun habitDao(): HabitDao
}
