package com.first_project.chronoai.ui1.navigation

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.first_project.chronoai.ui1.navigation.dailybrief.DailyBriefingScreen
import com.first_project.chronoai.BuildConfig
import com.first_project.chronoai.ai.GroqManager
import com.first_project.chronoai.data.CalendarRepository
import com.first_project.chronoai.data.local.db.DatabaseProvider
import com.first_project.chronoai.data.local.prefs.UserPreferencesRepo
import com.first_project.chronoai.domain.ScheduleTaskUseCase
import com.first_project.chronoai.ui.theme.*
import com.first_project.chronoai.ui1.viewmodel.*
import com.first_project.chronoai.ui1.util.HapticManager
import com.first_project.chronoai.ui1.util.HapticHelper
import com.google.api.services.calendar.CalendarScopes
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavGraph(
    themeViewModel: ThemeViewModel,
    initialShortcut: String? = null,
    onShortcutConsumed: () -> Unit = {}
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val prefs by themeViewModel.prefs.collectAsStateWithLifecycle()
    val themeMode = prefs.themeMode

    if (!prefs.isLoaded) {
        Box(Modifier.fillMaxSize().background(Color.Black))
        return
    }

    val account = GoogleSignIn.getLastSignedInAccount(context)
    val startDestination = remember(account, prefs.hasAcceptedTerms) {
        if (account != null && prefs.hasAcceptedTerms) "home" else "login"
    }

    val database = remember { DatabaseProvider.getDatabase(context) }
    val taskDao = remember { database.taskDao() }
    val userPreferencesRepo = remember { UserPreferencesRepo(context) }

    val credential = remember(account) {
        account?.let {
            GoogleAccountCredential.usingOAuth2(context, listOf(
                CalendarScopes.CALENDAR,
                CalendarScopes.CALENDAR_EVENTS, 
                CalendarScopes.CALENDAR_READONLY
            ))
                .setSelectedAccount(it.account)
        }
    }

    val calendarRepository = remember(credential) {
        CalendarRepository(
            com.google.api.services.calendar.Calendar.Builder(
                com.google.api.client.http.javanet.NetHttpTransport(),
                com.google.api.client.json.gson.GsonFactory.getDefaultInstance(),
                credential
            ).setApplicationName("Vynta").build(),
            context = context.applicationContext
        )
    }

    val groqManager = remember { GroqManager(BuildConfig.GROQ_API_KEY) }

    val homeViewModel = remember(calendarRepository, taskDao, groqManager, userPreferencesRepo) {
        com.first_project.chronoai.ui1.viewmodel.HomeViewModel(
            repository = calendarRepository,
            taskDao = taskDao,
            aiManager = groqManager,
            userPreferencesRepo = userPreferencesRepo
        )
    }
    
    val scheduleTaskUseCase = remember(calendarRepository, groqManager, userPreferencesRepo) {
        ScheduleTaskUseCase(calendarRepository, groqManager, userPreferencesRepo)
    }

    val inputViewModel = remember(groqManager, homeViewModel, scheduleTaskUseCase, userPreferencesRepo) {
        InputViewModel(
            aiManager = groqManager,
            homeViewModel = homeViewModel,
            scheduleTaskUseCase = scheduleTaskUseCase,
            userPreferencesRepo = userPreferencesRepo
        )
    }

    LaunchedEffect(initialShortcut) {
        if (initialShortcut != null) {
            delay(500)
            when (initialShortcut) {
                "plan_day" -> navController.navigate("input?triggerMic=true")
                "history" -> navController.navigate("history")
                "daily_brief" -> navController.navigate("daily_brief")
            }
            onShortcutConsumed()
        }
    }

    LaunchedEffect(prefs.hapticsEnabled) {
        HapticManager.hapticsEnabled = prefs.hapticsEnabled
        HapticHelper.hapticsEnabled = prefs.hapticsEnabled
    }

    VyntaTheme(themeMode = themeMode, dynamicColor = prefs.dynamicColorsEnabled) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            SharedTransitionLayout {
                Box(modifier = Modifier.fillMaxSize()) {
                    NavHost(
                        navController = navController, 
                        startDestination = startDestination,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable("login") {
                            LoginScreen(
                                onLoginSuccess = {
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                },
                                onNavigateToTerms = { navController.navigate("terms") },
                                isTermsAccepted = prefs.hasAcceptedTerms
                            )
                        }

                        composable("terms") {
                            TermsScreen(
                                onBack = { navController.popBackStack() },
                                onAcceptChanged = { accepted ->
                                    scope.launch {
                                        userPreferencesRepo.updateTermsAcceptance(accepted)
                                    }
                                },
                                initiallyAccepted = prefs.hasAcceptedTerms
                            )
                        }

                        composable("discovery") {
                            DiscoveryScreen(
                                themeViewModel = themeViewModel,
                                onComplete = {
                                    navController.navigate("home") {
                                        popUpTo("discovery") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(
                            route = "home",
                            enterTransition = { fadeIn(tween(500, easing = LinearOutSlowInEasing)) },
                            exitTransition = { fadeOut(tween(400, easing = FastOutLinearInEasing)) }
                        ) {
                            HomeScreen(
                                viewModel = homeViewModel,
                                onNavigateToInput = { taskId -> 
                                    if (taskId != null) {
                                        navController.navigate("input?taskId=$taskId&triggerMic=false")
                                    } else {
                                        navController.navigate("input?triggerMic=true")
                                    }
                                },
                                onNavigateToFocus = { mission ->
                                    navController.navigate("focus?mission=$mission")
                                },
                                onNavigateToBriefing = {
                                    navController.navigate("daily_brief")
                                }
                            )
                        }

                        composable(
                            route = "daily_brief",
                            enterTransition = { slideInVertically { it } + fadeIn() },
                            exitTransition = { slideOutVertically { it } + fadeOut() }
                        ) {
                            DailyBriefingScreen(
                                viewModel = homeViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = "input?triggerMic={triggerMic}&taskId={taskId}",
                            arguments = listOf(
                                navArgument("triggerMic") { defaultValue = false; type = NavType.BoolType },
                                navArgument("taskId") { defaultValue = -1; type = NavType.IntType }
                            ),
                            enterTransition = { fadeIn(tween(600, easing = LinearOutSlowInEasing)) },
                            exitTransition = { fadeOut(tween(400, easing = FastOutLinearInEasing)) }
                        ) { backStackEntry ->
                            val triggerMic = backStackEntry.arguments?.getBoolean("triggerMic") ?: false
                            val taskId = backStackEntry.arguments?.getInt("taskId") ?: -1
                            
                            InputScreen(
                                viewModel = inputViewModel,
                                onBack = { navController.popBackStack() },
                                triggerMic = triggerMic,
                                taskId = if (taskId != -1) taskId else null,
                                sharedTransitionScope = this@SharedTransitionLayout,
                                animatedVisibilityScope = this@composable
                            )
                        }

                        composable(
                            route = "history",
                            enterTransition = { fadeIn(tween(500, easing = LinearOutSlowInEasing)) },
                            exitTransition = { fadeOut(tween(400, easing = FastOutLinearInEasing)) }
                        ) {
                            HistoryScreen(viewModel = homeViewModel)
                        }

                        composable(
                            route = "settings",
                            enterTransition = { fadeIn(tween(500, easing = LinearOutSlowInEasing)) },
                            exitTransition = { fadeOut(tween(400, easing = FastOutLinearInEasing)) }
                        ) {
                            SettingsScreen(
                                onNavigateToAbout = { navController.navigate("about") },
                                onNavigateToManual = { navController.navigate("manual") },
                                onSignOut = {
                                    com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, 
                                        com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()
                                        .addOnCompleteListener {
                                            navController.navigate("login") {
                                                popUpTo("home") { inclusive = true }
                                            }
                                        }
                                },
                                themeViewModel = themeViewModel
                            )
                        }

                        composable("about") {
                            AboutScreen(onBack = { navController.popBackStack() })
                        }

                        composable("manual") {
                            ManualScreen(onBack = { navController.popBackStack() })
                        }

                        composable(
                            route = "focus?mission={mission}",
                            arguments = listOf(navArgument("mission") { defaultValue = "Deep Work" })
                        ) { backStackEntry ->
                            val mission = backStackEntry.arguments?.getString("mission") ?: "Deep Work"
                            FocusScreen(
                                missionTitle = mission,
                                onEndFocus = { navController.popBackStack() }
                            )
                        }
                    }

                    DockWrapper(navController = navController, themeViewModel = themeViewModel)
                }
            }
        }
    }
}

@Composable
fun BoxScope.DockWrapper(
    navController: NavHostController,
    themeViewModel: ThemeViewModel
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    val hideDock = currentRoute == null || 
                   currentRoute.startsWith("login") || 
                   currentRoute.startsWith("terms") || 
                   currentRoute.startsWith("discovery") ||
                   currentRoute.startsWith("about") ||
                   currentRoute.startsWith("manual") ||
                   currentRoute.startsWith("focus") ||
                   currentRoute.startsWith("input") ||
                   currentRoute.startsWith("daily_brief")
    
    AnimatedVisibility(
        visible = !hideDock,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .zIndex(100f)
    ) {
        PersistentVyntaDock(navController = navController, themeViewModel = themeViewModel)
    }
}

@Composable
fun PersistentVyntaDock(
    navController: NavHostController,
    themeViewModel: ThemeViewModel
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    val currentRoute = currentDestination?.route ?: "home"
    val selectedIndex = when {
        currentRoute.startsWith("home") -> 0
        currentRoute.startsWith("input") -> 1
        currentRoute.startsWith("history") -> 2
        currentRoute.startsWith("settings") -> 3
        else -> 0
    }
    
    val navWidth = 360.dp 
    val dockHeight = 72.dp
    
    Box(
        modifier = Modifier
            .width(navWidth)
            .height(dockHeight + 24.dp)
            .padding(bottom = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(dockHeight),
            shape = ShapePill,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            shadowElevation = 12.dp
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val totalWidth = maxWidth
                
                val items = listOf(
                    Triple(Icons.Default.Home, "HOME", "home"),
                    Triple(Icons.Default.CalendarMonth, "PLAN", "input?triggerMic=false"),
                    Triple(Icons.Default.History, "HISTORY", "history"),
                    Triple(Icons.Default.Settings, "SETTINGS", "settings")
                )
                
                val itemWidths = remember(totalWidth) { List(items.size) { totalWidth / items.size } }
                
                // Simplified indicator logic: center it on the selected item
                val indicatorWidth = itemWidths[selectedIndex] * 0.9f
                val indicatorOffset by animateDpAsState(
                    targetValue = (itemWidths[selectedIndex] * selectedIndex) + (itemWidths[selectedIndex] * 0.05f),
                    animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioNoBouncy),
                    label = "indicatorOffset"
                )

                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset)
                        .width(indicatorWidth)
                        .fillMaxHeight()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
                    )
                }

                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEachIndexed { index, item ->
                        DockItem(
                            modifier = Modifier.weight(1f),
                            icon = item.first,
                            label = item.second,
                            isSelected = selectedIndex == index,
                            themeViewModel = themeViewModel,
                            onClick = { 
                                if (selectedIndex != index) {
                                    if (index == 0) {
                                        navController.navigate("home") { popUpTo("home") { inclusive = true } }
                                    } else {
                                        navController.navigate(item.third)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DockItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    themeViewModel: ThemeViewModel,
    onClick: () -> Unit
) {
    val view = LocalView.current
    val prefs by themeViewModel.prefs.collectAsStateWithLifecycle()
    
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "contentColor"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    if (prefs.hapticsEnabled) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Icon(
                icon, 
                null, 
                tint = contentColor, 
                modifier = Modifier.size(20.dp)
            )
            AnimatedVisibility(
                visible = isSelected,
                enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(),
                exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        label, 
                        style = VyntaTypography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.2.sp),
                        color = contentColor, 
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}
