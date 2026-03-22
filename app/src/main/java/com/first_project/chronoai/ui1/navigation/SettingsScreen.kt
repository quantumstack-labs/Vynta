package com.first_project.chronoai.ui1.navigation

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.first_project.chronoai.data.CalendarAuthManager
import com.first_project.chronoai.ui.theme.*
import com.first_project.chronoai.ui1.viewmodel.ThemeMode
import com.first_project.chronoai.ui1.viewmodel.ThemeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onSignOut: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    themeViewModel: ThemeViewModel = viewModel()
) {
    val prefs by themeViewModel.prefs.collectAsStateWithLifecycle()
    val view = LocalView.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authManager = remember { CalendarAuthManager(context) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, null, tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
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
                                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                    themeViewModel.setEnergyWindows(it) 
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
                        Text("Neural Voice Persona", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Minimalist", "Motivator", "Professional").forEach { persona ->
                                FilterChip(
                                    selected = prefs.voicePersona == persona,
                                    onClick = { 
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        themeViewModel.setVoicePersona(persona) 
                                    },
                                    label = { Text(persona) },
                                    shape = CircleShape
                                )
                            }
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Focus Guard", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                Text("Silence distractions during deep work", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = prefs.focusGuardEnabled,
                                onCheckedChange = { 
                                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                    themeViewModel.setFocusGuard(it) 
                                }
                            )
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Dynamic Gap Logic", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                Text("Smart transitions between tasks", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = prefs.dynamicGapEnabled,
                                onCheckedChange = { 
                                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                    themeViewModel.setDynamicGap(it) 
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
                                    .size(40.dp)
                                    .background(MaterialTheme.colorScheme.surface, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Cloud,
                                    null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Google Calendar", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                Text("Connected & Synced", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
                            }
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                        }
                        
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            color = MaterialTheme.colorScheme.surface
                        )
                        
                        TextButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                scope.launch {
                                    authManager.signOut()
                                    onSignOut()
                                }
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Logout, null, tint = Color(0xFFE57373), modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("SIGN OUT", style = MaterialTheme.typography.labelLarge, color = Color(0xFFE57373), fontWeight = FontWeight.Bold)
                            }
                        }
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
                Button(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("SAVE CHANGES", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
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
