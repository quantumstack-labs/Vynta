package com.first_project.chronoai.ui1.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualScreen(onBack: () -> Unit) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("How Vynta Works") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState)
        ) {
            Spacer(Modifier.height(16.dp))
            
            ManualSection("1. Getting Started", "When you first open Vynta, you will be prompted to sign in with your Google Account. This allows Vynta to sync with your Google Calendar so you can see your existing events alongside your new AI-generated tasks.")

            ManualSection("2. Navigation", "Vynta uses a Persistent Dock at the bottom of the screen:\n- Home Icon: Your daily agenda.\n- History Icon: Review your past accomplishments.\n- Settings Icon: Customize your experience.")

            ManualSection("3. Adding Tasks with AI 🧠", "This is the heart of Vynta. Instead of filling out long forms, just talk to it:\n1. Tap the Mic button or the Input Field.\n2. Speak or type naturally: \"I need to write the project report tomorrow at 10 AM, it's high priority.\"\n3. Vynta will automatically extract the Task Name, Time, Priority, and Energy Level.")

            ManualSection("4. Energy-Based Productivity", "Vynta categorizes tasks by Energy Levels:\n⚡ High Energy: Complex tasks (Coding, Writing).\n🔋 Medium Energy: Routine tasks (Emails).\n🧘 Low Energy: Admin tasks (Filing).\n\nUse the Filter chips on the Home screen to view tasks that match your current mental state.")

            ManualSection("5. Smart Features", "- Focus Shield: Automatically enables DND during high-energy tasks if enabled in Settings.\n- Smart Spacing: Adds breathing room (5-30 mins) between tasks automatically.\n- Redemption: One-tap recovery for yesterday's forgotten tasks.")

            ManualSection("6. Customization", "Go to Settings to:\n- Toggle between Light and Dark modes.\n- Choose between four Voice Personas: Atlas, Lyra, Sloane, or Orion.\n- Enable/Disable Focus Shield and Smart Spacing.")

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ManualSection(title: String, content: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(Modifier.height(8.dp))
    Text(
        content,
        style = MaterialTheme.typography.bodyMedium,
        lineHeight = 24.sp
    )
    Spacer(Modifier.height(32.dp))
}
