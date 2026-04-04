package org.mosyagin.project.export

import org.koin.dsl.module

/**
 * Платформенный модуль для Android (Экспорт)
 */
val androidExportModule = module {
    single<PropExporter> { AndroidPropExporter() }
    single<FileSaver> { AndroidFileSaver(get()) }
}
