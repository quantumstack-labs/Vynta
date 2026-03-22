package com.first_project.chronoai.domain.model

import androidx.compose.ui.graphics.Color

data class Task(
    val id: String,
    val title: String,
    val startTime: String,
    val duration: String,
    val energyLevel: String, // "High", "Medium", "Low"
    val color: Color
)