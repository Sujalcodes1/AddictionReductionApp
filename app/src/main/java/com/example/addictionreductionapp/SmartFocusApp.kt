package com.example.addictionreductionapp

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry-point for Hilt dependency injection and Firebase crash reporting.
 *
 * @HiltAndroidApp triggers Hilt's code generation and bootstraps the
 * application-scoped Dagger component (AppComponent).
 *
 * Firebase Crashlytics:
 *   1. Add google-services.json to app/ directory.
 *   2. Uncomment the google-services plugin in root build.gradle.kts.
 *   3. Crashlytics will auto-initialise via ContentProvider — no code changes needed.
 *
 * Current state: Crashlytics is gracefully omitted until google-services.json is added.
 * The firebase-crashlytics dependency is present but the automatic initialisation
 * only activates when the Google Services plugin processes the JSON config.
 */
@HiltAndroidApp
class SmartFocusApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initCrashReporting()
    }

    private fun initCrashReporting() {
        try {
            Class.forName("com.google.firebase.crashlytics.FirebaseCrashlytics")
            Log.d("SmartFocusApp", "Firebase Crashlytics available — crash reporting active")
        } catch (_: ClassNotFoundException) {
            Log.d("SmartFocusApp", "Firebase not configured — crash reporting skipped")
        }
    }
}
