package org.mosyagin.project.di

import org.koin.dsl.module
import org.mosyagin.project.export.*
import org.mosyagin.project.crypto.*

val desktopModule = module {
    single<PropExporter> { DesktopPropExporter() }
    single<FileSaver> { DesktopFileSaver() }
    
    // Провайдер ключей для Desktop (JVM)
    single<KeyProvider> { JvmKeyProvider() }
}
