package com.example.addictionreductionapp.screens

import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.addictionreductionapp.components.AppUsageItem
import com.example.addictionreductionapp.components.StatCard
import com.example.addictionreductionapp.data.AppDataStore
import com.example.addictionreductionapp.data.DailyUsage
import com.example.addictionreductionapp.data.RealTimeUsage
import com.example.addictionreductionapp.ui.theme.*
import kotlinx.coroutines.delay
import java.util.*
import java.util.concurrent.TimeUnit

@Composable
fun AnalyticsScreen() {
    val context = LocalContext.current
    var selectedPeriod by remember { mutableIntStateOf(0) }
    var usageList by remember { mutableStateOf(listOf<RealTimeUsage>()) }
    var dailyHistory by remember { mutableStateOf(listOf<DailyUsage>()) }
    var totalScreenTime by remember { mutableLongStateOf(0L) }

    val usageStatsManager = remember {
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    }

    LaunchedEffect(selectedPeriod) {
        while (true) {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val now = System.currentTimeMillis()
            val startTime = when (selectedPeriod) {
                1 -> { calendar.add(Calendar.DAY_OF_YEAR, -6); calendar.timeInMillis }
                2 -> { calendar.add(Calendar.MONTH, -1); calendar.timeInMillis }
                else -> calendar.timeInMillis
            }

            val stats = usageStatsManager.queryAndAggregateUsageStats(startTime, now)
            usageList = AppDataStore.apps.map { app ->
                RealTimeUsage(
                    app.name,
                    app.packageName,
                    stats[app.packageName]?.totalTimeInForeground ?: 0L,
                    app.limitMinutes
                )
            }
            totalScreenTime = usageList.sumOf { it.timeSpentMillis }

            if (selectedPeriod > 0) {
                val dailyStats = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY, startTime, now
                )
                dailyHistory = dailyStats.groupBy {
                    val cal = Calendar.getInstance().apply { timeInMillis = it.firstTimeStamp }
                    cal.get(Calendar.DAY_OF_YEAR)
                }.map { (dayOfYear, statsInDay) ->
                    val cal = Calendar.getInstance().apply { set(Calendar.DAY_OF_YEAR, dayOfYear) }
                    val dayLabel = when (cal.get(Calendar.DAY_OF_WEEK)) {
                        Calendar.MONDAY -> "Mon"
                        Calendar.TUESDAY -> "Tue"
                        Calendar.WEDNESDAY -> "Wed"
                        Calendar.THURSDAY -> "Thu"
                        Calendar.FRIDAY -> "Fri"
                        Calendar.SATURDAY -> "Sat"
                        else -> "Sun"
                    }
                    DailyUsage(dayLabel, statsInDay.sumOf { it.totalTimeInForeground })
                }
            } else {
                dailyHistory = listOf()
            }
            delay(5000)
        }
    }

    val totalMins = TimeUnit.MILLISECONDS.toMinutes(totalScreenTime)
    val productiveMins = AppDataStore.totalFocusMinutes.intValue.toLong()
    val distractingMins = (totalMins - productiveMins).coerceAtLeast(0L)

    Box(Modifier.fillMaxSize()) {
        // Gradient background
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        DarkBackground,
                        Color(0xFF040810),
                        Color(0xFF0B1220),
                        Color(0xFF040810),
                        DarkBackground
                    )
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(RegainBlue.copy(alpha = 0.04f), Color.Transparent),
                    radius = size.width * 0.5f
                ),
                radius = size.width * 0.5f,
                center = Offset(size.width * 0.8f, size.height * 0.2f)
            )
        }

    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(
            "Screen Time",
            color = TextWhite,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Track your digital habits",
            color = TextGray,
            fontSize = 13.sp
        )

        Spacer(Modifier.height(16.dp))

        // Period Tabs
        Surface(
            color = DarkCard,
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
            ) {
                listOf("Today", "Weekly", "Monthly").forEachIndexed { index, title ->
                    val isSelected = selectedPeriod == index
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .padding(2.dp),
                        color = if (isSelected) RegainTeal else DarkCard,
                        shape = RoundedCornerShape(8.dp),
                        onClick = { selectedPeriod = index }
                    ) {
                        Text(
                            title,
                            color = if (isSelected) DarkBackground else TextGray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 10.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Summary Card
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Total Screen Time", color = TextGray, fontSize = 12.sp)
                Text(
                    "${totalMins / 60}h ${totalMins % 60}m",
                    color = TextWhite,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Productive vs Distracting
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = SuccessGreen.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Column {
                                Text("Focus", color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("${productiveMins}m", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = ErrorRed.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.PhonelinkErase,
                                contentDescription = null,
                                tint = ErrorRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Column {
                                Text("Distract", color = ErrorRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("${distractingMins}m", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Bar Chart for weekly/monthly
        if (selectedPeriod > 0 && dailyHistory.isNotEmpty()) {
            UsageBarChart(dailyHistory)
            Spacer(Modifier.height(16.dp))
        }

        // App Breakdown
        Text(
            if (selectedPeriod == 0) "App Usage Today" else "App Breakdown",
            color = TextWhite,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))

        LazyColumn(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(usageList.sortedByDescending { it.timeSpentMillis }) { appUsage ->
                AppUsageItem(
                    name = appUsage.name,
                    timeSpentMins = TimeUnit.MILLISECONDS.toMinutes(appUsage.timeSpentMillis),
                    limitMinutes = appUsage.limitMinutes
                )
            }
        }
    }
    } // Close Box
}

@Composable
fun UsageBarChart(data: List<DailyUsage>) {
    val maxUsage = data.maxOfOrNull { it.totalMillis }?.coerceAtLeast(1L) ?: 1L

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Usage Trend", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                data.forEach { day ->
                    val barHeight = (day.totalMillis.toFloat() / maxUsage.toFloat()).coerceAtLeast(0.05f)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .width(16.dp)
                                .fillMaxHeight(barHeight)
                                .background(
                                    Brush.verticalGradient(listOf(RegainTeal, RegainPurple)),
                                    RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                )
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(day.dayLabel, color = TextGray, fontSize = 9.sp)
                    }
                }
            }
        }
    }
}
