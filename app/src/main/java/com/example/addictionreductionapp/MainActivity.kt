package com.example.addictionreductionapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.example.addictionreductionapp.data.DEFAULT_APPS
import com.example.addictionreductionapp.screens.BlockScreen
import com.example.addictionreductionapp.screens.AICoachScreen
import com.example.addictionreductionapp.screens.AnalyticsScreen
import com.example.addictionreductionapp.screens.AppBlockerScreen
import com.example.addictionreductionapp.screens.FocusTimerScreen
import com.example.addictionreductionapp.screens.GoalsScreen
import com.example.addictionreductionapp.screens.HomeScreen
import com.example.addictionreductionapp.screens.OnboardingScreen
import com.example.addictionreductionapp.screens.PermissionScreen
import com.example.addictionreductionapp.screens.PrivacyPolicyScreen
import com.example.addictionreductionapp.screens.ProfileScreen
import com.example.addictionreductionapp.screens.RoadmapScreen
import com.example.addictionreductionapp.screens.SmartReductionSetupScreen
import com.example.addictionreductionapp.screens.BlockScreen
import com.example.addictionreductionapp.screens.BottomNavigationBar
import com.example.addictionreductionapp.screens.LoginScreen
import com.example.addictionreductionapp.screens.RegisterScreen
import com.example.addictionreductionapp.ui.theme.DarkBackground
import com.example.addictionreductionapp.ui.theme.DarkCard
import com.example.addictionreductionapp.ui.theme.DarkCardLight
import com.example.addictionreductionapp.ui.theme.ErrorRed
import com.example.addictionreductionapp.ui.theme.RegainOrange
import com.example.addictionreductionapp.ui.theme.RegainTeal
import com.example.addictionreductionapp.ui.theme.RegainTheme
import com.example.addictionreductionapp.ui.theme.SuccessGreen
import com.example.addictionreductionapp.ui.theme.TextGray
import com.example.addictionreductionapp.ui.theme.TextWhite

