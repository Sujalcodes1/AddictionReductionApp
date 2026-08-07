package com.example.addictionreductionapp.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reduction_plans")
data class ReductionPlanEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "baseline_minutes")
    val baselineMinutes: Int,

    @ColumnInfo(name = "current_target")
    val currentTarget: Int,

    @ColumnInfo(name = "daily_step_down")
    val dailyStepDown: Int = 10,

    @ColumnInfo(name = "floor_minutes")
    val floorMinutes: Int = 30,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "days_active")
    val daysActive: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
