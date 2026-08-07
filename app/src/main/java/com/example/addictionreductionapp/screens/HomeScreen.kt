package com.example.addictionreductionapp.screens

import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.addictionreductionapp.components.*
import com.example.addictionreductionapp.ui.theme.*
import com.example.addictionreductionapp.utils.PermissionUtils
import android.provider.Settings
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import kotlinx.coroutines.delay
import java.util.*
import java.util.concurrent.TimeUnit
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.addictionreductionapp.viewmodel.GoalsViewModel
import com.example.addictionreductionapp.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    onStartFocus: () -> Unit,
    onNavigateToApps: () -> Unit,
    onNavigateToGoals: () -> Unit = {},
    onNavigateToRoadmap: () -> Unit = {},
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    val startCompose = android.os.SystemClock.elapsedRealtime()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    val uiState by homeViewModel.uiState.collectAsState()
    val totalUsedMillis = uiState.totalUsedMillis
    val appsBlockedToday = uiState.appsBlockedToday

    val profile by homeViewModel.profile.collectAsState()
    val recentInterventions by homeViewModel.recentInterventions.collectAsState()
    val reductionPlans by homeViewModel.reductionPlans.collectAsState()

    val goalsViewModel: GoalsViewModel = hiltViewModel()
    val activeGoals by goalsViewModel.activeGoals.collectAsState()
    val dailyGoalMins = activeGoals.firstOrNull()?.targetScreenTimePerDay?.toLong() ?: uiState.totalLimitMins

    val totalUsedMins = TimeUnit.MILLISECONDS.toMinutes(totalUsedMillis)
    val totalLimitMins = dailyGoalMins.coerceAtLeast(1L)
    val remainingMins = (totalLimitMins - totalUsedMins).coerceAtLeast(0L)
    val usageProgress = (totalUsedMins.toFloat() / totalLimitMins.toFloat()).coerceIn(0f, 1f)

    val focusScore = if (uiState.hasSelectedApps) {
        ((1f - usageProgress) * 100).toInt().coerceIn(0, 100)
    } else {
        100
    }

    val motivationalMessages = listOf(
        "Great job staying focused! Keep it up!",
        "You're building strong habits. Keep going!",
        "Every minute of focus counts. You're doing amazing!",
        "Stay disciplined, greatness is built daily!",
        "Your future self will thank you for today's focus!"
    )
    val buddyMessage = remember {
        motivationalMessages[Calendar.getInstance().get(Calendar.HOUR_OF_DAY) % motivationalMessages.size]
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DarkBackground,
                        Color(0xFF050A12),
                        Color(0xFF0A1628),
                        Color(0xFF050A12),
                        DarkBackground
                    )
                )
            )
    ) {

        val hasUsage = PermissionUtils.hasUsageAccess(context)
        val hasOverlay = PermissionUtils.hasOverlayPermission(context)
        val hasAccessibility = PermissionUtils.hasAccessibilityPermission(context)
        val batteryOptWhitelisted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.isIgnoringBatteryOptimizations(context.packageName)
        } else true

    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(scrollState)
    ) {
        if (!hasUsage || !hasOverlay || !hasAccessibility) {
            Card(
                colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, ErrorRed),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRed)
                        Spacer(Modifier.width(8.dp))
                        Text("Permissions Required", color = ErrorRed, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("The app blocker needs the following permissions to work:", color = TextWhite, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))

                    if (!hasUsage) {
                        Button(
                            onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply { data = Uri.parse("package:${context.packageName}") }) },
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("1. Grant Usage Access", color = Color.White)
                        }
                    }
                    if (!hasOverlay) {
                        Button(
                            onClick = { context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply { data = Uri.parse("package:${context.packageName}") }) },
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("2. Grant Display Over Other Apps", color = Color.White)
                        }
                    }
                    if (!hasAccessibility) {
                        Button(
                            onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("3. Enable Accessibility Service", color = Color.White)
                        }
                    }
                }
            }
        }

        // Battery optimization warning (M10 — prevents service disabling on OEMs)
        if (hasAccessibility && !batteryOptWhitelisted) {
            Card(
                colors = CardDefaults.cardColors(containerColor = WarningYellow.copy(alpha = 0.12f)),
                border = BorderStroke(1.dp, WarningYellow.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BatteryAlert, contentDescription = null, tint = WarningYellow, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Battery Optimization", color = WarningYellow, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Battery optimization may disable app monitoring. Whitelist SmartFocus to keep it running reliably.",
                        color = TextGray, fontSize = 12.sp, lineHeight = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                context.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                })
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WarningYellow),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Disable Battery Optimization", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Header
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Welcome back,", color = TextGray, fontSize = 14.sp)
                Text(
                    profile?.userName ?: "User",
                    color = TextWhite,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Focus Score Badge
                Surface(
                    color = DarkCard,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, RegainTeal.copy(alpha = 0.3f)),
                    modifier = Modifier.clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    ) { onNavigateToGoals() }
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.FlashOn,
                            contentDescription = null,
                            tint = RegainTeal,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "FOCUS",
                                color = TextGray,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                "$focusScore%",
                                color = TextWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // Circular Progress
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            AnimatedCircularProgress(
                progress = 1f - usageProgress,
                size = 240.dp
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        color = RegainTeal.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "REMAINING",
                            color = RegainTeal,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "${remainingMins / 60}",
                            color = TextWhite,
                            fontSize = 56.sp,
                            fontWeight = FontWeight.Light
                        )
                        Text(
                            "h",
                            color = TextGray,
                            fontSize = 20.sp,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${remainingMins % 60}",
                            color = TextWhite,
                            fontSize = 56.sp,
                            fontWeight = FontWeight.Light
                        )
                        Text(
                            "m",
                            color = TextGray,
                            fontSize = 20.sp,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = CircleShape,
                        border = BorderStroke(
                            1.dp,
                            if (usageProgress < 0.7f) RegainTeal.copy(alpha = 0.5f)
                            else ErrorRed.copy(alpha = 0.5f)
                        ),
                        color = Color.Transparent
                    ) {
                        Text(
                            if (usageProgress < 0.7f) "On Track" else "At Risk",
                            color = if (usageProgress < 0.7f) RegainTeal else ErrorRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        // Focus Mode Toggle Button
        val isFocusActive = profile?.isFocusModeActive ?: false

        OutlinedButton(
            onClick = {
                homeViewModel.toggleFocusMode()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(
                    elevation = if (isFocusActive) 12.dp else 0.dp,
                    shape = RoundedCornerShape(14.dp),
                    clip = false,
                    ambientColor = Color(0xFF00BFA5).copy(alpha = 0.5f),
                    spotColor = Color(0xFF00BFA5).copy(alpha = 0.5f)
                ),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                if (isFocusActive) Color(0xFF00BFA5) else Color(0xFF444444)
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                focusedElevation = 0.dp,
                hoveredElevation = 0.dp
            )
        ) {
            Icon(
                if (isFocusActive) Icons.Default.Shield else Icons.Default.PlayCircleFilled,
                contentDescription = null,
                tint = if (isFocusActive) Color(0xFF00BFA5) else Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                if (isFocusActive) "Stop Focus Mode" else "Start Deep Focus",
                color = if (isFocusActive) Color(0xFF00BFA5) else Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Status chip below button
        Spacer(Modifier.height(10.dp))
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(
                        if (isFocusActive) Color(0xFF00BFA5) else Color(0xFF555555),
                        CircleShape
                    )
            )
            Spacer(Modifier.width(6.dp))
            Text(
                if (isFocusActive) "Focus Active" else "Focus Off",
                color = if (isFocusActive) Color(0xFF00BFA5) else Color(0xFF555555),
                fontSize = 13.sp
            )
        }

        Spacer(Modifier.height(8.dp))

        // Focus Mode Status Chip
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(
                        if (isFocusActive) SuccessGreen else TextGray,
                        CircleShape
                    )
            )
            Spacer(Modifier.width(6.dp))
            Text(
                if (isFocusActive) "Focus Active" else "Focus Off",
                color = if (isFocusActive) SuccessGreen else TextGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(20.dp))

        // Stats Row
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Block,
                value = "$appsBlockedToday",
                label = "Apps Blocked",
                trend = if (appsBlockedToday > 0) "+$appsBlockedToday" else "",
                accentColor = RegainTeal
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.LocalFireDepartment,
                value = "${profile?.streakCount ?: 0}",
                label = "Day Streak",
                trend = if ((profile?.streakCount ?: 0) > 0) "+${profile?.streakCount}" else "",
                accentColor = RegainOrange
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.HourglassEmpty,
                value = "${totalUsedMins}m",
                label = "Usage Today",
                accentColor = RegainPurple
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.CheckCircle,
                value = "${profile?.sessionsCompleted ?: 0}",
                label = "Sessions Done",
                accentColor = RegainBlue
            )
        }

        Spacer(Modifier.height(20.dp))

        if (recentInterventions.isNotEmpty()) {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.SelfImprovement,
                        contentDescription = null,
                        tint = RegainPurple,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "${recentInterventions.size} intervention${if (recentInterventions.size != 1) "s" else ""} today",
                        color = TextGray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        if (reductionPlans.isNotEmpty()) {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = RegainTeal.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        "Today's Smart Reduction",
                        color = RegainTeal,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    reductionPlans.forEach { plan ->
                        Text(
                            "${plan.category}: ${plan.currentTarget} min target · Day ${plan.daysActive}",
                            color = TextGray,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // AI Buddy Card (Rega)
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, RegainTeal.copy(alpha = 0.2f))
        ) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = RegainTeal.copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Psychology,
                            contentDescription = null,
                            tint = RegainTeal,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Arjuna says...",
                        color = RegainTeal,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        buddyMessage,
                        color = TextWhite,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Goals CTA Card
        Card(
            Modifier
                .fillMaxWidth()
                .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { onNavigateToGoals() },
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, RegainTeal.copy(alpha = 0.2f))
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = RegainTeal.copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Flag, contentDescription = null, tint = RegainTeal, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    val baselineMins = if (reductionPlans.isNotEmpty()) {
                        reductionPlans.sumOf { it.baselineMinutes }
                    } else {
                        180
                    }
                    val todaySavedMins = (baselineMins - totalUsedMins).coerceAtLeast(0L)
                    val todaySavedHours = todaySavedMins / 60f

                    Text(
                        text = if (todaySavedHours > 0) {
                            "Today's saved: ${String.format(java.util.Locale.US, "%.1f", todaySavedHours)}h toward your goals"
                        } else {
                            "Track progress toward your goals"
                        },
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (activeGoals.isNotEmpty()) {
                            "${activeGoals.size} active goal${if (activeGoals.size != 1) "s" else ""} in progress"
                        } else {
                            "Connect screen-time reduction to meaningful life outcomes"
                        },
                        color = TextGray,
                        fontSize = 12.sp
                    )
                }
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = TextGray)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Roadmap CTA
        Card(
            Modifier
                .fillMaxWidth()
                .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { onNavigateToRoadmap() },
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, RegainPurple.copy(alpha = 0.2f))
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = RegainPurple.copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.TrendingDown, contentDescription = null, tint = RegainPurple, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Reduction Plan", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("View your weekly reduction targets", color = TextGray, fontSize = 12.sp)
                }
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = TextGray)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Daily Insight
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = DarkCardLight,
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = RegainAmber,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Daily Insight",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    if (!uiState.hasSelectedApps) {
                        Text(
                            "No apps selected for tracking. Tap the block icon to set up app limits.",
                            color = TextGray,
                            fontSize = 12.sp
                        )
                    } else {
                        Text(
                            "You've used ${totalUsedMins}m of your ${totalLimitMins}m limit today. ${
                                if (usageProgress < 0.5f) "Excellent progress!" 
                                else if (usageProgress < 0.8f) "Stay mindful." 
                                else "Consider a focus session."
                            }",
                            color = TextGray,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
    } // Close Box
    androidx.compose.runtime.SideEffect {
        val duration = android.os.SystemClock.elapsedRealtime() - startCompose
        android.util.Log.d("PerfDebug", "HomeScreen composed in $duration ms")
    }
}
