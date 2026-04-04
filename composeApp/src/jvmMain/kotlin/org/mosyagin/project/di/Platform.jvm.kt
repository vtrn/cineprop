package org.mosyagin.project.di

import org.koin.core.module.Module
import org.koin.dsl.module
import org.mosyagin.project.export.*

/**
 * JVM-специфичная реализация модулей Koin.
 */
actual val platformModule: Module = module {
    single<PropExporter> { DesktopPropExporter() }
    single<FileSaver> { DesktopFileSaver() }
}
