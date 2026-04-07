package org.mosyagin.project.di

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.realtime.Realtime
import io.ktor.client.engine.*
import org.koin.dsl.module
import org.mosyagin.project.BuildKonfig

/**
 * Модуль для работы с Supabase.
 */
val supabaseModule = module {
    single {
        val engine = get<HttpClientEngine>()
        createSupabaseClient(
            supabaseUrl = BuildKonfig.SUPABASE_URL,
            supabaseKey = BuildKonfig.SUPABASE_KEY
        ) {
            httpEngine = engine
            install(Postgrest)
            install(Auth) {
                // Добавьте эти строки:
                scheme = "cineprop"
                host = "auth"
            }
            install(Storage)
            install(Realtime)
        }
    }
}
