package com.example.addictionreductionapp.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.shadow
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.addictionreductionapp.TimerViewModel
import com.example.addictionreductionapp.ui.theme.*

@Composable
fun TimerScreen() {
    val startCompose = android.os.SystemClock.elapsedRealtime()
    val timerViewModel: TimerViewModel = viewModel()
    val elapsedSeconds by timerViewModel.elapsedSeconds.collectAsState()
    val isRunning by timerViewModel.isRunning.collectAsState()

    val hours = elapsedSeconds / 3600
    val minutes = (elapsedSeconds % 3600) / 60
    val seconds = elapsedSeconds % 60
    val timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds)

    Box(Modifier.fillMaxSize()
        .background(DarkBackground)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Title
            Text(
                "Stopwatch",
                color = TextGray,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 3.sp
            )

            Spacer(Modifier.height(48.dp))

            // HH:MM:SS display
            Text(
                text = timeString,
                color = Color(0xFF00BFA5),
                fontSize = 72.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-2).sp
            )

            Spacer(Modifier.height(16.dp))

            Text(
                if (isRunning) "Running..." else if (elapsedSeconds > 0L) "Paused" else "Ready",
                color = TextGray,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            )

            Spacer(Modifier.height(56.dp))

            // Start / Pause button
            Button(
                onClick = {
                    if (isRunning) timerViewModel.stopTimer()
                    else timerViewModel.startTimer()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(16.dp),
                        clip = false,
                        ambientColor = Color(0xFF00BFA5).copy(alpha = 0.5f),
                        spotColor = Color(0xFF00BFA5).copy(alpha = 0.5f)
                    ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00BFA5)
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    focusedElevation = 0.dp,
                    hoveredElevation = 0.dp
                )
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    if (isRunning) "Pause" else "Start",
                    color = Color.Black,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(16.dp))

            // Reset button
            OutlinedButton(
                onClick = { timerViewModel.resetTimer() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(16.dp),
                        clip = false,
                        ambientColor = Color(0xFF00BFA5).copy(alpha = 0.5f),
                        spotColor = Color(0xFF00BFA5).copy(alpha = 0.5f)
                    ),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color(0xFF00BFA5).copy(alpha = 0.4f)
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    focusedElevation = 0.dp,
                    hoveredElevation = 0.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = Color(0xFF00BFA5),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Reset",
                    color = Color(0xFF00BFA5),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
    androidx.compose.runtime.SideEffect {
        val duration = android.os.SystemClock.elapsedRealtime() - startCompose
        android.util.Log.d("PerfDebug", "TimerScreen composed in $duration ms")
    }
}
