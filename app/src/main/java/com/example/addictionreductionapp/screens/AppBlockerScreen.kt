package com.example.addictionreductionapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.shadow
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.addictionreductionapp.components.GradientButton
import com.example.addictionreductionapp.data.local.entities.AppLimitEntity
import com.example.addictionreductionapp.ui.theme.*
import com.example.addictionreductionapp.utils.AppCategoryResolver
import com.example.addictionreductionapp.viewmodel.AppBlockerViewModel

@Composable
fun AppBlockerScreen(
    onBack: (() -> Unit)? = null,
    viewModel: AppBlockerViewModel = hiltViewModel()
) {
    var showScheduleDialog by remember { mutableStateOf(false) }
    var scheduleApp by remember { mutableStateOf<AppLimitEntity?>(null) }

    val apps by viewModel.allApps.collectAsState()
    val reductionPlans by viewModel.activeReductionPlans.collectAsState()

    Box(Modifier.fillMaxSize()
        .background(DarkBackground)
    ) {

    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        // Header
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = TextWhite)
                }
                Spacer(Modifier.width(8.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "App Blocker",
                    color = TextWhite,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Select apps to restrict and set their daily limits",
                    color = TextGray,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Quick Block All Selected
        GradientButton(
            text = "Quick Block All Selected",
            onClick = {
                apps.filter { it.isSelected }.forEach { app ->
                    viewModel.toggleLock(app)
                }
            },
            icon = Icons.Default.Shield
        )

        Spacer(Modifier.height(16.dp))

        // App List
        LazyColumn(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(apps, key = { it.packageName }) { app ->
                val index = apps.indexOf(app)
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // App Icon Circle
                            Surface(
                                color = if (app.isSelected) RegainTeal.copy(alpha = 0.15f) else DarkCardLight,
                                shape = CircleShape,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        app.appName.first().toString(),
                                        color = if (app.isSelected) RegainTeal else TextGray,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(Modifier.weight(1f)) {
                                Text(
                                    app.appName,
                                    color = TextWhite,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                val isReductionManaged = reductionPlans.any {
                                    AppCategoryResolver.resolveCategory(app.packageName) == it.category && it.isActive
                                }
                                if (app.isSelected) {
                                    Text(
                                        "Limit: ${app.limitMinutes}m/day" +
                                                if (app.blockScheduleStart >= 0) " • Scheduled ${app.blockScheduleStart}:00-${app.blockScheduleEnd}:00" else "",
                                        color = RegainTeal,
                                        fontSize = 11.sp
                                    )
                                }
                                if (isReductionManaged) {
                                    Spacer(Modifier.height(2.dp))
                                    Surface(
                                        color = RegainPurple.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            "Smart Reduction",
                                            color = RegainPurple,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Switch(
                                checked = app.isSelected,
                                onCheckedChange = {
                                    viewModel.toggleSelection(app)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = RegainTeal,
                                    checkedTrackColor = RegainTeal.copy(alpha = 0.3f),
                                    uncheckedThumbColor = TextGray,
                                    uncheckedTrackColor = DarkCardLight
                                )
                            )
                        }

                        if (app.isSelected) {
                            Spacer(Modifier.height(12.dp))

                            // Limit Slider
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = TextGray,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Daily Limit",
                                    color = TextGray,
                                    fontSize = 12.sp
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    "${app.limitMinutes} min",
                                    color = RegainTeal,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = app.limitMinutes.toFloat(),
                                onValueChange = {
                                    viewModel.updateLimit(app, it.toInt())
                                },
                                valueRange = 1f..180f,
                                colors = SliderDefaults.colors(
                                    thumbColor = RegainTeal,
                                    activeTrackColor = RegainTeal,
                                    inactiveTrackColor = DarkCardLight
                                )
                            )

                            // Schedule & Whitelist buttons
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            scheduleApp = app
                                            showScheduleDialog = true
                                        },
                                    color = DarkCardLight,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = TextGray,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "Schedule",
                                            color = TextGray,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            viewModel.toggleWhitelist(app)
                                        },
                                    color = if (app.isWhitelisted) RegainTeal.copy(alpha = 0.15f) else DarkCardLight,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            if (app.isWhitelisted) Icons.Default.CheckCircle else Icons.Default.AddCircleOutline,
                                            contentDescription = null,
                                            tint = if (app.isWhitelisted) RegainTeal else TextGray,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "Whitelist",
                                            color = if (app.isWhitelisted) RegainTeal else TextGray,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Save button
        Button(
            onClick = { onBack?.invoke() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(16.dp),
                    clip = false,
                    ambientColor = RegainTeal.copy(alpha = 0.5f),
                    spotColor = RegainTeal.copy(alpha = 0.5f)
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
                "Save & Close",
                color = DarkBackground,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
    } // Close Box

    // Schedule Dialog
    if (showScheduleDialog && scheduleApp != null) {
        val app = scheduleApp!!
        ScheduleDialog(
            appName = app.appName,
            currentStart = app.blockScheduleStart,
            currentEnd = app.blockScheduleEnd,
            onDismiss = { showScheduleDialog = false },
            onSave = { start, end ->
                viewModel.setSchedule(app, start, end)
                showScheduleDialog = false
            }
        )
    }
}

@Composable
fun ScheduleDialog(
    appName: String,
    currentStart: Int,
    currentEnd: Int,
    onDismiss: () -> Unit,
    onSave: (Int, Int) -> Unit
) {
    var startHour by remember { mutableIntStateOf(if (currentStart >= 0) currentStart else 9) }
    var endHour by remember { mutableIntStateOf(if (currentEnd >= 0) currentEnd else 17) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Block Schedule",
                color = TextWhite,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    "Set automatic blocking hours for $appName",
                    color = TextGray,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(24.dp))

                Text("Start: ${startHour}:00", color = TextWhite, fontWeight = FontWeight.SemiBold)
                Slider(
                    value = startHour.toFloat(),
                    onValueChange = { startHour = it.toInt() },
                    valueRange = 0f..23f,
                    steps = 22,
                    colors = SliderDefaults.colors(
                        thumbColor = RegainTeal,
                        activeTrackColor = RegainTeal
                    )
                )

                Spacer(Modifier.height(8.dp))

                Text("End: ${endHour}:00", color = TextWhite, fontWeight = FontWeight.SemiBold)
                Slider(
                    value = endHour.toFloat(),
                    onValueChange = { endHour = it.toInt() },
                    valueRange = 0f..23f,
                    steps = 22,
                    colors = SliderDefaults.colors(
                        thumbColor = RegainTeal,
                        activeTrackColor = RegainTeal
                    )
                )

                Spacer(Modifier.height(8.dp))

                TextButton(onClick = { onSave(-1, -1) }) {
                    Text("Remove Schedule", color = ErrorRed, fontSize = 14.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(startHour, endHour) },
                colors = ButtonDefaults.buttonColors(containerColor = RegainTeal),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save", color = DarkBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextGray)
            }
        },
        containerColor = DarkCard,
        shape = RoundedCornerShape(20.dp)
    )
}
