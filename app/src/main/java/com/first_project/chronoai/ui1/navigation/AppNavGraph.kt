package com.first_project.chronoai.ui1.navigation

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
import com.first_project.chronoai.BuildConfig
import com.first_project.chronoai.ai.GroqManager
import com.first_project.chronoai.data.CalendarRepository
import com.first_project.chronoai.data.local.db.DatabaseProvider
import com.first_project.chronoai.data.local.prefs.UserPreferencesRepo
import com.first_project.chronoai.domain.ScheduleTaskUseCase
import com.first_project.chronoai.ui.theme.VyntaTheme
import com.first_project.chronoai.ui1.viewmodel.HomeViewModel
import com.first_project.chronoai.ui1.viewmodel.ThemeViewModel
import com.first_project.chronoai.ui1.navigation.ChangelogScreen
import com.first_project.chronoai.ui1.navigation.TermsScreen
import com.first_project.chronoai.ui1.navigation.ManualScreen
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.calendar.CalendarScopes
import kotlinx.coroutines.launch

@OptIn(ExperimentalSharedTransitionApi::class)
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

    val account = GoogleSignIn.getLastSignedInAccount(context)
    val startDestination = if (account != null) "home" else "login"

    val database = remember { DatabaseProvider.getDatabase(context) }
    val taskDao = remember { database.taskDao() }
    
    val userPreferencesRepo = remember { UserPreferencesRepo(context) }

    val credential = remember(account) {
        account?.let {
            GoogleAccountCredential.usingOAuth2(context, listOf(CalendarScopes.CALENDAR_EVENTS, CalendarScopes.CALENDAR_READONLY))
                .setSelectedAccount(it.account)
        }
    }

    val calendarRepository = remember(credential) {
        CalendarRepository(
            com.google.api.services.calendar.Calendar.Builder(
                com.google.api.client.http.javanet.NetHttpTransport(),
                com.google.api.client.json.gson.GsonFactory(),
                credential
            ).setApplicationName("Vynta").build()
        )
    }

    // Securely fetching API Key from BuildConfig
    val groqManager = remember { GroqManager(BuildConfig.GROQ_API_KEY) }

    val homeViewModel = remember(calendarRepository, taskDao, groqManager) {
        HomeViewModel(
            repository = calendarRepository,
            taskDao = taskDao,
            aiManager = groqManager
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

    // Handle initial shortcuts and changelog
    LaunchedEffect(initialShortcut, prefs.lastSeenVersion) {
        val currentVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionCode
        
        if (currentVersion > prefs.lastSeenVersion && account != null) {
            navController.navigate("changelog")
        } else if (initialShortcut != null) {
            when (initialShortcut) {
                "plan_day" -> {
                    navController.navigate("input?triggerMic=true")
                }
                "history" -> {
                    navController.navigate("history")
                }
            }
            onShortcutConsumed()
        }
    }

    VyntaTheme(themeMode = themeMode) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            SharedTransitionLayout {
                Box(modifier = Modifier.fillMaxSize()) {
                    NavHost(
                        navController = navController, 
                        startDestination = startDestination,
                        modifier = Modifier.fillMaxSize(),
                enterTransition = {
                    val targetRoute = targetState.destination.route
                    val initialRoute = initialState.destination.route
                    val routeOrder = mapOf("home" to 0, "history" to 1, "settings" to 2)
                    
                    val initialIndex = routeOrder[initialRoute]
                    val targetIndex = routeOrder[targetRoute]
                    
                    val slideDirection = if (initialIndex != null && targetIndex != null) {
                        if (targetIndex < initialIndex) AnimatedContentTransitionScope.SlideDirection.End 
                        else AnimatedContentTransitionScope.SlideDirection.Start
                    } else {
                        AnimatedContentTransitionScope.SlideDirection.Start
                    }

                    slideIntoContainer(
                        towards = slideDirection,
                        animationSpec = tween(400, easing = EaseInOutQuart)
                    ) + fadeIn(animationSpec = tween(400))
                },
                exitTransition = {
                    val targetRoute = targetState.destination.route
                    val initialRoute = initialState.destination.route
                    val routeOrder = mapOf("home" to 0, "history" to 1, "settings" to 2)
                    
                    val initialIndex = routeOrder[initialRoute]
                    val targetIndex = routeOrder[targetRoute]
                    
                    val slideDirection = if (initialIndex != null && targetIndex != null) {
                        if (targetIndex < initialIndex) AnimatedContentTransitionScope.SlideDirection.End 
                        else AnimatedContentTransitionScope.SlideDirection.Start
                    } else {
                        AnimatedContentTransitionScope.SlideDirection.Start
                    }

                    slideOutOfContainer(
                        towards = slideDirection,
                        animationSpec = tween(400, easing = EaseInOutQuart)
                    ) + fadeOut(animationSpec = tween(400))
                },
                popEnterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(400, easing = EaseInOutQuart)
                    ) + fadeIn(animationSpec = tween(400))
                },
                popExitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(400, easing = EaseInOutQuart)
                    ) + fadeOut(animationSpec = tween(400))
                }
            ) {

                composable("login") {
                    LoginScreen(
                        onLoginSuccess = {
                            if (!prefs.hasCompletedOnboarding) {
                                navController.navigate("discovery") {
                                    popUpTo("login") { inclusive = true }
                                }
                            } else {
                                navController.navigate("home") {
                                    popUpTo("login") { inclusive = true }
                                }
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

                composable("home") {
                    HomeScreen(
                        viewModel = homeViewModel,
                        onNavigateToInput = { taskId -> 
                            if (taskId != null) {
                                navController.navigate("input?taskId=$taskId&triggerMic=false")
                            } else {
                                navController.navigate("input?triggerMic=true")
                            }
                        },
                        onNavigateToHistory = { navController.navigate("history") },
                        onNavigateToSettings = { navController.navigate("settings") },
                        onNavigateToDetail = { taskId ->
                            navController.navigate("task_detail/$taskId")
                        }
                    )
                }

                composable(
                    route = "input?triggerMic={triggerMic}&taskId={taskId}",
                    arguments = listOf(
                        navArgument("triggerMic") { defaultValue = false; type = NavType.BoolType },
                        navArgument("taskId") { defaultValue = -1; type = NavType.IntType }
                    )
                ) { backStackEntry ->
                    val triggerMic = backStackEntry.arguments?.getBoolean("triggerMic") ?: false
                    val taskId = backStackEntry.arguments?.getInt("taskId") ?: -1
                    InputScreen(
                        viewModel = inputViewModel,
                        onBack = { navController.popBackStack() },
                        triggerMic = triggerMic,
                        taskId = if (taskId != -1) taskId else null
                    )
                }

                composable(
                    route = "task_detail/{taskId}",
                    arguments = listOf(navArgument("taskId") { type = NavType.IntType })
                ) { backStackEntry ->
                    val taskId = backStackEntry.arguments?.getInt("taskId") ?: -1
                    TaskDetailScreen(
                        taskId = taskId,
                        viewModel = homeViewModel,
                        onBack = { navController.popBackStack() },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@composable
                    )
                }

                composable("history") {
                    HistoryScreen()
                }

                composable("settings") {
                    SettingsScreen(
                        onSignOut = {
                            navController.navigate("login") {
                                popUpTo("home") { inclusive = true }
                            }
                        },
                        onNavigateToAbout = { navController.navigate("about") },
                        onNavigateToManual = { navController.navigate("manual") },
                        themeViewModel = themeViewModel
                    )
                }

                composable("about") {
                    AboutScreen(onBack = { navController.popBackStack() })
                }

                composable("manual") {
                    ManualScreen(onBack = { navController.popBackStack() })
                }

                composable("changelog") {
                    ChangelogScreen(onDismiss = {
                        scope.launch {
                            val currentVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionCode
                            userPreferencesRepo.updateLastSeenVersion(currentVersion)
                            navController.popBackStack()
                        }
                    })
                }
            }

                    DockWrapper(navController = navController)
                }
            }
        }
    }
}

