package com.first_project.chronoai.ui1.navigation

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.first_project.chronoai.data.CalendarAuthManager
import com.first_project.chronoai.ui.theme.*
import com.first_project.chronoai.ui1.utils.rememberVyntaHaptic
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue

data class OnboardingStep(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color
)

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    val hapticEngine = rememberVyntaHaptic()
    val authManager = remember { CalendarAuthManager(context) }
    val googleSignInClient = remember {
        GoogleSignIn.getClient(context, authManager.getGoogleSignInOptions())
    }

    val steps = remember {
        listOf(
            OnboardingStep(
                "AI-Powered Focus",
                "Vynta analyzes your energy patterns to schedule tasks when you're most productive.",
                Icons.Default.AutoAwesome,
                Primary
            ),
            OnboardingStep(
                "Energy-Aware Growth",
                "Align your hardest challenges with your peak energy levels automatically.",
                Icons.Default.ElectricBolt,
                AccentGreen
            ),
            OnboardingStep(
                "Seamless Harmony",
                "Your Google Calendar and tasks synchronized into one unified intelligence.",
                Icons.Default.CalendarMonth,
                AccentBlue
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { steps.size })
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                hapticEngine.successSparkle()
                onLoginSuccess()
            }
        } catch (e: ApiException) {
            Log.e("LoginError", "Status Code: ${e.statusCode}")
            // Show the error code to the user for debugging on APK
            Toast.makeText(context, "Login failed. Error Code: ${e.statusCode}", Toast.LENGTH_LONG).show()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AnimatedBackground()

        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            BrandHeader()
            Spacer(modifier = Modifier.weight(0.5f))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().height(360.dp),
                contentPadding = PaddingValues(horizontal = 32.dp),
                pageSpacing = 16.dp
            ) { page ->
                val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
                OnboardingPage(
                    step = steps[page],
                    modifier = Modifier.graphicsLayer {
                        alpha = 1f - pageOffset.coerceIn(0f, 1f)
                        scaleX = 1f - (pageOffset * 0.1f)
                        scaleY = 1f - (pageOffset * 0.1f)
                    }
                )
            }

            PagerIndicator(steps.size, pagerState.currentPage)
            Spacer(modifier = Modifier.weight(1f))

            LoginButton(
                onClick = {
                    hapticEngine.mechanicalPress()
                    launcher.launch(googleSignInClient.signInIntent)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "SECURE INTELLIGENCE • PRIVACY FIRST",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun BrandHeader() {
    val infiniteTransition = rememberInfiniteTransition(label = "header")
    val letterSpacing by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "spacing"
    )

    Text(
        text = "VYNTA",
        style = MaterialTheme.typography.displayMedium.copy(
            letterSpacing = letterSpacing.sp,
            fontWeight = FontWeight.ExtraLight,
            color = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
fun OnboardingPage(step: OnboardingStep, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "icon")
        val floatOffset by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = -15f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ), label = "float"
        )

        Surface(
            modifier = Modifier
                .size(120.dp)
                .offset(y = floatOffset.dp),
            shape = RoundedCornerShape(40.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = androidx.compose.foundation.BorderStroke(1.dp, step.accentColor.copy(alpha = 0.2f)),
            shadowElevation = 8.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = step.icon,
                    contentDescription = null,
                    tint = step.accentColor,
                    modifier = Modifier.size(56.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = step.title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = step.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 26.sp
        )
    }
}

@Composable
fun PagerIndicator(size: Int, currentPage: Int) {
    Row(
        Modifier.height(48.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(size) { iteration ->
            val isSelected = currentPage == iteration
            val width by animateDpAsState(
                targetValue = if (isSelected) 32.dp else 8.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "width"
            )
            val color by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                label = "color"
            )
            
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(color)
                    .size(height = 8.dp, width = width)
            )
        }
    }
}

@Composable
fun LoginButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .padding(horizontal = 32.dp)
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Login,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                "CONTINUE WITH GOOGLE",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
fun AnimatedBackground() {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "alpha"
    )
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )

    Canvas(modifier = Modifier.fillMaxSize().blur(100.dp).scale(scale)) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primaryColor.copy(alpha = alpha), Color.Transparent),
                center = center.copy(y = size.height * 0.3f, x = size.width * 0.2f),
                radius = size.width
            )
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(secondaryColor.copy(alpha = alpha * 0.5f), Color.Transparent),
                center = center.copy(y = size.height * 0.7f, x = size.width * 0.8f),
                radius = size.width
            )
        )
    }
}
