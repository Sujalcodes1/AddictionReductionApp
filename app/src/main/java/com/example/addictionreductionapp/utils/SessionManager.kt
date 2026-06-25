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

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "auth_session_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

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
}
