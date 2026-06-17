package com.example.addictionreductionapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.addictionreductionapp.data.AppDataStore
import com.example.addictionreductionapp.data.models.CoachInsight
import com.example.addictionreductionapp.data.repository.AICoachRepository
import com.google.ai.client.generativeai.GenerativeModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AICoachUiState(
    val messages: List<Pair<String, String>> = emptyList(),
    val currentInput: String = "",
    val isTyping: Boolean = false,
    val lastResponseTimestamp: Long = 0L
)

@HiltViewModel
class AICoachViewModel @Inject constructor(
    private val aiCoachRepository: AICoachRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AICoachUiState())
    val uiState: StateFlow<AICoachUiState> = _uiState.asStateFlow()

    // ── AI Coach Intelligence Engine ─────────────────────────────────────────
    // Ranked list of CoachInsight objects derived from historical trends,
    // behavior patterns, and relapse predictions.
    private val _coachInsights = MutableStateFlow<List<CoachInsight>>(emptyList())
    val coachInsights: StateFlow<List<CoachInsight>> = _coachInsights.asStateFlow()

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.0-flash",
        apiKey = "BuildConfig.GEMINI_API_KEY"
    )

    init {
        // Initialize with welcome message
        val welcomeMessage = Pair(
            "arjuna",
            "Hey ${AppDataStore.userName.value}! 🔥 I'm Arjuna, your personal focus coach. " +
            "Like the legendary archer who mastered discipline, I'm here to help you build better habits. " +
            "How can I help you today?"
        )
        _uiState.update { it.copy(messages = listOf(welcomeMessage)) }

        // Subscribe to the AI Coach Intelligence Engine
        viewModelScope.launch {
            android.util.Log.d("AICoachAudit", "Subscribing to aiCoachRepository.generateInsights()")
            aiCoachRepository.generateInsights()
                .catch { e ->
                    android.util.Log.d("AICoachAudit", "Error generating insights: ${e.message}")
                    /* silently ignore errors; insights remain empty */
                }
                .collect { insights ->
                    android.util.Log.d("AICoachAudit", "Collected insights. Size: ${insights.size}")
                    _coachInsights.value = insights
                }
        }
    }

    fun updateInput(input: String) {
        _uiState.update { it.copy(currentInput = input) }
    }

    fun sendMessage(text: String = _uiState.value.currentInput) {
        if (text.isBlank()) return
        val trimmed = text.trim()
        
        _uiState.update { currentState ->
            currentState.copy(
                messages = currentState.messages + Pair("user", trimmed),
                currentInput = "",
                isTyping = true
            )
        }

        viewModelScope.launch {
            val streak = AppDataStore.streakCount.intValue
            val sessions = AppDataStore.sessionsCompleted.intValue
            val topApp = AppDataStore.apps.firstOrNull { it.isSelected }?.name ?: "social media"

            val prompt = "You are Arjuna, a compassionate but firm digital addiction coach inside an app called SmartFocus. " +
                "The user's name is ${AppDataStore.userName.value}. " +
                "Current streak: $streak days. Sessions completed: $sessions. Most tracked app: $topApp. " +
                "Keep responses SHORT (2-3 sentences max), warm, and motivational. " +
                "Never suggest harmful behavior. If user mentions self-harm, gently recommend professional help.\n\n" +
                "User: $trimmed"

            val reply = try {
                val response = generativeModel.generateContent(prompt)
                response.text ?: "I'm here for you! Keep pushing forward. 💪"
            } catch (e: Exception) {
                "I'm having a connection issue right now. But remember — every moment of resistance builds your strength. 💪"
            }
            
            addCoachResponse(reply)
        }
    }

    fun addCoachResponse(response: String) {
        _uiState.update { currentState ->
            currentState.copy(
                messages = currentState.messages + Pair("arjuna", response),
                isTyping = false,
                lastResponseTimestamp = System.currentTimeMillis()
            )
        }
    }

    fun clearConversation() {
        val welcomeMessage = Pair(
            "arjuna",
            "Hey ${AppDataStore.userName.value}! 🔥 I'm Arjuna, your personal focus coach. " +
            "Like the legendary archer who mastered discipline, I'm here to help you build better habits. " +
            "How can I help you today?"
        )
        _uiState.update { 
            it.copy(
                messages = listOf(welcomeMessage),
                currentInput = "",
                isTyping = false,
                lastResponseTimestamp = System.currentTimeMillis()
            ) 
        }
    }
}
