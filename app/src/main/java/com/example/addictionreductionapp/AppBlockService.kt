package com.example.addictionreductionapp

import android.accessibilityservice.AccessibilityService
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import java.util.*

class AppBlockService : AccessibilityService() {

    companion object {
        private const val TAG = "AppBlockService"
        private const val PREFS_NAME = "regain_prefs"
    }

    private val handler = Handler(Looper.getMainLooper())
    private var lastActivePackage: String? = null
    private var lastBlockedTime: Long = 0L

    // FIX 1 — Comprehensive whitelist: packages that must NEVER be blocked
    private val blockedPackageWhitelist = setOf(
        "com.example.addictionreductionapp",       // our own app
        "com.android.systemui",
        "com.android.launcher",
        "com.android.launcher2",
        "com.android.launcher3",
        "com.google.android.apps.nexuslauncher",
        "com.miui.home",                           // Xiaomi launcher
        "com.sec.android.app.launcher",            // Samsung launcher
        "com.huawei.android.launcher",             // Huawei launcher
        "com.oppo.launcher",
        "com.vivo.launcher",
        "com.android.settings"
    )

    // List of packages to monitor - must match AppDataStore defaults
    private val monitoredApps = listOf(
        "com.instagram.android",
        "com.zhiliaoapp.musically",
        "com.google.android.youtube",
        "com.twitter.android",
        "com.netflix.mediaclient",
        "com.snapchat.android",
        "com.facebook.katana",
        "com.reddit.frontpage",
        "com.whatsapp",
        "org.telegram.messenger"
    )

    private val checkRunnable = object : Runnable {
        override fun run() {
            checkCurrentAppUsage()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "AppBlockService connected")
        handler.post(checkRunnable)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            // FIX 2 — whitelist guard: never track our own app or launchers
            if (packageName in blockedPackageWhitelist) return
            if (packageName.startsWith("com.android.launcher") ||
                packageName.startsWith("android")
            ) return
            lastActivePackage = packageName
            Log.d(TAG, "Window changed to: $packageName")
            checkCurrentAppUsage()
        }
    }

    private fun getCurrentForegroundPackage(): String? {
        // Try UsageStatsManager as primary source
        try {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return lastActivePackage
            val endTime = System.currentTimeMillis()
            val startTime = endTime - 5000 // last 5 seconds
            val events = usageStatsManager.queryEvents(startTime, endTime)
            val event = android.app.usage.UsageEvents.Event()
            var latestPackage: String? = null
            var latestTime: Long = 0
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED ||
                    event.eventType == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND
                ) {
                    // Never treat our own app as the foreground package to block
                    if (event.packageName == packageName) continue
                    if (event.timeStamp > latestTime) {
                        latestTime = event.timeStamp
                        latestPackage = event.packageName
                    }
                }
            }
            return latestPackage ?: lastActivePackage
        } catch (e: Exception) {
            Log.e(TAG, "Error getting foreground package", e)
            return lastActivePackage
        }
    }

    private fun checkCurrentAppUsage() {
        // FIX 1 — Use lastActivePackage directly; whitelist guard is the very first check
        val packageName = lastActivePackage ?: return

        // CRITICAL: never block whitelisted packages (own app, launchers, system UI)
        if (packageName in blockedPackageWhitelist) return
        if (packageName.startsWith("com.android.launcher") ||
            packageName.startsWith("android")
        ) return

        // FIX 3 — 3-second cooldown to prevent block-screen re-trigger loop
        if (System.currentTimeMillis() - lastBlockedTime < 3000) return

        // Read directly from SharedPreferences (NOT via Compose state objects)
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val focusActive = prefs.getBoolean("focus_mode_active", false)
        val isSelected = prefs.getBoolean("${packageName}_selected", false)
        val isWhitelisted = prefs.getBoolean("${packageName}_whitelisted", false)
        val limitMinutes = prefs.getInt("${packageName}_limit", 60)
        val scheduleStart = prefs.getInt("${packageName}_schedule_start", -1)
        val scheduleEnd = prefs.getInt("${packageName}_schedule_end", -1)

        // Only monitor selected, non-whitelisted apps
        if (!isSelected || isWhitelisted) return

        // Find the app name for the block screen message
        val appName = getAppNameForPackage(packageName)

        Log.d(TAG, "Checking $packageName: focusActive=$focusActive, selected=$isSelected, limit=$limitMinutes")

        // Check block schedule first
        if (scheduleStart >= 0 && scheduleEnd >= 0) {
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val inSchedule = if (scheduleStart <= scheduleEnd) {
                currentHour in scheduleStart until scheduleEnd
            } else {
                currentHour >= scheduleStart || currentHour < scheduleEnd
            }
            if (inSchedule) {
                Log.d(TAG, "Blocking $packageName: scheduled block")
                blockApp(appName, "schedule")
                lastActivePackage = null  // FIX 4 — reset so the loop doesn't re-fire
                return
            }
        }

        // Block if Focus Mode is active
        if (focusActive) {
            Log.d(TAG, "Blocking $packageName: focus mode active")
            blockApp(appName, "focus")
            lastActivePackage = null  // FIX 4 — reset so the loop doesn't re-fire
            return
        }

        // Check daily usage limit
        if (isLimitExceeded(packageName, limitMinutes)) {
            Log.d(TAG, "Blocking $packageName: limit exceeded")
            blockApp(appName, "limit")
            lastActivePackage = null  // FIX 4 — reset so the loop doesn't re-fire
        }
    }

    private fun getAppNameForPackage(packageName: String): String {
        return when (packageName) {
            "com.instagram.android" -> "Instagram"
            "com.zhiliaoapp.musically" -> "TikTok"
            "com.google.android.youtube" -> "YouTube"
            "com.twitter.android" -> "Twitter"
            "com.netflix.mediaclient" -> "Netflix"
            "com.snapchat.android" -> "Snapchat"
            "com.facebook.katana" -> "Facebook"
            "com.reddit.frontpage" -> "Reddit"
            "com.whatsapp" -> "WhatsApp"
            "org.telegram.messenger" -> "Telegram"
            else -> packageName.substringAfterLast(".")
        }
    }

    private fun isLimitExceeded(packageName: String, limitMinutes: Int): Boolean {
        try {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startTime = calendar.timeInMillis
            val endTime = System.currentTimeMillis()

            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY, startTime, endTime
            )
            val totalTime = stats.filter { it.packageName == packageName }
                .sumOf { it.totalTimeInForeground }

            val limitInMillis = limitMinutes * 60 * 1000L
            return totalTime >= limitInMillis
        } catch (e: Exception) {
            Log.e(TAG, "Error checking usage limit", e)
            return false
        }
    }

    private fun blockApp(appName: String, reason: String) {
        lastBlockedTime = System.currentTimeMillis()
        Log.d(TAG, "Blocking app: $appName reason: $reason")

        // Use performGlobalAction to go home (more reliable than launching home intent)
        performGlobalAction(GLOBAL_ACTION_HOME)

        // Small delay to let the home screen appear, then launch our block screen
        handler.postDelayed({
            try {
                val intent = Intent(this, MainActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    )
                    putExtra("show_block_screen", true)
                    putExtra("blocked_app_name", appName)
                    putExtra("block_reason", reason)
                }
                startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error launching block screen", e)
            }
        }, 300)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(checkRunnable)
        Log.d(TAG, "AppBlockService destroyed")
    }

    override fun onInterrupt() {
        Log.d(TAG, "AppBlockService interrupted")
    }
}
