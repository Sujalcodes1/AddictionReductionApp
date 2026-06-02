package com.example.addictionreductionapp.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persists the user's per-app blocking configuration.
 *
 * Mirrors the in-memory [com.example.addictionreductionapp.data.AppTarget] model
 * but survives process death and device reboots.
 *
 * Table: "app_limits"
 */
@Entity(tableName = "app_limits")
data class AppLimitEntity(

    /** Android package name — unique natural key used as the primary key. */
    @PrimaryKey
    @ColumnInfo(name = "package_name")
    val packageName: String,

    /** Human-readable display name, e.g. "Instagram". */
    @ColumnInfo(name = "app_name")
    val appName: String,

    /** Whether this app is currently selected for monitoring/blocking. */
    @ColumnInfo(name = "is_selected")
    val isSelected: Boolean = false,

    /** Daily usage limit in minutes. Default 60 min. */
    @ColumnInfo(name = "limit_minutes")
    val limitMinutes: Int = 60,

    /**
     * Hard-lock flag. When true the user cannot modify settings for this app
     * without disabling focus mode first.
     */
    @ColumnInfo(name = "is_locked")
    val isLocked: Boolean = false,

    /**
     * Start hour (0-23) for a scheduled block window.
     * -1 means no scheduled blocking.
     */
    @ColumnInfo(name = "block_schedule_start")
    val blockScheduleStart: Int = -1,

    /** End hour (0-23) for the scheduled block window. -1 = disabled. */
    @ColumnInfo(name = "block_schedule_end")
    val blockScheduleEnd: Int = -1,

    /** Whitelisted apps are never blocked, even in focus mode. */
    @ColumnInfo(name = "is_whitelisted")
    val isWhitelisted: Boolean = false
)
