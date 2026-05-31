package com.first_project.chronoai.ui1.navigation

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.first_project.chronoai.ui.theme.*
import com.first_project.chronoai.ui1.viewmodel.ThemeMode
import com.first_project.chronoai.ui1.viewmodel.ThemeViewModel
import com.first_project.chronoai.voice.VyntaVoiceManager
import com.first_project.chronoai.data.CalendarRepository
import com.first_project.chronoai.data.local.db.DatabaseProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.Calendar
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.first_project.chronoai.ui1.util.HapticManager
import androidx.compose.ui.draw.shadow
import kotlin.math.roundToInt
import com.first_project.chronoai.data.local.prefs.SchedulingPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onSignOut: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToManual: () -> Unit = {},
    themeViewModel: ThemeViewModel = viewModel()
) {
    val prefs by themeViewModel.prefs.collectAsStateWithLifecycle()
    val view = LocalView.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val userEmail = remember { com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)?.email ?: "Not signed in" }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val voiceManager = remember { VyntaVoiceManager(context) }
    DisposableEffect(Unit) {
        onDispose { voiceManager.shutdown() }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out of Vynta?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    onSignOut()
                }) {
                    Text("SIGN OUT", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AdaptiveMeshGradient()

        Scaffold(
            containerColor = Color.Transparent
        ) { padding ->
            var visibleItems by remember { mutableIntStateOf(0) }
            LaunchedEffect(Unit) {
                repeat(8) {
                    delay(100)
                    visibleItems++
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 140.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    AnimatedVisibility(visible = visibleItems > 0, enter = fadeIn() + slideInVertically { 20 }) {
                        Column {
                            Text("Settings", style = VyntaTypography.displayLarge)
                            Text("CURATE YOUR TEMPORAL EXPERIENCE", style = VyntaTypography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                item {
                    AnimatedVisibility(visible = visibleItems > 1, enter = fadeIn() + slideInVertically { 20 }) {
                        Column {
                            SettingsSectionHeader("APPEARANCE")
                            VyntaCard(
                                modifier = Modifier.fillMaxWidth(),
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ) {
                                Column(modifier = Modifier.padding(24.dp)) {
                                    Text("Theme Mode", style = VyntaTypography.titleMedium)
                                    Spacer(Modifier.height(20.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.SYSTEM).forEach { mode ->
                                            ThemeButton(
                                                label = mode.name,
                                                isSelected = prefs.themeMode == mode,
                                                icon = when(mode) {
                                                    ThemeMode.LIGHT -> Icons.Default.WbSunny
                                                    ThemeMode.DARK -> Icons.Default.DarkMode
                                                    ThemeMode.SYSTEM -> Icons.Default.Dns
                                                },
                                                onClick = { 
                                                    if (prefs.hapticsEnabled) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                                    themeViewModel.setThemeMode(mode) 
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    AnimatedVisibility(visible = visibleItems > 2, enter = fadeIn() + slideInVertically { 20 }) {
                        Column {
                            SettingsSectionHeader("CHRONOTYPE ARCHITECT")
                            VyntaCard(
                                modifier = Modifier.fillMaxWidth(),
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ) {
                                Column(modifier = Modifier.padding(24.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Energy Windows", style = VyntaTypography.titleMedium, modifier = Modifier.weight(1f))
                                        Icon(Icons.Default.ElectricBolt, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
                                    }
                                    Text("Calibrate AI to your biological peaks.", style = VyntaTypography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    
                                    Spacer(Modifier.height(24.dp))
                                    
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column {
                                            Text("${prefs.workStart.roundToInt()}:00", style = VyntaTypography.labelSmall)
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            val duration = prefs.workEnd - prefs.workStart
                                            val label = when {
                                                duration < 6 -> "HYPER-FOCUS"
                                                duration > 14 -> "LONG-RUN"
                                                else -> "BALANCED"
                                            }
                                            Text(label, style = VyntaTypography.labelSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("${prefs.workEnd.roundToInt()}:00", style = VyntaTypography.labelSmall)
                                        }
                                    }
                                    Spacer(Modifier.height(16.dp))
                                    VyntaVelocitySlider(
                                        value = prefs.workStart..prefs.workEnd,
                                        onValueChange = { range ->
                                            themeViewModel.setWorkHours(range.start, range.endInclusive)
                                        }
                                    )
                                    
                                    Spacer(Modifier.height(24.dp))
                                    val impactDescription = remember(prefs.workStart, prefs.workEnd) {
                                        val duration = prefs.workEnd - prefs.workStart
                                        when {
                                            duration < 6 -> "NEURAL IMPACT: AI will strictly prioritize high-impact missions in this short window."
                                            duration > 14 -> "NEURAL IMPACT: AI will space missions generously to optimize for long-term endurance."
                                            else -> "NEURAL IMPACT: AI will follow your natural circadian rhythms for a balanced flow."
                                        }
                                    }
                                    Text(impactDescription, style = VyntaTypography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                item {
                    AnimatedVisibility(visible = visibleItems > 3, enter = fadeIn() + slideInVertically { 20 }) {
                        Column {
                            SettingsSectionHeader("INTELLIGENT PARTNER")
                            VyntaCard(
                                modifier = Modifier.fillMaxWidth(),
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ) {
                                Column(modifier = Modifier.padding(24.dp)) {
                                    Text("Neural Voice Persona", style = VyntaTypography.titleMedium)
                                    Spacer(Modifier.height(16.dp))
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("Atlas", "Lyra", "Sloane", "Orion").forEach { persona ->
                                            FilterChip(
                                                selected = prefs.voicePersona == persona,
                                                onClick = { 
                                                    themeViewModel.setVoicePersona(persona)
                                                    val sampleText = when(persona) {
                                                        "Atlas" -> "Duty and discipline are the foundations of your day."
                                                        "Lyra" -> "Let's go! Time to crush your goals and win the day!"
                                                        "Sloane" -> "Optimizing your temporal flow for maximum efficiency."
                                                        "Orion" -> "Find harmony in your schedule, and the day will follow your rhythm."
                                                        else -> "Hello, I am Vynta."
                                                    }
                                                    voiceManager.speak(sampleText, persona)
                                                },
                                                label = { Text(persona) },
                                                shape = ShapePill
                                            )
                                        }
                                    }
                                    
                                    Spacer(Modifier.height(24.dp))
                                    
                                    SettingsToggle(
                                        title = "Focus Guard",
                                        subtitle = "Block intrusive pings.",
                                        checked = prefs.focusShieldEnabled,
                                        onCheckedChange = { 
                                            if (it) HapticManager.playToggleOn(view) else HapticManager.playToggleOff(view)
                                            themeViewModel.setFocusShield(it) 
                                        }
                                    )
                                    
                                    Spacer(Modifier.height(16.dp))
                                    
                                    SettingsToggle(
                                        title = "Dynamic Gap Logic",
                                        subtitle = "Auto-buffer transitions.",
                                        checked = prefs.smartSpacingEnabled,
                                        onCheckedChange = { 
                                            if (it) HapticManager.playToggleOn(view) else HapticManager.playToggleOff(view)
                                            themeViewModel.setSmartSpacing(it) 
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    AnimatedVisibility(visible = visibleItems > 4, enter = fadeIn() + slideInVertically { 20 }) {
                        Column {
                            SettingsSectionHeader("SYSTEM")
                            VyntaCard(
                                modifier = Modifier.fillMaxWidth(),
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ) {
                                Column(modifier = Modifier.padding(24.dp)) {
                                    SettingsToggle(
                                        title = "Haptic Feedback",
                                        subtitle = "Tactile response for interactions.",
                                        checked = prefs.hapticsEnabled,
                                        onCheckedChange = { 
                                            // Always play toggle haptic if possible for this specific toggle
                                            if (it) {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                                    view.performHapticFeedback(HapticFeedbackConstants.TOGGLE_ON)
                                                } else {
                                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                                }
                                            } else {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                                    view.performHapticFeedback(HapticFeedbackConstants.TOGGLE_OFF)
                                                } else {
                                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                                }
                                            }
                                            themeViewModel.setHapticsEnabled(it) 
                                        }
                                    )
                                    
                                    Spacer(Modifier.height(16.dp))
                                    
                                    SettingsToggle(
                                        title = "Neural Link",
                                        subtitle = "Receive AI-driven notifications.",
                                        checked = prefs.notificationsEnabled,
                                        onCheckedChange = { 
                                            if (it) HapticManager.playToggleOn(view) else HapticManager.playToggleOff(view)
                                            themeViewModel.setNotificationsEnabled(it) 
                                        }
                                    )

                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        Spacer(Modifier.height(16.dp))
                                        SettingsToggle(
                                            title = "Dynamic Colors",
                                            subtitle = "Material You system theme.",
                                            checked = prefs.dynamicColorsEnabled,
                                            onCheckedChange = { 
                                                if (it) HapticManager.playToggleOn(view) else HapticManager.playToggleOff(view)
                                                themeViewModel.setDynamicColors(it) 
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    AnimatedVisibility(visible = visibleItems > 5, enter = fadeIn() + slideInVertically { 20 }) {
                        Column {
                            SettingsSectionHeader("SECURITY")
                            VyntaCard(
                                modifier = Modifier.fillMaxWidth(),
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ) {
                                Column(modifier = Modifier.padding(24.dp)) {
                                    SettingsToggle(
                                        title = "Biometric Lock",
                                        subtitle = "Secure your temporal data.",
                                        checked = prefs.biometricLockEnabled,
                                        onCheckedChange = { 
                                            if (it) HapticManager.playToggleOn(view) else HapticManager.playToggleOff(view)
                                            themeViewModel.setBiometricLock(it) 
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    AnimatedVisibility(visible = visibleItems > 6, enter = fadeIn() + slideInVertically { 20 }) {
                        Column {
                            SettingsSectionHeader("INFORMATION")
                            VyntaCard(
                                modifier = Modifier.fillMaxWidth(),
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                showBorder = true
                            ) {
                                Column(modifier = Modifier.padding(24.dp)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(VyntaShapes.medium)
                                            .clickable { 
                                                if (prefs.hapticsEnabled) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                                onNavigateToAbout() 
                                            }
                                            .padding(vertical = 12.dp, horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.width(16.dp))
                                        Text("About Vynta", style = VyntaTypography.titleMedium)
                                        Spacer(Modifier.weight(1f))
                                        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(VyntaShapes.medium)
                                            .clickable { 
                                                if (prefs.hapticsEnabled) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                                onNavigateToManual() 
                                            }
                                            .padding(vertical = 12.dp, horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.MenuBook, null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.width(16.dp))
                                        Text("User Manual", style = VyntaTypography.titleMedium)
                                        Spacer(Modifier.weight(1f))
                                        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    AnimatedVisibility(visible = visibleItems > 7, enter = fadeIn() + slideInVertically { 20 }) {
                        Button(
                            onClick = { 
                                if (prefs.hapticsEnabled) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                showLogoutDialog = true 
                            },
                            modifier = Modifier.fillMaxWidth().height(64.dp),
                            shape = ShapePill,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.Logout, null)
                                Spacer(Modifier.width(12.dp))
                                Text("Sign Out of Vynta Ecosystem", style = VyntaTypography.titleMedium)
                            }
                        }
                    }
                }
                
                item { Spacer(Modifier.height(140.dp)) }
            }
        }
    }
}

@Composable
fun SettingsToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = VyntaTypography.titleMedium)
            Text(subtitle, style = VyntaTypography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        VyntaSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun VyntaSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    val transition = updateTransition(targetState = checked, label = "SwitchTransition")
    
    val thumbOffset by transition.animateDp(
        transitionSpec = { spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy) },
        label = "ThumbOffset"
    ) { if (it) 24.dp else 0.dp }

    val trackColor by transition.animateColor(label = "TrackColor") {
        if (it) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) 
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }
    
    val thumbColor by transition.animateColor(label = "ThumbColor") {
        if (it) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline
    }

    val glowColor = MaterialTheme.colorScheme.primary.copy(alpha = if (checked) 0.5f else 0f)
    
    Box(
        modifier = Modifier
            .size(width = 52.dp, height = 28.dp)
            .clip(CircleShape)
            .background(trackColor)
            .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(20.dp)
                .shadow(elevation = if (checked) 8.dp else 0.dp, shape = CircleShape, ambientColor = glowColor, spotColor = glowColor)
                .clip(CircleShape)
                .background(thumbColor)
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun SettingsScreenPreview() {
    VyntaTheme {
        // We cannot easily preview the full SettingsScreen because of the ViewModel dependency
        // In a real scenario, we'd use a stateless version of the screen
        Text("Settings Screen Preview (ViewModel dependency)", modifier = Modifier.padding(24.dp))
    }
}

@Composable
fun ThemeButton(label: String, isSelected: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier.size(100.dp).scale(scale),
        shape = CircleShape,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null)
            Spacer(Modifier.height(8.dp))
            Text(label, style = VyntaTypography.labelSmall)
        }
    }
}

@Composable
fun SettingsSectionHeader(text: String) {
    Text(
        text = text,
        style = VyntaTypography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}
