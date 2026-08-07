package com.example.addictionreductionapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.addictionreductionapp.data.local.entities.ChatMessageEntity
import com.example.addictionreductionapp.data.models.CoachInsight
import com.example.addictionreductionapp.data.models.RoadmapPlan
import com.example.addictionreductionapp.data.repository.AICoachRepository
import com.example.addictionreductionapp.data.repository.AdaptiveReductionRepository
import com.example.addictionreductionapp.data.repository.AppLimitRepository
import com.example.addictionreductionapp.data.repository.InterventionRepository
import com.example.addictionreductionapp.data.repository.UserProfileRepository
import com.example.addictionreductionapp.data.repository.ChatMessageRepository
import com.example.addictionreductionapp.data.ai.CoachContextBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import javax.inject.Inject

// ── Supabase Edge Function Response Model ──────────────────────────────────
data class AiCoachResponse(val reply: String, val safetyFlags: List<String> = emptyList())

data class AICoachUiState(
    val messages: List<Pair<String, String>> = emptyList(),
    val currentInput: String = "",
    val isTyping: Boolean = false,
    val lastResponseTimestamp: Long = 0L,
    val messageCountThisSession: Int = 0,
    val rateLimitReached: Boolean = false
)

private const val MAX_MESSAGES_PER_SESSION = 10

