package com.example.addictionreductionapp.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.addictionreductionapp.viewmodel.AICoachViewModel
import kotlinx.coroutines.launch

@Composable
fun AICoachScreen(
    viewModel: AICoachViewModel = hiltViewModel()
) {
    val startCompose = android.os.SystemClock.elapsedRealtime()
    
    val uiState by viewModel.uiState.collectAsState()
    val coachInsights by viewModel.coachInsights.collectAsState()

    var showReportDialog by remember { mutableStateOf(false) }
    var reportMessageIndex by remember { mutableIntStateOf(-1) }
    var reportReason by remember { mutableStateOf("") }
    
    val listState = rememberLazyListState()

    val quickReplies = listOf(
        "I'm struggling", "Give me a tip", "What's my progress?", "I relapsed", "Motivate me"
    )

    // ── Lifecycle probe: fires every time this composable enters/leaves composition
    DisposableEffect(Unit) {
        android.util.Log.d("NavDebug", "AICoachScreen ENTERED composition (messages=${uiState.messages.size})")
        onDispose {
            android.util.Log.d("NavDebug", "AICoachScreen LEFT composition — messages state preserved in ViewModel")
        }
    }

    // ── Recomposition probe
    SideEffect {
        android.util.Log.d("NavDebug", "AICoachScreen RECOMPOSED (messages.size=${uiState.messages.size})")
    }

    LaunchedEffect(uiState.messages.size, uiState.isTyping) {
        val totalItems = uiState.messages.size + if (uiState.isTyping) 1 else 0
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ── Header ───────────────────────────────────────────────
        Box(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F171E))
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .background(Color(0xFF00BFA5).copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🔥", fontSize = 22.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Arjuna",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (uiState.rateLimitReached) "Limit reached" else "AI Focus Coach • Online",
                        color = if (uiState.rateLimitReached) Color(0xFFFF9800) else Color(0xFF00BFA5),
                        fontSize = 12.sp
                    )
                }
                if (uiState.messages.size > 1) {
                    TextButton(
                        onClick = { viewModel.clearConversation() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "Clear",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // ── Insight Cards ─────────────────────────────────────────
        if (coachInsights.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 12.dp)
            ) {
                items(coachInsights.take(3)) { insight ->
                    val accent = when (insight.priority) {
                        com.example.addictionreductionapp.data.models.CoachPriority.CRITICAL -> Color(0xFFFF4444)
                        com.example.addictionreductionapp.data.models.CoachPriority.HIGH -> Color(0xFFFF9800)
                        com.example.addictionreductionapp.data.models.CoachPriority.MEDIUM -> Color(0xFFFFEB3B)
                        com.example.addictionreductionapp.data.models.CoachPriority.LOW -> Color(0xFF00BFA5)
                    }
                    Box(
                        modifier = Modifier
                            .width(200.dp)
                            .background(accent.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                insight.title,
                                color = accent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                insight.description,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }

        // ── Messages ──────────────────────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(uiState.messages.withIndex().toList()) { (index, pair) ->
                val sender = pair.first
                val text = pair.second
                val isUser = sender == "user"
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
                        Box(
                            Modifier
                                .widthIn(max = 280.dp)
                                .background(
                                    if (isUser) Color(0xFF00BFA5).copy(alpha = 0.2f)
                                    else Color(0xFF0F171E),
                                    RoundedCornerShape(
                                        topStart = 16.dp, topEnd = 16.dp,
                                        bottomStart = if (isUser) 16.dp else 4.dp,
                                        bottomEnd = if (isUser) 4.dp else 16.dp
                                    )
                                )
                                .border(
                                    1.dp,
                                    if (isUser) Color(0xFF00BFA5).copy(alpha = 0.4f)
                                    else Color(0xFF1B262F),
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Text(text, color = Color.White, fontSize = 14.sp, lineHeight = 20.sp)
                        }
                        if (!isUser) {
                            Spacer(Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                Icon(
                                    Icons.Default.WarningAmber,
                                    contentDescription = "AI-generated",
                                    tint = Color(0xFFFF9800).copy(alpha = 0.7f),
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    "AI-generated",
                                    color = Color(0xFFFF9800).copy(alpha = 0.6f),
                                    fontSize = 9.sp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Report",
                                    color = Color.Gray,
                                    fontSize = 9.sp,
                                    modifier = Modifier.clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) {
                                        reportMessageIndex = index
                                        showReportDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (uiState.isTyping) {
                item {
                    Row {
                        Box(
                            Modifier
                                .background(Color(0xFF0F171E), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text("Arjuna is thinking...", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // ── Quick Replies ─────────────────────────────────────────
        LazyRow(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickReplies) { reply ->
                OutlinedButton(
                    onClick = { viewModel.sendMessage(reply) },
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFF00BFA5).copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(reply, color = Color(0xFF00BFA5), fontSize = 12.sp)
                }
            }
        }

        // ── Input Row ─────────────────────────────────────────────
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = uiState.currentInput,
                onValueChange = { viewModel.updateInput(it) },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask Arjuna...", color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00BFA5),
                    unfocusedBorderColor = Color(0xFF1B262F),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFF00BFA5)
                ),
                shape = RoundedCornerShape(24.dp),
                maxLines = 3
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = { viewModel.sendMessage() },
                modifier = Modifier
                    .size(48.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = CircleShape,
                        clip = false,
                        ambientColor = Color(0xFF00BFA5).copy(alpha = 0.5f),
                        spotColor = Color(0xFF00BFA5).copy(alpha = 0.5f)
                    )
                    .background(Color(0xFF00BFA5), CircleShape)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.Black)
            }
        }
    }

    // ── Report AI Response Dialog ────────────────────────────────────────
    if (showReportDialog) {
        val reportOptions = listOf("Offensive or inappropriate", "Harmful advice", "Inaccurate or misleading", "Spam or irrelevant", "Other")
        AlertDialog(
            onDismissRequest = { showReportDialog = false; reportReason = "" },
            containerColor = Color(0xFF0F171E),
            shape = RoundedCornerShape(16.dp),
            title = { Text("Report AI Response", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column {
                    Text(
                        "Why are you reporting this response?",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    reportOptions.forEach { option ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { reportReason = option }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = reportReason == option,
                                onClick = { reportReason = option },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00BFA5))
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(option, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        android.util.Log.w("AICoachAudit", "AI response reported: reason=$reportReason, messageIndex=$reportMessageIndex")
                        showReportDialog = false
                        reportReason = ""
                        reportMessageIndex = -1
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BFA5)),
                    shape = RoundedCornerShape(8.dp),
                    enabled = reportReason.isNotEmpty()
                ) {
                    Text("Submit Report", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false; reportReason = "" }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    SideEffect {
        val duration = android.os.SystemClock.elapsedRealtime() - startCompose
        android.util.Log.d("PerfDebug", "AICoachScreen composed in $duration ms")
    }
}
