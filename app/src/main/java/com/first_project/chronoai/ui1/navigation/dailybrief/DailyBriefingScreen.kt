package com.first_project.chronoai.ui1.navigation.dailybrief

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.first_project.chronoai.ui.theme.VyntaTypography
import com.first_project.chronoai.ui1.viewmodel.HomeViewModel
import com.first_project.chronoai.ui1.navigation.VyntaCard
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DailyBriefingScreen(
    viewModel: HomeViewModel,
    onBack: () -> Unit
) {
    val tasks by viewModel.personalTasks.collectAsState()
    val events by viewModel.events.collectAsState()
    val progress by viewModel.completionProgress.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val charIndex by viewModel.currentVoiceCharIndex.collectAsState()
    val dailyBriefingResponse by viewModel.neuralInsights.collectAsState()
    val immersiveBriefing by viewModel.immersiveBriefing.collectAsState()
    val isSynthesizing by viewModel.isSynthesizing.collectAsState()
    
    // Derived companion greeting
    val companionGreeting = remember(dailyBriefingResponse) {
        val hour = LocalTime.now().hour
        val timeOfDay = when(hour) {
            in 5..11 -> "morning"
            in 12..17 -> "afternoon"
            else -> "evening"
        }
        "Awaiting your command for this $timeOfDay."
    }

    val dateStr = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d")) }

    // Now Brief: Immediate focus
    val nextActivity = remember(tasks, events) {
        val nowStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        val nextTask = tasks.filter { it.status != "COMPLETED" && it.deadlineTime != null && it.deadlineTime >= nowStr }
            .sortedBy { it.deadlineTime }.firstOrNull()
        val nextEvent = events.filter { it.start.dateTime != null && it.start.dateTime.toString().substring(11, 16) >= nowStr }
            .sortedBy { it.start.dateTime.toString() }.firstOrNull()
        
        if (nextEvent != null && (nextTask == null || (nextEvent.start.dateTime.toString().substring(11, 16) < nextTask.deadlineTime!!))) {
            Pair(nextEvent.summary ?: "Meeting", nextEvent.start.dateTime.toString().substring(11, 16))
        } else if (nextTask != null) {
            Pair(nextTask.title, nextTask.deadlineTime!!)
        } else null
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Mesh background for companion feel
        com.first_project.chronoai.ui1.navigation.AdaptiveMeshGradient(modifier = Modifier.fillMaxSize())

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                HeaderSection(onBack, dateStr)
            }

            item {
                CompanionGreetingSection(companionGreeting, isSpeaking)
            }

            item {
                AIStrategyModule(
                    isSynthesizing = isSynthesizing,
                    immersiveBriefing = immersiveBriefing,
                    charIndex = charIndex,
                    isSpeaking = isSpeaking,
                    onTogglePlayback = {
                        if (isSpeaking) viewModel.stopSpeaking() 
                        else viewModel.speak(immersiveBriefing)
                    }
                )
            }

            dailyBriefingResponse?.let { response ->
                item {
                    SectionHeader("STRATEGIC FORECAST", Icons.Default.AutoGraph)
                    StrategicForecastSection(response.insights)
                }
            }

            nextActivity?.let { (title, time) ->
                item {
                    SectionHeader("IMMEDIATE FOCUS", Icons.Default.Bolt)
                    NowBriefCard(title, time)
                }
            }

            item {
                SectionHeader("TIMELINE", Icons.Default.Timeline)
                ActionableTimeline(events)
            }

            item {
                SectionHeader("DAILY RESONANCE", Icons.Default.Analytics)
                UtilityProgressCard(progress, tasks.size, tasks.count { it.status == "COMPLETED" })
            }
        }
    }
}

@Composable
fun NowBriefCard(title: String, time: String) {
    VyntaCard(
        modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        showBorder = true
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(52.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Timer, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(20.dp))
            Column {
                Text("UP NEXT", style = VyntaTypography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                Text(title, style = VyntaTypography.titleLarge, fontWeight = FontWeight.Black)
                Text("Scheduled for $time", style = VyntaTypography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
fun HeaderSection(onBack: () -> Unit, date: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack, 
            modifier = Modifier
                .size(44.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), CircleShape)
                .border(0.5.dp, Color.White.copy(alpha = 0.1f), CircleShape)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                date.uppercase(), 
                style = VyntaTypography.labelLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Text(
                "Briefing", 
                style = VyntaTypography.displaySmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                )
            )
        }
        Spacer(Modifier.weight(1f))
        
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "live")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
                label = "alpha"
            )
            Box(modifier = Modifier.size(10.dp).graphicsLayer { this.alpha = alpha }.background(MaterialTheme.colorScheme.secondary, CircleShape))
        }
    }
}

