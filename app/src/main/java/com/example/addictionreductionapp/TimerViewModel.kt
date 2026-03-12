package com.example.addictionreductionapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Stopwatch-style TimerViewModel.
 * elapsedSeconds counts UP from 0 while isRunning is true.
 * Survives page navigation because it lives in the ViewModel scope.
 */
class TimerViewModel : ViewModel() {

    val elapsedSeconds = MutableStateFlow(0L)
    val isRunning = MutableStateFlow(false)

    private var timerJob: Job? = null

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
    }
}
