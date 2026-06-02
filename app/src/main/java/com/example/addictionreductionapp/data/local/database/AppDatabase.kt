package com.example.addictionreductionapp.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.addictionreductionapp.data.local.converters.Converters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.addictionreductionapp.data.local.dao.AppLimitDao
import com.example.addictionreductionapp.data.local.dao.AppUsageDao
import com.example.addictionreductionapp.data.local.dao.FocusSessionDao
import com.example.addictionreductionapp.data.local.dao.UserProfileDao
import com.example.addictionreductionapp.data.local.entities.AppLimitEntity
import com.example.addictionreductionapp.data.local.entities.AppUsageEntity
import com.example.addictionreductionapp.data.local.entities.FocusSessionEntity
import com.example.addictionreductionapp.data.local.entities.UserProfileEntity

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
        AppLimitEntity::class,
        AppUsageEntity::class,
        FocusSessionEntity::class,
        UserProfileEntity::class
    ],
    version = 2,
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

    // ── Manual singleton (for Workers / non-Hilt contexts) ────────────────────

    companion object {

        /** Current schema version. Increment on every schema change. */
        const val DATABASE_VERSION = 2

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

        private fun buildDatabase(context: Context): AppDatabase =
            Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                // ── Migration strategy ─────────────────────────────────────
                // Add new migrations here in version order. Room applies them
                // sequentially; never use fallbackToDestructiveMigration() in
                // production unless data loss is explicitly acceptable.
                .addMigrations(MIGRATION_1_2)
                // ── Performance ────────────────────────────────────────────
                // enableMultiInstanceInvalidation is needed if you open the same
                // DB from multiple processes (e.g. an isolated :accessibility process).
                .enableMultiInstanceInvalidation()
                .build()

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
        // val MIGRATION_2_3 = object : Migration(2, 3) {
        //     override fun migrate(db: SupportSQLiteDatabase) {
        //         db.execSQL("ALTER TABLE app_usage ADD COLUMN new_column TEXT NOT NULL DEFAULT ''")
        //     }
        // }
    }
}
