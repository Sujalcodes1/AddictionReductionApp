package com.example.addictionreductionapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Stopwatch-style ViewModel for the Focus Timer screen.
 *
 * [elapsedSeconds] counts UP from 0 while [isRunning] is true.
 * Survives configuration changes because it lives in the ViewModel scope.
 *
 * Annotated with [@HiltViewModel] so Compose can obtain an instance via:
 * ```kotlin
 * val vm: TimerViewModel = hiltViewModel()
 * ```
 * No repository dependency is needed here — the pure timer state is ephemeral
 * and intentionally not persisted to Room during an active session.
 * Persisting is done by [com.example.addictionreductionapp.viewmodel.FocusSessionViewModel.recordSession]
 * once the user stops or completes the timer.
 */
@HiltViewModel
class TimerViewModel @Inject constructor() : ViewModel() {

    val elapsedSeconds = MutableStateFlow(0L)
    val isRunning = MutableStateFlow(false)

    private var timerJob: Job? = null

    init {
        android.util.Log.d("NavDebug", "TimerViewModel INITIALIZED (hashCode=${hashCode()})")
    }

    fun startTimer() {
        if (isRunning.value) return
        isRunning.value = true
        timerJob = viewModelScope.launch {
            while (isRunning.value) {
                delay(1000)
                if (isRunning.value) {
                    elapsedSeconds.value++
                }
            }
        }
    }

    fun stopTimer() {
        isRunning.value = false
        timerJob?.cancel()
    }

    fun resetTimer() {
        stopTimer()
        elapsedSeconds.value = 0L
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        android.util.Log.d("NavDebug", "TimerViewModel CLEARED (hashCode=${hashCode()})")
    }
}
