package com.first_project.chronoai.ui1.navigation

import android.view.HapticFeedbackConstants
import com.google.android.gms.auth.api.signin.GoogleSignIn
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.first_project.chronoai.data.CalendarAuthManager
import com.first_project.chronoai.ui.theme.*
import com.first_project.chronoai.ui1.viewmodel.ThemeMode
import com.first_project.chronoai.ui1.viewmodel.ThemeViewModel
import com.first_project.chronoai.ui1.utils.FocusManager
import com.first_project.chronoai.voice.VyntaVoiceManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
    val authManager = remember { CalendarAuthManager(context) }
    val voiceManager = remember { VyntaVoiceManager(context) }
    
    val userEmail = remember { GoogleSignIn.getLastSignedInAccount(context)?.email ?: "Not signed in" }
    var showLogoutDialog by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            voiceManager.shutdown()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                SettingsSectionHeader("APPEARANCE")
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Theme Mode", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ThemeToggleButton("Light", prefs.themeMode == ThemeMode.LIGHT) { 
                                themeViewModel.setThemeMode(ThemeMode.LIGHT) 
                            }
                            ThemeToggleButton("Dark", prefs.themeMode == ThemeMode.DARK) { 
                                themeViewModel.setThemeMode(ThemeMode.DARK) 
                            }
                            ThemeToggleButton("System", prefs.themeMode == ThemeMode.SYSTEM) { 
                                themeViewModel.setThemeMode(ThemeMode.SYSTEM) 
                            }
                        }
                    }
                }
            }

            item {
                SettingsSectionHeader("CHRONOTYPE ARCHITECT")
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Energy Windows", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                Text("Align tasks with productivity peaks", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = prefs.energyWindowsEnabled,
                                onCheckedChange = { 
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    themeViewModel.setEnergyWindows(it) 
                                },
                                thumbContent = {
                                    AnimatedContent(
                                        targetState = prefs.energyWindowsEnabled,
                                        transitionSpec = {
                                            (scaleIn(animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), initialScale = 0.5f) + fadeIn())
                                                .togetherWith(scaleOut(targetScale = 0.5f) + fadeOut())
                                        },
                                        label = "switch_icon"
                                    ) { isEnabled ->
                                        Icon(
                                            imageVector = if (isEnabled) Icons.Rounded.Check else Icons.Rounded.Close,
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize)
                                        )
                                    }
                                }
                            )
                        }
                        
                        Spacer(Modifier.height(24.dp))
                        
                        Text("Active Hours: ${prefs.workStart.toInt()}:00 - ${prefs.workEnd.toInt()}:00", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        RangeSlider(
                            value = prefs.workStart..prefs.workEnd,
                            onValueChange = { range ->
                                if (range.start.toInt() != prefs.workStart.toInt() || range.endInclusive.toInt() != prefs.workEnd.toInt()) {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                }
                                themeViewModel.setWorkHours(range.start, range.endInclusive)
                            },
                            valueRange = 0f..24f,
                            steps = 23,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            item {
                SettingsSectionHeader("INTELLIGENT PARTNER")
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Vocal Essence", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Atlas", "Lyra", "Sloane", "Orion").forEach { persona ->
                                FilterChip(
                                    selected = prefs.voicePersona == persona,
                                    onClick = { 
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        themeViewModel.setVoicePersona(persona) 
                                        
                                        val phrase = when (persona) {
                                            "Atlas" -> "I am Atlas. I will ensure your schedule remains balanced and your objectives stay on track. Shall we begin?"
                                            "Lyra" -> "I'm Lyra. I'll be here to keep your momentum high and your day flowing smoothly. You've got this!"
                                            "Sloane" -> "This is Sloane. My focus is your efficiency. I’ll keep the briefings concise and your focus sharp."
                                            "Orion" -> "I am Orion. I've analyzed your upcoming tasks and I'm ready to help you optimize your time today."
                                            else -> "Hello, I am your Vynta assistant."
                                        }
                                        voiceManager.speak(phrase, persona)
                                    },
                                    label = { Text(persona) },
                                    shape = CircleShape
                                )
                            }
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Focus Shield", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                Text("Automatically silences notifications so you can stay in the flow", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = prefs.focusShieldEnabled,
                                onCheckedChange = { isChecked -> 
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    if (isChecked) {
                                        val focusManager = FocusManager(context)
                                        if (!focusManager.hasDndPermission()) {
                                            focusManager.requestDndPermission()
                                        }
                                    }
                                    themeViewModel.setFocusShield(isChecked)
                                },
                                thumbContent = {
                                    AnimatedContent(
                                        targetState = prefs.focusShieldEnabled,
                                        transitionSpec = {
                                            (scaleIn(animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), initialScale = 0.5f) + fadeIn())
                                                .togetherWith(scaleOut(targetScale = 0.5f) + fadeOut())
                                        },
                                        label = "switch_icon"
                                    ) { isEnabled ->
                                        Icon(
                                            imageVector = if (isEnabled) Icons.Rounded.Check else Icons.Rounded.Close,
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize)
                                        )
                                    }
                                }
                            )
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Smart Spacing", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                Text("Intelligently adds breathing room between tasks to prevent burnout", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = prefs.smartSpacingEnabled,
                                onCheckedChange = { 
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    themeViewModel.setSmartSpacing(it) 
                                },
                                thumbContent = {
                                    AnimatedContent(
                                        targetState = prefs.smartSpacingEnabled,
                                        transitionSpec = {
                                            (scaleIn(animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), initialScale = 0.5f) + fadeIn())
                                                .togetherWith(scaleOut(targetScale = 0.5f) + fadeOut())
                                        },
                                        label = "switch_icon"
                                    ) { isEnabled ->
                                        Icon(
                                            imageVector = if (isEnabled) Icons.Rounded.Check else Icons.Rounded.Close,
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }

            item {
                SettingsSectionHeader("ACCOUNT")
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CloudQueue,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                // Live Connection Pulse
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .align(Alignment.TopEnd)
                                        .background(Color(0xFF81C784), CircleShape)
                                        .border(2.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Google Calendar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text(userEmail, style = MaterialTheme.typography.labelSmall, color = Color(0xFF81C784))
                            }
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        
                        TextButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                showLogoutDialog = true
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Logout, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text("Do you want to Log out? 🥺", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                SettingsSectionHeader("MANUAL")
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        onNavigateToManual()
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.MenuBook, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("How Vynta Works", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text("Guide to your AI partner", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item {
                SettingsSectionHeader("ABOUT")
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        onNavigateToAbout()
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("About the Developer", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text("The vision behind Vynta", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item {
                Spacer(Modifier.height(32.dp))
            }
        }

        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                icon = { Icon(Icons.Default.SentimentVeryDissatisfied, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error) },
                title = { Text("Are you sure? Really? 🥺", fontWeight = FontWeight.Black) },
                text = { Text("You'll definitely miss me. We're a great team, aren't we?", textAlign = TextAlign.Center) },
                confirmButton = {
                    Button(
                        onClick = {
                            showLogoutDialog = false
                            scope.launch {
                                authManager.signOut()
                                onSignOut()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Yes, Log out")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("No, Stay with me")
                    }
                }
            )
        }
    }
}

@Composable
fun SettingsSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 28.dp, bottom = 12.dp),
        letterSpacing = 1.5.sp
    )
}

@Composable
fun ThemeToggleButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = if (isSelected) null else ButtonDefaults.outlinedButtonBorder,
        modifier = Modifier.height(40.dp)
    ) {
        Box(modifier = Modifier.padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
