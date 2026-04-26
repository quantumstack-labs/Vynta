package com.first_project.chronoai.ui1.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsScreen(
    onBack: () -> Unit,
    onAcceptChanged: (Boolean) -> Unit,
    initiallyAccepted: Boolean = false
) {
    var isAccepted by remember { mutableStateOf(initiallyAccepted) }
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terms & Conditions") },
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
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(vertical = 16.dp)
            ) {
                Text(
                    "Welcome to Vynta",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "By using Vynta, you agree to the following terms and conditions. Please read them carefully.",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Section("1. Acceptance of Terms", "By accessing or using the Vynta application, you agree to be bound by these terms. If you do not agree to all terms, you may not use the app.")
                
                Section("2. Google Calendar Integration", "Vynta requires access to your Google Calendar to sync and manage your schedule. We use the minimum permissions necessary to read and write your calendar events for the purpose of personal productivity.")
                
                Section("3. Data Privacy", "Your calendar data and tasks are processed locally on your device or via secure API calls. We do not sell or share your personal scheduling data with third parties for marketing purposes.")
                
                Section("4. AI Processing", "Vynta uses AI (via Groq/LLM) to analyze your voice and text inputs. While we strive for accuracy, AI-generated schedules should be reviewed for correctness.")
                
                Section("5. User Responsibility", "You are responsible for maintaining the security of your Google account and for all activities that occur under your account.")
                
                Section("6. Modifications", "We reserve the right to modify these terms at any time. Continued use of the app after changes constitutes acceptance of the new terms.")
                
                Spacer(Modifier.height(32.dp))
            }

            Divider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isAccepted,
                    onCheckedChange = {
                        isAccepted = it
                        onAcceptChanged(it)
                    }
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "I have read and agree to the Terms and Conditions",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun Section(title: String, content: String) {
    Spacer(Modifier.height(24.dp))
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(8.dp))
    Text(
        content,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
