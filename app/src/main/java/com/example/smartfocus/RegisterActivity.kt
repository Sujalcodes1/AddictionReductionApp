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
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.addictionreductionapp.MainActivity
import com.example.addictionreductionapp.R
import com.example.addictionreductionapp.databinding.ActivityRegisterBinding
import com.example.addictionreductionapp.utils.SessionManager
import dagger.hilt.android.AndroidEntryPoint

@Deprecated("Replaced by Compose RegisterScreen in AuthScreens.kt — use MainActivity NavHost 'register' route instead")
@AndroidEntryPoint
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. STATUS BAR & NAVIGATION BAR
        window.statusBarColor = Color.parseColor("#04090e")
        window.navigationBarColor = Color.parseColor("#04090e")
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        // 2. TWO-TONE HEADING: "Join the Future"
        val span = SpannableStringBuilder("Join the Future")
        span.setSpan(
            ForegroundColorSpan(Color.parseColor("#00d4aa")),
            9, 15,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        binding.tvHeading.text = span

        // 3. TERMS LINK STYLING: "I agree to the Terms of Service"
        val termsSpan = SpannableStringBuilder("I agree to the Terms of Service")
        termsSpan.setSpan(
            ForegroundColorSpan(Color.parseColor("#00d4aa")),
            15, 31,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        binding.tvTermsLabel.text = termsSpan

        // 4. FOCUS LISTENERS FOR BORDER SWAP
        setFocusListeners()

        // 5. JOIN NOW BUTTON CLICK
        binding.btnJoinNow.setOnClickListener {
            val fullName = binding.etFullName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            var valid = true

            if (fullName.isEmpty()) {
                binding.tilFullName.error = "Full name cannot be empty"
                valid = false
            } else {
                binding.tilFullName.error = null
            }

            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.tilEmail.error = "Enter a valid email"
                valid = false
            } else {
                binding.tilEmail.error = null
            }

            if (password.length < 6) {
                binding.tilPassword.error = "Password must be at least 6 characters"
                valid = false
            } else {
                binding.tilPassword.error = null
            }

            if (!binding.cbTerms.isChecked) {
                Toast.makeText(this, "Please agree to the Terms of Service", Toast.LENGTH_SHORT).show()
                valid = false
            }

            if (valid) {
                binding.btnJoinNow.isEnabled = false
                binding.btnJoinNow.text = "Creating account..."
                viewModel.register(fullName, email, password)
            }
        }

        // 6. LOGIN LINK CLICK
        val loginText = SpannableString("Login")
        loginText.setSpan(UnderlineSpan(), 0, loginText.length, 0)
        binding.tvLoginLink.text = loginText
        binding.tvLoginLink.setOnClickListener {
            finish()
        }

        // 7. OBSERVE VIEWMODEL
        viewModel.registerResult.observe(this) { result ->
            binding.btnJoinNow.isEnabled = true
            binding.btnJoinNow.text = "Join Now →"
            if (result.isSuccess) {
                val email = binding.etEmail.text.toString().trim()
                if (!SessionManager(applicationContext).isLoggedIn()) {
                    SessionManager(applicationContext).saveSession("dev_local_session_$email")
                }
                startActivity(Intent(this, MainActivity::class.java))
                finishAffinity()
            } else {
                binding.tilEmail.error = result.errorMessage
            }
        }
    }

    private fun setFocusListeners() {
        binding.etFullName.setOnFocusChangeListener { _, focused ->
            binding.tilFullName.background = if (focused)
                ContextCompat.getDrawable(this, R.drawable.bg_input_focused)
            else
                ContextCompat.getDrawable(this, R.drawable.bg_input_field)
        }
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
