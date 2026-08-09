package com.example.addictionreductionapp

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import com.example.addictionreductionapp.data.repository.AppLimitRepository
import com.example.addictionreductionapp.data.repository.UserProfileRepository
import com.example.addictionreductionapp.data.repository.AppUsageRepository
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
 * App limits and focus mode state are read from [UserProfileRepository] (Room),
 * which is the same source the UI ([AppBlockerScreen], [HomeScreen]) writes to.
 * Daily usage totals are tracked and written to [AppUsageRepository] (Room) directly within this service.
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

    @Inject
    lateinit var appLimitRepository: AppLimitRepository

    @Inject
    lateinit var userProfileRepository: UserProfileRepository

    @Inject
    lateinit var reductionPlanRepository: com.example.addictionreductionapp.data.repository.ReductionPlanRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "AppBlockService"
        private const val BLOCK_COOLDOWN_MS = 5_000L
        private const val POLL_INTERVAL_MS = 2_000L
        private const val MIN_SESSION_DURATION_MS = 3_000L
        private const val NOTIFICATION_ID = 2002
        private const val CHANNEL_ID = "accessibility_service"
        private val DATE_FMT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }

    private val handler = Handler(Looper.getMainLooper())
    private var lastActivePackage: String? = null
    private var activePackageStartElapsed: Long = 0L
    private var lastBlockedPackage: String? = null
    private var lastBlockedTime: Long = 0L
    private var sessionStartWallMs: Long = 0L

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                Log.d(TAG, "Screen off detected — flushing current session")
                flushCurrentSession()
            }
        }
    }

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
            val km = getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
            if (km?.isKeyguardLocked == true) {
                handler.postDelayed(this, POLL_INTERVAL_MS)
                return
            }
            checkCurrentAppUsage()
            handler.postDelayed(this, POLL_INTERVAL_MS)
        }
    }

    // ── Service lifecycle ─────────────────────────────────────────────────────

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "AppBlockService CONNECTED — polling every ${POLL_INTERVAL_MS}ms")

        // Persistent notification — prevents Android from killing this service (M10)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildServiceNotification())

        handler.post(checkRunnable)

        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenOffReceiver, filter)
    }

    /**
     * Called when Android unbinds the accessibility service (e.g., user toggles
     * accessibility off in Settings). Attempt recovery by scheduling a health
     * check notification that guides the user back to settings.
     */
    override fun onUnbind(intent: Intent?): Boolean {
        Log.w(TAG, "AppBlockService UNBOUND — accessibility may have been disabled")
        flushCurrentSession()
        handler.removeCallbacks(checkRunnable)
        try { unregisterReceiver(screenOffReceiver) } catch (_: Exception) {}
        stopForeground(STOP_FOREGROUND_REMOVE)
        // Schedule an immediate health check to alert the user
        AccessibilityHealthChecker.scheduleImmediate(applicationContext)
        return super.onUnbind(intent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "App Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "SmartFocus is monitoring app usage in the background"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildServiceNotification(): android.app.Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SmartFocus Active")
            .setContentText("Monitoring app usage to help you stay focused")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (isSystemPackage(pkg)) {
            flushCurrentSession()
            return
        }

        // Confirm foreground package with USM to avoid overlays
        val confirmedPkg = confirmForegroundPackage(pkg) ?: return

        if (confirmedPkg != lastActivePackage) {
            Log.d(TAG, "Foreground changed: $confirmedPkg (Accessibility event: $pkg)")

            // Flush the previous app's session
            lastActivePackage?.let { prevPkg ->
                val durationMs = SystemClock.elapsedRealtime() - activePackageStartElapsed
                if (durationMs >= MIN_SESSION_DURATION_MS) {
                    persistSessionAsync(
                        packageName = prevPkg,
                        sessionStartWall = sessionStartWallMs,
                        sessionEndWall = System.currentTimeMillis(),
                        durationMs = durationMs
                    )
                }
            }

            lastActivePackage = confirmedPkg
            activePackageStartElapsed = SystemClock.elapsedRealtime()
            sessionStartWallMs = System.currentTimeMillis()
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
                val profile = userProfileRepository.getProfile()
                val focusActive = profile?.isFocusModeActive ?: false

                val appConfig = appLimitRepository.getAppByPackageOnce(packageName)
                if (appConfig != null && appConfig.isWhitelisted) {
                    Log.d(TAG, "  Skip — $packageName is whitelisted")
                    return@launch
                }

                val appName = getAppNameForPackage(packageName)

                // ── Priority 1: Scheduled block window ────────────────────────
                if (appConfig != null) {
                    val scheduleStart = appConfig.blockScheduleStart
                    val scheduleEnd = appConfig.blockScheduleEnd
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
                }

                // ── Priority 2: Focus Mode active ─────────────────────────────
                if (focusActive) {
                    if (appConfig != null && appConfig.isSelected) {
                        Log.i(TAG, "LIMIT REACHED — $packageName blocked by focus mode")
                        withContext(Dispatchers.Main) {
                            triggerBlock(packageName, appName, "focus")
                        }
                        return@launch
                    }
                }

                // ── Priority 3: Smart Reduction Category Limit ────────────────
                val category = com.example.addictionreductionapp.utils.AppCategoryResolver.resolveCategory(packageName)
                val activePlans = reductionPlanRepository.getActive()
                val plan = activePlans.find { it.category.equals(category, ignoreCase = true) && it.isActive }
                if (plan != null) {
                    val today = DATE_FMT.format(Date())
                    val categoryMinutes = appUsageRepository.getTotalMinutesForCategory(category, today)
                    val liveSessionMs = if (packageName == lastActivePackage && activePackageStartElapsed > 0L) {
                        SystemClock.elapsedRealtime() - activePackageStartElapsed
                    } else 0L
                    val totalCategoryMinutes = categoryMinutes + (liveSessionMs / 60_000L).toInt()

                    Log.d(TAG, "  Smart Reduction Category check: category=$category usage=$totalCategoryMinutes target=${plan.currentTarget}")

                    if (totalCategoryMinutes >= plan.currentTarget) {
                        Log.i(TAG, "SMART REDUCTION LIMIT REACHED — Category $category: ${totalCategoryMinutes}m >= ${plan.currentTarget}m target. Blocking triggered.")
                        withContext(Dispatchers.Main) {
                            triggerBlock(packageName, appName, "limit")
                        }
                        return@launch
                    }
                }

                // ── Priority 4: Daily usage limit exceeded ────────────────────
                if (appConfig != null && appConfig.isSelected) {
                    val limitMinutes = appConfig.limitMinutes
                    val today = DATE_FMT.format(Date())
                    val savedUsage = appUsageRepository.getUsageForAppOnDate(packageName, today)
                    val savedMinutes = savedUsage?.usageMinutes ?: 0
                    val liveSessionMs = if (packageName == lastActivePackage && activePackageStartElapsed > 0L) {
                        SystemClock.elapsedRealtime() - activePackageStartElapsed
                    } else 0L
                    val totalMinutes = savedMinutes + (liveSessionMs / 60_000L).toInt()

                    Log.d(TAG, "  App limit check: savedMinutes=$savedMinutes, liveSessionMs=${liveSessionMs}ms, totalMinutes=$totalMinutes / $limitMinutes")

                    if (totalMinutes >= limitMinutes) {
                        Log.i(TAG, "LIMIT REACHED — $packageName: ${totalMinutes}m >= ${limitMinutes}m limit. Blocking triggered.")
                        withContext(Dispatchers.Main) {
                            triggerBlock(packageName, appName, "limit")
                        }
                        return@launch
                    }
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
        lastActivePackage = null

        Log.i(TAG, "BLOCKING TRIGGERED — package=$packageName reason=$reason")

        // 1. Immediately navigate to home screen
        performGlobalAction(GLOBAL_ACTION_HOME)
        Log.d(TAG, "  performGlobalAction(HOME) called")

        // 2. Launch overlay service to persist the block
        handler.postDelayed({
            try {
                val overlayIntent = Intent(this, BlockOverlayService::class.java).apply {
                    putExtra(BlockOverlayService.EXTRA_APP_NAME, appName)
                    putExtra(BlockOverlayService.EXTRA_REASON, reason)
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    startForegroundService(overlayIntent)
                } else {
                    startService(overlayIntent)
                }
                Log.i(TAG, "OVERLAY SERVICE STARTED — BlockOverlayService launched for $appName ($reason)")
            } catch (e: Exception) {
                Log.e(TAG, "ERROR — Failed to launch overlay service", e)
                // Fallback: launch MainActivity block screen
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

    private fun flushCurrentSession() {
        val pkg = lastActivePackage ?: return
        val nowWall = System.currentTimeMillis()
        val durationMs = SystemClock.elapsedRealtime() - activePackageStartElapsed
        if (durationMs >= MIN_SESSION_DURATION_MS) {
            persistSessionAsync(
                packageName = pkg,
                sessionStartWall = sessionStartWallMs,
                sessionEndWall = nowWall,
                durationMs = durationMs
            )
        }
        lastActivePackage = null
    }

    private fun persistSessionAsync(
        packageName: String,
        sessionStartWall: Long,
        sessionEndWall: Long,
        durationMs: Long
    ) {
        serviceScope.launch {
            try {
                val durationMinutes = (durationMs / 60_000L).toInt()
                if (durationMinutes <= 0) {
                    Log.d(TAG, "Skipping persist for sub-minute session (${durationMs}ms) on $packageName")
                    return@launch
                }
                val usageDate = DATE_FMT.format(Date(sessionStartWall))

                Log.d(TAG, "Persisting $durationMinutes minutes for package $packageName")

                val appName = getAppNameForPackage(packageName)
                val category = com.example.addictionreductionapp.utils.AppCategoryResolver.resolveCategory(packageName)

                appUsageRepository.persistSession(
                    packageName = packageName,
                    appName = appName,
                    category = category,
                    sessionStartWall = sessionStartWall,
                    sessionEndWall = sessionEndWall,
                    durationMinutes = durationMinutes,
                    usageDate = usageDate
                )
                Log.i(TAG, "Room insert/update SUCCESS for $packageName: added ${durationMinutes}m")
            } catch (e: Exception) {
                Log.e(TAG, "Room insert/update FAILURE for $packageName", e)
            }
        }
    }

    private fun confirmForegroundPackage(candidatePackage: String): String? {
        val usm = applicationContext.getSystemService(Context.USAGE_STATS_SERVICE)
            as? UsageStatsManager ?: return candidatePackage

        val now = System.currentTimeMillis()
        val lookbackStart = now - 3_000L

        return try {
            val events = usm.queryEvents(lookbackStart, now)
            val event = UsageEvents.Event()
            var lastResumedPackage: String? = null
            var hasEvents = false

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                    lastResumedPackage = event.packageName
                    hasEvents = true
                }
            }

            if (!hasEvents) {
                return candidatePackage
            }

            if (lastResumedPackage == candidatePackage) {
                candidatePackage
            } else {
                null
            }
        } catch (e: SecurityException) {
            candidatePackage
        } catch (e: Exception) {
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(checkRunnable)
        try { unregisterReceiver(screenOffReceiver) } catch (_: Exception) {}
        flushCurrentSession()
        stopForeground(STOP_FOREGROUND_REMOVE)
        serviceScope.cancel()
        Log.i(TAG, "AppBlockService DESTROYED")
    }

    override fun onInterrupt() {
        Log.w(TAG, "AppBlockService INTERRUPTED")
        flushCurrentSession()
    }
}
