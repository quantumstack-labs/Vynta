package com.first_project.chronoai.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.first_project.chronoai.ui.theme.VyntaTheme

@Composable
fun VyntaLogoNeural(modifier: Modifier = Modifier, color: Color = Color(0xFF2563EB)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        val path = Path().apply {
            // Start top left
            moveTo(w * 0.25f, h * 0.3f)
            // Curve down to bottom middle
            cubicTo(
                w * 0.25f, h * 0.7f,
                w * 0.45f, h * 0.85f,
                w * 0.5f, h * 0.85f
            )
            // Curve up to top right
            cubicTo(
                w * 0.55f, h * 0.85f,
                w * 0.75f, h * 0.7f,
                w * 0.75f, h * 0.3f
            )
            
            // Loop back for the "Neural/Intuition" feel
            moveTo(w * 0.75f, h * 0.3f)
            quadraticBezierTo(
                w * 0.65f, h * 0.15f,
                w * 0.5f, h * 0.25f
            )
        }
        
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        
        // Add a "node" at the end of the loop
        drawCircle(
            color = color,
            radius = 4.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.25f)
        )
    }
}

@Composable
fun VyntaLogoTemporal(modifier: Modifier = Modifier, color: Color = Color(0xFF6750A4)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        val path = Path().apply {
            // Left stroke: Straight but soft
            moveTo(w * 0.3f, h * 0.3f)
            lineTo(w * 0.5f, h * 0.8f)
            
            // Right stroke: Curves like a clock hand or a pulse
            moveTo(w * 0.5f, h * 0.8f)
            cubicTo(
                w * 0.6f, h * 0.6f,
                w * 0.9f, h * 0.4f,
                w * 0.8f, h * 0.2f
            )
        }
        
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun LogoShowcase() {
    VyntaTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(40.dp)
        ) {
            Text("Vynta Brand Identity Proposals", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(Color.White, CircleShape)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    VyntaLogoNeural(modifier = Modifier.fillMaxSize())
                }
                Spacer(Modifier.height(8.dp))
                Text("Option 1: The Neural Flow", fontWeight = FontWeight.Bold, color = Color.Black)
                Text("Organic, intuitive, interconnected.", fontSize = 12.sp, color = Color.Gray)
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(Color.White, CircleShape)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    VyntaLogoTemporal(modifier = Modifier.fillMaxSize(), color = Color(0xFF6750A4))
                }
                Spacer(Modifier.height(8.dp))
                Text("Option 2: The Temporal Pulse", fontWeight = FontWeight.Bold, color = Color.Black)
                Text("Precision, speed, futuristic.", fontSize = 12.sp, color = Color.Gray)
            }
            
            Text("Current Logo (for comparison)", fontSize = 14.sp, color = Color.Gray)
            // Just a placeholder for the android head
            Box(modifier = Modifier.size(60.dp).background(Color.LightGray, CircleShape), contentAlignment = Alignment.Center) {
                Text("🤖", fontSize = 30.sp)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewLogoShowcase() {
    LogoShowcase()
}
