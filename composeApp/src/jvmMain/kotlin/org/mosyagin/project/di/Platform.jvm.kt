package org.mosyagin.project.di

import io.ktor.client.engine.cio.*
import org.koin.core.module.Module
import org.koin.dsl.module
import org.mosyagin.project.export.*
import org.mosyagin.project.crypto.*

/**
 * JVM-специфичная реализация модулей Koin.
 */
actual val platformModule: Module = module {
    single<PropExporter> { DesktopPropExporter() }
    single<FileSaver> { DesktopFileSaver() }
    
    // Провайдер ключей для Desktop (JVM)
    single<KeyProvider> { JvmKeyProvider() }

    // Конфигурация CIO для Desktop
    single {
        CIO.create {
            endpoint {
                keepAliveTime = 30000
                connectTimeout = 30000
            }
        }
    }
}
