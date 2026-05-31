package com.first_project.chronoai.ui1.navigation

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

enum class ActiveThumb { NONE, START, END }

@Composable
fun VyntaVelocitySlider(
    value: ClosedFloatingPointRange<Float>,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = primaryColor.copy(alpha = 0.1f)
    
    var lastHapticValueStart by remember { mutableIntStateOf(value.start.roundToInt()) }
    var lastHapticValueEnd by remember { mutableIntStateOf(value.endInclusive.roundToInt()) }
    var draggingThumb by remember { mutableStateOf(ActiveThumb.NONE) }

    val infiniteTransition = rememberInfiniteTransition(label = "liquid")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "wave"
    )

    // Animated scale for the active thumb
    val startThumbScale by animateFloatAsState(
        targetValue = if (draggingThumb == ActiveThumb.START) 1.5f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "startThumbScale"
    )
    val endThumbScale by animateFloatAsState(
        targetValue = if (draggingThumb == ActiveThumb.END) 1.5f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "endThumbScale"
    )

    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentValue by rememberUpdatedState(value)

    BoxWithConstraints(modifier = modifier.height(64.dp).fillMaxWidth()) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val sidePadding = 48f
        val usableWidth = width - (sidePadding * 2)
        
        fun hourToPx(hour: Float): Float = sidePadding + (hour / 24f) * usableWidth
        fun pxToHour(px: Float): Float = ((px - sidePadding) / usableWidth * 24f).coerceIn(0f, 24f)

        Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { offset ->
                    val startPx = hourToPx(currentValue.start)
                    val endPx = hourToPx(currentValue.endInclusive)
                    val distStart = abs(offset.x - startPx)
                    val distEnd = abs(offset.x - endPx)
                    
                    draggingThumb = when {
                        distStart < 80f && distStart < distEnd -> ActiveThumb.START
                        distEnd < 80f -> ActiveThumb.END
                        else -> ActiveThumb.NONE
                    }
                    
                    if (draggingThumb != ActiveThumb.NONE) {
                        view.performHapticFeedback(HapticFeedbackConstants.GESTURE_START)
                    }
                },
                onDragEnd = { draggingThumb = ActiveThumb.NONE },
                onDragCancel = { draggingThumb = ActiveThumb.NONE },
                onDrag = { change, dragAmount ->
                    if (draggingThumb == ActiveThumb.NONE) return@detectDragGestures
                    
                    change.consume()
                    
                    if (draggingThumb == ActiveThumb.START) {
                        val currentPx = hourToPx(currentValue.start)
                        val newStart = pxToHour(currentPx + dragAmount.x)
                        if (newStart <= currentValue.endInclusive - 1f) {
                            currentOnValueChange(newStart..currentValue.endInclusive)
                            val rounded = newStart.roundToInt()
                            if (rounded != lastHapticValueStart) {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                lastHapticValueStart = rounded
                            }
                        }
                    } else if (draggingThumb == ActiveThumb.END) {
                        val currentPx = hourToPx(currentValue.endInclusive)
                        val newEnd = pxToHour(currentPx + dragAmount.x)
                        if (newEnd >= currentValue.start + 1f) {
                            currentOnValueChange(currentValue.start..newEnd)
                            val rounded = newEnd.roundToInt()
                            if (rounded != lastHapticValueEnd) {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                lastHapticValueEnd = rounded
                            }
                        }
                    }
                }
            )
        }) {
            val centerY = height / 2f
            
            // Background Track
            drawLine(
                color = trackColor,
                start = Offset(sidePadding, centerY),
                end = Offset(width - sidePadding, centerY),
                strokeWidth = 10f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            
            val startPx = hourToPx(value.start)
            val endPx = hourToPx(value.endInclusive)
            
            // Liquid Active Track
            val path = Path().apply {
                moveTo(startPx, centerY)
                val segments = 30
                val segW = (endPx - startPx) / segments
                for (i in 1..segments) {
                    val x = startPx + i * segW
                    val y = centerY + kotlin.math.sin((i.toFloat() / segments + waveOffset) * 2 * kotlin.math.PI.toFloat()).toFloat() * 12f
                    lineTo(x, y)
                }
            }
            
            drawPath(
                path = path,
                brush = Brush.horizontalGradient(listOf(primaryColor.copy(alpha = 0.6f), primaryColor)),
                style = Stroke(width = 14f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
            
            // Glow Effect
            drawPath(
                path = path,
                color = primaryColor.copy(alpha = 0.25f),
                style = Stroke(width = 28f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
            
            // Start Thumb
            drawCircle(
                color = Color.White.copy(alpha = 0.3f),
                radius = 24f * startThumbScale,
                center = Offset(startPx, centerY)
            )
            drawCircle(
                color = Color.White,
                radius = 14f * startThumbScale,
                center = Offset(startPx, centerY)
            )
            drawCircle(
                color = primaryColor,
                radius = 10f * startThumbScale,
                center = Offset(startPx, centerY)
            )
            
            // End Thumb
            drawCircle(
                color = Color.White.copy(alpha = 0.3f),
                radius = 24f * endThumbScale,
                center = Offset(endPx, centerY)
            )
            drawCircle(
                color = Color.White,
                radius = 14f * endThumbScale,
                center = Offset(endPx, centerY)
            )
            drawCircle(
                color = primaryColor,
                radius = 10f * endThumbScale,
                center = Offset(endPx, centerY)
            )
        }
    }
}
