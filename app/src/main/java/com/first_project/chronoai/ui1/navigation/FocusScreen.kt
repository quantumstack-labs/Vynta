package com.first_project.chronoai.ui1.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.first_project.chronoai.ui.theme.VyntaTypography
import kotlinx.coroutines.delay

@Composable
fun FocusScreen(
    missionTitle: String,
    onEndFocus: () -> Unit
) {
    var secondsLeft by remember { mutableStateOf(1500) } // 25 mins default
    val minutes = secondsLeft / 60
    val seconds = secondsLeft % 60
    
    val personaMessage = remember {
        listOf(
            "Atlas is guarding your focus.",
            "Stay on mission. Time is your greatest asset.",
            "The world can wait. This mission matters.",
            "Deep work in progress. Do not break the flow."
        ).random()
    }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft--
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AdaptiveMeshGradient()
        
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Shield, 
                null, 
                tint = MaterialTheme.colorScheme.primary, 
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(Modifier.height(24.dp))
            
            Text(
                "FOCUS SHIELD ACTIVE", 
                style = VyntaTypography.labelSmall, 
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 2.sp
            )
            
            Spacer(Modifier.height(48.dp))
            
            Text(
                String.format("%02d:%02d", minutes, seconds),
                style = VyntaTypography.displayLarge.copy(fontSize = 80.sp),
                color = Color.White
            )
            
            Spacer(Modifier.height(24.dp))
            
            Text(
                missionTitle.uppercase(),
                style = VyntaTypography.headlineMedium,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(Modifier.height(16.dp))
            
            Text(
                personaMessage,
                style = VyntaTypography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(Modifier.weight(1f))
            
            Button(
                onClick = onEndFocus,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.1f),
                    contentColor = Color.White
                ),
                shape = CircleShape,
                modifier = Modifier.height(56.dp).fillMaxWidth()
            ) {
                Icon(Icons.Default.Close, null)
                Spacer(Modifier.width(8.dp))
                Text("END MISSION")
            }
        }
    }
}
