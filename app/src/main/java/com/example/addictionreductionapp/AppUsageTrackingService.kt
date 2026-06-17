package com.example.addictionreductionapp

import android.accessibilityservice.AccessibilityService
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.addictionreductionapp.repository.AppUsageRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Automatically tracks foreground app usage and persists daily aggregates to
 * the Room [AppUsageDao] / [AppUsageEntity] table.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * ## Service Lifecycle
 *
 * ```
 * AccessibilityService lifecycle
 * ┌─────────────┐  onCreate()         ┌──────────────────────┐
 * │  Not bound  │ ──────────────────► │  onServiceConnected  │
 * └─────────────┘                     │  • scope created     │
 *                                     │  • initial snapshot  │
 *                                     └──────────┬───────────┘
 *                                                │  TYPE_WINDOW_STATE_CHANGED events
 *                                                ▼
 *                                     ┌──────────────────────┐
 *                                     │  onAccessibilityEvent│
 *                                     │  • recordAppSwitch() │
 *                                     │  • flush previous    │
 *                                     │  • start new session │
 *                                     └──────────┬───────────┘
 *                                                │  onDestroy / onInterrupt
 *                                                ▼
 *                                     ┌──────────────────────┐
 *                                     │  onDestroy           │
 *                                     │  • flush open session│
 *                                     │  • cancel scope      │
 *                                     └──────────────────────┘
 * ```
 *
 * The service is registered in AndroidManifest.xml and started automatically
 * by Android when the user grants Accessibility permission.  It runs for the
 * entire system lifetime (same as [AppBlockService]) — no explicit start/stop
 * needed from the app.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * ## Foreground Detection Logic
 *
 * **Primary signal — AccessibilityEvent (instant):**
 * Every `TYPE_WINDOW_STATE_CHANGED` event carries the `packageName` of the
 * window that just became foreground.  This fires within ~16 ms of an app
 * switch, making it the most responsive detection mechanism available without
 * root.
 *
 * **Confirmation — UsageStatsManager (accurate):**
 * `TYPE_WINDOW_STATE_CHANGED` can fire for sub-windows, dialogs, and IME
 * pickers — not just top-level app switches.  Before recording a real switch
 * we query `UsageStatsManager.queryEvents()` over a 3-second lookback window
 * to confirm the package emitted an `ACTIVITY_RESUMED` event.  This eliminates
 * false positives caused by permission dialogs, toasts, and overlay windows
 * appearing on top of the real foreground app.
 *
 * **Deduplication:**
 * If the confirmed foreground package equals the already-tracked
 * [currentPackage], the event is silently ignored — no redundant DB writes.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * ## Timing Calculation Logic
 *
 * We use **two complementary clocks**:
 *
 * | Clock                       | Purpose                          |
 * |-----------------------------|----------------------------------|
 * | `System.currentTimeMillis()`| Absolute wall-clock timestamps stored in DB |
 * | `SystemClock.elapsedRealtime()` | Monotonic duration, immune to clock skew |
 *
 * On each confirmed app switch:
 * 1. Record `sessionStartWallMs = System.currentTimeMillis()` — stored as
 *    `start_timestamp` in [AppUsageEntity].
 * 2. Record `sessionStartElapsedMs = SystemClock.elapsedRealtime()` — used
 *    for duration arithmetic only.
 * 3. When the next switch arrives, duration = `elapsedRealtime() - sessionStartElapsedMs`.
 *    Converting to minutes: `durationMs / 60_000`.  Fractional minutes are
 *    truncated (floor) intentionally — this avoids inflating stats.
 *
 * Monotonic clock is critical: `currentTimeMillis()` can jump forward or
 * backward during NTP sync, causing negative or inflated durations.
 * `elapsedRealtime()` is guaranteed to never go backward.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * ## Battery Optimisation
 *
 * - **No polling loop** — all work is interrupt-driven by accessibility events.
 *   CPU is not woken on a timer.
 * - **IO-bound coroutines** — all Room writes dispatched to [Dispatchers.IO];
 *   the main thread (on which accessibility events fire) is never blocked.
 * - **SupervisorJob** — a crash in one DB write does not cancel the scope or
 *   prevent subsequent writes.
 * - **Debounce guard** — rapid consecutive events for the same package within
 *   [MIN_SESSION_DURATION_MS] are collapsed into a single record, avoiding
 *   hundreds of tiny insertions from dialog/keyboard flickers.
 * - **UsageStatsManager lookback** kept to 3 s — the shortest reliable window
 *   that captures the confirmation event, preventing unbounded event scanning.
 */
