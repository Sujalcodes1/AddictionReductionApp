package com.example.smartfocus

import android.graphics.Color
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.addictionreductionapp.databinding.ActivityAnalyticsBinding

class AnalyticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnalyticsBinding
    private val viewModel: AnalyticsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalyticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Status bar — same as Login: #010d0f, light=false, outer radial bg
        window.statusBarColor = Color.parseColor("#010d0f")
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        viewModel.uiState.observe(this) { state ->
            if (state.isLoading) return@observe
            if (state.error != null) {
                // show error via Snackbar pattern — keep simple
                return@observe
            }
            val hrs = state.screenTimeMinutes / 60
            val mins = state.screenTimeMinutes % 60
            binding.tvScreenTime.text = if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
            binding.tvOpens.text = "${state.totalOpens} opens"
            binding.tvFocusScore.text = "${state.focusScore}"
        }
    }
}
