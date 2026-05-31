package com.first_project.chronoai.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp

/**
 * Requirement 1: PHYSICS-BASED MOTION
 * Expressive spring parameters: dampingRatio = 0.7f, stiffness = 200f for a fluid, natural feel.
 */
val VyntaSpring = spring<Float>(
    dampingRatio = 0.7f,
    stiffness = 200f
)

val VyntaSpringInt = spring<Int>(
    dampingRatio = 0.7f,
    stiffness = 200f
)

val VyntaSpringIntOffset = spring<IntOffset>(
    dampingRatio = 0.7f,
    stiffness = 200f
)

val VyntaSpringDp = spring<Dp>(
    dampingRatio = 0.7f,
    stiffness = 200f
)

val VyntaSpringColor = spring<Color>(
    dampingRatio = 0.7f,
    stiffness = 200f
)