@HiltViewModel
class AICoachViewModel @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val aiCoachRepository: AICoachRepository,
    private val appLimitRepository: AppLimitRepository,
    private val adaptiveReductionRepository: AdaptiveReductionRepository,
    private val userProfileRepository: UserProfileRepository,
    private val interventionRepository: InterventionRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val coachContextBuilder: CoachContextBuilder
) : ViewModel() {

    private val _uiState = MutableStateFlow(AICoachUiState())
    val uiState: StateFlow<AICoachUiState> = _uiState.asStateFlow()

    // ── AI Coach Intelligence Engine ─────────────────────────────────────────
    // Ranked list of CoachInsight objects derived from historical trends,
    // behavior patterns, and relapse predictions.
    private val _coachInsights = MutableStateFlow<List<CoachInsight>>(emptyList())
    val coachInsights: StateFlow<List<CoachInsight>> = _coachInsights.asStateFlow()

    private val _roadmapPlan = MutableStateFlow<RoadmapPlan?>(null)

    init {
        viewModelScope.launch {
            val savedMessages = chatMessageRepository.getRecentOnce(20)
            if (savedMessages.isNotEmpty()) {
                val pairs = savedMessages.map { it.sender to it.text }
                _uiState.update { it.copy(messages = pairs) }
            } else {
                val welcomeMessage = Pair(
                    "arjuna",
                    "Hey ${getUserName()}! 🔥 I'm Arjuna, your personal focus coach. " +
                    "Like the legendary archer who mastered discipline, I'm here to help you build better habits. " +
                    "How can I help you today?"
                )
                _uiState.update { it.copy(messages = listOf(welcomeMessage)) }
            }
        }

        // Subscribe to the AI Coach Intelligence Engine
        viewModelScope.launch {
            try {
                android.util.Log.d("AICoachAudit", "Subscribing to aiCoachRepository.generateInsights()")
                aiCoachRepository.generateInsights()
                    .catch { e ->
                        android.util.Log.e("AICoachAudit", "Error generating insights: ${e.message}", e)
                        /* silently ignore errors; insights remain empty */
                    }
                    .collect { insights ->
                        android.util.Log.d("AICoachAudit", "Collected insights. Size: ${insights.size}")
                        _coachInsights.value = insights
                    }
            } catch (e: Exception) {
                android.util.Log.e("AICoachAudit", "Fatal error in insight subscription: ${e.message}", e)
            }
        }

        // Subscribe to the Adaptive Reduction Roadmap
        viewModelScope.launch {
            try {
                adaptiveReductionRepository.generateRoadmap()
                    .catch { e ->
                        android.util.Log.e("AICoachAudit", "Error generating roadmap: ${e.message}", e)
                    }
                    .collect { plan ->
                        _roadmapPlan.value = plan
                    }
            } catch (e: Exception) {
                android.util.Log.e("AICoachAudit", "Fatal error in roadmap subscription: ${e.message}", e)
            }
        }
    }

    fun updateInput(input: String) {
        _uiState.update { it.copy(currentInput = input) }
    }

    fun sendMessage(text: String = _uiState.value.currentInput) {
        if (text.isBlank()) return
        val trimmed = text.trim()

        val state = _uiState.value
        if (state.rateLimitReached) {
            addCoachResponse("Let's check in again tomorrow. You've asked great questions today! 🌟")
            return
        }

        val newCount = state.messageCountThisSession + 1
        val reached = newCount >= MAX_MESSAGES_PER_SESSION

        _uiState.update { currentState ->
            currentState.copy(
                messages = currentState.messages + Pair("user", trimmed),
                currentInput = "",
                isTyping = true,
                messageCountThisSession = newCount,
                rateLimitReached = reached
            )
        }

        viewModelScope.launch {
            chatMessageRepository.insert(ChatMessageEntity(sender = "user", text = trimmed))

            val prompt = try {
                coachContextBuilder.buildFullContext(trimmed)
            } catch (e: Exception) {
                android.util.Log.e("AICoachAudit", "Context builder failed: ${e.message}", e)
                "You are Arjuna, a digital wellness coach. Keep it short and encouraging. User: $trimmed"
            }

            val reply = try {
                callSupabaseAICoach(prompt)
            } catch (e: Exception) {
                android.util.Log.e("AICoachAudit", "AI coach call failed: ${e.message}", e)
                "I'm having a connection issue right now. But remember — every moment of resistance builds your strength. 💪"
            }

            addCoachResponse(reply)
        }
    }

    private suspend fun callSupabaseAICoach(prompt: String): String = withContext(Dispatchers.IO) {
        try {
            val httpResponse = supabaseClient.functions.invoke("ai-coach", mapOf("prompt" to prompt))
            val json = httpResponse.body<String>()
            val responseObj = com.google.gson.Gson().fromJson(json, AiCoachResponse::class.java)
            if (responseObj.safetyFlags.isNotEmpty()) {
                android.util.Log.w("AICoachAudit", "Safety flags in response: ${responseObj.safetyFlags}")
            }
            responseObj.reply.ifBlank { "I'm here for you! Keep pushing forward." }
        } catch (e: Exception) {
            android.util.Log.e("AICoachAudit", "Supabase function call failed: ${e.message}", e)
            "I'm having a connection issue right now. But remember — every moment of resistance builds your strength."
        }
    }

    private suspend fun getUserName(): String {
        return userProfileRepository.getProfile()?.userName ?: "User"
    }

    fun addCoachResponse(response: String) {
        _uiState.update { currentState ->
            currentState.copy(
                messages = currentState.messages + Pair("arjuna", response),
                isTyping = false,
                lastResponseTimestamp = System.currentTimeMillis()
            )
        }
        viewModelScope.launch {
            chatMessageRepository.insert(ChatMessageEntity(sender = "arjuna", text = response))
        }
    }

    fun clearConversation() {
        viewModelScope.launch {
            val welcomeMessage = Pair(
                "arjuna",
                "Hey ${getUserName()}! 🔥 I'm Arjuna, your personal focus coach. " +
                "Like the legendary archer who mastered discipline, I'm here to help you build better habits. " +
                "How can I help you today?"
            )
            _uiState.update { 
                it.copy(
                    messages = listOf(welcomeMessage),
                    currentInput = "",
                    isTyping = false,
                    lastResponseTimestamp = System.currentTimeMillis(),
                    messageCountThisSession = 0,
                    rateLimitReached = false
                ) 
            }
            chatMessageRepository.deleteAll()
        }
    }
}
