package org.mosyagin.project.di

import org.koin.dsl.module
import org.mosyagin.project.export.*
import org.mosyagin.project.crypto.*

val androidModule = module {
    single<PropExporter> { AndroidPropExporter() }
    single<FileSaver> { AndroidFileSaver(get()) }
    
    // Провайдер ключей для Android (использует KeyStore)
    single<KeyProvider> { AndroidKeyProvider(get()) }
}
