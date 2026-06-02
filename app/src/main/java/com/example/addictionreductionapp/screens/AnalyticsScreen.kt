package com.example.addictionreductionapp.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.addictionreductionapp.data.models.AppUsageSummary
import com.example.addictionreductionapp.data.models.BehavioralIntelligenceSnapshot
import com.example.addictionreductionapp.data.models.CategoryAnalytics
import com.example.addictionreductionapp.data.models.FocusScoreDetails
import com.example.addictionreductionapp.data.models.HourlyUsagePoint
import com.example.addictionreductionapp.data.models.StreakAnalysis
import com.example.addictionreductionapp.ui.theme.*
import com.example.addictionreductionapp.viewmodel.AnalyticsUiState
import com.example.addictionreductionapp.viewmodel.AnalyticsViewModel
import com.example.addictionreductionapp.viewmodel.DashboardUiState
import com.example.addictionreductionapp.viewmodel.DashboardViewModel

// ─────────────────────────────────────────────────────────────────────────────
// Root composable — wired to both ViewModels via Hilt
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AnalyticsScreen(
    analyticsViewModel: AnalyticsViewModel = hiltViewModel(),
    dashboardViewModel: DashboardViewModel = hiltViewModel()
) {
    val analyticsState by analyticsViewModel.uiState.collectAsState()
    val dashboardState by dashboardViewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Gradient background matching app theme ─────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        DarkBackground,
                        Color(0xFF040810),
                        Color(0xFF060E1A),
                        Color(0xFF040810),
                        DarkBackground
                    )
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(RegainTeal.copy(alpha = 0.05f), Color.Transparent),
                    radius = size.width * 0.6f
                ),
                radius = size.width * 0.6f,
                center = Offset(size.width * 0.85f, size.height * 0.12f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(RegainPurple.copy(alpha = 0.04f), Color.Transparent),
                    radius = size.width * 0.5f
                ),
                radius = size.width * 0.5f,
                center = Offset(size.width * 0.15f, size.height * 0.75f)
            )
        }

        when {
            analyticsState.isLoading || dashboardState.isLoading -> {
                AnalyticsLoadingState()
            }
            analyticsState.error != null -> {
                AnalyticsErrorState(message = analyticsState.error ?: "Unknown error")
            }
            else -> {
                AnalyticsDashboardContent(
                    analytics = analyticsState,
                    dashboard = dashboardState
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Full scrollable dashboard
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AnalyticsDashboardContent(
    analytics: AnalyticsUiState,
    dashboard: DashboardUiState
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Header ─────────────────────────────────────────────────────────
        item {
            ScreenHeader()
        }

        // ── Active Warnings Banner ─────────────────────────────────────────
        if (dashboard.activeWarnings.isNotEmpty()) {
            item {
                WarningsBanner(warnings = dashboard.activeWarnings)
            }
        }

        // ── 1. Dashboard Overview ──────────────────────────────────────────
        item {
            SectionLabel(title = "Dashboard Overview", icon = Icons.Default.Dashboard)
        }
        item {
            DashboardOverviewSection(analytics = analytics, dashboard = dashboard)
        }

        // ── 2. Top Apps ────────────────────────────────────────────────────
        item {
            SectionLabel(title = "Top Apps", icon = Icons.Default.Apps)
        }
        item {
            TopAppsSection(
                topUsed = analytics.topUsedApps,
                topOpened = analytics.topOpenedApps
            )
        }

        // ── 3. Category Analytics ──────────────────────────────────────────
        item {
            SectionLabel(title = "Category Analytics", icon = Icons.Default.Category)
        }
        item {
            CategoryAnalyticsSection(
                categories = analytics.categoryUsage,
                totalMinutes = analytics.dailyScreenTimeMinutes
            )
        }

        // ── 4. Weekly Analytics ────────────────────────────────────────────
        item {
            SectionLabel(title = "Weekly Analytics", icon = Icons.Default.CalendarMonth)
        }
        item {
            WeeklyAnalyticsSection(
                hourlyUsage = analytics.hourlyUsage,
                weeklyTotal = analytics.weeklyTotalMinutes,
                weeklyAverage = analytics.weeklyAverageMinutes,
                todayMinutes = analytics.dailyScreenTimeMinutes
            )
        }

        // ── 5. Behavioral Intelligence ─────────────────────────────────────
        item {
            SectionLabel(title = "Behavioral Intelligence", icon = Icons.Default.Psychology)
        }
        item {
            BehavioralIntelligenceSection(
                snapshot = analytics.behavioralSnapshot,
                focusScore = analytics.focusScoreDetails,
                streak = analytics.streakAnalysis
            )
        }

        // Bottom padding for nav bar
        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Header
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ScreenHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Analytics",
                color = TextWhite,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Your behavioral intelligence report",
                color = TextGray,
                fontSize = 13.sp
            )
        }
        Surface(
            color = DarkCard,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, RegainTeal.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(RegainTeal, CircleShape)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Live",
                    color = RegainTeal,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section label
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SectionLabel(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = RegainTeal,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            color = TextWhite,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Warnings banner
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WarningsBanner(warnings: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = ErrorRed,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Active Alerts",
                    color = ErrorRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            warnings.forEach { warning ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("•", color = ErrorRed.copy(alpha = 0.7f), fontSize = 13.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(warning, color = TextGrayLight, fontSize = 13.sp)
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// SECTION 1 — Dashboard Overview
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun DashboardOverviewSection(
    analytics: AnalyticsUiState,
    dashboard: DashboardUiState
) {
    val screenTimeHrs = analytics.dailyScreenTimeMinutes / 60
    val screenTimeMins = analytics.dailyScreenTimeMinutes % 60
    val focusScore = dashboard.currentFocusScore
    val riskScore = dashboard.overallRiskScore

    // Top row: screen time + focus score
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Total screen time
        OverviewMetricCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.AccessTime,
            accentColor = RegainTeal,
            label = "Screen Time Today",
            value = if (screenTimeHrs > 0) "${screenTimeHrs}h ${screenTimeMins}m" else "${screenTimeMins}m",
            subValue = "${analytics.totalOpens} opens"
        )
        // Focus score
        FocusScoreCard(
            modifier = Modifier.weight(1f),
            focusScore = focusScore,
            explanation = analytics.focusScoreDetails?.explanation ?: ""
        )
    }

    Spacer(Modifier.height(12.dp))

    // Bottom row: streak + risk
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Streak
        StreakCard(
            modifier = Modifier.weight(1f),
            streakDays = analytics.streakAnalysis?.streakInfo?.currentStreakDays ?: dashboard.currentStreak,
            longestStreak = analytics.streakAnalysis?.streakInfo?.longestStreakDays ?: 0,
            isRecovered = analytics.streakAnalysis?.isRecovered ?: false
        )
        // Risk score
        RiskScoreCard(
            modifier = Modifier.weight(1f),
            riskScore = riskScore
        )
    }
}

@Composable
private fun OverviewMetricCard(
    modifier: Modifier,
    icon: ImageVector,
    accentColor: Color,
    label: String,
    value: String,
    subValue: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Surface(
                color = accentColor.copy(alpha = 0.15f),
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(value, color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(subValue, color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(label, color = TextGray, fontSize = 11.sp)
        }
    }
}

@Composable
private fun FocusScoreCard(
    modifier: Modifier,
    focusScore: Int,
    explanation: String
) {
    val scoreColor = when {
        focusScore >= 75 -> SuccessGreen
        focusScore >= 50 -> RegainAmber
        else -> ErrorRed
    }
    val animatedScore by animateFloatAsState(
        targetValue = focusScore.toFloat(),
        animationSpec = tween(durationMillis = 1200, easing = EaseOutCubic),
        label = "focusScore"
    )

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Focus Score", color = TextGray, fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(72.dp)) {
                Canvas(modifier = Modifier.size(72.dp)) {
                    // Track
                    drawArc(
                        color = DarkCardLight,
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // Progress
                    drawArc(
                        brush = Brush.sweepGradient(listOf(scoreColor, scoreColor.copy(alpha = 0.6f))),
                        startAngle = 135f,
                        sweepAngle = (animatedScore / 100f) * 270f,
                        useCenter = false,
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Text(
                    text = "$focusScore",
                    color = scoreColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(6.dp))
            Surface(
                color = scoreColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = when {
                        focusScore >= 75 -> "Excellent"
                        focusScore >= 50 -> "Moderate"
                        else -> "Low"
                    },
                    color = scoreColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun StreakCard(
    modifier: Modifier,
    streakDays: Int,
    longestStreak: Int,
    isRecovered: Boolean
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Surface(
                color = RegainOrange.copy(alpha = 0.15f),
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = RegainOrange,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "$streakDays",
                color = TextWhite,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "day streak",
                color = RegainOrange,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (longestStreak > 0) {
                Text(
                    text = "Best: ${longestStreak}d",
                    color = TextGray,
                    fontSize = 11.sp
                )
            }
            if (isRecovered) {
                Spacer(Modifier.height(4.dp))
                Surface(
                    color = SuccessGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        "Recovered",
                        color = SuccessGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RiskScoreCard(
    modifier: Modifier,
    riskScore: Float
) {
    val riskPct = (riskScore * 100).toInt()
    val riskColor = when {
        riskScore < 0.3f -> SuccessGreen
        riskScore < 0.6f -> WarningYellow
        else -> ErrorRed
    }
    val riskLabel = when {
        riskScore < 0.3f -> "Low Risk"
        riskScore < 0.6f -> "Moderate"
        else -> "High Risk"
    }
    val animatedRisk by animateFloatAsState(
        targetValue = riskScore,
        animationSpec = tween(1000, easing = EaseOutCubic),
        label = "risk"
    )

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Surface(
                color = riskColor.copy(alpha = 0.15f),
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = riskColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "$riskPct%",
                color = riskColor,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = riskLabel,
                color = riskColor.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { animatedRisk },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape),
                color = riskColor,
                trackColor = DarkCardLight,
                strokeCap = StrokeCap.Round
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// SECTION 2 — Top Apps
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun TopAppsSection(
    topUsed: List<AppUsageSummary>,
    topOpened: List<AppUsageSummary>
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Tab switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCardLight, RoundedCornerShape(10.dp))
                    .padding(4.dp)
            ) {
                listOf("Most Used", "Most Opened").forEachIndexed { index, label ->
                    val isSelected = selectedTab == index
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = if (isSelected) RegainTeal else Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
                        onClick = { selectedTab = index }
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) DarkBackground else TextGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            val apps = if (selectedTab == 0) topUsed else topOpened

            if (apps.isEmpty()) {
                EmptyDataView(message = "No app data recorded yet")
            } else {
                val maxValue = if (selectedTab == 0) {
                    (apps.maxOfOrNull { it.totalMinutes } ?: 1).coerceAtLeast(1)
                } else {
                    (apps.maxOfOrNull { it.openCount } ?: 1).coerceAtLeast(1)
                }

                apps.take(7).forEachIndexed { index, app ->
                    AppUsageRow(
                        rank = index + 1,
                        app = app,
                        maxValue = maxValue,
                        showTime = selectedTab == 0
                    )
                    if (index < apps.take(7).lastIndex) {
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AppUsageRow(
    rank: Int,
    app: AppUsageSummary,
    maxValue: Int,
    showTime: Boolean
) {
    val progress = if (showTime) {
        app.totalMinutes.toFloat() / maxValue.toFloat()
    } else {
        app.openCount.toFloat() / maxValue.toFloat()
    }.coerceIn(0f, 1f)

    val barColor = when (rank) {
        1 -> RegainTeal
        2 -> RegainBlue
        3 -> RegainPurple
        else -> RegainTeal.copy(alpha = 0.6f)
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(800, delayMillis = rank * 60, easing = EaseOutCubic),
        label = "appProgress"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank badge
        Surface(
            color = DarkCardLight,
            shape = CircleShape,
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "#$rank",
                    color = if (rank <= 3) barColor else TextGray,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = app.appName,
                    color = TextWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (showTime) formatMinutes(app.totalMinutes) else "${app.openCount} opens",
                    color = barColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape),
                color = barColor,
                trackColor = DarkCardLight,
                strokeCap = StrokeCap.Round
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// SECTION 3 — Category Analytics
// ═════════════════════════════════════════════════════════════════════════════
private val CATEGORY_ORDER = listOf(
    "social", "video", "productivity", "games", "music", "browser", "messaging", "other"
)

private fun categoryIcon(cat: String): ImageVector = when (cat.lowercase()) {
    "social" -> Icons.Default.People
    "video" -> Icons.Default.PlayCircle
    "productivity" -> Icons.Default.WorkspacePremium
    "games" -> Icons.Default.SportsEsports
    "music" -> Icons.Default.MusicNote
    "browser" -> Icons.Default.Language
    "messaging" -> Icons.Default.Message
    else -> Icons.Default.Category
}

private fun categoryColor(cat: String): Color = when (cat.lowercase()) {
    "social" -> Color(0xFF1DA1F2)
    "video" -> Color(0xFFFF0000)
    "productivity" -> SuccessGreen
    "games" -> RegainPurple
    "music" -> Color(0xFF1DB954)
    "browser" -> RegainBlue
    "messaging" -> Color(0xFF25D366)
    else -> TextGray
}

@Composable
private fun CategoryAnalyticsSection(
    categories: List<CategoryAnalytics>,
    totalMinutes: Int
) {
    // Ensure all 8 categories are present (even zeros)
    val displayCategories = CATEGORY_ORDER.map { catName ->
        categories.find { it.category.lowercase() == catName }
            ?: CategoryAnalytics(catName, 0)
    }
    val maxMins = (displayCategories.maxOfOrNull { it.totalMinutes } ?: 1).coerceAtLeast(1)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            displayCategories.forEachIndexed { index, cat ->
                CategoryRow(
                    category = cat,
                    maxMinutes = maxMins,
                    totalMinutes = totalMinutes,
                    animDelay = index * 80
                )
                if (index < displayCategories.lastIndex) {
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(
    category: CategoryAnalytics,
    maxMinutes: Int,
    totalMinutes: Int,
    animDelay: Int
) {
    val progress = if (maxMinutes > 0) category.totalMinutes.toFloat() / maxMinutes.toFloat() else 0f
    val pctOfTotal = if (totalMinutes > 0) (category.totalMinutes.toFloat() / totalMinutes * 100).toInt() else 0
    val color = categoryColor(category.category)
    val icon = categoryIcon(category.category)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(900, delayMillis = animDelay, easing = EaseOutCubic),
        label = "catProgress"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = color.copy(alpha = 0.15f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = category.category.replaceFirstChar { it.uppercase() },
                    color = if (category.totalMinutes > 0) TextWhite else TextGray.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (category.totalMinutes > 0) {
                        Text(
                            text = formatMinutes(category.totalMinutes),
                            color = color,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = "$pctOfTotal%",
                        color = TextGray,
                        fontSize = 11.sp
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape),
                color = if (category.totalMinutes > 0) color else DarkCardLight,
                trackColor = DarkCardLight,
                strokeCap = StrokeCap.Round
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// SECTION 4 — Weekly Analytics
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun WeeklyAnalyticsSection(
    hourlyUsage: List<HourlyUsagePoint>,
    weeklyTotal: Int,
    weeklyAverage: Int,
    todayMinutes: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Weekly stat chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WeeklyStatChip(
                modifier = Modifier.weight(1f),
                label = "7-Day Total",
                value = formatMinutes(weeklyTotal),
                icon = Icons.Default.DateRange,
                color = RegainTeal
            )
            WeeklyStatChip(
                modifier = Modifier.weight(1f),
                label = "Daily Avg",
                value = formatMinutes(weeklyAverage),
                icon = Icons.Default.TrendingFlat,
                color = RegainBlue
            )
            WeeklyStatChip(
                modifier = Modifier.weight(1f),
                label = "Today",
                value = formatMinutes(todayMinutes),
                icon = Icons.Default.Today,
                color = if (todayMinutes > weeklyAverage) WarningYellow else SuccessGreen
            )
        }

        // Trend indicator
        TrendIndicatorCard(todayMinutes = todayMinutes, avgMinutes = weeklyAverage)

        // Hourly usage heatmap
        HourlyUsageCard(hourlyUsage = hourlyUsage)
    }
}

@Composable
private fun WeeklyStatChip(
    modifier: Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(6.dp))
            Text(value, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(label, color = TextGray, fontSize = 10.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun TrendIndicatorCard(todayMinutes: Int, avgMinutes: Int) {
    val diff = todayMinutes - avgMinutes
    val diffPct = if (avgMinutes > 0) ((diff.toFloat() / avgMinutes) * 100).toInt() else 0
    val isAbove = diff > 0
    val trendColor = if (isAbove) WarningYellow else SuccessGreen
    val trendIcon = if (isAbove) Icons.Default.TrendingUp else Icons.Default.TrendingDown

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = trendColor.copy(alpha = 0.15f),
                shape = CircleShape,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(trendIcon, contentDescription = null, tint = trendColor, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = if (avgMinutes == 0) "Usage Trend" else if (isAbove) "Above Average Usage" else "Below Average — Well Done!",
                    color = TextWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (avgMinutes == 0) "Not enough data to show trend yet." else {
                        val sign = if (isAbove) "+" else ""
                        "Today is $sign$diffPct% vs your weekly average of ${formatMinutes(avgMinutes)}"
                    },
                    color = TextGray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun HourlyUsageCard(hourlyUsage: List<HourlyUsagePoint>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Hourly Usage Today", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("24h view", color = TextGray.copy(alpha = 0.6f), fontSize = 11.sp)
            }
            Spacer(Modifier.height(12.dp))

            if (hourlyUsage.isEmpty()) {
                EmptyDataView(message = "No hourly data yet")
            } else {
                HourlyBarChart(hourlyUsage = hourlyUsage)
            }
        }
    }
}

@Composable
private fun HourlyBarChart(hourlyUsage: List<HourlyUsagePoint>) {
    // Build full 24-hour map
    val hourMap = hourlyUsage.associate { it.hourOfDay to it.minutesUsed }
    val allHours = (0..23).map { h -> Pair(h, hourMap[h] ?: 0) }
    val maxMins = (allHours.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)

    val lateNightHours = setOf(22, 23, 0, 1, 2, 3, 4, 5)
    val eveningHours = setOf(18, 19, 20, 21)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        allHours.forEach { (hour, mins) ->
            val barHeightFrac = (mins.toFloat() / maxMins.toFloat()).coerceAtLeast(0.04f)
            val barColor = when (hour) {
                in lateNightHours -> ErrorRed
                in eveningHours -> RegainAmber
                else -> RegainTeal
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(barHeightFrac)
                        .background(
                            if (mins > 0) Brush.verticalGradient(listOf(barColor, barColor.copy(alpha = 0.4f)))
                            else Brush.verticalGradient(listOf(DarkCardLight, DarkCardLight)),
                            RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                        )
                )
            }
        }
    }

    Spacer(Modifier.height(4.dp))
    // Hour labels (every 6 hours)
    Row(modifier = Modifier.fillMaxWidth()) {
        listOf(0, 6, 12, 18, 23).forEachIndexed { i, h ->
            if (i > 0) Spacer(Modifier.weight(1f))
            Text(
                text = when (h) { 0 -> "12am"; 6 -> "6am"; 12 -> "12pm"; 18 -> "6pm"; else -> "11pm" },
                color = TextGray.copy(alpha = 0.6f),
                fontSize = 9.sp,
                textAlign = TextAlign.Center
            )
        }
    }

    Spacer(Modifier.height(8.dp))
    // Legend
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        LegendDot(color = RegainTeal, label = "Day")
        LegendDot(color = RegainAmber, label = "Evening")
        LegendDot(color = ErrorRed, label = "Late night")
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.width(4.dp))
        Text(label, color = TextGray.copy(alpha = 0.7f), fontSize = 10.sp)
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// SECTION 5 — Behavioral Intelligence
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun BehavioralIntelligenceSection(
    snapshot: BehavioralIntelligenceSnapshot?,
    focusScore: FocusScoreDetails?,
    streak: StreakAnalysis?
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (snapshot == null) {
            EmptyDataCardFull(message = "Behavioral analysis unavailable yet")
            return@Column
        }

        // Doomscroll risk
        BehaviorSignalCard(
            icon = Icons.Default.ScreenLockPortrait,
            title = "Doomscroll Risk",
            detected = snapshot.doomscroll.detected,
            severity = snapshot.doomscroll.severityScore,
            detail = if (snapshot.doomscroll.detected)
                "${snapshot.doomscroll.continuousMinutes}min continuous session detected"
            else "No prolonged scrolling sessions detected",
            accentColor = if (snapshot.doomscroll.detected) ErrorRed else SuccessGreen
        )

        // Compulsive switching
        BehaviorSignalCard(
            icon = Icons.Default.SwapHoriz,
            title = "Compulsive Switching",
            detected = snapshot.compulsiveSwitching.detected,
            severity = snapshot.compulsiveSwitching.severityScore,
            detail = "%.1f opens/hr".format(snapshot.compulsiveSwitching.switchesPerHour) +
                    if (snapshot.compulsiveSwitching.detected) " — excessive app switching" else " — within normal range",
            accentColor = if (snapshot.compulsiveSwitching.detected) WarningYellow else SuccessGreen
        )

        // Late night usage
        BehaviorSignalCard(
            icon = Icons.Default.Bedtime,
            title = "Late-Night Usage",
            detected = snapshot.lateNightUsage.detected,
            severity = snapshot.lateNightUsage.severityScore,
            detail = if (snapshot.lateNightUsage.detected)
                "${snapshot.lateNightUsage.minutesUsed}min used between 10pm–5am"
            else "No significant late-night usage",
            accentColor = if (snapshot.lateNightUsage.detected) RegainOrange else SuccessGreen
        )

        // Relapse risk
        BehaviorSignalCard(
            icon = Icons.Default.Warning,
            title = "Relapse Risk",
            detected = snapshot.relapse.detected,
            severity = snapshot.relapse.severityScore,
            detail = snapshot.relapse.indicator,
            accentColor = if (snapshot.relapse.detected) ErrorRed else SuccessGreen
        )

        // Focus engine explanation
        if (focusScore != null) {
            FocusInsightCard(focusScore = focusScore)
        }

        // Streak insight
        if (streak != null) {
            StreakInsightCard(streak = streak)
        }
    }
}

@Composable
private fun BehaviorSignalCard(
    icon: ImageVector,
    title: String,
    detected: Boolean,
    severity: Float,
    detail: String,
    accentColor: Color
) {
    val animatedSeverity by animateFloatAsState(
        targetValue = severity,
        animationSpec = tween(900, easing = EaseOutCubic),
        label = "severity"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (detected) accentColor.copy(alpha = 0.08f) else DarkCard
        ),
        shape = RoundedCornerShape(14.dp),
        border = if (detected) androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)) else null
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = accentColor.copy(alpha = 0.15f),
                shape = CircleShape,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Surface(
                        color = accentColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (detected) "DETECTED" else "CLEAR",
                            color = accentColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(detail, color = TextGray, fontSize = 12.sp, lineHeight = 16.sp)
                if (detected && severity > 0f) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Severity", color = TextGray.copy(alpha = 0.6f), fontSize = 10.sp)
                        Spacer(Modifier.width(8.dp))
                        LinearProgressIndicator(
                            progress = { animatedSeverity },
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(CircleShape),
                            color = accentColor,
                            trackColor = DarkCardLight,
                            strokeCap = StrokeCap.Round
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${(severity * 100).toInt()}%",
                            color = accentColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FocusInsightCard(focusScore: FocusScoreDetails) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                color = RegainTeal.copy(alpha = 0.15f),
                shape = CircleShape,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Insights,
                        contentDescription = null,
                        tint = RegainTeal,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Focus Engine Report", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                if (focusScore.explanation.isNotBlank()) {
                    Text(focusScore.explanation, color = TextGray, fontSize = 12.sp, lineHeight = 17.sp)
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    MiniStat(
                        label = "Productive",
                        value = "${(focusScore.productiveRatio * 100).toInt()}%",
                        color = SuccessGreen
                    )
                    MiniStat(
                        label = "Distraction",
                        value = "${(focusScore.distractionRatio * 100).toInt()}%",
                        color = ErrorRed
                    )
                    MiniStat(
                        label = "App Switches",
                        value = "${focusScore.totalAppSwitches}",
                        color = RegainAmber
                    )
                }
            }
        }
    }
}

@Composable
private fun StreakInsightCard(streak: StreakAnalysis) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                color = RegainOrange.copy(alpha = 0.15f),
                shape = CircleShape,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = RegainOrange,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Streak Analysis", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(streak.explanation, color = TextGray, fontSize = 12.sp, lineHeight = 17.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    MiniStat(
                        label = "Current",
                        value = "${streak.streakInfo.currentStreakDays}d",
                        color = RegainOrange
                    )
                    MiniStat(
                        label = "Best",
                        value = "${streak.streakInfo.longestStreakDays}d",
                        color = GoldAccent
                    )
                    if (streak.isProductiveDayStreak) {
                        MiniStat(label = "Status", value = "Active", color = SuccessGreen)
                    } else {
                        MiniStat(label = "Status", value = "Inactive", color = TextGray)
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TextGray, fontSize = 10.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Loading / error / empty states
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AnalyticsLoadingState() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "shimmer"
    )
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = RegainTeal, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(16.dp))
            Text("Loading analytics…", color = TextGray.copy(alpha = alpha), fontSize = 14.sp)
        }
    }
}

@Composable
private fun AnalyticsErrorState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.padding(24.dp),
            colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(12.dp))
                Text("Something went wrong", color = ErrorRed, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(message, color = TextGray, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun EmptyDataView(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(message, color = TextGray.copy(alpha = 0.6f), fontSize = 13.sp)
    }
}

@Composable
private fun EmptyDataCardFull(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(message, color = TextGray.copy(alpha = 0.6f), fontSize = 13.sp, textAlign = TextAlign.Center)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Utility helpers
// ─────────────────────────────────────────────────────────────────────────────
private fun formatMinutes(totalMinutes: Int): String {
    return if (totalMinutes >= 60) {
        "${totalMinutes / 60}h ${totalMinutes % 60}m"
    } else {
        "${totalMinutes}m"
    }
}
