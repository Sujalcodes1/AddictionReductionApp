package com.example.addictionreductionapp.viewmodel

import com.example.addictionreductionapp.data.models.AppUsageSummary
import com.example.addictionreductionapp.data.models.BehaviorTrendSummary
import com.example.addictionreductionapp.data.models.BehavioralIntelligenceSnapshot
import com.example.addictionreductionapp.data.models.CategoryAnalytics
import com.example.addictionreductionapp.data.models.FocusScoreDetails
import com.example.addictionreductionapp.data.models.HourlyUsagePoint
import com.example.addictionreductionapp.data.models.StreakAnalysis

data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,

    // UI-ready models for general metrics
    val dailyScreenTimeMinutes: Int = 0,
    val totalOpens: Int = 0,

    // UI-ready models for Category Bars
    val categoryUsage: List<CategoryAnalytics> = emptyList(),

    // UI-ready models for Heatmaps / Hourly Graphs
    val hourlyUsage: List<HourlyUsagePoint> = emptyList(),

    // UI-ready models for Charts / Lists
    val topUsedApps: List<AppUsageSummary> = emptyList(),
    val topOpenedApps: List<AppUsageSummary> = emptyList(),

    // UI-ready models for Weekly Graphs
    val weeklyTotalMinutes: Int = 0,
    val weeklyAverageMinutes: Int = 0,

    // UI-ready models for Engines (Score, Streak, Intelligence)
    val focusScoreDetails: FocusScoreDetails? = null,
    val streakAnalysis: StreakAnalysis? = null,
    val behavioralSnapshot: BehavioralIntelligenceSnapshot? = null,

    // UI-ready models for Trend Cards, Trend Arrows, and future dashboards
    // Future ML Dataset Source: trend summaries encode the behavioral trajectory vector.
    val weeklyTrendSummary: BehaviorTrendSummary? = null,
    val monthlyTrendSummary: BehaviorTrendSummary? = null
)

data class DashboardUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    
    // Top-level summary widgets
    val currentFocusScore: Int = 0,
    val currentStreak: Int = 0,
    val todayScreenTime: Int = 0,
    val overallRiskScore: Float = 0f,
    
    // Realtime alerts and highlights
    val activeWarnings: List<String> = emptyList(),
    val mostDistractingApp: String? = null
)
