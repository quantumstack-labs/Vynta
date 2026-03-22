package com.first_project.chronoai.ui1.navigation

import android.Manifest
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.first_project.chronoai.voice.SpeechRecognitionManager
import com.first_project.chronoai.ui1.utils.HapticManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale

// Data class with a stable ID for better performance in lists
@Immutable
data class SubtaskItem(val id: String, val text: String, val isSelected: Boolean = true)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun InputScreen(
    viewModel: InputViewModel,
    onBack: () -> Unit,
    triggerMic: Boolean = true
) {
    val context = LocalContext.current
    val view = LocalView.current
    val focusManager = LocalFocusManager.current
    val speechManager = remember { SpeechRecognitionManager(context) }
    val hapticManager = remember { HapticManager(context) }
    
    val uiState by viewModel.uiState.collectAsState()
    val spokenText by speechManager.spokenText.collectAsState()
    val isListening by speechManager.isListening.collectAsState()
    val detectedContext by viewModel.detectedContext.collectAsState()
    
    var textInput by remember { mutableStateOf("") }
    val mainFocusRequester = remember { FocusRequester() }

    var subtasks by remember { mutableStateOf(listOf<SubtaskItem>()) }
    var newSubtaskText by remember { mutableStateOf("") }
    var selectedEnergy by remember { mutableStateOf("Medium") }

    // Fix: Clear previous subtasks and update with new ones when AI context changes
    LaunchedEffect(detectedContext.subtasks) {
        if (detectedContext.subtasks.isNotEmpty()) {
            subtasks = detectedContext.subtasks.map { 
                SubtaskItem(java.util.UUID.randomUUID().toString(), it) 
            }
        }
    }

    // AI Pulse Animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    // AI Crunching Haptic Loop
    LaunchedEffect(uiState) {
        if (uiState is InputUiState.Analyzing) {
            while (isActive) {
                hapticManager.play(HapticManager.VyntaEffect.AI_CRUNCHING)
                delay(200)
            }
        }
    }

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                isTtsReady = true
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) { }
                    override fun onDone(utteranceId: String?) {
                        if (utteranceId == "clarification") {
                            speechManager.startListening()
                        }
                    }
                    override fun onError(utteranceId: String?) { }
                })
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { 
            tts?.shutdown()
            viewModel.resetState()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted && triggerMic) {
            hapticManager.play(HapticManager.VyntaEffect.MIC_TRIGGER)
            speechManager.startListening()
        }
    }

    LaunchedEffect(Unit) {
        mainFocusRequester.requestFocus()
        if (triggerMic) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(spokenText) {
        if (spokenText.isNotEmpty() && spokenText != "Listening..." && spokenText != "Processing..." && !spokenText.startsWith("Error:")) {
            textInput = spokenText
            viewModel.onTextChanged(spokenText)
        }
    }

    LaunchedEffect(uiState, isTtsReady) {
        if (isTtsReady) {
            when (val state = uiState) {
                is InputUiState.Success -> {
                    hapticManager.play(HapticManager.VyntaEffect.SUCCESS)
                    val ttsMessage = state.personaMessage ?: state.message
                    tts?.speak(ttsMessage, TextToSpeech.QUEUE_FLUSH, null, "success")
                    delay(3000)
                    onBack()
                    viewModel.resetState()
                }
                is InputUiState.Error -> {
                    hapticManager.play(HapticManager.VyntaEffect.ERROR)
                    tts?.speak(state.message, TextToSpeech.QUEUE_FLUSH, null, "error")
                }
                is InputUiState.NeedClarification -> {
                    tts?.speak(state.question, TextToSpeech.QUEUE_FLUSH, null, "clarification")
                }
                is InputUiState.Conflict -> {
                    hapticManager.play(HapticManager.VyntaEffect.ERROR)
                    val message = state.suggestion ?: "Your calendar is too busy."
                    tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "conflict")
                }
                else -> {}
            }
        }
    }

    val isConfirmEnabled by remember(textInput, uiState) {
        derivedStateOf { uiState !is InputUiState.Analyzing && textInput.isNotBlank() }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Plan with AI", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(16.dp))

                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                    if (isListening || uiState is InputUiState.Analyzing) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .scale(pulseScale)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha), CircleShape)
                        )
                    }
                    
                    Surface(
                        shape = CircleShape,
                        color = if (isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp),
                        modifier = Modifier.size(80.dp).clickable {
                             permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        shadowElevation = 2.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (isListening) Icons.Default.Mic else Icons.Default.MicNone,
                                null,
                                tint = if (isListening) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                OutlinedTextField(
                    value = textInput,
                    onValueChange = {
                        textInput = it
                        viewModel.onTextChanged(it)
                    },
                    modifier = Modifier.fillMaxWidth().focusRequester(mainFocusRequester),
                    placeholder = { Text("What's on your mind?") },
                    shape = RoundedCornerShape(20.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                AnimatedVisibility(
                    visible = uiState is InputUiState.Conflict || uiState is InputUiState.NeedClarification,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    val message = when (val state = uiState) {
                        is InputUiState.Conflict -> state.suggestion
                        is InputUiState.NeedClarification -> state.question
                        else -> null
                    }
                    if (message != null) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = message,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                Text("BREAKDOWN", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.2.sp)
                Spacer(Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    subtasks.forEach { item ->
                        key(item.id) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (item.isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) 
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.fillMaxWidth().animateContentSize().clickable {
                                    subtasks = subtasks.map { if (it.id == item.id) it.copy(isSelected = !it.isSelected) else it }
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Checkbox(
                                        checked = item.isSelected,
                                        onCheckedChange = { isChecked ->
                                            subtasks = subtasks.map { if (it.id == item.id) it.copy(isSelected = isChecked) else it }
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        item.text, 
                                        style = MaterialTheme.typography.bodyLarge, 
                                        modifier = Modifier.weight(1f),
                                        color = if (item.isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    IconButton(onClick = { subtasks = subtasks.filter { it.id != item.id } }) {
                                        Icon(Icons.Default.DeleteOutline, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) {
                        TextField(
                            value = newSubtaskText,
                            onValueChange = { newSubtaskText = it },
                            placeholder = { Text("Add a step...", fontSize = 14.sp) },
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onAny = {
                                if (newSubtaskText.isNotBlank()) {
                                    subtasks = subtasks + SubtaskItem(java.util.UUID.randomUUID().toString(), newSubtaskText)
                                    newSubtaskText = ""
                                }
                            })
                        )
                        IconButton(
                            onClick = {
                                if (newSubtaskText.isNotBlank()) {
                                    subtasks = subtasks + SubtaskItem(java.util.UUID.randomUUID().toString(), newSubtaskText)
                                    newSubtaskText = ""
                                }
                            },
                            enabled = newSubtaskText.isNotBlank()
                        ) {
                            Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Text("ENERGY REQUIRED", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.2.sp)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Low", "Medium", "High").forEach { energy ->
                        val isSelected = selectedEnergy == energy
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedEnergy = energy },
                            label = { Text(energy) },
                            leadingIcon = if (isSelected) { { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) } } else null,
                            shape = CircleShape
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                Text("INTELLIGENT DEFAULTS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.2.sp)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    AssistChip(
                        onClick = {}, 
                        label = { Text(detectedContext.date ?: "Today") },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, null, Modifier.size(18.dp)) },
                        shape = CircleShape
                    )
                    AssistChip(
                        onClick = {}, 
                        label = { Text(detectedContext.deadlineTime ?: "Optimal Time") },
                        leadingIcon = { Icon(Icons.Default.Schedule, null, Modifier.size(18.dp)) },
                        shape = CircleShape
                    )
                }

                Spacer(Modifier.height(40.dp))

                Button(
                    onClick = {
                        if (isConfirmEnabled) {
                            val selectedSubtasks = subtasks.filter { it.isSelected }.map { it.text }
                            viewModel.processTask(context, textInput, selectedEnergy, selectedSubtasks)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = isConfirmEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    if (uiState is InputUiState.Analyzing) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 3.dp)
                    } else {
                        Text("Confirm Schedule", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    }
                }
                Spacer(Modifier.height(32.dp))
            }

            AnimatedVisibility(
                visible = uiState is InputUiState.Success,
                enter = fadeIn(tween(400)),
                exit = fadeOut(tween(400))
            ) {
                val state = uiState as? InputUiState.Success
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.CheckCircle, 
                            null, 
                            tint = Color(0xFF4CAF50), 
                            modifier = Modifier.size(120.dp).scale(1.1f)
                        )
                        Spacer(Modifier.height(32.dp))
                        Text("Task Scheduled!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = state?.personaMessage ?: "I've optimized your schedule.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
