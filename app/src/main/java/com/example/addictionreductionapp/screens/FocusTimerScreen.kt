package com.example.addictionreductionapp.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.addictionreductionapp.components.AnimatedCircularProgress
import com.example.addictionreductionapp.data.AppDataStore
import com.example.addictionreductionapp.ui.theme.*
import kotlinx.coroutines.delay

data class AmbientSound(
    val name: String,
    val emoji: String,
    val isSelected: Boolean = false
)

@Composable
fun FocusTimerScreen() {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Timer state
    var selectedDuration by remember { mutableIntStateOf(25) } // minutes
    var isTimerRunning by remember { mutableStateOf(false) }
    var remainingSeconds by remember { mutableIntStateOf(25 * 60) }
    var isLockMode by remember { mutableStateOf(false) }
    var showComplete by remember { mutableStateOf(false) }
    var selectedSoundIndex by remember { mutableIntStateOf(4) } // Silence by default

    val durations = listOf(15, 25, 45, 60, 90, 120)
    val sounds = listOf(
        AmbientSound("Rain", "🌧️"),
        AmbientSound("Forest", "🌲"),
        AmbientSound("Café", "☕"),
        AmbientSound("Lo-fi", "🎵"),
        AmbientSound("Silence", "🔇")
    )

    // Timer logic
    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            while (remainingSeconds > 0 && isTimerRunning) {
                delay(1000)
                remainingSeconds--
            }
            if (remainingSeconds <= 0 && isTimerRunning) {
                isTimerRunning = false
                showComplete = true
                AppDataStore.completeFocusSession(context, selectedDuration)
                AppDataStore.incrementStreak(context)
            }
        }
    }

    if (showComplete) {
        SessionCompleteScreen(
            duration = selectedDuration,
            onDismiss = {
                showComplete = false
                remainingSeconds = selectedDuration * 60
            }
        )
        return
    }

    Box(Modifier.fillMaxSize()) {
        // Unique gradient background
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        DarkBackground,
                        Color(0xFF03080F),
                        Color(0xFF081420),
                        Color(0xFF03080F),
                        DarkBackground
                    )
                )
            )
            // Soft teal accent glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        RegainTeal.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    radius = size.width * 0.7f
                ),
                radius = size.width * 0.7f,
                center = Offset(size.width * 0.5f, size.height * 0.3f)
            )
        }

    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Focus Timer",
                color = TextWhite,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Surface(
                color = if (isLockMode) RegainTeal.copy(alpha = 0.2f) else DarkCard,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.clickable { if (!isTimerRunning) isLockMode = !isLockMode }
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isLockMode) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = if (isLockMode) RegainTeal else TextGray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Lock",
                        color = if (isLockMode) RegainTeal else TextGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Text(
            "Stay focused. Block distractions.",
            color = TextGray,
            fontSize = 14.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(40.dp))

        // Timer Circle
        val totalSeconds = selectedDuration * 60
        val progress = if (isTimerRunning || remainingSeconds < totalSeconds) {
            remainingSeconds.toFloat() / totalSeconds.toFloat()
        } else 1f

        AnimatedCircularProgress(
            progress = progress,
            size = 260.dp,
            strokeWidth = 10.dp
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val mins = remainingSeconds / 60
                val secs = remainingSeconds % 60
                Text(
                    String.format("%02d:%02d", mins, secs),
                    color = TextWhite,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Light
                )
                Text(
                    if (isTimerRunning) "focusing..." else "ready",
                    color = RegainTeal,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // Duration Presets
        if (!isTimerRunning) {
            Text(
                "DURATION",
                color = TextGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                durations.forEach { mins ->
                    val isSelected = selectedDuration == mins
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedDuration = mins
                                remainingSeconds = mins * 60
                            },
                        color = if (isSelected) RegainTeal.copy(alpha = 0.15f) else DarkCard,
                        shape = RoundedCornerShape(12.dp),
                        border = if (isSelected) {
                            androidx.compose.foundation.BorderStroke(1.dp, RegainTeal)
                        } else null
                    ) {
                        Text(
                            "${mins}m",
                            color = if (isSelected) RegainTeal else TextGray,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Ambient Sounds
            Text(
                "AMBIENT SOUND",
                color = TextGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sounds.forEachIndexed { index, sound ->
                    val isSelected = selectedSoundIndex == index
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedSoundIndex = index },
                        color = if (isSelected) RegainTeal.copy(alpha = 0.15f) else DarkCard,
                        shape = RoundedCornerShape(12.dp),
                        border = if (isSelected) {
                            androidx.compose.foundation.BorderStroke(1.dp, RegainTeal)
                        } else null
                    ) {
                        Column(
                            Modifier.padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(sound.emoji, fontSize = 20.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                sound.name,
                                color = if (isSelected) RegainTeal else TextGray,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // Control Buttons
        if (isTimerRunning) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!isLockMode) {
                    OutlinedButton(
                        onClick = {
                            isTimerRunning = false
                            remainingSeconds = selectedDuration * 60
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, tint = ErrorRed)
                        Spacer(Modifier.width(8.dp))
                        Text("Stop", color = ErrorRed, fontWeight = FontWeight.Bold)
                    }
                }
                Button(
                    onClick = { isTimerRunning = false },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Pause, contentDescription = null, tint = RegainTeal)
                    Spacer(Modifier.width(8.dp))
                    Text("Pause", color = TextWhite, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Button(
                onClick = {
                    if (remainingSeconds == 0) remainingSeconds = selectedDuration * 60
                    isTimerRunning = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .border(
                        1.dp,
                        Brush.linearGradient(listOf(RegainTeal, RegainPurple)),
                        RoundedCornerShape(16.dp)
                    ),
                colors = ButtonDefaults.buttonColors(containerColor = DarkCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = RegainTeal,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "Start Focus Session",
                    color = TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Lock mode info
        if (isLockMode && isTimerRunning) {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = RegainTeal)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Lock Mode is ON. You cannot stop the timer until it finishes. Stay focused!",
                        color = TextGray,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
    } // Close Box
}

@Composable
fun SessionCompleteScreen(duration: Int, onDismiss: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "celebrate")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text("🎉", fontSize = 72.sp)
            Spacer(Modifier.height(24.dp))
            Text(
                "Session Complete!",
                color = TextWhite,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "You focused for $duration minutes",
                color = TextGray,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(32.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("⏱️", fontSize = 24.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("$duration min", color = TextWhite, fontWeight = FontWeight.Bold)
                        Text("Duration", color = TextGray, fontSize = 11.sp)
                    }
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🔥", fontSize = 24.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${AppDataStore.streakCount.intValue}",
                            color = TextWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Streak", color = TextGray, fontSize = 11.sp)
                    }
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("✅", fontSize = 24.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${AppDataStore.sessionsCompleted.intValue}",
                            color = TextWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Total", color = TextGray, fontSize = 11.sp)
                    }
                }
            }

            Spacer(Modifier.height(40.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RegainTeal),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    "Continue",
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
