package com.first_project.chronoai.ui1.navigation

import android.util.Log
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.first_project.chronoai.data.CalendarAuthManager
import com.first_project.chronoai.ui.theme.*
import com.first_project.chronoai.ui1.utils.HapticManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.delay

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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Breathing Aura Background
            BreathingAura()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(Modifier.height(80.dp))

                // Minimal Brand Header
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "VYNTA",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Light,
                            letterSpacing = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    Text(
                        "BECAUSE 'SOON' ISN'T A TIME.",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 4.sp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            fontWeight = FontWeight.ExtraBold
                        ),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Symbolic Keyboard Animation
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    SymbolicAnimation(hapticManager)
                }

                // Login Action
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 64.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (loginError != null) {
                        Text(
                            text = loginError!!,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .clickable { onNavigateToTerms() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isTermsAccepted,
                            onCheckedChange = null, // Controlled by navigation to TermsScreen
                            modifier = Modifier.scale(0.8f),
                            colors = CheckboxDefaults.colors(
                                disabledCheckedColor = MaterialTheme.colorScheme.primary,
                                disabledUncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Text(
                            "I agree to the Terms & Conditions",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isTermsAccepted) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

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
                            containerColor = if (isTermsAccepted) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isTermsAccepted) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(16.dp))
                            Text(
                                "Connect with Google",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Text(
                        "Synchronize your vision and reality.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 24.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun SymbolicAnimation(hapticManager: HapticManager) {
    var step by remember { mutableIntStateOf(0) }
    val view = LocalView.current
    
    LaunchedEffect(Unit) {
        while (true) {
            step = 0
            delay(1500)
            
            step = 1 // [   ]
            // Use Direct View Haptics for repetitive mechanical ticks to prevent "blurring"
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            delay(1000)
            
            step = 2 // [ * ]
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            delay(200)
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            delay(1000)
            
            step = 3 // [ Task ]
            hapticManager.play(HapticManager.VyntaEffect.AI_PROCESSING, 0.7f)
            delay(1000)
            
            step = 4 // [ Task ] -> ✓
            hapticManager.play(HapticManager.VyntaEffect.SUCCESS, 0.8f)
            delay(2500)
        }
    }

    AnimatedContent(
        targetState = step,
        transitionSpec = {
            fadeIn(tween(600)) + slideInVertically { it / 2 } togetherWith 
            fadeOut(tween(400)) + slideOutVertically { -it / 2 }
        },
        label = "symbolic_step"
    ) { currentStep ->
        val text = when(currentStep) {
            1 -> "[   ]"
            2 -> "[ * ]"
            3 -> "[ Task ]"
            4 -> "[ Task ] -> ✓"
            else -> ""
        }
        
        Text(
            text = text,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        )
    }
}

@Composable
fun BreathingAura() {
    val infiniteTransition = rememberInfiniteTransition(label = "aura")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .scale(scale)
            .alpha(alpha)
            .blur(100.dp)
            .background(
                Brush.radialGradient(
                    colors = listOf(MaterialTheme.colorScheme.primary, Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset.Unspecified,
                    radius = 800f
                )
            )
    )
}
