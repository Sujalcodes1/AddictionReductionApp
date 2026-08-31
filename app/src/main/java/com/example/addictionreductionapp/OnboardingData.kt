package com.example.addictionreductionapp

import com.example.addictionreductionapp.R

object OnboardingData {

    data class Page(
        val number: String,
        val iconRes: Int,
        val title1: String,
        val title2: String,
        val body: String,
        val btnText: String,
        val showBack: Boolean,
        val showSkip: Boolean
    )

    val pages = listOf(
        Page(
            "01", R.drawable.ic_target_arrow,
            "Reclaim Your", "Focus",
            "Take control of your screen time and build better digital habits with smart tools and AI-powered coaching.",
            "Next →", false, true
        ),
        Page(
            "02", R.drawable.ic_shield_lock,
            "Block", "Distractions",
            "Select apps to block, set daily limits, and schedule blocking windows. Stay productive effortlessly.",
            "Next →", true, true
        ),
        Page(
            "03", R.drawable.ic_clock,
            "Focus", "Timer",
            "Use the study timer with ambient sounds to stay concentrated. Lock your phone and enter deep focus mode.",
            "Next →", true, true
        ),
        Page(
            "04", R.drawable.ic_gift_reward,
            "Earn", "Rewards",
            "Build streaks, unlock achievements, and track your progress. Make productivity fun and engaging.",
            "Get Started →", true, false
        )
    )
}
