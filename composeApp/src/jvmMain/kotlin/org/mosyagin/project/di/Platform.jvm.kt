package org.mosyagin.project.di

import io.ktor.client.engine.cio.*
import org.koin.core.module.Module
import org.koin.dsl.module
import org.mosyagin.project.export.*
import org.mosyagin.project.crypto.*
import org.mosyagin.project.util.NetworkObserver
import org.mosyagin.project.util.JvmNetworkObserver

/**
 * JVM-специфичная реализация модулей Koin.
 */
actual val platformModule: Module = module {
    single<PropExporter> { DesktopPropExporter() }
    single<FileSaver> { DesktopFileSaver() }
    
    // Провайдер ключей для Desktop (JVM)
    single<KeyProvider> { JvmKeyProvider() }

    // Наблюдатель за сетью для Desktop
    single<NetworkObserver> { JvmNetworkObserver() }

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
