package com.example.addictionreductionapp.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.StrokeCap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.addictionreductionapp.components.*
import com.example.addictionreductionapp.ui.theme.*
import com.example.addictionreductionapp.utils.PermissionUtils
import java.util.*
import java.util.concurrent.TimeUnit
import com.example.addictionreductionapp.viewmodel.GoalsViewModel
import com.example.addictionreductionapp.viewmodel.HomeViewModel

private fun categoryIcon(category: String) = when (category.lowercase()) {
    "social" -> Icons.Default.ChatBubble
    "entertainment" -> Icons.Default.PlayArrow
    "games" -> Icons.Default.SportsEsports
    else -> Icons.Default.Category
}

private fun categoryTint(category: String) = when (category.lowercase()) {
    "social" -> RegainTeal
    "entertainment" -> Color(0xFFB388FF)
    "games" -> Color(0xFFFFAB40)
    else -> RegainTeal
}

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

    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val totalUsedMillis = uiState.totalUsedMillis
    val appsBlockedToday = uiState.appsBlockedToday

    val profile by homeViewModel.profile.collectAsStateWithLifecycle()
    val recentInterventions by homeViewModel.recentInterventions.collectAsStateWithLifecycle()
    val reductionPlans by homeViewModel.reductionPlans.collectAsStateWithLifecycle()

    val goalsViewModel: GoalsViewModel = hiltViewModel()
    val activeGoals by goalsViewModel.activeGoals.collectAsStateWithLifecycle()
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

    val motivationalMessages = remember {
        listOf(
            "Great job staying focused! Keep it up!",
            "You're building strong habits. Keep going!",
            "Every minute of focus counts. You're doing amazing!",
            "Stay disciplined, greatness is built daily!",
            "Your future self will thank you for today's focus!"
        )
    }
    val buddyMessage = remember(motivationalMessages) {
        motivationalMessages[Calendar.getInstance().get(Calendar.HOUR_OF_DAY) % motivationalMessages.size]
    }

    val homeRadial = remember {
        Brush.radialGradient(
            colors = listOf(DarkBackground, Color.Transparent),
            center = Offset(0.5f * 1000f, 0.45f * 1000f),
            radius = 800f
        )
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .background(homeRadial)
    ) {
        val hasUsage = PermissionUtils.hasUsageAccess(context)
        val hasOverlay = PermissionUtils.hasOverlayPermission(context)
        val hasAccessibility = PermissionUtils.hasAccessibilityPermission(context)
        val batteryOptWhitelisted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.isIgnoringBatteryOptimizations(context.packageName)
        } else true

        // Virtualized LazyColumn — only visible items composed, smooth scroll
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!hasUsage || !hasOverlay || !hasAccessibility) {
                item(key = "perm_warn") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, ErrorRed),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
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
                                ) { Text("1. Grant Usage Access", color = Color.White) }
                            }
                            if (!hasOverlay) {
                                Button(
                                    onClick = { context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply { data = Uri.parse("package:${context.packageName}") }) },
                                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) { Text("2. Grant Display Over Other Apps", color = Color.White) }
                            }
                            if (!hasAccessibility) {
                                Button(
                                    onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) { Text("3. Enable Accessibility Service", color = Color.White) }
                            }
                        }
                    }
                }
            }

            if (hasAccessibility && !batteryOptWhitelisted) {
                item(key = "battery") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = WarningYellow.copy(alpha = 0.12f)),
                        border = BorderStroke(1.dp, WarningYellow.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
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
            }

            item(key = "header") {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Welcome back,", color = TextGray, fontSize = 13.sp)
                        Text(
                            profile?.userName ?: "spider",
                            color = TextWhite,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Box(
                            Modifier
                                .width(72.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(RegainTeal)
                        )
                    }
                    Surface(
                        color = DarkCardLight,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, RegainTeal.copy(alpha = 0.45f)),
                        modifier = Modifier.clickable(
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        ) { onNavigateToGoals() }
                    ) {
                        Row(
                            Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.FlashOn, contentDescription = null, tint = RegainTeal, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Text("FOCUS", color = RegainTeal, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                                Text("$focusScore%", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item(key = "hero") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, DarkCardLight)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                                Text("REMAINING", color = RegainTeal, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, maxLines = 1)
                                Spacer(Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text("${remainingMins / 60}", color = TextWhite, fontSize = 40.sp, fontWeight = FontWeight.Light, maxLines = 1, softWrap = false, lineHeight = 40.sp)
                                    Text("h", color = TextGray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 7.dp, start = 2.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("${remainingMins % 60}", color = TextWhite, fontSize = 40.sp, fontWeight = FontWeight.Light, maxLines = 1, softWrap = false, lineHeight = 40.sp)
                                    Text("m", color = TextGray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 7.dp, start = 2.dp))
                                }
                                Spacer(Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, if (usageProgress < 0.7f) RegainTeal.copy(alpha = 0.45f) else ErrorRed.copy(alpha = 0.45f)),
                                    color = Color.Transparent
                                ) {
                                    Text(
                                        if (usageProgress < 0.7f) "On Track" else "At Risk",
                                        color = if (usageProgress < 0.7f) RegainTeal else ErrorRed,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                                Text("FOCUS MODE", color = RegainTeal, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.9.sp, maxLines = 1)
                                Spacer(Modifier.height(10.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Text("On Track", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, modifier = Modifier.weight(1f, fill = false))
                                    Spacer(Modifier.width(8.dp))
                                    Surface(color = RegainTeal.copy(alpha = 0.14f), shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, RegainTeal.copy(alpha = 0.28f))) {
                                        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Box(Modifier.size(6.dp).background(RegainTeal, CircleShape))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Active", color = RegainTeal, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1, softWrap = false)
                                        }
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                Text("Stay focused. Build better habits. Achieve more.", color = TextGray, fontSize = 11.sp, lineHeight = 14.sp, maxLines = 3)
                            }
                        }
                        Spacer(Modifier.height(18.dp))
                        val isFocusActive = profile?.isFocusModeActive ?: false
                        OutlinedButton(
                            onClick = { homeViewModel.toggleFocusMode() },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, if (isFocusActive) RegainTeal.copy(alpha = 0.9f) else DarkCardLight),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Icon(if (isFocusActive) Icons.Default.Shield else Icons.Default.PlayCircleFilled, contentDescription = null, tint = RegainTeal, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (isFocusActive) "Stop Focus Mode" else "Start Deep Focus", color = RegainTeal, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(7.dp).background(if (isFocusActive) SuccessGreen else TextGray.copy(alpha = 0.55f), CircleShape))
                            Spacer(Modifier.width(7.dp))
                            Text(if (isFocusActive) "Focus Active" else "Focus Off", color = if (isFocusActive) SuccessGreen else TextGray, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }

            item(key = "mini_stats") {
                val goalsProgress = if (activeGoals.isNotEmpty()) {
                    val completed = 0
                    "${completed}/${activeGoals.size}"
                } else "0/1"
                val goalsProgressFrac = if (activeGoals.isNotEmpty()) 0f else 0f
                Row(
                    Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    HomeStatCardCompact(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        icon = Icons.Default.HourglassEmpty,
                        value = "${totalUsedMins}m",
                        label = "Usage Today",
                        accentColor = Color(0xFFB388FF)
                    )
                    HomeStatCardCompact(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        icon = Icons.Default.TrackChanges,
                        value = "${profile?.sessionsCompleted ?: 0}",
                        label = "Focus Sessions",
                        accentColor = RegainTeal
                    )
                    HomeStatCardCompact(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        icon = Icons.Default.LocalFireDepartment,
                        value = "${profile?.streakCount ?: 0}",
                        label = "Day Streak",
                        accentColor = Color(0xFFFFAB40)
                    )
                    HomeStatCardCompact(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        icon = Icons.Default.BarChart,
                        value = goalsProgress,
                        label = "Goals Progress",
                        accentColor = Color(0xFF69F0AE),
                        progress = goalsProgressFrac
                    )
                }
            }

            if (recentInterventions.isNotEmpty()) {
                item(key = "interventions") {
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, DarkCardLight.copy(alpha = 0.5f))
                    ) {
                        Row(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SelfImprovement, contentDescription = null, tint = Color(0xFFB388FF), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("${recentInterventions.size} intervention${if (recentInterventions.size != 1) "s" else ""} today",
                                color = TextGray, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            item(key = "smart_reduction") {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, DarkCardLight)
                ) {
                    Column(Modifier.padding(12.dp).fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Today's Smart Reduction",
                                color = RegainTeal,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = DarkCardLight,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, DarkCardLight),
                                modifier = Modifier.clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { onNavigateToRoadmap() }
                            ) {
                                Row(Modifier.padding(horizontal = 9.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("View All", color = RegainTeal, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                                    Spacer(Modifier.width(2.dp))
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextGray, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        if (reductionPlans.isEmpty()) {
                            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Insights, contentDescription = null, tint = TextGray.copy(alpha = 0.6f), modifier = Modifier.size(26.dp))
                                Spacer(Modifier.height(6.dp))
                                Text("No active reduction plans", color = TextGray, fontSize = 12.sp, maxLines = 1)
                                Text("Set up Smart Reduction to see categories here", color = TextGray.copy(alpha = 0.7f), fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        } else {
                            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                reductionPlans.forEach { plan ->
                                    val tint = categoryTint(plan.category)
                                    val progress = (plan.currentTarget.toFloat() / plan.baselineMinutes.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Surface(color = DarkCardLight, shape = CircleShape, modifier = Modifier.size(34.dp), border = BorderStroke(1.dp, DarkCardLight)) {
                                            Box(contentAlignment = Alignment.Center) { Icon(categoryIcon(plan.category), contentDescription = null, tint = tint, modifier = Modifier.size(15.dp)) }
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Column(Modifier.weight(1f)) {
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Text(plan.category, color = TextWhite, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                                                Spacer(Modifier.width(8.dp))
                                                Text("0m / ${plan.currentTarget}m", color = TextGray, fontSize = 11.sp, maxLines = 1, softWrap = false)
                                            }
                                            Spacer(Modifier.height(5.dp))
                                            Box(
                                                Modifier.fillMaxWidth().height(5.dp).clip(CircleShape).background(DarkCardLight)
                                            ) {
                                                Box(
                                                    Modifier.fillMaxWidth(progress * 0.35f + 0.12f)
                                                        .fillMaxHeight()
                                                        .clip(CircleShape)
                                                        .background(RegainTeal)
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

            item(key = "arjuna") {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, DarkCardLight)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = DarkCardLight, shape = CircleShape, modifier = Modifier.size(36.dp), border = BorderStroke(1.dp, DarkCardLight)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = RegainTeal, modifier = Modifier.size(17.dp)) }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Arjuna says...", color = RegainTeal, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Spacer(Modifier.height(3.dp))
                            Text(buddyMessage, color = TextWhite, fontSize = 12.5.sp, lineHeight = 16.sp, maxLines = 3)
                        }
                        Spacer(Modifier.width(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(6.dp).background(RegainTeal, CircleShape))
                            Spacer(Modifier.width(3.dp))
                            Box(Modifier.size(6.dp).background(RegainTeal.copy(alpha = 0.5f), CircleShape))
                            Spacer(Modifier.width(3.dp))
                            Box(Modifier.size(6.dp).background(RegainTeal.copy(alpha = 0.25f), CircleShape))
                        }
                    }
                }
            }

            item(key = "bottom_half") {
                Row(
                    Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        Modifier.weight(1f).fillMaxHeight().clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { onNavigateToGoals() },
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, DarkCardLight)
                    ) {
                        Row(
                            Modifier.padding(11.dp).fillMaxHeight(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(color = DarkCardLight, shape = CircleShape, modifier = Modifier.size(34.dp), border = BorderStroke(1.dp, DarkCardLight)) {
                                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Description, contentDescription = null, tint = RegainTeal, modifier = Modifier.size(15.dp)) }
                            }
                            Spacer(Modifier.width(9.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                                Text("Today's saved", color = TextGray, fontSize = 11.sp, maxLines = 1)
                                val baselineMins = if (reductionPlans.isNotEmpty()) reductionPlans.sumOf { it.baselineMinutes } else 180
                                val todaySavedMins = (baselineMins - totalUsedMins).coerceAtLeast(0L)
                                val todaySavedHours = todaySavedMins / 60f
                                Text(String.format(Locale.US, "%.1fh", todaySavedHours), color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
                                Text("toward your goals", color = TextGray, fontSize = 10.sp, maxLines = 1)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextGray, modifier = Modifier.size(16.dp))
                        }
                    }
                    Card(
                        Modifier.weight(1f).fillMaxHeight().clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { onNavigateToRoadmap() },
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, DarkCardLight)
                    ) {
                        Row(
                            Modifier.padding(11.dp).fillMaxHeight(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(color = DarkCardLight, shape = CircleShape, modifier = Modifier.size(34.dp), border = BorderStroke(1.dp, DarkCardLight)) {
                                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.TrendingDown, contentDescription = null, tint = Color(0xFFB388FF), modifier = Modifier.size(15.dp)) }
                            }
                            Spacer(Modifier.width(9.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                                Text("Reduction Plan", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, maxLines = 1)
                                Text("View your weekly reduction targets", color = TextGray, fontSize = 10.sp, lineHeight = 12.sp, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextGray, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            item(key = "insight") {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, DarkCardLight)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = DarkCardLight, shape = CircleShape, modifier = Modifier.size(34.dp), border = BorderStroke(1.dp, DarkCardLight)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFFFCA28), modifier = Modifier.size(17.dp)) }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Daily Insight", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, maxLines = 1)
                            Spacer(Modifier.height(2.dp))
                            if (!uiState.hasSelectedApps) {
                                Text("No apps selected for tracking. Tap to set up app limits.", color = TextGray, fontSize = 11.sp, lineHeight = 14.sp)
                            } else {
                                Text(
                                    "You've used ${totalUsedMins}m of ${totalLimitMins}m today. ${if (usageProgress < 0.5f) "Excellent progress!" else if (usageProgress < 0.8f) "Stay mindful." else "Consider a focus session."}",
                                    color = TextGray, fontSize = 11.sp, lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    SideEffect {
        val duration = android.os.SystemClock.elapsedRealtime() - startCompose
        android.util.Log.d("PerfDebug", "HomeScreen composed in $duration ms")
    }
}
