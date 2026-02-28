package com.example.addictionreductionapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.addictionreductionapp.data.AppDataStore
import com.example.addictionreductionapp.screens.*
import com.example.addictionreductionapp.ui.theme.*

class MainActivity : ComponentActivity() {
    private var isBlockTriggered = mutableStateOf(false)
    private var blockedAppName = mutableStateOf("")
    private var blockReason = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppDataStore.loadFromPrefs(this)
        isBlockTriggered.value = intent?.getBooleanExtra("show_block_screen", false) ?: false
        blockedAppName.value = intent?.getStringExtra("blocked_app_name") ?: ""
        blockReason.value = intent?.getStringExtra("block_reason") ?: ""

        // FIX 2 — Full screen: hide status bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        setContent {
            RegainTheme {
                Surface(color = DarkBackground) {
                    AppRoot(
                        isBlockTriggered = isBlockTriggered.value,
                        blockedAppName = blockedAppName.value,
                        blockReason = blockReason.value,
                        onBlockShown = {
                            isBlockTriggered.value = false
                            blockedAppName.value = ""
                            blockReason.value = ""
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra("show_block_screen", false)) {
            isBlockTriggered.value = true
            blockedAppName.value = intent.getStringExtra("blocked_app_name") ?: ""
            blockReason.value = intent.getStringExtra("block_reason") ?: ""
        }
    }
}

@Composable
fun AppRoot(
    isBlockTriggered: Boolean,
    blockedAppName: String,
    blockReason: String,
    onBlockShown: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showOnboarding = !AppDataStore.hasCompletedOnboarding.value

    // Store block info for the composable to use
    var currentBlockedApp by remember { mutableStateOf("") }
    var currentBlockReason by remember { mutableStateOf("") }

    LaunchedEffect(isBlockTriggered) {
        if (isBlockTriggered) {
            currentBlockedApp = blockedAppName
            currentBlockReason = blockReason
            navController.navigate("block") {
                popUpTo(navController.graph.startDestinationId)
            }
            onBlockShown()
        }
    }

    val screensWithNav = setOf("home", "timer", "analytics", "coach", "profile")

    Scaffold(
        bottomBar = {
            if (currentRoute in screensWithNav) {
                RegainBottomNavBar(navController)
            }
        },
        containerColor = DarkBackground
    ) { padding ->
        Box(Modifier.padding(padding)) {
            NavHost(
                navController = navController,
                startDestination = if (showOnboarding) "onboarding" else "home"
            ) {
                composable("onboarding") {
                    OnboardingScreen(
                        onComplete = {
                            navController.navigate("home") {
                                popUpTo("onboarding") { inclusive = true }
                            }
                        }
                    )
                }

                composable("home") {
                    HomeScreen(
                        onStartFocus = { navController.navigate("timer") },
                        onNavigateToApps = { navController.navigate("app_blocker") }
                    )
                }

                composable("timer") {
                    FocusTimerScreen()
                }

                composable("analytics") {
                    AnalyticsScreen()
                }

                composable("coach") {
                    AICoachScreen()
                }

                composable("profile") {
                    ProfileScreen(
                        onNavigateToApps = { navController.navigate("app_blocker") }
                    )
                }

                composable("app_blocker") {
                    AppBlockerScreen(
                        onBack = { navController.popBackStack() }
                    )
                }

                composable("block") {
                    BlockScreen(
                        appName = currentBlockedApp,
                        reason = currentBlockReason,
                        onExit = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

@Composable
fun BlockScreen(
    appName: String = "",
    reason: String = "",
    onExit: () -> Unit
) {
    val displayName = appName.ifEmpty { "This app" }

    val (title, message, icon) = when (reason) {
        "focus" -> Triple(
            "Focus Mode Active",
            "$displayName is blocked during Focus Mode",
            Icons.Default.Shield
        )
        "schedule" -> Triple(
            "Scheduled Block",
            "$displayName is blocked during scheduled hours",
            Icons.Default.Schedule
        )
        else -> Triple(
            "Limit Reached",
            "You've reached your $displayName limit",
            Icons.Default.Block
        )
    }

    val accentColor = when (reason) {
        "focus" -> RegainTeal
        "schedule" -> RegainOrange
        else -> ErrorRed
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                title,
                color = TextWhite,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                message,
                color = TextGray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                when (reason) {
                    "focus" -> "Stay focused! You can do this! 💪"
                    "schedule" -> "This app is restricted right now."
                    else -> "Take a break and come back tomorrow!"
                },
                color = TextGray,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onExit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (reason == "focus") RegainTeal else accentColor
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    "Return Home",
                    color = DarkBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

data class NavItem(
    val label: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun RegainBottomNavBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val items = listOf(
        NavItem("Home", "home", Icons.Filled.Home, Icons.Outlined.Home),
        NavItem("Timer", "timer", Icons.Filled.Timer, Icons.Outlined.Timer),
        NavItem("Stats", "analytics", Icons.Filled.BarChart, Icons.Outlined.BarChart),
        NavItem("Coach", "coach", Icons.Filled.Psychology, Icons.Outlined.Psychology),
        NavItem("Profile", "profile", Icons.Filled.Person, Icons.Outlined.Person)
    )

    NavigationBar(
        containerColor = DarkNavBar,
        tonalElevation = 0.dp,
        modifier = Modifier.height(72.dp)
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (isSelected) {
                            Box(
                                Modifier
                                    .size(32.dp)
                                    .background(RegainTeal.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    item.selectedIcon,
                                    contentDescription = null,
                                    tint = RegainTeal,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else {
                            Icon(
                                item.unselectedIcon,
                                contentDescription = null,
                                tint = TextGray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                label = {
                    Text(
                        item.label,
                        fontSize = 10.sp,
                        color = if (isSelected) RegainTeal else TextGray,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}
