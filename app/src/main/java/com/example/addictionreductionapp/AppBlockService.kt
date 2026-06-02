package com.example.addictionreductionapp

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.addictionreductionapp.data.AppDataStore
import com.example.addictionreductionapp.repository.AppUsageRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * Accessibility service responsible for detecting foreground apps and blocking
 * them when their daily usage limit is exceeded, focus mode is active, or a
 * scheduled block window is active.
 *
 * ## Data source
 * App limits and focus mode state are read from [AppDataStore] (SharedPreferences),
 * which is the same source the UI ([AppBlockerScreen], [HomeScreen]) writes to.
 * Daily usage totals are read from [AppUsageRepository] (Room), which is populated
 * by [AppUsageTrackingService].
 *
 * ## Blocking flow
 * 1. [onAccessibilityEvent] fires when any window comes to the foreground.
 * 2. [checkCurrentAppUsage] is called immediately AND on a 2-second polling loop.
 * 3. Limit/focus/schedule checks run on [Dispatchers.IO].
 * 4. If a block is needed, [triggerBlock] posts a HOME action + MainActivity launch
 *    back to the main thread via [handler].
 */
@AndroidEntryPoint
class AppBlockService : AccessibilityService() {

    @Inject
    lateinit var appUsageRepository: AppUsageRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "AppBlockService"

        /** Minimum ms between two block actions for the same package. */
        private const val BLOCK_COOLDOWN_MS = 5_000L

        /** How often the polling loop fires. */
        private const val POLL_INTERVAL_MS = 2_000L

