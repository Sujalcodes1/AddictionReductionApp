package com.example.addictionreductionapp.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = try {
        createEncryptedSharedPreferences(context)
    } catch (e: Exception) {
        android.util.Log.e("SessionManager", "EncryptedSharedPreferences creation failed, clearing and retrying...", e)
        // Clear preference files if Keystore got corrupted or keys mismatch
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                context.deleteSharedPreferences("auth_session_prefs")
            } else {
                context.getSharedPreferences("auth_session_prefs", Context.MODE_PRIVATE).edit().clear().commit()
            }
            // Also attempt to delete the key from the keystore if possible, but recreating works for AES256_GCM builder normally
        } catch (clearEx: Exception) {
            android.util.Log.e("SessionManager", "Failed to clear corrupt preferences file", clearEx)
        }
        // Fallback to normal shared preferences or try creating a new one
        try {
            createEncryptedSharedPreferences(context)
        } catch (retryEx: Exception) {
            android.util.Log.e("SessionManager", "EncryptedSharedPreferences retry failed, falling back to standard prefs", retryEx)
            context.getSharedPreferences("auth_session_prefs_fallback", Context.MODE_PRIVATE)
        }
    }

    private fun createEncryptedSharedPreferences(context: Context): SharedPreferences {
        return EncryptedSharedPreferences.create(
            context,
            "auth_session_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveSession(sessionString: String) {
        sharedPreferences.edit().putString("supabase_session", sessionString).apply()
    }

    fun getSession(): String? {
        return sharedPreferences.getString("supabase_session", null)
    }

    fun clearSession() {
        sharedPreferences.edit().remove("supabase_session").apply()
    }

    fun isLoggedIn(): Boolean {
        return getSession() != null
    }

    /**
     * Clears ALL local data for account deletion.
     * Removes session, SharedPreferences, and resets the database passphrase.
     */
    fun clearAllData() {
        clearSession()
        try {
            sharedPreferences.edit().clear().apply()
        } catch (e: Exception) {
            android.util.Log.e("SessionManager", "Failed to clear encrypted prefs", e)
        }
    }
}
