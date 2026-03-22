package com.first_project.chronoai.ui1.navigation

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.first_project.chronoai.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val view = LocalView.current
    val uriHandler = LocalUriHandler.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    val colorScheme = MaterialTheme.colorScheme
    val primaryColor = colorScheme.primary
    val secondaryColor = colorScheme.secondary
    val backgroundColor = colorScheme.background
    val onSurface = colorScheme.onSurface

    // Interactive state for haptic "excitement"
    var isExcited by remember { mutableStateOf(false) }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(backgroundColor)) {
        NeuralBackground(primaryColor, secondaryColor, isExcited)

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "ARCHITECT.LOG",
                            style = MaterialTheme.typography.labelLarge,
                            letterSpacing = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = onSurface.copy(alpha = 0.5f)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onBack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = onSurface)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 2. THE CORE (Photo + Pulse)
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(280.dp)) {
                    PulseCore(primaryColor, isExcited)

                    // Circular Photo
                    Surface(
                        modifier = Modifier
                            .size(120.dp)
                            .padding(4.dp)
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                isExcited = true
                                scope.launch {
                                    delay(2000)
                                    isExcited = false
                                }
                            },
                        shape = CircleShape,
                        color = colorScheme.surface,
                        border = BorderStroke(
                            2.dp,
                            Brush.linearGradient(listOf(primaryColor, secondaryColor))
                        ),
                        shadowElevation = 20.dp
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.murshid_r),
                            contentDescription = "Architect Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Text(
                    "MURSHID R",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = onSurface,
                    letterSpacing = (-1).sp
                )
                Text(
                    "SYNTACTIC ARCHITECT",
                    style = MaterialTheme.typography.labelSmall,
                    color = primaryColor,
                    letterSpacing = 4.sp
                )

                Spacer(Modifier.height(48.dp))

                // 3. BENTO GRID SECTIONS
                BentoTile(
                    modifier = Modifier.fillMaxWidth(),
                    title = "THE VISION",
                    icon = Icons.Default.AutoAwesome
                ) {
                    Text(
                        "Building Vynta wasn't about another app. It was about creating a cognitive bridge. An interface that adapts to human intuition rather than forcing the user to adapt to the machine.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = onSurface.copy(alpha = 0.7f),
                        lineHeight = 28.sp
                    )
                }

                Spacer(Modifier.height(16.dp))

                // TECH STACK (Full Width)
                BentoTile(
                    modifier = Modifier.fillMaxWidth(),
                    title = "TECH STACK",
                    icon = Icons.Default.Layers
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf(
                            "Kotlin",
                            "Jetpack Compose",
                            "PyTorch",
                            "LLMs",
                            "Material 3",
                            "Architecture"
                        ).forEach {
                            TechChip(it, primaryColor)
                        }
                    }
                }

                Spacer(Modifier.height(40.dp))

                // 4. THE JOURNEY
                Text(
                    "MILESTONES",
                    modifier = Modifier.align(Alignment.Start),
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 5.sp,
                    color = onSurface.copy(alpha = 0.3f)
                )

                Spacer(Modifier.height(24.dp))

                val milestones = listOf(
                    "Inception" to "Initial neural mapping for habit loops.",
                    "Vynta 1.0" to "Release of the core predictive engine.",
                    "Expansion" to "Scaling to distributed intelligence."
                )

                milestones.forEachIndexed { index, (title, desc) ->
                    TimelineItem(title, desc, index == milestones.lastIndex, primaryColor)
                }

                Spacer(Modifier.height(60.dp))

                // 5. CONNECT BENTO
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SocialBento(Icons.Default.Link, "LinkedIn", modifier = Modifier.weight(1f)) {
                        uriHandler.openUri("https://linkedin.com/in/murshid-r-37088b272")
                    }
                    SocialBento(Icons.Default.Code, "GitHub", modifier = Modifier.weight(1f)) {
                        uriHandler.openUri("https://github.com/murshidr")
                    }
                    SocialBento(Icons.Default.Email, "Email", modifier = Modifier.weight(1f)) {
                        uriHandler.openUri("mailto:murshidreyas@gmail.com")
                    }
                }

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun NeuralBackground(primary: Color, secondary: Color, isExcited: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing)),
        label = ""
    )

    val excitementScale by animateFloatAsState(
        targetValue = if (isExcited) 2f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "excitement"
    )

    Canvas(modifier = Modifier
        .fillMaxSize()
        .blur(80.dp)) {
        val width = size.width
        val height = size.height

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primary.copy(alpha = 0.15f * excitementScale.coerceAtMost(2f)),
                    Color.Transparent
                ),
                center = Offset(width * 0.2f + (width * 0.1f * phase), height * 0.3f),
                radius = width * 1.2f * excitementScale
            )
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    secondary.copy(alpha = 0.12f * excitementScale.coerceAtMost(2f)),
                    Color.Transparent
                ),
                center = Offset(width * 0.8f - (width * 0.1f * phase), height * 0.7f),
                radius = width * 1.0f * excitementScale
            )
        )
    }
}

@Composable
fun PulseCore(color: Color, isExcited: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val baseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(4000), RepeatMode.Reverse),
        label = ""
    )

    val excitementScale by animateFloatAsState(
        targetValue = if (isExcited) 1.5f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy),
        label = ""
    )

    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(240.dp)) {
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to color.copy(alpha = 0.2f),
                    1.0f to Color.Transparent
                ),
                radius = size.width / 2 * baseScale * excitementScale
            )
            drawCircle(
                color = color.copy(alpha = 0.1f),
                radius = (size.width / 2.5f) * (2f - baseScale) * excitementScale,
                style = Stroke(width = 2f)
            )
        }
    }
}

@Composable
fun BentoTile(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, colorScheme.onSurface.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    null,
                    tint = colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurface.copy(alpha = 0.4f),
                    letterSpacing = 2.sp
                )
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun TechChip(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
        )
    }
}

@Composable
fun TimelineItem(title: String, desc: String, isLast: Boolean, color: Color) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
                    .drawBehind {
                        drawCircle(
                            color = color.copy(alpha = 0.4f),
                            radius = size.width * 1.5f,
                            center = center
                        )
                    }
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(
                            Brush.verticalGradient(
                                listOf(color, color.copy(alpha = 0.1f))
                            )
                        )
                )
            }
        }
        Column(modifier = Modifier.padding(bottom = 32.dp, start = 8.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                desc,
                style = MaterialTheme.typography.bodyMedium,
                color = onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun SocialBento(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val view = LocalView.current
    Surface(
        modifier = modifier
            .height(80.dp)
            .clickable {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                onClick()
            },
        shape = RoundedCornerShape(20.dp),
        color = colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, colorScheme.onSurface.copy(alpha = 0.1f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, label, tint = colorScheme.onSurface.copy(alpha = 0.8f))
        }
    }
}
