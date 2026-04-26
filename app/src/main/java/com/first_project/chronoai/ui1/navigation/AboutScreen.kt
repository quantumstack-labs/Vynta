package com.first_project.chronoai.ui1.navigation

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.first_project.chronoai.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val view = LocalView.current
    val uriHandler = LocalUriHandler.current
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        // Dynamic background
        AnimatedMeshBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "ARCHITECT",
                            style = MaterialTheme.typography.labelLarge,
                            letterSpacing = 4.sp,
                            fontWeight = FontWeight.Black,
                            color = colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            onBack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    ProfileHeader()
                }

                item {
                    Text(
                        "Murshid R",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        "AI Engineer • Mobile Architect",
                        style = MaterialTheme.typography.titleMedium,
                        color = colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                "THE MISSION",
                                style = MaterialTheme.typography.labelLarge,
                                color = colorScheme.primary,
                                letterSpacing = 2.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Vynta is the convergence of AI and human intuition. I engineered this platform to eliminate the cognitive load of scheduling, allowing creators to focus on what truly matters.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = colorScheme.onSurface.copy(alpha = 0.8f),
                                lineHeight = 28.sp
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StatCard(
                            label = "Stack",
                            value = "Kotlin/Compose",
                            icon = Icons.Default.Layers,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Intelligence",
                            value = "Groq LPU",
                            icon = Icons.Default.AutoAwesome,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "CORE TECHNOLOGIES",
                            style = MaterialTheme.typography.labelMedium,
                            letterSpacing = 2.sp,
                            color = colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Clean Architecture", "Room DB", "Material 3", "Coroutine Flows", "Hilt", "Dagger").forEach {
                                TechTag(it)
                            }
                        }
                    }
                }

                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                "CONNECT",
                                style = MaterialTheme.typography.labelLarge,
                                letterSpacing = 2.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(20.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                SocialIcon(Icons.Default.Link) { uriHandler.openUri("https://linkedin.com/in/murshid-r-37088b272") }
                                SocialIcon(Icons.Default.Code) { uriHandler.openUri("https://github.com/murshidr") }
                                SocialIcon(Icons.Default.Email) { uriHandler.openUri("mailto:murshidreyas@gmail.com") }
                            }
                        }
                    }
                }
                
                item {
                    Spacer(Modifier.height(40.dp))
                    Text(
                        "Vynta v1.0 • Built with Passion",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileHeader() {
    val infiniteTransition = rememberInfiniteTransition(label = "profile_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(3000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "glow"
    )
    val color = MaterialTheme.colorScheme.primary

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
        // Glow layer
        Box(
            modifier = Modifier
                .size(140.dp)
                .drawBehind {
                    drawCircle(
                        Brush.radialGradient(
                            listOf(color.copy(alpha = glowAlpha), Color.Transparent)
                        ),
                        radius = size.width * 1.2f
                    )
                }
        )

        Surface(
            modifier = Modifier
                .size(130.dp)
                .border(2.dp, Brush.linearGradient(listOf(color, color.copy(alpha = 0.3f))), CircleShape),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Image(
                painter = painterResource(id = R.drawable.murshid_r),
                contentDescription = "Murshid",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = colorScheme.surfaceVariant.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, colorScheme.onSurface.copy(alpha = 0.08f)),
        tonalElevation = 2.dp
    ) {
        content()
    }
}

@Composable
fun StatCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(12.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TechTag(text: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun SocialIcon(icon: ImageVector, onClick: () -> Unit) {
    val view = LocalView.current
    IconButton(
        onClick = {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            onClick()
        },
        modifier = Modifier
            .size(56.dp)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f), CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), CircleShape)
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun AnimatedMeshBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "mesh")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(15000, easing = LinearEasing)),
        label = "phase"
    )

    val primary = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    val secondary = MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f)

    Canvas(modifier = Modifier.fillMaxSize().blur(100.dp)) {
        val width = size.width
        val height = size.height
        
        drawCircle(
            color = primary,
            center = Offset(
                width * 0.5f + (width * 0.2f * Math.cos(phase.toDouble()).toFloat()),
                height * 0.3f + (height * 0.1f * Math.sin(phase.toDouble()).toFloat())
            ),
            radius = width * 0.8f
        )

        drawCircle(
            color = secondary,
            center = Offset(
                width * 0.2f + (width * 0.3f * Math.sin(phase.toDouble()).toFloat()),
                height * 0.7f + (height * 0.2f * Math.cos(phase.toDouble()).toFloat())
            ),
            radius = width * 0.7f
        )
    }
}
