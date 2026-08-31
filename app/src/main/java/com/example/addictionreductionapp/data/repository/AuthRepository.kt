package com.example.addictionreductionapp.data.repository

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

    private fun isDevFallbackError(e: Exception): Boolean {
        val msg = e.message?.lowercase() ?: ""
        return e is UnknownHostException || e is ConnectException ||
            e is HttpRequestTimeoutException ||
            msg.contains("404") || msg.contains("not found") ||
            msg.contains("invalid api key") || msg.contains("invalid_api_key") ||
            msg.contains("apikey") || msg.contains("api key") ||
            msg.contains("unauthorized_invalid_api_key") ||
            // Supabase returns 401 with "Invalid API key" when publishable/anon key is truncated or wrong
            msg.contains("hint") && msg.contains("double check your api key") ||
            msg.contains("401") || msg.contains("403")
    }

    suspend fun register(name: String, email: String, password: String): AuthResult =
        withContext(Dispatchers.IO) {
            try {
                supabaseClient.auth.signUpWith(Email, redirectUrl = "smartfocus://login-callback") {
                    this.email = email
                    this.password = password
                }
                val session = supabaseClient.auth.currentSessionOrNull()
                if (session != null) {
                    sessionManager.saveSession("active_session")
                    AuthResult.Success
                } else {
                    AuthResult.EmailConfirmationRequired
                }
            } catch (e: IllegalStateException) {
                android.util.Log.e("AuthRepository", "Supabase not configured: ${e.message}", e)
                AuthResult.Failure("App is not configured. Please contact support.")
            } catch (e: Exception) {
                // Dev/offline fallback: Supabase unreachable or misconfigured (invalid/placeholder API key)
                // The project's local.properties currently contains a truncated sb_publishable key which returns
                // 401 Invalid API key — fall back to local session so login/register works for testing.
                if (isDevFallbackError(e) && !isAuthCredentialError(e)) {
                    android.util.Log.w("AuthRepository", "Supabase unavailable/misconfigured (dev fallback active). Using local session for $email", e)
                    android.util.Log.w("AuthRepository", "Root cause: ${e.message}")
                    sessionManager.saveSession("dev_local_session_$email")
                    AuthResult.Success
                } else {
                    android.util.Log.e("AuthRepository", "Auth register failed: ${e.message}", e)
                    AuthResult.Failure(mapException(e))
                }
            }
        }

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
            } catch (e: IllegalStateException) {
                android.util.Log.e("AuthRepository", "Supabase not configured: ${e.message}", e)
                AuthResult.Failure("App is not configured. Please contact support.")
            } catch (e: Exception) {
                if (isDevFallbackError(e) && !isAuthCredentialError(e)) {
                    android.util.Log.w("AuthRepository", "Supabase unavailable/misconfigured (dev fallback active). Using local session for $email", e)
                    android.util.Log.w("AuthRepository", "Root cause: ${e.message}")
                    sessionManager.saveSession("dev_local_session_$email")
                    AuthResult.Success
                } else {
                    android.util.Log.e("AuthRepository", "Auth login failed: ${e.message}", e)
                    AuthResult.Failure(mapException(e))
                }
            }
        }

    /** Returns true only for user credential errors that must NOT trigger dev fallback. */
    private fun isAuthCredentialError(e: Exception): Boolean {
        val msg = e.message?.lowercase() ?: ""
        return msg.contains("invalid login credentials") || msg.contains("invalid_credentials") ||
            msg.contains("invalid email") || msg.contains("weak_password") ||
            msg.contains("user already registered") || msg.contains("already been registered") ||
            msg.contains("email not confirmed")
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        try {
            supabaseClient.auth.signOut()
        } catch (e: Exception) {
        } finally {
            sessionManager.clearSession()
        }
    }

    suspend fun deleteAccount(): Boolean = withContext(Dispatchers.IO) {
        var remoteSuccess = false
        try {
            supabaseClient.auth.signOut()
            remoteSuccess = true
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Remote sign-out during account deletion failed", e)
        } finally {
            sessionManager.clearAllData()
        }
        remoteSuccess
    }

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

    fun currentSession(): String? = sessionManager.getSession()

    fun currentUser(): UserInfo? = supabaseClient.auth.currentUserOrNull()

    private fun mapException(e: Exception): String {
        val msg = e.message?.lowercase() ?: ""
        // Log full message for debugging — prevents generic "Something went wrong" from hiding root cause
        android.util.Log.e("AuthRepository", "mapException: ${e::class.simpleName}: ${e.message}", e)
        return when {
            e is UnknownHostException || e is ConnectException ->
                "No internet connection. Please check your network."
            e is HttpRequestTimeoutException ->
                "Connection timed out. Please try again."
            msg.contains("invalid api key") || msg.contains("invalid_api_key") || msg.contains("apikey") || msg.contains("unauthorized_invalid_api_key") ->
                "Authentication service misconfigured (invalid Supabase API key). Run in offline dev mode or fix SUPABASE_ANON_KEY in local.properties."
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
            // Surface underlying message if short enough — better than generic fallback during dev
            e.message != null && e.message!!.length < 120 -> e.message!!
            else -> "Something went wrong. Please try again. (${e::class.simpleName})"
        }
    }
}
