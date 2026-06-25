package com.example.addictionreductionapp.data

import android.util.Log
import com.example.addictionreductionapp.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseProvider {

    val client: SupabaseClient by lazy {

        // DEBUG
        Log.d("SUPABASE_TEST", "URL = ${BuildConfig.SUPABASE_URL}")
        Log.d(
            "SUPABASE_TEST",
            "KEY = ${BuildConfig.SUPABASE_ANON_KEY.take(20)}..."
        )

        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth)
            install(Postgrest)
        }
    }
}