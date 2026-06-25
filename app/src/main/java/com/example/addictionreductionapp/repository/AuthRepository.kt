package com.example.addictionreductionapp.repository

import com.example.addictionreductionapp.utils.AuthResult
import com.example.addictionreductionapp.utils.SessionManager
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserInfo
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.ConnectException
import java.net.UnknownHostException
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val sessionManager: SessionManager
) {

    // ── Register ──────────────────────────────────────────────────────────────────
    suspend fun register(name: String, email: String, password: String): AuthResult =
        withContext(Dispatchers.IO) {
            try {
                supabaseClient.auth.signUpWith(Email, redirectUrl = "smartfocus://login-callback") {
                    this.email = email
                    this.password = password
                }
                val session = supabaseClient.auth.currentSessionOrNull()
                if (session != null) {
                    // Email confirmation disabled — session returned immediately
                    sessionManager.saveSession("active_session")
                    AuthResult.Success
                } else {
                    // Email confirmation enabled (Supabase default) — this is NOT an error.
                    // The user must verify their email before a session is created.
                    AuthResult.EmailConfirmationRequired
                }
            } catch (e: Exception) {
                AuthResult.Failure(mapException(e))
            }
        }

    // ── Login ─────────────────────────────────────────────────────────────────────
    suspend fun login(email: String, password: String): AuthResult =
        withContext(Dispatchers.IO) {
            try {
                supabaseClient.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                val session = supabaseClient.auth.currentSessionOrNull()
                if (session != null) {
                    sessionManager.saveSession("active_session")
                    AuthResult.Success
                } else {
                    AuthResult.Failure("Login failed: no session returned. Please try again.")
                }
            } catch (e: Exception) {
                AuthResult.Failure(mapException(e))
            }
        }

    // ── Logout ────────────────────────────────────────────────────────────────────
    suspend fun logout() = withContext(Dispatchers.IO) {
        try {
            supabaseClient.auth.signOut()
        } catch (e: Exception) {
            // Best-effort: always clear local session even if remote sign-out fails
        } finally {
            sessionManager.clearSession()
        }
    }

    // ── Deep Link / Callback Handling ──────────────────────────────────────────────
    suspend fun handleAuthCallback(uriString: String): AuthResult = withContext(Dispatchers.IO) {
        try {
            val uri = android.net.Uri.parse(uriString)
            val code = uri.getQueryParameter("code")
            if (code != null) {
                supabaseClient.auth.exchangeCodeForSession(code)
                val session = supabaseClient.auth.currentSessionOrNull()
                if (session != null) {
                    sessionManager.saveSession("active_session")
                    AuthResult.Success
                } else {
                    AuthResult.Failure("Failed to retrieve session from verification code.")
                }
            } else {
                AuthResult.Failure("No authorization code found in link.")
            }
        } catch (e: Exception) {
            AuthResult.Failure(mapException(e))
        }
    }

    // ── Session helpers ───────────────────────────────────────────────────────────
    fun currentSession(): String? = sessionManager.getSession()

    fun currentUser(): UserInfo? = supabaseClient.auth.currentUserOrNull()

    // ── Exception → user-friendly message mapper ──────────────────────────────────
    private fun mapException(e: Exception): String {
        val msg = e.message?.lowercase() ?: ""
        return when {
            e is UnknownHostException || e is ConnectException ->
                "No internet connection. Please check your network."

            e is HttpRequestTimeoutException ->
                "Connection timed out. Please try again."

            msg.contains("invalid login credentials") || msg.contains("invalid_credentials") ->
                "Invalid email or password."

            msg.contains("user already registered") || msg.contains("already been registered") ->
                "An account with this email already exists."

            msg.contains("password should be at least") || msg.contains("weak_password") ->
                "Password must contain at least 6 characters."

            msg.contains("unable to validate email") || msg.contains("invalid email") ->
                "Invalid email address."

            msg.contains("email not confirmed") ->
                "Please verify your email address before logging in."

            msg.contains("rate limit") || msg.contains("too many requests") ->
                "Too many attempts. Please wait a moment and try again."

            else -> "Something went wrong. Please try again."
        }
    }
}
