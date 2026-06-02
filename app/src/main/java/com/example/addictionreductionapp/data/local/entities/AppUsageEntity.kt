package com.example.addictionreductionapp.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persists one day-level usage record per app per calendar date.
 *
 * The [packageName] + [usageDate] composite index is the logical unique key:
 * each row represents a single app's aggregated usage for a single day.
 * Use [AppUsageDao.updateUsage] to accumulate minutes and open counts across
 * multiple foreground events on the same day rather than inserting duplicates.
 *
 * ## Granularity
 * | Field            | Granularity                                  |
 * |------------------|----------------------------------------------|
 * | [usageMinutes]   | Whole minutes; fractional minutes are lost   |
 * | [openCount]      | Incremented on every distinct foreground event |
 * | [startTimestamp] | Epoch ms of the *first* foreground event today |
 * | [endTimestamp]   | Epoch ms of the *last* background event today |
 * | [usageDate]      | "YYYY-MM-DD" string for efficient day-scoped queries |
 *
 * ## Data lifecycle
 * Old records should be pruned periodically via [AppUsageDao.deleteOldUsageData]
 * to prevent unbounded table growth.
 *
 * Table: "app_usage"
 */
@Entity(
    tableName = "app_usage",
    indices = [
        // Accelerates the most common query: all apps used on a given date.
        Index(value = ["usage_date"]),
        // Accelerates per-app history lookups.
        Index(value = ["package_name", "usage_date"], unique = true)
    ]
)
data class AppUsageEntity(

    /** Auto-generated surrogate primary key. */
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    /**
     * Android package name of the tracked app, e.g. "com.instagram.android".
     * This is the stable identifier used to correlate records across days.
     */
    @ColumnInfo(name = "package_name")
    val packageName: String,

    /**
     * Human-readable display label retrieved from [PackageManager], e.g. "Instagram".
     * Stored redundantly for fast display without re-querying PackageManager.
     * May be "Unknown" when PackageManager cannot resolve the label.
     */
    @ColumnInfo(name = "app_name")
    val appName: String,

    /**
     * Total foreground time for this app on [usageDate], measured in whole minutes.
     * Accumulated incrementally as the tracking service records foreground events.
     */
    @ColumnInfo(name = "usage_minutes")
    val usageMinutes: Int = 0,

    /**
     * Number of distinct foreground launches on [usageDate].
     * Incremented each time the app transitions from background → foreground.
     */
    @ColumnInfo(name = "open_count")
    val openCount: Int = 0,

    /**
     * Epoch milliseconds of the very first foreground event for this app today.
     * Useful for "first opened at" analytics. 0L until the first event is recorded.
     */
    @ColumnInfo(name = "start_timestamp")
    val startTimestamp: Long = 0L,

    /**
     * Epoch milliseconds of the most recent background transition for this app today.
     * Updated on each background event. 0L until the app first goes to background.
     */
    @ColumnInfo(name = "end_timestamp")
    val endTimestamp: Long = 0L,

    /**
     * Calendar date in "YYYY-MM-DD" format (e.g. "2025-05-27").
     * Stored as a string so Room can filter by exact day without epoch arithmetic,
     * and so records survive DST changes without ambiguity.
     */
    @ColumnInfo(name = "usage_date")
    val usageDate: String,

    /**
     * Optional coarse category label for the app, e.g. "Social", "Productivity",
     * "Entertainment", "Games", "Unknown".
     *
     * Populated from [android.app.usage.UsageStats] / PackageManager heuristics.
     * Stored as a plain string so new categories can be added without a migration.
     */
    @ColumnInfo(name = "app_category")
    val appCategory: String = "Unknown"
)
