package com.example.addictionreductionapp.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.addictionreductionapp.data.local.converters.Converters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.addictionreductionapp.data.local.dao.AchievementDao
import com.example.addictionreductionapp.data.local.dao.AppLimitDao
import com.example.addictionreductionapp.data.local.dao.AppUsageDao
import com.example.addictionreductionapp.data.local.dao.ChatMessageDao
import com.example.addictionreductionapp.data.local.dao.FocusSessionDao
import com.example.addictionreductionapp.data.local.dao.ReductionPlanDao
import com.example.addictionreductionapp.data.local.dao.UserProfileDao
import com.example.addictionreductionapp.data.local.entities.AchievementEntity
import com.example.addictionreductionapp.data.local.entities.AppLimitEntity
import com.example.addictionreductionapp.data.local.entities.AppUsageEntity
import com.example.addictionreductionapp.data.local.entities.ChatMessageEntity
import com.example.addictionreductionapp.data.local.entities.FocusSessionEntity
import com.example.addictionreductionapp.data.local.entities.GoalEntity
import com.example.addictionreductionapp.data.local.entities.InterventionEntity
import com.example.addictionreductionapp.data.local.entities.ReductionPlanEntity
import com.example.addictionreductionapp.data.local.entities.UserProfileEntity
import net.sqlcipher.database.SupportFactory

/**
 * SmartFocus Room Database — the single source of truth for all persistent data.
 *
 * ## Architecture Notes
 *
 * - **Singleton** enforced via Hilt's `@Singleton` scope in [DatabaseModule].
 *   Direct construction via [getInstance] is kept for testing / Workers that
 *   cannot use Hilt injection.
 *
 * - **Version**: bump [DATABASE_VERSION] and provide a [Migration] whenever you
 *   add/rename columns or tables.  Never use `fallbackToDestructiveMigration`
 *   in production unless data loss is acceptable.
 *
 * - **TypeConverters**: [Converters] handles List<String> and List<Int> ↔ JSON.
 *   Registered here so they are available to every DAO in this database.
 *
 * - **exportSchema**: set to `true` in production so Room generates a schema
 *   JSON file that can be committed to version control for auditing migrations.
 *   Set `room.schemaLocation` in `build.gradle.kts` ksp block (see below).
 *
 * ## ksp schema export (add to app/build.gradle.kts):
 * ```
 * ksp {
 *     arg("room.schemaLocation", "$projectDir/schemas")
 *     arg("room.incremental", "true")
 * }
 * ```
 *
 * ## Entities registered:
 * - [AppLimitEntity]    — per-app blocking configuration
 * - [AppUsageEntity]    — daily per-app usage records (automatic tracking)
 * - [FocusSessionEntity] — completed focus session history
 * - [UserProfileEntity]  — user profile & aggregate stats (single-row)
 */
