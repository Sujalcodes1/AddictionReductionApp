package com.example.addictionreductionapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.addictionreductionapp.repository.AuthRepository
import com.example.addictionreductionapp.utils.AuthResult
import com.example.addictionreductionapp.utils.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun checkSession() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            _authState.value = if (authRepository.currentSession() != null) {
                AuthState.Authenticated
            } else {
                AuthState.Unauthenticated
            }
        }
    }

    fun register(name: String, email: String, password: String, onResult: (AuthResult) -> Unit) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.register(name, email, password)
            _authState.value = when (result) {
                is AuthResult.Success -> AuthState.Authenticated
                // Email confirmation required — user is not yet authenticated locally;
                // they must click the confirmation link first.
                is AuthResult.EmailConfirmationRequired -> AuthState.Unauthenticated
                is AuthResult.Failure -> AuthState.Error(result.message)
            }
            onResult(result)
        }
    }

    fun login(email: String, password: String, onResult: (AuthResult) -> Unit) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.login(email, password)
            _authState.value = when (result) {
                is AuthResult.Success -> AuthState.Authenticated
                is AuthResult.EmailConfirmationRequired -> AuthState.Unauthenticated
                is AuthResult.Failure -> AuthState.Error(result.message)
            }
            onResult(result)
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun handleDeepLink(uri: String, onResult: (AuthResult) -> Unit) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.handleAuthCallback(uri)
            _authState.value = when (result) {
                is AuthResult.Success -> AuthState.Authenticated
                is AuthResult.EmailConfirmationRequired -> AuthState.Unauthenticated
                is AuthResult.Failure -> AuthState.Error(result.message)
            }
            onResult(result)
        }
    }
}
