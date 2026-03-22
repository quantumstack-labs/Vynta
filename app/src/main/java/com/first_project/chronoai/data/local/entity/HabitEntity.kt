package com.first_project.chronoai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val icon: String, // Emoji or icon name
    val streak: Int = 0,
    val isCompletedToday: Boolean = false,
    val completionHistory: String = "" // Simple CSV or JSON of dates "2023-10-01,2023-10-02"
)
