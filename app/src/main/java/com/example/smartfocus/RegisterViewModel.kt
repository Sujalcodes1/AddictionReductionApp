package com.example.smartfocus

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.addictionreductionapp.data.repository.AuthRepository
import com.example.addictionreductionapp.utils.AuthResult
import com.example.addictionreductionapp.utils.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    data class RegisterResult(
        val isSuccess: Boolean,
        val errorMessage: String? = null
    )

    private val _registerResult = MutableLiveData<RegisterResult>()
    val registerResult: LiveData<RegisterResult> = _registerResult

    fun register(name: String, email: String, password: String) {
        viewModelScope.launch {
            if (name.isBlank() || email.isBlank() || password.isBlank()) {
                _registerResult.postValue(RegisterResult(isSuccess = false, errorMessage = "Fields cannot be empty"))
                return@launch
            }

            when (val result = authRepository.register(name, email, password)) {
                is AuthResult.Success -> {
                    _registerResult.postValue(RegisterResult(isSuccess = true))
                }
                is AuthResult.EmailConfirmationRequired -> {
                    // For dev/offline fallback, sign in immediately
                    sessionManager.saveSession("dev_local_session_$email")
                    _registerResult.postValue(RegisterResult(isSuccess = true))
                }
                is AuthResult.Failure -> {
                    _registerResult.postValue(RegisterResult(isSuccess = false, errorMessage = result.message))
                }
            }
        }
    }
}
