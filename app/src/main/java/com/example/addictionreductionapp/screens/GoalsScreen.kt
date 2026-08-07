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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.addictionreductionapp.ui.theme.*
import com.example.addictionreductionapp.viewmodel.GoalsViewModel

val GOAL_TYPES = listOf(
    "LEARNING" to "Learning",
    "FITNESS" to "Fitness",
    "CAREER" to "Career",
    "CREATIVE" to "Creative",
    "CUSTOM" to "Custom"
)

@Composable
fun GoalsScreen(
    onBack: () -> Unit,
    viewModel: GoalsViewModel = hiltViewModel()
) {
    val activeGoals by viewModel.activeGoals.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(20.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextWhite)
            }
            Spacer(Modifier.width(8.dp))
            Text("My Goals", color = TextWhite, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { viewModel.showCreateDialog() }) {
                Icon(Icons.Default.Add, contentDescription = "Add Goal", tint = RegainTeal)
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            "Connect your screen-time reduction to meaningful personal goals",
            color = TextGray,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(20.dp))

        if (activeGoals.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier.padding(32.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.FlagCircle,
                        contentDescription = null,
                        tint = TextGray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("No goals yet", color = TextWhite, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Create a goal to start tracking progress",
                        color = TextGray,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.showCreateDialog() },
                        colors = ButtonDefaults.buttonColors(containerColor = RegainTeal),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Create First Goal", color = DarkBackground, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(activeGoals) { goal ->
                    GoalCard(
                        goal = goal,
                        onComplete = { viewModel.completeGoal(goal) },
                        onDelete = { viewModel.deleteGoal(goal) }
                    )
                }
            }
        }
    }

    // ── Create Goal Dialog ─────────────────────────────────────────────
    if (uiState.showCreateDialog) {
        CreateGoalDialog(
            onDismiss = { viewModel.dismissCreateDialog() },
            onCreate = { title, description, goalType, targetMinutes, targetDate ->
                viewModel.createGoal(title, description, goalType, targetMinutes, targetDate)
            }
        )
    }
}

@Composable
private fun GoalCard(
    goal: com.example.addictionreductionapp.data.local.entities.GoalEntity,
    onComplete: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val accentColor = when (goal.goalType) {
        "LEARNING", "CAREER" -> RegainTeal
        "FITNESS" -> RegainOrange
        "CREATIVE" -> RegainPurple
        else -> RegainBlue
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = accentColor.copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.FlagCircle, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(goal.title, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(goal.description, color = TextGray, fontSize = 12.sp, maxLines = 1)
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    color = accentColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        GOAL_TYPES.firstOrNull { it.first == goal.goalType }?.second ?: goal.goalType,
                        color = accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Surface(
                    color = DarkCardLight,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        "${goal.targetScreenTimePerDay}m/day target",
                        color = TextGray,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                if (goal.savedHoursTotal > 0) {
                    Surface(
                        color = SuccessGreen.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            "${goal.savedHoursTotal}h saved",
                            color = SuccessGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { goal.progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = if (goal.progress >= 0.5f) SuccessGreen else accentColor,
                trackColor = DarkCardLight,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )

            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onComplete) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Complete", color = SuccessGreen, fontSize = 12.sp)
                }
                TextButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Delete", color = ErrorRed, fontSize = 12.sp)
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = DarkCard,
            shape = RoundedCornerShape(16.dp),
            title = { Text("Delete Goal?", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = { Text("Remove \"${goal.title}\" permanently?", color = TextGray, fontSize = 14.sp) },
            confirmButton = {
                Button(
                    onClick = { showDeleteConfirm = false; onDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) { Text("Delete", color = TextWhite) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = TextGray)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateGoalDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String, Int, String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("CUSTOM") }
    var targetMinutes by remember { mutableIntStateOf(120) }
    var targetDate by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FlagCircle, contentDescription = null, tint = RegainTeal)
                Spacer(Modifier.width(8.dp))
                Text("Create Goal", color = TextWhite, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("What do you want to achieve?", color = TextGray) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RegainTeal, cursorColor = RegainTeal,
                        focusedTextColor = TextWhite, unfocusedTextColor = TextWhite,
                        unfocusedBorderColor = DarkCardLight
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GOAL_TYPES.forEach { (key, label) ->
                        FilterChip(
                            selected = selectedType == key,
                            onClick = { selectedType = key },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RegainTeal.copy(alpha = 0.2f),
                                selectedLabelColor = RegainTeal
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = if (selectedType == key) RegainTeal else DarkCardLight,
                                enabled = true, selected = selectedType == key
                            )
                        )
                    }
                }

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = TextGray, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Target: ${targetMinutes}m/day", color = TextGray, fontSize = 13.sp)
                }
                Slider(
                    value = targetMinutes.toFloat(),
                    onValueChange = { targetMinutes = it.toInt() },
                    valueRange = 15f..300f,
                    steps = 18,
                    colors = SliderDefaults.colors(thumbColor = RegainTeal, activeTrackColor = RegainTeal, inactiveTrackColor = DarkCardLight)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(title, description, selectedType, targetMinutes, targetDate.ifBlank { null }) },
                colors = ButtonDefaults.buttonColors(containerColor = RegainTeal),
                shape = RoundedCornerShape(12.dp),
                enabled = title.isNotBlank()
            ) {
                Text("Create", color = DarkBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextGray) }
        }
    )
}
