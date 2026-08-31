package com.example.smartfocus

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.addictionreductionapp.data.repository.AuthRepository
import com.example.addictionreductionapp.utils.AuthResult
import com.example.addictionreductionapp.utils.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    data class LoginResult(
        val isSuccess: Boolean,
        val errorMessage: String? = null
    )

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult

    fun login(email: String, password: String) {
        viewModelScope.launch {
            if (email.isBlank() || password.isBlank()) {
                _loginResult.postValue(LoginResult(isSuccess = false, errorMessage = "Fields cannot be empty"))
                return@launch
            }
            
            when (val result = authRepository.login(email, password)) {
                is AuthResult.Success -> {
                    _loginResult.postValue(LoginResult(isSuccess = true))
                }
                is AuthResult.Failure -> {
                    _loginResult.postValue(LoginResult(isSuccess = false, errorMessage = result.message))
                }
                else -> {
                    // Fallback to local session if unknown result
                    sessionManager.saveSession("dev_local_session_$email")
                    _loginResult.postValue(LoginResult(isSuccess = true))
                }
            }
        }
    }
}