@Composable
fun BoxScope.DockWrapper(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    val showDock = remember(currentRoute) {
        currentRoute == "home" || currentRoute == "history" || currentRoute == "settings"
    }

    AnimatedVisibility(
        visible = showDock,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(bottom = 24.dp)
            .zIndex(100f)
    ) {
        PersistentVyntaDock(navController = navController)
    }
}

@Composable
fun PersistentVyntaDock(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    val isHome = currentDestination?.hierarchy?.any { it.route == "home" } == true
    val isHistory = currentDestination?.hierarchy?.any { it.route == "history" } == true
    val isSettings = currentDestination?.hierarchy?.any { it.route == "settings" } == true
    
    val springSpec = spring<Dp>(dampingRatio = 0.8f, stiffness = 300f)
    
    // M3 Expressive: Fixed Width, Pill Shape
    val navWidth = 200.dp 
    
    Surface(
        modifier = Modifier
            .height(64.dp)
            .width(navWidth)
            .shadow(
                elevation = 12.dp,
                shape = CircleShape,
                ambientColor = Color.Black.copy(alpha = 0.2f),
                spotColor = Color.Black.copy(alpha = 0.2f)
            ),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer // Expressive Accent Style
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val itemWidth = (navWidth - 16.dp) / 3
            val targetOffset by animateDpAsState(
                targetValue = when {
                    isHome -> -itemWidth
                    isHistory -> 0.dp
                    else -> itemWidth
                },
                animationSpec = springSpec,
                label = "PillOffset"
            )

            // The "Selection" pill - following M3 Expressive guidelines
            Box(
                modifier = Modifier
                    .offset(x = targetOffset)
                    .width(52.dp)
                    .height(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSecondaryContainer)
            )

            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PersistentDockItem(
                    icon = Icons.Default.Home,
                    isSelected = isHome,
                    onClick = { 
                        if (!isHome) {
                            navController.navigate("home") {
                                popUpTo("home") { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                PersistentDockItem(
                    icon = Icons.Default.History,
                    isSelected = isHistory,
                    onClick = { 
                        if (!isHistory) {
                            navController.navigate("history") {
                                launchSingleTop = true
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                PersistentDockItem(
                    icon = Icons.Default.Settings,
                    isSelected = isSettings,
                    onClick = { 
                        if (!isSettings) {
                            navController.navigate("settings") {
                                launchSingleTop = true
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun PersistentDockItem(
    icon: ImageVector, 
    isSelected: Boolean, 
    onClick: () -> Unit, 
    modifier: Modifier
) {
    val view = LocalView.current
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
        animationSpec = tween(300),
        label = "IconColor"
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "IconScale"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var hasBeenPressed by remember { mutableStateOf(false) }
    
    LaunchedEffect(isPressed) {
        if (isPressed) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            hasBeenPressed = true
        } else if (hasBeenPressed) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            hasBeenPressed = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                onClick = onClick,
                interactionSource = interactionSource,
                indication = null
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
        ) {
            Icon(
                icon, 
                null, 
                tint = iconColor, 
                modifier = Modifier.size(24.dp).scale(scale)
            )
            
            // Text removed for cleaner, more compact look
        }
    }
}
