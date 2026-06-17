package com.example.addictionreductionapp.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(44.dp)
                        .background(Color(0xFF00BFA5).copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🔥", fontSize = 22.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Arjuna",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "AI Focus Coach • Online",
                        color = Color(0xFF00BFA5),
                        fontSize = 12.sp
                    )
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
            items(uiState.messages) { (sender, text) ->
                val isUser = sender == "user"
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
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
    SideEffect {
        val duration = android.os.SystemClock.elapsedRealtime() - startCompose
        android.util.Log.d("PerfDebug", "AICoachScreen composed in $duration ms")
    }
}
