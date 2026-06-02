package com.example.addictionreductionapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry-point for Hilt dependency injection.
 *
 * @HiltAndroidApp triggers Hilt's code generation and bootstraps the
 * application-scoped Dagger component (AppComponent).  Every @Inject,
 * @HiltViewModel, and @AndroidEntryPoint in the app ultimately resolves
 * its dependencies through the component that is created here.
 *
 * Registration in AndroidManifest.xml is required:
 *   android:name=".SmartFocusApp"
 */
@HiltAndroidApp
class SmartFocusApp : Application()
