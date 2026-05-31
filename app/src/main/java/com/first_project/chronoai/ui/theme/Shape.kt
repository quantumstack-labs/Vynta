package com.first_project.chronoai.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

val VyntaShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(32.dp),
    extraLarge = RoundedCornerShape(40.dp)
)

val ShapePill = RoundedCornerShape(100.dp)
val ShapeCircle = CircleShape
val ShapeCard = RoundedCornerShape(36.dp)

// M3 Expressive: Scalloped Shape (Flower-like / Star)
val ScallopedShape = GenericShape { size, _ ->
    val points = 12
    val innerRadius = size.width * 0.42f
    val outerRadius = size.width * 0.5f
    val centerX = size.width / 2
    val centerY = size.height / 2
    
    moveTo(centerX + outerRadius, centerY)
    for (i in 1 until points * 2) {
        val angle = i * Math.PI / points
        val radius = if (i % 2 == 0) outerRadius else innerRadius
        lineTo(
            (centerX + radius * cos(angle)).toFloat(),
            (centerY + radius * sin(angle)).toFloat()
        )
    }
    close()
}

// M3 Expressive: Wavy Star Shape
val WavyStarShape = GenericShape { size, _ ->
    val points = 8
    val innerRadius = size.width * 0.35f
    val outerRadius = size.width * 0.5f
    val centerX = size.width / 2
    val centerY = size.height / 2
    
    moveTo(centerX + outerRadius, centerY)
    for (i in 1 until points * 2) {
        val angle = i * Math.PI / points
        val radius = if (i % 2 == 0) outerRadius else innerRadius
        // Using quadratic bezier for waviness
        val prevAngle = (i - 1) * Math.PI / points
        val prevRadius = if ((i - 1) % 2 == 0) outerRadius else innerRadius
        
        val cpAngle = (prevAngle + angle) / 2
        val cpRadius = outerRadius * 1.1f
        
        quadraticBezierTo(
            (centerX + cpRadius * cos(cpAngle)).toFloat(),
            (centerY + cpRadius * sin(cpAngle)).toFloat(),
            (centerX + radius * cos(angle)).toFloat(),
            (centerY + radius * sin(angle)).toFloat()
        )
    }
    close()
}

// Super Ellipse (Squircle) approximation
fun SuperEllipseShape(cornerRadius: Float = 0.2f) = GenericShape { size, _ ->
    val n = 4.0 // Power for super-ellipse
    val centerX = size.width / 2
    val centerY = size.height / 2
    val a = size.width / 2
    val b = size.height / 2

    for (i in 0..360) {
        val angle = Math.toRadians(i.toDouble())
        val cosA = cos(angle)
        val sinA = sin(angle)
        
        val x = centerX + Math.signum(cosA) * a * Math.pow(Math.abs(cosA), 2.0 / n)
        val y = centerY + Math.signum(sinA) * b * Math.pow(Math.abs(sinA), 2.0 / n)
        
        if (i == 0) moveTo(x.toFloat(), y.toFloat())
        else lineTo(x.toFloat(), y.toFloat())
    }
    close()
}

// M3 Expressive: Squircle/Organic Shape
val OrganicShape = RoundedCornerShape(
    topStart = 48.dp,
    topEnd = 20.dp,
    bottomEnd = 48.dp,
    bottomStart = 20.dp
)
