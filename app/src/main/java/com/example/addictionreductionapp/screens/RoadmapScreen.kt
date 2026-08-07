package com.example.addictionreductionapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlagCircle
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.addictionreductionapp.ui.theme.*
import com.example.addictionreductionapp.viewmodel.RoadmapViewModel

@Composable
fun RoadmapScreen(
    onBack: () -> Unit,
    viewModel: RoadmapViewModel = hiltViewModel()
) {
    val plan by viewModel.plan.collectAsState()
    val reductionPlans by viewModel.reductionPlans.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(20.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextWhite)
            }
            Spacer(Modifier.width(8.dp))
            Text("Reduction Plan", color = TextWhite, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Adaptive weekly reduction targets based on your progress",
            color = TextGray,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(20.dp))

        if (reductionPlans.isNotEmpty()) {
            Text("Smart Reduction", color = RegainTeal, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            reductionPlans.forEach { plan ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(plan.category, color = TextWhite, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Day ${plan.daysActive}", color = TextGray, fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Target: ${plan.currentTarget} min · Baseline: ${plan.baselineMinutes} min",
                            color = TextGray,
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        val progress = if (plan.baselineMinutes > 0) {
                            (plan.currentTarget.toFloat() / plan.baselineMinutes.toFloat()).coerceIn(0f, 1f)
                        } else 0f
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = RegainTeal,
                            trackColor = DarkCardLight,
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        if (plan == null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FlagCircle, contentDescription = null, tint = TextGray, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No data yet", color = TextWhite, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("A reduction plan appears after 7+ days of tracking.", color = TextGray, fontSize = 13.sp)
                }
            }
        } else {
            val p = plan!!
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.TrendingDown, contentDescription = null, tint = RegainTeal)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Baseline: ${p.baselineDailyAverage} min/day", color = TextWhite, fontWeight = FontWeight.Bold)
                        Text("10% reduction per week — target your habits", color = TextGray, fontSize = 12.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Text("WEEKLY MILESTONES", color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(p.milestones) { milestone ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                Modifier.size(40.dp),
                                shape = CircleShape,
                                color = if (milestone.isComplete) SuccessGreen.copy(alpha = 0.2f) else RegainTeal.copy(alpha = 0.2f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        if (milestone.isComplete) Icons.Default.CheckCircle else Icons.Default.FlagCircle,
                                        contentDescription = null,
                                        tint = if (milestone.isComplete) SuccessGreen else RegainTeal,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Week ${milestone.weekNumber}",
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    "${milestone.startDate} - ${milestone.endDate}",
                                    color = TextGray,
                                    fontSize = 11.sp
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "${milestone.targetMinutes} min",
                                    color = RegainTeal,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Light
                                )
                                Text(
                                    "${milestone.achievedMinutes} min used",
                                    color = TextGray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