import com.example.addictionreductionapp.data.repository.SnapshotReconciliationManager
import com.example.addictionreductionapp.data.repository.UserProfileRepository
import com.example.addictionreductionapp.data.local.entities.UserProfileEntity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var isBlockTriggered = mutableStateOf(false)
    private var blockedAppName = mutableStateOf("")
    private var blockReason = mutableStateOf("")
    private var deepLinkUri = mutableStateOf<String?>(null)
    private var prefsLoaded = mutableStateOf(false)

    @Inject
    lateinit var reconciliationManager: SnapshotReconciliationManager

    @Inject
    lateinit var appLimitRepository: com.example.addictionreductionapp.data.repository.AppLimitRepository

    @Inject
    lateinit var userProfileRepository: UserProfileRepository

    private var profileSnapshot: UserProfileEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scheduleNotifications()
        
        if (intent?.data?.scheme == "smartfocus" && intent?.data?.host == "login-callback") {
            deepLinkUri.value = intent?.dataString
        }
        
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val sessionManager = com.example.addictionreductionapp.utils.SessionManager(this@MainActivity)
            val isLoggedIn = sessionManager.isLoggedIn()

            // Load profile from Room (single source of truth — M3)
            var profile = userProfileRepository.getProfile()
            if (profile == null) {
                profile = UserProfileEntity()
                userProfileRepository.upsert(profile)
            }
            if (isLoggedIn && !profile.isLoggedIn) {
                userProfileRepository.upsert(profile.copy(isLoggedIn = true))
                profile = profile.copy(isLoggedIn = true)
            }
            profileSnapshot = profile

            // Seed default apps if database is empty
            val allApps = appLimitRepository.getAllAppsOnce()
            if (allApps.isEmpty()) {
                android.util.Log.d("ConfigMigration", "App blocker database is empty. Seeding defaults...")
                try {
                    val entities = DEFAULT_APPS.map { app ->
                        com.example.addictionreductionapp.data.local.entities.AppLimitEntity(
                            packageName = app.packageName,
                            appName = app.name,
                            isSelected = app.isSelected,
                            limitMinutes = app.limitMinutes,
                            isLocked = app.isLocked,
                            blockScheduleStart = app.blockScheduleStart,
                            blockScheduleEnd = app.blockScheduleEnd,
                            isWhitelisted = app.isWhitelisted
                        )
                    }
                    appLimitRepository.upsertAll(entities)
                    android.util.Log.d("ConfigMigration", "Seeding complete. ${entities.size} apps seeded.")
                } catch (e: Exception) {
                    android.util.Log.e("ConfigMigration", "Seeding failed: ${e.message}", e)
                }
            }

            // One-time snapshot rebuild safety gate
            val snapshotPrefs = getSharedPreferences("snapshot_prefs", android.content.Context.MODE_PRIVATE)
            val rebuildCompleted = snapshotPrefs.getBoolean("snapshot_rebuild_completed", false)
            if (!rebuildCompleted) {
                android.util.Log.d("SnapshotRebuild", "snapshot_rebuild_completed=false â€” running one-time rebuild.")
                try {
                    reconciliationManager.rebuildAllSnapshots(this@MainActivity)
                    snapshotPrefs.edit().putBoolean("snapshot_rebuild_completed", true).apply()
                    android.util.Log.d("SnapshotRebuild", "snapshot_rebuild_completed flag set to true.")
                } catch (e: Exception) {
                    android.util.Log.e("SnapshotRebuild", "Rebuild failed: ${e.message}", e)
                }
            } else {
                android.util.Log.d("SnapshotRebuild", "snapshot_rebuild_completed=true â€” skipping rebuild.")
            }

            // Perform lightweight startup reconciliation for any new missing dates
            try {
                reconciliationManager.reconcileMissingSnapshots()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Signal that prefs are fully loaded so NavHost can use the correct startDestination
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                prefsLoaded.value = true
            }
        }
        
        isBlockTriggered.value = intent?.getBooleanExtra("show_block_screen", false) ?: false
        blockedAppName.value = intent?.getStringExtra("blocked_app_name") ?: ""
        blockReason.value = intent?.getStringExtra("block_reason") ?: ""

        // FIX 2 â€” Full screen: hide status bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController?.hide(WindowInsetsCompat.Type.statusBars())
        windowInsetsController?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        setContent {
            RegainTheme {
                Surface(color = DarkBackground) {
                    AppRoot(
                        isBlockTriggered = isBlockTriggered.value,
                        blockedAppName = blockedAppName.value,
                        blockReason = blockReason.value,
                        deepLinkUri = deepLinkUri.value,
                        prefsLoaded = prefsLoaded.value,
                        isLoggedIn = profileSnapshot?.isLoggedIn ?: false,
                        hasCompletedOnboarding = profileSnapshot?.hasCompletedOnboarding ?: false,
                        hasCompletedPermissionsScreen = profileSnapshot?.hasCompletedPermissionsScreen ?: false,
                        hasCompletedSmartReductionSetup = profileSnapshot?.hasCompletedSmartReductionSetup ?: false,
                        onDeepLinkHandled = { deepLinkUri.value = null },
                        onOnboardingCompleted = {
                            profileSnapshot = profileSnapshot?.copy(hasCompletedOnboarding = true)
                            lifecycleScope.launch(Dispatchers.IO) {
                                userProfileRepository.upsert(profileSnapshot!!)
                            }
                        },
                        onPermissionsCompleted = {
                            profileSnapshot = profileSnapshot?.copy(hasCompletedPermissionsScreen = true)
                            lifecycleScope.launch(Dispatchers.IO) {
                                userProfileRepository.upsert(profileSnapshot!!)
                            }
                        },
                        onSmartReductionCompleted = {
                            profileSnapshot = profileSnapshot?.copy(hasCompletedSmartReductionSetup = true)
                            lifecycleScope.launch(Dispatchers.IO) {
                                userProfileRepository.upsert(profileSnapshot!!)
                            }
                        },
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
        if (intent.data?.scheme == "smartfocus" && intent.data?.host == "login-callback") {
            deepLinkUri.value = intent.dataString
        }
        if (intent.getBooleanExtra("show_block_screen", false)) {
            isBlockTriggered.value = true
            blockedAppName.value = intent.getStringExtra("blocked_app_name") ?: ""
            blockReason.value = intent.getStringExtra("block_reason") ?: ""
        }
    }

    private fun scheduleNotifications() {
        WorkScheduler.scheduleAll(this)
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface InterventionRepositoryEntryPoint {
    fun interventionRepository(): com.example.addictionreductionapp.data.repository.InterventionRepository
}

@Composable
fun AppRoot(
    isBlockTriggered: Boolean,
    blockedAppName: String,
    blockReason: String,
    deepLinkUri: String?,
    prefsLoaded: Boolean,
    isLoggedIn: Boolean,
    hasCompletedOnboarding: Boolean,
    hasCompletedPermissionsScreen: Boolean,
    hasCompletedSmartReductionSetup: Boolean,
    onDeepLinkHandled: () -> Unit,
    onOnboardingCompleted: () -> Unit,
    onPermissionsCompleted: () -> Unit,
    onSmartReductionCompleted: () -> Unit,
    onBlockShown: () -> Unit
) {
    val context = LocalContext.current
    val authViewModel: com.example.addictionreductionapp.viewmodel.AuthViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = remember(navBackStackEntry) { navBackStackEntry?.destination?.route }

    val showOnboarding = !hasCompletedOnboarding
    val showPermissions = !hasCompletedPermissionsScreen
    val showSmartReductionSetup = !hasCompletedSmartReductionSetup

    // Wait until prefs are loaded before rendering any navigation
    if (!prefsLoaded) {
        Box(Modifier.fillMaxSize().background(DarkBackground))
        return
    }

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

    LaunchedEffect(deepLinkUri) {
        if (deepLinkUri != null) {
            authViewModel.handleDeepLink(deepLinkUri) { result ->
                if (result is com.example.addictionreductionapp.utils.AuthResult.Success) {
                    // Login state is managed by SessionManager (encrypted prefs)
                    navController.navigate("home") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            onDeepLinkHandled()
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
        Box(Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = if (!isLoggedIn) "login" else if (showOnboarding) "onboarding" else if (showPermissions) "permissions" else if (showSmartReductionSetup) "smart_reduction_setup" else "home",
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None }
            ) {
                composable("login") {
                    LoginScreen(
                        onLoginSuccess = {
                            navController.navigate(
                                if (showOnboarding) "onboarding"
                                else if (showPermissions) "permissions"
                                else if (showSmartReductionSetup) "smart_reduction_setup"
                                else "home"
                            ) {
                                popUpTo("login") { inclusive = true }
                            }
                        },
                        onNavigateToRegister = {
                            navController.navigate("register")
                        }
                    )
                }

                composable("register") {
                    RegisterScreen(
                        onRegisterSuccess = {
                            navController.navigate(
                                if (showOnboarding) "onboarding"
                                else if (showPermissions) "permissions"
                                else if (showSmartReductionSetup) "smart_reduction_setup"
                                else "home"
                            ) {
                                popUpTo("register") { inclusive = true }
                                popUpTo("login") { inclusive = true }
                            }
                        },
                        onNavigateToLogin = {
                            navController.popBackStack()
                        }
                    )
                }

                composable("onboarding") {
                    OnboardingScreen(
                        onComplete = {
                            navController.navigate("permissions") {
                                popUpTo("onboarding") { inclusive = true }
                            }
                        }
                    )
                }

                composable("permissions") {
                    PermissionScreen(
                        onSkip = {
                            onPermissionsCompleted()
                            navController.navigate("smart_reduction_setup") {
                                popUpTo("permissions") { inclusive = true }
                            }
                        },
                        onContinue = {
                            onPermissionsCompleted()
                            navController.navigate("smart_reduction_setup") {
                                popUpTo("permissions") { inclusive = true }
                            }
                        }
                    )
                }

                composable("smart_reduction_setup") {
                    SmartReductionSetupScreen(
                        onSkip = {
                            onSmartReductionCompleted()
                            navController.navigate("home") {
                                popUpTo("smart_reduction_setup") { inclusive = true }
                            }
                        },
                        onComplete = {
                            onSmartReductionCompleted()
                            navController.navigate("home") {
                                popUpTo("smart_reduction_setup") { inclusive = true }
                            }
                        }
                    )
                }

                composable("home") {
                    Box(Modifier.padding(padding)) {
                        HomeScreen(
                            onStartFocus = { navController.navigate("timer") },
                            onNavigateToApps = { navController.navigate("app_blocker") },
                            onNavigateToGoals = { navController.navigate("goals") },
                            onNavigateToRoadmap = { navController.navigate("roadmap") }
                        )
                    }
                }

                composable("timer") {
                    Box(Modifier.padding(padding)) {
                        FocusTimerScreen()
                    }
                }

                composable("analytics") {
                    Box(Modifier.padding(padding)) {
                        AnalyticsScreen()
                    }
                }

                composable("coach") {
                    Box(Modifier.padding(padding)) {
                        AICoachScreen()
                    }
                }

                composable("profile") {
                    val context = LocalContext.current
                    Box(Modifier.padding(padding)) {
                        ProfileScreen(
                            onNavigateToApps = { navController.navigate("app_blocker") },
                            onNavigateToPrivacy = { navController.navigate("privacy") },
                            onLogout = {
                                navController.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }
                }

                composable("settings") {
                    val context = LocalContext.current
                    Box(Modifier.padding(padding)) {
                        ProfileScreen(
                            onNavigateToApps = { navController.navigate("app_blocker") },
                            onNavigateToPrivacy = { navController.navigate("privacy") },
                            onLogout = {
                                navController.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }
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
                        onExit = { navController.popBackStack() },
                        onStartFocus = { navController.navigate("timer") {
                            popUpTo(navController.graph.startDestinationId)
                        } }
                    )
                }

                composable("goals") {
                    GoalsScreen(
                        onBack = { navController.popBackStack() }
                    )
                }

                composable("roadmap") {
                    RoadmapScreen(
                        onBack = { navController.popBackStack() }
                    )
                }

                composable("privacy") {
                    PrivacyPolicyScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
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
                BottomNavItem(
                    route = route,
                    label = label,
                    icon = icon,
                    isSelected = currentRoute == route,
                    onNavigate = {
                        if (currentRoute != route) {
                            navController.navigate(route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                            android.util.Log.d("NavDebug", "Navigated to $route, BackStack size: ${navController.currentBackStack.value.size}")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun BottomNavItem(
    route: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onNavigate: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
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
                interactionSource = interactionSource,
                onClick = onNavigate
            ),
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
