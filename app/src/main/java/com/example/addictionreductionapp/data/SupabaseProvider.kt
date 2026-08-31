package com.example.addictionreductionapp.data

import android.util.Log
import com.example.addictionreductionapp.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.functions.Functions

object SupabaseProvider {

    val client: SupabaseClient by lazy {

        // Sanitize the URL: strip any accidental trailing path segments that
        // supabase-kt does not accept (e.g. /rest/v1, /auth/v1, trailing slash).
        val rawUrl = BuildConfig.SUPABASE_URL.trim()
        val cleanUrl = rawUrl
            .trimEnd('/')                        // remove trailing slash
            .replace(Regex("/rest/v1$"), "")     // remove /rest/v1 suffix
            .replace(Regex("/auth/v1$"), "")     // remove /auth/v1 suffix
            .replace(Regex("/functions/v1$"), "")

        Log.d("SUPABASE_TEST", "URL (raw)   = $rawUrl")
        Log.d("SUPABASE_TEST", "URL (clean) = $cleanUrl")
        Log.d(
            "SUPABASE_TEST",
            "KEY = ${BuildConfig.SUPABASE_ANON_KEY.take(20)}..."
        )

        // Guard: fail fast with a readable message if credentials are missing.
        check(cleanUrl.isNotBlank()) {
            "SUPABASE_URL is not set. Add it to local.properties: " +
            "SUPABASE_URL=https://<project>.supabase.co"
        }
        check(BuildConfig.SUPABASE_ANON_KEY.isNotBlank()) {
            "SUPABASE_ANON_KEY is not set. Add it to local.properties: " +
            "SUPABASE_ANON_KEY=<your-anon-key>"
        }

        createSupabaseClient(
            supabaseUrl = cleanUrl,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth)
            install(Postgrest)
            install(Functions)
        }
    }
}