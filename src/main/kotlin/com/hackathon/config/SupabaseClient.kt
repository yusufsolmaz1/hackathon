package com.hackathon.config

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

fun buildSupabaseClient(): SupabaseClient {
    val url = System.getenv("SUPABASE_URL")
        ?: error("SUPABASE_URL environment variable is required")
    val key = System.getenv("SUPABASE_KEY")
        ?: error("SUPABASE_KEY environment variable is required")

    return createSupabaseClient(supabaseUrl = url, supabaseKey = key) {
        install(Postgrest)
    }
}
