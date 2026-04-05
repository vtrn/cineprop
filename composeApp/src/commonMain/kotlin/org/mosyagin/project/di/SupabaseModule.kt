package org.mosyagin.project.di

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.storage.Storage
import org.koin.dsl.module
import org.mosyagin.project.BuildKonfig

/**
 * Модуль для работы с Supabase.
 * Ключи подтягиваются из BuildKonfig (из local.properties), 
 * что безопасно для публичных репозиториев.
 */
val supabaseModule = module {
    single {
        createSupabaseClient(
            supabaseUrl = BuildKonfig.SUPABASE_URL,
            supabaseKey = BuildKonfig.SUPABASE_KEY
        ) {
            install(Postgrest) // Для синхронизации таблиц
            install(Auth)      // Для авторизации пользователей
            install(Storage)   // Для хранения файлов
        }
    }
}
