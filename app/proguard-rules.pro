# ── Security / Tink ──────────────────────────────────────────────────
-dontwarn com.google.errorprone.annotations.**

# ── Hilt / Dagger ──────────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# ── Room ───────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ── Supabase + Ktor ─────────────────────────────────────────────────
-keep class io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# ── Kotlin Serialization ────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.example.addictionreductionapp.**$$serializer { *; }
-keepclassmembers class com.example.addictionreductionapp.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.addictionreductionapp.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ── Gson (used by Room TypeConverters) ──────────────────────────────
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }

# ── BuildConfig (Supabase credentials) ─────────────────────────────
-keep class com.example.addictionreductionapp.BuildConfig { *; }

# ── SQLCipher (Room encryption) ──────────────────────────────────
-keep class net.zetetic.database.** { *; }
-keep class net.sqlcipher.** { *; }
-dontwarn net.sqlcipher.**

# ── Compose ─────────────────────────────────────────────────────────
-dontwarn androidx.compose.**

# ── Line numbers for crash reports ──────────────────────────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Firebase (M9) ──────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

