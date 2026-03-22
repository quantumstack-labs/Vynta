package com.first_project.chronoai.ui1.util

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.glassmorphism(
    shape: Shape,
    blur: Dp = 15.dp,
    borderColor: Color = Color.White.copy(alpha = 0.2f),
    backgroundColor: Color = Color.White.copy(alpha = 0.1f)
): Modifier = this
    .clip(shape)
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(
                backgroundColor,
                backgroundColor.copy(alpha = 0.05f)
            )
        )
    )
    .blur(blur)
    .border(1.dp, borderColor, shape)
