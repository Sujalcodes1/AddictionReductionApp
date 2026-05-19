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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.addictionreductionapp.data.AppDataStore
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.launch

@Composable
fun AICoachScreen() {
    val coroutineScope = rememberCoroutineScope()

    var userInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val messages = remember {
        mutableStateListOf(
            Pair(
                "arjuna",
                "Hey ${AppDataStore.userName.value}! 🔥 I'm Arjuna, your personal focus coach. " +
                "Like the legendary archer who mastered discipline, I'm here to help you build better habits. " +
                "How can I help you today?"
            )
        )
    }
    val listState = rememberLazyListState()

    val quickReplies = listOf(
        "I'm struggling", "Give me a tip", "What's my progress?", "I relapsed", "Motivate me"
    )

    // Official Gemini SDK — no manual HTTP, no JSON issues
    val generativeModel = remember {
        GenerativeModel(
            modelName = "gemini-2.0-flash",
            apiKey = "AIzaSyBksSkcOqTlwvvEK04DkzB2a445fzD8ZZI"
        )
    }

    suspend fun sendToGemini(userMessage: String): String {
        val streak = AppDataStore.streakCount.intValue
        val sessions = AppDataStore.sessionsCompleted.intValue
        val topApp = AppDataStore.apps.firstOrNull { it.isSelected }?.name ?: "social media"

        val prompt = "You are Arjuna, a compassionate but firm digital addiction coach inside an app called SmartFocus. " +
            "The user's name is ${AppDataStore.userName.value}. " +
            "Current streak: $streak days. Sessions completed: $sessions. Most tracked app: $topApp. " +
            "Keep responses SHORT (2-3 sentences max), warm, and motivational. " +
            "Never suggest harmful behavior. If user mentions self-harm, gently recommend professional help.\n\n" +
            "User: $userMessage"

        return try {
            val response = generativeModel.generateContent(prompt)
            response.text ?: "I'm here for you! Keep pushing forward. 💪"
        } catch (e: Exception) {
            "I'm having a connection issue right now. But remember — every moment of resistance builds your strength. 💪"
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val trimmed = text.trim()
        messages.add(Pair("user", trimmed))
        userInput = ""
        isLoading = true
        coroutineScope.launch {
            val reply = sendToGemini(trimmed)
            messages.add(Pair("arjuna", reply))
            isLoading = false
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.size - 1)
            }
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
            items(messages) { (sender, text) ->
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

            if (isLoading) {
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
                    onClick = { sendMessage(reply) },
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
                value = userInput,
                onValueChange = { userInput = it },
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
                onClick = { sendMessage(userInput) },
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF00BFA5), CircleShape)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.Black)
            }
        }
    }
}
