package com.example.addictionreductionapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Future ML Dataset Source
 * Future Relapse Prediction Features
 * Future Training Inputs
 */
@Entity(tableName = "daily_behavior_snapshots")
data class DailyBehaviorSnapshotEntity(
    @PrimaryKey
    val date: String, // Format: YYYY-MM-DD
    val totalScreenTimeMinutes: Int,
    val totalOpens: Int,
    val focusScore: Int,
    val productiveRatio: Float,
    val distractionRatio: Float,
    val appSwitches: Int,
    val overallRiskScore: Float,

    // Behavior Signals
    val doomscrollDetected: Boolean,
    val compulsiveSwitchingDetected: Boolean,
    val lateNightUsageDetected: Boolean,
    val relapseDetected: Boolean,

    // Metadata
    val createdAt: Long
)
