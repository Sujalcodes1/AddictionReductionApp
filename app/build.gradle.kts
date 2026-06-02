plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // Required in Kotlin 2.x — enables the Compose compiler Gradle plugin.
    // This replaces the old composeOptions { kotlinCompilerExtensionVersion = "..." } block.
    alias(libs.plugins.kotlin.compose)
    // KSP for Room + Hilt annotation processing (KAPT is not supported in Kotlin 2.x)
    alias(libs.plugins.ksp)
    // Hilt DI — was incorrectly commented out; @HiltAndroidApp requires this plugin
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "com.example.addictionreductionapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.addictionreductionapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
    // NOTE: Do NOT add composeOptions { kotlinCompilerExtensionVersion = "..." } here.
    // With Kotlin 2.x + kotlin.plugin.compose, the compiler version is managed
    // automatically by the plugin. Adding it manually causes a conflict.
}

/**
 * KSP arguments for Room:
 * - room.schemaLocation: exports schema JSON files so you can diff schema
 *   changes in git and write proper Migrations.
 * - room.incremental: skips regeneration for unchanged entities (faster builds).
 */
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}

dependencies {
    // ── Core AndroidX ─────────────────────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // ── Activity ──────────────────────────────────────────────────────────────
    // activity-compose provides setContent{} — required for Compose entry point
    implementation(libs.androidx.activity.compose)
    // activity-ktx provides ComponentActivity.viewModels() and other KTX helpers
    implementation(libs.androidx.activity.ktx)

    // ── Compose BOM (manages all Compose artifact versions consistently) ───────
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    // Material Icons Extended — provides Icons.Default.Psychology, etc.
    implementation("androidx.compose.material:material-icons-extended")

    // ── Navigation ────────────────────────────────────────────────────────────
    implementation(libs.androidx.navigation.compose)

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // ── WorkManager ───────────────────────────────────────────────────────────
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // ── Room Persistence Library ───────────────────────────────────────────────
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // ── Hilt Dependency Injection ──────────────────────────────────────────────
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    // hiltViewModel() bridge for Compose Navigation
    implementation(libs.hilt.navigation.compose)

    // ── Kotlin Coroutines ─────────────────────────────────────────────────────
    implementation(libs.kotlinx.coroutines.android)

    // ── Generative AI (Gemini) ────────────────────────────────────────────────
    implementation(libs.generativeai)

    // ── Gson (used by Room TypeConverters for List<String> / List<Int>) ────────
    implementation("com.google.code.gson:gson:2.10.1")

    // ── Testing ───────────────────────────────────────────────────────────────
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}