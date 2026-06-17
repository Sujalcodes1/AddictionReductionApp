package com.example.addictionreductionapp.data.local.database

import android.content.Context
import com.example.addictionreductionapp.data.local.dao.AppLimitDao
import com.example.addictionreductionapp.data.local.dao.AppUsageDao
import com.example.addictionreductionapp.data.local.dao.FocusSessionDao
import com.example.addictionreductionapp.data.local.dao.UserProfileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides database and DAO instances to the DI graph.
 *
 * ## Why a separate module?
 * Room's [AppDatabase] requires a [Context] to be constructed.  Hilt's
 * [SingletonComponent] already holds the application-scoped [Context], so we
 * install this module there and bind the database to that lifetime.
 *
 * ## Scope
 * - [AppDatabase] → @Singleton  (one instance for the entire app lifetime)
 * - Each DAO      → @Singleton  (DAOs are cheap interfaces; sharing is safe)
 *
 * ## Consumption
 * Inject any DAO directly into a Repository:
 * ```kotlin
 * class FocusRepository @Inject constructor(
 *     private val focusSessionDao: FocusSessionDao
 * )
 * ```
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Provides the application-scoped [AppDatabase] singleton.
     *
     * [ApplicationContext] qualifies the injected [Context] to ensure we always
     * receive the application context (not an Activity context), preventing
     * accidental memory leaks.
     */
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase = AppDatabase.getInstance(context)

    /**
     * Provides [AppLimitDao].
     *
     * Room generates the implementation; Hilt wires the dependency automatically.
     * The DAO instance is valid for as long as the database instance is alive
     * (application lifetime).
     */
    @Provides
    @Singleton
    fun provideAppLimitDao(db: AppDatabase): AppLimitDao = db.appLimitDao()

    /**
     * Provides [AppUsageDao] for injecting into Repositories and the usage-tracking
     * Service / Worker that records foreground events.
     */
    @Provides
    @Singleton
    fun provideAppUsageDao(db: AppDatabase): AppUsageDao = db.appUsageDao()

    /**
     * Provides [FocusSessionDao] for injecting into Repositories and Workers.
     */
    @Provides
    @Singleton
    fun provideFocusSessionDao(db: AppDatabase): FocusSessionDao = db.focusSessionDao()

    /**
     * Provides [UserProfileDao] for profile reads/writes across the app.
     */
    @Provides
    @Singleton
    fun provideUserProfileDao(db: AppDatabase): UserProfileDao = db.userProfileDao()

    /**
     * Provides [AnalyticsDao] for analytics reads across the app.
     */
    @Provides
    @Singleton
    fun provideAnalyticsDao(db: AppDatabase): com.example.addictionreductionapp.data.local.dao.AnalyticsDao = db.analyticsDao()

    /**
     * Provides [DailyBehaviorSnapshotDao] for persisting historical behavioral snapshots.
     */
    @Provides
    @Singleton
    fun provideDailyBehaviorSnapshotDao(db: AppDatabase): com.example.addictionreductionapp.data.local.dao.DailyBehaviorSnapshotDao = db.dailyBehaviorSnapshotDao()
}
