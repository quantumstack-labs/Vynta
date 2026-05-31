package com.first_project.chronoai.ui1.navigation

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.first_project.chronoai.ui.theme.*
import com.first_project.chronoai.ui1.utils.HapticManager
import com.first_project.chronoai.voice.SpeechRecognitionManager
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

@Immutable
data class SubtaskItem(val id: String, val text: String, val isSelected: Boolean = true)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun InputScreen(
    viewModel: InputViewModel,
    onBack: () -> Unit,
    triggerMic: Boolean = true,
    taskId: Int? = null,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val context = LocalContext.current
    val speechManager = remember { SpeechRecognitionManager(context) }
    val hapticManager = remember { HapticManager(context) }
    
    val uiState by viewModel.uiState.collectAsState()
    val spokenText by speechManager.spokenText.collectAsState()
    val detectedContext by viewModel.detectedContext.collectAsState()
    
    var textInput by remember { mutableStateOf("") }
    val mainFocusRequester = remember { FocusRequester() }

    var subtasks by remember { mutableStateOf(listOf<SubtaskItem>()) }
    var selectedEnergy by remember { mutableStateOf("Medium") }
    var durationMinutes by remember { mutableStateOf(60) }
    
    var selectedDate by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }
    var selectedTime by remember { mutableStateOf<LocalTime?>(LocalTime.now().withSecond(0).withNano(0)) }

    var showRefineOptions by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    var newSubtaskText by remember { mutableStateOf("") }
    var hasInitializedFromContext by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    LaunchedEffect(taskId) {
        if (taskId != null) {
            viewModel.loadTaskForEditing(context, taskId)
            hasInitializedFromContext = false
        } else {
            viewModel.resetState()
            hasInitializedFromContext = true
        }
    }

    LaunchedEffect(detectedContext) {
        // Reset subtasks if the context is empty (e.g. after a reset)
        if (detectedContext.title.isBlank() && detectedContext.subtasks.isEmpty()) {
            subtasks = emptyList()
        }

        // Only update title if it's the very first time (initial load for editing)
        if (!hasInitializedFromContext && detectedContext.title.isNotBlank()) {
            textInput = detectedContext.title
            hasInitializedFromContext = true
        }
        
        selectedEnergy = detectedContext.energyLevel
        durationMinutes = detectedContext.durationMinutes
        
        // Sync subtasks while preserving selection state if possible
        val currentSubtaskTexts = subtasks.map { it.text }
        if (detectedContext.subtasks.isNotEmpty() && detectedContext.subtasks != currentSubtaskTexts) {
            if (subtasks.isEmpty() || !hasInitializedFromContext) {
                 subtasks = detectedContext.subtasks.map { SubtaskItem(UUID.randomUUID().toString(), it) }
            }
        }
        
        detectedContext.deadlineDate?.let {
            try { selectedDate = LocalDate.parse(it) } catch (e: Exception) {}
        }
        detectedContext.deadlineTime?.let {
            try { selectedTime = LocalTime.parse(it) } catch (e: Exception) {}
        }
    }

    LaunchedEffect(spokenText) {
        if (spokenText.isNotEmpty() && spokenText != "Listening..." && spokenText != "Processing..." && !spokenText.startsWith("Error:")) {
            textInput = spokenText
            viewModel.onTextChanged(spokenText)
        }
    }

    LaunchedEffect(uiState) {
        val state = uiState
        when (state) {
            is InputUiState.Loading -> {
                hapticManager.play(HapticManager.VyntaEffect.THROB)
            }
            is InputUiState.Success -> {
                hapticManager.stop()
                hapticManager.play(HapticManager.VyntaEffect.SUCCESS)
                viewModel.speak(state.personaMessage)
                delay(1500)
                viewModel.clearSuccessState()
                onBack()
            }
            is InputUiState.Error, is InputUiState.Conflict -> {
                hapticManager.stop()
                hapticManager.play(HapticManager.VyntaEffect.ERROR)
            }
            else -> {
                hapticManager.stop()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted && triggerMic) speechManager.startListening()
    }

    LaunchedEffect(Unit) {
        delay(300)
        mainFocusRequester.requestFocus()
        if (taskId == null && triggerMic) permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    val isConfirmEnabled by remember(textInput, uiState) {
        derivedStateOf { uiState !is InputUiState.Loading && textInput.isNotBlank() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AdaptiveMeshGradient()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("MISSION CENTRAL", style = VyntaTypography.labelSmall, letterSpacing = 2.sp) },
                    navigationIcon = {
                        IconButton(onClick = { onBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                    },
                    actions = {
                        IconButton(onClick = { speechManager.startListening() }) {
                            val isListening = speechManager.isListening.collectAsState().value
                            Icon(
                                if (isListening) Icons.Default.GraphicEq else Icons.Default.Mic, 
                                null, 
                                tint = if (isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            },
            bottomBar = {
                // FLOATING REFINEMENT BAR
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp).navigationBarsPadding()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                        shadowElevation = 12.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            RefineAction(
                                icon = Icons.Default.ElectricBolt,
                                label = selectedEnergy.uppercase(),
                                onClick = { showRefineOptions = !showRefineOptions }
                            )
                            RefineAction(
                                icon = Icons.Default.Timer,
                                label = "$durationMinutes MINS",
                                onClick = { showRefineOptions = !showRefineOptions }
                            )
                            RefineAction(
                                icon = Icons.Default.CalendarToday,
                                label = selectedDate?.format(DateTimeFormatter.ofPattern("MMM d")) ?: "DATE",
                                onClick = { showDatePicker = true }
                            )
                            RefineAction(
                                icon = Icons.Default.Schedule,
                                label = selectedTime?.format(DateTimeFormatter.ofPattern("hh:mm a")) ?: "TIME",
                                onClick = { showTimePicker = true }
                            )
                            
                            // THE PRIMARY CONFIRM FAB-LIKE BUTTON
                            Button(
                                onClick = {
                                    val steps = subtasks.filter { it.isSelected }.map { it.text }
                                    viewModel.processTask(
                                        context = context, 
                                        input = textInput, 
                                        energyOverride = selectedEnergy, 
                                        subtasksOverride = steps, 
                                        dateOverride = selectedDate?.toString(), 
                                        timeOverride = selectedTime?.toString() ?: "OPTIMAL",
                                        durationOverride = durationMinutes
                                    )
                                },
                                modifier = Modifier.size(56.dp),
                                shape = CircleShape,
                                contentPadding = PaddingValues(0.dp),
                                enabled = isConfirmEnabled,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                if (uiState is InputUiState.Loading) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Check, null)
                                }
                            }
                        }
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(scrollState).padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Spacer(Modifier.height(16.dp))

                // FEEDBACK SECTION (Conflict/Error)
                if (uiState is InputUiState.Conflict || uiState is InputUiState.Error) {
                    VyntaCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = if (uiState is InputUiState.Conflict) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (uiState is InputUiState.Conflict) Icons.Default.Warning else Icons.Default.ErrorOutline,
                                    null,
                                    tint = if (uiState is InputUiState.Conflict) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    if (uiState is InputUiState.Conflict) "Temporal Conflict" else "System Obstacle",
                                    style = VyntaTypography.titleMedium,
                                    color = if (uiState is InputUiState.Conflict) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                when (val s = uiState) {
                                    is InputUiState.Conflict -> s.suggestion ?: "I've detected a clash in your timeline."
                                    is InputUiState.Error -> s.message
                                    else -> ""
                                },
                                style = VyntaTypography.bodyMedium,
                                color = if (uiState is InputUiState.Conflict) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                    .copy(alpha = 0.8f)
                            )
                            
                            if (uiState is InputUiState.Conflict) {
                                val s = uiState as InputUiState.Conflict
                                if (s.suggestedTime != null) {
                                    Spacer(Modifier.height(16.dp))
                                    Button(
                                        onClick = {
                                            try {
                                                selectedTime = LocalTime.parse(s.suggestedTime)
                                                val steps = subtasks.filter { it.isSelected }.map { it.text }
                                                viewModel.processTask(
                                                    context = context, 
                                                    input = textInput, 
                                                    energyOverride = selectedEnergy, 
                                                    subtasksOverride = steps, 
                                                    dateOverride = selectedDate?.toString(), 
                                                    timeOverride = s.suggestedTime,
                                                    durationOverride = durationMinutes,
                                                    skipAiReanalysis = true
                                                )
                                            } catch (e: Exception) {}
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                                    ) {
                                        Text("Adopt Suggestion (${s.suggestedTime})")
                                    }
                                }
                            }
                        }
                    }
                }

                // MISSION INPUT - LARGE & ELEGANT
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.1f,
                    targetValue = 0.3f,
                    animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse),
                    label = "alpha"
                )
                val primaryColor = MaterialTheme.colorScheme.primary

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawWithContent {
                            if (textInput.isNotEmpty()) {
                                drawRect(
                                    brush = Brush.radialGradient(
                                        colors = listOf(primaryColor.copy(alpha = pulseAlpha), Color.Transparent),
                                        center = this.center,
                                        radius = this.size.maxDimension / 1.5f
                                    )
                                )
                            }
                            drawContent()
                        }
                ) {
                    Text("OBJECTIVE", style = VyntaTypography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(16.dp))
                    BasicTextField(
                        value = textInput,
                        onValueChange = {
                            textInput = it
                            viewModel.onTextChanged(it, context)
                        },
                        modifier = Modifier.fillMaxWidth().focusRequester(mainFocusRequester),
                        textStyle = VyntaTypography.displaySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 44.sp
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        decorationBox = { innerTextField ->
                            if (textInput.isEmpty()) {
                                Text("Define your mission...", 
                                     style = VyntaTypography.displaySmall, 
                                     color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                     fontWeight = FontWeight.Bold)
                            }
                            innerTextField()
                        }
                    )
                }

                // MISSION MANIFEST - ORGANIC SUBTASKS
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ACTION MANIFEST", style = VyntaTypography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        
                        IconButton(onClick = {
                            if (newSubtaskText.isNotBlank()) {
                                subtasks = subtasks + SubtaskItem(UUID.randomUUID().toString(), newSubtaskText, true)
                                newSubtaskText = ""
                            }
                        }) {
                            Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    
                    if (subtasks.isNotEmpty() || textInput.length > 5) {
                        // Manual subtask entry
                        BasicTextField(
                            value = newSubtaskText,
                            onValueChange = { newSubtaskText = it },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            textStyle = VyntaTypography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                if (newSubtaskText.isNotBlank()) {
                                    subtasks = subtasks + SubtaskItem(UUID.randomUUID().toString(), newSubtaskText, true)
                                    newSubtaskText = ""
                                }
                            }),
                            decorationBox = { innerTextField ->
                                if (newSubtaskText.isEmpty()) {
                                    Text("Add manual step...", style = VyntaTypography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                                }
                                innerTextField()
                            }
                        )
                        
                        subtasks.forEachIndexed { index, subtask ->
                            var visible by remember { mutableStateOf(false) }
                            LaunchedEffect(subtask.id) {
                                delay(index * 100L)
                                visible = true
                            }
                            
                            AnimatedVisibility(
                                visible = visible,
                                enter = fadeIn() + slideInHorizontally { -20 }
                            ) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    // Organic Connectors
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(32.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .background(if (subtask.isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                                .border(2.dp, if (subtask.isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent, CircleShape)
                                        )
                                        if (index < subtasks.size - 1) {
                                            Box(
                                                modifier = Modifier
                                                    .width(1.dp)
                                                    .height(40.dp)
                                                    .background(
                                                        Brush.verticalGradient(
                                                            listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), Color.Transparent)
                                                        )
                                                    )
                                            )
                                        }
                                    }
                                    
                                    Spacer(Modifier.width(16.dp))
                                    
                                    Text(
                                        subtask.text,
                                        style = VyntaTypography.bodyLarge,
                                        color = if (subtask.isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.weight(1f)
                                            .clip(VyntaShapes.small)
                                            .clickable {
                                                subtasks = subtasks.map { if (it.id == subtask.id) it.copy(isSelected = !it.isSelected) else it }
                                            }
                                            .padding(vertical = 4.dp, horizontal = 8.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }

                // OPTIONAL REFINEMENTS
                AnimatedVisibility(visible = showRefineOptions || textInput.length > 5) {
                    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                        
                        Column {
                            Text("DURATION", style = VyntaTypography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf(15, 30, 60, 120, 180).forEach { mins ->
                                    val isSelected = durationMinutes == mins
                                    Surface(
                                        onClick = { durationMinutes = mins },
                                        modifier = Modifier.weight(1f).height(40.dp),
                                        shape = ShapePill,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
                                        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f))
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(if (mins >= 60) "${mins/60}H" else "${mins}M", style = VyntaTypography.labelSmall, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }

                        Column {
                            Text("ENERGY WINDOW", style = VyntaTypography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                listOf("Low", "Medium", "High").forEach { energy ->
                                    val isSelected = selectedEnergy == energy
                                    Surface(
                                        onClick = { selectedEnergy = energy },
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        shape = ShapePill,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
                                        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f))
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(energy.uppercase(), style = VyntaTypography.labelSmall, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(120.dp))
            }
        }

        // DATE PICKER DIALOG
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = selectedDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
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
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        // TIME PICKER DIALOG
        if (showTimePicker) {
            val timePickerState = rememberTimePickerState(
                initialHour = selectedTime?.hour ?: LocalTime.now().hour,
                initialMinute = selectedTime?.minute ?: LocalTime.now().minute
            )
            TimePickerDialog(
                onDismissRequest = { showTimePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        showTimePicker = false
                    }) { Text("Confirm") }
                }
            ) {
                TimePicker(state = timePickerState)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        text = { content() }
    )
}

@Composable
fun RefineAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = VyntaShapes.medium,
        color = Color.Transparent
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(label, style = VyntaTypography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
