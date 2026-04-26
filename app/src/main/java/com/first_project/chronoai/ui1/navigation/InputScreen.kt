package com.first_project.chronoai.ui1.navigation

import android.Manifest
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.first_project.chronoai.ui.theme.*
import com.first_project.chronoai.ui1.utils.HapticManager
import com.first_project.chronoai.voice.SpeechRecognitionManager
import com.first_project.chronoai.voice.VyntaVoiceManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

@Immutable
data class SubtaskItem(val id: String, val text: String, val isSelected: Boolean = true)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun InputScreen(
    viewModel: InputViewModel,
    onBack: () -> Unit,
    triggerMic: Boolean = true,
    taskId: Int? = null
) {
    val context = LocalContext.current
    val view = LocalView.current
    val focusManager = LocalFocusManager.current
    val speechManager = remember { SpeechRecognitionManager(context) }
    val hapticManager = remember { HapticManager(context) }
    val voiceManager = remember { VyntaVoiceManager(context) }
    
    val uiState by viewModel.uiState.collectAsState()
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val voicePersona = prefs.voicePersona
    
    val spokenText by speechManager.spokenText.collectAsState()
    val isListening by speechManager.isListening.collectAsState()
    val detectedContext by viewModel.detectedContext.collectAsState()
    
    var textInput by remember { mutableStateOf("") }
    val mainFocusRequester = remember { FocusRequester() }

    var subtasks by remember { mutableStateOf(listOf<SubtaskItem>()) }
    var newSubtaskText by remember { mutableStateOf("") }
    var selectedEnergy by remember { mutableStateOf("Medium") }
    
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedTime by remember { mutableStateOf<LocalTime?>(null) }
    var isRecurring by remember { mutableStateOf(false) }
    var recurrencePattern by remember { mutableStateOf<String?>(null) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    // AI Sync
    LaunchedEffect(detectedContext) {
        if (detectedContext.title.isNotEmpty() && textInput.isEmpty()) {
            textInput = detectedContext.title
        }
        if (detectedContext.energyLevel.isNotEmpty()) {
            selectedEnergy = detectedContext.energyLevel
        }
        if (detectedContext.subtasks.isNotEmpty() && subtasks.isEmpty()) {
            subtasks = detectedContext.subtasks.map { 
                SubtaskItem(UUID.randomUUID().toString(), it) 
            }
        }
        
        // Fix: Update selected date/time from AI detection even if user hasn't touched them
        // This ensures the chips reflect what the AI understood from your voice/text.
        if (detectedContext.deadlineDate != null) {
            try { selectedDate = LocalDate.parse(detectedContext.deadlineDate) } catch(e: Exception) {}
        }
        if (detectedContext.deadlineTime != null) {
            try { selectedTime = LocalTime.parse(detectedContext.deadlineTime) } catch(e: Exception) {}
        }

        isRecurring = detectedContext.isRecurring
        recurrencePattern = detectedContext.recurrencePattern
    }

    // Load for editing
    LaunchedEffect(taskId) {
        if (taskId != null) viewModel.loadTaskForEditing(context, taskId)
    }

    // Voice Input Sync
    LaunchedEffect(spokenText) {
        if (spokenText.isNotEmpty() && spokenText != "Listening..." && spokenText != "Processing..." && !spokenText.startsWith("Error:")) {
            textInput = spokenText
            viewModel.onTextChanged(spokenText)
        }
    }

    // Haptic Rain Pulse & State Transitions
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is InputUiState.Loading -> {
                var delayMs = 450L
                var intensity = 0.2f
                while (isActive) {
                    hapticManager.play(HapticManager.VyntaEffect.AI_PROCESSING, intensity)
                    delay(delayMs)
                    if (delayMs > 60L) delayMs = (delayMs * 0.91f).toLong().coerceAtLeast(60L)
                    if (intensity < 1.0f) intensity = (intensity + 0.05f).coerceAtMost(1.0f)
                }
            }
            is InputUiState.Success -> {
                hapticManager.play(HapticManager.VyntaEffect.SUCCESS)
                voiceManager.speak(state.personaMessage ?: state.message, voicePersona)
                delay(2000)
                onBack()
            }
            is InputUiState.Error -> {
                hapticManager.play(HapticManager.VyntaEffect.ERROR)
                voiceManager.speak(state.message, voicePersona)
            }
            is InputUiState.Conflict -> {
                hapticManager.play(HapticManager.VyntaEffect.ERROR)
                val msg = state.suggestion ?: "Your calendar is too busy."
                if (!voiceManager.isSpeaking.value) voiceManager.speak(msg, voicePersona)
            }
            else -> {}
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted && triggerMic) {
            hapticManager.play(HapticManager.VyntaEffect.MIC_TRIGGER)
            speechManager.startListening()
        }
    }

    LaunchedEffect(Unit) {
        if (taskId == null) {
            mainFocusRequester.requestFocus()
            if (triggerMic) permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(Unit) {
        onDispose { 
            voiceManager.shutdown()
            viewModel.resetState()
        }
    }

    val isConfirmEnabled by remember(textInput, uiState) {
        derivedStateOf { uiState !is InputUiState.Loading && textInput.isNotBlank() && uiState !is InputUiState.Conflict }
    }

    var isMicActive by remember { mutableStateOf(false) }
    LaunchedEffect(isListening) { isMicActive = isListening }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (taskId != null) "Edit Plan" else "New Plan", fontWeight = FontWeight.Black, letterSpacing = (-1).sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (isListening) {
                            speechManager.stopListening()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }) {
                        val iconScale by animateFloatAsState(if (isListening) 1.2f else 1f)
                        Icon(
                            if (isListening) Icons.Default.Mic else Icons.Default.MicNone,
                            null,
                            tint = if (isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.scale(iconScale)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).imePadding()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(Modifier.height(24.dp))

                // FOCUS-FIRST INPUT SECTION
                Box(modifier = Modifier.fillMaxWidth()) {
                    BasicTextField(
                        value = textInput,
                        onValueChange = {
                            textInput = it
                            viewModel.onTextChanged(it)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(mainFocusRequester)
                            .animateContentSize(),
                        textStyle = MaterialTheme.typography.displaySmall.copy(
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 44.sp
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            if (textInput.isEmpty()) {
                                Text(
                                    "Tell Vynta what to plan...",
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            innerTextField()
                        }
                    )
                }

                Spacer(Modifier.height(32.dp))

                // INTELLIGENT DASHBOARD (Chips)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PlanChip(
                        icon = Icons.Default.CalendarToday,
                        label = selectedDate?.format(DateTimeFormatter.ofPattern("MMM d")) ?: "Today",
                        onClick = { showDatePicker = true }
                    )
                    PlanChip(
                        icon = Icons.Default.Schedule,
                        label = if (selectedTime == null) "Optimal" else selectedTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "Optimal",
                        isActive = selectedTime != null,
                        onClick = { 
                            if (selectedTime != null) {
                                // If already has a time, toggle back to Optimal
                                selectedTime = null
                                hapticManager.play(HapticManager.VyntaEffect.CLICK)
                            } else {
                                // If Optimal, open picker to set a time
                                showTimePicker = true 
                            }
                        }
                    )
                    if (isRecurring) {
                        PlanChip(
                            icon = Icons.Default.Repeat,
                            label = "Recurring",
                            isActive = true,
                            onClick = { isRecurring = false }
                        )
                    }
                }

                Spacer(Modifier.height(40.dp))

                // ENERGY ARCHITECTURE
                Text("VITALITY LEVEL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("Low", "Medium", "High").forEach { energy ->
                        val isSelected = selectedEnergy == energy
                        EnergyOption(
                            label = energy,
                            isSelected = isSelected,
                            onClick = { 
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                selectedEnergy = energy 
                            }
                        )
                    }
                }

                Spacer(Modifier.height(48.dp))

                // PROCESS MAP (Subtasks)
                Text("PROCESS MAP", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)
                Spacer(Modifier.height(24.dp))
                
                Column(modifier = Modifier.fillMaxWidth()) {
                    subtasks.forEachIndexed { index, item ->
                        ProcessStep(
                            item = item,
                            isLast = index == subtasks.size - 1,
                            onToggle = {
                                subtasks = subtasks.map { if (it.id == item.id) it.copy(isSelected = !it.isSelected) else it }
                            },
                            onDelete = { subtasks = subtasks.filter { it.id != item.id } }
                        )
                    }
                    
                    // Add Step Field
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(16.dp))
                        BasicTextField(
                            value = newSubtaskText,
                            onValueChange = { newSubtaskText = it },
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (newSubtaskText.isNotBlank()) {
                                    subtasks = subtasks + SubtaskItem(UUID.randomUUID().toString(), newSubtaskText)
                                    newSubtaskText = ""
                                }
                            }),
                            decorationBox = { innerTextField ->
                                if (newSubtaskText.isEmpty()) {
                                    Text("Add step...", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                }
                                innerTextField()
                            }
                        )
                    }
                }

                Spacer(Modifier.height(100.dp))
            }

            // FLOATING CONFIRM ACTION
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background.copy(alpha = 0.9f)),
                            startY = 0f
                        )
                    )
                    .padding(24.dp)
            ) {
                Button(
                    onClick = {
                        if (isConfirmEnabled) {
                            val selectedSteps = subtasks.filter { it.isSelected }.map { it.text }
                            
                            // Check if time is NOT explicitly set (Optimal mode)
                            if (selectedTime == null) {
                                // Instead of scheduling, we trigger the AI to suggest a time first
                                // by calling the process function but the use case will return a Conflict (suggestion)
                                // since deadlineTime is null.
                                viewModel.processTask(
                                    context = context, 
                                    input = textInput, 
                                    energyOverride = selectedEnergy, 
                                    subtasksOverride = selectedSteps,
                                    dateOverride = selectedDate?.toString(),
                                    timeOverride = "OPTIMAL", // Force trigger findOptimalSlot
                                    recurrenceOverride = isRecurring,
                                    rruleOverride = recurrencePattern
                                )
                            } else {
                                viewModel.processTask(
                                    context = context, 
                                    input = textInput, 
                                    energyOverride = selectedEnergy, 
                                    subtasksOverride = selectedSteps,
                                    dateOverride = selectedDate?.toString(),
                                    timeOverride = selectedTime?.toString(),
                                    recurrenceOverride = isRecurring,
                                    rruleOverride = recurrencePattern
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(20.dp),
                    enabled = isConfirmEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    if (uiState is InputUiState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 3.dp)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(if (taskId != null) "Confirm Update" else "Confirm Schedule", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Success Overlay
            AnimatedVisibility(
                visible = uiState is InputUiState.Success,
                enter = fadeIn() + scaleIn(initialScale = 0.9f),
                exit = fadeOut()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.CheckCircle, 
                            null, 
                            tint = Color(0xFF81C784), 
                            modifier = Modifier.size(100.dp)
                        )
                        Spacer(Modifier.height(24.dp))
                        Text(
                            if (taskId != null) "Plan Updated" else "Plan Scheduled", 
                            style = MaterialTheme.typography.headlineMedium, 
                            fontWeight = FontWeight.Black
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            (uiState as? InputUiState.Success)?.personaMessage ?: "Architecture confirmed.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 40.dp)
                        )
                    }
                }
            }

            // Dialogs for Date and Time
            if (showDatePicker) {
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = selectedDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli() 
                        ?: Instant.now().toEpochMilli()
                )
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let {
                                selectedDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                            }
                            showDatePicker = false
                        }) { Text("Confirm") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            if (showTimePicker) {
                val timePickerState = rememberTimePickerState(
                    initialHour = selectedTime?.hour ?: LocalTime.now().hour,
                    initialMinute = selectedTime?.minute ?: LocalTime.now().minute
                )
                AlertDialog(
                    onDismissRequest = { showTimePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                            showTimePicker = false
                        }) { Text("Confirm") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                    },
                    text = {
                        TimePicker(state = timePickerState)
                    }
                )
            }

            // Conflict Overlay
            AnimatedVisibility(
                visible = uiState is InputUiState.Conflict,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                ConflictOverlay(uiState as? InputUiState.Conflict, onDismiss = { viewModel.resetState() }) { time ->
                     val selectedSteps = subtasks.filter { it.isSelected }.map { it.text }
                     viewModel.processTask(
                        context = context,
                        input = textInput,
                        energyOverride = selectedEnergy,
                        subtasksOverride = selectedSteps,
                        dateOverride = selectedDate?.toString(),
                        timeOverride = time,
                        recurrenceOverride = isRecurring,
                        rruleOverride = recurrencePattern,
                        skipAiReanalysis = true,
                        force = true
                    )
                }
            }
        }
    }
}

