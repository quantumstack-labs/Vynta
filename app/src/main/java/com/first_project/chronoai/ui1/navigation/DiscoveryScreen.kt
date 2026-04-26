package com.first_project.chronoai.ui1.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.first_project.chronoai.ui1.utils.HapticManager
import com.first_project.chronoai.voice.VyntaVoiceManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.first_project.chronoai.ui1.viewmodel.ThemeViewModel

@Composable
fun DiscoveryScreen(
    themeViewModel: ThemeViewModel,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val hapticManager = remember { HapticManager(context) }
    val voiceManager = remember { VyntaVoiceManager(context) }
    val scope = rememberCoroutineScope()
    
    var step by remember { mutableIntStateOf(0) }
    
    // Background Color State for transitions
    val bgColor by animateColorAsState(
        targetValue = when(step) {
            1 -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            2 -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
            else -> MaterialTheme.colorScheme.background
        },
        animationSpec = tween(1000),
        label = "discovery_bg"
    )

    DisposableEffect(Unit) {
        onDispose { voiceManager.shutdown() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        // Breathing Aura background
        DiscoveryAura(step)

        AnimatedContent(
            targetState = step,
            transitionSpec = {
                (fadeIn(tween(1000)) + scaleIn(initialScale = 0.92f, animationSpec = spring(Spring.DampingRatioMediumBouncy)))
                    .togetherWith(fadeOut(tween(600)) + scaleOut(targetScale = 1.05f))
            },
            label = "discovery_step"
        ) { currentStep ->
            Column(
                modifier = Modifier.padding(32.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (currentStep) {
                    0 -> {
                        DiscoveryGreeting()
                        Spacer(Modifier.height(64.dp))
                        DiscoveryButton("Architect my time") { 
                            hapticManager.play(HapticManager.VyntaEffect.CLICK)
                            step = 1 
                        }
                    }
                    1 -> {
                        DiscoveryQuestion(
                            title = "Vocal Essence",
                            subtitle = "How should your AI partner talk to you?",
                            options = listOf("Atlas", "Lyra", "Sloane", "Orion"),
                            onSelect = { persona ->
                                hapticManager.play(HapticManager.VyntaEffect.MIC_TRIGGER)
                                themeViewModel.setVoicePersona(persona)
                                
                                val phrase = when (persona) {
                                    "Atlas" -> "I am Atlas. I will ensure your schedule remains balanced and your objectives stay on track."
                                    "Lyra" -> "I'm Lyra. I'll be here to keep your momentum high and your day flowing smoothly."
                                    "Sloane" -> "This is Sloane. My focus is your efficiency. I’ll keep the briefings concise."
                                    "Orion" -> "I am Orion. I've analyzed your upcoming tasks and I'm ready to help you optimize."
                                    else -> "I am your Vynta partner."
                                }
                                voiceManager.speak(phrase, persona)
                                
                                // Delayed transition to let the user hear the voice
                                scope.launch {
                                    delay(3000)
                                    step = 2
                                }
                            }
                        )
                    }
                    2 -> {
                        DiscoveryQuestion(
                            title = "The Focus Pulse",
                            subtitle = "When is your peak performance?",
                            options = listOf("Early Bird", "Night Owl", "Balanced"),
                            onSelect = { choice ->
                                hapticManager.play(HapticManager.VyntaEffect.CLICK)
                                if (choice == "Early Bird") themeViewModel.setWorkHours(5f, 20f)
                                else if (choice == "Night Owl") themeViewModel.setWorkHours(11f, 2f)
                                
                                step = 3
                            }
                        )
                    }
                    3 -> {
                        DiscoveryLoading(hapticManager) {
                            hapticManager.play(HapticManager.VyntaEffect.SUCCESS)
                            themeViewModel.completeOnboarding()
                            onComplete()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiscoveryAura(step: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "discovery_aura")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(5000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "aura_scale"
    )
    
    val color = when(step) {
        1 -> MaterialTheme.colorScheme.primary
        2 -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.tertiary
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .scale(scale)
            .alpha(0.08f)
            .blur(120.dp)
            .background(Brush.radialGradient(listOf(color, Color.Transparent)))
    )
}

@Composable
fun DiscoveryGreeting() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Default.AutoAwesome, 
            null, 
            modifier = Modifier.size(80.dp), 
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(40.dp))
        Text(
            "VYNTA",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Black,
            letterSpacing = 12.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "YOUR TIME. ARCHITECTED.",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 4.sp
        )
    }
}

@Composable
fun DiscoveryQuestion(title: String, subtitle: String, options: List<String>, onSelect: (String) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            title.uppercase(), 
            style = MaterialTheme.typography.labelLarge, 
            color = MaterialTheme.colorScheme.primary, 
            letterSpacing = 3.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(Modifier.height(16.dp))
        Text(
            subtitle, 
            style = MaterialTheme.typography.displaySmall, 
            fontWeight = FontWeight.Black, 
            textAlign = TextAlign.Center,
            lineHeight = 44.sp,
            letterSpacing = (-1).sp
        )
        Spacer(Modifier.height(64.dp))
        options.forEach { option ->
            Button(
                onClick = { onSelect(option) },
                modifier = Modifier.fillMaxWidth().height(72.dp).padding(vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), 
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Text(option, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
fun DiscoveryLoading(hapticManager: HapticManager, onFinish: () -> Unit) {
    LaunchedEffect(Unit) {
        // High-fidelity sync haptics (Rain Pulse style)
        var delayMs = 400L
        var intensity = 0.2f
        repeat(15) {
            hapticManager.play(HapticManager.VyntaEffect.AI_PROCESSING, intensity)
            delay(delayMs)
            delayMs = (delayMs * 0.85f).toLong().coerceAtLeast(50L)
            intensity = (intensity + 0.05f).coerceAtMost(1f)
        }
        onFinish()
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(
            modifier = Modifier.size(80.dp), 
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 6.dp
        )
        Spacer(Modifier.height(40.dp))
        Text(
            "SYNCHRONIZING VISION...", 
            style = MaterialTheme.typography.titleMedium, 
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        )
    }
}

@Composable
fun DiscoveryButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(64.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(12.dp))
            Icon(Icons.Default.ChevronRight, null)
        }
    }
}
