import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // Required in Kotlin 2.x — enables the Compose compiler Gradle plugin.
    // This replaces the old composeOptions { kotlinCompilerExtensionVersion = "..." } block.
    alias(libs.plugins.kotlin.compose)
    // Required by Supabase-kt — enables @Serializable annotation processing.
    alias(libs.plugins.kotlin.serialization)
    // KSP for Room + Hilt annotation processing (KAPT is not supported in Kotlin 2.x)
    alias(libs.plugins.ksp)
    // Hilt DI — was incorrectly commented out; @HiltAndroidApp requires this plugin
    alias(libs.plugins.hilt.android)
}

// ── Credentials: read SUPABASE_URL and SUPABASE_ANON_KEY from local.properties ──────────
// NEVER hardcode credentials. Keys are injected into BuildConfig at compile time.
// local.properties is in .gitignore and never committed to version control.
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "com.example.addictionreductionapp"
    compileSdk = 36  // Raised from 35: androidx.browser:1.9.0 (transitive from supabase auth-kt) requires 36.
                     // gradle.properties already has android.suppressUnsupportedCompileSdk=36 for AGP 8.7.3 compat.

    defaultConfig {
        applicationId = "com.example.addictionreductionapp"
        minSdk = 26  // Raised from 24: Supabase-kt 3.x native requirement; eliminates desugaring overhead
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ── Supabase BuildConfig fields ────────────────────────────────────────────
        // Read from local.properties — never from hardcoded string literals.
        // Usage in Kotlin: BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY
        buildConfigField(
            "String",
            "SUPABASE_URL",
            "\"${localProperties["SUPABASE_URL"] ?: ""}\""
        )
        buildConfigField(
            "String",
            "SUPABASE_ANON_KEY",
            "\"${localProperties["SUPABASE_ANON_KEY"] ?: ""}\""
        )
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
        // Enables BuildConfig generation so SUPABASE_URL and SUPABASE_ANON_KEY
        // can be read from local.properties at compile time without hardcoding.
        buildConfig = true
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

// ── Dependency resolution overrides ──────────────────────────────────────────
// androidx.browser:1.9.0 (transitive from supabase auth-kt) requires AGP 8.9.1.
// This project uses AGP 8.7.3 and only uses email/password auth — no OAuth browser
// flows. Forcing 1.8.0 satisfies Supabase's runtime needs with the current AGP.
configurations.all {
    resolutionStrategy {
        force("androidx.browser:browser:1.8.0")
    }
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

    // ── Supabase Auth + Postgrest (BOM manages versions — no explicit version needed) ──
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)      // auth-kt: Supabase GoTrue v2 / Auth
    implementation(libs.supabase.postgrest) // postgrest-kt: user_profiles upsert

    // ── Ktor OkHttp engine (required at runtime by Supabase-kt on Android) ────────
    implementation(libs.ktor.client.okhttp)

    // ── Kotlinx Serialization JSON (required by Supabase-kt for data encoding) ────
    implementation(libs.kotlinx.serialization.json)

    // ── Jetpack Security: EncryptedSharedPreferences for auth token storage ───────
    // Session tokens are security-sensitive — must never live in plaintext prefs.
    implementation(libs.security.crypto)

    // ── Testing ───────────────────────────────────────────────────────────────
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}