@Composable
fun CompanionGreetingSection(greeting: String, isSpeaking: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val statusColor = if (isSpeaking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(statusColor, CircleShape)
                    .graphicsLayer { 
                        alpha = if (isSpeaking) 1f else 0.6f 
                    }
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = if (isSpeaking) "VYNTA IS COMMUNICATING" else "STRATEGY CORE READY",
                style = VyntaTypography.labelSmall,
                color = statusColor,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = greeting,
            style = VyntaTypography.headlineSmall.copy(fontWeight = FontWeight.Light, lineHeight = 32.sp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun AIStrategyModule(
    isSynthesizing: Boolean,
    immersiveBriefing: String,
    charIndex: Int,
    isSpeaking: Boolean,
    onTogglePlayback: () -> Unit
) {
    VyntaCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
        showBorder = true
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    com.first_project.chronoai.ui1.navigation.RefractiveCrystal(isSpeaking = isSpeaking)
                }
                Spacer(Modifier.width(16.dp))
                Text("NEURAL SYNTHESIS", style = VyntaTypography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
            }
            
            Spacer(Modifier.height(24.dp))

            Box(modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp), contentAlignment = Alignment.TopStart) {
                if (isSynthesizing) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 3.dp, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(16.dp))
                        Text("Decoding schedule patterns...", style = VyntaTypography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    }
                } else {
                    GlassBlurLyrics(
                        text = immersiveBriefing,
                        currentCharIndex = charIndex,
                        isSpeaking = isSpeaking,
                        textColor = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onTogglePlayback,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSpeaking) MaterialTheme.colorScheme.error.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(if (isSpeaking) Icons.Default.Stop else Icons.Default.PlayArrow, null)
                Spacer(Modifier.width(12.dp))
                Text(if (isSpeaking) "TERMINATE AUDIO" else "BEGIN BRIEFING", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun StrategicForecastSection(insights: List<HomeViewModel.StrategicInsight>) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        insights.forEach { insight ->
            val color = when {
                insight.urgency > 0.7f -> MaterialTheme.colorScheme.error
                insight.urgency > 0.4f -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.primary
            }

            VyntaCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = color.copy(alpha = 0.05f),
                showBorder = true,
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(color.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (insight.urgency > 0.7f) Icons.Default.PriorityHigh else Icons.Default.Lightbulb,
                                null,
                                modifier = Modifier.size(16.dp),
                                tint = color
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(insight.label.uppercase(), style = VyntaTypography.labelSmall, color = color, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Text(
                        insight.observation, 
                        style = VyntaTypography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 24.sp
                    )
                    
                    Spacer(Modifier.height(8.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AdsClick, null, modifier = Modifier.size(16.dp), tint = color)
                            Spacer(Modifier.width(12.dp))
                            Text(insight.action, style = VyntaTypography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon, 
            null, 
            modifier = Modifier.size(18.dp), 
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            style = VyntaTypography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            letterSpacing = 1.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ActionableTimeline(events: List<com.google.api.services.calendar.model.Event>) {
    if (events.isEmpty()) {
        UtilityEmptyState("No events scheduled for today.")
    } else {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            events.take(4).forEach { event ->
                TimelineNode(
                    title = event.summary ?: "Busy",
                    time = event.start.dateTime?.toString()?.substring(11, 16) ?: "All Day",
                    location = event.location
                )
            }
        }
    }
}

@Composable
fun TimelineNode(title: String, time: String, location: String? = null) {
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(32.dp)) {
            Box(modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
            Box(modifier = Modifier.width(1.dp).weight(1f).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)))
        }
        VyntaCard(
            modifier = Modifier.weight(1f),
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.08f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = VyntaTypography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text(time, style = VyntaTypography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                if (location != null) {
                    Text(location, style = VyntaTypography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
fun UtilityProgressCard(progress: Float, total: Int, completed: Int) {
    VyntaCard(
        modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.03f)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("COMPLETION", style = VyntaTypography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                    Text("$completed of $total secured", style = VyntaTypography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                }
                Text("${(progress * 100).toInt()}%", style = VyntaTypography.displaySmall.copy(fontWeight = FontWeight.Black))
            }
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}

@Composable
fun UtilityEmptyState(text: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(64.dp).border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = VyntaTypography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GlassBlurLyrics(
    text: String, 
    currentCharIndex: Int,
    isSpeaking: Boolean,
    textColor: Color
) {
    val wordsWithMetadata = remember(text) {
        var currentPos = 0
        text.split(Regex("\\s+")).map { word ->
            val start = text.indexOf(word, currentPos)
            val end = start + word.length
            currentPos = end
            Triple(word, start, end)
        }
    }

    FlowRow(
        modifier = Modifier.fillMaxWidth()
    ) {
        wordsWithMetadata.forEach { (word, start, end) ->
            val isPast = currentCharIndex > end
            val isCurrent = currentCharIndex in start..end
            val isIdle = !isSpeaking

            val blur by animateDpAsState(
                targetValue = when {
                    isCurrent || isPast || isIdle -> 0.dp
                    else -> 6.dp
                },
                animationSpec = tween(400)
            )
            
            val opacity by animateFloatAsState(
                targetValue = when {
                    isCurrent -> 1f
                    isPast -> 0.4f
                    isIdle -> 0.9f
                    else -> 0.2f
                },
                animationSpec = tween(400)
            )

            val scale by animateFloatAsState(
                targetValue = if (isCurrent) 1.15f else 1f,
                animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioLowBouncy)
            )

            val glow by animateColorAsState(
                targetValue = if (isCurrent) MaterialTheme.colorScheme.primary else textColor.copy(alpha = if (isPast) 0.6f else 0.2f),
                animationSpec = tween(300)
            )

            Text(
                text = "$word ",
                style = VyntaTypography.headlineSmall.copy(
                    fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Bold,
                    color = glow,
                    lineHeight = 36.sp,
                    letterSpacing = (-0.5).sp
                ),
                modifier = Modifier
                    .graphicsLayer { 
                        alpha = opacity
                        scaleX = scale
                        scaleY = scale
                    }
                    .blur(blur)
                    .padding(vertical = 4.dp, horizontal = 2.dp)
            )
        }
    }
}