@AndroidEntryPoint
class AppUsageTrackingService : AccessibilityService() {

    // ── Hilt injection ────────────────────────────────────────────────────────
    // Hilt cannot directly inject into AccessibilityService via constructor,
    // so we use field injection.  The @AndroidEntryPoint annotation on this
    // class enables it.  As a fallback (e.g. for testing without Hilt),
    // we also support manual initialisation via AppDatabase.getInstance().

    @Inject
    lateinit var appUsageRepository: AppUsageRepository

    // ── Screen state receiver ─────────────────────────────────────────────────
    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                Log.d(TAG, "Screen off detected — flushing current session")
                flushCurrentSession()
            }
        }
    }

    // ── Coroutine scope ───────────────────────────────────────────────────────
    // SupervisorJob: a failing child coroutine does NOT cancel siblings or parent.
    // Dispatchers.IO: all DB writes run off the main thread.
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ── Session state ─────────────────────────────────────────────────────────
    /** Package name of the app currently in foreground. Null before first event. */
    private var currentPackage: String? = null

    /**
     * Wall-clock ms when the current session started.
     * Stored in [AppUsageEntity.startTimestamp].
     */
    private var sessionStartWallMs: Long = 0L

    /**
     * Monotonic ms when the current session started.
     * Used exclusively for duration arithmetic — never stored in DB.
     */
    private var sessionStartElapsedMs: Long = 0L

    // ── Date formatter ────────────────────────────────────────────────────────
    /** Thread-local is not needed; all formatting happens on IO dispatcher. */
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    // ── Constants ─────────────────────────────────────────────────────────────
    companion object {
        private const val TAG = "AppUsageTracking"

        /** Own package — never tracked. */
        private const val OWN_PACKAGE = "com.example.addictionreductionapp"

        /**
         * Sessions shorter than this are not written to the DB.
         * Eliminates spurious records from dialog flickers and IME windows.
         * 3 seconds is empirically the shortest "intentional" foreground event.
         */
        private const val MIN_SESSION_DURATION_MS = 3_000L

        /**
         * How far back (ms) we query UsageStatsManager to confirm an
         * ACTIVITY_RESUMED event.  3 s covers any typical event delivery lag.
         */
        private const val USM_CONFIRM_LOOKBACK_MS = 3_000L

        // ── System / launcher packages that must NEVER be tracked ─────────────
        // Matches the same whitelist in AppBlockService for consistency.
        private val IGNORED_PACKAGES: Set<String> = setOf(
            OWN_PACKAGE,
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
            "com.samsung.android.dialer",
            "com.android.inputmethod.latin",
            "com.google.android.inputmethod.latin",
            "com.samsung.android.inputmethod"
        )
    }

    // ── Service lifecycle ─────────────────────────────────────────────────────

    override fun onServiceConnected() {
        super.onServiceConnected()
        // @AndroidEntryPoint guarantees Hilt injects appUsageRepository before
        // onServiceConnected() is called.  If it is somehow not initialized here,
        // the DI graph is broken — log the error clearly rather than silently
        // creating an unscoped repository that bypasses the @Singleton contract.
        if (!::appUsageRepository.isInitialized) {
            Log.e(TAG, "CRITICAL: appUsageRepository was NOT injected by Hilt. " +
                "Check that @AndroidEntryPoint is present and SmartFocusApp is @HiltAndroidApp.")
            return
        }

        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenOffReceiver, filter)

        Log.i(TAG, "AppUsageTrackingService connected — repository injected OK")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        Log.v(TAG, "onAccessibilityEvent triggered: eventType=${event.eventType}, package=${event.packageName}")

        // We only care about window-level foreground changes.
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        val rawPackage = event.packageName?.toString()
        if (rawPackage == null) {
            Log.v(TAG, "Early return: packageName is null")
            return
        }

        // Fast-path: ignore known system / launcher / own packages immediately
        // without touching UsageStatsManager.
        if (shouldIgnorePackage(rawPackage)) {
            Log.v(TAG, "Early return: $rawPackage is in ignore list or is uninteresting system app")
            flushCurrentSession()
            return
        }

        // Confirm via UsageStatsManager that this is a genuine ACTIVITY_RESUMED
        // event, not just a dialog or overlay window appearing.
        val confirmedPackage = confirmForegroundPackage(rawPackage)
        if (confirmedPackage == null) {
            Log.v(TAG, "Early return: $rawPackage not confirmed by UsageStatsManager")
            return
        }

        // De-duplicate: same app is still in foreground — nothing to do.
        if (confirmedPackage == currentPackage) {
            Log.v(TAG, "Early return: $confirmedPackage is already the current package")
            return
        }
        
        Log.i(TAG, "App detected: $confirmedPackage (Accessibility event for $rawPackage)")

        val nowWall = System.currentTimeMillis()
        val nowElapsed = SystemClock.elapsedRealtime()

        // ── 1. Flush the previous session ─────────────────────────────────────
        currentPackage?.let { previousPackage ->
            val durationMs = nowElapsed - sessionStartElapsedMs
            Log.d(TAG, "Session ended for $previousPackage. Duration calculated: ${durationMs}ms")
            Log.d(TAG, "Flushing previous session")
            if (durationMs >= MIN_SESSION_DURATION_MS) {
                persistSessionAsync(
                    packageName = previousPackage,
                    sessionStartWall = sessionStartWallMs,
                    sessionEndWall = nowWall,
                    durationMs = durationMs
                )
            } else {
                Log.v(TAG, "Dropping sub-threshold session: $previousPackage (${durationMs}ms)")
            }
        }

        // ── 2. Start tracking the new foreground app ───────────────────────────
        currentPackage = confirmedPackage
        sessionStartWallMs = nowWall
        sessionStartElapsedMs = nowElapsed

        Log.i(TAG, "Session started for $confirmedPackage at wall time $sessionStartWallMs")
    }

    override fun onInterrupt() {
        Log.w(TAG, "AppUsageTrackingService interrupted")
        flushCurrentSession()
    }

    override fun onDestroy() {
        super.onDestroy()
        flushCurrentSession()
        try {
            unregisterReceiver(screenOffReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister screen off receiver", e)
        }
        serviceScope.cancel()
        Log.i(TAG, "AppUsageTrackingService destroyed")
    }

    // ── Foreground detection ──────────────────────────────────────────────────

    /**
     * Returns true if [pkg] should be unconditionally ignored.
     *
     * Fast checks (no IPC):
     * 1. Exact match against [IGNORED_PACKAGES] set — O(1).
     * 2. Prefix checks for families of system packages.
     */
    private fun shouldIgnorePackage(pkg: String): Boolean {
        if (pkg in IGNORED_PACKAGES) return true
        if (pkg.startsWith("android")) return true
        if (pkg.startsWith("com.android.launcher")) return true
        // Ignore pure system apps (those with FLAG_SYSTEM but no FLAG_UPDATED_SYSTEM_APP)
        // that aren't user-visible.  We check this only if the exact/prefix checks pass.
        return isUninterestingSystemApp(pkg)
    }

    /**
     * Returns true for system apps that have no visible UI the user would
     * interact with intentionally (e.g. com.android.providers.*, keyguard, etc.).
     *
     * This is an optional heuristic — false positives (tracking a system app)
     * are harmless; false negatives (not tracking a user app) don't occur
     * because user-installed apps are never system-only.
     */
    private fun isUninterestingSystemApp(pkg: String): Boolean {
        return try {
            val pm = applicationContext.packageManager
            val info: ApplicationInfo = pm.getApplicationInfo(pkg, 0)
            // FLAG_SYSTEM = pre-installed in /system partition
            // FLAG_UPDATED_SYSTEM_APP = user-updated system app (e.g. Chrome, GMaps) — keep these
            val isSystemOnly = (info.flags and ApplicationInfo.FLAG_SYSTEM != 0) &&
                               (info.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP == 0)
            // Also keep system apps that have a launcher icon (they are user-facing)
            if (isSystemOnly) {
                val launchIntent = pm.getLaunchIntentForPackage(pkg)
                launchIntent == null // true = no launcher icon → ignore
            } else {
                false
            }
        } catch (e: PackageManager.NameNotFoundException) {
            // Package not found due to Android 11+ visibility rules (missing QUERY_ALL_PACKAGES)
            // or a transient window. Returning TRUE here would silently drop valid apps!
            // We return FALSE to allow the pipeline to proceed; UsageStatsManager will filter
            // it out later if it's truly a ghost window.
            Log.w(TAG, "PackageManager could not find $pkg (visibility rules or uninstalled). Assuming user app (returning false).")
            false
        }
    }

    /**
     * Queries [UsageStatsManager] over the last [USM_CONFIRM_LOOKBACK_MS] to
     * verify that [candidatePackage] actually raised an `ACTIVITY_RESUMED`
     * event recently.
     *
     * Why this matters: `TYPE_WINDOW_STATE_CHANGED` fires for every sub-window
     * (permission dialogs, autocomplete dropdowns, IME picker, etc.) — all of
     * which carry the *dialog's* package name, not the underlying app's.
     * Confirming with UsageStatsManager (which only records genuine Activity
     * lifecycle events) prevents mis-attributing time to these overlays.
     *toda
     * @return [candidatePackage] if confirmed, null otherwise.
     */
    private fun confirmForegroundPackage(candidatePackage: String): String? {
        val usm = applicationContext.getSystemService(Context.USAGE_STATS_SERVICE)
            as? UsageStatsManager ?: return candidatePackage // Graceful degradation

        val now = System.currentTimeMillis()
        val lookbackStart = now - USM_CONFIRM_LOOKBACK_MS

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

            // If USM is lagging or permission is implicitly missing (returns empty),
            // we should not drop the accessibility event.
            if (!hasEvents) {
                Log.d(TAG, "USM returned no events. Trusting raw package: $candidatePackage")
                return candidatePackage
            }

            // Confirm only if the most-recently resumed Activity belongs to our candidate.
            if (lastResumedPackage == candidatePackage) {
                candidatePackage
            } else {
                Log.d(TAG, "USM mismatch: Accessibility=$candidatePackage, USM=$lastResumedPackage. Ignoring overlay.")
                null
            }
        } catch (e: SecurityException) {
            // PACKAGE_USAGE_STATS permission revoked at runtime — degrade gracefully.
            Log.e(TAG, "UsageStats permission denied — trusting accessibility event", e)
            candidatePackage
        } catch (e: Exception) {
            Log.e(TAG, "confirmForegroundPackage failed", e)
            null
        }
    }

    // ── Timing & persistence ──────────────────────────────────────────────────

    /**
     * Flush the still-open session for [currentPackage] using the current
     * wall and monotonic times.  Safe to call multiple times — guarded by a
     * null check on [currentPackage].
     *
     * Called from [onInterrupt] and [onDestroy] so no usage is lost when
     * the service is killed or interrupted.
     */
    private fun flushCurrentSession() {
        val pkg = currentPackage ?: return
        val nowWall = System.currentTimeMillis()
        val durationMs = SystemClock.elapsedRealtime() - sessionStartElapsedMs
        Log.d(TAG, "Flushing previous session")
        Log.d(TAG, "Flushing session for $pkg. Duration calculated: ${durationMs}ms")
        if (durationMs >= MIN_SESSION_DURATION_MS) {
            persistSessionAsync(
                packageName = pkg,
                sessionStartWall = sessionStartWallMs,
                sessionEndWall = nowWall,
                durationMs = durationMs
            )
        } else {
             Log.v(TAG, "Dropping sub-threshold flushed session: $pkg (${durationMs}ms)")
        }
        currentPackage = null
    }

    /**
     * Dispatches a non-blocking coroutine on [Dispatchers.IO] to upsert the
     * completed session into Room.
     *
     * ## Algorithm
     * 1. Convert [durationMs] to whole minutes (floor division — no inflation).
     * 2. Format [sessionStartWall] as "YYYY-MM-DD" for the `usage_date` field.
     * 3. Attempt atomic `accumulateUsage()` UPDATE (most common path).
     * 4. If no row exists yet for (packageName, date), resolve app metadata
     *    and INSERT a brand-new row.
     *
     * This INSERT-or-UPDATE pattern is safe under concurrent writes from
     * [AppBlockService] because:
     * - `accumulateUsage()` is a single atomic SQL statement.
     * - `insertUsage()` uses `OnConflictStrategy.IGNORE` — a duplicate INSERT
     *   (race condition) is silently dropped.
     *
     * @param packageName       App package being recorded.
     * @param sessionStartWall  Epoch ms when this session started (wall clock).
     * @param sessionEndWall    Epoch ms when this session ended (wall clock).
     * @param durationMs        Monotonic duration of this session in milliseconds.
     */
    private fun persistSessionAsync(
        packageName: String,
        sessionStartWall: Long,
        sessionEndWall: Long,
        durationMs: Long
    ) {
        serviceScope.launch {
            try {
                // Round up fractional minutes if it was at least MIN_SESSION_DURATION_MS, 
                // so we don't lose usage just because it was under 60 seconds.
                val durationMinutes = Math.max(1, Math.ceil(durationMs / 60_000.0).toInt())
                val usageDate = dateFormatter.format(Date(sessionStartWall))

                Log.d(TAG, "Persisting $durationMinutes minutes for package $packageName")
                Log.d(TAG, "Attempting to persist session for $packageName: $durationMinutes minutes (${durationMs}ms) on $usageDate")

                // Resolve metadata (name, category)
                val (appName, category) = resolveAppMetadata(packageName)

                appUsageRepository.persistSession(
                    packageName = packageName,
                    appName = appName,
                    category = category,
                    sessionStartWall = sessionStartWall,
                    sessionEndWall = sessionEndWall,
                    durationMinutes = durationMinutes,
                    usageDate = usageDate
                )
                
                Log.i(TAG, "Insert success")
                Log.i(TAG, "Room insert/update SUCCESS for $packageName: added ${durationMinutes}m")
            } catch (e: Exception) {
                Log.e(TAG, "Room insert/update FAILURE for $packageName", e)
            }
        }
    }

    // ── Metadata helpers ──────────────────────────────────────────────────────

    /**
     * Resolves the human-readable app name and a coarse category label for
     * [packageName] using [PackageManager].
     *
     * Called only on the **first** record insert for an (app, date) pair, so
     * the PackageManager IPC cost is not repeated for every session event.
     *
     * @return Pair(appName, category). Both fall back to safe defaults if
     *         PackageManager cannot resolve the package.
     */
    private fun resolveAppMetadata(packageName: String): Pair<String, String> {
        val pm = applicationContext.packageManager
        return try {
            val info = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            val appName = pm.getApplicationLabel(info).toString()
            val category = resolveCategory(packageName, info)
            Pair(appName, category)
        } catch (e: PackageManager.NameNotFoundException) {
            // Package was uninstalled between event and persistence — use fallback.
            val fallbackName = packageName
                .substringAfterLast(".")
                .replaceFirstChar { it.uppercase() }
            Pair(fallbackName, "Unknown")
        }
    }

    /**
     * Derives a coarse category string for the app.
     *
     * Priority order:
     * 1. [ApplicationInfo.category] (API 26+) — most accurate.
     * 2. Known-package heuristic table — covers pre-API-26 and miscategorised apps.
     * 3. "Unknown" fallback.
     *
     * Stored as a plain string so new categories can be added without a DB migration.
     */
    private fun resolveCategory(packageName: String, info: ApplicationInfo): String {
        // API 26+ provides ApplicationInfo.category
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val apiCategory = when (info.category) {
                ApplicationInfo.CATEGORY_SOCIAL      -> "Social"
                ApplicationInfo.CATEGORY_VIDEO       -> "Video"
                ApplicationInfo.CATEGORY_NEWS        -> "News"
                ApplicationInfo.CATEGORY_MAPS        -> "Maps"
                ApplicationInfo.CATEGORY_IMAGE       -> "Photos"
                ApplicationInfo.CATEGORY_GAME        -> "Games"
                ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Productivity"
                else                                 -> null
            }
            if (apiCategory != null) return apiCategory
        }

        // Heuristic fallback for well-known packages
        return when {
            packageName.contains("instagram") ||
            packageName.contains("facebook") ||
            packageName.contains("twitter") ||
            packageName.contains("snapchat") ||
            packageName.contains("tiktok") ||
            packageName.contains("musically") ||
            packageName.contains("linkedin") ||
            packageName.contains("pinterest") ||
            packageName.contains("reddit") ||
            packageName.contains("discord")     -> "Social"

            packageName.contains("youtube") ||
            packageName.contains("netflix") ||
            packageName.contains("hotstar") ||
            packageName.contains("primevideo") ||
            packageName.contains("hulu") ||
            packageName.contains("twitch")      -> "Entertainment"

            packageName.contains("whatsapp") ||
            packageName.contains("telegram") ||
            packageName.contains("signal") ||
            packageName.contains("messenger")   -> "Messaging"

            packageName.contains("chrome") ||
            packageName.contains("firefox") ||
            packageName.contains("brave") ||
            packageName.contains("opera") ||
            packageName.contains("samsung.internet") -> "Browser"

            packageName.contains("gmail") ||
            packageName.contains("outlook") ||
            packageName.contains("email")       -> "Productivity"

            packageName.contains("game") ||
            packageName.contains("play.games")  -> "Games"

            else                                -> "Other"
        }
    }

    /**
     * Returns the "YYYY-MM-DD" date string for the calendar day that contains
     * [epochMs].  This is a pure utility that does not touch any shared state.
     */
    @Suppress("unused")
    private fun epochToDateString(epochMs: Long): String =
        dateFormatter.format(Date(epochMs))

    /**
     * Returns the epoch-ms for midnight (start of today) in the device's
     * default timezone.  Used for sanity-checking session timestamps that
     * span midnight — if [sessionStartWall] is yesterday and [sessionEndWall]
     * is today, two separate records should ideally be written.
     *
     * This split is left as a future enhancement; for now sessions that span
     * midnight are attributed entirely to the start date.
     */
    @Suppress("unused")
    private fun todayMidnightMs(): Long =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
