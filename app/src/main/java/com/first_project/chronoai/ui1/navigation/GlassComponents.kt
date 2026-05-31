package com.first_project.chronoai.ui1.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.first_project.chronoai.ui.theme.VyntaShapes

@Composable
fun VyntaCard(
    modifier: Modifier = Modifier,
    shape: Shape = VyntaShapes.large,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    showBorder: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            color = containerColor,
            contentColor = contentColor,
            border = if (showBorder) BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)) else null,
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
                content = content
            )
        }
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = containerColor,
            contentColor = contentColor,
            border = if (showBorder) BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)) else null,
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
                content = content
            )
        }
    }
}

@Composable
fun AdaptiveMeshGradient(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "mesh")
    
    val pulse1 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(8000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "pulse1"
    )
    
    val pulse2 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(12000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "pulse2"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Primary Glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f * pulse1),
                            Color.Transparent
                        ),
                        center = androidx.compose.ui.geometry.Offset(0f, 0f),
                        radius = 1200f
                    )
                )
        )
        // Secondary Glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f * pulse2),
                            Color.Transparent
                        ),
                        center = androidx.compose.ui.geometry.Offset(1000f, 1500f),
                        radius = 1000f
                    )
                )
        )
        // Subtle Noise or Texture could go here
    }
}

@Composable
fun GlassActionPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .height(44.dp),
        shape = CircleShape,
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(
            width = 0.5.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f)
        )
    ) {
        Box(modifier = Modifier.padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RefractiveCrystal(
    modifier: Modifier = Modifier,
    isSpeaking: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "crystal")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing)),
        label = "rotation"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isSpeaking) 1.2f else 1.05f,
        animationSpec = infiniteRepeatable(tween(if (isSpeaking) 800 else 2000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "pulse"
    )

    Canvas(modifier = modifier.size(32.dp).graphicsLayer { rotationZ = rotation; scaleX = pulse; scaleY = pulse }) {
        drawCircle(
            brush = Brush.sweepGradient(listOf(Color(0xFFD0BCFF), Color(0xFFB4F0AD), Color(0xFFD0BCFF))),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
fun LiquidProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = color.copy(alpha = 0.2f)
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioLowBouncy),
        label = "progress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "waveOffset"
    )

    Canvas(modifier = modifier.clip(CircleShape)) {
        val width = size.width
        val height = size.height
        
        // Track
        drawRoundRect(
            color = trackColor,
            size = size,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(height / 2f)
        )
        
        // Progress with Liquid Wave
        val progressWidth = width * animatedProgress
        if (progressWidth > 0f) {
            val path = Path().apply {
                moveTo(0f, height)
                lineTo(0f, height * 0.5f)
                
                val waveCount = 4
                val waveW = progressWidth / waveCount.toFloat()
                for (i in 0..waveCount) {
                    val x = i.toFloat() * waveW
                    val y = kotlin.math.sin((i.toFloat() / waveCount.toFloat() + waveOffset) * 2f * kotlin.math.PI.toFloat()) * (height * 0.15f)
                    lineTo(x, (height * 0.5f) + y)
                }
                
                lineTo(progressWidth, height)
                close()
            }
            
            drawPath(
                path = path,
                brush = Brush.horizontalGradient(
                    listOf(color.copy(alpha = 0.8f), color)
                )
            )
        }
    }
}

@Composable
fun VyntaCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val scale by animateFloatAsState(
        targetValue = if (checked) 1.2f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    Box(
        modifier = modifier
            .size(24.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(CircleShape)
            .background(if (checked) color else Color.Transparent)
            .border(
                BorderStroke(
                    width = 1.5.dp,
                    color = if (checked) color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                ),
                CircleShape
            )
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = checked,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