        private val DATE_FMT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }

    private val handler = Handler(Looper.getMainLooper())
    private var lastActivePackage: String? = null
    private var activePackageStartElapsed: Long = 0L
    private var lastBlockedPackage: String? = null
    private var lastBlockedTime: Long = 0L

    // ── Packages that must NEVER be blocked ──────────────────────────────────
    private val systemPackageWhitelist = setOf(
        "com.example.addictionreductionapp",
        "com.android.systemui",
        "com.android.launcher",
        "com.android.launcher2",
        "com.android.launcher3",
        "com.google.android.apps.nexuslauncher",
        "com.miui.home",
        "com.sec.android.app.launcher",
        "com.huawei.android.launcher",
        "com.oppo.launcher",
        "com.vivo.launcher",
        "com.realme.launcher",
        "com.oneplus.launcher",
        "com.android.settings",
        "com.android.phone",
        "com.google.android.dialer",
        "com.samsung.android.dialer"
    )

    // ── Background polling loop ───────────────────────────────────────────────
    private val checkRunnable = object : Runnable {
        override fun run() {
            checkCurrentAppUsage()
            handler.postDelayed(this, POLL_INTERVAL_MS)
        }
    }

    // ── Service lifecycle ─────────────────────────────────────────────────────

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "AppBlockService CONNECTED — polling every ${POLL_INTERVAL_MS}ms")
        handler.post(checkRunnable)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (isSystemPackage(pkg)) return

        if (pkg != lastActivePackage) {
            Log.d(TAG, "Foreground changed: $pkg")
            lastActivePackage = pkg
            activePackageStartElapsed = SystemClock.elapsedRealtime()
        }

        // Immediate check on foreground change (don't wait for poll)
        checkCurrentAppUsage()
    }

    // ── Core blocking logic ───────────────────────────────────────────────────

    private fun checkCurrentAppUsage() {
        val packageName = lastActivePackage ?: run {
            Log.v(TAG, "Limit check skipped — no foreground package tracked yet")
            return
        }
        if (isSystemPackage(packageName)) return

        val now = System.currentTimeMillis()
        if (packageName == lastBlockedPackage && now - lastBlockedTime < BLOCK_COOLDOWN_MS) {
            Log.v(TAG, "Limit check skipped — cooldown active for $packageName")
            return
        }

        Log.d(TAG, "Limit check started for $packageName")

        serviceScope.launch {
            try {
                // ── Read app config from AppDataStore (SharedPreferences) ──────
                // AppDataStore is the authoritative config source — the UI writes there.
                val prefs = getSharedPreferences("regain_prefs", Context.MODE_PRIVATE)
                val isSelected = prefs.getBoolean("${packageName}_selected", false)
                val isWhitelisted = prefs.getBoolean("${packageName}_whitelisted", false)

                Log.d(TAG, "  isSelected=$isSelected, isWhitelisted=$isWhitelisted")

                if (!isSelected) {
                    Log.d(TAG, "  Skip — $packageName not selected for monitoring")
                    return@launch
                }
                if (isWhitelisted) {
                    Log.d(TAG, "  Skip — $packageName is whitelisted")
                    return@launch
                }

                val focusActive = prefs.getBoolean("focus_mode_active", false)
                val limitMinutes = prefs.getInt("${packageName}_limit", 60)
                val scheduleStart = prefs.getInt("${packageName}_schedule_start", -1)
                val scheduleEnd = prefs.getInt("${packageName}_schedule_end", -1)
                val appName = getAppNameForPackage(packageName)

                Log.d(TAG, "  focusActive=$focusActive, limitMinutes=$limitMinutes, schedule=[$scheduleStart,$scheduleEnd]")

                // ── Priority 1: Scheduled block window ────────────────────────
                if (scheduleStart >= 0 && scheduleEnd >= 0) {
                    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    val inSchedule = if (scheduleStart <= scheduleEnd) {
                        currentHour in scheduleStart until scheduleEnd
                    } else {
                        currentHour >= scheduleStart || currentHour < scheduleEnd
                    }
                    if (inSchedule) {
                        Log.i(TAG, "LIMIT REACHED — $packageName in scheduled block window ($scheduleStart:00–$scheduleEnd:00)")
                        withContext(Dispatchers.Main) {
                            triggerBlock(packageName, appName, "schedule")
                        }
                        return@launch
                    }
                }

                // ── Priority 2: Focus Mode active ─────────────────────────────
                if (focusActive) {
                    Log.i(TAG, "LIMIT REACHED — $packageName blocked by focus mode")
                    withContext(Dispatchers.Main) {
                        triggerBlock(packageName, appName, "focus")
                    }
                    return@launch
                }

                // ── Priority 3: Daily usage limit exceeded ────────────────────
                val today = DATE_FMT.format(Date())
                val savedUsage = appUsageRepository.getUsageForAppOnDate(packageName, today)
                val savedMinutes = savedUsage?.usageMinutes ?: 0
                // Add live untracked time for the current active session
                val liveSessionMs = if (packageName == lastActivePackage && activePackageStartElapsed > 0L) {
                    SystemClock.elapsedRealtime() - activePackageStartElapsed
                } else 0L
                val totalMinutes = savedMinutes + (liveSessionMs / 60_000L).toInt()

                Log.d(TAG, "  Current usage fetched: savedMinutes=$savedMinutes, liveSessionMs=${liveSessionMs}ms, totalMinutes=$totalMinutes / $limitMinutes")

                if (totalMinutes >= limitMinutes) {
                    Log.i(TAG, "LIMIT REACHED — $packageName: ${totalMinutes}m >= ${limitMinutes}m limit. Blocking triggered.")
                    withContext(Dispatchers.Main) {
                        triggerBlock(packageName, appName, "limit")
                    }
                } else {
                    Log.d(TAG, "  $packageName within limit: ${totalMinutes}m / ${limitMinutes}m")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in checkCurrentAppUsage for $packageName", e)
            }
        }
    }

    // ── Block action ──────────────────────────────────────────────────────────

    /**
     * Must be called from the main thread (use [withContext(Dispatchers.Main)]).
     */
    private fun triggerBlock(packageName: String, appName: String, reason: String) {
        lastBlockedPackage = packageName
        lastBlockedTime = System.currentTimeMillis()
        // Clear so the polling loop doesn't re-trigger while block screen shows
        lastActivePackage = null

        Log.i(TAG, "BLOCKING TRIGGERED — package=$packageName reason=$reason")

        // 1. Immediately navigate to home screen
        performGlobalAction(GLOBAL_ACTION_HOME)
        Log.d(TAG, "  performGlobalAction(HOME) called")

        // 2. After a short delay, bring MainActivity to foreground with block screen
        handler.postDelayed({
            try {
                val intent = Intent(this, MainActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )
                    putExtra("show_block_screen", true)
                    putExtra("blocked_app_name", appName)
                    putExtra("block_reason", reason)
                }
                startActivity(intent)
                Log.i(TAG, "OVERLAY LAUNCHED — MainActivity started with block screen for $appName ($reason)")
            } catch (e: Exception) {
                Log.e(TAG, "ERROR — Failed to launch block screen overlay", e)
            }
        }, 400L)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun isSystemPackage(pkg: String): Boolean =
        pkg in systemPackageWhitelist ||
        pkg.startsWith("com.android.launcher") ||
        pkg.startsWith("android")

    private fun getAppNameForPackage(packageName: String): String = when (packageName) {
        "com.instagram.android"      -> "Instagram"
        "com.zhiliaoapp.musically"   -> "TikTok"
        "com.google.android.youtube" -> "YouTube"
        "com.twitter.android"        -> "Twitter"
        "com.netflix.mediaclient"    -> "Netflix"
        "com.snapchat.android"       -> "Snapchat"
        "com.facebook.katana"        -> "Facebook"
        "com.reddit.frontpage"       -> "Reddit"
        "com.whatsapp"               -> "WhatsApp"
        "org.telegram.messenger"     -> "Telegram"
        else -> packageName.substringAfterLast(".").replaceFirstChar { it.uppercase() }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(checkRunnable)
        serviceScope.cancel()
        Log.i(TAG, "AppBlockService DESTROYED")
    }

    override fun onInterrupt() {
        Log.w(TAG, "AppBlockService INTERRUPTED")
    }
}
