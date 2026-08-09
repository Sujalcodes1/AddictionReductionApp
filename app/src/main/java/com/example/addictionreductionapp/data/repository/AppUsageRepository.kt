package com.example.addictionreductionapp.data.repository

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import com.example.addictionreductionapp.data.local.dao.AppUsageDao
import com.example.addictionreductionapp.data.local.entities.AppUsageEntity
import com.example.addictionreductionapp.utils.AppCategoryResolver
import com.example.addictionreductionapp.utils.PermissionUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUsageRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appUsageDao: AppUsageDao
) {
    companion object {
        private const val TAG = "AppUsageRepository"
        private val DATE_FMT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }

    /**
     * Tracks the last time syncUsageFromSystem ran successfully.
     * persistSession is suppressed for packages that have already been synced
     * by the OS for today, preventing dual-writer inflation.
     */
    @Volatile private var lastOsSyncTimeMs: Long = 0L
    @Volatile private var lastOsSyncDate: String = ""

    fun getTopUsedAppsToday(today: String, limit: Int = 5): Flow<List<AppUsageEntity>> =
        appUsageDao.getTopUsedAppsToday(today, limit)

    fun getMostOpenedApps(today: String, limit: Int = 5): Flow<List<AppUsageEntity>> =
        appUsageDao.getMostOpenedApps(today, limit)

    suspend fun getUsageForAppOnDate(packageName: String, date: String): AppUsageEntity? =
        appUsageDao.getUsageForAppOnDate(packageName, date)

    suspend fun persistSession(
        packageName: String,
        appName: String,
        category: String,
        sessionStartWall: Long,
        sessionEndWall: Long,
        durationMinutes: Int,
        usageDate: String
    ) {
        if (isIgnoredPackage(packageName)) {
            Log.v(TAG, "persistSession skipped for ignored package: $packageName")
            return
        }

        // If the OS has already synced this date authoritatively, do NOT accumulate
        // on top of it. The OS sync (queryAndAggregateUsageStats) is the single source
        // of truth. Adding accessibility service session minutes on top causes 2-10x inflation.
        if (usageDate == lastOsSyncDate && lastOsSyncTimeMs > 0L) {
            Log.v(TAG, "persistSession suppressed for $packageName on $usageDate — OS sync is authoritative")
            return
        }

        Log.d(TAG, "app_usage INSERT START: pkg=$packageName date=$usageDate minutes=$durationMinutes")

        val rowsAffected: Int = appUsageDao.accumulateUsage(
            packageName = packageName,
            date = usageDate,
            additionalMinutes = durationMinutes,
            newEndTimestamp = sessionEndWall
        )

        Log.d(TAG, "app_usage SQL row count affected by UPDATE: $rowsAffected (pkg=$packageName date=$usageDate)")

        if (rowsAffected > 0) {
            Log.i(TAG, "app_usage UPDATE SUCCESS: pkg=$packageName date=$usageDate +${durationMinutes}m (rowsAffected=$rowsAffected)")
            return
        }

        val entity = AppUsageEntity(
            packageName = packageName,
            appName = appName,
            usageMinutes = durationMinutes,
            openCount = 1,
            startTimestamp = sessionStartWall,
            endTimestamp = sessionEndWall,
            usageDate = usageDate,
            appCategory = category
        )

        val insertedRowId: Long = appUsageDao.insertUsage(entity)

        if (insertedRowId != -1L) {
            Log.i(TAG, "app_usage INSERT SUCCESS: pkg=$packageName date=$usageDate minutes=${durationMinutes}m rowId=$insertedRowId")
        } else {
            Log.w(TAG, "app_usage INSERT IGNORED (race-condition duplicate): pkg=$packageName date=$usageDate")
        }
    }

    suspend fun getTotalMinutesForCategory(category: String, date: String): Int =
        appUsageDao.getTotalMinutesForCategory(category, date)

    /**
     * Synchronizes historical and real-time usage data from Android's UsageStatsManager
     * into Room database (app_usage table).
     *
     * Uses queryAndAggregateUsageStats to ensure single-source-of-truth accuracy
     * that matches Android Digital Wellbeing without interval bucket duplication.
     */
    suspend fun syncUsageFromSystem(daysToSync: Int = 7) = withContext(Dispatchers.IO) {
        if (!PermissionUtils.hasUsageAccess(context)) {
            Log.d(TAG, "syncUsageFromSystem skipped: Usage Access permission not granted.")
            return@withContext
        }

        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return@withContext
        val pm = context.packageManager

        // Purge self and system daemons from Room DB
        try {
            appUsageDao.cleanIgnoredPackages(context.packageName)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clean ignored packages: ${e.message}", e)
        }

        Log.i(TAG, "Starting syncUsageFromSystem for past $daysToSync days...")

        val todayStr = DATE_FMT.format(Date(System.currentTimeMillis()))

        for (dayOffset in daysToSync downTo 0) {
            val dayCalendar = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -dayOffset)
                // CRITICAL: always start exactly at midnight 00:00:00.000 of the calendar day
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startTime = dayCalendar.timeInMillis
            val dateStr = DATE_FMT.format(Date(startTime))

            // For today: query from midnight up to NOW only (not 23:59)
            // For past days: query the full day
            val endTime = if (dayOffset == 0) {
                System.currentTimeMillis()
            } else {
                val endCal = (dayCalendar.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                endCal.timeInMillis
            }

            // Safety: if startTime >= endTime (can happen just after midnight), skip
            if (startTime >= endTime) {
                Log.d(TAG, "Skipping $dateStr: startTime=$startTime >= endTime=$endTime")
                continue
            }

            // Use queryAndAggregateUsageStats to get exact, deduplicated foreground times for the day
            val statsMap = usm.queryAndAggregateUsageStats(startTime, endTime)

            // Query UsageEvents for open counts and session timestamps
            val openCounts = mutableMapOf<String, Int>()
            val firstTimestamps = mutableMapOf<String, Long>()
            val lastTimestamps = mutableMapOf<String, Long>()

            try {
                val events = usm.queryEvents(startTime, endTime)
                val event = UsageEvents.Event()
                while (events.hasNextEvent()) {
                    events.getNextEvent(event)
                    val pkg = event.packageName ?: continue
                    if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                        openCounts[pkg] = (openCounts[pkg] ?: 0) + 1
                        if (!firstTimestamps.containsKey(pkg)) {
                            firstTimestamps[pkg] = event.timeStamp
                        }
                        lastTimestamps[pkg] = event.timeStamp
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error querying UsageEvents for $dateStr", e)
            }

            for ((packageName, stat) in statsMap) {
                if (isIgnoredPackage(packageName)) continue

                val foregroundMs = stat.totalTimeInForeground
                val minutes = (foregroundMs / 60_000L).toInt()
                val opens = openCounts[packageName] ?: if (minutes > 0) 1 else 0
                if (minutes <= 0 && opens <= 0) continue

                var appInfo: ApplicationInfo? = null
                val appName = try {
                    appInfo = pm.getApplicationInfo(packageName, 0)
                    pm.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) {
                    getAppNameForPackage(packageName)
                }

                val category = AppCategoryResolver.resolveCategory(packageName, appInfo)
                val startTs = firstTimestamps[packageName] ?: stat.firstTimeStamp.takeIf { it > 0 } ?: startTime
                val endTs = lastTimestamps[packageName] ?: stat.lastTimeUsed.takeIf { it > 0 } ?: endTime

                // Atomically overwrite with exact OS ground-truth — this is the single source of truth
                appUsageDao.upsertDailyUsage(
                    packageName = packageName,
                    appName = appName,
                    category = category,
                    date = dateStr,
                    minutes = minutes,
                    opens = maxOf(opens, 1),
                    startTs = startTs,
                    endTs = endTs
                )
            }

            // Mark today's sync complete so persistSession is suppressed
            if (dayOffset == 0) {
                lastOsSyncDate = todayStr
                lastOsSyncTimeMs = System.currentTimeMillis()
                Log.d(TAG, "OS sync complete for $dateStr — persistSession suppressed for today")
            }
        }
        Log.i(TAG, "syncUsageFromSystem completed successfully.")
    }

    private fun isIgnoredPackage(pkg: String): Boolean {
        return pkg == context.packageName ||
               pkg == "com.example.addictionreductionapp" ||
               pkg == "com.android.systemui" ||
               pkg == "android" ||
               pkg.contains("inputmethod") ||
               pkg.contains("latin") ||
               pkg.contains("launcher") ||
               pkg.startsWith("com.google.android.gms") ||
               pkg.startsWith("com.google.android.gsf") ||
               pkg.startsWith("com.android.providers")
    }

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
}
