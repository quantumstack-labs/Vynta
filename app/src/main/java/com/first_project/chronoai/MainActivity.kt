package com.first_project.chronoai

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.first_project.chronoai.data.local.prefs.UserPreferencesRepo
import com.first_project.chronoai.ui1.navigation.AppNavGraph
import com.first_project.chronoai.ui1.viewmodel.ThemeViewModel
import com.first_project.chronoai.ui1.viewmodel.ThemeViewModelFactory
import com.first_project.chronoai.receiver.SunsetReceiver
import com.first_project.chronoai.worker.FocusShieldWorker
import com.first_project.chronoai.util.BiometricHelper
import java.util.Calendar

class MainActivity : FragmentActivity() {
    
    private var deepLinkTrigger = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }

        window.attributes.preferredDisplayModeId = display?.supportedModes
            ?.filter { it.refreshRate >= 119f }
            ?.maxByOrNull { it.refreshRate }
            ?.modeId ?: 0

        enableEdgeToEdge()
        
        FocusShieldWorker.enqueue(this)
        scheduleSunsetReflection()
        
        // Handle initial shortcut or widget deep link
        val shortcut = intent?.getStringExtra("shortcut")
        val data = intent?.data?.toString()
        deepLinkTrigger.value = when {
            shortcut != null -> shortcut
            data == "vynta://add_task" -> "plan_day"
            else -> null
        }

        setContent {
            val context = LocalContext.current
            val userPreferencesRepo = remember { UserPreferencesRepo(context) }
            val themeViewModel: ThemeViewModel = viewModel(
                factory = ThemeViewModelFactory(userPreferencesRepo)
            )

            val prefs by themeViewModel.prefs.collectAsStateWithLifecycle()
            var isAuthenticated by remember { mutableStateOf(false) }
            var isAuthTriggered by remember { mutableStateOf(false) }

            // Re-trigger biometric prompt when returning to the app
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        if (prefs.isLoaded && prefs.biometricLockEnabled && !isAuthenticated) {
                            isAuthTriggered = false
                        }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            // Sync authentication state with settings
            LaunchedEffect(prefs.isLoaded, prefs.biometricLockEnabled) {
                if (prefs.isLoaded && !prefs.biometricLockEnabled) {
                    isAuthenticated = true
                }
            }

            LaunchedEffect(prefs.isLoaded, prefs.biometricLockEnabled, isAuthTriggered) {
                if (prefs.isLoaded && prefs.biometricLockEnabled && !isAuthenticated && !isAuthTriggered) {
                    isAuthTriggered = true
                    val biometricHelper = BiometricHelper(this@MainActivity)
                    if (biometricHelper.canAuthenticate()) {
                        biometricHelper.showBiometricPrompt(
                        onSuccess = { isAuthenticated = true }
                    ) { 
                        // Stay on locked screen if user cancels
                    }
                    } else {
                        isAuthenticated = true
                    }
                }
            }

            if ((isAuthenticated || !prefs.biometricLockEnabled) && prefs.isLoaded) {
                AppNavGraph(
                    themeViewModel = themeViewModel,
                    initialShortcut = deepLinkTrigger.value,
                    onShortcutConsumed = { deepLinkTrigger.value = null }
                )
            } else if (prefs.isLoaded) {
                // Locked State UI
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Vynta is Locked",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { isAuthTriggered = false },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(0.6f).height(56.dp)
                        ) {
                            Text("Unlock Vynta", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black)
                )
            }
        }
    }

    private fun scheduleSunsetReflection() {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, SunsetReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 22) 
            set(Calendar.MINUTE, 10)
            set(Calendar.SECOND, 0)
            if (timeInMillis < System.currentTimeMillis()) {
                add(Calendar.MINUTE, 5)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val shortcut = intent.getStringExtra("shortcut")
        val data = intent.data?.toString()
        deepLinkTrigger.value = when {
            shortcut != null -> shortcut
            data == "vynta://add_task" -> "plan_day"
            else -> null
        }
    }
}
