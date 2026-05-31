package com.first_project.chronoai.ui1.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ChangelogItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun ChangelogScreen(onDismiss: () -> Unit) {
    val updates = listOf(
        ChangelogItem(
            "Temporal Alignment",
            "Experience an immersive new login animation that visualizes your day coming together.",
            Icons.Default.Timeline,
            Color(0xFFD0BCFF)
        ),
        ChangelogItem(
            "Actionable Insights",
            "Daily briefings now provide grounded, human-centric strategic observations and recommendations.",
            Icons.Default.AutoGraph,
            Color(0xFFC8B99A)
        ),
        ChangelogItem(
            "Material 3 Widgets",
            "Widgets now follow your system's dynamic colors and Light/Dark themes perfectly.",
            Icons.Default.Palette,
            Color(0xFFB4F0AD)
        ),
        ChangelogItem(
            "Reliable Biometric Lock",
            "Fixed lock screen behavior to strictly respect your settings and ensure a smooth startup.",
            Icons.Default.Fingerprint,
            Color(0xFFE91E63)
        ),
        ChangelogItem(
            "Real-time Intelligence",
            "Briefings and insights now update instantly as you add or complete your missions.",
            Icons.Default.Bolt,
            Color(0xFFFF9800)
        )
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(64.dp))
            
            Icon(
                Icons.Default.Celebration,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Text(
                "What's New in Vynta",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp,
                modifier = Modifier.padding(top = 16.dp)
            )
            
            Text(
                "Version 3.1.0",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                items(updates) { item ->
                    Row(verticalAlignment = Alignment.Top) {
                        Surface(
                            shape = CircleShape,
                            color = item.color.copy(alpha = 0.1f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(item.icon, null, tint = item.color, modifier = Modifier.size(24.dp))
                            }
                        }
                        
                        Spacer(Modifier.width(20.dp))
                        
                        Column {
                            Text(
                                item.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                item.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 48.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Get Started", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
