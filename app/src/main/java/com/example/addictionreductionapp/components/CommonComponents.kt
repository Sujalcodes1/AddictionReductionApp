package com.example.addictionreductionapp.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.addictionreductionapp.ui.theme.*

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val borderBrush = remember { Brush.linearGradient(listOf(RegainTeal, RegainPurple)) }
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                clip = false,
                ambientColor = RegainTeal.copy(alpha = 0.25f),
                spotColor = RegainPurple.copy(alpha = 0.25f)
            )
            .border(
                1.dp,
                borderBrush,
                RoundedCornerShape(16.dp)
            ),
        colors = ButtonDefaults.buttonColors(
            containerColor = DarkCard,
            disabledContainerColor = DarkCard.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp
        ),
        enabled = enabled
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = RegainTeal,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(12.dp))
            }
            Text(
                text,
                color = if (enabled) TextWhite else TextGray,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                clip = false,
                ambientColor = RegainTeal.copy(alpha = 0.25f),
                spotColor = RegainTeal.copy(alpha = 0.25f)
            ),
        colors = ButtonDefaults.buttonColors(containerColor = RegainTeal),
        shape = RoundedCornerShape(16.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp
        )
    ) {
        Text(
            text,
            color = Color.Black,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AnimatedCircularProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 240.dp,
    strokeWidth: Dp = 12.dp,
    progressColor: Color = CyanAccent,
    trackColor: Color = ProgressTrack,
    content: @Composable () -> Unit = {}
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1000, easing = EaseOutCubic),
        label = "progress"
    )

    Box(contentAlignment = Alignment.Center, modifier = modifier.size(size)) {
        Canvas(modifier = Modifier.size(size - 20.dp)) {
            // Background track — solid, no gradient
            drawArc(
                color = trackColor.copy(alpha = 0.5f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
            // Progress arc — solid cyan, lag-safe (no sweepGradient)
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = animatedProgress * 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
        }
        content()
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    trend: String = "",
    accentColor: Color = CyanAccent
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = FocusCardBg),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, FocusCardStroke.copy(alpha = 0.6f))
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = FocusInputBg,
                    shape = CircleShape,
                    modifier = Modifier.size(38.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FocusInputBorder)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                if (trend.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            trend,
                            color = accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                value,
                color = TextWhite,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                label,
                color = SubtitleWhite,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 13.sp
            )
        }
    }
}

@Composable
fun HomeStatCardCompact(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    accentColor: Color = CyanAccent,
    progress: Float? = null
) {
    Card(
        modifier = modifier
            .heightIn(min = 108.dp),
        colors = CardDefaults.cardColors(containerColor = FocusCardBg),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, FocusCardStroke.copy(alpha = 0.6f))
    ) {
        Column(
            Modifier
                .padding(horizontal = 6.dp, vertical = 10.dp)
                .fillMaxWidth()
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Surface(
                color = FocusInputBg,
                shape = CircleShape,
                modifier = Modifier.size(32.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, FocusInputBorder)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(value, color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, textAlign = TextAlign.Center)
            Spacer(Modifier.height(2.dp))
            Text(
                label,
                color = SubtitleWhite,
                fontSize = 9.5.sp,
                lineHeight = 11.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                softWrap = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (progress != null) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape),
                    color = CyanAccent,
                    trackColor = ProgressTrack,
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
fun AppUsageItem(
    name: String,
    timeSpentMins: Long,
    limitMinutes: Int,
    modifier: Modifier = Modifier
) {
    val progress = (timeSpentMins.toFloat() / limitMinutes).coerceIn(0f, 1f)
    val percentage = (progress * 100).toInt()
    val barColor = when {
        progress >= 0.9f -> ErrorRed
        progress >= 0.7f -> WarningYellow
        else -> RegainTeal
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    name,
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "$percentage%",
                    color = barColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "${timeSpentMins}m / ${limitMinutes}m",
                color = TextGray,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = barColor,
                trackColor = ProgressTrack,
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun AchievementBadge(
    emoji: String,
    title: String,
    isUnlocked: Boolean,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val icon = when (emoji) {
        "Target"    -> Icons.Default.TrackChanges
        "Streak"    -> Icons.Default.LocalFireDepartment
        "Lightning" -> Icons.Default.ElectricBolt
        "Crown"     -> Icons.Default.EmojiEvents
        "Clock"     -> Icons.Default.Timer
        "Ocean"     -> Icons.Default.Pool
        "Shield"    -> Icons.Default.Shield
        "Strong"    -> Icons.Default.FitnessCenter
        "Trophy"    -> Icons.Default.MilitaryTech
        "Zen"       -> Icons.Default.SelfImprovement
        else        -> Icons.Default.Star
    }
    val iconTint = if (isUnlocked) RegainTeal else TextGray.copy(alpha = 0.4f)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) DarkCard else DarkCard.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier
                    .size(32.dp)
                    .padding(bottom = 4.dp)
            )
            Text(
                title,
                color = if (isUnlocked) TextWhite else TextGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
            Spacer(Modifier.height(8.dp))
            if (!isUnlocked) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = RegainTeal,
                    trackColor = ProgressTrack,
                    strokeCap = StrokeCap.Round
                )
            } else {
                Surface(
                    color = RegainTeal.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        "Done",
                        color = RegainTeal,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        color = TextWhite,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(vertical = 8.dp)
    )
}
