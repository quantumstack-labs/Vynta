package com.first_project.chronoai.ui1.navigation

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.first_project.chronoai.data.CalendarAuthManager
import com.first_project.chronoai.ui.theme.*
import com.first_project.chronoai.ui1.utils.HapticManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToTerms: () -> Unit,
    isTermsAccepted: Boolean
) {
    val context = LocalContext.current
    val hapticManager = remember { HapticManager(context) }
    val authManager = remember { CalendarAuthManager(context) }
    val googleSignInClient = remember {
        GoogleSignIn.getClient(context, authManager.getGoogleSignInOptions())
    }
    
    var loginError by remember { mutableStateOf<String?>(null) }
    var isVisible by remember { mutableStateOf(false) }
    var syncStatus by remember { mutableStateOf("Ready to begin") }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                hapticManager.play(HapticManager.VyntaEffect.SUCCESS)
                onLoginSuccess()
            }
        } catch (e: ApiException) {
            Log.e("LoginError", "Status Code: ${e.statusCode}")
            loginError = "Authentication failed. Let's try again."
        }
    }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Ambient Background
        AdaptiveMeshGradient(modifier = Modifier.fillMaxSize())

        Scaffold(
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(Modifier.height(40.dp))

                // Section 1: Minimal Branding (Staggered Entrance)
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(1000)) + slideInVertically(tween(1000)) { -40 }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "VYNTA",
                            style = TextStyle(
                                fontFamily = BricolageFont,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 44.sp,
                                letterSpacing = 12.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                        Text(
                            "YOUR VISION, ORGANIZED.",
                            style = TextStyle(
                                fontFamily = SyneFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 4.sp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            ),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                // Section 2: Core Animation Illustration
                Box(
                    modifier = Modifier
                        .size(320.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TemporalAlignmentIllustration(
                        onSyncStatusChange = { syncStatus = it },
                        hapticManager = hapticManager
                    )
                }

                // Section 3: Interaction Area (Staggered Entrance)
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(1000, 500)) + slideInVertically(tween(1000, 500)) { 40 }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = syncStatus.uppercase(),
                            style = TextStyle(
                                fontFamily = SyneFont,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                letterSpacing = 2.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        
                        Spacer(Modifier.height(24.dp))

                        if (loginError != null) {
                            Text(
                                text = loginError!!,
                                style = TextStyle(fontFamily = SyneFont, fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }

                        // Clean Terms Row
                        Row(
                            modifier = Modifier
                                .padding(bottom = 24.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onNavigateToTerms() }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(if (isTermsAccepted) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .border(1.5.dp, if (isTermsAccepted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isTermsAccepted) {
                                    Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "I AGREE TO THE TERMS & CONDITIONS",
                                style = TextStyle(
                                    fontFamily = SyneFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.sp,
                                    color = if (isTermsAccepted) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }

                        // Primary Action
                        Button(
                            onClick = {
                                if (isTermsAccepted) {
                                    hapticManager.play(HapticManager.VyntaEffect.CLICK)
                                    launcher.launch(googleSignInClient.signInIntent)
                                }
                            },
                            enabled = isTermsAccepted,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isTermsAccepted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isTermsAccepted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(16.dp))
                                Text(
                                    "CONNECT WITH GOOGLE",
                                    style = TextStyle(
                                        fontFamily = SyneFont,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        letterSpacing = 2.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class TaskNode(
    val id: Int,
    val chaosOffset: Offset,
    val targetY: Float,
    val color: Color,
    val size: Float
)

@Composable
fun TemporalAlignmentIllustration(
    onSyncStatusChange: (String) -> Unit,
    hapticManager: HapticManager
) {
    // Colors consistent with primary palette (Primary gold/lavender hues)
    val colorPalette = listOf(
        Color(0xFFD0BCFF), // Lavender
        Color(0xFFC8B99A), // Gold/Beige
        Color(0xFFEADDFF), // Light Lavender
        Color(0xFFF3E7C9)  // Light Gold
    )

    val nodes = remember {
        List(10) { i ->
            TaskNode(
                id = i,
                chaosOffset = Offset(Random.nextFloat() * 240f - 120f, Random.nextFloat() * 240f - 120f),
                targetY = (i - 4.5f) * 36f,
                color = colorPalette[i % colorPalette.size],
                size = Random.nextFloat() * 6f + 6f
            )
        }
    }

    var isAligning by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "chaos")
    
    val chaosDrift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing), RepeatMode.Restart),
        label = "drift"
    )

    val alignmentProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(3500)
            onSyncStatusChange("Organizing your day...")
            isAligning = true
            alignmentProgress.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
            )
            onSyncStatusChange("Schedule Optimized")
            hapticManager.play(HapticManager.VyntaEffect.SUCCESS, 0.4f)
            
            delay(4500)
            onSyncStatusChange("Awaiting input...")
            isAligning = false
            alignmentProgress.animateTo(targetValue = 0f, animationSpec = tween(1800, easing = EaseInOutSine))
            onSyncStatusChange("Ready to begin")
        }
    }

    // Monitor alignment to fire haptics when nodes "snap"
    LaunchedEffect(isAligning) {
        if (isAligning) {
            launch {
                repeat(nodes.size) { i ->
                    delay(120 + i * 70L)
                    hapticManager.play(HapticManager.VyntaEffect.AI_CRUNCHING, 0.25f)
                }
            }
        }
    }

    Canvas(modifier = Modifier.size(300.dp)) {
        val center = center
        val progress = alignmentProgress.value

        // Timeline Axis with subtle glow
        val axisAlpha = progress * 0.5f
        if (axisAlpha > 0.05f) {
            // Shadow Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = axisAlpha * 0.15f), Color.Transparent),
                    center = center,
                    radius = 200.dp.toPx()
                ),
                radius = 200.dp.toPx(),
                center = center
            )
            
            drawLine(
                color = Color.White.copy(alpha = axisAlpha),
                start = Offset(center.x, center.y - 170.dp.toPx()),
                end = Offset(center.x, center.y + 170.dp.toPx()),
                strokeWidth = 1.2.dp.toPx()
            )
        }

        nodes.forEach { node ->
            // Chaos drifting
            val driftX = kotlin.math.sin((chaosDrift * 2 * kotlin.math.PI + node.id).toFloat()) * 25f
            val driftY = kotlin.math.cos((chaosDrift * 2 * kotlin.math.PI + node.id * 1.5).toFloat()) * 25f
            
            val chaosPos = center + node.chaosOffset + Offset(driftX, driftY)
            val targetPos = Offset(center.x, center.y + node.targetY.dp.toPx())
            
            val currentPos = Offset(
                x = lerp(chaosPos.x, targetPos.x, progress),
                y = lerp(chaosPos.y, targetPos.y, progress)
            )

            // Particle Trail (only when moving significantly)
            if (progress > 0.1f && progress < 0.9f) {
                val trailOffset = (chaosPos - targetPos) * (0.05f * (1f - progress))
                drawCircle(
                    color = node.color.copy(alpha = 0.2f * progress),
                    radius = node.size.dp.toPx() * 0.8f,
                    center = currentPos + trailOffset
                )
            }

            val nodeAlpha = lerp(0.4f, 1f, progress)
            val glowScale = if (progress > 0.85f) 2.5f else 1.8f

            // Node Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(node.color.copy(alpha = 0.35f * nodeAlpha), Color.Transparent),
                    center = currentPos,
                    radius = node.size.dp.toPx() * glowScale
                ),
                radius = node.size.dp.toPx() * glowScale,
                center = currentPos
            )

            // Node Core
            drawCircle(
                color = node.color.copy(alpha = nodeAlpha),
                radius = node.size.dp.toPx(),
                center = currentPos
            )
            
            // Refined horizontal alignment indicator
            if (progress > 0.85f) {
                val lineProgress = (progress - 0.85f) / 0.15f
                val lineHalfWidth = 14.dp.toPx() * lineProgress
                drawLine(
                    color = node.color.copy(alpha = lineProgress * 0.8f),
                    start = Offset(currentPos.x - lineHalfWidth, currentPos.y),
                    end = Offset(currentPos.x + lineHalfWidth, currentPos.y),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
        }
    }
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + fraction * (stop - start)
}
