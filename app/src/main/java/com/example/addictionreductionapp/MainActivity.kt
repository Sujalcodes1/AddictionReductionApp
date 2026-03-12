package com.example.addictionreductionapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.addictionreductionapp.data.AppDataStore
import com.example.addictionreductionapp.screens.AICoachScreen
import com.example.addictionreductionapp.screens.AnalyticsScreen
import com.example.addictionreductionapp.screens.AppBlockerScreen
import com.example.addictionreductionapp.screens.FocusTimerScreen
import com.example.addictionreductionapp.screens.TimerScreen
import com.example.addictionreductionapp.screens.HomeScreen
import com.example.addictionreductionapp.screens.OnboardingScreen
import com.example.addictionreductionapp.screens.ProfileScreen
import com.example.addictionreductionapp.ui.theme.DarkBackground
import com.example.addictionreductionapp.ui.theme.ErrorRed
import com.example.addictionreductionapp.ui.theme.RegainOrange
import com.example.addictionreductionapp.ui.theme.RegainTeal
import com.example.addictionreductionapp.ui.theme.RegainTheme
import com.example.addictionreductionapp.ui.theme.TextGray
import com.example.addictionreductionapp.ui.theme.TextWhite
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private var isBlockTriggered = mutableStateOf(false)
    private var blockedAppName = mutableStateOf("")
    private var blockReason = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scheduleNotifications()
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

    private fun scheduleNotifications() {
        NotificationHelper.createChannels(this)
        val workManager = WorkManager.getInstance(this)

        val now = Calendar.getInstance()
        
        // Daily Report: 24h, 9 PM
        val dailyTarget = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 21)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        if (now.after(dailyTarget)) {
            dailyTarget.add(Calendar.DAY_OF_YEAR, 1)
        }
        val dailyDelay = dailyTarget.timeInMillis - now.timeInMillis
        val dailyRequest = PeriodicWorkRequestBuilder<ReportWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(dailyDelay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("report_type" to "daily"))
            .build()
        workManager.enqueueUniquePeriodicWork("daily_report", ExistingPeriodicWorkPolicy.KEEP, dailyRequest)

        // Weekly Report: 7 days, 9 AM
        val weeklyTarget = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        if (now.after(weeklyTarget)) {
            weeklyTarget.add(Calendar.DAY_OF_YEAR, 1)
        }
        val weeklyDelay = weeklyTarget.timeInMillis - now.timeInMillis
        val weeklyRequest = PeriodicWorkRequestBuilder<ReportWorker>(7, TimeUnit.DAYS)
            .setInitialDelay(weeklyDelay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("report_type" to "weekly"))
            .build()
        workManager.enqueueUniquePeriodicWork("weekly_report", ExistingPeriodicWorkPolicy.KEEP, weeklyRequest)

        // Monthly Report: 30 days
        val monthlyRequest = PeriodicWorkRequestBuilder<ReportWorker>(30, TimeUnit.DAYS)
            .setInputData(workDataOf("report_type" to "monthly"))
            .build()
        workManager.enqueueUniquePeriodicWork("monthly_report", ExistingPeriodicWorkPolicy.KEEP, monthlyRequest)

        // Hourly Nudge: 1 hour
        val nudgeRequest = PeriodicWorkRequestBuilder<NudgeWorker>(1, TimeUnit.HOURS)
            .build()
        workManager.enqueueUniquePeriodicWork("hourly_nudge", ExistingPeriodicWorkPolicy.KEEP, nudgeRequest)
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
    val currentRoute = remember(navBackStackEntry) { navBackStackEntry?.destination?.route }

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

    val screensWithNav = remember { setOf("home", "timer", "analytics", "coach", "settings") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentRoute in screensWithNav) {
                Column {
                    BottomNavigationBar(navController)
                    Spacer(
                        Modifier
                            .windowInsetsBottomHeight(WindowInsets.navigationBars)
                            .fillMaxWidth()
                            .background(Color(0xFF0F171E))
                    )
                }
            }
        },
        containerColor = DarkBackground
    ) { padding ->
        Box(Modifier.padding(padding)) {
            NavHost(
                navController = navController,
                startDestination = if (showOnboarding) "onboarding" else "home",
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None }
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
                    TimerScreen()
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

                composable("settings") {
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

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = remember(navBackStackEntry) {
        navBackStackEntry?.destination?.route
    }
    val items = remember {
        listOf(
            Triple("home",      "Home",     Icons.Default.Home),
            Triple("timer",     "Timer",    Icons.Default.Timer),
            Triple("analytics", "Stats",    Icons.Default.BarChart),
            Triple("coach",     "Coach",    Icons.Default.Psychology),
            Triple("settings",  "Settings", Icons.Default.Settings)
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F171E))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { (route, label, icon) ->
                key(route) {
                    val isSelected = currentRoute == route
                    val chipWidth by animateDpAsState(
                        targetValue = if (isSelected) 100.dp else 44.dp,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = route
                    )
                    Box(
                        modifier = Modifier
                            .width(chipWidth)
                            .height(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) Color(0xFF00BFA5).copy(alpha = 0.15f)
                                else Color.Transparent
                            )
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                if (currentRoute != route) {
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isSelected) Color(0xFF00BFA5) else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                            AnimatedVisibility(
                                visible = isSelected,
                                enter = fadeIn(tween(200)) + expandHorizontally(tween(250)),
                                exit = fadeOut(tween(150)) + shrinkHorizontally(tween(200))
                            ) {
                                Row {
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        text = label,
                                        color = Color(0xFF00BFA5),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