@Composable
fun PlanChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isActive: Boolean = false, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier.height(36.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, modifier = Modifier.size(14.dp), tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(6.dp))
            Text(
                label, 
                style = MaterialTheme.typography.labelMedium, 
                color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
fun EnergyOption(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (isSelected) 1.05f else 1f)
    
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.width(100.dp).scale(scale)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val weightIcon = when(label) {
                "Low" -> Icons.Default.Filter1
                "Medium" -> Icons.Default.Filter2
                else -> Icons.Default.Filter3
            }
            Icon(
                weightIcon, 
                null, 
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                label, 
                style = MaterialTheme.typography.labelLarge, 
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ProcessStep(item: SubtaskItem, isLast: Boolean, onToggle: () -> Unit, onDelete: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (item.isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                if (item.isSelected) Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimary)
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(40.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                )
            }
        }
        
        Spacer(Modifier.width(16.dp))
        
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                item.text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (item.isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun ConflictOverlay(state: InputUiState.Conflict?, onDismiss: () -> Unit, onAccept: (String?) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.ErrorOutline, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(24.dp))
            Text("Scheduling Conflict", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(16.dp))
            Text(
                state?.suggestion ?: "Your calendar is booked during this time.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(48.dp))
            Button(
                onClick = { onAccept(state?.suggestedTime) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Accept Optimized Time")
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onDismiss) {
                Text("Dismiss", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
