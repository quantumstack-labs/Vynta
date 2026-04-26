package com.first_project.chronoai.data.local.entity

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val deadline: String?, // Format: "YYYY-MM-DD HH:mm"
    val status: String = "SCHEDULED", // SCHEDULED, COMPLETED, OVERDUE
    val isRecurring: Boolean = false,
    val recurrencePattern: String? = null,
    val energyLevel: String? = "Medium",
    val deadlineTime: String? = null,
    val priority: Int = 3, // Requirement: priority (1-5)
    val calendarEventId: String? = null,
    val subtasks: List<String> = emptyList(),
    val schedulingReason: String? = null // Why this slot receipt
)
