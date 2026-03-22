package com.first_project.chronoai

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.first_project.chronoai.data.local.prefs.UserPreferencesRepo
import com.first_project.chronoai.ui1.navigation.AppNavGraph
import com.first_project.chronoai.ui1.viewmodel.ThemeViewModel
import com.first_project.chronoai.ui1.viewmodel.ThemeViewModelFactory

class MainActivity : ComponentActivity() {
    
    private var shortcutTrigger = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.attributes.preferredDisplayModeId = display?.supportedModes
                ?.filter { it.refreshRate >= 119f }
                ?.maxByOrNull { it.refreshRate }
                ?.modeId ?: 0
        }

        enableEdgeToEdge()
        
        shortcutTrigger.value = intent?.getStringExtra("shortcut")

        setContent {
            val context = LocalContext.current
            val userPreferencesRepo = remember { UserPreferencesRepo(context) }
            val themeViewModel: ThemeViewModel = viewModel(
                factory = ThemeViewModelFactory(userPreferencesRepo)
            )

            val prefs by themeViewModel.prefs.collectAsStateWithLifecycle()
            
            AppNavGraph(
                themeViewModel = themeViewModel,
                initialShortcut = shortcutTrigger.value,
                onShortcutConsumed = { shortcutTrigger.value = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        shortcutTrigger.value = intent.getStringExtra("shortcut")
    }
}
