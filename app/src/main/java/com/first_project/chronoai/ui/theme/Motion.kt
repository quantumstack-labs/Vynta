package com.first_project.chronoai.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp

/**
 * Requirement 1: PHYSICS-BASED MOTION
 * Standardized spring parameters: dampingRatio = 0.75f, stiffness = 300f.
 */
val VyntaSpring = spring<Float>(
    dampingRatio = 0.75f,
    stiffness = 300f
)

val VyntaSpringInt = spring<Int>(
    dampingRatio = 0.75f,
    stiffness = 300f
)

val VyntaSpringIntOffset = spring<IntOffset>(
    dampingRatio = 0.75f,
    stiffness = 300f
)

val VyntaSpringDp = spring<Dp>(
    dampingRatio = 0.75f,
    stiffness = 300f
)

val VyntaSpringColor = spring<Color>(
    dampingRatio = 0.75f,
    stiffness = 300f
)
