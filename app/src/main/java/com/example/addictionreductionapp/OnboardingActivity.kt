package com.example.addictionreductionapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import com.example.addictionreductionapp.databinding.ActivityOnboardingBinding

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("SmartFocus", MODE_PRIVATE)
        // One-time reset to restore about pages after redesign (user reported not appearing)
        if (!prefs.getBoolean("onboarding_v2_reset_done", false)) {
            prefs.edit().remove("onboarding_done").putBoolean("onboarding_v2_reset_done", true).apply()
        }
        if (prefs.getBoolean("onboarding_done", false)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = Color.parseColor("#070B0E")
        window.navigationBarColor = Color.parseColor("#070B0E")
        WindowInsetsControllerCompat(window, window.decorView)
            .isAppearanceLightStatusBars = false

        binding.viewPager.adapter = OnboardingAdapter(this)
        binding.viewPager.isUserInputEnabled = false
    }

    fun goToNext(currentPage: Int) {
        if (currentPage < 3)
            binding.viewPager.setCurrentItem(currentPage + 1, true)
        else
            finishOnboarding()
    }

    fun goBack(currentPage: Int) {
        if (currentPage > 0)
            binding.viewPager.setCurrentItem(currentPage - 1, true)
    }

    fun skipOnboarding() = finishOnboarding()

    private fun finishOnboarding() {
        getSharedPreferences("SmartFocus", MODE_PRIVATE)
            .edit().putBoolean("onboarding_done", true).apply()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
