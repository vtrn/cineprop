package org.mosyagin.project.di

import io.ktor.client.engine.okhttp.*
import org.koin.core.module.Module
import org.koin.dsl.module
import org.mosyagin.project.export.*
import org.mosyagin.project.crypto.*
import java.util.concurrent.TimeUnit

/**
 * Android-специфичная реализация модулей Koin.
 */
actual val platformModule: Module = module {
    single<PropExporter> { AndroidPropExporter() }
    single<FileSaver> { AndroidFileSaver(get()) }
    
    // Провайдер ключей для Android (использует KeyStore)
    single<KeyProvider> { AndroidKeyProvider(get()) }

    // Конфигурация OkHttp для Supabase Realtime (WebSockets)
    single {
        OkHttp.create {
            config {
                pingInterval(30, TimeUnit.SECONDS) // Поддерживаем соединение живым
                connectTimeout(30, TimeUnit.SECONDS)
                readTimeout(30, TimeUnit.SECONDS)
                writeTimeout(30, TimeUnit.SECONDS)
            }
        }
    }
}
