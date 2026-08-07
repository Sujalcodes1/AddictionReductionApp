package com.example.addictionreductionapp.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String = "",

    @ColumnInfo(name = "goal_type")
    val goalType: String = "CUSTOM",

    @ColumnInfo(name = "target_screen_time_per_day")
    val targetScreenTimePerDay: Int = 120,

    @ColumnInfo(name = "saved_hours_total")
    val savedHoursTotal: Int = 0,

    val progress: Float = 0f,

    val category: String? = null,

    @ColumnInfo(name = "start_date")
    val startDate: String = "",

    @ColumnInfo(name = "target_date")
    val targetDate: String? = null,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null
)