@Database(
    entities = [
        AchievementEntity::class,
        ChatMessageEntity::class,
        AppLimitEntity::class,
        AppUsageEntity::class,
        FocusSessionEntity::class,
        GoalEntity::class,
        InterventionEntity::class,
        ReductionPlanEntity::class,
        UserProfileEntity::class,
        com.example.addictionreductionapp.data.local.entities.DailyBehaviorSnapshotEntity::class
    ],
    version = 10,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    // ── DAO accessors ─────────────────────────────────────────────────────────

    /** DAO for per-app blocking rules. */
    abstract fun appLimitDao(): AppLimitDao

    /** DAO for automatic app usage tracking (daily records). */
    abstract fun appUsageDao(): AppUsageDao

    /** DAO for completed focus sessions (analytics source). */
    abstract fun focusSessionDao(): FocusSessionDao

    /** DAO for user profile and aggregate stats. */
    abstract fun userProfileDao(): UserProfileDao

    /** DAO for analytics reads. */
    abstract fun analyticsDao(): com.example.addictionreductionapp.data.local.dao.AnalyticsDao

    /** DAO for historical behavior snapshots. */
    abstract fun dailyBehaviorSnapshotDao(): com.example.addictionreductionapp.data.local.dao.DailyBehaviorSnapshotDao

    /** DAO for user goals. */
    abstract fun goalDao(): com.example.addictionreductionapp.data.local.dao.GoalDao

    /** DAO for interventions. */
    abstract fun interventionDao(): com.example.addictionreductionapp.data.local.dao.InterventionDao

    /** DAO for achievements. */
    abstract fun achievementDao(): AchievementDao

    /** DAO for chat messages. */
    abstract fun chatMessageDao(): ChatMessageDao

    /** DAO for reduction plans. */
    abstract fun reductionPlanDao(): ReductionPlanDao

    // ── Manual singleton (for Workers / non-Hilt contexts) ────────────────────

    companion object {

        /** Current schema version. Increment on every schema change. */
        const val DATABASE_VERSION = 10

        /** SQLite file name on disk. */
        private const val DATABASE_NAME = "smartfocus.db"

        /**
         * Volatile ensures all threads always see the most recently written
         * value — critical for double-checked locking correctness on JVM.
         */
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Returns the application-scoped singleton instance.
         *
         * Prefer Hilt injection ([DatabaseModule]) over calling this directly.
         * This method exists for contexts where Hilt is unavailable
         * (e.g. [androidx.work.Worker], instrumented tests).
         *
         * Thread-safe via double-checked locking.
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            val passphrase = DatabaseSecurity.getOrCreatePassphrase(context)
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                // ── SQLCipher encryption (M1.3) ──────────────────────────
                // All data at rest is encrypted with a device-unique key
                // managed by DatabaseSecurity via Android Keystore.
                .openHelperFactory(SupportFactory(passphrase))
                // ── Migration strategy ─────────────────────────────────────
                // Add new migrations here in version order. Room applies them
                // sequentially; never use fallbackToDestructiveMigration() in
                // production unless data loss is explicitly acceptable.
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
                // ── Performance ────────────────────────────────────────────
                // enableMultiInstanceInvalidation is needed if you open the same
                // DB from multiple processes (e.g. an isolated :accessibility process).
                .enableMultiInstanceInvalidation()
                .build()
        }

        /**
         * Migration 1 → 2: adds the "app_usage" table with its indices.
         *
         * Written as raw SQL so it precisely matches the schema that Room's
         * annotation processor would generate — verified against the KSP output.
         * Existing rows in other tables are untouched.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create the app_usage table.
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `app_usage` (
                        `id`              INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `package_name`    TEXT    NOT NULL,
                        `app_name`        TEXT    NOT NULL,
                        `usage_minutes`   INTEGER NOT NULL DEFAULT 0,
                        `open_count`      INTEGER NOT NULL DEFAULT 0,
                        `start_timestamp` INTEGER NOT NULL DEFAULT 0,
                        `end_timestamp`   INTEGER NOT NULL DEFAULT 0,
                        `usage_date`      TEXT    NOT NULL,
                        `app_category`    TEXT    NOT NULL DEFAULT 'Unknown'
                    )
                    """.trimIndent()
                )
                // Single-column index on usage_date for day-scoped reads.
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_app_usage_usage_date` " +
                    "ON `app_usage` (`usage_date`)"
                )
                // Composite unique index enforcing one record per app per day.
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " +
                    "`index_app_usage_package_name_usage_date` " +
                    "ON `app_usage` (`package_name`, `usage_date`)"
                )
            }
        }

        // ── Future migration stub ────────────────────────────────────────────
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `daily_behavior_snapshots` (
                        `date` TEXT NOT NULL,
                        `totalScreenTimeMinutes` INTEGER NOT NULL,
                        `totalOpens` INTEGER NOT NULL,
                        `focusScore` INTEGER NOT NULL,
                        `productiveRatio` REAL NOT NULL,
                        `distractionRatio` REAL NOT NULL,
                        `appSwitches` INTEGER NOT NULL,
                        `overallRiskScore` REAL NOT NULL,
                        `doomscrollDetected` INTEGER NOT NULL,
                        `compulsiveSwitchingDetected` INTEGER NOT NULL,
                        `lateNightUsageDetected` INTEGER NOT NULL,
                        `relapseDetected` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`date`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `goals` (
                        `id` INTEGER NOT NULL,
                        `daily_target_minutes` INTEGER NOT NULL DEFAULT 120,
                        `weekly_target_minutes` INTEGER NOT NULL DEFAULT 840,
                        `baseline_daily_average` INTEGER NOT NULL DEFAULT 0,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `interventions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `type` TEXT NOT NULL,
                        `package_name_blocked` TEXT,
                        `journal_text` TEXT,
                        `timestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `achievements` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `icon` TEXT NOT NULL,
                        `is_unlocked` INTEGER NOT NULL DEFAULT 0,
                        `progress` REAL NOT NULL DEFAULT 0.0,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `chat_messages` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sender` TEXT NOT NULL,
                        `text` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `reduction_plans` (
                        `id` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `baseline_minutes` INTEGER NOT NULL,
                        `current_target` INTEGER NOT NULL,
                        `daily_step_down` INTEGER NOT NULL DEFAULT 10,
                        `floor_minutes` INTEGER NOT NULL DEFAULT 30,
                        `is_active` INTEGER NOT NULL DEFAULT 1,
                        `days_active` INTEGER NOT NULL DEFAULT 0,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * Migration 8 → 9: adds onboarding/permissions tracking columns to user_profile.
         * Also adds last_streak_date for StreakSyncManager (M3.1).
         */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `user_profile` ADD COLUMN `has_completed_permissions_screen` INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE `user_profile` ADD COLUMN `has_completed_smart_reduction_setup` INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE `user_profile` ADD COLUMN `last_streak_date` TEXT"
                )
            }
        }
        /**
         * Migration 9 → 10: rewrites goals table from single-row screen-time targets
         * to a multi-row personal goal system (M4).
         * Existing data is preserved as a "Reduce Screen Time" default goal.
         */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE goals_new (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL DEFAULT '',
                        `description` TEXT NOT NULL DEFAULT '',
                        `goal_type` TEXT NOT NULL DEFAULT 'CUSTOM',
                        `target_screen_time_per_day` INTEGER NOT NULL DEFAULT 120,
                        `saved_hours_total` INTEGER NOT NULL DEFAULT 0,
                        `progress` REAL NOT NULL DEFAULT 0.0,
                        `category` TEXT,
                        `start_date` TEXT NOT NULL DEFAULT '',
                        `target_date` TEXT,
                        `is_active` INTEGER NOT NULL DEFAULT 1,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        `completed_at` INTEGER
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO goals_new (title, description, goal_type, target_screen_time_per_day, start_date, created_at, updated_at)
                    SELECT 'Reduce Screen Time', 'Reduce daily screen time to target', 'CUSTOM', COALESCE(daily_target_minutes, 120), date('now'), created_at, updated_at
                    FROM goals WHERE id = 1
                """.trimIndent())
                db.execSQL("DROP TABLE goals")
                db.execSQL("ALTER TABLE goals_new RENAME TO goals")
            }
        }
    }
}
