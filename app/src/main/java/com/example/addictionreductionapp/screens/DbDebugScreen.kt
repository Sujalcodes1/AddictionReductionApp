package com.example.addictionreductionapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.addictionreductionapp.data.local.entities.FocusSessionEntity
import com.example.addictionreductionapp.ui.theme.*
import com.example.addictionreductionapp.viewmodel.FocusSessionViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.addictionreductionapp.data.local.entities.DailyBehaviorSnapshotEntity
import com.example.addictionreductionapp.data.local.dao.DailyBehaviorSnapshotDao
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * TEMPORARY DEBUG SCREEN — Room Database Inspector
 *
 * Purpose: Verify that Room is initialised, Hilt DI is wired correctly,
 *          and that data survives a process restart.
 *
 * Usage:
 *  1. Navigate to this screen via the debug entry point in [MainActivity].
 *  2. Press "Insert Test Session" → row appears immediately in the list below.
 *  3. Open Android Studio → App Inspection → Database Inspector to confirm
 *     the row was written to `focus_sessions`.
 *
 * Remove this file and its nav-graph entry before shipping to production.
 */
@Composable
fun DbDebugScreen(
    onBack: () -> Unit,
    viewModel: FocusSessionViewModel = hiltViewModel()
) {
    // ── State ─────────────────────────────────────────────────────────────────
    val sessions by viewModel.allSessions.collectAsStateWithLifecycle()
    val totalMinutes by viewModel.totalFocusMinutes.collectAsStateWithLifecycle()
    val sessionCount by viewModel.sessionCount.collectAsStateWithLifecycle()

    // ── UI ────────────────────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .systemBarsPadding()
    ) {

        // ── Top bar ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0D1B2A))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
            }
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Default.BugReport,
                contentDescription = null,
                tint = RegainTeal,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    "Room DB Debug",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    "focus_sessions table · debug only",
                    color = TextGray,
                    fontSize = 11.sp
                )
            }
        }

        // ── Stats row ─────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF111D2B))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            DebugStat(label = "ROWS", value = "${sessionCount}")
            DebugStat(label = "TOTAL MIN", value = "${totalMinutes ?: 0}")
        }

        HorizontalDivider(color = Color(0xFF1E2D3D), thickness = 1.dp)

        // ── Action buttons ────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Insert test session
            Button(
                onClick = {
                    viewModel.recordSession(
                        durationMinutes = (5..60).random(),
                        soundType = listOf("rain", "forest", "silence", "lo-fi").random()
                    )
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = RegainTeal),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    "Insert Test Session",
                    color = DarkBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            // Delete all rows
            OutlinedButton(
                onClick = { viewModel.clearAllSessions() },
                modifier = Modifier.weight(1f),
                border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = ErrorRed,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Clear All", color = ErrorRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        val seederViewModel: DebugSeederViewModel = hiltViewModel()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { seederViewModel.seedFakeSnapshots() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = RegainOrange),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    "Seed 20 Fake Snapshots",
                    color = DarkBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        HorizontalDivider(color = Color(0xFF1E2D3D), thickness = 1.dp)

        // ── Column headers ────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0A1520))
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("id", "dur_min", "sound_type", "completed_at").forEach { col ->
                Text(
                    col,
                    color = TextGray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        HorizontalDivider(color = Color(0xFF1E2D3D), thickness = 1.dp)

        // ── Session rows ──────────────────────────────────────────────────────
        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No rows yet", color = TextGray, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Press \"Insert Test Session\" to write to Room",
                        color = TextGray.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(
                    items = sessions,
                    key = { it.id }
                ) { session ->
                    SessionDebugRow(session)
                HorizontalDivider(color = Color(0xFF1A2535), thickness = 0.5.dp)
                }
            }
        }
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun SessionDebugRow(session: FocusSessionEntity) {
    val dateStr = remember(session.completedAt) {
        SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date(session.completedAt))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        MonoText("${session.id}", color = RegainTeal)
        MonoText("${session.durationMinutes}")
        MonoText(session.soundType, color = RegainPurple)
        MonoText(dateStr, color = TextGray)
    }
}

@Composable
private fun DebugStat(label: String, value: String) {
    Column {
        Text(label, color = TextGray, fontSize = 9.sp, letterSpacing = 1.sp)
        Text(value, color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MonoText(text: String, color: Color = TextWhite) {
    Text(
        text = text,
        color = color,
        fontSize = 12.sp,
        fontFamily = FontFamily.Monospace
    )
}

// ── Missing helper — remember the formatter key ───────────────────────────────
@Composable
private fun <T> remember(key: T, calculation: () -> String): String =
    androidx.compose.runtime.remember(key) { calculation() }

@HiltViewModel
class DebugSeederViewModel @Inject constructor(
    private val snapshotDao: DailyBehaviorSnapshotDao
) : ViewModel() {

    fun seedFakeSnapshots() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val today = LocalDate.now()

            for (i in 0 until 20) {
                // i=0 is 20 days ago, i=19 is 1 day ago
                val daysAgo = 20 - i
                val date = today.minusDays(daysAgo.toLong())

                // Linearly interpolate values across the 20 days
                val focus = 100 - ((i * 60) / 19) // 100 -> 40
                val screenTime = 20 + ((i * 40) / 19) // 20 -> 60
                val risk = 0.1f + ((i * 0.8f) / 19f) // 0.1 -> 0.9

                val entity = DailyBehaviorSnapshotEntity(
                    date = date.format(formatter),
                    totalScreenTimeMinutes = screenTime,
                    totalOpens = 50 + (i * 2),
                    focusScore = focus,
                    productiveRatio = 0.8f - (i * 0.02f),
                    distractionRatio = 0.2f + (i * 0.02f),
                    appSwitches = 20 + i,
                    overallRiskScore = risk,
                    doomscrollDetected = i > 10,
                    compulsiveSwitchingDetected = i > 12,
                    lateNightUsageDetected = i > 15,
                    relapseDetected = i == 19,
                    createdAt = System.currentTimeMillis()
                )
                snapshotDao.insertSnapshot(entity)
            }
        }
    }
}
