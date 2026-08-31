package com.example.smartfocus

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.util.Patterns
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.addictionreductionapp.MainActivity
import com.example.addictionreductionapp.R
import com.example.addictionreductionapp.databinding.ActivityLoginBinding
import com.example.addictionreductionapp.utils.SessionManager
import dagger.hilt.android.AndroidEntryPoint

@Deprecated("Replaced by Compose LoginScreen in AuthScreens.kt — use MainActivity NavHost 'login' route instead")
@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. STATUS BAR & NAVIGATION BAR
        window.statusBarColor = Color.parseColor("#04090e")
        window.navigationBarColor = Color.parseColor("#04090e")
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        // 2. TWO-TONE HEADING using SpannableStringBuilder
        val span = SpannableStringBuilder("Welcome Back")
        span.setSpan(
            ForegroundColorSpan(Color.parseColor("#00d4aa")),
            8, 12,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        binding.tvHeading.text = span

        // 3. INPUT FOCUS BORDER SWAP
        setFocusListeners()

        // 4. SIGN IN BUTTON — validation + loading state
        binding.btnSignIn.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            var valid = true

            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.tilEmail.error = "Enter a valid email"
                valid = false
            } else {
                binding.tilEmail.error = null
            }

            if (password.isEmpty()) {
                binding.tilPassword.error = "Password cannot be empty"
                valid = false
            } else {
                binding.tilPassword.error = null
            }

            if (valid) {
                binding.btnSignIn.isEnabled = false
                binding.btnSignIn.text = "Signing in..."
                viewModel.login(email, password)
            }
        }

        // 5. CREATE ACCOUNT LINK
        val linkText = SpannableString("Create one")
        linkText.setSpan(UnderlineSpan(), 0, linkText.length, 0)
        binding.tvCreateAccount.text = linkText
        binding.tvCreateAccount.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // 6. OBSERVE VIEWMODEL — persist session so MainActivity stays logged in (fixes loop back to login)
        viewModel.loginResult.observe(this) { result ->
            binding.btnSignIn.isEnabled = true
            binding.btnSignIn.text = "Sign In to Focus Shield →"
            if (result.isSuccess) {
                // Save dummy session for local login (no Supabase) — whole app was looping because isLoggedIn was false
                try {
                    val sm = com.example.addictionreductionapp.utils.SessionManager(this)
                    // store a simple marker; SessionManager checks existence of supabase_session
                    if (sm.getSession() == null) {
                        sm.saveSession("local_session_${System.currentTimeMillis()}")
                    }
                } catch (_: Exception) {
                    // Fallback plain prefs if encrypted fails
                    getSharedPreferences("auth_session_prefs_fallback", MODE_PRIVATE)
                        .edit().putString("supabase_session", "fallback_${System.currentTimeMillis()}").apply()
                }
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                binding.tilEmail.error = result.errorMessage
            }
        }
    }

    private fun setFocusListeners() {
        binding.etEmail.setOnFocusChangeListener { _, focused ->
            binding.tilEmail.background = if (focused)
                ContextCompat.getDrawable(this, R.drawable.bg_input_focused)
            else
                ContextCompat.getDrawable(this, R.drawable.bg_input_field)
        }
        binding.etPassword.setOnFocusChangeListener { _, focused ->
            binding.tilPassword.background = if (focused)
                ContextCompat.getDrawable(this, R.drawable.bg_input_focused)
            else
                ContextCompat.getDrawable(this, R.drawable.bg_input_field)
        }
    }
}
