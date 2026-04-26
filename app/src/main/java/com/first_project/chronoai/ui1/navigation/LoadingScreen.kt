package com.first_project.chronoai.ui1.navigation

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.first_project.chronoai.ui.theme.VyntaTypography

@Composable
fun LoadingScreen(status: String = "Synchronizing your timeline...") {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "rotation"
    )

    val primaryColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
            Canvas(modifier = Modifier.size(80.dp)) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(Color.Transparent, primaryColor)
                    ),
                    startAngle = rotation,
                    sweepAngle = 280f,
                    useCenter = false,
                    style = Stroke(width = 4.dp.toPx())
                )
            }
            Text(
                text = "V",
                style = VyntaTypography.headlineLarge.copy(
                    color = primaryColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                )
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "VYNTA",
            style = VyntaTypography.labelLarge.copy(
                letterSpacing = 4.sp,
                fontWeight = FontWeight.ExtraBold,
                color = primaryColor
            )
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = status,
            style = VyntaTypography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        )
    }
}
