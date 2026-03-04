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
import com.example.addictionreductionapp.data.AppDataStore
import com.example.addictionreductionapp.ui.theme.*
import kotlinx.coroutines.delay
import java.util.*
import java.util.concurrent.TimeUnit

@Composable
fun HomeScreen(
    onStartFocus: () -> Unit,
    onNavigateToApps: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var totalUsedMillis by remember { mutableLongStateOf(0L) }
    var appsBlockedToday by remember { mutableIntStateOf(0) }

    val usageStatsManager = remember {
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    }

    LaunchedEffect(Unit) {
        while (true) {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val stats = usageStatsManager.queryAndAggregateUsageStats(
                calendar.timeInMillis,
                System.currentTimeMillis()
            )

            var total = 0L
            var blockedCount = 0
            AppDataStore.apps.forEach { app ->
                val time = stats[app.packageName]?.totalTimeInForeground ?: 0L
                if (app.isSelected) {
                    total += time
                    if (time >= app.limitMinutes * 60 * 1000L) {
                        blockedCount++
                    }
                }
            }
            totalUsedMillis = total
            appsBlockedToday = blockedCount
            delay(5000)
        }
    }

    val totalUsedMins = TimeUnit.MILLISECONDS.toMinutes(totalUsedMillis)
    val selectedApps = AppDataStore.apps.filter { it.isSelected }
    val totalLimitMins = selectedApps.sumOf { it.limitMinutes }.toLong().coerceAtLeast(1L)
    val remainingMins = (totalLimitMins - totalUsedMins).coerceAtLeast(0L)
    val usageProgress = (totalUsedMins.toFloat() / totalLimitMins.toFloat()).coerceIn(0f, 1f)

    val focusScore = if (selectedApps.isNotEmpty()) {
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
        Modifier.fillMaxSize()
    ) {
        // Unique gradient background with subtle pattern
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Deep dark gradient base
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        DarkBackground,
                        Color(0xFF050A12),
                        Color(0xFF0A1628),
                        Color(0xFF050A12),
                        DarkBackground
                    )
                )
            )
            // Subtle teal glow at top-right
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        RegainTeal.copy(alpha = 0.06f),
                        Color.Transparent
                    ),
                    radius = size.width * 0.6f
                ),
                radius = size.width * 0.6f,
                center = Offset(size.width * 0.9f, size.height * 0.08f)
            )
            // Subtle purple glow at bottom-left
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        RegainPurple.copy(alpha = 0.04f),
                        Color.Transparent
                    ),
                    radius = size.width * 0.5f
                ),
                radius = size.width * 0.5f,
                center = Offset(size.width * 0.1f, size.height * 0.85f)
            )
        }

    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(scrollState)
    ) {
        // Header
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Welcome back,", color = TextGray, fontSize = 14.sp)
                Text(
                    AppDataStore.userName.value,
                    color = TextWhite,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            // Focus Score Badge
            Surface(
                color = DarkCard,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, RegainTeal.copy(alpha = 0.3f))
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
        val isFocusActive = AppDataStore.isFocusModeActive.value

        OutlinedButton(
            onClick = {
                AppDataStore.isFocusModeActive.value = !isFocusActive
                AppDataStore.saveToPrefs(context)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                if (isFocusActive) Color(0xFF00BFA5) else Color(0xFF444444)
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent
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
                value = "${AppDataStore.streakCount.intValue}",
                label = "Day Streak",
                trend = if (AppDataStore.streakCount.intValue > 0) "+${AppDataStore.streakCount.intValue}" else "",
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
                value = "${AppDataStore.sessionsCompleted.intValue}",
                label = "Sessions Done",
                accentColor = RegainBlue
            )
        }

        Spacer(Modifier.height(20.dp))

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
                    if (selectedApps.isEmpty()) {
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
}
