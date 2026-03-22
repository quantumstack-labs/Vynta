package com.first_project.chronoai.ui1.navigation

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.calendar.CalendarScopes

@Composable
fun AppNavGraph(
    themeViewModel: ThemeViewModel,
    initialShortcut: String? = null,
    onShortcutConsumed: () -> Unit = {}
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    
    val prefs by themeViewModel.prefs.collectAsStateWithLifecycle()
    val themeMode = prefs.themeMode

    val account = GoogleSignIn.getLastSignedInAccount(context)
    val startDestination = if (account != null) "home" else "login"

    val database = remember { DatabaseProvider.getDatabase(context) }
    val taskDao = remember { database.taskDao() }
    
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

    val homeViewModel = remember(calendarRepository, taskDao) {
        HomeViewModel(
            repository = calendarRepository,
            taskDao = taskDao
        )
    }

    // Securely fetching API Key from BuildConfig
    val groqManager = remember { GroqManager(BuildConfig.GROQ_API_KEY) }
    
    val userPreferencesRepo = remember { UserPreferencesRepo(context) }
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

    VyntaTheme(themeMode = themeMode) {
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController, 
                startDestination = startDestination,
                modifier = Modifier.fillMaxSize(),
                enterTransition = {
                    val targetRoute = targetState.destination.route
                    val initialRoute = initialState.destination.route
                    
                    val slideDirection = if (initialRoute == "history" && targetRoute == "home") {
                        AnimatedContentTransitionScope.SlideDirection.End
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
                    
                    val slideDirection = if (initialRoute == "history" && targetRoute == "home") {
                        AnimatedContentTransitionScope.SlideDirection.End
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
                    LoginScreen(onLoginSuccess = {
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    })
                }

                composable("home") {
                    HomeScreen(
                        viewModel = homeViewModel,
                        onNavigateToInput = { _ -> navController.navigate("input?triggerMic=true") },
                        onNavigateToHistory = { 
                            navController.navigate("history") {
                                launchSingleTop = true
                            }
                        },
                        onNavigateToSettings = { navController.navigate("settings") }
                    )
                }

                composable(
                    route = "input?triggerMic={triggerMic}",
                    arguments = listOf(navArgument("triggerMic") { defaultValue = false; type = NavType.BoolType })
                ) { backStackEntry ->
                    val triggerMic = backStackEntry.arguments?.getBoolean("triggerMic") ?: false
                    InputScreen(
                        viewModel = inputViewModel,
                        onBack = { navController.popBackStack() },
                        triggerMic = triggerMic
                    )
                }

                composable("history") {
                    HistoryScreen(
                        onBack = { navController.popBackStack() },
                        onNavigateToHome = { 
                            navController.navigate("home") {
                                popUpTo("home") { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onNavigateToSettings = { navController.navigate("settings") }
                    )
                }

                composable("settings") {
                    SettingsScreen(
                        onBack = { navController.popBackStack() },
                        onSignOut = {
                            navController.navigate("login") {
                                popUpTo("home") { inclusive = true }
                            }
                        },
                        onNavigateToAbout = { navController.navigate("about") },
                        themeViewModel = themeViewModel
                    )
                }

                composable("about") {
                    AboutScreen(onBack = { navController.popBackStack() })
                }
            }

            DockWrapper(navController = navController)
        }
    }
}

@Composable
fun BoxScope.DockWrapper(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    val showDock = remember(currentRoute) {
        currentRoute == "home" || currentRoute == "history"
    }

    AnimatedVisibility(
        visible = showDock,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp).zIndex(10f)
    ) {
        PersistentVyntaDock(navController = navController)
    }
}

@Composable
fun PersistentVyntaDock(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val view = LocalView.current
    
    val isHome = currentDestination?.hierarchy?.any { it.route == "home" } == true
    val isHistory = currentDestination?.hierarchy?.any { it.route == "history" } == true
    
    val springSpec = spring<Dp>(dampingRatio = 0.8f, stiffness = 400f)
    
    val dockWidth by animateDpAsState(
        targetValue = if (isHome) 200.dp else 220.dp,
        animationSpec = springSpec,
        label = "DockWidth"
    )

    Surface(
        modifier = Modifier
            .height(64.dp)
            .width(dockWidth)
            .shadow(
                elevation = 12.dp,
                shape = CircleShape
            ),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val targetOffset by animateDpAsState(
                targetValue = if (isHome) (-45).dp else 45.dp,
                animationSpec = springSpec,
                label = "PillOffset"
            )

            Box(
                modifier = Modifier
                    .offset(x = targetOffset)
                    .width(if (isHome) 90.dp else 110.dp)
                    .height(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )

            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PersistentDockItem(
                    icon = Icons.Default.Home,
                    label = "Home",
                    isSelected = isHome,
                    onClick = { 
                        if (!isHome) {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
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
                    label = "History",
                    isSelected = isHistory,
                    onClick = { 
                        if (!isHistory) {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            navController.navigate("history") {
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
    label: String,
    isSelected: Boolean, 
    onClick: () -> Unit, 
    modifier: Modifier
) {
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(300),
        label = "IconColor"
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "IconScale"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Icon(
                icon, 
                null, 
                tint = iconColor, 
                modifier = Modifier.size(24.dp).scale(scale)
            )
            
            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = iconColor,
                    maxLines = 1
                )
            }
        }
    }
}
